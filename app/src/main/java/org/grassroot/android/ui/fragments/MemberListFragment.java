package org.grassroot.android.ui.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.grassroot.android.R;
import org.grassroot.android.adapters.MemberListAdapter;
import org.grassroot.android.interfaces.ClickListener;
import org.grassroot.android.services.GrassrootRestService;
import org.grassroot.android.services.model.Member;
import org.grassroot.android.services.model.MemberList;
import org.grassroot.android.ui.views.RecyclerTouchListener;
import org.grassroot.android.utils.ErrorUtils;
import org.grassroot.android.utils.PreferenceUtils;

import java.util.ArrayList;
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
    private MemberClickListener clickListener;
    private GrassrootRestService grassrootRestService;
    private MemberListAdapter userListAdapter;

    @BindView(R.id.mlist_frag_recycler_view)
    RecyclerView memberListRecyclerView;

    private ViewGroup vgContainer;

    public interface MemberListListener {
        void onMemberListInitiated(MemberListFragment fragment);
    }

    public interface MemberClickListener {
        void onMemberDismissed(int position, String memberUid);
        void onMemberClicked(int position, String memberUid);
    }

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

    public void addMembers(List<Member> members) {
        if (userListAdapter != null) {
            userListAdapter.addMembers(members);
        }
    }

    public void removeMember(Member member) {
        if (userListAdapter != null) {
            userListAdapter.removeMember(member);
        }
    }

    public void removeMembers(List<Member> members) {
        if (userListAdapter != null) {
            userListAdapter.removeMembers(members);
        }
    }

    public List<Member> getMemberList() {
        return this.userListAdapter.getMembers();
    }

    public List<Member> getSelectedMembers() {
        // Java 8 Lambdas would be really nice here ...
        if (!showSelected) {
            return userListAdapter.getMembers();
        } else {
            List<Member> membersToReturn = new ArrayList<>();
            for (Member m : userListAdapter.getMembers()) {
                if (m.isSelected()) membersToReturn.add(m);
            }
            return membersToReturn;
        }
    }

    public void selectAllMembers() {
        for (Member m : userListAdapter.getMembers()) {
            m.setSelected(true);
        }
        userListAdapter.notifyDataSetChanged(); // todo : as elsewhere, use more efficient method
    }

    public void unselectAllMembers() {
        for (Member m : userListAdapter.getMembers()) {
            m.setSelected(false);
        }
        userListAdapter.notifyDataSetChanged();
    }

    public void setID(String ID) { this.ID = ID; }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        try {
            mListener = (MemberListListener) context;
        } catch (ClassCastException e) {
            // todo : do we really want to do this? not sure if listener should be compulsory, to reexamine
            throw new ClassCastException(context.toString() + " must implement onMemberListListener");
        }
        clickListener = (context instanceof MemberClickListener) ? (MemberClickListener) context : null;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
        mListener.onMemberListInitiated(this);
    }

    private void init() {
        if (userListAdapter == null) {
            userListAdapter = new MemberListAdapter(this.getContext());
        }
        if (groupUid != null) {
            this.grassrootRestService = new GrassrootRestService(this.getContext());
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View viewToReturn = inflater.inflate(R.layout.fragment_member_list, container, false);
        ButterKnife.bind(this, viewToReturn);
        setUpRecyclerView();
        this.vgContainer = container;
        return viewToReturn;
    }

    private void setUpRecyclerView() {
        memberListRecyclerView.setLayoutManager(new LinearLayoutManager(this.getContext()));
        memberListRecyclerView.setAdapter(userListAdapter);
        userListAdapter.setShowSelected(showSelected);
        if (groupUid != null)
            fetchGroupMembers();
        if (canDismissItems)
            setUpDismissal();
        if (showSelected)
            setUpSelectionListener();
        memberListRecyclerView.setVisibility(View.VISIBLE);
    }

    private void setUpDismissal() {
        ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {

            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                int swipedPosition = viewHolder.getAdapterPosition();
                // todo: revisit whether we really need this
                clickListener.onMemberDismissed(swipedPosition, userListAdapter.getMemberUid(swipedPosition));
                userListAdapter.removeMembers(new int[]{swipedPosition});
            }
        };

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleCallback);
        itemTouchHelper.attachToRecyclerView(memberListRecyclerView);
    }

    private void setUpSelectionListener() {
        if (clickListener == null) {
            throw new UnsupportedOperationException("Selection and dismisal require calling activity to implement listener");
        }

        memberListRecyclerView.addOnItemTouchListener(new RecyclerTouchListener(getContext(), memberListRecyclerView,
                new ClickListener() {
                    @Override
                    public void onClick(View view, int position) {
                        userListAdapter.toggleMemberSelected(position);
                        clickListener.onMemberClicked(position, userListAdapter.getMemberUid(position));
                    }

                    @Override
                    public void onLongClick(View view, int position) {
                        userListAdapter.toggleMemberSelected(position);
                        clickListener.onMemberClicked(position, userListAdapter.getMemberUid(position));
                    }
        }));
    }

    private void fetchGroupMembers() {
        if (groupUid == null)
            throw new UnsupportedOperationException("Cannot retrieve group members from null group uid");

        String userPhoneNumber = PreferenceUtils.getuser_mobilenumber(this.getContext());
        String userSessionCode = PreferenceUtils.getuser_token(this.getContext());

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

