package com.techmorphosis.grassroot.ui.fragments;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.techmorphosis.grassroot.R;
import com.techmorphosis.grassroot.adapters.UserListAdapter;
import com.techmorphosis.grassroot.interfaces.ClickListener;
import com.techmorphosis.grassroot.services.GrassrootRestService;
import com.techmorphosis.grassroot.services.model.Member;
import com.techmorphosis.grassroot.services.model.MemberList;
import com.techmorphosis.grassroot.ui.views.RecyclerTouchListener;
import com.techmorphosis.grassroot.ui.views.SwipeableRecyclerViewTouchListener;
import com.techmorphosis.grassroot.utils.ErrorUtils;
import com.techmorphosis.grassroot.utils.SettingPreference;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Created by luke on 2016/05/08.
 */
public class MemberListFragment extends Fragment {

    private static final String TAG = MemberListFragment.class.getCanonicalName();

    // since android's framework is so abysmal, an equals comparison on fragment at different stages in its lifecycle fails
    // hence have to create this horrible hack just to be able to compare fragments ...
    private String ID;
    private String groupUid;
    private boolean canDismissItems;
    private boolean showSelected;
    private boolean selectedByDefault;

    private MemberListListener mListener;
    private MemberListClickDismissListener clickDismissListener;
    private GrassrootRestService grassrootRestService;
    private UserListAdapter userListAdapter;

    @BindView(R.id.mlist_frag_recycler_view)
    RecyclerView memberListRecyclerView;

    private ViewGroup vgContainer;

    // NB: we are not doing checks for null on this because we will use the fragment in create group
    public void setGroupUid(String groupUid) {
        this.groupUid = groupUid;
    }

    public void setSelectedByDefault(boolean selectedByDefault) {
        this.selectedByDefault = selectedByDefault;
    }

    // Note: Since Android has terrible framework of no overriding constructors, only non-type-safe bundle nonsense, do this manually
    public void setCanDismissItems(boolean canDismissItems) {
        this.canDismissItems = canDismissItems;
    }

    public void setShowSelected(boolean showSelected) {
        this.showSelected = showSelected;
        if (userListAdapter != null)
            userListAdapter.setShowSelected(showSelected);
    }

    public void setMemberList(List<Member> members) {
        if (userListAdapter != null) {
            userListAdapter.resetMembers(members);
        }
    }

    public void addMembers(List<Member> members) {
        if (userListAdapter != null) {
            userListAdapter.addMembers(members);
        }
    }

    public List<Member> getMemberList() {
        return this.userListAdapter.getMembers();
    }

    public interface MemberListListener {
        void onMemberListInitiated(MemberListFragment fragment);
    }

    public interface MemberListClickDismissListener {
        void onMemberDismissed(int position, String memberUid);
        void onMemberClicked(int position, String memberUid);
    }

    public void setID(String ID) { this.ID = ID; }
    public String getID() { return ID; }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        try {
            mListener = (MemberListListener) context;
        } catch (ClassCastException e) {
            // todo : do we really want to do this? not sure if listener should be compulsory, to reexamine
            throw new ClassCastException(context.toString() + " must implement onMemberListListener");
        }

        clickDismissListener = (context instanceof MemberListClickDismissListener) ? (MemberListClickDismissListener) context : null;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
        mListener.onMemberListInitiated(this);
    }

    private void init() {
        if (userListAdapter == null) {
            userListAdapter = new UserListAdapter(this.getContext());
        }
        if (groupUid != null) {
            this.grassrootRestService = new GrassrootRestService(this.getContext());
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.e(TAG, "setting up the fragment!");
        View viewToReturn = inflater.inflate(R.layout.fragment_member_list, container, false);
        ButterKnife.bind(this, viewToReturn);
        setUpRecyclerView();
        this.vgContainer = container;
        return viewToReturn;
    }

    private void setUpRecyclerView() {
        Log.e(TAG, "setting up the recycler view!");
        memberListRecyclerView.setLayoutManager(new LinearLayoutManager(this.getContext()));
        memberListRecyclerView.setAdapter(userListAdapter);
        userListAdapter.setShowSelected(showSelected);
        if (groupUid != null)
            retrieveGroupMembers();
        if (canDismissItems)
            setUpDismissal();
        memberListRecyclerView.setVisibility(View.VISIBLE);
        Log.e(TAG, "ZOG : set up view, adaptor has : " + userListAdapter.getItemCount() + " items");
        // todo: set this up (also, have a dismiss swipe)
    }

    private void setUpDismissal() {
        if (clickDismissListener == null) {
            throw new UnsupportedOperationException("Selection and dismisal require calling activity to implement listener");
        }

        SwipeableRecyclerViewTouchListener swipeDeleteListener = new SwipeableRecyclerViewTouchListener(
                getContext(), memberListRecyclerView, R.id.mlist_tv_member_name, R.id.mlist_tv_member_name,
                new SwipeableRecyclerViewTouchListener.SwipeListener() {
                    @Override
                    public boolean canSwipe(int position) {
                        return true;
                    }

                    @Override
                    public void onDismissedBySwipe(RecyclerView recyclerView, int[] reverseSortedPositions) {
                        userListAdapter.removeMembers(reverseSortedPositions);
                        for (int i = 0; i < reverseSortedPositions.length; i++) {
                            clickDismissListener.onMemberDismissed(reverseSortedPositions[i],
                                    userListAdapter.getMemberUid(reverseSortedPositions[i]));
                        }
                    }
                });
        memberListRecyclerView.addOnItemTouchListener(swipeDeleteListener);

        if (showSelected) {
            memberListRecyclerView.addOnItemTouchListener(new RecyclerTouchListener(getContext(), memberListRecyclerView,
                    new ClickListener() {
                        @Override
                        public void onClick(View view, int position) {
                            userListAdapter.toggleMemberSelected(position);
                            clickDismissListener.onMemberClicked(position, userListAdapter.getMemberUid(position));
                        }

                        @Override
                        public void onLongClick(View view, int position) {
                            userListAdapter.toggleMemberSelected(position);
                            clickDismissListener.onMemberClicked(position, userListAdapter.getMemberUid(position));
                        }
            }));
        }
    }

    private void retrieveGroupMembers() {
        if (groupUid == null)
            throw new UnsupportedOperationException("Cannot retrieve group members from null group uid");

        String userPhoneNumber = SettingPreference.getuser_mobilenumber(this.getContext());
        String userSessionCode = SettingPreference.getuser_token(this.getContext());

        Log.d(TAG, "inside MemberListFragment, retrieving group members for uid = " + groupUid);

        grassrootRestService.getApi()
                .getGroupMembers(groupUid, userPhoneNumber, userSessionCode, selectedByDefault)
                .enqueue(new Callback<MemberList>() {
                    @Override
                    public void onResponse(Call<MemberList> call, Response<MemberList> response) {
                        if (response.isSuccessful()) {
                            List<Member> membersReturned = response.body().getMembers();
                            userListAdapter.addMembers(membersReturned);
                        } else {
                            // todo: handle error, via a dialog box
                        }
                    }

                    @Override
                    public void onFailure(Call<MemberList> call, Throwable t) {
                        ErrorUtils.handleNetworkError(getContext(), vgContainer, t); // todo : will snackbar show correctly??
                    }
                });
    }

}

