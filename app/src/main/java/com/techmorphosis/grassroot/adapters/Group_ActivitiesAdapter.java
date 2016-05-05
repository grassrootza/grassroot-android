package com.techmorphosis.grassroot.adapters;

import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.techmorphosis.grassroot.R;
import com.techmorphosis.grassroot.services.model.TaskModel;
import com.techmorphosis.grassroot.ui.activities.Group_Activities;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by ravi on 15/4/16.
 */
public class Group_ActivitiesAdapter extends RecyclerView.Adapter<Group_ActivitiesAdapter.GA_ViewHolder> {

    private final Group_Activities activity;
    private List<TaskModel> tasks;
    private static final String TAG = Group_ActivitiesAdapter.class.getCanonicalName();

    public Group_ActivitiesAdapter(ArrayList<TaskModel> tasks, Group_Activities group_activities) {
        this.tasks = tasks;
        this.activity = group_activities;
    }

    @Override
    public GA_ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_group_activities, parent, false);
        GA_ViewHolder holder = new GA_ViewHolder(view);
        return holder;
    }

    @Override
    public void onBindViewHolder(GA_ViewHolder holder, int position) {
        Log.d(TAG, "Inside onBindViewHolder ... at position = " + position);

        TaskModel taskModel = tasks.get(position);

        holder.txtTiltle.setText(taskModel.getTitle());
        holder.txtGroupownername.setText("Posted by " + taskModel.getName());
        holder.txtGroupdesc.setText(taskModel.getDescription());
        holder.datetime.setText(taskModel.getDeadline());

        if (taskModel.getType().equalsIgnoreCase("vote")) {
            holder.iv_type.setImageResource(R.drawable.ic_home_vote_active);
            votemeeting(holder, taskModel, position);
        } else if (taskModel.getType().equalsIgnoreCase("meeting")) {
            holder.iv_type.setImageResource(R.drawable.ic_home_call_meeting_active);
            votemeeting(holder, taskModel, position);
        } else if (taskModel.getType().equalsIgnoreCase("todo")) {
            holder.iv_type.setImageResource(R.drawable.ic_home_to_do_active);
            ToDo(holder, taskModel, position);
        }
    }


    private void ToDo(GA_ViewHolder holder, TaskModel model1, int position) {

        //pending
        holder.iv1.setImageResource(R.drawable.ic_group_to_do_pending_inactive);

        //completed
        holder.iv2.setImageResource(R.drawable.ic_vote_tick_inactive);

        //overdue
        holder.iv3.setImageResource(R.drawable.ic_group_to_do_overdue_inactive);


        TaskModel model = model1;

        switch (model.getReply().toLowerCase()) {
            case "pending":
                //pending active
                holder.iv1.setImageResource(R.drawable.ic_group_to_do_pending);

                if (model.getCompletedYes().equalsIgnoreCase("enableclick")) {
                    activity.completed(holder.iv2, position, "Pending");
                } else {
                    holder.iv2.setEnabled(false);
                }
                holder.iv3.setEnabled(false);


                break;

            case "completed":
                //completed
                holder.iv2.setImageResource(R.drawable.ic_vote_tick_active);
                holder.iv2.setEnabled(false);
                holder.iv3.setEnabled(false);
                break;

            case "overdue":
                //overdue
                holder.iv3.setImageResource(R.drawable.ic_group_to_do_overdue);

                if (model.getCompletedYes().equalsIgnoreCase("enableclick")) {
                    activity.completed(holder.iv2, position, "OVERDUE");
                } else {
                    holder.iv2.setEnabled(false);
                }
                holder.iv3.setEnabled(false);
                break;

        }


    }

    private void votemeeting(GA_ViewHolder holder, TaskModel model1, int position) {

        TaskModel model = model1;
        if (model.getHasResponded()) {
            holder.iv1.setImageResource(R.drawable.ic_vote_tick_active);
            getCanAction(holder, model, position);
        } else if (!model.getHasResponded()) {
            holder.iv1.setImageResource(R.drawable.ic_vote_tick_inactive);
            getCanAction(holder, model, position);
        }
    }

    private void getCanAction(GA_ViewHolder holder, TaskModel model1, int position) {
        // model=tasks.get(position);
        TaskModel model = model1;
        if (model.getCanAction()) {
            if (model.getHasResponded()) {
                getHasRespondedAndCanAction(holder, model, position);
            } else {
                hasNotRespondedButgetCanAction(holder, model, position);
            }
        } else if (!model.getCanAction()) {
            getCanActionIsFalse(holder, model, position);
        }
    }

    private void getHasRespondedAndCanAction(GA_ViewHolder holder, TaskModel model1, int position) {
        TaskModel model = model1;

        if (model.getReply().equalsIgnoreCase("yes")) {
            holder.iv2.setImageResource(R.drawable.ic_vote_active); // thumbs up
            holder.iv3.setImageResource(R.drawable.ic_no_vote_inactive); // thumbs down

            if (model.getThumbsUp().equalsIgnoreCase("enableclick")) {
                activity.thumbsUp(holder.iv2, position);
            } else {
                holder.iv2.setEnabled(false);
            }

            if (model.getThumbsDown().equalsIgnoreCase("enableclick")) {
                activity.thumbsDown(holder.iv3, position);
            } else {
                holder.iv3.setEnabled(false);
            }
        } else if (model.getReply().equalsIgnoreCase("no")) {
            holder.iv2.setImageResource(R.drawable.ic_vote_inactive); // thumbs up
            holder.iv3.setImageResource(R.drawable.ic_no_vote_active); // thumbs down

            if (model.getThumbsUp().equalsIgnoreCase("enableclick")) {
                activity.thumbsUp(holder.iv2, position);
            } else {
                holder.iv2.setEnabled(false);
            }

            if (model.getThumbsDown().equalsIgnoreCase("enableclick")) {
                activity.thumbsDown(holder.iv3, position);
            } else {
                holder.iv3.setEnabled(false);
            }
        }
    }

    private void hasNotRespondedButgetCanAction(GA_ViewHolder holder, TaskModel model, int position) { //all grey
        //Thumbs down
        holder.iv3.setImageResource(R.drawable.ic_no_vote_inactive);
        //Thumbs up
        holder.iv2.setImageResource(R.drawable.ic_vote_inactive);

        if (model.getThumbsUp().equalsIgnoreCase("enableclick")) {
            activity.thumbsUp(holder.iv2, position);
        } else {
            holder.iv2.setEnabled(false);
        }

        if (model.getThumbsDown().equalsIgnoreCase("enableclick")) {
            activity.thumbsDown(holder.iv3, position);
        } else {
            holder.iv3.setEnabled(false);
        }

    }

    private void getCanActionIsFalse(GA_ViewHolder holder, TaskModel model, int position) {

        if (model.getReply().equalsIgnoreCase("Yes")) {
            //Thumbs up
            holder.iv2.setImageResource(R.drawable.ic_vote_active);
            //Thumbs down
            holder.iv3.setImageResource(R.drawable.ic_no_vote_inactive);

                        /*getThumbsUp()*/
            if (model.getThumbsUp().equalsIgnoreCase("enableclick")) {

                activity.thumbsUp(holder.iv2, position);
            } else {
                holder.iv2.setEnabled(false);
            }

                        /*getThumbsDown()*/
            if (model.getThumbsDown().equalsIgnoreCase("enableclick")) {
                activity.thumbsDown(holder.iv3, position);

            } else {
                holder.iv3.setEnabled(false);
            }

        } else if (model.getReply().equalsIgnoreCase("NO")) {

            //Thumbs up
            holder.iv2.setImageResource(R.drawable.ic_vote_inactive);

            //Thumbs down
            holder.iv3.setImageResource(R.drawable.ic_no_vote_active);

            if (model.getThumbsUp().equalsIgnoreCase("enableclick")) {

                activity.thumbsUp(holder.iv2, position);
            } else {
                holder.iv2.setEnabled(false);
            }

            if (model.getThumbsDown().equalsIgnoreCase("enableclick")) {
                activity.thumbsDown(holder.iv3, position);

            } else {
                holder.iv3.setEnabled(false);
            }

        } else if (model.getReply().equalsIgnoreCase("NO_RESPONSE")) {

            //Thumbs up
            holder.iv2.setImageResource(R.drawable.ic_vote_inactive);

            //Thumbs down
            holder.iv3.setImageResource(R.drawable.ic_no_vote_inactive);

            if (model.getThumbsUp().equalsIgnoreCase("enableclick")) {

                activity.thumbsUp(holder.iv2, position);
            } else {
                holder.iv2.setEnabled(false);
            }

            if (model.getThumbsDown().equalsIgnoreCase("enableclick")) {
                activity.thumbsDown(holder.iv3, position);

            } else {
                holder.iv3.setEnabled(false);
            }
        }
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

    public static class GA_ViewHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.main_view)
        CardView mainView;
        @BindView(R.id.header)
        RelativeLayout header;
        @BindView(R.id.iv_type)
        ImageView iv_type;
        @BindView(R.id.txt_title)
        TextView txtTiltle;
        @BindView(R.id.txt_groupownername)
        TextView txtGroupownername;
        @BindView(R.id.txt_groupdesc)
        TextView txtGroupdesc;
        @BindView(R.id.divider)
        View divider;
        @BindView(R.id.footer)
        RelativeLayout footer;
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
