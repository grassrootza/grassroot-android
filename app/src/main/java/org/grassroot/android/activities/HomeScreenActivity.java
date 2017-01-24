package org.grassroot.android.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import org.grassroot.android.R;
import org.grassroot.android.adapters.GroupPickAdapter;
import org.grassroot.android.events.GroupChatEvent;
import org.grassroot.android.events.TaskAddedEvent;
import org.grassroot.android.events.TaskCancelledEvent;
import org.grassroot.android.events.UserLoggedOutEvent;
import org.grassroot.android.fragments.GroupPickFragment;
import org.grassroot.android.fragments.HomeGroupListFragment;
import org.grassroot.android.fragments.JoinRequestMasterFragment;
import org.grassroot.android.fragments.NavigationDrawerFragment;
import org.grassroot.android.fragments.NewTaskMenuFragment;
import org.grassroot.android.fragments.NotificationCenterFragment;
import org.grassroot.android.fragments.QuickTaskModalFragment;
import org.grassroot.android.fragments.TaskListFragment;
import org.grassroot.android.fragments.ViewTaskFragment;
import org.grassroot.android.fragments.dialogs.ConfirmCancelDialogFragment;
import org.grassroot.android.interfaces.GroupConstants;
import org.grassroot.android.interfaces.GroupPickCallbacks;
import org.grassroot.android.interfaces.NavigationConstants;
import org.grassroot.android.interfaces.TaskConstants;
import org.grassroot.android.models.Group;
import org.grassroot.android.services.GcmListenerService;
import org.grassroot.android.services.SharingService;
import org.grassroot.android.utils.NetworkUtils;
import org.grassroot.android.utils.PermissionUtils;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;

public class HomeScreenActivity extends PortraitActivity implements NavigationDrawerFragment.NavigationDrawerCallbacks,
    GroupPickCallbacks, NewTaskMenuFragment.NewTaskMenuListener, TaskListFragment.TaskListListener,
    SearchView.OnQueryTextListener, GroupPickAdapter.GroupPickAdapterListener {

    private static final String TAG = HomeScreenActivity.class.getSimpleName();

    @BindView(R.id.home_toolbar) Toolbar toolbar;
    @BindView(R.id.drawer_layout) DrawerLayout drawer;
    private TextView toolbarTitle;

    private HomeGroupListFragment groupListFragment;
    private TaskListFragment taskListFragment;
    private NotificationCenterFragment notificationCenterFragment;
    private JoinRequestMasterFragment joinRequestsFragment;

    private boolean isFirstFragmentSwap = true;
    private boolean showMenuOptions = true;

    private ActionBarDrawerToggle drawerToggle;
    private int currentMainFragment;
    private int mainFragmentFromNewIntent = -1;
    private boolean isVisible;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_home_screen);
        ButterKnife.bind(this);
        EventBus.getDefault().register(this);
        setUpToolbar();
        String openOn = getIntent().getStringExtra(NavigationConstants.HOME_OPEN_ON_NAV);

        if (openOn == null) {
            openOn = NavigationConstants.ITEM_SHOW_GROUPS;
        }

        switch (openOn) {
            case NavigationConstants.ITEM_SHOW_GROUPS:
                switchToGroupFragment();
                break;
            case NavigationConstants.ITEM_TASKS:
                switchToTasksFragment();
                break;
            case NavigationConstants.ITEM_NOTIFICATIONS:
                switchToNotificationFragment();
                break;
            case NavigationConstants.ITEM_JOIN_REQS:
                switchToJoinRequestsFragment();
                break;
            default:
                switchToGroupFragment();
                break;
        }
        setNavBarToItem(openOn);

        Intent i = new Intent(this, SharingService.class);
        i.putExtra(SharingService.ACTION_TYPE,SharingService.TYPE_SEARCH);
        startService(i);
    }

    private void setUpToolbar() {
        setSupportActionBar(toolbar);
        drawerToggle = new ActionBarDrawerToggle(this, drawer, toolbar, R.string.nav_open, R.string.nav_close);
        drawer.addDrawerListener(drawerToggle);
        toggleClickableTitle(true);
        resetDrawerToggle();
    }

    private void resetDrawerToggle() {
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
        drawerToggle.syncState();
    }

    @Override
    public void onNewIntent(Intent intent) {
        // note : using string constant for interchange btw activities & nav drawer, slightly more efficient
        // ints for within-activity tracking ... may revisit in future if creates fragility
        final String fragmentTag = intent.getStringExtra(NavigationConstants.HOME_OPEN_ON_NAV);
        if (fragmentTag == null) {
            mainFragmentFromNewIntent = -1;
        } else {
            switch (fragmentTag) {
                case NavigationConstants.ITEM_TASKS:
                    mainFragmentFromNewIntent = NavigationConstants.HOME_NAV_TASKS;
                    break;
                case NavigationConstants.ITEM_NOTIFICATIONS:
                    mainFragmentFromNewIntent = NavigationConstants.HOME_NAV_NOTIFICATIONS;
                    break;
                case NavigationConstants.ITEM_JOIN_REQS:
                    mainFragmentFromNewIntent = NavigationConstants.HOME_NAV_JOIN_REQUESTS;
                    break;
                default:
                    mainFragmentFromNewIntent = NavigationConstants.HOME_NAV_GROUPS;
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mainFragmentFromNewIntent != -1 && mainFragmentFromNewIntent != currentMainFragment) {
            switch(mainFragmentFromNewIntent) {
                case NavigationConstants.HOME_NAV_GROUPS:
                    switchToGroupFragment();
                    setNavBarToItem(NavigationConstants.ITEM_SHOW_GROUPS);
                    break;
                case NavigationConstants.HOME_NAV_TASKS:
                    switchToTasksFragment();
                    setNavBarToItem(NavigationConstants.ITEM_TASKS);
                    break;
                case NavigationConstants.HOME_NAV_JOIN_REQUESTS:
                    switchToJoinRequestsFragment();
                    setNavBarToItem(NavigationConstants.ITEM_JOIN_REQS);
                    break;
                case NavigationConstants.HOME_NAV_NOTIFICATIONS:
                    switchToNotificationFragment();
                    setNavBarToItem(NavigationConstants.ITEM_NOTIFICATIONS);
                    break;
                default:
                    // expand this if necessary so any other fragment / activity can do similar
                    break;
            }
        }
        isVisible = true;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == NavigationConstants.NETWORK_SETTINGS_DIALOG) {
            NetworkUtils.syncAndStartTasks(this, true, false);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_home_screen, menu);
        final MenuItem searchItem = menu.findItem(R.id.action_search);
        final SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchItem);
        if (searchView != null) {
            searchView.setOnQueryTextListener(this);
        }
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (!showMenuOptions) {
            return false;
        } else {
            if (menu.findItem(R.id.action_search) != null)
                menu.findItem(R.id.action_search).setVisible(true);
            return true;
        }
    }

    @Override
    protected void onDestroy() {
        EventBus.getDefault().unregister(this);
        super.onDestroy();
    }


    @Override
    protected void onPause() {
        super.onPause();
        isVisible= false;
    }

    @Override
    public void onBackPressed() {
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            if (groupListFragment != null && groupListFragment.isVisible() && groupListFragment.isFloatingMenuOpen()) {
                groupListFragment.toggleFloatingMenu();
            } else {
                super.onBackPressed();
                setTitleAndNavDrawerSelection(getCurrentFragmentTag(), true);
            }
        }
    }

    private void setNavBarToItem(String tag) {
        NavigationDrawerFragment navDrawer = (NavigationDrawerFragment) getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);
        if (navDrawer != null) {
            navDrawer.setSelectedItem(tag);
        }
    }

    @Override
    public void onNavigationDrawerItemSelected(final String tag) {
        if (drawer != null) {
            drawer.closeDrawer(Gravity.LEFT);
        }

        switch (tag) {
            case NavigationConstants.ITEM_SHOW_GROUPS:
                switchToGroupFragment();
                break;
            case NavigationConstants.ITEM_TASKS:
                switchToTasksFragment();
                break;
            case NavigationConstants.ITEM_NOTIFICATIONS:
                switchToNotificationFragment();
                break;
            case NavigationConstants.ITEM_JOIN_REQS:
                switchToJoinRequestsFragment();
                break;
            default:
                break;
        }
    }

    private void switchToGroupFragment() {
        setTitleAndNavDrawerSelection(NavigationConstants.ITEM_SHOW_GROUPS, false);

        if (groupListFragment == null) {
            groupListFragment = new HomeGroupListFragment();
        }
        showOrReplaceFragment(groupListFragment, NavigationConstants.ITEM_SHOW_GROUPS);
        currentMainFragment = NavigationConstants.HOME_NAV_GROUPS;
        invalidateOptionsMenu();
    }

    private void switchToTasksFragment() {
        setTitleAndNavDrawerSelection(NavigationConstants.ITEM_TASKS, false);
        if (taskListFragment == null) {
            taskListFragment = TaskListFragment.newInstance(null, this);
        }
        showOrReplaceFragment(taskListFragment, NavigationConstants.ITEM_TASKS);
        currentMainFragment = NavigationConstants.HOME_NAV_TASKS;
        invalidateOptionsMenu();
    }

    private void switchToNotificationFragment() {
        setTitleAndNavDrawerSelection(NavigationConstants.ITEM_NOTIFICATIONS, false);
        if (notificationCenterFragment == null) {
            notificationCenterFragment = new NotificationCenterFragment();
        }
        showOrReplaceFragment(notificationCenterFragment, NavigationConstants.ITEM_NOTIFICATIONS);
        currentMainFragment = NavigationConstants.HOME_NAV_NOTIFICATIONS;
        invalidateOptionsMenu();
    }

    private void switchToJoinRequestsFragment() {
        setTitleAndNavDrawerSelection(NavigationConstants.ITEM_JOIN_REQS, false);
        if (joinRequestsFragment == null) {
            joinRequestsFragment = new JoinRequestMasterFragment();
        }
        showOrReplaceFragment(joinRequestsFragment, NavigationConstants.ITEM_JOIN_REQS);
        currentMainFragment = NavigationConstants.HOME_NAV_JOIN_REQUESTS;
        invalidateOptionsMenu();
    }

    private void showOrReplaceFragment(Fragment fragment, String backStackTag) {
        if (fragment == null) {
            throw new UnsupportedOperationException("Error! Null fragment passed to swap");
        }

        Fragment currentMain = getSupportFragmentManager().findFragmentById(R.id.home_fragment_container);
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

        if (currentMain != null) {
            currentMain.setMenuVisibility(false);
            fragment.setMenuVisibility(true);
            if (currentMain != groupListFragment) {
                transaction.remove(currentMain);
            }
        }

        if (fragment.isAdded()) {
            transaction.show(fragment);
        } else {
            transaction.add(R.id.home_fragment_container, fragment, backStackTag);
        }

        if (!isFirstFragmentSwap) {
            transaction.addToBackStack(backStackTag);
        } else {
            isFirstFragmentSwap = false;
        }

        transaction.commit();
    }

    private String getCurrentFragmentTag() {
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.home_fragment_container);
        return (fragment != null) ? fragment.getTag() : NavigationConstants.ITEM_SHOW_GROUPS; // default fall back ... maybe use "home"?
    }

    private void setTitleAndNavDrawerSelection(String currentNavTag, boolean changeNavDrawerSelection) {
        switch (currentNavTag) {
            case NavigationConstants.ITEM_SHOW_GROUPS:
                setTitle(R.string.ghp_toolbar_title);
                break;
            case NavigationConstants.ITEM_TASKS:
                setTitle(R.string.tasks_toolbar_title);
                break;
            case NavigationConstants.ITEM_NOTIFICATIONS:
                setTitle(R.string.drawer_notis);
                break;
            case NavigationConstants.ITEM_JOIN_REQS:
                setTitle(R.string.jreq_frag_title);
                break;
        }

        if (changeNavDrawerSelection) {
            setNavBarToItem(currentNavTag);
        }
    }

    @Override
    public boolean onQueryTextChange(String query) {
        directSearchInput(query);
        return false;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
    }

    private void directSearchInput(String query) {
        final String queryString = query.toLowerCase(Locale.getDefault());
        switch (currentMainFragment) {
            case NavigationConstants.HOME_NAV_GROUPS:
                groupListFragment.searchStringChanged(queryString);
                break;
            case NavigationConstants.HOME_NAV_TASKS:
                taskListFragment.searchStringChanged(queryString);
                break;
            case NavigationConstants.HOME_NAV_NOTIFICATIONS:
                notificationCenterFragment.searchNotifications(query);
                break;
        }
    }

    @Override
    public void menuCloseClicked() {
        GroupPickFragment fragment = (GroupPickFragment) getSupportFragmentManager()
                .findFragmentByTag(GroupPickFragment.class.getCanonicalName());
        getSupportFragmentManager().beginTransaction()
                .remove(fragment)
                .commit();
    }

    @Override
    public void groupPickerTriggered(final String taskType) {
        switchActionBarToPicker();
        final String permission = PermissionUtils.permissionForTaskType(taskType);
        Fragment groupPicker = GroupPickFragment.newInstance(permission, taskType);
        getSupportFragmentManager().beginTransaction()
                .add(R.id.home_fragment_container, groupPicker, GroupPickFragment.class.getCanonicalName())
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void onGroupPicked(final Group group, final String returnTag) {
        // todo : have a "don't show this again" option
        final String message = String.format(getString(R.string.group_picker_confirm_string),
            getString(constructVerb(returnTag)), group.getGroupName());
        ConfirmCancelDialogFragment.newInstance(message,
            new ConfirmCancelDialogFragment.ConfirmDialogListener() {
                @Override public void doConfirmClicked() {
                    finishGroupPick(group, returnTag);
                }
            }).show(getSupportFragmentManager(), "confirm");
    }

    private int constructVerb(final String returnTag) {
        switch (returnTag) {
            case TaskConstants.MEETING:
                return R.string.picker_call_mtg;
            case TaskConstants.VOTE:
                return R.string.picker_call_vote;
            case TaskConstants.TODO:
                return R.string.picker_rec_action;
            default:
                throw new UnsupportedOperationException("Error! Return tag in group picker not known type");
        }
    }

    private void finishGroupPick(final Group group, final String taskType) {
        final Class activityToLaunch = TaskConstants.MEETING.equals(taskType) ? CreateMeetingActivity.class :
            TaskConstants.VOTE.equals(taskType) ? CreateVoteActivity.class : CreateTodoActivity.class;
        Fragment picker = getSupportFragmentManager().findFragmentByTag(GroupPickFragment.class.getCanonicalName());
        if (picker != null && picker.isVisible()) {
            getSupportFragmentManager().beginTransaction().remove(picker).commit();
        }
        switchActionBarOffPicker();
        Intent i = new Intent(HomeScreenActivity.this, activityToLaunch);
        i.putExtra(GroupConstants.UID_FIELD, group.getGroupUid());
        i.putExtra(GroupConstants.LOCAL_FIELD, group.getIsLocal());
        startActivity(i);
    }

    private void toggleClickableTitle(boolean clickable) {
        if (toolbarTitle == null) {
          toolbarTitle = findToolbarTitle();
        }

        // not fully confident of action bar behavior, and don't want to crash app for this, hence check again
        if (toolbarTitle != null) {
            if (clickable) {
                toolbarTitle.setClickable(true);
                toolbarTitle.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showQuickTaskModal();
                    }
                });
            } else {
                toolbarTitle.setClickable(false);
            }
        }
    }

    private TextView findToolbarTitle() {
        for (int i = 0; i < toolbar.getChildCount(); i++) {
            View child = toolbar.getChildAt(i);
            if (child instanceof TextView) {
                return (TextView) child;
            }
        }
        Log.e(TAG, "Error! Couldn't find text view in toolbar title");
        return null;
    }

    private void showQuickTaskModal() {
        QuickTaskModalFragment modal = QuickTaskModalFragment.newInstance(false, null, new QuickTaskModalFragment.TaskModalListener() {
            @Override
            public void onTaskClicked(String taskType) {
                groupPickerTriggered(taskType);
            }
        });
        modal.show(getSupportFragmentManager(), QuickTaskModalFragment.class.getSimpleName());
    }

    private void switchActionBarToPicker() {
        setTitle(R.string.home_group_pick);
        toggleClickableTitle(false);
        switchOffMenu();

        drawerToggle.setDrawerIndicatorEnabled(false);
        drawerToggle.setHomeAsUpIndicator(R.drawable.btn_close_white);
        drawerToggle.setToolbarNavigationClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getSupportFragmentManager().popBackStack();
                switchActionBarOffPicker();
            }
        });
    }

    private void switchActionBarOffPicker() {
        drawerToggle.setDrawerIndicatorEnabled(true);
        final int title = (currentMainFragment == NavigationConstants.HOME_NAV_NOTIFICATIONS) ? R.string.drawer_notis
                : (currentMainFragment == NavigationConstants.HOME_NAV_TASKS) ? R.string.tasks_toolbar_title : R.string.ghp_toolbar_title;
        setTitle(title);
        toggleClickableTitle(true);
        switchOnMenu();
    }


    @Subscribe
    public void onUserLoggedOut(UserLoggedOutEvent e) {
        // to make sure fragments, mobile number etc are destroyed and hence refreshed on subsequent login
        finish();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(GroupChatEvent groupChatEvent) {
        if (this.isVisible) {
            GcmListenerService.showNotification(groupChatEvent.getBundle()).subscribe();
        }
    }

    @Subscribe
    public void onTaskAddedEvent(TaskAddedEvent e) {
        Fragment frag = getSupportFragmentManager().findFragmentByTag(NewTaskMenuFragment.class.getCanonicalName());
        if (frag != null) {
            getSupportFragmentManager().beginTransaction()
                    .remove(frag).commit();
        }
    }

    @Override
    public void loadSingleTask(String taskName) {
        setTitle(taskName);
        toggleClickableTitle(false);
        // switchOffMenu();
        drawerToggle.setDrawerIndicatorEnabled(false);
        drawerToggle.setHomeAsUpIndicator(R.drawable.btn_close_white);
        drawerToggle.setToolbarNavigationClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeViewTaskFragment();
            }
        });
    }

    @Override
    public void loadSingleTask(String taskUid, String taskType) {

    }

    @Override
    public void onFabClicked() {
        QuickTaskModalFragment modal = QuickTaskModalFragment.newInstance(false, null,
            new QuickTaskModalFragment.TaskModalListener() {
                @Override public void onTaskClicked(String taskType) {
                    groupPickerTriggered(taskType);
                }
            });
        modal.show(getSupportFragmentManager(), QuickTaskModalFragment.class.getSimpleName());
    }

    private void closeViewTaskFragment() {
        getSupportFragmentManager().popBackStack();
        drawerToggle.setDrawerIndicatorEnabled(true);
        toggleClickableTitle(true);
        setTitle(R.string.tasks_toolbar_title);
        invalidateOptionsMenu();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(TaskCancelledEvent e) {
        Log.e(TAG, "on task cancelled event ...");
        Fragment frag = getSupportFragmentManager().findFragmentByTag(ViewTaskFragment.class.getCanonicalName());
        if (frag != null && frag.isVisible()) {
            closeViewTaskFragment();
        }
    }



    private void switchOffMenu() {
        showMenuOptions = false;
        invalidateOptionsMenu();
    }

    private void switchOnMenu() {
        showMenuOptions = true;
        invalidateOptionsMenu();
    }

}