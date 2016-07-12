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

import io.realm.RealmList;
import org.grassroot.android.R;
import org.grassroot.android.adapters.MemberListAdapter;
import org.grassroot.android.interfaces.ClickListener;
import org.grassroot.android.services.GrassrootRestService;
import org.grassroot.android.models.Member;
import org.grassroot.android.models.MemberList;
import org.grassroot.android.adapters.RecyclerTouchListener;
import org.grassroot.android.utils.ErrorUtils;
import org.grassroot.android.utils.PreferenceUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import org.grassroot.android.utils.RealmUtils;
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
    private List<Member> preSelectedMembers;

    private MemberListListener mListener;
    private MemberClickListener clickListener;
    private MemberListAdapter memberListAdapter;

    @BindView(R.id.mlist_frag_recycler_view)
    RecyclerView memberListRecyclerView;

    private ViewGroup vgContainer;

    // todo : clean this up (probably don't need)
    public interface MemberListListener {
        void onMemberListInitiated(MemberListFragment fragment);
        void onMemberListPopulated(List<Member> memberList);
        void onMemberListDone();
    }

    public interface MemberClickListener {
        void onMemberDismissed(int position, String memberUid);
        void onMemberClicked(int position, String memberUid);
    }

    // note : groupUid can be set null, in which case we are adding members generated locally
    public static MemberListFragment newInstance(String parentUid, boolean showSelected, boolean canDismissItems,
                                                 MemberListListener listListener, MemberClickListener clickListener,
                                                 List<Member> selectedMembers) {
        MemberListFragment fragment = new MemberListFragment();
        fragment.groupUid = parentUid;
        fragment.showSelected = showSelected;
        fragment.canDismissItems = canDismissItems;
        fragment.mListener = listListener;
        fragment.clickListener = clickListener;
        fragment.preSelectedMembers = selectedMembers;
        return fragment;
    }

    public void setSelectedByDefault(boolean selectedByDefault) {
        this.selectedByDefault = selectedByDefault;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mListener = (context instanceof MemberListListener) ? (MemberListListener) context : null;
        clickListener = (context instanceof MemberClickListener) ? (MemberClickListener) context : null;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
        if (mListener != null) {
            mListener.onMemberListInitiated(this);
        }
    }

    private void init() {
        if (memberListAdapter == null) {
            memberListAdapter = new MemberListAdapter(this.getContext());
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.e(TAG, "setting up member list fragment view");
        View viewToReturn = inflater.inflate(R.layout.fragment_member_list, container, false);
        ButterKnife.bind(this, viewToReturn);
        setUpRecyclerView();
        this.vgContainer = container;
        return viewToReturn;
    }

    public void transitionToMemberList(List<Member> members) {
        // todo : optimize this (get difference), maybe
        if (memberListAdapter != null) {
            memberListAdapter.resetMembers(members);
        }
    }

    public void addMembers(List<Member> members) {
        if (memberListAdapter != null) {
            memberListAdapter.addMembers(members);
        }
    }

    public List<Member> getSelectedMembers() {
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
                if (clickListener != null) {
                    clickListener.onMemberDismissed(swipedPosition, memberListAdapter.getMemberUid(swipedPosition));
                }
                memberListAdapter.removeMembers(new int[]{swipedPosition});
            }
        };

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleCallback);
        itemTouchHelper.attachToRecyclerView(memberListRecyclerView);
    }

    private void setUpSelectionListener() {
        memberListRecyclerView.addOnItemTouchListener(new RecyclerTouchListener(getContext(), memberListRecyclerView,
                new ClickListener() {
                    @Override
                    public void onClick(View view, int position) {
                        memberListAdapter.toggleMemberSelected(position);
                        if (clickListener != null) {
                            clickListener.onMemberClicked(position, memberListAdapter.getMemberUid(position));
                        }
                    }

                    @Override
                    public void onLongClick(View view, int position) {
                        memberListAdapter.toggleMemberSelected(position);
                        if (clickListener != null) {
                            clickListener.onMemberClicked(position, memberListAdapter.getMemberUid(position));
                        }
                    }
        }));
    }

    private void fetchGroupMembers() {
        if (groupUid == null)
            throw new UnsupportedOperationException("Cannot retrieve group members from null group uid");
        RealmList<Member> members = RealmUtils.loadListFromDB(Member.class,"groupUid",groupUid);
        handleReturnedMembers(members);
        mListener.onMemberListPopulated(members);
        String userPhoneNumber = PreferenceUtils.getPhoneNumber();
        String userSessionCode = PreferenceUtils.getAuthToken();

        Log.d(TAG, "inside MemberListFragment, retrieving group members for uid = " + groupUid);

        //GrassrootRestService.getInstance().getApi()
        //        .getGroupMembers(groupUid, userPhoneNumber, userSessionCode, selectedByDefault)
        //        .enqueue(new Callback<MemberList>() {
        //            @Override
        //            public void onResponse(Call<MemberList> call, Response<MemberList> response) {
        //                if (response.isSuccessful()) {
        //                    final List<Member> membersReturned = response.body().getMembers();
        //                    handleReturnedMembers(membersReturned);
        //                    if (mListener != null) {
        //                        mListener.onMemberListPopulated(membersReturned);
        //                    }
        //                } else {
        //                    ErrorUtils.handleServerError(vgContainer, getActivity(), response);
        //                }
        //            }
        //
        //            @Override
        //            public void onFailure(Call<MemberList> call, Throwable t) {
        //                ErrorUtils.handleNetworkError(getContext(), vgContainer, t);
        //            }
        //        });
    }

    private void handleReturnedMembers(List<Member> membersReturned) {
        if(memberListAdapter.getMembers().isEmpty()) {
            memberListAdapter.addMembers(membersReturned);
        } else {
            membersReturned.removeAll(memberListAdapter.getMembers());
            memberListAdapter.addMembers(membersReturned);
        }

        if (preSelectedMembers != null && !selectedByDefault) {
            // todo : consider using list.contains on members when can trust hashing/equals
            final Map<String, Integer> positionMap = new HashMap<>();
            final int listSize = preSelectedMembers.size();
            for (int i = 0; i < listSize; i++) {
                positionMap.put(membersReturned.get(i).getMemberUid(),i);
            }
            for (Member m : preSelectedMembers) {
                if (positionMap.containsKey(m.getMemberUid())) {
                    memberListAdapter.toggleMemberSelected(positionMap.get(m.getMemberUid()));
                }
            }
        }
    }

}