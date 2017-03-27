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
import android.widget.Toast;

import org.grassroot.android.R;
import org.grassroot.android.events.JoinRequestEvent;
import org.grassroot.android.fragments.GiantMessageFragment;
import org.grassroot.android.fragments.GroupSearchResultsFragment;
import org.grassroot.android.fragments.GroupSearchStartFragment;
import org.grassroot.android.interfaces.NavigationConstants;
import org.grassroot.android.models.PublicGroupModel;
import org.grassroot.android.models.exceptions.ApiCallException;
import org.grassroot.android.services.ApplicationLoader;
import org.grassroot.android.services.GroupSearchService;
import org.grassroot.android.utils.ErrorUtils;
import org.grassroot.android.utils.NetworkUtils;
import org.grassroot.android.utils.RealmUtils;
import org.greenrobot.eventbus.EventBus;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Consumer;

public class GroupSearchActivity extends PortraitActivity implements GroupSearchStartFragment.GroupSearchInputListener,
    GroupSearchResultsFragment.SearchResultsListener {

    private static final String TAG = GroupSearchActivity.class.getSimpleName();

    private String currentFragmentTag;
    private static final String SEARCH = "on_search_screen";
    private static final String RESULTS = "on_results_screen";
    private static final String DONE = "on_done_message";

    private String homeActivityFragmentTag;

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

        homeActivityFragmentTag = getIntent().getStringExtra(NavigationConstants.HOME_OPEN_ON_NAV);
        Log.e(TAG, homeActivityFragmentTag == null ? "no navigation constant" : "nav constant = " + homeActivityFragmentTag);

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage(getString(R.string.wait_message));
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
                        switchToolbar(SEARCH);
                        currentFragment = startFragment;
                        currentFragmentTag = SEARCH;
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
    public void searchTriggered(String searchOption, final boolean includeTopics, final boolean geoFilter, int geoRadius) {
        Log.e(TAG, "search for group triggered, includeTopics = " + includeTopics + ", geoFilter = "
            + geoFilter + ", geoRadius = " + geoRadius);

        progressDialog.show();
        GroupSearchService.getInstance().searchForGroups(searchOption, includeTopics, geoFilter, geoRadius)
            .subscribe(new Consumer<String>() {
                @Override
                public void accept(String s) {
                    progressDialog.dismiss();
                    if (GroupSearchService.getInstance().hasResults()) {
                        switchToFragment(RESULTS, false, true);
                        switchToolbar(RESULTS);
                    } else {
                        AlertDialog.Builder builder = new AlertDialog.Builder(GroupSearchActivity.this);
                        int message = geoFilter ? R.string.find_group_none_location_on
                                : !includeTopics ? R.string.find_group_none_name_only : R.string.find_group_none_found;
                        builder.setMessage(message)
                                .setCancelable(true)
                                .create()
                                .show();
                    }
                }
            }, new Consumer<Throwable>() {
                @Override
                public void accept(Throwable e) {
                    progressDialog.dismiss();
                    if (e instanceof ApiCallException) {
                        if (NetworkUtils.CONNECT_ERROR.equals(e.getMessage())) {
                            AlertDialog.Builder builder = new AlertDialog.Builder(GroupSearchActivity.this);
                            builder.setMessage(R.string.find_group_connect_error)
                                .setPositiveButton(R.string.find_group_connect_positive, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        exitToHomeScreen();
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
                            Snackbar.make(fragmentContainer, ErrorUtils.serverErrorText(((ApiCallException) e).errorTag
                            ), Snackbar.LENGTH_SHORT).show();
                        }
                    }
                }
            });
    }

    @Override
    public void sendJoinRequest(PublicGroupModel groupModel) {
        progressDialog.show();
        final String groupName = groupModel.getGroupName();
        GroupSearchService.getInstance().sendJoinRequest(groupModel, AndroidSchedulers.mainThread())
            .subscribe(new Consumer<String>() {
                @Override
                public void accept(String s) {
                    progressDialog.dismiss();
                    showDoneMessage(groupName, true);
                }
            }, new Consumer<Throwable>() {
                @Override
                public void accept(@NonNull Throwable e) throws Exception {
                    progressDialog.dismiss();
                    Log.d(TAG, "send join request error thrown, with message ... " + e.getMessage());
                    if (e instanceof ApiCallException) {
                        if (e.getMessage().equals(NetworkUtils.CONNECT_ERROR)) {
                            showDoneMessage(groupName, false);
                        } else {
                            final String errorMessage = ErrorUtils.serverErrorText(e);
                            Snackbar.make(fragmentContainer, errorMessage, Snackbar.LENGTH_SHORT).show();
                        }
                    }
                }
            });
    }

    @Override
    public void cancelJoinRequest(PublicGroupModel groupModel) {
        progressDialog.show(); // should really switch to just a prog bar
        GroupSearchService.getInstance().cancelJoinRequest(groupModel.getId(), AndroidSchedulers.mainThread())
            .subscribe(new Consumer<String>() {
                @Override
                public void accept(String s) {
                    progressDialog.dismiss();
                    Toast.makeText(ApplicationLoader.applicationContext, R.string.gs_req_cancelled, Toast.LENGTH_SHORT).show();
                    EventBus.getDefault().post(new JoinRequestEvent(TAG));
                }
            }, new Consumer<Throwable>() {
                @Override
                public void accept(Throwable e) {
                    progressDialog.dismiss();
                    if (NetworkUtils.CONNECT_ERROR.equals(e.getMessage())) {
                        final String errorMsg = getString(R.string.gs_req_remind_cancelled_connect_error);
                        Snackbar.make(fragmentContainer, errorMsg, Snackbar.LENGTH_SHORT).show();
                    } else {
                        Snackbar.make(fragmentContainer, ErrorUtils.serverErrorText(e),
                            Snackbar.LENGTH_LONG).show();
                    }
                }
            });
    }

    @Override
    public void remindJoinRequest(PublicGroupModel groupModel) {
        progressDialog.show();
        GroupSearchService.getInstance().remindJoinRequest(groupModel.getId(), AndroidSchedulers.mainThread())
            .subscribe(new Consumer<String>() {
                @Override
                public void accept(String s) {
                    progressDialog.dismiss();
                    Toast.makeText(ApplicationLoader.applicationContext, R.string.gs_req_remind, Toast.LENGTH_SHORT).show();
                }
            }, new Consumer<Throwable>() {
                @Override
                public void accept(Throwable e) {
                    progressDialog.dismiss();
                    if (NetworkUtils.CONNECT_ERROR.equals(e.getMessage())) {
                        final String errorMsg = getString(R.string.gs_req_remind_cancelled_connect_error);
                        Snackbar.make(fragmentContainer, errorMsg, Snackbar.LENGTH_SHORT).show();
                    } else {
                        Snackbar.make(fragmentContainer, ErrorUtils.serverErrorText(e),
                            Snackbar.LENGTH_LONG).show();
                    }
                }
            });
    }

    @Override
    public void returnToSearchStart() {
        switchToFragment(SEARCH, false, true);
        switchToolbar(SEARCH);
    }

    private void showDoneMessage(final String groupName, boolean onLineSent) {
        final int header = onLineSent ? R.string.fgroup_jreq_sent_title_online :
            R.string.fgroup_jreq_sent_title_offline;
        final String body = String.format(getString(onLineSent ?
            R.string.fgroup_jreq_sent_body_online : R.string.fgroup_jreq_sent_body_offline), groupName);

        GiantMessageFragment.Builder builder = new GiantMessageFragment.Builder(header)
            .setBody(body)
            .setButtonOne(R.string.find_group_search_again, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (startFragment != null) {
                    startFragment.setSearchText("");
                }
                switchToFragment(SEARCH, true, false);
            }
        });

        GiantMessageFragment giantMessageFragment = builder.build();
        currentFragment = giantMessageFragment;
        getSupportFragmentManager().beginTransaction()
            .add(R.id.find_group_fragment_container, giantMessageFragment) // make sure fragment is killed after
            .addToBackStack(null)
            .commit();
        currentFragmentTag = DONE;
        switchToolbar(DONE);
        EventBus.getDefault().post(new JoinRequestEvent(TAG));
    }

    private void exitToHomeScreen() {
        Intent upIntent;
        if (RealmUtils.loadPreferencesFromDB().isHasGroups()) {
            upIntent = new Intent(this, HomeScreenActivity.class);
            if (homeActivityFragmentTag != null) {
                upIntent.putExtra(NavigationConstants.HOME_OPEN_ON_NAV, homeActivityFragmentTag);
            }
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