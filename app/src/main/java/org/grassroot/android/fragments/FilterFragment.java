package org.grassroot.android.fragments;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;

import org.grassroot.android.R;
import org.grassroot.android.interfaces.TaskConstants;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by ravi on 13/4/16.
 */
public class FilterFragment extends DialogFragment {

    private static final String TAG = FilterFragment.class.getSimpleName();

    private TasksFilterListener filterListener;

    @BindView(R.id.tv_Vote)
    TextView tvVote;
    @BindView(R.id.tv_meeting)
    TextView tvMeeting;
    @BindView(R.id.tv_todo)
    TextView tvtoDo;
    @BindView(R.id.tv_clear)
    TextView tvClear;

    public boolean filterVote = false, filterMeeting = false, filterTodo = false;

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
            filterVote = b.getBoolean(TaskConstants.VOTE);
            filterMeeting = b.getBoolean(TaskConstants.MEETING);
            filterTodo = b.getBoolean(TaskConstants.TODO);
        } else {
            filterVote = false;
            filterMeeting = false;
            filterTodo = false;
        }

        initViews();
        updateUi();
    }

    private void initViews() {
        tvVote.setText("Vote");
        tvMeeting.setText("Meeting");
        tvtoDo.setText("ToDo");
    }

    private void updateUi() {
        final Context context = getActivity();
        final int primaryColor = ContextCompat.getColor(context, R.color.primaryColor);
        final int greyColor = ContextCompat.getColor(context, R.color.grey);

        tvVote.setTextColor(filterVote ? primaryColor : greyColor);
        tvMeeting.setTextColor(filterMeeting ? primaryColor : greyColor);
        tvtoDo.setTextColor(filterTodo ? primaryColor : greyColor);

        tvVote.setTypeface(null, filterVote ? Typeface.BOLD : Typeface.NORMAL);
        tvMeeting.setTypeface(null, filterMeeting ? Typeface.BOLD : Typeface.NORMAL);
        tvtoDo.setTypeface(null, filterTodo ? Typeface.BOLD : Typeface.NORMAL);
    }

    private void callClickOrClear(String typeChanged, boolean changedFlagState) {
        // Log.e(TAG, "call click or clear : mtg = " + filterMeeting + ", ")
        if (filterMeeting || filterVote || filterTodo) {
           filterListener.itemClicked(typeChanged, changedFlagState);
        } else {
            filterListener.clearFilters();
        }
    }

    @OnClick(R.id.tv_clear)
    public void clearClick() {
        filterListener.clearFilters();
        getDialog().dismiss();
    }

    @OnClick(R.id.tv_Vote)
    public void tvVoteClick() {
        filterVote = !filterVote;
        callClickOrClear(TaskConstants.VOTE, filterVote);
        getDialog().dismiss();
    }


    @OnClick(R.id.tv_meeting)
    public void meetingClick() {
        filterMeeting = !filterMeeting;
        callClickOrClear(TaskConstants.MEETING, filterMeeting);
        getDialog().dismiss();
    }

    @OnClick(R.id.tv_todo)
    public void toDoClick() {
        filterTodo = !filterTodo;
        callClickOrClear(TaskConstants.TODO, filterTodo);
        getDialog().dismiss();
    }

    public interface TasksFilterListener {
        void itemClicked(String typeChanged, boolean changedFlagState);
        void clearFilters();
    }

    public void setListener(TasksFilterListener tasksFilterListener) {
        filterListener = tasksFilterListener;
    }

}
