package com.techmorphosis.grassroot.adapters;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.techmorphosis.grassroot.interfaces.TaskListListener;
import com.techmorphosis.grassroot.R;
import com.techmorphosis.grassroot.services.model.TaskModel;
import com.techmorphosis.grassroot.utils.Constant;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by ravi on 15/4/16.
 */
public class TasksAdapter extends RecyclerView.Adapter<TasksAdapter.GA_ViewHolder> {

    private final TaskListListener listener;
    private List<TaskModel> tasks;

    private final int primaryColor, textColor, secondaryColor;

    private static final String TAG = TasksAdapter.class.getCanonicalName();
    private static final SimpleDateFormat deadlineFormat = new SimpleDateFormat("H:mm, EEE, d MMM"); // todo: differentiate

    public TasksAdapter(ArrayList<TaskModel> tasks, TaskListListener listListener, Context context) {
        this.tasks = tasks;
        this.listener = listListener;
        this.primaryColor = ContextCompat.getColor(context, R.color.primaryColor);
        this.textColor = ContextCompat.getColor(context, R.color.black);
        this.secondaryColor = ContextCompat.getColor(context, R.color.text_grey);
    }

    @Override
    public GA_ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_group_activities, parent, false);
        GA_ViewHolder holder = new GA_ViewHolder(view);
        return holder;
    }

    @Override
    public void onBindViewHolder(GA_ViewHolder holder, int position) {

        TaskModel taskModel = tasks.get(position);
        taskModel.resetResponseFlags(); // since we can't trust Android's construction mechanism

        holder.txtTiltle.setText(taskModel.getTitle());
        holder.txtGroupownername.setText("Posted by " + taskModel.getName());
        if (taskModel.getDescription() == null || taskModel.getDescription().trim().equals("")) {
            holder.txtGroupdesc.setVisibility(View.GONE);
        } else {
            holder.txtGroupdesc.setText(taskModel.getDescription());
        }

        holder.datetime.setText(deadlineFormat.format(taskModel.getDeadlineDate()));
        listener.onCardClick(holder.cardView, position);//todo fix this

        switch (taskModel.getType()) {
            case Constant.MEETING:
                holder.iv_type.setImageResource(R.drawable.ic_home_call_meeting_active);
                setUpVoteOrMeeting(holder, taskModel);
                break;
            case Constant.VOTE:
                holder.iv_type.setImageResource(R.drawable.ic_home_vote_active);
                setUpVoteOrMeeting(holder, taskModel);
                break;
            case Constant.TODO:
                holder.iv_type.setImageResource(R.drawable.ic_home_to_do_active);
                setUpToDo(holder, taskModel);
                break;
            default:
                throw new UnsupportedOperationException("Task holder without a valid task type!");
        }
    }

    private void setResponseListener(ImageView icon, final TaskModel task, final String response) {
        icon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.respondToTask(task.getId(), task.getType(), response);
            }
        });
    }



    private void setUpCardStyle(GA_ViewHolder viewHolder, boolean isCardPrimary) {
        viewHolder.txtTiltle.setTextColor(isCardPrimary ? primaryColor : secondaryColor);
        viewHolder.txtGroupownername.setTextColor(isCardPrimary ? textColor : secondaryColor);
        viewHolder.txtGroupdesc.setTextColor(isCardPrimary ? textColor : secondaryColor);
        viewHolder.divider.setBackgroundColor(isCardPrimary ? textColor : secondaryColor);
    }

    private void setUpToDo(GA_ViewHolder holder, final TaskModel task) {
        holder.iv1.setImageResource(R.drawable.ic_group_to_do_pending_inactive); //pending icon
        holder.iv2.setImageResource(R.drawable.ic_vote_tick_inactive); //completed icon
        holder.iv3.setImageResource(R.drawable.ic_group_to_do_overdue_inactive); //overdue icon

        setUpCardStyle(holder, task.isInFuture());

        if (task.isCanMarkCompleted()) setResponseListener(holder.iv2, task, "COMPLETED");
        holder.iv2.setEnabled(task.isCanMarkCompleted());
        holder.iv3.setEnabled(false);

        switch (task.getReply().toLowerCase()) {
            case "pending":
                holder.iv1.setImageResource(R.drawable.ic_group_to_do_pending);
                break;
            case "completed":
                holder.iv2.setImageResource(R.drawable.ic_vote_tick_active);
                break;
            case "overdue":
                holder.iv3.setImageResource(R.drawable.ic_group_to_do_overdue);
                break;
        }
    }

    private void setUpVoteOrMeeting(GA_ViewHolder holder, final TaskModel task) {

        holder.iv1.setImageResource(task.hasResponded() ? R.drawable.ic_vote_tick_active : R.drawable.ic_vote_tick_inactive);
        setUpCardStyle(holder, task.isInFuture());

        if (task.canAction()) {
            if (task.hasResponded()) {
                hasRespondedButCanAction(holder, task);
            } else {
                hasNotRespondedButCanAction(holder, task);
            }
        } else if (!task.canAction()) {
            getCanActionIsFalse(holder, task);
        }
    }

    private void hasRespondedButCanAction(GA_ViewHolder holder, TaskModel model) {

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

    private void hasNotRespondedButCanAction(GA_ViewHolder holder, TaskModel model) { //all grey
        holder.iv3.setImageResource(R.drawable.ic_no_vote_inactive);
        holder.iv2.setImageResource(R.drawable.ic_vote_inactive);
        setResponseListener(holder.iv2, model, "Yes");
        setResponseListener(holder.iv3, model, "No");
    }

    private void getCanActionIsFalse(GA_ViewHolder holder, TaskModel model) {
        holder.iv2.setImageResource(model.getReply().equalsIgnoreCase("Yes") ? R.drawable.ic_vote_active : R.drawable.ic_vote_inactive);
        holder.iv3.setImageResource(model.getReply().equalsIgnoreCase("No") ? R.drawable.ic_no_vote_active : R.drawable.ic_no_vote_inactive);
        holder.iv2.setEnabled(false);
        holder.iv3.setEnabled(false);
    }


    @Override
    public int getItemCount() {
        return tasks.size();
    }

    public void addTasks(List<TaskModel> activitiesList) {
        tasks.addAll(activitiesList);
        this.notifyItemRangeInserted(0, activitiesList.size() - 1);
    }

    public void clearTasks() {
        int size = this.tasks.size();
        if (size > 0) {
            for (int i = 0; i < size; i++) {
                tasks.remove(0);
            }
            this.notifyItemRangeRemoved(0, size);
        }
    }

    public void changeToTaskList(List<TaskModel> tasksToView) {
        // todo: optimize this, a lot, is used in filtering what can be quite large lists
        this.tasks.clear();
        this.tasks.addAll(tasksToView); // not great, but otherwise run into lots of errors because of assignment etc
        notifyDataSetChanged(); // very bad, just a stopgap
    }

    public static class GA_ViewHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.iv_type)
        ImageView iv_type;
        @BindView(R.id.task_card_view_root)
        CardView cardView;
        @BindView(R.id.txt_title)
        TextView txtTiltle;
        @BindView(R.id.txt_groupownername)
        TextView txtGroupownername;
        @BindView(R.id.txt_groupdesc)
        TextView txtGroupdesc;
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

        public GA_ViewHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
        }
    }
}
