package org.grassroot.android.adapters;

import android.content.Context;
import android.os.SystemClock;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.grassroot.android.R;
import org.grassroot.android.interfaces.TaskConstants;
import org.grassroot.android.services.model.TaskModel;
import org.grassroot.android.utils.Constant;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by ravi on 15/4/16.
 */
public class TasksAdapter extends RecyclerView.Adapter<TasksAdapter.TaskViewHolder> {

    private final TaskListListener listener;
    private List<TaskModel> viewedTasks;

    private List<TaskModel> fullTaskList;
    private Map<String, List<TaskModel>> decomposedList;

    private final int primaryColor, textColor, secondaryColor;

    private static final String TAG = TasksAdapter.class.getCanonicalName();

    public interface TaskListListener {
        void respondToTask(String taskUid, String taskType, String response);
        void onCardClick(int position, String taskUid, String taskType);
    }


    public TasksAdapter(TaskListListener listListener, Context context) {
        this.listener = listListener;
        this.primaryColor = ContextCompat.getColor(context, R.color.primaryColor);
        this.textColor = ContextCompat.getColor(context, R.color.black);
        this.secondaryColor = ContextCompat.getColor(context, R.color.text_grey);
        this.viewedTasks = new ArrayList<>();
    }

    @Override
    public TaskViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_group_activities, parent, false);
        TaskViewHolder holder = new TaskViewHolder(view);
        return holder;
    }

    @Override
    public void onBindViewHolder(TaskViewHolder holder, final int position) {
        final TaskModel taskModel = viewedTasks.get(position);
        taskModel.resetResponseFlags(); // since we can't trust Android's construction mechanism (todo : revisit)
        setCardListener(holder.cardView, position, taskModel);
        setUpCardImagesAndView(taskModel, holder);
    }

    private void setCardListener(CardView view, final int position, final TaskModel task) {
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                listener.onCardClick(position, task.getId(), task.getType());
            }
        });
    }

    private void setResponseListener(ImageView icon, final TaskModel task, final String response) {
        icon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.respondToTask(task.getId(), task.getType(), response);
            }
        });
    }

    private void setUpCardImagesAndView(final TaskModel task, TaskViewHolder holder) {

        holder.txtTitle.setText(task.getTitle());
        holder.txtTaskCallerName.setText("Posted by " + task.getName());

        if (task.getDescription() == null || task.getDescription().trim().equals("")) {
            holder.txtTaskDesc.setVisibility(View.GONE);
        } else {
            holder.txtTaskDesc.setText(task.getDescription());
        }

        holder.datetime.setText(TaskConstants.dateDisplayWithDayName.format(task.getDeadlineDate()));
        setUpCardStyle(holder, task.isInFuture());

        switch (task.getType()) {
            case TaskConstants.MEETING:
                holder.iv_type.setImageResource(R.drawable.ic_home_call_meeting_active);
                setUpVoteOrMeeting(holder, task);
                break;
            case TaskConstants.VOTE:
                holder.iv_type.setImageResource(R.drawable.ic_home_vote_active);
                setUpVoteOrMeeting(holder, task);
                break;
            case TaskConstants.TODO:
                holder.iv_type.setImageResource(R.drawable.ic_home_to_do_active);
                setUpToDo(holder, task);
                break;
            default:
                throw new UnsupportedOperationException("Task holder without a valid task type!");
        }
    }

    private void setUpCardStyle(TaskViewHolder viewHolder, final boolean isCardPrimary) {
        viewHolder.txtTitle.setTextColor(isCardPrimary ? primaryColor : secondaryColor);
        viewHolder.txtTaskCallerName.setTextColor(isCardPrimary ? textColor : secondaryColor);
        viewHolder.txtTaskDesc.setTextColor(isCardPrimary ? textColor : secondaryColor);
        viewHolder.divider.setBackgroundColor(isCardPrimary ? textColor : secondaryColor);
    }

    private void setUpToDo(TaskViewHolder holder, final TaskModel task) {
        holder.iv1.setImageResource(R.drawable.ic_group_to_do_pending_inactive); //pending icon
        holder.iv2.setImageResource(R.drawable.ic_vote_tick_inactive); //completed icon
        holder.iv3.setImageResource(R.drawable.ic_group_to_do_overdue_inactive); //overdue icon

        if (task.isCanMarkCompleted()) setResponseListener(holder.iv2, task, "COMPLETED");
        holder.iv2.setEnabled(task.isCanMarkCompleted());
        holder.iv3.setEnabled(false);

        switch (task.getReply().toLowerCase()) {
            case TaskConstants.TODO_PENDING:
                holder.iv1.setImageResource(R.drawable.ic_group_to_do_pending);
                break;
            case TaskConstants.TODO_DONE:
                holder.iv2.setImageResource(R.drawable.ic_vote_tick_active);
                break;
            case TaskConstants.TODO_OVERDUE:
                holder.iv3.setImageResource(R.drawable.ic_group_to_do_overdue);
                break;
        }
    }

    private void setUpVoteOrMeeting(TaskViewHolder holder, final TaskModel task) {
        holder.iv1.setImageResource(task.hasResponded() ? R.drawable.ic_vote_tick_active : R.drawable.ic_vote_tick_inactive);
        if (task.canAction()) {
            if (task.hasResponded()) {
                hasRespondedButCanAction(holder, task);
            } else {
                hasNotRespondedButCanAction(holder, task);
            }
        } else if (!task.canAction()) {
            cannotRespond(holder, task);
        }
    }

    private void hasRespondedButCanAction(TaskViewHolder holder, TaskModel model) {
        boolean repliedYes = model.getReply().equalsIgnoreCase("yes");
        boolean repliedNo = model.getReply().equalsIgnoreCase("no");

        holder.iv2.setImageResource(repliedYes ? R.drawable.ic_vote_active : R.drawable.ic_vote_inactive);
        holder.iv3.setImageResource(repliedNo ? R.drawable.ic_no_vote_active : R.drawable.ic_no_vote_inactive);

        if (model.isCanRespondYes())
            setResponseListener(holder.iv2, model, "Yes");
        else
            holder.iv2.setEnabled(false);

        if (model.isCanRespondNo())
            setResponseListener(holder.iv3, model, "No");
        else
            holder.iv3.setEnabled(false);

    }

    private void hasNotRespondedButCanAction(TaskViewHolder holder, TaskModel model) { //all grey
        holder.iv3.setImageResource(R.drawable.ic_no_vote_inactive);
        holder.iv2.setImageResource(R.drawable.ic_vote_inactive);
        holder.iv3.setEnabled(true);
        holder.iv2.setEnabled(true);
        setResponseListener(holder.iv2, model, "Yes");
        setResponseListener(holder.iv3, model, "No");
    }

    private void cannotRespond(TaskViewHolder holder, TaskModel model) {
        holder.iv2.setImageResource(model.getReply().equalsIgnoreCase("Yes") ? R.drawable.ic_vote_active : R.drawable.ic_vote_inactive);
        holder.iv3.setImageResource(model.getReply().equalsIgnoreCase("No") ? R.drawable.ic_no_vote_active : R.drawable.ic_no_vote_inactive);
        holder.iv2.setEnabled(false);
        holder.iv3.setEnabled(false);
    }

    @Override
    public int getItemCount() {
        return viewedTasks.size();
    }

    public void clearTasks() {
        int size = this.viewedTasks.size();
        if (size > 0) {
            for (int i = 0; i < size; i++) {
                viewedTasks.remove(0);
            }
            this.notifyItemRangeRemoved(0, size);
        }
    }

    public void changeToTaskList(List<TaskModel> tasksToView) {
        // todo: optimize this, a lot, is used in filtering what can be quite large lists
        if (viewedTasks == null) {
            viewedTasks = new ArrayList<>();
        } else {
            viewedTasks.clear();
        }
        viewedTasks.addAll(tasksToView); // not great, but otherwise run into lots of errors because of assignment etc
        notifyDataSetChanged(); // very bad, just a stopgap
    }

    /*
    SECTION : FILTER BY DATES, TIMES, ETC.
    // todo : make this async off the UI thread
     */

    private void decomposeLists() {
        Long startTime = SystemClock.currentThreadTimeMillis();

        decomposedList = new HashMap<>();
        List<TaskModel> voteList = new ArrayList<>();
        List<TaskModel> meetingList = new ArrayList<>();
        List<TaskModel> toDoList = new ArrayList<>();

        for (TaskModel tm : fullTaskList) {
            if (Constant.MEETING.equals(tm.getType())) meetingList.add(tm);
            else if (Constant.VOTE.equals(tm.getType())) voteList.add(tm);
            else if (Constant.TODO.equals(tm.getType())) toDoList.add(tm);
        }

        decomposedList.put(Constant.MEETING, meetingList);
        decomposedList.put(Constant.VOTE, voteList);
        decomposedList.put(Constant.TODO, toDoList);

        Log.d(TAG, String.format("decomposed task list, sizes: %d total, %d votes, %d mtgs, %d todos, took %d msecs", fullTaskList.size(),
                voteList.size(), meetingList.size(), toDoList.size(), SystemClock.currentThreadTimeMillis() - startTime));
    }

    public void startFiltering() {
        // todo : be careful / thoroughly debug this, including corner cases (user refreshes then hits filter etc)
        if (fullTaskList == null || fullTaskList.isEmpty()) {
            fullTaskList = new ArrayList<>(viewedTasks);
        }
        decomposeLists();
        viewedTasks.clear();
    }

    public void stopFiltering() {
        viewedTasks.clear();
        viewedTasks.addAll(fullTaskList);
        notifyDataSetChanged();
    }

    public void addOrRemoveTaskType(final String taskType, final boolean add) {
        final List<TaskModel> tasks = decomposedList.get(taskType);
        if (tasks == null) throw new UnsupportedOperationException("Error! Calling add remove with invalid type");

        if (!add) {
            viewedTasks.remove(tasks);
        } else {
            viewedTasks.addAll(tasks);
        }

        Collections.sort(viewedTasks, Collections.reverseOrder());
        notifyDataSetChanged();
    }

    /*
    The task view holder class
     */

    public static class TaskViewHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.iv_type)
        ImageView iv_type;
        @BindView(R.id.task_card_view_root)
        CardView cardView;
        @BindView(R.id.txt_title)
        TextView txtTitle;
        @BindView(R.id.txt_task_caller_name)
        TextView txtTaskCallerName;
        @BindView(R.id.txt_task_description)
        TextView txtTaskDesc;
        @BindView(R.id.divider)
        View divider;

        @BindView(R.id.iv1)
        ImageView iv1;
        @BindView(R.id.iv2)
        ImageView iv2;
        @BindView(R.id.iv3)
        ImageView iv3;
        @BindView(R.id.datetime)
        TextView datetime;

        public TaskViewHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
        }
    }
}
