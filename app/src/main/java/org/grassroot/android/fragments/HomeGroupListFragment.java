package org.grassroot.android.fragments;

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
import android.view.Menu;
import android.view.MenuItem;
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
import org.grassroot.android.events.LocalGroupToServerEvent;
import org.grassroot.android.events.TaskAddedEvent;
import org.grassroot.android.events.UserLoggedOutEvent;
import org.grassroot.android.interfaces.GroupConstants;
import org.grassroot.android.interfaces.GroupPickCallbacks;
import org.grassroot.android.interfaces.TaskConstants;
import org.grassroot.android.models.Group;
import org.grassroot.android.models.PreferenceObject;
import org.grassroot.android.models.exceptions.ApiCallException;
import org.grassroot.android.services.GroupService;
import org.grassroot.android.utils.Constant;
import org.grassroot.android.utils.ErrorUtils;
import org.grassroot.android.utils.IntentUtils;
import org.grassroot.android.utils.NetworkUtils;
import org.grassroot.android.utils.RealmUtils;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

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
    private boolean triggeredGroupRefresh;

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

    private GroupPickCallbacks mCallbacks;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        Log.d(TAG, "home group list fragment ... on attach ... timer ... " + SystemClock.currentThreadTimeMillis());

        try {
            mCallbacks = (GroupPickCallbacks) context;
        } catch (ClassCastException e) {
            throw new ClassCastException("Activity must implement group pick callbacks");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                       Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.activity_group__homepage, container, false);
        unbinder = ButterKnife.bind(this, view);
        EventBus.getDefault().register(this);
        setHasOptionsMenu(true);

        setUpRecyclerView();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        fabOpenMenu.setVisibility(View.VISIBLE);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        if (menu.findItem(R.id.mi_icon_filter) != null)
            menu.findItem(R.id.mi_icon_filter).setVisible(false);
        if (menu.findItem(R.id.mi_icon_sort) != null)
            menu.findItem(R.id.mi_icon_sort).setVisible(true);
        if (menu.findItem(R.id.mi_share_default) != null)
            menu.findItem(R.id.mi_share_default).setVisible(false);
        if (menu.findItem(R.id.mi_only_unread) != null)
            menu.findItem(R.id.mi_only_unread).setVisible(false);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        hideProgress();
        EventBus.getDefault().unregister(this);
        unbinder.unbind();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallbacks = null;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.mi_icon_sort:
                sortGroups();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void setUpRecyclerView() {
        triggeredGroupRefresh = true;
        if (RealmUtils.countGroupsInDB() > 0) {
            loadGroupsFromDB(false);
            GroupService.getInstance().fetchGroupList(AndroidSchedulers.mainThread())
                .subscribe(new Action1<String>() {
                    @Override
                    public void call(String s) {
                        if (groupListRowAdapter != null) {
                            groupListRowAdapter.refreshGroupsToDB();
                        }
                        triggeredGroupRefresh = false;
                    }
            });
        } else {
            showProgress();
            GroupService.getInstance().fetchGroupList(AndroidSchedulers.mainThread())
                .subscribe(new Action1<String>() {
                @Override
                public void call(String s) {
                    loadGroupsFromDB(true);
                    triggeredGroupRefresh = false;
                }
            });
        }

        glSwipeRefresh.setColorSchemeColors(ContextCompat.getColor(getActivity(), R.color.primaryColor));
        glSwipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refreshGroupList();
            }
        });
    }

    private void loadGroupsFromDB(final boolean toggleProgress) {
        RealmUtils.loadGroupsSorted().subscribe(new Action1<List<Group>>() {
            @Override
            public void call(List<Group> groups) {
                Log.d(TAG, "loaded groups ... setting recycler parameters ... timer ... " + SystemClock.currentThreadTimeMillis());
                if (groupListRowAdapter == null) {
                    setUpAdapterAndView(groups);
                } else {
                    groupListRowAdapter.setGroupList(groups);
                }
                if (toggleProgress) {
                    hideProgress();
                }
            }
        });
    }

    private void setUpAdapterAndView(List<Group> groups) {
        groupListRowAdapter = new GroupListAdapter(groups, HomeGroupListFragment.this);
        if (rcGroupList != null) {
            rcGroupList.setAdapter(groupListRowAdapter);
            rcGroupList.setHasFixedSize(true);
            rcGroupList.setLayoutManager(new LinearLayoutManager(getActivity()));
            rcGroupList.setItemViewCacheSize(20);
            rcGroupList.setDrawingCacheEnabled(true);
            rcGroupList.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);
            rcGroupList.setVisibility(View.VISIBLE);
        }
    }

    /*
    Separating this method from the above, because we will probably want it to call some kind of diff
    in time, rather than doing a full refresh, and don't need to worry about progress bar, etc
   */
    public void refreshGroupList() {
        triggeredGroupRefresh = true;
        GroupService.getInstance().fetchGroupList(null).subscribe(new Subscriber<String>() {
            @Override
            public void onNext(String s) {
                Log.e(TAG, "came back from group list with string : " + s);
                switch (s) {
                    case NetworkUtils.FETCHED_SERVER:
                        groupListRowAdapter.refreshGroupsToDB();
                        break;
                    case NetworkUtils.CONNECT_ERROR:
                        Snackbar.make(rlGhpRoot, R.string.connect_error_homescreen, Snackbar.LENGTH_SHORT).show();
                        break;
                    case NetworkUtils.OFFLINE_SELECTED:
                        Snackbar.make(rlGhpRoot, R.string.connect_error_offline_home, Snackbar.LENGTH_SHORT).show();
                        break;
                    default:
                        final String errorMsg = ErrorUtils.serverErrorText(s);
                        Snackbar.make(rlGhpRoot, errorMsg, Snackbar.LENGTH_SHORT).show();
                }
                hideProgress();
                triggeredGroupRefresh = false;
            }

            @Override
            public void onError(Throwable e) {
                hideProgress();
                triggeredGroupRefresh = false;
                e.printStackTrace(); // means something else went wrong, so just fail gracefully
            }

            @Override
            public void onCompleted() { }
        });
    }

    // fragment only handles this if it isn't in the middle of a sequence the fragment initiated
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(GroupsRefreshedEvent e) {
        if (!triggeredGroupRefresh && groupListRowAdapter != null) {
            groupListRowAdapter.refreshGroupsToDB();
        }
    }

    @OnClick(R.id.ic_fab_new_task)
    public void icFabNewTask() {
        closeFloatingMenu();
        QuickTaskModalFragment modal = QuickTaskModalFragment.newInstance(false, null, new QuickTaskModalFragment.TaskModalListener() {
            @Override
            public void onTaskClicked(String taskType) {
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
        startActivity(new Intent(getActivity(), CreateGroupActivity.class));
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
                        trySendOfflineGroup(group);
                        break;
                    case 1:
                        startActivity(IntentUtils.constructIntent(getActivity(), CreateGroupActivity.class, group));
                        break;
                    case 2:
                        startActivity(IntentUtils.constructIntent(getActivity(), GroupTasksActivity.class, group));
                        break;
                    case 3:
                        GroupService.getInstance().deleteLocallyCreatedGroup(group.getGroupUid());
                        break;
                    default:
                        break;
                }
            }
        });
        builder.create().show();
    }

    private void trySendOfflineGroup(final Group group) {
        showProgress();
        // if user has selected offline, override it so the service tries to connect (but just set preference
        // so that the events & UI changes aren't triggered
        if (!NetworkUtils.isOnline()) {
            PreferenceObject prefs = RealmUtils.loadPreferencesFromDB();
            prefs.setOnlineStatus(NetworkUtils.OFFLINE_ON_FAIL);
            RealmUtils.saveDataToRealmSync(prefs);
        }

        GroupService.getInstance().sendNewGroupToServer(group.getGroupUid(), AndroidSchedulers.mainThread())
            .subscribe(new Subscriber<String>() {
                @Override
                public void onNext(String s) {
                    // final String serverUid = s.substring("OK-".length());
                    // groupListRowAdapter.replaceGroup(group.getGroupUid(), serverUid);
                    hideProgress();
                    Log.e(TAG, "group created on server ... now try send the rest ...");
                    NetworkUtils.trySwitchToOnlineQuiet(getContext(), true, Schedulers.computation());

                    if ("OK".equals(s.substring(0, 2))) {
                        Snackbar.make(rlGhpRoot, R.string.cg_sent_server, Snackbar.LENGTH_SHORT).show();
                    } else {
                        Snackbar.make(rlGhpRoot, R.string.server_error_background_group_numbers, Snackbar.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onError(Throwable e) {
                    hideProgress();
                    if (e instanceof ApiCallException) {
                        if (NetworkUtils.CONNECT_ERROR.equals(e.getMessage())) {
                            Snackbar.make(rlGhpRoot, R.string.cg_sending_failed_connect, Snackbar.LENGTH_SHORT).show();
                        } else if (NetworkUtils.SERVER_ERROR.equals(e.getMessage())) {
                            final String userMessage = ErrorUtils.serverErrorText(e);
                            Snackbar.make(rlGhpRoot, userMessage, Snackbar.LENGTH_LONG).show();
                        }
                    }
                }

                @Override
                public void onCompleted() { }
        });
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
                                        group.getGroupUid(), group.getGroupName(), group.getIsLocal());
                                break;
                            case TaskConstants.VOTE:
                                i = IntentUtils.constructIntent(getActivity(), CreateVoteActivity.class,
                                        group.getGroupUid(), group.getGroupName(), group.getIsLocal());
                                break;
                            case TaskConstants.TODO:
                                i = IntentUtils.constructIntent(getActivity(), CreateTodoActivity.class,
                                        group.getGroupUid(), group.getGroupName(), group.getIsLocal());
                                break;
                            default:
                                throw new UnsupportedOperationException("Error! Unknown task type");
                        }
                        startActivity(i);
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

    public void sortGroups() {

        // note : unfortunately we have to pass the array to the builder, instead of assembling
        // a map etc., so always make sure this array has to correspond to sequence in the string array file

        final String[] searchOptions = {
            GroupListAdapter.SORT_BY_GROUP_NAME,
            GroupListAdapter.SORT_BY_SIZE,
            GroupListAdapter.SORT_BY_ROLE,
            GroupListAdapter.SORT_BY_TASK_DATE,
            GroupListAdapter.SORT_BY_DATE_CHANGED };

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.group_sort_title)
            .setItems(R.array.group_sort_options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                groupListRowAdapter.setSortType(searchOptions[which]);
            }
        });

        builder.setCancelable(true)
            .create()
            .show();
    }

    public void searchStringChanged(String query) {
        if (TextUtils.isEmpty(query)) {
            groupListRowAdapter.refreshGroupsToDB();
        } else {
            groupListRowAdapter.localSearchByText(query);
        }
    }

    private void showProgress() {
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(getContext());
            progressDialog.setIndeterminate(true);
            progressDialog.setMessage(getString(R.string.wait_message));
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

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(LocalGroupToServerEvent event) {
        Log.e(TAG, "onEvent ... localgrouptoserver recevied ...");
        groupListRowAdapter.replaceGroup(event.localGroupUid, event.serverUid);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(GroupPictureChangedEvent event) {
        if (!TextUtils.isEmpty(event.groupUid)) {
            groupListRowAdapter.refreshSingleGroup(event.groupUid);
        } else {
            groupListRowAdapter.refreshGroupsToDB();
        }
    }

    @Subscribe
    public void onTaskCreatedEvent(TaskAddedEvent e) {
        if (e.getTaskCreated() != null) {
            groupListRowAdapter.refreshSingleGroup(e.getTaskCreated().getParentUid());
        } else {
            groupListRowAdapter.refreshGroupsToDB();
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onGroupAdded(GroupCreatedEvent e) {
        if (e.getGroup() != null) {
            // may be more efficient to compute number of children after layout, but possibly also micro-optimizing for later
            // note : at present adapter method calls notifyDataSetChanged instead of using range, because of alpha weirdness
            groupListRowAdapter.addGroupToTop(e.getGroup(), rcGroupList.getLayoutManager().getChildCount());
        } else if (!TextUtils.isEmpty(e.getGroupUid())) {
            groupListRowAdapter.addGroupToTop(RealmUtils.loadGroupFromDB(e.getGroupUid()),
                rcGroupList.getLayoutManager().getChildCount());
        } else {
            // this should not be possible, but failure around/when one of these events is triggered is going to be critical, so deal with it gracefully
            groupListRowAdapter.refreshGroupsToDB();
        }
    }

    @Subscribe
    public void onGroupEditedEvent(GroupEditedEvent e) {
        if (!TextUtils.isEmpty(e.groupUid)) {
            groupListRowAdapter.refreshSingleGroup(e.groupUid);
        } else {
            groupListRowAdapter.refreshGroupsToDB();
        }
    }

    @Subscribe public void onGroupDeletedEvent(GroupDeletedEvent e) {
        groupListRowAdapter.removeSingleGroup(e.groupUid);
    }

    @Subscribe
    public void onEvent(UserLoggedOutEvent e) {
        // finish on main activity seems to not clear this
        groupListRowAdapter.setGroupList(new ArrayList<Group>());
    }

    @OnClick(R.id.fab_menu_open)
    public void toggleFloatingMenu() {
        if (!floatingMenuOpen) {
            openFloatingMenu();
        } else {
            closeFloatingMenu();
        }
    }

    public boolean isFloatingMenuOpen() {
        return floatingMenuOpen;
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

}