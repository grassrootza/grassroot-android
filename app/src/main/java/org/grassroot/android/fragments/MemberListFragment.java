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

import io.realm.RealmResults;
import org.grassroot.android.R;
import org.grassroot.android.adapters.MemberListAdapter;
import org.grassroot.android.adapters.RecyclerTouchListener;
import org.grassroot.android.interfaces.ClickListener;
import org.grassroot.android.models.Group;
import org.grassroot.android.models.Member;
import org.grassroot.android.utils.RealmUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.realm.RealmList;
import rx.functions.Action1;

/**
 * Created by luke on 2016/05/08.
 */
public class MemberListFragment extends Fragment {

    private static final String TAG = MemberListFragment.class.getCanonicalName();

    private Group group;

    private boolean canClickItems;
    private boolean canDismissItems;
    private boolean showSelected;
    private boolean selectedByDefault;

    private List<Member> preSelectedMembers;
    private List<Member> filteredMembers;

    private MemberClickListener clickListener;
    private MemberListAdapter memberListAdapter;

    RecyclerView memberListRecyclerView;

    public interface MemberClickListener {
        void onMemberDismissed(int position, String memberUid);
        void onMemberClicked(int position, String memberUid);
    }

    // note : groupUid can be set null, in which case we are adding members generated locally
    public static MemberListFragment newInstance(String parentUid, boolean clickEnabled, boolean showSelected,
                                                 boolean canDismissItems, List<Member> selectedMembers, MemberClickListener clickListener) {
        MemberListFragment fragment = new MemberListFragment();
        if (parentUid != null) {
            fragment.group = RealmUtils.loadGroupFromDB(parentUid);
        } else {
            fragment.group = null;
        }
        fragment.canClickItems = clickEnabled;
        fragment.showSelected = showSelected;
        fragment.canDismissItems = canDismissItems;
        fragment.clickListener = clickListener;
        fragment.preSelectedMembers = selectedMembers;
        return fragment;
    }

    public static MemberListFragment newInstance(Group group, boolean showSelected, List<Member> filteredMembers,
                                                 MemberClickListener clickListener) {
        MemberListFragment fragment = new MemberListFragment();
        fragment.group = group;
        fragment.canClickItems = true;
        fragment.showSelected = showSelected;
        fragment.clickListener = clickListener;
        fragment.filteredMembers = filteredMembers;
        return fragment;
    }

    public void setSelectedByDefault(boolean selectedByDefault) {
        this.selectedByDefault = selectedByDefault;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (memberListAdapter == null) {
            memberListAdapter = new MemberListAdapter(this.getContext());
        };
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View viewToReturn = inflater.inflate(R.layout.fragment_member_list, container, false);
        memberListRecyclerView = (RecyclerView) viewToReturn.findViewById(R.id.mlist_frag_recycler_view);
        setUpRecyclerView();
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

    public void updateMember(int position, Member revisedMember) {
        if (memberListAdapter != null) {
            memberListAdapter.updateMember(position, revisedMember); // todo : rethink all this pass-through stuff
        }
    }

    public void removeMember(int position) {
        if (memberListAdapter != null) {
            memberListAdapter.removeMembers(new int[] { position });
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

        if (group != null)
            fetchGroupMembers();
        if (canDismissItems)
            setUpDismissal();
        if (canClickItems)
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
        Log.d(TAG, "inside MemberListFragment, retrieving group members for uid = " + group.getGroupUid());
        RealmUtils.loadListFromDB(Member.class,"groupUid", group.getGroupUid()).subscribe(new Action1<RealmResults>() {
            @Override public void call(RealmResults realmResults) {
                List<Member> membersToRemove = new ArrayList<>(memberListAdapter.getMembers());
                if (filteredMembers != null) {
                    membersToRemove.addAll(filteredMembers);
                }
                realmResults.removeAll(membersToRemove);
                memberListAdapter.addMembers(realmResults);

                if (preSelectedMembers != null && !selectedByDefault) {
                    // todo : consider using list.contains on members when can trust hashing/equals
                    final Map<String, Integer> positionMap = new HashMap<>();
                    final int listSize = preSelectedMembers.size();
                    for (int i = 0; i < listSize; i++) {
                        positionMap.put(((Member)realmResults.get(i)).getMemberUid(),i);
                    }
                    for (Member m : preSelectedMembers) {
                        if (positionMap.containsKey(m.getMemberUid())) {
                            memberListAdapter.toggleMemberSelected(positionMap.get(m.getMemberUid()));
                        }
                    }
                }
            }
        });

    }

}