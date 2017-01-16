package org.grassroot.android.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;

import org.grassroot.android.R;
import org.grassroot.android.fragments.AccountTypeFragment;
import org.grassroot.android.fragments.GiantMessageFragment;
import org.grassroot.android.fragments.NavigationDrawerFragment;
import org.grassroot.android.fragments.SingleInputFragment;
import org.grassroot.android.interfaces.NavigationConstants;

import butterknife.BindView;
import butterknife.ButterKnife;
import rx.functions.Action1;

/**
 * Created by luke on 2017/01/13.
 */

public class AccountSignupActivity extends PortraitActivity implements NavigationDrawerFragment.NavigationDrawerCallbacks {

    private static final String TAG = GrassrootExtraActivity.class.getSimpleName();

    @BindView(R.id.acs_toolbar) Toolbar toolbar;
    @BindView(R.id.navigation_drawer) DrawerLayout drawer;

    // @BindView(R.id.progressBar) ProgressBar progressBar;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account_signup);
        ButterKnife.bind(this);

        setSupportActionBar(toolbar);
        ActionBarDrawerToggle drawerToggle = new ActionBarDrawerToggle(this, drawer, toolbar, R.string.nav_open, R.string.nav_close);
        drawer.addDrawerListener(drawerToggle);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
        }
        drawerToggle.syncState();

        welcomeAndStart();
    }

    private void welcomeAndStart() {
        GiantMessageFragment fragment = new GiantMessageFragment.Builder(R.string.gr_extra_welcome_header)
                .setBody(getString(R.string.gr_extra_body))
                .showHomeButton(false)
                .setButtonOne(R.string.gr_extra_start, new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        initiateSignup();
                    }
                }).build();

        getSupportFragmentManager().beginTransaction()
                .add(R.id.acs_fragment_holder, fragment)
                .commit();
    }

    private void initiateSignup() {
        SingleInputFragment fragment = new SingleInputFragment.SingleInputBuilder()
                .header(R.string.account_name_header)
                .explanation(R.string.account_name_expl)
                .hint(R.string.account_name_hint)
                .next(R.string.bt_next)
                .subscriber(new Action1<String>() {
                    @Override
                    public void call(String s) {
                        validateNameAndNext(s);
                    }
                })
                .build();

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.acs_fragment_holder, fragment)
                .addToBackStack(null)
                .commit();
    }

    private void validateNameAndNext(String name) {
        SingleInputFragment fragment = new SingleInputFragment.SingleInputBuilder()
                .header(R.string.billing_email_header)
                .explanation(R.string.billing_email_expl)
                .next(R.string.bt_next)
                .hint(R.string.billing_email_hint)
                .subscriber(new Action1<String>() {
                    @Override
                    public void call(String s) {
                        validateEmailAndNext(s);
                    }
                })
                .build();

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.acs_fragment_holder, fragment)
                .addToBackStack(null)
                .commit();
    }

    private void validateEmailAndNext(String email) {
        AccountTypeFragment fragment = AccountTypeFragment.newInstance(new Action1<String>() {
            @Override
            public void call(String s) {
                initiatePayment(s);
            }
        });

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.acs_fragment_holder, fragment)
                .addToBackStack(null)
                .commit();
    }

    private void initiatePayment(final String accountType) {
        Log.e(TAG, "account type selected: " + accountType);
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
