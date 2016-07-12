package org.grassroot.android.adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.squareup.picasso.Callback;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Transformation;

import butterknife.BindView;
import butterknife.ButterKnife;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.grassroot.android.R;
import org.grassroot.android.fragments.HomeGroupListFragment;
import org.grassroot.android.interfaces.GroupConstants;
import org.grassroot.android.models.Group;
import org.grassroot.android.utils.CircularImageTransformer;
import org.grassroot.android.utils.ScalingUtilities;

/**
 * P
 */
public class GroupListAdapter extends RecyclerView.Adapter<GroupListAdapter.GHP_ViewHolder> {

    private String TAG = GroupListAdapter.class.getSimpleName();

    private final Context context;
    private final GroupRowListener listener;

    List<Group> displayedGroups;

    private static final SimpleDateFormat outputSDF = new SimpleDateFormat("EEE, d MMM");

    public interface GroupRowListener {
        void onGroupRowShortClick(Group group);

        void onGroupRowLongClick(Group group);

        void onGroupRowMemberClick(Group group, int position);

        void onGroupRowAvatarClick(Group group, int position);
    }

    public GroupListAdapter(List<Group> groups, HomeGroupListFragment fragment) {
        this.displayedGroups = groups;
        this.context = fragment.getContext();
        this.listener = fragment;
    }

    // todo: consider moving these back out to fragment (esp given notifyDataSet being bad..)
    public void sortByDate() {
        Collections.sort(displayedGroups,
                Collections.reverseOrder()); // since Date entity sorts earliest to latest
        notifyDataSetChanged();
    }

    public void sortByRole() {
        Collections.sort(displayedGroups,
                Collections.reverseOrder(Group.GroupRoleComparator)); // as above
        notifyDataSetChanged();
    }

    @Override
    public GHP_ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.row_group_homepage, parent, false);
        ButterKnife.bind(this, view);
        GHP_ViewHolder holder = new GHP_ViewHolder(view);
        return holder;
    }

    @Override
    public void onBindViewHolder(final GHP_ViewHolder holder, final int position) {

        // todo : this is a bit of a mess, see if possible to simplify

        holder.itemView.setLongClickable(true);
        final Group group = displayedGroups.get(position);
        if (Build.VERSION.SDK_INT > 11) {
            holder.itemView.setAlpha(!group.getIsLocal() ? 1f : 0.3f);
        }

        final String groupOrganizerDescription =
                String.format(context.getString(R.string.group_organizer_prefix), group.getGroupCreator());
        holder.txtGroupname.setText(group.getGroupName());
        holder.txtGroupownername.setText(groupOrganizerDescription);

        if (GroupConstants.NO_JOIN_CODE.equals(group.getJoinCode())) {
            final String groupDescription = group.getDescription();
            final int visibility = (TextUtils.isEmpty(groupDescription)) ? View.GONE : View.VISIBLE;
            holder.txtGroupdesc.setText(
                    String.format(context.getString(R.string.desc_body_pattern), getChangePrefix(group),
                            groupDescription));
            holder.txtGroupdesc.setVisibility(visibility);
        } else {
            final String tokenCode =
                    context.getString(R.string.join_code_prefix) + group.getJoinCode() + "#";
            holder.txtGroupdesc.setText(tokenCode);
        }

        // todo : check later if there's a more efficient way to do this?
        final int height = holder.profileV1.getDrawable().getIntrinsicWidth();
        final int width = holder.profileV1.getDrawable().getIntrinsicHeight();

        RelativeLayout.LayoutParams params =
                (RelativeLayout.LayoutParams) holder.profileV2.getLayoutParams();
        params.height = height;
        params.width = width;

        holder.profileV2.setLayoutParams(params);
        holder.profileV1.setVisibility(View.VISIBLE);
        holder.profileV2.setVisibility(View.VISIBLE);

        holder.profileV2.setText("+" + String.valueOf(group.getGroupMemberCount()));

        holder.datetime.setText(
                String.format(context.getString(R.string.date_time_pattern), getChangePrefix(group),
                        outputSDF.format(group.getDate())));

        holder.cardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                listener.onGroupRowShortClick(group);
            }
        });

        holder.cardView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                listener.onGroupRowLongClick(group);
                return true;
            }
        });

        holder.memberIcons.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                listener.onGroupRowMemberClick(group, position);
            }
        });
        holder.avatar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onGroupRowAvatarClick(group, position);
            }
        });

        final String imageUrl = group.getImageUrl();

        if (imageUrl != null) {
            setAvatar(holder.avatar, imageUrl);
        }
    }

    private String getChangePrefix(Group group) {
        switch (group.getLastChangeType()) {
            case GroupConstants.MEETING_CALLED:
                return (group.getDate().after(new Date()) ? context.getString(
                        R.string.future_meeting_prefix) : context.getString(R.string.past_meeting_prefix));
            case GroupConstants.VOTE_CALLED:
                return (group.getDate().after(new Date())) ? context.getString(R.string.future_vote_prefix)
                        : context.getString(R.string.past_vote_prefix);
            case GroupConstants.GROUP_CREATED:
                return context.getString(R.string.group_created_prefix);
            case GroupConstants.MEMBER_ADDED:
                return context.getString(R.string.member_added_prefix);
            case GroupConstants.GROUP_MOD_OTHER:
                return context.getString(R.string.group_other_prefix);
            default:
                throw new UnsupportedOperationException(
                        "Error! Should only be one of standard change types");
        }
    }

    @Override
    public int getItemCount() {
        return displayedGroups.size();
    }

    public void addData(List<Group> groupList) {
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
        notifyDataSetChanged(); // ugh, again, item inserted is not working! really need to figure out / fix
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
        @BindView(R.id.iv_gp_avatar)
        ImageView avatar;

        public GHP_ViewHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
        }
    }

    private void setAvatar(final ImageView view, final String imageUrl) {
        Picasso.with(context).load(imageUrl)
                .error(R.drawable.ic_profile_image)
                .networkPolicy(NetworkPolicy.OFFLINE)
                .transform(new CircularImageTransformer())
                .into(view, new Callback() {
                    @Override
                    public void onSuccess() {}
                    @Override
                    public void onError() {
                        Picasso.with(context).load(imageUrl)
                                .transform(new CircularImageTransformer())
                                .error(R.drawable.ic_profile_image)
                                .into(view);
                    }
                });

    }
}