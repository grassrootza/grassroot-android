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
import com.techmorphosis.grassroot.services.GrassrootRestService;
import com.techmorphosis.grassroot.services.model.Member;
import com.techmorphosis.grassroot.services.model.MemberList;
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

    MemberListListener mListener;
    private String groupUid;
    private GrassrootRestService grassrootRestService;
    private UserListAdapter userListAdapter;

    @BindView(R.id.mlist_frag_recycler_view)
    RecyclerView memberListRecyclerView;

    private ViewGroup vgContainer;

    // NB: we are not doing checks for null on this because we will use the fragment in create group
    public void setGroupUid(String groupUid) {
        this.groupUid = groupUid;
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
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.e(TAG, "listFragment! Inside onCreate");
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
        if (groupUid != null)
            retrieveGroupMembers();
        memberListRecyclerView.setVisibility(View.VISIBLE);
        Log.e(TAG, "ZOG : set up view, adaptor has : " + userListAdapter.getItemCount() + " items");

        // todo: set this up (also, have a dismiss swipe)
        /*this.mRecyclerView.addOnItemTouchListener(new RecyclerTouchListener(this, this.mRecyclerView, new ClickListener() {
            @Override
            public void onClick(View view, int position) {
                // todo: integrate selected / not selected back into adapter
                Log.e(TAG, "position is  " + position);
                Contact clickedContact = CreateGroupActivity.this.mergeList.get(position);
                if (clickedContact == null) {
                    Log.e(CreateGroupActivity.this.TAG, "click_model  is  " + null);
                } else {
                    Log.e(CreateGroupActivity.this.TAG, "click_model  is not null ");
                }
                if (clickedContact.isSelected) {
                    clickedContact.isSelected = false;
                    memberAdapter.notifyDataSetChanged();
                    return;
                }
                clickedContact.isSelected = true;
                CreateGroupActivity.this.mAdapter.notifyDataSetChanged();
            }

            @Override
            public void onLongClick(View view, int position) {

            }
        }));*/
    }

    @Override
    public void onPause() {
        // todo: persist this list of members temporarily, to cut down on redundant calls
        super.onPause();
    }

    private void retrieveGroupMembers() {
        if (groupUid == null)
            throw new UnsupportedOperationException("Cannot retrieve group members from null group uid");

        String userPhoneNumber = SettingPreference.getuser_mobilenumber(this.getContext());
        String userSessionCode = SettingPreference.getuser_token(this.getContext());

        Log.d(TAG, "inside MemberListFragment, retrieving group members for uid = " + groupUid);

        grassrootRestService.getApi()
                .getGroupMembers(groupUid, userPhoneNumber, userSessionCode)
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
