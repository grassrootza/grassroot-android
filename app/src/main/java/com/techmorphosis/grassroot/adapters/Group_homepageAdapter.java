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
import com.techmorphosis.grassroot.ui.fragments.Group_Homepage;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;

/**P
 */
public class Group_homepageAdapter  extends RecyclerView.Adapter<Group_homepageAdapter.GHP_ViewHolder>{

    private final Group_Homepage activity;
    Context context;
    View v;
    ArrayList<Group> groups;
    ArrayList<Group> oldGroupModel;
    private String TAG= Group_homepageAdapter.class.getSimpleName();


    private static final SimpleDateFormat inputSDF = new SimpleDateFormat("dd-MM-yyyy");
    private static final SimpleDateFormat outputSDF = new SimpleDateFormat("EEE, d MMM, ''yy");



    public Group_homepageAdapter(Context context,ArrayList<Group> groups,Group_Homepage activity)
    {
        this.context = context;
        this.groups = groups;
        this.activity=activity;
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
        Group group= groups.get(position);
        holder.txtGroupname.setText(group.getGroupName());
        holder.txtGroupownername.setText(group.getGroupCreator());
        holder.txtGroupdesc.setText(group.getDescription());

        int height = holder.profileV1.getDrawable().getIntrinsicWidth();
        int width = holder.profileV1.getDrawable().getIntrinsicHeight();

        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) holder.profileV3.getLayoutParams();
        params.height = height;
        params.width=width;
        holder.profileV3.setLayoutParams(params);

        if (group.getGroupMemberCount() == 1)
        {
            holder.profileV1.setVisibility(View.VISIBLE);
        }
        else if (group.getGroupMemberCount() == 2)
        {
            holder.profileV1.setVisibility(View.VISIBLE);
            holder.profileV2.setVisibility(View.VISIBLE);
        }
        else if (group.getGroupMemberCount() > 2)
        {
            holder.profileV2.setVisibility(View.VISIBLE);
            holder.profileV3.setVisibility(View.VISIBLE);
            holder.profileV3.setText("+" + String.valueOf(group.getGroupMemberCount() - 2));
        }

        String displayDateTime;

        try {
            // todo: move this into the Model constructor instead (oh for Java 8)
            Date date = inputSDF.parse(group.getDateTimefull());
            displayDateTime = date.after(new Date()) ? "Next event: " + outputSDF.format(date) : "Last event: " + outputSDF.format(date);
        } catch (Exception e) {
            displayDateTime = group.getDateTimeShort();
        }

        holder.datetime.setText(displayDateTime);

        activity.addLongClickStringAction(context, holder.cardView, position);
        activity.addClickStringAction(context, holder.cardView, position);

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

        private CardView cardView;
        private TextView txtGroupname;
        private TextView txtGroupownername;
        private TextView txtGroupdesc;

        private ImageView profileV1;
        private ImageView profileV2;
        private TextView profileV3;
        private  TextView datetime;


        public GHP_ViewHolder(View view) {
            super(view);

           cardView = (CardView) view.findViewById(R.id.main_view);
            txtGroupname = (TextView) view.findViewById(R.id.txt_groupname);
            txtGroupownername = (TextView) view.findViewById(R.id.txt_groupownername);
            txtGroupdesc = (TextView) view.findViewById(R.id.txt_groupdesc);

            profileV1 = (ImageView) view.findViewById(R.id.profile_v1);
            profileV2 = (ImageView) view.findViewById(R.id.profile_v2);
            profileV3 = (TextView) view.findViewById(R.id.profile_v3);
            datetime = (TextView) view.findViewById(R.id.datetime);

        }
    }
}
