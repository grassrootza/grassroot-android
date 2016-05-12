package com.techmorphosis.grassroot.ui.fragments;

import android.app.DialogFragment;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;

import com.techmorphosis.grassroot.Interface.FilterInterface;
import com.techmorphosis.grassroot.R;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by ravi on 13/4/16.
 */
public class FilterFragment extends DialogFragment {

    @BindView(R.id.tv_Vote)
    TextView tvVote;
    @BindView(R.id.tv_meeting)
    TextView tvMeeting;
    @BindView(R.id.tv_todo)
    TextView tvtoDo;
    @BindView(R.id.tv_clear)
    TextView tvClear;
    private static final String TAG = "FilterFragment";
    public boolean vote = false, meeting = false, todo = false;
    private FilterInterface filterinterface;
    private boolean clear;


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_filter, container, false);
        ButterKnife.bind(this,view);
        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Bundle b = getArguments();
        if (b != null) {
            vote = b.getBoolean("Vote");
            meeting = b.getBoolean("Meeting");
            todo = b.getBoolean("ToDo");
            clear = b.getBoolean("Clear");

            Log.e(TAG, "vote is " + vote);
            Log.e(TAG, "meeting is " + meeting);
            Log.e(TAG, "todo is " + todo);
            Log.e(TAG, "clear is " + clear);

        } else {

        }

        initViews();
        updateui(vote, meeting, todo);

    }

    private void updateui(boolean vote, boolean meeting, boolean todo) {

        if (vote) {

            tvVote.setTypeface(null, Typeface.BOLD);

            tvVote.setTextColor(getResources().getColor(R.color.primaryColor));
            tvMeeting.setTextColor(getResources().getColor(R.color.grey));
            tvtoDo.setTextColor(getResources().getColor(R.color.grey));
            tvClear.setTextColor(getResources().getColor(R.color.grey));


        } else if (meeting) {
            tvVote.setTextColor(getResources().getColor(R.color.grey));
            tvMeeting.setTextColor(getResources().getColor(R.color.primaryColor));
            tvtoDo.setTextColor(getResources().getColor(R.color.grey));
            tvClear.setTextColor(getResources().getColor(R.color.grey));


            tvMeeting.setTypeface(null, Typeface.BOLD);

        } else if (todo) {
            tvtoDo.setTypeface(null, Typeface.BOLD);

            tvVote.setTextColor(getResources().getColor(R.color.grey));
            tvMeeting.setTextColor(getResources().getColor(R.color.grey));
            tvtoDo.setTextColor(getResources().getColor(R.color.primaryColor));
            tvClear.setTextColor(getResources().getColor(R.color.grey));


        } else if (clear) {
            tvClear.setTypeface(null, Typeface.BOLD);
            tvVote.setTextColor(getResources().getColor(R.color.grey));
            tvMeeting.setTextColor(getResources().getColor(R.color.grey));
            tvtoDo.setTextColor(getResources().getColor(R.color.grey));
            tvClear.setTextColor(getResources().getColor(R.color.primaryColor));


        }
    }

    private void initViews() {
        tvVote.setText("Vote");
        tvMeeting.setText("Meeting");
        tvtoDo.setText("ToDo");

    }

    @OnClick(R.id.tv_clear)
    public void clearClick() {


/*
                        vote = false;
                        meeting=false;
                        todo=false;
                        clear=true;

                        tvClear.setTypeface(null, Typeface.BOLD);


                        tvVote.setTextColor(getResources().getColor(R.color.grey));
                        tvMeeting.setTextColor(getResources().getColor(R.color.grey));
                        tvtoDo.setTextColor(getResources().getColor(R.color.grey));
                        tvClear.setTextColor(getResources().getColor(R.color.primaryColor));
*/


        // ((HomeGroupListFragment) getActivity()).tvVoteClick(date,role,defaults);
        filterinterface.clear(vote, meeting, todo, clear);
        getDialog().dismiss();


    }

    @OnClick(R.id.tv_Vote)
    public void tvVoteClick() {


/*
                vote = true;
                meeting=false;
                todo=false;                        clear=true;



                tvVote.setTypeface(null, Typeface.BOLD);


                tvVote.setTextColor(getResources().getColor(R.color.primaryColor));
                tvMeeting.setTextColor(getResources().getColor(R.color.grey));
                tvtoDo.setTextColor(getResources().getColor(R.color.grey));
*/

        // ((HomeGroupListFragment) getActivity()).tvVoteClick(date,role,defaults);
        filterinterface.vote(vote, meeting, todo, clear);

        getDialog().dismiss();

    }


    @OnClick(R.id.tv_meeting)
    public void meetingClick() {


/*
                vote = false;
                meeting=true;
                todo=false;
                clear=true;


                tvVote.setTextColor(getResources().getColor(R.color.grey));
                tvMeeting.setTextColor(getResources().getColor(R.color.primaryColor));
                tvtoDo.setTextColor(getResources().getColor(R.color.grey));

                tvMeeting.setTypeface(null, Typeface.BOLD);*/

        // ((HomeGroupListFragment) getActivity()).meetingClick(date, role, defaults);
        filterinterface.meeting(vote, meeting, todo, clear);
        getDialog().dismiss();

    }

    @OnClick(R.id.tv_todo)
    public void toDoClick() {


/*
                vote = false;
                meeting=false;
                todo=true;
                clear=true;


                tvtoDo.setTypeface(null, Typeface.BOLD);

                tvVote.setTextColor(getResources().getColor(R.color.grey));
                tvMeeting.setTextColor(getResources().getColor(R.color.grey));
                tvtoDo.setTextColor(getResources().getColor(R.color.primaryColor));
*/


        // ((HomeGroupListFragment) getActivity()).defaultsClick(date, role, defaults);
        filterinterface.todo(vote, meeting, todo, clear);
        getDialog().dismiss();


    }


    public void setListener(FilterInterface fragmentsCalllistner) {
        filterinterface = fragmentsCalllistner;
    }

}
