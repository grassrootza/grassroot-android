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
import org.grassroot.android.events.UserLoggedOutEvent;
import org.grassroot.android.fragments.HomeGroupListFragment;
import org.grassroot.android.fragments.NavigationDrawerFragment;
import org.grassroot.android.fragments.NewTaskMenuFragment;
import org.grassroot.android.fragments.WelcomeFragment;
import org.grassroot.android.models.Group;
import org.grassroot.android.utils.Constant;
import org.grassroot.android.utils.MenuUtils;
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
    private NewTaskMenuFragment newTaskMenuFragment;

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

    @Override
    public void onNavigationDrawerItemSelected(int position) {
        if (drawer != null) {
            drawer.closeDrawer(Gravity.LEFT);
        }
    }

    @Override
    protected void onDestroy() {
        EventBus.getDefault().unregister(this);
        super.onDestroy();
    }

    @Override
    public void groupRowClick(Group group) {
        if (group.isHasTasks()) {
            startActivity(MenuUtils.constructIntent(this, GroupTasksActivity.class, group));
        } else {
            newTaskMenuFragment = NewTaskMenuFragment.newInstance(group, true, true);
            getSupportFragmentManager().beginTransaction()
                    .setCustomAnimations(R.anim.up_from_bottom, R.anim.down_from_top)
                    .add(R.id.drawer_layout, newTaskMenuFragment, NewTaskMenuFragment.class.getCanonicalName())
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
        newTaskMenuFragment = (NewTaskMenuFragment) getSupportFragmentManager()
                .findFragmentByTag(NewTaskMenuFragment.class.getCanonicalName());
        getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(R.anim.push_down_in, R.anim.push_down_out)
                .remove(newTaskMenuFragment)
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
}