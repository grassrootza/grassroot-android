package org.grassroot.android.fragments;


import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;

import org.grassroot.android.R;
import org.grassroot.android.interfaces.TaskConstants;
import org.grassroot.android.models.Group;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;


public class QuickTaskModalFragment extends android.support.v4.app.DialogFragment {

    private static final String TAG = QuickTaskModalFragment.class.getSimpleName();

    public interface TaskModalListener {
        void onTaskClicked(String taskType);
    }

    private TaskModalListener listener;
    private Unbinder unbinder;

    private boolean groupSelected;
    private Group group;

    @BindView(R.id.ic_home_vote_active)
    ImageView icHomeVoteActive;
    @BindView(R.id.ic_home_call_meeting_active)
    ImageView icHomeCallMeetingActive;
    @BindView(R.id.ic_home_to_do_active)
    ImageView icHomeToDoActive;


    public boolean votePermitted = false, meetingPermitted = false, todoPermitted = false;

    public QuickTaskModalFragment() {}

    public static QuickTaskModalFragment newInstance(boolean groupSelected, final Group group, TaskModalListener listener) {
        QuickTaskModalFragment fragment = new QuickTaskModalFragment();
        fragment.groupSelected = groupSelected;
        fragment.listener = listener;
        if (groupSelected && group == null) {
            throw new UnsupportedOperationException("Error! Task modal instantiated with group selected true but null group");
        }
        fragment.group = groupSelected ? group : null;
        return fragment;
    }

    @Override
    public void onStart() {
        super.onStart();
        getDialog().getWindow().setWindowAnimations(R.style.animation_fast_flyinout);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.modal_group_tasks_quick, container, false);
        unbinder = ButterKnife.bind(this, view);
        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(0));
        return view;
    }


    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        meetingPermitted = !groupSelected || group.canCallMeeting();
        votePermitted = !groupSelected || group.canCallVote();
        todoPermitted = !groupSelected || group.canCreateTodo();

        int mtgIcon = meetingPermitted ? R.drawable.ic_home_call_meeting_active : R.drawable.ic_home_call_meeting_inactive;
        int voteIcon = votePermitted ? R.drawable.ic_home_vote_active : R.drawable.ic_home_vote_inactive;
        int todoIcon = todoPermitted ? R.drawable.ic_home_to_do_active : R.drawable.ic_home_to_do_inactive;

        icHomeCallMeetingActive.setImageResource(mtgIcon);
        icHomeVoteActive.setImageResource(voteIcon);
        icHomeToDoActive.setImageResource(todoIcon);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }

    @OnClick(R.id.ic_home_to_do_active)
    public void icHomeToDoActive() {
        getDialog().dismiss();
        if (todoPermitted) {
            listener.onTaskClicked(TaskConstants.TODO);
        }
    }

    @OnClick(R.id.ic_home_vote_active)
    public void icHomeVoteActive() {
        getDialog().dismiss();
        if (votePermitted) {
            listener.onTaskClicked(TaskConstants.VOTE);
        }
    }

    @OnClick(R.id.ic_home_call_meeting_active)
    public void icHomeCallMeetingActive() {
        getDialog().dismiss();
        if (meetingPermitted) {
            listener.onTaskClicked(TaskConstants.MEETING);
        }
    }
}