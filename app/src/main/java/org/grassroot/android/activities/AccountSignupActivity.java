package org.grassroot.android.activities;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;

import com.oppwa.mobile.connect.checkout.dialog.CheckoutActivity;
import com.oppwa.mobile.connect.checkout.meta.CheckoutPaymentMethod;
import com.oppwa.mobile.connect.checkout.meta.CheckoutSettings;
import com.oppwa.mobile.connect.exception.PaymentError;
import com.oppwa.mobile.connect.exception.PaymentException;
import com.oppwa.mobile.connect.provider.Connect;
import com.oppwa.mobile.connect.service.ConnectService;
import com.oppwa.mobile.connect.service.IProviderBinder;

import org.grassroot.android.R;
import org.grassroot.android.fragments.AccountTypeFragment;
import org.grassroot.android.fragments.GiantMessageFragment;
import org.grassroot.android.fragments.NavigationDrawerFragment;
import org.grassroot.android.fragments.SingleInputFragment;
import org.grassroot.android.interfaces.NavigationConstants;
import org.grassroot.android.models.AccountBill;
import org.grassroot.android.models.responses.RestResponse;
import org.grassroot.android.services.GrassrootRestService;
import org.grassroot.android.utils.RealmUtils;

import butterknife.BindView;
import butterknife.ButterKnife;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import rx.functions.Action1;

/**
 * Created by luke on 2017/01/13.
 */

public class AccountSignupActivity extends PortraitActivity implements NavigationDrawerFragment.NavigationDrawerCallbacks {

    private static final String TAG = GrassrootExtraActivity.class.getSimpleName();

    @BindView(R.id.acs_toolbar) Toolbar toolbar;
    @BindView(R.id.navigation_drawer) DrawerLayout drawer;

    private String accountName;
    private String billingEmail;
    private String accountType;
    private String paymentId;

    @BindView(R.id.progressBar) ProgressBar progressBar;

    private IProviderBinder binder;
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            binder = (IProviderBinder) iBinder;
            try {
                binder.initializeProvider(Connect.ProviderMode.TEST);
            } catch (PaymentException e) {
                Log.e(TAG, "error initializing payment!");
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            binder = null;
        }
    };

    @Override
    protected void onStart() {
        super.onStart();

        Intent intent = new Intent(this, ConnectService.class);
        startService(intent);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

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

    @Override
    protected void onStop() {
        super.onStop();
        unbindService(serviceConnection);
        stopService(new Intent(this, ConnectService.class));
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
        accountName = name;

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
        billingEmail = email;

        AccountTypeFragment fragment = AccountTypeFragment.newInstance(AccountTypeFragment.STD, new Action1<String>() {
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

    private void initiatePayment(final String type) {
        accountType = type;

        progressBar.setVisibility(View.VISIBLE);
        if (!TextUtils.isEmpty(paymentId)) {
            initiateCheckout(paymentId);
        } else {
            final String phoneNumber = RealmUtils.loadPreferencesFromDB().getMobileNumber();
            final String code = RealmUtils.loadPreferencesFromDB().getToken();
            GrassrootRestService.getInstance().getApi().initiateAccountSignup(phoneNumber, code,
                    accountName, billingEmail, accountType).enqueue(new Callback<RestResponse<AccountBill>>() {
                @Override
                public void onResponse(Call<RestResponse<AccountBill>> call, Response<RestResponse<AccountBill>> response) {
                    paymentId = response.body().getData().getPaymentId();
                    Log.e(TAG, "initating payment with ID : " + paymentId);
                    initiateCheckout(paymentId);
                }

                @Override
                public void onFailure(Call<RestResponse<AccountBill>> call, Throwable t) {

                }
            });
        }
    }

    private void initiateCheckout(final String checkoutId) {
        final String paymentTitle = getString(R.string.billing_signup_title);
        CheckoutSettings settings = new CheckoutSettings(
                checkoutId,
                paymentTitle,
                new CheckoutPaymentMethod[] {
                        CheckoutPaymentMethod.VISA,
                        CheckoutPaymentMethod.MASTERCARD
                });

        Intent intent = new Intent(AccountSignupActivity.this, CheckoutActivity.class);
        intent.putExtra(CheckoutActivity.CHECKOUT_SETTINGS, settings);

        progressBar.setVisibility(View.GONE);
        startActivityForResult(intent, CheckoutActivity.CHECKOUT_ACTIVITY);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (resultCode) {
            case CheckoutActivity.RESULT_OK:
                showSuccessDialogAndExit();
                break;
            case CheckoutActivity.RESULT_CANCELED:
                showCancelledDialogAndOptions();
                break;
            case CheckoutActivity.RESULT_ERROR:
                PaymentError error = data.getExtras().getParcelable(CheckoutActivity.CHECKOUT_RESULT_ERROR);
                Log.e(TAG, "payment error! log: " + (error == null ? "null result" : error.getErrorMessage() + ", code: " + error.getErrorCode()));
                showErrorDialogAndOptions();
        }
    }

    private void showSuccessDialogAndExit() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.account_paid_success)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        exitToGrExtra();
                    }
                })
                .setCancelable(true)
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialogInterface) {
                        exitToGrExtra();
                    }
                });
        builder.show();
    }

    private void exitToGrExtra() {
        startActivity(new Intent(AccountSignupActivity.this, GrassrootExtraActivity.class));
        finish();
    }

    private void showCancelledDialogAndOptions() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.account_paid_cancelled)
                .setPositiveButton(R.string.account_payment_error_tryagain, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        initiateCheckout(paymentId);
                    }
                })
                .setNegativeButton(R.string.account_payment_error_exit, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        exitToHome(null, true);
                    }
                })
                .setCancelable(true);
        builder.show();
    }

    private void showErrorDialogAndOptions() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.account_paid_error)
                .setPositiveButton(R.string.account_payment_error_tryagain, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        initiateCheckout(paymentId);
                    }
                })
                .setNegativeButton(R.string.account_payment_error_exit, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        exitToHome(null, true);
                    }
                })
                .setCancelable(true);
        builder.show();
    }

    private void exitToHome(final String openOnTab, boolean finish) {
        // consider just rewinding in stack
        Intent i = new Intent(this, HomeScreenActivity.class);
        if (!TextUtils.isEmpty(openOnTab)) {
            i.putExtra(NavigationConstants.HOME_OPEN_ON_NAV, openOnTab);
        }
        startActivity(i);
        if (finish) {
            finish();
        }
    }

    @Override
    public void onNavigationDrawerItemSelected(String tag) {
        if (drawer != null) {
            drawer.closeDrawer(GravityCompat.START);
        }
        exitToHome(tag, false); // finish false so can tap back to this
    }

}
