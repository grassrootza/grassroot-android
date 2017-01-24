package org.grassroot.android.activities;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;

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

    @BindView(R.id.gextra_toolbar) Toolbar toolbar;
    @BindView(R.id.gextra_drawer_layout) DrawerLayout drawer;

    ProgressDialog progressDialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_grassroot_extra);
        ButterKnife.bind(this);

        setSupportActionBar(toolbar);
        ActionBarDrawerToggle drawerToggle = new ActionBarDrawerToggle(this, drawer, toolbar, R.string.nav_open, R.string.nav_close);
        drawer.addDrawerListener(drawerToggle);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
        }
        drawerToggle.syncState();

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
        GroupPickFragment fragment = GroupPickFragment.newInstance(true, FFORM);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.gextra_fragment_holder, fragment, "group_pick")
                .addToBackStack(null)
                .commit();
        setTitle(R.string.home_group_pick);
    }

    public void addGroupToAccount() {
        GroupPickFragment fragment = GroupPickFragment.newInstance(false, ADDTOACC);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.gextra_fragment_holder, fragment, "group_pick")
                .addToBackStack(null)
                .commit();
        setTitle(R.string.home_group_pick);

    }

    public void onGroupPicked(final Group group, String returnTag) {
        if (FFORM.equals(returnTag)) {
            String dialogBody = getString(R.string.free_form_body);
            MultiLineTextDialog.showMultiLineDialog(getSupportFragmentManager(), R.string.free_form_title,
                    dialogBody, R.string.free_form_hint, R.string.free_form_okay)
                    .subscribe(new Action1<String>() {
                        @Override
                        public void call(String s) {
                            confirmSendMessage(group);
                        }
                    });
        } else if (ADDTOACC.equals(returnTag)) {

        }
    }

    private void confirmSendMessage(Group group) {
        Log.e(TAG, "okay, we're sending this thing");
    }

    public void changeAccountType() {

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

    private void showEnabledFragment(final Account account) {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(true);
        }

        GRExtraEnabledAccountFragment fragment = GRExtraEnabledAccountFragment.newInstance(account, this);
        getSupportFragmentManager().beginTransaction()
                .add(R.id.gextra_fragment_holder, fragment, "settings")
                .commit();
    }

    private void redirectToSignup() {
        startActivity(new Intent(this, AccountSignupActivity.class));
        finish();
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
        Intent i = new Intent(this, HomeScreenActivity.class);
        i.putExtra(NavigationConstants.HOME_OPEN_ON_NAV, tag);
        startActivity(i);
    }

}
