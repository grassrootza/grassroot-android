package org.grassroot.android.adapters;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RelativeLayout;
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

    private static final String TAG = GroupPickAdapter.class.getSimpleName();

    private ArrayList<Group> groupsToDisplay;
    private Context containingContext;
    private GroupPickAdapterListener listener;

    public interface GroupPickAdapterListener {
        void onGroupPicked(Group group);
    }

    public static GroupPickAdapter newInstance(List<Group> groupsToDisplay, Context context, GroupPickAdapterListener listener) {
        if (groupsToDisplay == null) {
            throw new UnsupportedOperationException("Error! Trying to call group picker with null list of groups");
        }
        GroupPickAdapter adapter = new GroupPickAdapter();
        adapter.groupsToDisplay = new ArrayList<>(groupsToDisplay);
        adapter.containingContext = context;
        adapter.listener = listener;
        return adapter;
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

        // todo :send actual group description, rename this to lastChange or so on
        if (!TextUtils.isEmpty(group.getDescription())) {
            holder.groupDescription.setText(group.getDescription());
        } else {
            final String organizer = String.format(containingContext.getString(R.string.group_organizer_prefix), group.getGroupCreator());
            holder.groupDescription.setText(organizer);
        }

        holder.groupEvent.setText(String.format(containingContext.getString(R.string.desc_body_pattern),
                group.constructChangeType(containingContext), group.getDescription()));

        holder.pickButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onGroupPicked(group);
            }
        });
    }

    @Override
    public int getItemCount() {
        return groupsToDisplay.size();
    }

    public static class GroupPickViewHolder extends RecyclerView.ViewHolder {

        TextView groupName;
        TextView groupDescription;
        TextView groupEvent;
        Button pickButton;

        public GroupPickViewHolder(View view) {
            super(view);

            groupName = (TextView) view.findViewById(R.id.gpick_group_name);
            groupDescription = (TextView) view.findViewById(R.id.gpick_group_description);
            groupEvent = (TextView) view.findViewById(R.id.gpick_item_last_event);
            pickButton = (Button) view.findViewById(R.id.gpick_btn_click);
        }
    }

}
