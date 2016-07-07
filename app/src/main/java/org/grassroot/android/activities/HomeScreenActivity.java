package org.grassroot.android.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import org.grassroot.android.R;
import org.grassroot.android.events.TaskAddedEvent;
import org.grassroot.android.events.TaskCancelledEvent;
import org.grassroot.android.events.UserLoggedOutEvent;
import org.grassroot.android.fragments.GroupPickFragment;
import org.grassroot.android.fragments.HomeGroupListFragment;
import org.grassroot.android.fragments.NavigationDrawerFragment;
import org.grassroot.android.fragments.NewTaskMenuFragment;
import org.grassroot.android.fragments.NotificationCenterFragment;
import org.grassroot.android.fragments.QuickTaskModalFragment;
import org.grassroot.android.fragments.TaskListFragment;
import org.grassroot.android.fragments.ViewTaskFragment;
import org.grassroot.android.interfaces.GroupConstants;
import org.grassroot.android.interfaces.GroupPickCallbacks;
import org.grassroot.android.interfaces.NavigationConstants;
import org.grassroot.android.interfaces.TaskConstants;
import org.grassroot.android.models.Group;
import org.grassroot.android.utils.Constant;
import org.grassroot.android.utils.PermissionUtils;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import butterknife.BindView;
import butterknife.ButterKnife;

public class HomeScreenActivity extends PortraitActivity implements NavigationDrawerFragment.NavigationDrawerCallbacks,
        GroupPickCallbacks, NewTaskMenuFragment.NewTaskMenuListener, TaskListFragment.TaskListListener {

    private static final String TAG = HomeScreenActivity.class.getSimpleName();

    @BindView(R.id.home_toolbar) Toolbar toolbar;
    @BindView(R.id.drawer_layout) DrawerLayout drawer;
    private TextView toolbarTitle;

    private HomeGroupListFragment groupListFragment;
    private TaskListFragment taskListFragment;
    private NotificationCenterFragment notificationCenterFragment;

    private boolean isFirstFragmentSwap = true;

    private ActionBarDrawerToggle drawerToggle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_screen);
        ButterKnife.bind(this);
        EventBus.getDefault().register(this);
        setUpToolbar();
        switchToGroupFragment();
    }

    private void setUpToolbar() {
        setTitle(R.string.ghp_toolbar_title);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
        drawerToggle = new ActionBarDrawerToggle(this, drawer, toolbar, R.string.nav_open, R.string.nav_close);
        drawer.addDrawerListener(drawerToggle);
        drawerToggle.syncState();
        toggleClickableTitle(true);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Constant.activityNetworkSettings) {
            groupListFragment.fetchGroupList();
        } else if (resultCode == RESULT_OK && data != null) {
            // todo : swap these to using eventbus inside the fragment ...
            if (requestCode == Constant.activityAddMembersToGroup || requestCode == Constant.activityRemoveMembers) {
                int groupPosition = data.getIntExtra(Constant.INDEX_FIELD, -1);
                String groupUid = data.getStringExtra(Constant.GROUPUID_FIELD);
                groupListFragment.updateSingleGroup(groupPosition, groupUid);
            } else if (requestCode == Constant.activityCreateTask) {
                groupListFragment.showSuccessMessage(data);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_home_screen, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.mi_icon_sort:
                groupListFragment.sortGroups(); // todo : and on other fragments ...
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onDestroy() {
        EventBus.getDefault().unregister(this);
        super.onDestroy();
    }

    @Override
    public void onNavigationDrawerItemSelected(int position) {
        if (drawer != null) {
            drawer.closeDrawer(Gravity.LEFT);
        }

        switch (position) {
            case NavigationConstants.HOME_NAV_GROUPS:
                switchToGroupFragment();
                break;
            case NavigationConstants.HOME_NAV_TASKS:
                switchToTasksFragment();
                break;
            case NavigationConstants.HOME_NAV_NOTIFICATIONS:
                switchToNotificationFragment();
                break;
            default:
                break;
        }
    }

    // todo : fix lifecycle management so it doesn't call this all the time
    private void switchToGroupFragment() {
        setTitle(R.string.ghp_toolbar_title);
        if (groupListFragment == null) {
            groupListFragment = new HomeGroupListFragment();
        }
        showOrReplaceFragment(groupListFragment);
    }

    private void switchToTasksFragment() {
        setTitle(R.string.tasks_toolbar_title);
        if (taskListFragment == null) {
            taskListFragment = TaskListFragment.newInstance(null, this, this, true);
        }
        showOrReplaceFragment(taskListFragment);
    }

    private void switchToNotificationFragment() {
        setTitle(R.string.Notifications);
        if (notificationCenterFragment == null) {
            notificationCenterFragment = new NotificationCenterFragment();
        }
        showOrReplaceFragment(notificationCenterFragment);
    }

    private void showOrReplaceFragment(Fragment fragment) {
        if (fragment == null) {
            throw new UnsupportedOperationException("Error! Null fragment passed to swap");
        }

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        if (fragment.isAdded()) {
            transaction.show(fragment);
        } else {
            transaction.replace(R.id.home_fragment_container, fragment);
        }

        if (!isFirstFragmentSwap) {
            transaction.addToBackStack(null);
        } else {
            isFirstFragmentSwap = false;
        }

        transaction.commit();
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
        setTitle(R.string.home_group_pick);
        final String permission = PermissionUtils.permissionForTaskType(taskType);
        final Class activityToLaunch = TaskConstants.MEETING.equals(taskType) ? CreateMeetingActivity.class :
                TaskConstants.VOTE.equals(taskType) ? CreateVoteActivity.class : CreateTodoActivity.class;
        Fragment groupPicker = GroupPickFragment.newInstance(permission, taskType, new GroupPickFragment.GroupPickListener() {
            @Override
            public void onGroupPicked(Group group, String returnTag) {
                Fragment picker = getSupportFragmentManager().findFragmentByTag(GroupPickFragment.class.getCanonicalName());
                if (picker != null && picker.isVisible()) {
                    getSupportFragmentManager().beginTransaction().remove(picker).commit();
                }
                Intent i = new Intent(HomeScreenActivity.this, activityToLaunch);
                i.putExtra(GroupConstants.UID_FIELD, group.getGroupUid());
                startActivity(i);
            }
        });
        getSupportFragmentManager().beginTransaction()
                .add(R.id.home_fragment_container, groupPicker, GroupPickFragment.class.getCanonicalName())
                .addToBackStack(null)
                .commit();
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
        // tvTitle.setText(R.string.home_group_pick);
        // ivGhpDrawer.setImageResource(R.drawable.btn_close_white);
        toggleClickableTitle(false);
    }

    private void setActionBarToDefault() {
        setUpToolbar();
        toggleClickableTitle(true);
    }


    @Subscribe
    public void onUserLoggedOut(UserLoggedOutEvent e) {
        // to make sure fragments, mobile number etc are destroyed and hence refreshed on subsequent login
        finish();
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
    public void onTaskLoaded(String taskName) {
        // todo : plus a bunch more
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.btn_close_white);
    }

    @Subscribe
    public void onTaskCancelled(TaskCancelledEvent e) {
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.btn_navigation);
    }
}