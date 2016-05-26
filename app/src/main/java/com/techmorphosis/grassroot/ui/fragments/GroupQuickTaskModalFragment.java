package com.techmorphosis.grassroot.ui.fragments;


import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;

import com.techmorphosis.grassroot.R;
import com.techmorphosis.grassroot.ui.activities.CreateMeetingActivity;
import com.techmorphosis.grassroot.ui.activities.CreateVoteActivity;
import com.techmorphosis.grassroot.ui.activities.NotBuiltActivity;
import com.techmorphosis.grassroot.utils.Constant;
import com.techmorphosis.grassroot.utils.MenuUtils;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;


public class GroupQuickTaskModalFragment extends android.support.v4.app.DialogFragment {

    private static final String TAG = GroupQuickTaskModalFragment.class.getSimpleName();

    private String groupUid;
    private String groupName;

    @BindView(R.id.ic_home_vote_active)
    ImageView icHomeVoteActive;
    @BindView(R.id.ic_home_call_meeting_active)
    ImageView icHomeCallMeetingActive;
    @BindView(R.id.ic_home_to_do_active)
    ImageView icHomeToDoActive;

    public boolean votePermitted = false, meetingPermitted = false, todoPermitted = false;

    // would rather use good practice and not have empty constructor, but Android is Android
    public GroupQuickTaskModalFragment() {
    }

    public void setGroupParameters(String groupUid, String groupName) {
        this.groupUid = groupUid;
        this.groupName = groupName;
    }

    @Override
    public void onStart() {
        super.onStart();
        getDialog().getWindow().setWindowAnimations(R.style.animation_fast_flyinout);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.modal_group_tasks_quick, container, false);
        ButterKnife.bind(this, view);
        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(0));
        return view;
    }


    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        Bundle b = getArguments();
        if (b == null) {
            throw new UnsupportedOperationException("Error! Null arguments passed to modal");
        }

        Log.d(TAG, "inside quickTaskModal, passed bundle = " + b.toString());
        meetingPermitted = b.getBoolean("Meeting");
        votePermitted = b.getBoolean("Vote");
        todoPermitted = b.getBoolean("ToDo");

        int mtgIcon = meetingPermitted ? R.drawable.ic_home_call_meeting_active : R.drawable.ic_home_call_meeting_inactive;
        int voteIcon = votePermitted ? R.drawable.ic_home_vote_active : R.drawable.ic_home_vote_inactive;
        int todoIcon = todoPermitted ? R.drawable.ic_home_to_do_active : R.drawable.ic_home_to_do_inactive;

        icHomeCallMeetingActive.setImageResource(mtgIcon);
        icHomeVoteActive.setImageResource(voteIcon);
        icHomeToDoActive.setImageResource(todoIcon);
    }

    @OnClick(R.id.ic_home_to_do_active)
    public void icHomeToDoActive() {
        if (todoPermitted) {
            Intent ToDo = new Intent(getActivity(), NotBuiltActivity.class);
            ToDo.putExtra("title", "ToDo");
            startActivity(ToDo);
            getDialog().dismiss();
        } else {
            getDialog().dismiss();
        }

    }

    @OnClick(R.id.ic_home_vote_active)
    public void icHomeVoteActive() {
        if (votePermitted) {
            Intent Vote = MenuUtils.constructIntent(getContext(), CreateVoteActivity.class, groupUid, groupName);
            Vote.putExtra("title", "Vote");
            startActivity(Vote);
            getDialog().dismiss();
        } else {
            getDialog().dismiss();
        }

    }

    @OnClick(R.id.ic_home_call_meeting_active)
    public void icHomeCallMeetingActive() {
        if (meetingPermitted) {
            Log.e(TAG, "about to start meeting for result!");
            Intent Meeting = MenuUtils.constructIntent(getContext(), CreateMeetingActivity.class, groupUid, groupName);
            getActivity().startActivityForResult(Meeting, Constant.activityCallMeeting);
            getDialog().dismiss();
        } else {
            getDialog().dismiss();
        }

    }


}