package org.grassroot.android.activities;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import org.grassroot.android.R;
import org.grassroot.android.fragments.GiantMessageFragment;
import org.grassroot.android.fragments.GroupSearchResultsFragment;
import org.grassroot.android.fragments.GroupSearchStartFragment;
import org.grassroot.android.models.PublicGroupModel;
import org.grassroot.android.models.exceptions.ApiCallException;
import org.grassroot.android.services.GroupSearchService;
import org.grassroot.android.utils.ErrorUtils;
import org.grassroot.android.utils.NetworkUtils;
import org.grassroot.android.utils.RealmUtils;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;

public class GroupSearchActivity extends PortraitActivity implements GroupSearchStartFragment.GroupSearchInputListener,
    GroupSearchResultsFragment.SearchResultsListener {

    private static final String TAG = GroupSearchActivity.class.getSimpleName();

    private String currentFragmentTag;
    private static final String SEARCH = "on_search_screen";
    private static final String RESULTS = "on_results_screen";
    private static final String DONE = "on_done_message";

    @BindView(R.id.find_group_toolbar) Toolbar toolbar;
    @BindView(R.id.find_group_fragment_container) ViewGroup fragmentContainer;

    ProgressDialog progressDialog;

    Fragment currentFragment;
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

        switchToFragment(SEARCH, false, false);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                switch (currentFragmentTag) {
                    case SEARCH:
                        exitToHomeScreen();
                        break;
                    case RESULTS:
                        getSupportFragmentManager().popBackStack();
                        break;
                    case DONE:
                        if (GroupSearchService.getInstance().hasResults()) {
                            switchToFragment(RESULTS, true, false);
                        } else {
                            switchToFragment(SEARCH, true, false);
                        }
                        break;
                }
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void switchToFragment(final String fragmentToOpen, boolean removeCurrent, boolean addToBackStack) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        if (removeCurrent && currentFragment != null && currentFragment.isAdded()) {
            transaction.remove(currentFragment);
        }

        switch (fragmentToOpen) {
            case SEARCH:
                if (startFragment == null) {
                    startFragment = new GroupSearchStartFragment();
                }
                if (!startFragment.isAdded()) {
                    transaction.add(R.id.find_group_fragment_container, startFragment);
                } else {
                    // distrust how Android handles fragment show/hide/replace, hence
                    if (resultsFragment != null && resultsFragment.isAdded()) {
                        transaction.hide(resultsFragment);
                    }
                    transaction.show(startFragment);
                }
                currentFragment = startFragment;
                currentFragmentTag = SEARCH;
                switchToolbar(SEARCH);
                break;
            case RESULTS:
                if (resultsFragment == null) {
                    resultsFragment = new GroupSearchResultsFragment();
                } else {
                    resultsFragment.refreshResultsList();
                }
                if (!resultsFragment.isAdded()) {
                    transaction.add(R.id.find_group_fragment_container, resultsFragment);
                } else {
                    transaction.show(resultsFragment);
                }
                currentFragment = resultsFragment;
                currentFragmentTag = RESULTS;
                switchToolbar(RESULTS);
                break;
        }

        if (addToBackStack) {
            transaction.addToBackStack(null);
        }
        transaction.commit();
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
                if (GroupSearchService.getInstance().hasResults()) {
                    switchToFragment(RESULTS, false, true);
                    switchToolbar(RESULTS);
                } else {
                    AlertDialog.Builder builder = new AlertDialog.Builder(GroupSearchActivity.this);
                    builder.setMessage(R.string.find_group_none_found)
                        .setCancelable(true)
                        .create()
                        .show();
                }
            }

            @Override
            public void onError(Throwable e) {
                progressDialog.dismiss();
                if (e instanceof ApiCallException) {
                    if (NetworkUtils.CONNECT_ERROR.equals(e.getMessage())) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(GroupSearchActivity.this);
                        builder.setMessage(R.string.find_group_connect_error)
                            .setPositiveButton(R.string.find_group_connect_positive, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    exitUp();
                                }
                            })
                            .setNegativeButton(R.string.find_group_connect_negative, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.cancel();
                                }
                            });
                        builder.create().show();
                    } else if (NetworkUtils.SERVER_ERROR.equals(e.getMessage())) {
                        Snackbar.make(fragmentContainer, ErrorUtils.serverErrorText(((ApiCallException) e).errorTag,
                            GroupSearchActivity.this), Snackbar.LENGTH_SHORT).show();
                    }
                }
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
                    Log.e(TAG, "send join request error thown, of type ... " + e.getMessage());
                    if (e instanceof ApiCallException) {
                        if (e.getMessage().equals(NetworkUtils.CONNECT_ERROR)) {
                            Log.e(TAG, "okay, it's a network connection error ...");
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

    @Override
    public void returnToSearchStart() {
        switchToFragment(SEARCH, false, true);
        switchToolbar(SEARCH);
    }

    @Override
    public void exitUp() {
        exitToHomeScreen();
    }

    private void showDoneMessage(final String groupName, boolean onLineSent) {
        final int header = onLineSent ? R.string.fgroup_jreq_sent_title_online :
            R.string.fgroup_jreq_sent_title_offline;
        final String body = String.format(getString(onLineSent ?
            R.string.fgroup_jreq_sent_body_online : R.string.fgroup_jreq_sent_body_offline), groupName);

        GiantMessageFragment giantMessageFragment = GiantMessageFragment
            .newInstance(header, body, true, false);

        giantMessageFragment.setButtonOne(R.string.find_group_search_again, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (startFragment != null) {
                    startFragment.setSearchText("");
                }
                switchToFragment(SEARCH, true, false);
            }
        });

        currentFragment = giantMessageFragment;
        getSupportFragmentManager().beginTransaction()
            .add(R.id.find_group_fragment_container, giantMessageFragment) // make sure fragment is killed after
            .addToBackStack(null)
            .commit();
        currentFragmentTag = DONE;
        switchToolbar(DONE);
    }

    private void exitToHomeScreen() {
        Intent upIntent;
        if (RealmUtils.loadPreferencesFromDB().isHasGroups()) { // todo : may want to just check DB count ...
            upIntent = new Intent(this, HomeScreenActivity.class);
            // todo : include bundle about where they were ...
        } else {
            upIntent = new Intent(this, NoGroupWelcomeActivity.class);
        }
        startActivity(upIntent);
    }

    private void switchToolbar(final String fragmentTag) {
        switch(fragmentTag) {
            case SEARCH:
                setTitle(R.string.find_group_title);
                getSupportActionBar().setHomeAsUpIndicator(R.drawable.btn_close_white);
                break;
            case RESULTS:
                setTitle(R.string.find_group_results_title);
                getSupportActionBar().setHomeAsUpIndicator(R.drawable.btn_back_wt);
                break;
            case DONE:
                setTitle("");
                getSupportActionBar().setHomeAsUpIndicator(R.drawable.btn_close_white);
                break;
        }
    }
}