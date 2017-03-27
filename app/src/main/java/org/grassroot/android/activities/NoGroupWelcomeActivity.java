package org.grassroot.android.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import org.grassroot.android.R;
import org.grassroot.android.events.GroupCreatedEvent;
import org.grassroot.android.events.UserLoggedOutEvent;
import org.grassroot.android.fragments.NavigationDrawerFragment;
import org.grassroot.android.fragments.WelcomeFragment;
import org.grassroot.android.models.PreferenceObject;
import org.grassroot.android.services.ApplicationLoader;
import org.grassroot.android.services.GroupService;
import org.grassroot.android.utils.NetworkUtils;
import org.grassroot.android.utils.RealmUtils;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;

/**
 * Created by luke on 2016/07/07.
 */
public class NoGroupWelcomeActivity extends PortraitActivity implements
    NavigationDrawerFragment.NavigationDrawerCallbacks {

    private static final String TAG = NoGroupWelcomeActivity.class.getSimpleName();

    private WelcomeFragment fragment;

    @BindView(R.id.toolbar) Toolbar toolbar;
    @BindView(R.id.drawer_layout) DrawerLayout drawer;
    @BindView(R.id.no_group_swipe_refresh) SwipeRefreshLayout refreshLayout;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EventBus.getDefault().register(this);
        setContentView(R.layout.activity_home_nogroups);
        ButterKnife.bind(this);
        setUpToolbar();

        fragment = new WelcomeFragment();
        getSupportFragmentManager().beginTransaction()
                .add(R.id.fragment_container, fragment)
                .commit();

        refreshLayout.setColorSchemeColors(ContextCompat.getColor(this, R.color.primaryColor));
        refreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refreshLayout.setRefreshing(true);
                checkIfStillNoGroups();
            }
        });
    }

    private void setUpToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        // todo : replace later with drawer toggle
        toolbar.setNavigationIcon(ContextCompat.getDrawable(this, R.drawable.btn_navigation));
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (drawer != null) drawer.openDrawer(GravityCompat.START);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_welc_no_groups, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.no_groups_refresh) {
            refreshLayout.setRefreshing(true);
            checkIfStillNoGroups();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    private void checkIfStillNoGroups() {
        GroupService.getInstance().fetchGroupList(AndroidSchedulers.mainThread())
            .subscribe(new Observer<String>() {
                @Override public void onSubscribe(Disposable d) { }

                @Override
                public void onComplete() { }

                @Override
                public void onError(Throwable e) {
                    // means a wholly unexpected error, show a generic snackbar
                    refreshLayout.setRefreshing(false);
                    Snackbar.make(refreshLayout, R.string.welcome_snackbar_connect_error, Snackbar.LENGTH_SHORT).show();
                }

                @Override
                public void onNext(String s) {
                    refreshLayout.setRefreshing(false);
                    if (NetworkUtils.FETCHED_SERVER.equals(s) && RealmUtils.countGroupsInDB() > 0) {
                        Toast.makeText(ApplicationLoader.applicationContext, R.string.welcome_toast_groups,
                            Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(NoGroupWelcomeActivity.this, HomeScreenActivity.class));
                    } else if (NetworkUtils.FETCHED_SERVER.equals(s) && RealmUtils.countGroupsInDB() == 0) {
                        Snackbar.make(refreshLayout, R.string.welcome_snackbar_refresh_no_groups, Snackbar.LENGTH_SHORT).show();
                    } else {
                        Snackbar.make(refreshLayout, R.string.welcome_snackbar_connect_error, Snackbar.LENGTH_SHORT).show();
                    }
                }
        });
    }

    @Subscribe
    public void onGroupCreated(GroupCreatedEvent e) {
        PreferenceObject object = RealmUtils.loadPreferencesFromDB();
        object.setHasGroups(true);
        RealmUtils.saveDataToRealmWithSubscriber(object);
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
    public void onNavigationDrawerItemSelected(String tag) {
        // nothing to do (will not even be passed through, but required for listener/class consistency)
    }
}
