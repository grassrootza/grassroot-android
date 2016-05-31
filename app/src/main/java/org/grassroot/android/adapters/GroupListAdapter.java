package org.grassroot.android.adapters;

import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.grassroot.android.R;
import org.grassroot.android.services.model.Group;
import org.grassroot.android.ui.fragments.HomeGroupListFragment;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**P
 */
public class GroupListAdapter extends RecyclerView.Adapter<GroupListAdapter.GHP_ViewHolder>{

    private String TAG = GroupListAdapter.class.getSimpleName();

    private final HomeGroupListFragment activity;

    List<Group> displayedGroups;

    private static final SimpleDateFormat outputSDF = new SimpleDateFormat("EEE, d MMM, ''yy");

    public GroupListAdapter(List<Group> groups, HomeGroupListFragment activity) {
        this.displayedGroups = groups;
        this.activity = activity;
    }

    // todo: consider moving these back out to fragment (esp given notifyDataSet being bad..)
    public void sortByDate() {
        Collections.sort(displayedGroups, Collections.reverseOrder()); // since Date entity sorts earliest to latest
        notifyDataSetChanged();
    }

    public void sortByRole() {
        Collections.sort(displayedGroups, Collections.reverseOrder(Group.GroupRoleComparator)); // as above
        notifyDataSetChanged();
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
        Group group = displayedGroups.get(position);

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
        params.width = width;

        holder.profileV2.setLayoutParams(params);
        holder.profileV1.setVisibility(View.VISIBLE);
        holder.profileV2.setVisibility(View.VISIBLE);
        holder.profileV2.setText("+" + String.valueOf(group.getGroupMemberCount()));

        Date date = group.getDate();
        String datePrefix = date.after(new Date()) ? "Next event: "  : "Last event: ";
        holder.datetime.setText(datePrefix + outputSDF.format(date));

        activity.addGroupRowLongClickListener(holder.cardView, position);
        activity.addGroupRowShortClickListener(holder.cardView, position);
        activity.addGroupRowMemberNumberClickListener(holder.memberIcons, position);
    }


    @Override
    public int getItemCount() {
        return displayedGroups.size();
    }

    public void addData(List<Group> groupList){
        Log.e(TAG, "adding data to group list! number of groups: " + groupList.size());
        displayedGroups.addAll(groupList);
        this.notifyItemRangeInserted(0, groupList.size() - 1);
    }

    // todo: this might not be the best way to do this (maybe rethink whole list structure/handling etc)
    public void updateGroup(int position, Group group) {
        displayedGroups.set(position, group);
        notifyItemChanged(position);
        notifyDataSetChanged(); // for some reason, the line above calls onBindViewHolder, but doesn't rewrite the text view!!
    }

    public void addGroup(int position, Group group) {
        displayedGroups.add(position, group);
        notifyItemInserted(position);
    }

    public void setGroupList(List<Group> groupList) {
        displayedGroups.clear();
        displayedGroups.addAll(groupList);
        notifyDataSetChanged(); // calling item range inserted causes a strange crash (related to main/background threads, I think)
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
