package org.grassroot.android.fragments;

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
import org.grassroot.android.models.Member;
import org.grassroot.android.models.MemberList;
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

    private String groupUid;
    private boolean canDismissItems;
    private boolean showSelected;
    private boolean selectedByDefault;

    private MemberListListener mListener;
    private MemberClickListener clickListener;
    private MemberListAdapter memberListAdapter;

    @BindView(R.id.mlist_frag_recycler_view)
    RecyclerView memberListRecyclerView;

    private ViewGroup vgContainer;

    public interface MemberListListener {
        void onMemberListInitiated(MemberListFragment fragment);
        void onMemberListPopulated(List<Member> memberList);
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
        if (memberListAdapter != null)
            memberListAdapter.setShowSelected(showSelected);
    }

    public void addMembers(List<Member> members) {
        if (memberListAdapter != null) {
            memberListAdapter.addMembers(members);
        }
    }

    public void removeMember(Member member) {
        if (memberListAdapter != null) {
            memberListAdapter.removeMember(member);
        }
    }

    public void removeMembers(List<Member> members) {
        if (memberListAdapter != null) {
            memberListAdapter.removeMembers(members);
        }
    }

    public List<Member> getMemberList() {
        return this.memberListAdapter.getMembers();
    }

    public List<Member> getSelectedMembers() {
        // Java 8 Lambdas would be really nice here ...
        if (!showSelected) {
            return memberListAdapter.getMembers();
        } else {
            List<Member> membersToReturn = new ArrayList<>();
            for (Member m : memberListAdapter.getMembers()) {
                if (m.isSelected()) membersToReturn.add(m);
            }
            return membersToReturn;
        }
    }

    public void selectAllMembers() {
        for (Member m : memberListAdapter.getMembers()) {
            m.setSelected(true);
        }
        memberListAdapter.notifyDataSetChanged(); // todo : as elsewhere, use more efficient method
    }

    public void unselectAllMembers() {
        for (Member m : memberListAdapter.getMembers()) {
            m.setSelected(false);
        }
        memberListAdapter.notifyDataSetChanged();
    }


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
        if (memberListAdapter == null) {
            memberListAdapter = new MemberListAdapter(this.getContext());
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
        memberListRecyclerView.setAdapter(memberListAdapter);
        memberListAdapter.setShowSelected(showSelected);
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
                clickListener.onMemberDismissed(swipedPosition, memberListAdapter.getMemberUid(swipedPosition));
                memberListAdapter.removeMembers(new int[]{swipedPosition});
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
                        memberListAdapter.toggleMemberSelected(position);
                        clickListener.onMemberClicked(position, memberListAdapter.getMemberUid(position));
                    }

                    @Override
                    public void onLongClick(View view, int position) {
                        memberListAdapter.toggleMemberSelected(position);
                        clickListener.onMemberClicked(position, memberListAdapter.getMemberUid(position));
                    }
        }));
    }

    private void fetchGroupMembers() {
        if (groupUid == null)
            throw new UnsupportedOperationException("Cannot retrieve group members from null group uid");

        String userPhoneNumber = PreferenceUtils.getUserPhoneNumber(this.getContext());
        String userSessionCode = PreferenceUtils.getAuthToken(this.getContext());

        Log.d(TAG, "inside MemberListFragment, retrieving group members for uid = " + groupUid);

        GrassrootRestService.getInstance().getApi()
                .getGroupMembers(groupUid, userPhoneNumber, userSessionCode, selectedByDefault)
                .enqueue(new Callback<MemberList>() {
                    @Override
                    public void onResponse(Call<MemberList> call, Response<MemberList> response) {
                        if (response.isSuccessful()) {
                            final List<Member> membersReturned = response.body().getMembers();
                            memberListAdapter.addMembers(membersReturned);
                            mListener.onMemberListPopulated(membersReturned);
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

