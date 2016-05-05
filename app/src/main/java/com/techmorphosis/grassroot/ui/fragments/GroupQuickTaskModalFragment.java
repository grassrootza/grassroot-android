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
import com.techmorphosis.grassroot.ui.activities.Blank;


public class GroupQuickTaskModalFragment extends android.support.v4.app.DialogFragment {

    private static final String TAG = GroupQuickTaskModalFragment.class.getSimpleName();

    private View view;
    private ImageView icHomeVoteActive;
    private ImageView icHomeCallMeetingActive;
    private ImageView icHomeToDoActive;
    public boolean votePermitted=false,meetingPermitted=false,todoPermitted=false;

    // would rather use good practice and not have empty constructor, but Android is Android
    public GroupQuickTaskModalFragment() { }

    @Override
    public void onStart() {
        super.onStart();
        getDialog().getWindow().setWindowAnimations(R.style.animation_fast_flyinout);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view =inflater.inflate(R.layout.modal_group_tasks_quick, container, false);
        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(0));
        return view;
    }

    private void findAndSetUpView() {

        icHomeVoteActive = (ImageView) view.findViewById(R.id.ic_home_vote_active);
        icHomeCallMeetingActive = (ImageView) view.findViewById(R.id.ic_home_call_meeting_active);
        icHomeToDoActive = (ImageView) view.findViewById(R.id.ic_home_to_do_active);

        icHomeCallMeetingActive.setOnClickListener(icHomeCallMeetingActive());
        icHomeVoteActive.setOnClickListener(icHomeVoteActive());
        icHomeToDoActive.setOnClickListener(icHomeToDoActive());

    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        Bundle b = getArguments();
        if (b == null) { throw new UnsupportedOperationException("Error! Null arguments passed to modal"); }

        Log.d(TAG, "inside quickTaskModal, passed bundle = " + b.toString());

        findAndSetUpView();
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

    private View.OnClickListener icHomeToDoActive() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (todoPermitted) {
                    Intent ToDo= new Intent(getActivity(), Blank.class);
                    ToDo.putExtra("title","ToDo");
                    startActivity(ToDo);
                    getDialog().dismiss();
                } else {
                    getDialog().dismiss();
                }
            }
        };
    }

    private View.OnClickListener icHomeVoteActive() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (votePermitted) {
                    Intent Vote= new Intent(getActivity(), Blank.class);
                    Vote.putExtra("title","Vote");
                    startActivity(Vote);
                    getDialog().dismiss();
                } else {
                    getDialog().dismiss();
                }
            }
        };
    }

    private View.OnClickListener icHomeCallMeetingActive() {
                return  new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (meetingPermitted) {
                            Intent Meeting= new Intent(getActivity(), Blank.class);
                            Meeting.putExtra("title","Meeting");
                            startActivity(Meeting);
                            getDialog().dismiss();
                        } else {
                            getDialog().dismiss();
                        }
                    }
                };
    }


}