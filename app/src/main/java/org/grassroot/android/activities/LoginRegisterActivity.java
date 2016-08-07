package org.grassroot.android.activities;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import org.grassroot.android.BuildConfig;
import org.grassroot.android.R;
import org.grassroot.android.fragments.LoginScreenFragment;
import org.grassroot.android.fragments.OtpScreenFragment;
import org.grassroot.android.fragments.RegisterScreenFragment;
import org.grassroot.android.interfaces.NotificationConstants;
import org.grassroot.android.models.GenericResponse;
import org.grassroot.android.models.PreferenceObject;
import org.grassroot.android.models.TokenResponse;
import org.grassroot.android.services.GcmRegistrationService;
import org.grassroot.android.services.GrassrootRestService;
import org.grassroot.android.utils.Constant;
import org.grassroot.android.utils.ErrorUtils;
import org.grassroot.android.utils.NetworkUtils;
import org.grassroot.android.utils.RealmUtils;
import org.grassroot.android.utils.Utilities;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import rx.functions.Action1;

/**
 * Created by luke on 2016/06/15.
 */
public class LoginRegisterActivity extends AppCompatActivity implements LoginScreenFragment.LoginFragmentListener,
        OtpScreenFragment.OtpListener, RegisterScreenFragment.RegisterListener {

    private static final String TAG = LoginRegisterActivity.class.getSimpleName();

    private static final String LOGIN = "login";
    private static final String REGISTER = "register";

    private boolean onRegisterOrLogin;

    private ViewGroup rootView;
    private ProgressDialog progressDialog;

    private String msisdn;
    private String displayName;

    private long otpRequestedTime;

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
        onRegisterOrLogin = true;
        otpRequestedTime = -1;
        switchFragments(defaultToLogin ? new LoginScreenFragment() : RegisterScreenFragment.newInstance(), false);
    }

    private void switchFragments(Fragment fragment, boolean addToBackStack) {
        FragmentTransaction transaction =  getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(R.anim.a_slide_in_right, R.anim.a_slide_out_left, R.anim.a_slide_in_left, R.anim.a_slide_out_right)
                .replace(R.id.lr_frag_content, fragment, fragment.getClass().getSimpleName());

        if (addToBackStack) {
            transaction.addToBackStack(fragment.getClass().getName());
        }

        transaction.commit();
    }

    @Override
    public void requestLogin(final String mobileNumber) {
        final String passedMobile = Utilities.formatNumberToE164(mobileNumber);
        if (shouldRequestOtp(passedMobile)) {
            progressDialog.show();
            msisdn = passedMobile;
            GrassrootRestService.getInstance().getApi()
                    .login(msisdn)
                    .enqueue(new Callback<GenericResponse>() {
                        @Override
                        public void onResponse(Call<GenericResponse> call, Response<GenericResponse> response) {
                            progressDialog.dismiss();
                            if (response.isSuccessful()) {
                                otpRequestedTime = System.currentTimeMillis();
                                final String otpToPass = BuildConfig.FLAVOR.equals(Constant.STAGING) ? (String) response.body().getData() : "";
                                switchToOtp(otpToPass, LOGIN);
                            } else {
                                final String restMessage = response.body().getMessage();
                                final String errorMsg = ErrorUtils.serverErrorText(restMessage, LoginRegisterActivity.this);
                                if (restMessage.equals(ErrorUtils.USER_DOESNT_EXIST)) {
                                    ErrorUtils.showSnackBar(rootView, errorMsg, Snackbar.LENGTH_LONG, "Register", new View.OnClickListener() {
                                            @Override
                                            public void onClick(View view) {
                                                switchFromLoginToRegister(msisdn);
                                            }
                                        });
                                } else {
                                    Snackbar.make(rootView, errorMsg, Snackbar.LENGTH_SHORT);
                                }
                            }
                        }

                        @Override
                        public void onFailure(Call<GenericResponse> call, Throwable t) {
                            otpRequestedTime = -1;
                            progressDialog.dismiss();
                            final String errorMessage = getString(R.string.connect_error_logreg);
                            final String actionMsg = getString(R.string.snackbar_try_again);
                            ErrorUtils.showSnackBar(rootView, errorMessage, Snackbar.LENGTH_LONG, actionMsg, new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    requestLogin(mobileNumber);
                                }
                            });
                        }
                    });
        } else {
            switchToOtp("", LOGIN);
        }
    }

    // todo : change this to two screens (phone number then name, skip name if already exists, etc)
    @Override
    public void requestRegistration(String userName, String phoneNumber) {
        final String passedMobile = Utilities.formatNumberToE164(phoneNumber);
        displayName = userName;

        if (shouldRequestOtp(passedMobile)) {
            progressDialog.show();
            msisdn = passedMobile;
            GrassrootRestService.getInstance().getApi()
                    .addUser(msisdn, userName)
                    .enqueue(new Callback<GenericResponse>() {
                        @Override
                        public void onResponse(Call<GenericResponse> call, Response<GenericResponse> response) {
                            progressDialog.dismiss();
                            if (response.isSuccessful()) {
                                otpRequestedTime = System.currentTimeMillis();
                                final String otpToPass = BuildConfig.FLAVOR.equals(Constant.STAGING) ?
                                        (String) response.body().getData() : "";
                                switchToOtp(otpToPass, REGISTER);
                            } else {
                                final String restMessage = response.body().getMessage();
                                final String errorMsg = ErrorUtils.serverErrorText(restMessage, LoginRegisterActivity.this);
                                if (restMessage.equals(ErrorUtils.USER_EXISTS)) {
                                    final String actionBtn = getResources().getString(R.string.bt_login);
                                    ErrorUtils.showSnackBar(rootView, errorMsg, Snackbar.LENGTH_LONG, actionBtn, new View.OnClickListener() {
                                        @Override
                                        public void onClick(View view) {
                                            switchFromRegisterToLogin(msisdn);
                                        }
                                    });
                                } else {
                                    Snackbar.make(rootView, errorMsg, Snackbar.LENGTH_SHORT);
                                }
                            }
                        }

                        @Override
                        public void onFailure(Call<GenericResponse> call, Throwable t) {
                            progressDialog.dismiss();
                            ErrorUtils.handleNetworkError(LoginRegisterActivity.this, rootView, t);
                        }
                    });
        } else {
            switchToOtp("", LOGIN);
        }
    }

    private boolean shouldRequestOtp(final String passedMobile) {
        final int otpRequestInterval = 5 * 60 * 1000; // i.e., 5 minutes
        return TextUtils.isEmpty(msisdn) || !msisdn.equals(passedMobile) ||
                System.currentTimeMillis() > otpRequestedTime + otpRequestInterval;
    }

    private void switchToOtp(String otpToPass, String purpose) {
        onRegisterOrLogin = false;
        OtpScreenFragment otpFragment = (OtpScreenFragment) getSupportFragmentManager()
                .findFragmentByTag(OtpScreenFragment.class.getSimpleName());
        if (otpFragment != null && otpFragment.isVisible()) {
            otpFragment.setOtpDisplayed(otpToPass);
            otpFragment.setPurpose(purpose);
        } else {
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

    private void switchFromLoginToRegister(final String userMobile) {
        RegisterScreenFragment fragment = RegisterScreenFragment.newInstance(Utilities.stripPrefixFromNumber(userMobile));
        getSupportFragmentManager().popBackStack(); // make sure login is gone
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.lr_frag_content, fragment, RegisterScreenFragment.class.getCanonicalName())
                .commit();
    }

    @Override
    public void onTextResendClick(String purpose) {
        if (LOGIN.equals(purpose)) {
            requestLogin(msisdn);
        } else {
            GrassrootRestService.getInstance().getApi().resendRegOtp(msisdn).enqueue(new Callback<GenericResponse>() {
                @Override
                public void onResponse(Call<GenericResponse> call, Response<GenericResponse> response) {
                    if (response.isSuccessful()) {
                        otpRequestedTime = System.currentTimeMillis();
                        final String otpToPass = BuildConfig.FLAVOR.equals(Constant.STAGING) ?
                            (String) response.body().getData() : "";
                        switchToOtp(otpToPass, REGISTER);
                    } else {
                        String errorMsg = ErrorUtils.serverErrorText(response.errorBody(), LoginRegisterActivity.this);
                        Snackbar.make(rootView, errorMsg, Snackbar.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<GenericResponse> call, Throwable t) {
                    Snackbar.make(rootView, R.string.connect_otp_resend_fail, Snackbar.LENGTH_SHORT).show();
                }
            });
        }
    }

    @Override
    public void onOtpSubmitButtonClick(String otp, String purpose) {
        if (LOGIN.equals(purpose)) {
            authenticateLogin(otp);
        } else {
            verifyRegistration(otp);
        }
    }

    private void authenticateLogin(String otpEntered) {
        progressDialog.show();
        GrassrootRestService.getInstance().getApi().authenticate(msisdn, otpEntered)
                .enqueue(new Callback<TokenResponse>() {
                    @Override
                    public void onResponse(Call<TokenResponse> call, final Response<TokenResponse> response) {
                        progressDialog.hide();
                        if (response.isSuccessful()) {
                            progressDialog.dismiss();
                            PreferenceObject preference = setUpPrefs(response.body());
                            preference.setHasGroups(response.body().getHasGroups());
                            preference.setUserName(response.body().getDisplayName());
                            preference.setMustRefresh(true);
                            RealmUtils.saveDataToRealm(preference).subscribe(new Action1() {
                                @Override public void call(Object o) {
                                    registerOrRefreshGCM(msisdn);
                                    launchHomeScreen(response.body().getHasGroups());
                                    finish();
                                }
                            });
                        } else {
                            handleAuthServerError(response.errorBody(), LOGIN);
                        }
                    }

                    @Override
                    public void onFailure(Call<TokenResponse> call, Throwable t) {
                        progressDialog.hide();
                        ErrorUtils.handleNetworkError(LoginRegisterActivity.this, rootView, t);
                    }
                });
    }

    private void verifyRegistration(final String otpEntered) {
        progressDialog.show();
        GrassrootRestService.getInstance().getApi()
                .verify(msisdn, otpEntered)
                .enqueue(new Callback<TokenResponse>() {
                    @Override
                    public void onResponse(Call<TokenResponse> call, Response<TokenResponse> response) {
                        progressDialog.hide();
                        if (response.isSuccessful()) {
                            progressDialog.dismiss();
                            PreferenceObject preference = setUpPrefs(response.body());
                            preference.setUserName(displayName);
                            preference.setHasGroups(false);
                            RealmUtils.saveDataToRealm(preference).subscribe(new Action1() {
                                @Override public void call(Object o) {
                                    registerOrRefreshGCM(msisdn);
                                    launchHomeScreen(false); // by definition, registering means no group
                                    finish();
                                }
                            });
                        } else {
                            handleAuthServerError(response.errorBody(), REGISTER);
                        }
                    }

                    @Override
                    public void onFailure(Call<TokenResponse> call, Throwable t) {
                        progressDialog.dismiss();
                        ErrorUtils.handleNetworkError(LoginRegisterActivity.this, rootView, t);
                    }
                });
    }

    private PreferenceObject setUpPrefs(TokenResponse response) {
        PreferenceObject preference = new PreferenceObject();
        preference.setToken(response.getToken().getCode());
        preference.setMobileNumber(msisdn);
        preference.setLoggedIn(true);
        return preference;
    }

    private void registerOrRefreshGCM(final String phoneNumber) {
        Log.d(TAG, "registering for GCM ... sending intent ...");
        Intent gcmRegistrationIntent = new Intent(LoginRegisterActivity.this, GcmRegistrationService.class);
        gcmRegistrationIntent.putExtra(NotificationConstants.ACTION, NotificationConstants.GCM_REGISTER);
        gcmRegistrationIntent.putExtra(NotificationConstants.PHONE_NUMBER, phoneNumber);
        startService(gcmRegistrationIntent);
    }

    private void launchHomeScreen(boolean userHasGroups) {
        if (userHasGroups) {
            NetworkUtils.syncAndStartTasks(this, false, true).subscribe();
            NetworkUtils.registerForGCM(this).subscribe();
            Intent homeScreenIntent = new Intent(LoginRegisterActivity.this, HomeScreenActivity.class);
            startActivity(homeScreenIntent);
        } else {
            Intent welcomeScreenIntent = new Intent(LoginRegisterActivity.this, NoGroupWelcomeActivity.class);
            startActivity(welcomeScreenIntent);
        }
    }

    private void handleAuthServerError(final ResponseBody errorBody, final String purpose) {
        final String restMessage = ErrorUtils.getRestMessage(errorBody);
        final String errorMsg = ErrorUtils.serverErrorText(restMessage, LoginRegisterActivity.this);
        if (ErrorUtils.WRONG_OTP.equals(restMessage)) {
            final String actionMsg = getString(R.string.resend_otp);
            ErrorUtils.showSnackBar(rootView, errorMsg, Snackbar.LENGTH_LONG, actionMsg, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onTextResendClick(purpose);
                }
            });
        } else {
        Snackbar.make(rootView, errorMsg, Snackbar.LENGTH_SHORT);
        }
    }

    @Override
    public void onBackPressed() {
        if (onRegisterOrLogin) {
            // doing it his way so can preserve keeping IntroActivity off history
            Intent backToIntro = new Intent(this, IntroActivity.class);
            startActivity(backToIntro);
            finish();
        } else {
            onRegisterOrLogin = true;
            super.onBackPressed();
        }
    }

}