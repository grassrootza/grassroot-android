package org.grassroot.android.fragments;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import org.grassroot.android.R;
import org.grassroot.android.activities.CreateGroupActivity;
import org.grassroot.android.activities.CreateMeetingActivity;
import org.grassroot.android.activities.CreateTodoActivity;
import org.grassroot.android.activities.CreateVoteActivity;
import org.grassroot.android.activities.GroupAvatarActivity;
import org.grassroot.android.activities.GroupSearchActivity;
import org.grassroot.android.activities.GroupTasksActivity;
import org.grassroot.android.adapters.GroupListAdapter;
import org.grassroot.android.events.GroupCreatedEvent;
import org.grassroot.android.events.GroupDeletedEvent;
import org.grassroot.android.events.GroupEditedEvent;
import org.grassroot.android.events.GroupPictureChangedEvent;
import org.grassroot.android.events.GroupsRefreshedEvent;
import org.grassroot.android.events.JoinRequestReceived;
import org.grassroot.android.events.TaskAddedEvent;
import org.grassroot.android.events.UserLoggedOutEvent;
import org.grassroot.android.interfaces.GroupConstants;
import org.grassroot.android.interfaces.GroupPickCallbacks;
import org.grassroot.android.interfaces.SortInterface;
import org.grassroot.android.interfaces.TaskConstants;
import org.grassroot.android.models.Group;
import org.grassroot.android.services.GroupService;
import org.grassroot.android.utils.Constant;
import org.grassroot.android.utils.ErrorUtils;
import org.grassroot.android.utils.IntentUtils;
import org.grassroot.android.utils.RealmUtils;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import rx.Subscriber;

public class HomeGroupListFragment extends android.support.v4.app.Fragment
        implements GroupListAdapter.GroupRowListener {

    private String TAG = HomeGroupListFragment.class.getSimpleName();

    private Unbinder unbinder;

    @BindView(R.id.rl_ghp_root)
    RelativeLayout rlGhpRoot;

    private GroupListAdapter groupListRowAdapter;

    @BindView(R.id.gl_swipe_refresh)
    SwipeRefreshLayout glSwipeRefresh;
    @BindView(R.id.recycler_view)
    RecyclerView rcGroupList;

    private boolean floatingMenuOpen = false;
    @BindView(R.id.fab_menu_open)
    FloatingActionButton fabOpenMenu;
    @BindView(R.id.ll_fab_new_task)
    LinearLayout fabNewTask;
    @BindView(R.id.ll_fab_join_group)
    LinearLayout fabFindGroup;
    @BindView(R.id.ll_fab_start_group)
    LinearLayout fabStartGroup;

    ProgressDialog progressDialog;

    public boolean date_click = false, role_click = false, defaults_click = false;

    private GroupPickCallbacks mCallbacks;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        Activity activity = (Activity) context;
        try {
            mCallbacks = (GroupPickCallbacks) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException("Activity must implement group pick callbacks");
        }
    }

    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                       Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_group__homepage, container, false);
        unbinder = ButterKnife.bind(this, view);
        EventBus.getDefault().register(this);

        setUpRecyclerView();
        return view;
    }

    @Override public void onActivityCreated(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
    }

    private void setUpRecyclerView() {
        RealmUtils.loadGroupsSorted().subscribe(new Subscriber<List<Group>>() {
            @Override public void onCompleted() {

            }

            @Override public void onError(Throwable e) {

            }

            @Override public void onNext(List<Group> groups) {
                Log.e(TAG, "loaded groups ... setting recycler parameters");
                groupListRowAdapter = new GroupListAdapter(groups, HomeGroupListFragment.this);
                rcGroupList.setAdapter(groupListRowAdapter);
                rcGroupList.setHasFixedSize(true);
                rcGroupList.setLayoutManager(new LinearLayoutManager(getActivity()));
                rcGroupList.setItemViewCacheSize(20);
                rcGroupList.setDrawingCacheEnabled(true);
                rcGroupList.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);
                rcGroupList.setVisibility(View.VISIBLE);

                glSwipeRefresh.setColorSchemeColors(ContextCompat.getColor(getActivity(), R.color.primaryColor));
                glSwipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
                    @Override
                    public void onRefresh() {
                        refreshGroupList();
                    }
                });

                toggleProgressIfGroupsShowing();
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
		Log.e(TAG, "inside on start ... toggling progress indicator");
		toggleProgressIfGroupsShowing();
    }

    @Override public void onResume() {
        super.onResume();
        fabOpenMenu.setVisibility(View.VISIBLE);
        Log.e(TAG, "inside on resume ... toggling progress indicator");
        toggleProgressIfGroupsShowing();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        hideProgress();
        EventBus.getDefault().unregister(this);
        unbinder.unbind();
    }

    public void showSuccessMessage(Intent data) {
        String message = data.getStringExtra(Constant.SUCCESS_MESSAGE);
        ErrorUtils.showSnackBar(rlGhpRoot, message, Snackbar.LENGTH_LONG, "", null);
    }

    /**
     * Method executed to retrieve and populate list of groups.
     * todo : set some preference or flag as "offline" if error, and show a dialog box
     */

    @Subscribe
    public void onGroupJoinRequestsLoaded(JoinRequestReceived e) {
        ErrorUtils.showSnackBar(rlGhpRoot, R.string.jreq_notice, Snackbar.LENGTH_LONG);
    }

    /*
    Separating this method from the above, because we will probably want it to call some kind of diff
    in time, rather than doing a full refresh, and don't need to worry about progress bar, etc
   */
    public void refreshGroupList() {
        GroupService.getInstance()
                .fetchGroupListWithErrorDisplay(getActivity(), null, new GroupService.GroupServiceListener() {
                    @Override public void groupListLoaded() {
                        groupListRowAdapter.refreshGroupsToDB();
                        hideProgress();
                    }

                    @Override public void groupListLoadingError() {
                        hideProgress();
                    }
                });
    }

    public void updateSingleGroup(final int position, final String groupUid) {
        if (position == -1) {
            throw new UnsupportedOperationException("ERROR! This should not be called without a valid position");
        }
    }

    @OnClick(R.id.fab_menu_open)
    public void toggleFloatingMenu() {
        if (!floatingMenuOpen) {
            openFloatingMenu();
        } else {
            closeFloatingMenu();
        }
    }

    private void openFloatingMenu() {
        floatingMenuOpen = true;
        fabOpenMenu.setImageResource(R.drawable.ic_add_45d);
        fabNewTask.setVisibility(View.VISIBLE);
        fabFindGroup.setVisibility(View.VISIBLE);
        fabStartGroup.setVisibility(View.VISIBLE);
    }

    private void closeFloatingMenu() {
        floatingMenuOpen = false;
        fabOpenMenu.setImageResource(R.drawable.ic_add);
        fabNewTask.setVisibility(View.GONE);
        fabFindGroup.setVisibility(View.GONE);
        fabStartGroup.setVisibility(View.GONE);
    }

    @OnClick(R.id.ic_fab_new_task)
    public void icFabNewTask() {
        closeFloatingMenu();
        QuickTaskModalFragment modal = QuickTaskModalFragment.newInstance(false, null, new QuickTaskModalFragment.TaskModalListener() {
            @Override
            public void onTaskClicked(String taskType) {
                fabOpenMenu.setVisibility(View.GONE);
                mCallbacks.groupPickerTriggered(taskType);
            }
        });
        modal.show(getFragmentManager(), QuickTaskModalFragment.class.getSimpleName());
    }

    @OnClick(R.id.ic_fab_join_group)
    public void icFabJoinGroup() {
        closeFloatingMenu();
        Intent icFabJoinGroup = new Intent(getActivity(), GroupSearchActivity.class);
        startActivity(icFabJoinGroup);
    }

    @OnClick(R.id.ic_fab_start_group)
    public void icFabStartGroup() {
        closeFloatingMenu();
        Intent icFabStartGroup = new Intent(getActivity(), CreateGroupActivity.class);
        startActivityForResult(icFabStartGroup, Constant.activityCreateGroup);
    }

    @Override
    public void onGroupRowShortClick(Group group) {
        if (floatingMenuOpen) closeFloatingMenu();
        if (!group.getIsLocal()) {
            startActivity(IntentUtils.constructIntent(getActivity(), GroupTasksActivity.class, group));
        } else {
            showGroupWipMenu(group);
        }
    }

    private void showGroupWipMenu(final Group group) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(R.string.cg_wip_message);
        builder.setItems(R.array.cg_offline_group, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0:
                        startActivity(IntentUtils.constructIntent(getActivity(), CreateGroupActivity.class, group));
                        break;
                    case 1:
                        startActivity(IntentUtils.constructIntent(getActivity(), GroupTasksActivity.class, group));
                        break;
                    case 2:
                        GroupService.getInstance().deleteLocallyCreatedGroup(group.getGroupUid());
                        break;
                    default:
                        break;
                }
            }
        });
        builder.create().show();
    }

    @Override
    public void onGroupRowLongClick(Group group) {
        showQuickOptionsDialog(group);
    }

    private void showQuickOptionsDialog(final Group group) {
        QuickTaskModalFragment dialog = QuickTaskModalFragment.newInstance(true, group,
                new QuickTaskModalFragment.TaskModalListener() {
                    @Override
                    public void onTaskClicked(String taskType) {
                        Intent i;
                        switch (taskType) {
                            case TaskConstants.MEETING:
                                i = IntentUtils.constructIntent(getActivity(), CreateMeetingActivity.class,
                                        group.getGroupUid(), group.getGroupName());
                                break;
                            case TaskConstants.VOTE:
                                i = IntentUtils.constructIntent(getActivity(), CreateVoteActivity.class,
                                        group.getGroupUid(), group.getGroupName());
                                break;
                            case TaskConstants.TODO:
                                i = IntentUtils.constructIntent(getActivity(), CreateTodoActivity.class,
                                        group.getGroupUid(), group.getGroupName());
                                break;
                            default:
                                throw new UnsupportedOperationException("Error! Unknown task type");
                        }
                        startActivityForResult(i, Constant.activityCreateTask);
                    }
                });
        dialog.show(getFragmentManager(), QuickTaskModalFragment.class.getSimpleName());
    }

    @Override
    public void onGroupRowMemberClick(Group group, int position) {
        GroupQuickMemberModalFragment dialog = new GroupQuickMemberModalFragment();
        Bundle args = new Bundle();
        args.putParcelable(GroupConstants.OBJECT_FIELD, group);
        args.putInt(Constant.INDEX_FIELD, position);
        dialog.setArguments(args);
        dialog.show(getFragmentManager(), "GroupQuickMemberModalFragment");
    }

    @Override
    public void onGroupRowAvatarClick(Group group, int position) {
        Intent intent = IntentUtils.constructIntent(getActivity(), GroupAvatarActivity.class,
                group);
        intent.putExtra(Constant.INDEX_FIELD, position);
        startActivity(intent);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallbacks = null;
    }

    private void toggleProgressIfGroupsShowing() {
        if (GroupService.isFetchingGroups) {
			Log.e(TAG, "group service is fetching, hence showing ...");
			showProgress();
        } else if (groupListRowAdapter == null) {
			Log.e(TAG, "groupListRowAdaper null, showing dialog ...");
			showProgress();
		} else if (groupListRowAdapter.getItemCount() == 0) {
            Log.e(TAG, "groupListAdapter empty, hence showing ...");
			showProgress();
        } else {
            Log.e(TAG, "neither of the above, hence hiding");
			hideProgress();
        }
    }

    private void showProgress() {
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(getContext());
            progressDialog.setIndeterminate(true);
            progressDialog.setMessage(getString(R.string.txt_pls_wait));
        }
        progressDialog.show();
    }

    private void hideProgress() {
        if (progressDialog != null) {
            progressDialog.dismiss();
        }
        if (glSwipeRefresh != null && glSwipeRefresh.isRefreshing()) {
            glSwipeRefresh.setRefreshing(false);
        }
    }

    @Subscribe
    public void onEvent(GroupsRefreshedEvent e) {
        if (groupListRowAdapter != null) {
            groupListRowAdapter.refreshGroupsToDB();
        }
        hideProgress();
    }

    @Subscribe
    public void onEvent(GroupPictureChangedEvent groupPictureUploadedEvent) {
        groupListRowAdapter.refreshGroupsToDB();
    }

    @Subscribe
    public void onTaskCreatedEvent(TaskAddedEvent e) {
        groupListRowAdapter.refreshGroupsToDB();
    }

    @Subscribe
    public void onGroupAdded(GroupCreatedEvent e) {
        groupListRowAdapter.refreshGroupsToDB();
    }

    @Subscribe
    public void onGroupEditedEvent(GroupEditedEvent e) {
        groupListRowAdapter.refreshSingleGroup(e.groupUid);
    }

    @Subscribe public void onGroupDeletedEvent(GroupDeletedEvent e) { groupListRowAdapter.removeSingleGroup(e.groupUid); } // todo : make more efficient ...

    @Subscribe
    public void onEvent(UserLoggedOutEvent e) {
        // finish on main activity seems to not clear this
        groupListRowAdapter.setGroupList(new ArrayList<Group>());
    }

    public void sortGroups() {
        SortFragment sortFragment = new SortFragment();
        Bundle b = new Bundle();
        b.putBoolean("Date", date_click);
        b.putBoolean("Role", role_click);
        b.putBoolean("Default", defaults_click);
        sortFragment.setArguments(b);
        sortFragment.show(getFragmentManager(), "SortFragment");
        sortFragment.setListener(new SortInterface() {

            @Override
            public void tvDateClick(boolean date, boolean role, boolean defaults) {
                date_click = true;
                role_click = false;
                Long start = SystemClock.currentThreadTimeMillis();
                groupListRowAdapter.sortByDate();
                Log.d(TAG, String.format("sorting group list took %d msecs",
                        SystemClock.currentThreadTimeMillis() - start));
            }

            @Override
            public void roleClick(boolean date, boolean role, boolean defaults) {
                date_click = false;
                role_click = true;
                Long start = SystemClock.currentThreadTimeMillis();
                groupListRowAdapter.sortByRole();
                Log.d(TAG, String.format("sorting group list took %d msecs",
                        SystemClock.currentThreadTimeMillis() - start));
            }

            @Override
            public void defaultsClick(boolean date, boolean role, boolean defaults) {
                // todo : restore whatever was here
            }
        });
    }

    public void searchStringChanged(String query) {
        if (TextUtils.isEmpty(query)) {
            groupListRowAdapter.refreshGroupsToDB();
        } else {
            groupListRowAdapter.simpleSearchByName(query);
        }
    }
}