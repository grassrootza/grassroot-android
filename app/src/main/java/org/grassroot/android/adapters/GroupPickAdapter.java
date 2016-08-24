package org.grassroot.android.adapters;

import android.app.Activity;
import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.grassroot.android.R;
import org.grassroot.android.models.Group;
import org.grassroot.android.services.ApplicationLoader;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by luke on 2016/06/29.
 */
public class GroupPickAdapter extends RecyclerView.Adapter<GroupPickAdapter.GroupPickViewHolder> {

    private ArrayList<Group> groupsToDisplay;
    private GroupPickAdapterListener listener;
    private String returnTag;
    private Context context;

    public interface GroupPickAdapterListener {
        void onGroupPicked(Group group, String returnTag);
    }

    public GroupPickAdapter(String returnTag, List<Group> groupsToDisplay, Activity activity) {
        if (groupsToDisplay == null || TextUtils.isEmpty(returnTag)) {
            throw new UnsupportedOperationException("Error! Trying to call group picker with null list of groups");
        }

        this.groupsToDisplay = new ArrayList<>(groupsToDisplay);
        this.returnTag = returnTag;
        this.context = ApplicationLoader.applicationContext;

        try {
            listener = (GroupPickAdapterListener) activity;
        } catch (ClassCastException e) {
            throw new UnsupportedOperationException("Activity containing group pick adapter must implement listener");
        }
    }

    public void setGroupList(List<Group> groupsToDisplay) {
        this.groupsToDisplay = new ArrayList<>(groupsToDisplay);
        notifyDataSetChanged();
    }

    @Override
    public GroupPickViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_group_picker, parent, false);
        return new GroupPickViewHolder(view);
    }

    @Override
    public void onBindViewHolder(GroupPickViewHolder holder, int position) {
        final Group group = groupsToDisplay.get(position);
        holder.groupName.setText(group.getGroupName());


        if (!TextUtils.isEmpty(group.getDescription())) {
            final String description = String.format(context.getString(R.string.group_description_prefix), group.getDescription());
            holder.groupDescription.setText(description);
        } else if (!TextUtils.isEmpty(group.getLastChangeDescription())) {
            final String changeDescription = String.format(context.getString(R.string.desc_body_pattern),
                context.getString(group.getChangePrefix()), group.getLastChangeDescription());
            holder.groupDescription.setText(changeDescription);
        } else {
            final String organizer = String.format(context.getString(R.string.group_organizer_prefix),
                group.getGroupCreator());
            holder.groupDescription.setText(organizer);
        }

        holder.memberCount.setText(String.format(context.getString(R.string.group_member_count), group.getGroupMemberCount()));

        holder.groupRoot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onGroupPicked(group, returnTag);
            }
        });
    }

    @Override
    public int getItemCount() {
        return groupsToDisplay.size();
    }

    public static class GroupPickViewHolder extends RecyclerView.ViewHolder {

        ViewGroup groupRoot;
        TextView groupName;
        TextView groupDescription;
        TextView memberCount;

        public GroupPickViewHolder(View view) {
            super(view);

            groupRoot = (ViewGroup) view.findViewById(R.id.gpick_item_root);
            groupName = (TextView) view.findViewById(R.id.gpick_group_name);
            groupDescription = (TextView) view.findViewById(R.id.gpick_group_description);
            memberCount = (TextView) view.findViewById(R.id.gpick_item_last_event);
        }
    }

}