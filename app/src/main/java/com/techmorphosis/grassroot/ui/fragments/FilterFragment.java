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

/**
 * Created by ravi on 13/4/16.
 */
public class FilterFragment extends DialogFragment
{

    View view;
    private TextView tvVote;
    private TextView tvMeeting;
    private TextView tvtoDo;
    private static final String TAG = "FilterFragment";
    public boolean vote=false,meeting=false,todo=false;
    private FilterInterface filterinterface;
    private TextView tvClear;
    private boolean clear;


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
         //super.onCreateView(inflater, container, savedInstanceState);
        view=inflater.inflate(R.layout.fragment_filter,container,false);
        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        Bundle b= getArguments();
        if (b!=null)
        {
            vote= b.getBoolean("Vote");
            meeting=b.getBoolean("Meeting");
        todo=b.getBoolean("ToDo");
            clear=b.getBoolean("Clear");

            Log.e(TAG, "vote is " + vote);
            Log.e(TAG, "meeting is " + meeting);
            Log.e(TAG, "todo is " + todo);
            Log.e(TAG, "clear is " + clear);

        }
        else
        {

        }

        findAllViews();
        updateui(vote, meeting, todo);

    }

    private void updateui(boolean vote, boolean meeting, boolean todo)
    {

        if (vote)
        {

            tvVote.setTypeface(null, Typeface.BOLD);

            tvVote.setTextColor(getResources().getColor(R.color.primaryColor));
            tvMeeting.setTextColor(getResources().getColor(R.color.grey));
            tvtoDo.setTextColor(getResources().getColor(R.color.grey));
            tvClear.setTextColor(getResources().getColor(R.color.grey));


        }
        else if (meeting)
        {
            tvVote.setTextColor(getResources().getColor(R.color.grey));
            tvMeeting.setTextColor(getResources().getColor(R.color.primaryColor));
            tvtoDo.setTextColor(getResources().getColor(R.color.grey));
            tvClear.setTextColor(getResources().getColor(R.color.grey));


            tvMeeting.setTypeface(null, Typeface.BOLD);

        }
        else if (todo)
        {
            tvtoDo.setTypeface(null, Typeface.BOLD);

            tvVote.setTextColor(getResources().getColor(R.color.grey));
            tvMeeting.setTextColor(getResources().getColor(R.color.grey));
            tvtoDo.setTextColor(getResources().getColor(R.color.primaryColor));
            tvClear.setTextColor(getResources().getColor(R.color.grey));


        } else if (clear)
        {
            tvClear.setTypeface(null, Typeface.BOLD);
            tvVote.setTextColor(getResources().getColor(R.color.grey));
            tvMeeting.setTextColor(getResources().getColor(R.color.grey));
            tvtoDo.setTextColor(getResources().getColor(R.color.grey));
            tvClear.setTextColor(getResources().getColor(R.color.primaryColor));


        }
    }

    private void findAllViews() {

        tvVote = (TextView) view.findViewById(R.id.tv_Vote);
        tvMeeting = (TextView) view.findViewById(R.id.tv_meeting);
        tvtoDo = (TextView) view.findViewById(R.id.tv_todo);
        tvClear = (TextView) view.findViewById(R.id.tv_clear);
        
        tvVote.setText("Vote");
        tvMeeting.setText("Meeting");
        tvtoDo.setText("ToDo");

        tvVote.setOnClickListener(tvVoteClick());
        tvMeeting.setOnClickListener(meetingClick());
        tvtoDo.setOnClickListener(toDoClick());
        tvClear.setOnClickListener(clearClick());

    }

    private View.OnClickListener clearClick() {
                return new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

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


                        // ((Group_Homepage) getActivity()).tvVoteClick(date,role,defaults);
                        filterinterface.clear(vote, meeting, todo,clear);

                        getDialog().dismiss();
                    }
                };
    }

    private View.OnClickListener tvVoteClick() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {

/*
                vote = true;
                meeting=false;
                todo=false;                        clear=true;



                tvVote.setTypeface(null, Typeface.BOLD);


                tvVote.setTextColor(getResources().getColor(R.color.primaryColor));
                tvMeeting.setTextColor(getResources().getColor(R.color.grey));
                tvtoDo.setTextColor(getResources().getColor(R.color.grey));
*/

               // ((Group_Homepage) getActivity()).tvVoteClick(date,role,defaults);
                filterinterface.vote(vote, meeting, todo,clear);

                getDialog().dismiss();
            }
        };
    }

    private View.OnClickListener meetingClick() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {

/*
                vote = false;
                meeting=true;
                todo=false;
                clear=true;


                tvVote.setTextColor(getResources().getColor(R.color.grey));
                tvMeeting.setTextColor(getResources().getColor(R.color.primaryColor));
                tvtoDo.setTextColor(getResources().getColor(R.color.grey));

                tvMeeting.setTypeface(null, Typeface.BOLD);*/

               // ((Group_Homepage) getActivity()).meetingClick(date, role, defaults);
                filterinterface.meeting(vote, meeting, todo,clear);
                getDialog().dismiss();
            }
        };
    }

    private View.OnClickListener toDoClick() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {

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


               // ((Group_Homepage) getActivity()).defaultsClick(date, role, defaults);
                filterinterface.todo(vote, meeting, todo,clear);
                getDialog().dismiss();
            }
        };
    }


    public void setListener(FilterInterface fragmentsCalllistner)
    {
        filterinterface = fragmentsCalllistner;
    }

}
