package org.grassroot.android.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;

import org.grassroot.android.R;
import org.grassroot.android.events.GroupCreatedEvent;
import org.grassroot.android.events.UserLoggedOutEvent;
import org.grassroot.android.fragments.NavigationDrawerFragment;
import org.grassroot.android.fragments.WelcomeFragment;
import org.grassroot.android.models.PreferenceObject;
import org.grassroot.android.utils.RealmUtils;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by luke on 2016/07/07.
 */
public class NoGroupWelcomeActivity extends PortraitActivity implements WelcomeFragment.WelcomeFragmentListener,
        NavigationDrawerFragment.NavigationDrawerCallbacks {

    private static final String TAG = NoGroupWelcomeActivity.class.getSimpleName();

    private WelcomeFragment fragment;

    @BindView(R.id.drawer_layout)
    DrawerLayout drawer;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EventBus.getDefault().register(this);
        setContentView(R.layout.activity_home_nogroups);
        ButterKnife.bind(this);

        fragment = new WelcomeFragment();
        getSupportFragmentManager().beginTransaction()
                .add(R.id.fragment_container, fragment)
                .commit();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    // todo : also handle "group joined"

    @Subscribe
    public void onGroupCreated(GroupCreatedEvent e) {
        PreferenceObject object = RealmUtils.loadPreferencesFromDB();
        object.setHasGroups(true);
        RealmUtils.saveDataToRealm(object);
        // todo : may be able to remove this
        Intent goToHomeScreen = new Intent(NoGroupWelcomeActivity.this, HomeScreenActivity.class);
        goToHomeScreen.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(goToHomeScreen);
    }

    @Subscribe
    public void onUserLoggedOut(UserLoggedOutEvent e) {
        // to make sure fragments, mobile number etc are destroyed and hence refreshed on subsequent login
        finish();
    }

    @Override
    public void menuClick() { // Getting data from fragment
        if (drawer != null) drawer.openDrawer(GravityCompat.START);
    }

    @Override
    public void onNavigationDrawerItemSelected(String tag) {
        // todo : not really anything we can do, no?
    }
}
