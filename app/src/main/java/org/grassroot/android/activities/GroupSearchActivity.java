package org.grassroot.android.activities;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;

import org.grassroot.android.R;
import org.grassroot.android.fragments.GroupSearchResultsFragment;
import org.grassroot.android.fragments.GroupSearchStartFragment;
import org.grassroot.android.services.GroupSearchService;

import butterknife.BindView;
import butterknife.ButterKnife;
import rx.Subscriber;

public class GroupSearchActivity extends PortraitActivity
    implements GroupSearchStartFragment.GroupSearchInputListener {

    private static final String TAG = GroupSearchActivity.class.getSimpleName();

    @BindView(R.id.find_group_toolbar) Toolbar toolbar;
    @BindView(R.id.navigation_drawer) DrawerLayout drawer;
    private ActionBarDrawerToggle drawerToggle;

    ProgressDialog progressDialog;

    GroupSearchStartFragment startFragment;
    GroupSearchResultsFragment resultsFragment;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_search);
        ButterKnife.bind(this);

        setTitle(R.string.find_group_title);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
        }
        drawerToggle = new ActionBarDrawerToggle(this, drawer, toolbar, R.string.nav_open, R.string.nav_close);
        drawer.addDrawerListener(drawerToggle);
        drawerToggle.syncState();

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage(getString(R.string.txt_pls_wait));
        progressDialog.setIndeterminate(true);

        startFragment = new GroupSearchStartFragment();
        getSupportFragmentManager().beginTransaction()
            .add(R.id.find_group_fragment_container, startFragment)
            .commit();
    }

    @Override
    public void searchTriggered(String searchOption, boolean geoFilter, int geoRadius, boolean includeTopics) {
        GroupSearchService.getInstance().searchForGroups(searchOption, true).subscribe(new Subscriber<String>() {
            @Override
            public void onNext(String s) {
                FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                if (resultsFragment == null || !resultsFragment.isAdded()) {
                    transaction.add(R.id.find_group_fragment_container, resultsFragment);
                } else {
                    transaction.show(resultsFragment);
                }
                transaction
                    .addToBackStack(null)
                    .commit();
            }

            @Override
            public void onError(Throwable e) {
                // todo : add error dialog
            }

            @Override
            public void onCompleted() { }
        });
    }

}