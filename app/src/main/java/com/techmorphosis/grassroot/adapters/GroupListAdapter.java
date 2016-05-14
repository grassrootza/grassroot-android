package com.techmorphosis.grassroot.adapters;

import android.content.Context;

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
import com.techmorphosis.grassroot.services.model.Group;
import com.techmorphosis.grassroot.ui.fragments.HomeGroupListFragment;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;

/**P
 */
public class GroupListAdapter extends RecyclerView.Adapter<GroupListAdapter.GHP_ViewHolder>{

    private final HomeGroupListFragment activity;
    Context context;

    ArrayList<Group> groups;
    ArrayList<Group> oldGroupModel;
    private String TAG= GroupListAdapter.class.getSimpleName();

    private static final SimpleDateFormat inputSDF = new SimpleDateFormat("dd-MM-yy:HH:mm:SS");
    private static final SimpleDateFormat outputSDF = new SimpleDateFormat("EEE, d MMM, ''yy");

    public GroupListAdapter(Context context, ArrayList<Group> groups, HomeGroupListFragment activity) {
        this.context = context;
        this.groups = groups;
        this.activity = activity;
        Log.e(TAG,"Adapter data.size() is " + groups.size());
    }

    @Override
    public GHP_ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_group_homepage, parent, false);
        ButterKnife.bind(this, view);
        GHP_ViewHolder holder = new GHP_ViewHolder(view);
        return holder;
    }

    @Override
    public void onBindViewHolder(GHP_ViewHolder holder, int position) {

        holder.itemView.setLongClickable(true);
        Group group = groups.get(position);

        final String groupOrganizerDescription = "Organizer: " + group.getGroupCreator();
        final String groupDescription = group.getDescription();
        final int visibility = (groupDescription == null || groupDescription.trim().equals("")) ? View.GONE : View.VISIBLE;

        holder.txtGroupname.setText(group.getGroupName());
        holder.txtGroupownername.setText(groupOrganizerDescription);
        holder.txtGroupdesc.setText(groupDescription);
        holder.txtGroupdesc.setVisibility(visibility);

        int height = holder.profileV1.getDrawable().getIntrinsicWidth();
        int width = holder.profileV1.getDrawable().getIntrinsicHeight();

        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) holder.profileV2.getLayoutParams();
        params.height = height;
        params.width=width;

        holder.profileV2.setLayoutParams(params);
        holder.profileV1.setVisibility(View.VISIBLE);
        holder.profileV2.setVisibility(View.VISIBLE);
        holder.profileV2.setText("+" + String.valueOf(group.getGroupMemberCount()));

        String displayDateTime;

        try {
            // todo: move this into the Model constructor instead (oh for Java 8)
            Date date = inputSDF.parse(group.getDateTimeFull());
            displayDateTime = date.after(new Date()) ? "Next event: " + outputSDF.format(date)
                    : "Last event: " + outputSDF.format(date);
        } catch (Exception e) {
            displayDateTime = group.getDateTimeShort();
        }

        holder.datetime.setText(displayDateTime);

        activity.addGroupRowLongClickListener(holder.cardView, position);
        activity.addGroupRowShortClickListener(holder.cardView, position);
        activity.addGroupRowMemberNumberClickListener(holder.memberIcons, position);
    }


    @Override
    public int getItemCount() {
        return groups.size();
    }

    public void addData(ArrayList<Group> groupList){
        oldGroupModel = new ArrayList<>();
        groups.addAll(groupList);
        oldGroupModel.addAll(groupList);
        this.notifyItemRangeInserted(0, groupList.size() - 1);
    }

    public void filter(String searchwords)
    {
        //first clear the current data
        groups.clear();
        Log.e(TAG, "filter search_string is " + searchwords);

        if (searchwords.equals(""))
        {
            groups.addAll(oldGroupModel);
        }
        else
        {
            for (Group group:oldGroupModel)
            {

                if (group.getGroupName().trim().toLowerCase(Locale.getDefault()).contains(searchwords))
                {
                    Log.e(TAG,"model.groupName.trim() " + group.getGroupName().trim().toLowerCase(Locale.getDefault()));
                    Log.e(TAG,"searchwords is " + searchwords);
                    groups.add(group);
                }
                else
                {
                    //Log.e(TAG,"not found");
                }

            }
        }
        notifyDataSetChanged();

    }

    public void clearAll() {
        this.notifyDataSetChanged();
    }

    public void clearGroups() {
        int size = this.groups.size();
        if (size > 0) {
            for (int i = 0; i < size; i++) {
                groups.remove(0);
            }

            this.notifyItemRangeRemoved(0, size);
        }
    }

    public class GHP_ViewHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.task_card_view_root)
        CardView cardView;
        @BindView(R.id.txt_groupname)
        TextView txtGroupname;
        @BindView(R.id.txt_groupownername)
        TextView txtGroupownername;
        @BindView(R.id.txt_groupdesc)
        TextView txtGroupdesc;
        @BindView(R.id.profile_v1)
        ImageView profileV1;
        @BindView(R.id.profile_v2)
        TextView profileV2;
        @BindView(R.id.datetime)
        TextView datetime;
        @BindView(R.id.member_icons)
        RelativeLayout memberIcons;


        public GHP_ViewHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
        }
    }

}
