package org.grassroot.android.activities;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import org.grassroot.android.R;
import org.grassroot.android.adapters.GroupPickAdapter;
import org.grassroot.android.fragments.GRExtraEnabledAccountFragment;
import org.grassroot.android.fragments.GiantMessageFragment;
import org.grassroot.android.fragments.GroupPickFragment;
import org.grassroot.android.fragments.NavigationDrawerFragment;
import org.grassroot.android.fragments.dialogs.MultiLineTextDialog;
import org.grassroot.android.interfaces.NavigationConstants;
import org.grassroot.android.models.Account;
import org.grassroot.android.models.Group;
import org.grassroot.android.models.responses.AccountResponse;
import org.grassroot.android.models.responses.GenericResponse;
import org.grassroot.android.services.GrassrootRestService;
import org.grassroot.android.utils.NetworkUtils;
import org.grassroot.android.utils.RealmUtils;

import butterknife.BindView;
import butterknife.ButterKnife;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import rx.functions.Action1;

/**
 * Created by luke on 2017/01/11.
 */

public class GrassrootExtraActivity extends PortraitActivity implements NavigationDrawerFragment.NavigationDrawerCallbacks,
        GRExtraEnabledAccountFragment.GrExtraListener, GroupPickAdapter.GroupPickAdapterListener {

    private static final String TAG = GrassrootExtraActivity.class.getSimpleName();

    private static final String FFORM = "FREEFORM";
    private static final String ADDTOACC = "ADDTOACCCOUNT";

    private String accountUid;
    private int groupsLeft;
    private int messagesLeft;

    private GRExtraEnabledAccountFragment mainFragment;
    private GroupPickFragment pickFragment;
    private ActionBarDrawerToggle drawerToggle;

    @BindView(R.id.gextra_toolbar) Toolbar toolbar;
    @BindView(R.id.gextra_drawer_layout) DrawerLayout drawer;

    ProgressDialog progressDialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_grassroot_extra);
        ButterKnife.bind(this);

        setSupportActionBar(toolbar);
        drawerToggle = new ActionBarDrawerToggle(this, drawer, toolbar, R.string.nav_open, R.string.nav_close);
        drawer.addDrawerListener(drawerToggle);
        setActionBarToMain();

        NavigationDrawerFragment navDrawer = (NavigationDrawerFragment) getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);
        if (navDrawer != null) {
            navDrawer.clearSelected(); // sometimes retains selection shade on groups for some reason
        }

        showProgress();
        if (NetworkUtils.isNetworkAvailable(this)) {
            GrassrootRestService.getInstance().getApi().getGrassrootExtraSettings(RealmUtils.loadPreferencesFromDB().getMobileNumber(),
                    RealmUtils.loadPreferencesFromDB().getToken()).enqueue(new Callback<AccountResponse>() {
                @Override
                public void onResponse(Call<AccountResponse> call, Response<AccountResponse> response) {
                    hideProgress();
                    Log.e(TAG, "response body: " + response.body().getMessage());
                    Account account = response.body().getAccount();
                    if (account == null) {
                        redirectToSignup();
                    } else if (account.isEnabled()) {
                        accountUid = account.getUid();
                        messagesLeft = account.getMessagesLeft();
                        groupsLeft = account.getGroupsLeft();
                        showEnabledFragment(account);
                    } else {
                        Log.e(TAG, "disabled!");
                    }
                }

                @Override
                public void onFailure(Call<AccountResponse> call, Throwable t) {
                    hideProgress();
                    showNotConnectedMsg();
                }
            });
        } else {
            showNotConnectedMsg();
        }
    }

    public void sendFreeFormMessage() {
        if (messagesLeft > 0) {
            openPickFragment(true, FFORM);
        } else {
            showLimitDialog(getString(R.string.free_form_noneleft));
        }
    }

    public void addGroupToAccount() {
        if (groupsLeft > 0) {
            openPickFragment(false, ADDTOACC);
        } else {
            showLimitDialog(getString(R.string.add_group_nospace));
        }
    }

    public void onGroupPicked(final Group group, String returnTag) {
        if (FFORM.equals(returnTag)) {
            String dialogBody = getString(R.string.free_form_body);
            MultiLineTextDialog.showMultiLineDialog(getSupportFragmentManager(), R.string.free_form_title,
                    dialogBody, R.string.free_form_hint, R.string.free_form_okay)
                    .subscribe(new Action1<String>() {
                        @Override
                        public void call(String s) {
                            confirmSendMessage(group.getGroupUid(), s);
                        }
                    });
        } else if (ADDTOACC.equals(returnTag)) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this)
                    .setMessage(getString(R.string.add_group_confirm, groupsLeft - 1))
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            addGroupToAccount(group.getGroupUid());
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.dismiss();
                        }
                    })
                    .setCancelable(true);
            builder.show();
        }
    }

    private void addGroupToAccount(final String groupUid) {
        showProgress();
        final String phoneNumber = RealmUtils.loadPreferencesFromDB().getMobileNumber();
        final String token = RealmUtils.loadPreferencesFromDB().getToken();
        GrassrootRestService.getInstance().getApi().addGroupToAccount(phoneNumber, token, accountUid, groupUid)
                .enqueue(new Callback<GenericResponse>() {
                    @Override
                    public void onResponse(Call<GenericResponse> call, Response<GenericResponse> response) {
                        // todo : switch group to paid for locally
                        hideProgress();
                        if (response.isSuccessful()) {
                            groupsLeft--;
                            closePickFragment();
                            showEnabledFragment(null);
                            Toast.makeText(GrassrootExtraActivity.this, R.string.add_group_done, Toast.LENGTH_SHORT).show();
                        } else {
                            Log.e(TAG, "oops, went wrong");
                        }
                    }

                    @Override
                    public void onFailure(Call<GenericResponse> call, Throwable t) {
                        // todo : show an error message
                        hideProgress();
                    }
                });
    }

    private void confirmSendMessage(String grouUid, String message) {
        Log.e(TAG, "okay, we're sending this thing");
        showProgress();
        final String phoneNumber = RealmUtils.loadPreferencesFromDB().getMobileNumber();
        final String token = RealmUtils.loadPreferencesFromDB().getToken();
        GrassrootRestService.getInstance().getApi().sendFreeForm(phoneNumber, token, accountUid, grouUid, message)
                .enqueue(new Callback<GenericResponse>() {
                    @Override
                    public void onResponse(Call<GenericResponse> call, Response<GenericResponse> response) {
                        hideProgress();
                        if (response.isSuccessful()) {
                            closePickFragment();
                            showEnabledFragment(null);
                            Toast.makeText(GrassrootExtraActivity.this, R.string.free_form_done, Toast.LENGTH_SHORT).show();
                        } else {
                            Log.e(TAG, "okay that didn't work ...");
                        }
                    }

                    @Override
                    public void onFailure(Call<GenericResponse> call, Throwable t) {
                        // todo : fix this
                        hideProgress();
                    }
                });
    }

    public void changeAccountType() {

    }

    private void showEnabledFragment(final Account account) {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(true);
        }

        if (mainFragment == null && account != null) {
            mainFragment = GRExtraEnabledAccountFragment.newInstance(account, this);
        }

        getSupportFragmentManager().beginTransaction()
                .add(R.id.gextra_fragment_holder, mainFragment, "settings")
                .commit();
    }

    private void redirectToSignup() {
        startActivity(new Intent(this, AccountSignupActivity.class));
        finish();
    }

    private void openPickFragment(final boolean paidFor, final String returnTag) {
        pickFragment = GroupPickFragment.newInstance(paidFor, returnTag);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.gextra_fragment_holder, pickFragment, "group_pick")
                .addToBackStack(null)
                .commit();
        setTitle(R.string.home_group_pick);
        switchActionBarToClose();
    }

    private void closePickFragment() {
        if (pickFragment != null) {
            getSupportFragmentManager().beginTransaction()
                    .remove(pickFragment)
                    .commit();
        }
        setActionBarToMain();
        invalidateOptionsMenu();
    }

    private void setActionBarToMain() {
        setTitle(R.string.title_activity_grassroot_extra);
        drawerToggle.setDrawerIndicatorEnabled(true);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
        }
        drawerToggle.syncState();
    }

    private void switchActionBarToClose() {
        drawerToggle.setDrawerIndicatorEnabled(false);
        drawerToggle.setHomeAsUpIndicator(R.drawable.btn_close_white);
        drawerToggle.setToolbarNavigationClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getSupportFragmentManager().popBackStack();
                setActionBarToMain();
                invalidateOptionsMenu();
            }
        });
    }

    private void showLimitDialog(final String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setMessage(message)
                .setPositiveButton(R.string.account_upgrade, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        changeAccountType();
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                })
                .setCancelable(true);
        builder.show();
    }

    private void showNotConnectedMsg() {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        GiantMessageFragment messageFragment = new GiantMessageFragment.Builder(R.string.gextra_connect_header)
                .setBody(getString(R.string.gextra_connect_body))
                .setButtonOne(R.string.gextra_connect_try, new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {

                    }
                })
                .setButtonTwo(R.string.gextra_connect_abort, new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {

                    }
                })
                .showHomeButton(false)
                .build();

        getSupportFragmentManager().beginTransaction()
                .add(R.id.gextra_fragment_holder, messageFragment, "message")
                .commit();
    }

    @Override
    public void onNavigationDrawerItemSelected(String tag) {
        if (drawer != null) {
            drawer.closeDrawer(GravityCompat.START);
        }
        Log.e(TAG, "returning to home screen with tag: " + tag);
        Intent i = new Intent(this, HomeScreenActivity.class);
        i.putExtra(NavigationConstants.HOME_OPEN_ON_NAV, tag);
        startActivity(i);
    }

    private void showProgress() {
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(this);
            progressDialog.setIndeterminate(true);
            progressDialog.setMessage(getString(R.string.wait_message));
        }
        progressDialog.show();
    }

    private void hideProgress() {
        if (progressDialog != null) {
            progressDialog.dismiss();
        }
    }

}
