package org.grassroot.android.adapters;

import android.content.Context;
import android.os.Build;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.grassroot.android.R;
import org.grassroot.android.fragments.HomeGroupListFragment;
import org.grassroot.android.models.Group;
import org.grassroot.android.services.ApplicationLoader;
import org.grassroot.android.utils.ImageUtils;
import org.grassroot.android.utils.RealmUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

/**
 * P
 */
public class GroupListAdapter extends RecyclerView.Adapter<GroupListAdapter.GHP_ViewHolder> {

    private String TAG = GroupListAdapter.class.getSimpleName();

    private final Context context;
    private final GroupRowListener listener;

    // in future, sort by 'most active', and search by location
    public static final String SORT_BY_ROLE = "sort_role";
    public static final String SORT_BY_GROUP_NAME = "sort_by_role";
    public static final String SORT_BY_SIZE = "sort_by_size";
    public static final String SORT_BY_TASK_DATE = "sort_by_task_date";
    public static final String SORT_BY_DATE_CHANGED = "sort_changed";
    public static final String SORT_DEFAULT = "sort_default";

    private String currentSort;

    List<Group> fullGroupList;
    List<Group> displayedGroups;

    private static final SimpleDateFormat outputSDF = new SimpleDateFormat("EEE, d MMM");
    private final float localGroupAlpha = 0.5f;

    public interface GroupRowListener {
        void onGroupRowShortClick(Group group);

        void onGroupRowLongClick(Group group);

        void onGroupRowMemberClick(Group group, int position);

        void onGroupRowAvatarClick(Group group, int position);
    }

    public GroupListAdapter(List<Group> groups, HomeGroupListFragment fragment) {
        this.currentSort = SORT_DEFAULT;
        this.displayedGroups = groups;
        this.listener = fragment;
        this.context = ApplicationLoader.applicationContext; // to avoid memory leaks, since only use context to get strings
    }

    public void setGroupList(List<Group> groupList) {
        displayedGroups = new ArrayList<>(groupList);
        notifyDataSetChanged(); // calling item range inserted causes a strange crash (related to main/background threads, possibly)
    }

    public void refreshGroupsToDB() {
        RealmUtils.loadGroupsSorted().subscribe(new Action1<List<Group>>() {
            @Override
            public void call(List<Group> groups) {
                displayedGroups = new ArrayList<>(groups);
                if (currentSort.equals(SORT_DEFAULT) || currentSort.equals(SORT_BY_DATE_CHANGED)) {
                    notifyDataSetChanged();
                } else {
                    setSortType(currentSort); // makes sure current sort retained
                }
            }
        });
    }

    public void refreshSingleGroup(final String groupUid) {
        Group group = RealmUtils.loadGroupFromDB(groupUid);
        for (int i = 0; i < displayedGroups.size(); i++) {
            if (displayedGroups.get(i).getGroupUid().equals(groupUid)) {
                displayedGroups.remove(i);
                group.setGroupMemberCount((int) RealmUtils.countGroupMembers(groupUid)); // because of lazy loading, this otherwise can go wrong
                displayedGroups.add(0, group);
                notifyItemRangeChanged(0, i + 1);
                Log.d(TAG,"Changed group " + groupUid);
            }
        }
    }

    public void addGroupToTop(final Group group, final int numberItemsToRefresh) {
        displayedGroups.add(0, group);
        // notifyItemRangeChanged(0, numberItemsToRefresh); // this does not work with alpha because Google
        notifyDataSetChanged();
    }

    public void removeSingleGroup(final String groupUid) {
        for (int i = 0; i < displayedGroups.size(); i++) {
            if (displayedGroups.get(i).getGroupUid().equals(groupUid)) {
                displayedGroups.remove(i);
                notifyDataSetChanged(); // not efficient, but calling just item remove doesn't refresh enough
            }
        }
    }

    public void replaceGroup(final String originalGroupUid, final String replacementGroupUid) {
        final int size = displayedGroups.size();
        int i = 0;
        Group replacementGroup = RealmUtils.loadGroupFromDB(replacementGroupUid);
        boolean groupFound = false; // using this instead of for loop to avoid concurrent list modification
        if (replacementGroup != null) {
            while (!groupFound && i < size) {
                if (displayedGroups.get(i).getGroupUid().equals(originalGroupUid)) {
                    groupFound = true;
                    displayedGroups.remove(i);
                    displayedGroups.add(i, replacementGroup);
                    Log.e(TAG, "replacing the group! ... replacement group local = " + replacementGroup.getIsLocal());
                    notifyItemChanged(i);
                }
                i++;
            }
        }
    }

    private boolean sortBySearchType(final String sortType) {
        Comparator<Group> comparator;
        switch (sortType) {
            case SORT_BY_ROLE:
                comparator = Group.GroupRoleComparator;
                break;
            case SORT_BY_TASK_DATE:
                comparator = Collections.reverseOrder(Group.GroupTaskDateComparator); // earliest to latest
                break;
            case SORT_BY_SIZE:
                comparator = Collections.reverseOrder(Group.GroupSizeComparator); // largest to smallest
                break;
            case SORT_BY_GROUP_NAME:
                comparator = Group.GroupNameComparator;
                break;
            case SORT_BY_DATE_CHANGED:
            default:
                comparator = Collections.reverseOrder(Group.GroupLastChangeComparator);
        }

        Collections.sort(displayedGroups, comparator);
        return true;
    }

    public void setSortType(final String sortType) {
        Observable.create(new Observable.OnSubscribe<Boolean>() {
            @Override
            public void call(Subscriber<? super Boolean> subscriber) {
                sortBySearchType(sortType);
                subscriber.onNext(true);
            }
        }).subscribeOn(Schedulers.computation()).observeOn(AndroidSchedulers.mainThread())
        .subscribe(new Action1<Boolean>() {
            @Override
            public void call(Boolean aBoolean) {
                notifyDataSetChanged();
                currentSort = sortType;
            }
        }, new Action1<Throwable>() {
            @Override
            public void call(Throwable throwable) {
                throwable.printStackTrace();
            }
        });
    }

    /*
    note: it would be more efficient to detect if string is longer/shorter, and go for
    add or remove instead, but group lists are small (~20-30 items) and JIT/JVM has
    hyper-optimized copy and remove on arraylists, so likely first task is shift the arraylist
    operation onto computation thread, next is to do any kind of further micro-optimizations
    (placeholder for timing log : Log.d(TAG, "finished and refreshed display ... in time :" + (SystemClock.currentThreadTimeMillis() - startTime));)
     */

    public void localSearchByText(final String searchText) {
        // final long startTime = SystemClock.currentThreadTimeMillis();
        if (fullGroupList == null) {
            fullGroupList = new ArrayList<>(displayedGroups); // i.e., we are starting a search
        } else {
            displayedGroups = new ArrayList<>(fullGroupList); // i.e., we are modifying a search
        }

        final String lcQuery = searchText.toLowerCase();

        Observable.from(fullGroupList)
            .subscribeOn(Schedulers.computation())
            .observeOn(AndroidSchedulers.mainThread())
            .filter(new Func1<Group, Boolean>() {
                @Override
                public Boolean call(Group group) {
                return !group.containsQueryText(lcQuery);
                }
            }).subscribe(new Subscriber<Group>() {
            @Override
            public void onError(Throwable e) { e.printStackTrace(); }

            @Override
            public void onNext(Group group) {
                displayedGroups.remove(group);
            }

            @Override
            public void onCompleted() {
                notifyDataSetChanged();
            }
        });

    }

    @Override
    public GHP_ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.row_group_homepage, parent, false);
        ButterKnife.bind(this, view);
        return new GHP_ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final GHP_ViewHolder holder, final int position) {

        final Group group = displayedGroups.get(position);

        setUpTextDescriptions(holder, group);
        setUpMemberCount(holder, group);
        setUpListeners(holder, group);
        setAvatarImage(holder, group);

        setAlphaForLocal(holder, group);
    }

    private void setAlphaForLocal(GHP_ViewHolder holder, final Group group) {
        if (Build.VERSION.SDK_INT < 11) {
            final AlphaAnimation animation = new AlphaAnimation(!group.getIsLocal() ? 1f : localGroupAlpha,
                    !group.getIsLocal() ? 1f : localGroupAlpha);
            animation.setDuration(50);
            animation.setFillAfter(true);
            holder.itemView.startAnimation(animation);
        } else {
            holder.itemView.setAlpha(!group.getIsLocal() ? 1f : localGroupAlpha);
        }
    }

    private void setUpTextDescriptions(GHP_ViewHolder holder, final Group group) {
        final String groupOrganizerDescription =
                String.format(context.getString(R.string.group_organizer_prefix), group.getGroupCreator());
        holder.txtGroupname.setText(group.getGroupName());
        holder.txtGroupownername.setText(groupOrganizerDescription);

        if (group.hasJoinCode()) {
            final String tokenCode = context.getString(R.string.join_code_prefix) + group.getJoinCode() + "#";
            holder.txtGroupdesc.setText(tokenCode);
        } else if (!TextUtils.isEmpty(group.getDescription())) {
            holder.txtGroupdesc.setText(String.format(context.getString(R.string.group_description_prefix),
                group.getDescription()));
        } else if (!TextUtils.isEmpty(group.getLastChangeDescription())) {
            holder.txtGroupdesc.setText(String.format(context.getString(R.string.desc_body_pattern),
                context.getString(group.getChangePrefix()), group.getLastChangeDescription()));
        } else {
            holder.txtGroupdesc.setVisibility(View.GONE);
        }

        holder.datetime.setText(String.format(context.getString(R.string.date_time_pattern),
            context.getString(group.getChangePrefix()), outputSDF.format(group.getDate())));
    }

    private void setUpMemberCount(GHP_ViewHolder holder, final Group group) {
        // note: check during drawing optimization if this is best way to do it (also need to handle X00)
        final int height = holder.profileV1.getDrawable().getIntrinsicWidth();
        final int width = holder.profileV1.getDrawable().getIntrinsicHeight();

        RelativeLayout.LayoutParams params =
                (RelativeLayout.LayoutParams) holder.profileV2.getLayoutParams();
        params.height = height;
        params.width = width;

        holder.profileV2.setLayoutParams(params);
        // adding one for organizer if group is local (server includes in count)
        holder.profileV2.setText(String.format(context.getString(R.string.member_count_pattern),
            group.getGroupMemberCount() + (group.getIsLocal() ? 1 : 0)));
    }

    private void setUpListeners(GHP_ViewHolder holder, final Group group) {
        final int position = holder.getAdapterPosition();
        holder.itemView.setLongClickable(true);

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
    }

    private void setAvatarImage(GHP_ViewHolder holder, final Group group) {
        final String imageUrl = group.getImageUrl();
        try {
            if (imageUrl != null) {
                ImageUtils.setAvatarImage(holder.avatar, imageUrl, group.getDefaultImageRes());
            } else {
                holder.avatar.setImageResource(group.getDefaultImageRes());
            }
        } catch (OutOfMemoryError e) {
            holder.avatar.setImageResource(R.drawable.ic_groups_default_avatar);
        }
    }

    @Override
    public int getItemCount() {
        return displayedGroups.size();
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
}