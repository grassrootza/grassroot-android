package org.grassroot.android.fragments;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;

import org.grassroot.android.R;
import org.grassroot.android.activities.AddMembersActivity;
import org.grassroot.android.activities.CreateMeetingActivity;
import org.grassroot.android.activities.CreateTodoActivity;
import org.grassroot.android.activities.CreateVoteActivity;
import org.grassroot.android.interfaces.GroupConstants;
import org.grassroot.android.models.Group;
import org.grassroot.android.utils.ErrorUtils;
import org.grassroot.android.utils.IntentUtils;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;

/*
 NB : this presumes prior calling activities have done proper checks that at least one permission is present
 using group.hasCreatePermission. If none are present, and it is called, user will just see a green screen
 */

public class NewTaskMenuFragment extends Fragment {

    private static final String TAG = NewTaskMenuFragment.class.getCanonicalName();

    // switch to Rx subscriber pattern soon
    public interface NewTaskMenuListener {
        void menuCloseClicked();
    }

    Unbinder unbinder;
    @BindView(R.id.iv_back) ImageView ivBack;
    @BindView(R.id.bt_vote) Button bt_vote;
    @BindView(R.id.bt_meeting) Button bt_meeting;
    @BindView(R.id.bt_todo) Button bt_todo;
    @BindView(R.id.bt_newmember) Button bt_addmember;

    private NewTaskMenuListener listener;

    private Group groupMembership;
    private String groupUid;
    private String groupName;

    private boolean showAddMembers;

    public static NewTaskMenuFragment newInstance(Group groupMembership, boolean showAddMembers) {
        NewTaskMenuFragment fragment = new NewTaskMenuFragment();
        fragment.showAddMembers = showAddMembers;
        Bundle b = new Bundle();
        b.putParcelable(GroupConstants.OBJECT_FIELD, groupMembership);
        fragment.setArguments(b);
        return fragment;
    }

    public void setShowAddMembers(boolean showAddMembers) {
        this.showAddMembers = showAddMembers;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            this.listener = (NewTaskMenuListener) context;
        } catch (ClassCastException e) {
            throw new UnsupportedOperationException("Error! New task menu needs a listener");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle b = getArguments();

        if (b == null) {
            throw new UnsupportedOperationException("Error! Null arguments passed to modal");
        }

        this.groupMembership = b.getParcelable(GroupConstants.OBJECT_FIELD);
        if (groupMembership == null) {
            Log.e(TAG, "Error! New task called without valid group");
            ErrorUtils.gracefulExitToHome(getActivity());
        }

        groupUid = groupMembership.getGroupUid();
        groupName = groupMembership.getGroupName();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstance) {
        View viewToReturn = inflater.inflate(R.layout.fragment_new_task_menu, container, false);
        unbinder = ButterKnife.bind(this, viewToReturn);
        return viewToReturn;
    }

    @Override
    public void onResume() {
        super.onResume();
        setVisibility();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }

    private void setVisibility() {
        bt_meeting.setVisibility(groupMembership.canCallMeeting() ? View.VISIBLE : View.GONE);
        bt_vote.setVisibility(groupMembership.canCallVote() ? View.VISIBLE : View.GONE);
        bt_todo.setVisibility(groupMembership.canCreateTodo() ? View.VISIBLE : View.GONE);
        bt_addmember.setVisibility((groupMembership.canAddMembers() && showAddMembers) ? View.VISIBLE : View.GONE);
    }

    @OnClick(R.id.iv_back)
    public void onBackClick() {
        listener.menuCloseClicked();
    }

    @OnClick(R.id.bt_todo)
    public void onTodoButtonClick() {
        Intent todo = IntentUtils.constructIntent(getActivity(), CreateTodoActivity.class, groupUid, groupName, groupMembership.getIsLocal());
        startActivity(todo);
    }

    @OnClick(R.id.bt_meeting)
    public void onMeetingButtonClick() {
        Intent createMeeting = IntentUtils.constructIntent(getActivity(), CreateMeetingActivity.class, groupUid, groupName, groupMembership.getIsLocal());
        startActivity(createMeeting);
    }

    @OnClick(R.id.bt_vote)
    public void onVoteButtonClick() {
        Intent createVote = IntentUtils.constructIntent(getActivity(), CreateVoteActivity.class, groupUid, groupName, groupMembership.getIsLocal());
        createVote.putExtra("title", "Vote");
        startActivity(createVote);
    }

    @OnClick(R.id.bt_newmember)
    public void onNewMemberButtonClick() {
        // activity just uses finish, so will return to whatever activity called this fragment, as long as it manages fragment stack properly
        Intent addMembers = new Intent(getActivity(), AddMembersActivity.class);
        addMembers.putExtra(GroupConstants.UID_FIELD, groupUid);
        addMembers.putExtra(GroupConstants.NAME_FIELD, groupName);
        startActivity(addMembers);
    }

}
