package org.grassroot.android.activities;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;

import org.grassroot.android.R;
import org.grassroot.android.fragments.LoginScreenFragment;
import org.grassroot.android.fragments.OtpScreenFragment;
import org.grassroot.android.fragments.RegisterNameFragment;
import org.grassroot.android.fragments.RegisterPhoneFragment;
import org.grassroot.android.models.exceptions.ApiCallException;
import org.grassroot.android.utils.ErrorUtils;
import org.grassroot.android.utils.LoginRegUtils;
import org.grassroot.android.utils.NetworkUtils;
import org.grassroot.android.utils.Utilities;

import rx.Subscriber;

/**
 * Created by luke on 2016/06/15.
 */
public class LoginRegisterActivity extends AppCompatActivity implements LoginScreenFragment.LoginFragmentListener,
        OtpScreenFragment.OtpListener, RegisterNameFragment.RegisterNameListener, RegisterPhoneFragment.RegisterPhoneListener {

    // private static final String TAG = LoginRegisterActivity.class.getSimpleName();

    private static final String LOGIN = "login";
    private static final String REGISTER = "register";

    private int taskDepth;

    private ViewGroup rootView;
    private ProgressDialog progressDialog;

    private String msisdn;
    private String enteredNumber;
    private String displayName;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(R.style.Theme_IntroScreen);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_login_register);

        progressDialog = new ProgressDialog(this);
        progressDialog.setIndeterminate(true);
        progressDialog.setMessage(getString(R.string.wait_message));

        rootView = (ViewGroup) findViewById(R.id.rl_activity_root);

        boolean defaultToLogin = getIntent().getBooleanExtra("default_to_login", false);
        taskDepth = 0;
        switchFragments(defaultToLogin ? new LoginScreenFragment() :
            RegisterNameFragment.newInstance(this), false);
    }

    private void switchFragments(Fragment fragment, boolean addToBackStack) {
        FragmentTransaction transaction =  getSupportFragmentManager()
            .beginTransaction()
            .setCustomAnimations(R.anim.a_slide_in_right, R.anim.a_slide_out_left, R.anim.a_slide_in_left, R.anim.a_slide_out_right)
            .replace(R.id.lr_frag_content, fragment, fragment.getClass().getSimpleName());

        if (addToBackStack) {
            transaction.addToBackStack(fragment.getClass().getName());
        }

        transaction.commit();
    }

    @Override
    public void requestLogin(final String mobileNumber) {
        progressDialog.show();
        this.enteredNumber = mobileNumber;
        // the fragment does a check for local number before passing, so this should be acceptable
        // however server will also do a check (as with register)
        this.msisdn = Utilities.formatNumberToE164(mobileNumber);
        LoginRegUtils.reqLogin(msisdn).subscribe(new Subscriber<String>() {
            @Override
            public void onNext(String s) {
                progressDialog.dismiss();
                final String otpToPass = (LoginRegUtils.OTP_ALREADY_SENT.equals(s) ||
                    LoginRegUtils.OTP_PROD_SENT.equals(s)) ? "" : s;
                switchToOtpFragment(otpToPass, LOGIN);
            }

            @Override
            public void onError(Throwable e) {
                progressDialog.dismiss();
                handleError(msisdn, LOGIN, false, e);
            }

            @Override
            public void onCompleted() { }
        });
    }

    @Override
    public void nameEntered(String nameEntered) {
        this.displayName = nameEntered;
        this.taskDepth = 1;
        switchFragments(RegisterPhoneFragment.newInstance(this, enteredNumber), true);
    }

    @Override
    public void phoneEntered(String mobileNumber) {
        requestRegistration(mobileNumber);
    }

    private void requestRegistration(final String mobileNumber) {
        progressDialog.show();
        this.enteredNumber = mobileNumber;
        this.msisdn = Utilities.formatNumberToE164(mobileNumber);
        LoginRegUtils.reqRegister(msisdn, displayName).subscribe(new Subscriber<String>() {
            @Override
            public void onNext(String s) {
                progressDialog.dismiss();
                final String otpToPass = (LoginRegUtils.OTP_ALREADY_SENT.equals(s) ||
                    LoginRegUtils.OTP_PROD_SENT.equals(s)) ? "" : s;
                switchToOtpFragment(otpToPass, REGISTER);
            }

            @Override
            public void onError(Throwable e) {
                progressDialog.dismiss();
                handleError(mobileNumber, REGISTER, false, e);
            }

            @Override
            public void onCompleted() { }
        });
    }

    @Override
    public void requestResendOtp(final String purpose) {
        progressDialog.show();
        LoginRegUtils.resendRegistrationOtp(msisdn).subscribe(new Subscriber<String>() {
            @Override
            public void onNext(String s) {
                progressDialog.dismiss();
                final String otpToPass = (LoginRegUtils.OTP_ALREADY_SENT.equals(s) ||
                    LoginRegUtils.OTP_PROD_SENT.equals(s)) ? "" : s;
                switchToOtpFragment(otpToPass, purpose);
            }

            @Override
            public void onError(Throwable e) {
                progressDialog.dismiss();
                handleError(msisdn, purpose, false, e);
            }

            @Override
            public void onCompleted() { }
        });
    }

    private void authenticateLogin(String otpEntered) {
        progressDialog.show();
        LoginRegUtils.authenticateLogin(msisdn, otpEntered).subscribe(new Subscriber<String>() {
            @Override
            public void onNext(String s) {
                progressDialog.dismiss();
                launchHomeScreen(LoginRegUtils.AUTH_HAS_GROUPS.equals(s));
                setResult(RESULT_OK);
                finish();
            }

            @Override
            public void onError(Throwable e) {
                e.printStackTrace();
                progressDialog.dismiss();
                handleError(msisdn, LOGIN, true, e);
            }

            @Override
            public void onCompleted() { }
        });
    }

    private void verifyRegistration(final String otpEntered) {
        progressDialog.show();
        LoginRegUtils.authenticateRegister(msisdn, otpEntered).subscribe(new Subscriber<String>() {
            @Override
            public void onNext(String s) {
                progressDialog.dismiss();
                launchHomeScreen(false); // by definition, registering means no group
                setResult(RESULT_OK);
                finish();
            }

            @Override
            public void onError(Throwable e) {
                progressDialog.dismiss();
                handleError(msisdn, REGISTER, true, e);
            }

            @Override
            public void onCompleted() { }
        });
    }

    private void handleError(final String mobileNumber, final String purpose, boolean onOtpScreen, Throwable e) {
        if (e instanceof ApiCallException) {
            if (NetworkUtils.CONNECT_ERROR.equals(e.getMessage())) {
                handleConnectError(mobileNumber, purpose);
            } else if (NetworkUtils.SERVER_ERROR.equals(e.getMessage())) {
                if (!onOtpScreen) {
                    handleRequestServerError(((ApiCallException) e).errorTag, purpose);
                } else {
                    handleAuthServerError(((ApiCallException) e).errorTag, purpose);
                }
            }
        }
    }

    private void handleConnectError(final String mobileNumber, final String purpose) {
        // don't use dialog here as "offline" makes little sense
        ErrorUtils.networkErrorSnackbar(rootView, R.string.connect_error_logreg, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (LOGIN.equals(purpose)) {
                    requestLogin(mobileNumber);
                } else {
                    requestRegistration(mobileNumber);
                }
            }
        });
    }

    private void handleRequestServerError(final String restMessage, String purpose) {
        hideSoftKeyboard();
        final String errorMsg = ErrorUtils.serverErrorText(restMessage);
        if (LOGIN.equals(purpose) && restMessage.equals(ErrorUtils.USER_DOESNT_EXIST)) {
            ErrorUtils.snackBarWithAction(rootView, R.string.server_error_user_not_exist, R.string.bt_register,
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        switchFromLoginToRegister();
                    }
            });
        } else if (REGISTER.equals(purpose) && restMessage.equals(ErrorUtils.USER_EXISTS)) {
            ErrorUtils.snackBarWithAction(rootView, R.string.server_error_user_exists, R.string.bt_login, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    switchFromRegisterToLogin(msisdn);
                }
            });
        } else {
            Snackbar.make(rootView, errorMsg, Snackbar.LENGTH_SHORT).show();
        }
    }

    private void handleAuthServerError(final String restMessage, final String purpose) {
        hideSoftKeyboard();
        if (ErrorUtils.WRONG_OTP.equals(restMessage)) {
            ErrorUtils.snackBarWithAction(rootView, R.string.server_error_otp_wrong, R.string.resend_otp,
                new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    requestResendOtp(purpose);
                }
            });
        } else {
            Snackbar.make(rootView, ErrorUtils.serverErrorText(restMessage), Snackbar.LENGTH_SHORT).show();
        }
    }

    private void switchToOtpFragment(String otpToPass, String purpose) {
        taskDepth = (REGISTER.equals(purpose)) ? 2 : 1;
        OtpScreenFragment otpFragment = (OtpScreenFragment) getSupportFragmentManager()
                .findFragmentByTag(OtpScreenFragment.class.getSimpleName());
        if (otpFragment != null && otpFragment.isVisible()) {
            otpFragment.setOtpDisplayed(otpToPass);
            otpFragment.setPurpose(purpose);
        } else {
            Log.e("LRA", "switching to OTP fragment screen");
            OtpScreenFragment otpScreenFragment = OtpScreenFragment.newInstance(otpToPass, purpose);
            switchFragments(otpScreenFragment, true);
        }
    }

    private void switchFromRegisterToLogin(final String userMobile) {
        LoginScreenFragment fragment = LoginScreenFragment.newInstance(Utilities.stripPrefixFromNumber(userMobile));
        getSupportFragmentManager().popBackStack(); // make sure register is gone
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.lr_frag_content, fragment, LoginScreenFragment.class.getCanonicalName())
                .commit();
        requestLogin(userMobile);
    }

    private void switchFromLoginToRegister() {
        RegisterNameFragment fragment = RegisterNameFragment.newInstance(this);
        getSupportFragmentManager().popBackStack(); // make sure login is gone
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.lr_frag_content, fragment, RegisterNameFragment.class.getCanonicalName())
                .commit();
    }

    @Override
    public void onOtpSubmitButtonClick(String otp, String purpose) {
        if (LOGIN.equals(purpose)) {
            authenticateLogin(otp);
        } else {
            verifyRegistration(otp);
        }
    }

    private void launchHomeScreen(boolean userHasGroups) {
        hideSoftKeyboard();
        if (userHasGroups) {
            NetworkUtils.registerForGCM(this).subscribe();
            NetworkUtils.syncAndStartTasks(this, false, true).subscribe();
            Intent homeScreenIntent = new Intent(LoginRegisterActivity.this, HomeScreenActivity.class);
            homeScreenIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(homeScreenIntent);
        } else {
            NetworkUtils.registerForGCM(this).subscribe();
            Intent welcomeScreenIntent = new Intent(LoginRegisterActivity.this, NoGroupWelcomeActivity.class);
            welcomeScreenIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(welcomeScreenIntent);
        }
    }

    @Override
    public void onBackPressed() {
        if (taskDepth == 0) {
            try {
                Intent backToIntro = NavUtils.getParentActivityIntent(this);
                NavUtils.navigateUpTo(this, backToIntro);
            } catch (NullPointerException e) {
                // navutils causing crashes on old devices with low memory, so default to system behavior if it crashes
                super.onBackPressed();
            }
        } else {
            taskDepth--;
            super.onBackPressed();
        }
    }

    private void hideSoftKeyboard() {
        InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
    }

}