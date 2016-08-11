package org.grassroot.android.activities;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.util.Log;

import org.grassroot.android.R;
import org.grassroot.android.fragments.GroupSearchResultsFragment;
import org.grassroot.android.fragments.GroupSearchStartFragment;
import org.grassroot.android.fragments.NavigationDrawerFragment;
import org.grassroot.android.services.GroupSearchService;

import butterknife.BindView;
import butterknife.ButterKnife;
import rx.Subscriber;

public class GroupSearchActivity extends PortraitActivity implements GroupSearchStartFragment.GroupSearchInputListener,
    NavigationDrawerFragment.NavigationDrawerCallbacks {

    private static final String TAG = GroupSearchActivity.class.getSimpleName();

    @BindView(R.id.find_group_toolbar) Toolbar toolbar;

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
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.btn_close_white);
        }

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage(getString(R.string.txt_pls_wait));
        progressDialog.setIndeterminate(true);

        startFragment = new GroupSearchStartFragment();
        getSupportFragmentManager().beginTransaction()
            .add(R.id.find_group_fragment_container, startFragment)
            .commit();
    }

    @Override
    public void searchTriggered(String searchOption, boolean includeTopics, boolean geoFilter, int geoRadius) {
        Log.e(TAG, "search for group triggered, includeTopics = " + includeTopics + ", geoFilter = "
            + geoFilter + ", geoRadius = " + geoRadius);
        progressDialog.show();
        GroupSearchService.getInstance().searchForGroups(searchOption, true).subscribe(new Subscriber<String>() {
            @Override
            public void onNext(String s) {
                progressDialog.dismiss();
                FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                if (resultsFragment == null || !resultsFragment.isAdded()) {
                    transaction.add(R.id.find_group_fragment_container, resultsFragment);
                } else {
                    transaction.show(resultsFragment);
                }
                transaction
                    .addToBackStack(null)
                    .setTransitionStyle(R.style.animation_fast_flyinout)
                    .commit();

                switchToolbar(true);
            }

            @Override
            public void onError(Throwable e) {
                progressDialog.dismiss();
                // todo : add error dialog
            }

            @Override
            public void onCompleted() { }
        });
    }

    private void switchToolbar(boolean isViewingSearchResults) {
        if (isViewingSearchResults) {
            setTitle(R.string.find_group_results_title);
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.btn_back_wt);
        } else {
            setTitle(R.string.find_group_title);
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.btn_close_white);
        }
    }

    @Override
    public void onNavigationDrawerItemSelected(String tag) {
        // todo : maybe get rid of this ...
        Log.e(TAG, "uh ... pass it back to main activity?");
    }
}