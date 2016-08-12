package org.grassroot.android.activities;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.NavUtils;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.ViewGroup;

import org.grassroot.android.R;
import org.grassroot.android.fragments.GiantMessageFragment;
import org.grassroot.android.fragments.GroupSearchResultsFragment;
import org.grassroot.android.fragments.GroupSearchStartFragment;
import org.grassroot.android.fragments.NavigationDrawerFragment;
import org.grassroot.android.models.PublicGroupModel;
import org.grassroot.android.models.exceptions.ApiCallException;
import org.grassroot.android.services.GroupSearchService;
import org.grassroot.android.utils.ErrorUtils;
import org.grassroot.android.utils.NetworkUtils;

import butterknife.BindView;
import butterknife.ButterKnife;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;

public class GroupSearchActivity extends PortraitActivity implements GroupSearchStartFragment.GroupSearchInputListener,
    GroupSearchResultsFragment.SearchResultsListener {

    private static final String TAG = GroupSearchActivity.class.getSimpleName();

    private String currentFragment;
    private static final String SEARCH = "on_search_screen";
    private static final String RESULTS = "on_results_screen";
    private static final String DONE = "on_done_message";

    @BindView(R.id.find_group_toolbar) Toolbar toolbar;
    @BindView(R.id.find_group_fragment_container) ViewGroup fragmentContainer;

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
        currentFragment = SEARCH;
        getSupportFragmentManager().beginTransaction()
            .add(R.id.find_group_fragment_container, startFragment)
            .commit();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                switch (currentFragment) {
                    case SEARCH:
                        NavUtils.navigateUpFromSameTask(this);
                        break;
                    case RESULTS:
                        getSupportFragmentManager().popBackStack();
                        break;
                    case DONE:
                        if (GroupSearchService.getInstance().hasResults()) {
                            // open results fragment
                        } else {
                            // open search fragment
                        }
                        break;
                }
                return true;
        }
        return super.onOptionsItemSelected(item);
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
                    Log.d(TAG, "adding results fragment ...");
                    resultsFragment = new GroupSearchResultsFragment();
                    transaction.add(R.id.find_group_fragment_container, resultsFragment);
                } else {
                    Log.d(TAG, "showing results fragment ...");
                    transaction.show(resultsFragment);
                }

                transaction.addToBackStack(null).commit();
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

    @Override
    public void sendJoinRequest(PublicGroupModel groupModel) {
        progressDialog.show();
        final String groupName = groupModel.getGroupName();
        GroupSearchService.getInstance().sendJoinRequest(groupModel, AndroidSchedulers.mainThread())
            .subscribe(new Subscriber<String>() {
                @Override
                public void onNext(String s) {
                    progressDialog.dismiss();
                    showDoneMessage(groupName, true);
                }

                @Override
                public void onError(Throwable e) {
                    progressDialog.dismiss();
                    if (e instanceof ApiCallException) {
                        if (e.getMessage().equals(NetworkUtils.CONNECT_ERROR)) {
                            showDoneMessage(groupName, false);
                        } else {
                            final String errorMessage = ErrorUtils.serverErrorText(e, GroupSearchActivity.this);
                            Snackbar.make(fragmentContainer, errorMessage, Snackbar.LENGTH_SHORT).show();
                        }
                    }
                }

                @Override
                public void onCompleted() { }
            });
    }

    private void showDoneMessage(final String groupName, boolean onLineSent) {
        final int header = onLineSent ? R.string.fgroup_jreq_sent_title_online :
            R.string.fgroup_jreq_sent_title_offline;
        final String body = String.format(getString(onLineSent ?
            R.string.fgroup_jreq_sent_body_online : R.string.fgroup_jreq_sent_body_offline), groupName);

        GiantMessageFragment giantMessageFragment = GiantMessageFragment
            .newInstance(header, body, false, false);
        getSupportFragmentManager().beginTransaction()
            .add(R.id.find_group_fragment_container, giantMessageFragment) // make sure fragment is killed after
            .addToBackStack(null)
            .commit();
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
}