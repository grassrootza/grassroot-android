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
import com.techmorphosis.grassroot.models.Group_ActivitiesModel;
import com.techmorphosis.grassroot.ui.activities.Group_Activities;

import java.util.ArrayList;

/**
 * Created by ravi on 15/4/16.
 */
public class Group_ActivitiesAdapter extends RecyclerView.Adapter<Group_ActivitiesAdapter.GA_ViewHolder> {

    private final Group_Activities activity;
    private  ArrayList<Group_ActivitiesModel> data;
    private Group_ActivitiesModel model;
    private static final String TAG = Group_ActivitiesAdapter.class.getCanonicalName();

    public Group_ActivitiesAdapter(ArrayList<Group_ActivitiesModel> arrayList, Group_Activities group_activities) {
        this.data = arrayList;
        this.activity = group_activities;
     }

    @Override
    public GA_ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_group_activities,parent,false);
        GA_ViewHolder holder = new GA_ViewHolder(view);
        return holder;
    }

    @Override
    public void onBindViewHolder(GA_ViewHolder holder, int position) {
        Log.d(TAG, "Inside onBindViewHolder ... at position = " + position);

        Group_ActivitiesModel model = data.get(position);

        holder.txtTiltle.setText(model.title );
        holder.txtGroupownername.setText("Posted by " + model.name);
        holder.txtGroupdesc.setText(model.description);
        holder.datetime.setText(model.deadline);

        if (model.type.equalsIgnoreCase("vote")){
            holder.iv_type.setImageResource(R.drawable.ic_home_vote_active);
            votemeeting(holder, model,position);
        }

        else if (model.type.equalsIgnoreCase("meeting")){
            holder.iv_type.setImageResource(R.drawable.ic_home_call_meeting_active);
            votemeeting(holder, model, position);
        }

        else if (model.type.equalsIgnoreCase("todo")){
            holder.iv_type.setImageResource(R.drawable.ic_home_to_do_active);
            ToDo(holder, model,position);
        }
    }

//    private void setData(int position, GA_ViewHolder holder)
//    {
//
//        Group_ActivitiesModel model=data.get(position);
//
//        switch (model.type.toLowerCase())
//        {
//            case "vote" :
//                holder.iv_type.setImageResource(R.drawable.ic_home_vote_active);
//                votemeeting(holder, model);
//                break;
//
//
//            case "meeting" :
//                holder.iv_type.setImageResource(R.drawable.ic_home_call_meeting_active);
//                votemeeting(holder, model);
//                break;
//
//
//            case "todo" :
//                holder.iv_type.setImageResource(R.drawable.ic_home_to_do_active);
//                votemeeting(holder, model);
//                break;
//            default:
//                break;
//
//        }
//        holder.datetime.setText(model.deadline);
//    }

    private void ToDo(GA_ViewHolder holder, Group_ActivitiesModel model1, int position)
    {

        //pending
        holder.iv1.setImageResource(R.drawable.ic_group_to_do_pending_inactive);

        //completed
        holder.iv2.setImageResource(R.drawable.ic_vote_tick_inactive);

        //overdue
        holder.iv3.setImageResource(R.drawable.ic_group_to_do_overdue_inactive);


        Group_ActivitiesModel model=model1;

        switch (model.reply.toLowerCase())
        {
            case "pending" :
                //pending active
                holder.iv1.setImageResource(R.drawable.ic_group_to_do_pending);

                if (model.completedyes.equalsIgnoreCase("enableclick"))
                {
                        activity.Completed(holder.iv2,position,"Pending");
                }
                else
                {
                    holder.iv2.setEnabled(false);
                }
                holder.iv3.setEnabled(false);


                break;

            case "completed" :
                //completed
                holder.iv2.setImageResource(R.drawable.ic_vote_tick_active);
                holder.iv2.setEnabled(false);
                holder.iv3.setEnabled(false);
                break;

            case "overdue" :
                //overdue
                holder.iv3.setImageResource(R.drawable.ic_group_to_do_overdue);

                if (model.completedyes.equalsIgnoreCase("enableclick"))
                {
                    activity.Completed(holder.iv2,position, "OVERDUE");
                }
                else
                {
                    holder.iv2.setEnabled(false);
                }
                holder.iv3.setEnabled(false);


                break;

        }


    }

    private void votemeeting(GA_ViewHolder holder, Group_ActivitiesModel model1, int position) {

        Group_ActivitiesModel model = model1;
        if (model.hasResponded) {
            holder.iv1.setImageResource(R.drawable.ic_vote_tick_active);
            canAction(holder, model,position);
        } else if (!model.hasResponded) {
            holder.iv1.setImageResource(R.drawable.ic_vote_tick_inactive);
            canAction(holder, model, position);
        }
    }

    private void canAction(GA_ViewHolder holder, Group_ActivitiesModel model1, int position) {
       // model=data.get(position);
        Group_ActivitiesModel model=model1;
        if (model.canAction) {
            if (model.hasResponded) {
                hasRespondedAndCanAction(holder, model,position);
            } else {
                hasNotRespondedButCanAction(holder, model,position);
            }
        } else if (!model.canAction) {
            canActionIsFalse(holder, model, position);
        }
    }

    private void hasRespondedAndCanAction(GA_ViewHolder holder, Group_ActivitiesModel model1, int position) {
        Group_ActivitiesModel model= model1;

        if (model.reply.equalsIgnoreCase("yes")) {
            holder.iv2.setImageResource(R.drawable.ic_vote_active); // thumbs up
            holder.iv3.setImageResource(R.drawable.ic_no_vote_inactive); // thumbs down

            if (model.Thumpsup.equalsIgnoreCase("enableclick")) {
                activity.thumps_Up(holder.iv2,position);
            } else {
                holder.iv2.setEnabled(false);
            }

            if (model.Thumpsdown.equalsIgnoreCase("enableclick")) {
                activity.thumps_Down(holder.iv3, position);
            } else {
                holder.iv3.setEnabled(false);
            }
        }  else  if (model.reply.equalsIgnoreCase("no")) {
            holder.iv2.setImageResource(R.drawable.ic_vote_inactive); // thumbs up
            holder.iv3.setImageResource(R.drawable.ic_no_vote_active); // thumbs down

            if (model.Thumpsup.equalsIgnoreCase("enableclick")) {
                activity.thumps_Up(holder.iv2,position);
            } else {
                holder.iv2.setEnabled(false);
            }

            if (model.Thumpsdown.equalsIgnoreCase("enableclick")) {
                activity.thumps_Down(holder.iv3, position);
            } else {
                holder.iv3.setEnabled(false);
            }
        }
    }

    private void hasNotRespondedButCanAction(GA_ViewHolder holder, Group_ActivitiesModel model, int position)
    { //all grey
        //Thumbs down
        holder.iv3.setImageResource(R.drawable.ic_no_vote_inactive);
        //Thumbs up
        holder.iv2.setImageResource(R.drawable.ic_vote_inactive);

        if (model.Thumpsup.equalsIgnoreCase("enableclick")) {
            activity.thumps_Up(holder.iv2,position);
        } else {
            holder.iv2.setEnabled(false);
        }

        if (model.Thumpsdown.equalsIgnoreCase("enableclick")) {
            activity.thumps_Down(holder.iv3, position);
        } else {
            holder.iv3.setEnabled(false);
        }

    }

    private void canActionIsFalse(GA_ViewHolder holder, Group_ActivitiesModel model1, int position)
    {

        //model=data.get(position);

        Group_ActivitiesModel model=model1;

        if (model.reply.equalsIgnoreCase("Yes"))
        {
            //Thumbs up
            holder.iv2.setImageResource(R.drawable.ic_vote_active);
            //Thumbs down
            holder.iv3.setImageResource(R.drawable.ic_no_vote_inactive);

                        /*Thumpsup*/
            if (model.Thumpsup.equalsIgnoreCase("enableclick"))
            {

                activity.thumps_Up(holder.iv2,position);
            }
            else
            {
                holder.iv2.setEnabled(false);
            }


                        /*Thumpsdown*/
            if (model.Thumpsdown.equalsIgnoreCase("enableclick"))
            {
                activity.thumps_Down(holder.iv3, position);

            }
            else
            {
                holder.iv3.setEnabled(false);
            }

        }
        else  if (model.reply.equalsIgnoreCase("NO") )
        {

            //Thumbs up
            holder.iv2.setImageResource(R.drawable.ic_vote_inactive);

            //Thumbs down
            holder.iv3.setImageResource(R.drawable.ic_no_vote_active);

            if (model.Thumpsup.equalsIgnoreCase("enableclick"))
            {

                activity.thumps_Up(holder.iv2,position);
            }
            else
            {
                holder.iv2.setEnabled(false);
            }

            if (model.Thumpsdown.equalsIgnoreCase("enableclick"))
            {
                activity.thumps_Down(holder.iv3, position);

            }
            else
            {
                holder.iv3.setEnabled(false);
            }

        }
        else  if (model.reply.equalsIgnoreCase("NO_RESPONSE"))
        {

            //Thumbs up
            holder.iv2.setImageResource(R.drawable.ic_vote_inactive);

            //Thumbs down
            holder.iv3.setImageResource(R.drawable.ic_no_vote_inactive);

            if (model.Thumpsup.equalsIgnoreCase("enableclick"))
            {

                activity.thumps_Up(holder.iv2,position);
            }
            else
            {
                holder.iv2.setEnabled(false);
            }

            if (model.Thumpsdown.equalsIgnoreCase("enableclick"))
            {
                activity.thumps_Down(holder.iv3, position);

            }
            else
            {
                holder.iv3.setEnabled(false);
            }
        }
    }




    @Override
    public int getItemCount() {
        return data.size();
    }

    public void addApplications(ArrayList<Group_ActivitiesModel> activitiesList) {
        data.addAll(activitiesList);
        this.notifyItemRangeInserted(0,activitiesList.size()-1);
    }

    public void clearApplications() {
        int size = this.data.size();
        if (size > 0) {
            for (int i = 0; i < size; i++) {
                data.remove(0);
            }

            this.notifyItemRangeRemoved(0, size);
        }
            }

    public static  class GA_ViewHolder extends RecyclerView.ViewHolder {

        private CardView mainView;
        private RelativeLayout header;
        private ImageView iv_type;
        private TextView txtTiltle;
        private TextView txtGroupownername;
        private TextView txtGroupdesc;
        private View divider;
        private RelativeLayout footer;
        private ImageView iv1;
        private ImageView iv2;
        private ImageView iv3;
        private TextView datetime;

        public GA_ViewHolder(View view)
        {
            super(view);

            mainView = (CardView) view.findViewById(R.id.main_view);
            header = (RelativeLayout) view.findViewById(R.id.header);
            iv_type = (ImageView) view.findViewById(R.id.iv_type);
            txtTiltle = (TextView) view.findViewById(R.id.txt_title);
            txtGroupownername = (TextView) view.findViewById(R.id.txt_groupownername);
            txtGroupdesc = (TextView) view.findViewById(R.id.txt_groupdesc);
            divider = (View) view.findViewById(R.id.divider);
            footer = (RelativeLayout) view.findViewById(R.id.footer);
            iv1 = (ImageView) view.findViewById(R.id.iv1);
            iv2 = (ImageView) view.findViewById(R.id.iv2);
            iv3 = (ImageView) view.findViewById(R.id.iv3);
            datetime = (TextView) view.findViewById(R.id.datetime);
        }
    }
}
