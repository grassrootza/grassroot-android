package org.grassroot.android.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.Gravity;

import org.grassroot.android.R;
import org.grassroot.android.events.GroupCreatedEvent;
import org.grassroot.android.events.TaskAddedEvent;
import org.grassroot.android.events.UserLoggedOutEvent;
import org.grassroot.android.fragments.GroupPickFragment;
import org.grassroot.android.fragments.HomeGroupListFragment;
import org.grassroot.android.fragments.NavigationDrawerFragment;
import org.grassroot.android.fragments.NewTaskMenuFragment;
import org.grassroot.android.fragments.TaskListFragment;
import org.grassroot.android.fragments.WelcomeFragment;
import org.grassroot.android.interfaces.GroupConstants;
import org.grassroot.android.interfaces.NavigationConstants;
import org.grassroot.android.interfaces.TaskConstants;
import org.grassroot.android.models.Group;
import org.grassroot.android.utils.Constant;
import org.grassroot.android.utils.PermissionUtils;
import org.grassroot.android.utils.PreferenceUtils;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import butterknife.BindView;
import butterknife.ButterKnife;

public class HomeScreenActivity extends PortraitActivity implements NavigationDrawerFragment.NavigationDrawerCallbacks,
        WelcomeFragment.WelcomeFragmentListener, HomeGroupListFragment.GroupListFragmentListener, NewTaskMenuFragment.NewTaskMenuListener {

    private static final String TAG = HomeScreenActivity.class.getCanonicalName();

    @BindView(R.id.drawer_layout)
    DrawerLayout drawer;

    private Fragment mainFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_homescreen);
        ButterKnife.bind(this);
        EventBus.getDefault().register(this);
        setUpHomeFragment();
    }

    private void setUpHomeFragment() {
        mainFragment = PreferenceUtils.userHasGroups(this) ? new HomeGroupListFragment() : new WelcomeFragment();
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, mainFragment)
                .commit();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "onActivityResults, request_code = " + requestCode + ", result code = " + resultCode);
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Constant.activityNetworkSettings) {
            HomeGroupListFragment hgl = (HomeGroupListFragment) mainFragment;
            hgl.fetchGroupList();
        } else if (resultCode == RESULT_OK && data != null) {
            // todo : swap these to using eventbus inside the fragment ...
            if (requestCode == Constant.activityAddMembersToGroup || requestCode == Constant.activityRemoveMembers) {
                int groupPosition = data.getIntExtra(Constant.INDEX_FIELD, -1);
                String groupUid = data.getStringExtra(Constant.GROUPUID_FIELD);
                HomeGroupListFragment hgl = (HomeGroupListFragment) mainFragment;
                hgl.updateSingleGroup(groupPosition, groupUid);
            } else if (requestCode == Constant.activityCreateTask) {
                HomeGroupListFragment hgl = (HomeGroupListFragment) mainFragment;
                hgl.showSuccessMessage(data);
            }
        }
    }

    // todo : ah, fix the nav drawer. all of the nav drawer
    @Override
    public void onResume() {
        super.onResume();
        Log.e(TAG, "resuming home activity");
        Fragment fragment = getSupportFragmentManager().findFragmentByTag("navigation_drawer");
        if (fragment != null) {
            Log.e(TAG, "found the drawer fragment!");
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

        // todo : probably use a view pager for this
        if (position == NavigationConstants.HOME_NAV_TASKS) {
            Log.e(TAG, "triggering task list fragment ...");
            Fragment tasksFragment = TaskListFragment.newInstance(null);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, tasksFragment, TaskListFragment.class.getCanonicalName())
                    .addToBackStack(null)
                    .commit();
        }
    }

    @Override
    public void menuClick() { // Getting data from fragment
        if (drawer != null) drawer.openDrawer(GravityCompat.START);
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
                Intent i = new Intent(HomeScreenActivity.this, activityToLaunch);
                i.putExtra(GroupConstants.UID_FIELD, group.getGroupUid());
                startActivity(i);
            }
        });
        getSupportFragmentManager().beginTransaction()
                .add(R.id.fl_main_body, groupPicker, GroupPickFragment.class.getCanonicalName())
                .addToBackStack(null)
                .commit();
    }

    @Subscribe
    public void onGroupCreated(GroupCreatedEvent e) {
        Log.e(TAG, "group created! home activity triggered");
        if (mainFragment instanceof WelcomeFragment) {
            // todo : show a "group created or similar", and do this more robustly in general (right now causes duplicate cards)
            PreferenceUtils.setUserHasGroups(this, true);
            mainFragment = new HomeGroupListFragment();
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, mainFragment)
                    .commitAllowingStateLoss();
        }
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

}