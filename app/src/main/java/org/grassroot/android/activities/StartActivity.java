package org.grassroot.android.activities;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.grassroot.android.BuildConfig;
import org.grassroot.android.R;
import org.grassroot.android.events.NotificationEvent;
import org.grassroot.android.fragments.HomeScreenViewFragment;
import org.grassroot.android.fragments.LoginScreenFragment;
import org.grassroot.android.fragments.OtpScreenFragment;
import org.grassroot.android.fragments.RegisterScreenFragment;
import org.grassroot.android.fragments.ViewTaskFragment;
import org.grassroot.android.interfaces.NavigationConstants;
import org.grassroot.android.interfaces.TaskConstants;
import org.grassroot.android.models.GenericResponse;
import org.grassroot.android.models.Token;
import org.grassroot.android.models.TokenResponse;
import org.grassroot.android.services.GcmRegistrationService;
import org.grassroot.android.services.GrassrootRestService;
import org.grassroot.android.services.NotificationUpdateService;
import org.grassroot.android.utils.Constant;
import org.grassroot.android.utils.ErrorUtils;
import org.grassroot.android.utils.LocationUtils;
import org.grassroot.android.utils.NetworkUtils;
import org.grassroot.android.utils.PreferenceUtils;
import org.greenrobot.eventbus.EventBus;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Optional;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

//import butterknife.BindView;

/**
 * Created by admin on 22-Dec-15.
 */
public class StartActivity extends PortraitActivity implements HomeScreenViewFragment.OnHomeScreenInteractionListener,
        RegisterScreenFragment.OnRegisterScreenInteractionListener, LoginScreenFragment.OnLoginScreenInteractionListener,
        OtpScreenFragment.OnOtpScreenFragmentListener {

    private static final String TAG = StartActivity.class.getCanonicalName();
    private static final String LOGIN = "login";
    private static final String REGISTER = "register";

    private Handler defaultHandler;

    private DisplayMetrics displayMetrics;
    private int height;

    private String mobileNumber;
    private LocationUtils locationUtils;

    @Nullable
    @BindView(R.id.fl_content)
    public FrameLayout flContainer;

    @Nullable
    @BindView(R.id.iv_back)
    public ImageView ivBack;

    @Nullable
    @BindView(R.id.txt_toolbar)
    TextView txt_toolbar;

    @Nullable
    @BindView(R.id.iv_splashlogo)
    public ImageView iv_splashlogo;

    @Nullable
    @BindView(R.id.rl_homelogo)
    RelativeLayout rl_homelogo;

    @Nullable
    @BindView(R.id.rl_start)
    RelativeLayout rlStart;

    private ProgressDialog progressDialog;

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleNotificationIntent(intent);
    }

    private void handleNotification() {
        setContentView(R.layout.notification_layout);
        ButterKnife.bind(this);
        handleNotificationIntent(getIntent());
    }

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        if (getIntent().hasExtra(Constant.NOTIFICATION_UID)) {
            handleNotification();
        } else {
            if (PreferenceUtils.getisLoggedIn(this)) {
                userIsLoggedIn();
            } else {
                userIsNotLoggedIn();
            }
        }
    }

    private void userIsLoggedIn() {
        setContentView(R.layout.splashscreen);
        ButterKnife.bind(this);
        if (iv_splashlogo != null) {
            iv_splashlogo.setVisibility(View.VISIBLE);
        }
        this.locationUtils = new LocationUtils(getApplicationContext());
        locationUtils.connect();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent;
                intent = new Intent(StartActivity.this, HomeScreenActivity.class);
                startActivity(intent);
                finish();
            }
        }, Constant.shortDelay);
    }

    private void userIsNotLoggedIn() {
        setContentView(R.layout.start);
        ButterKnife.bind(this);

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage(getResources().getString(R.string.txt_pls_wait));

        defaultHandler = new Handler();
        displayMetrics = getApplicationContext().getResources().getDisplayMetrics();
        height = displayMetrics.heightPixels;
        showHomeScreen();
    }

    private void showHomeScreen() {
        defaultHandler.postDelayed(
                new Runnable() {
                    public void run() {
                        rl_homelogo.animate().translationY((float) (-height / 6)).setDuration(Constant.mediumDelay);
                        defaultHandler.postDelayed(
                                new Runnable() {
                                    public void run() {
                                        setUpHomeScreen();
                                    }
                                }, Constant.mediumDelay);
                    }
                }, Constant.mediumDelay);
    }

    private void setUpHomeScreen() {
        HomeScreenViewFragment homeScreenViewFragment = new HomeScreenViewFragment();
        getSupportFragmentManager().beginTransaction()
                .add(R.id.fl_content, homeScreenViewFragment)
                .commit();
    }

    private void setUpRegisterScreen() {
        ivBack.setVisibility(View.VISIBLE);
        switchFragments(RegisterScreenFragment.newInstance());
    }

    private void setUpLoginScreen() {
        ivBack.setVisibility(View.VISIBLE);
        switchFragments(new LoginScreenFragment());
    }

    private void setUpOtpScreen(String otpToPass, String purpose) {
        OtpScreenFragment otpFragment = (OtpScreenFragment) getSupportFragmentManager()
                .findFragmentByTag(OtpScreenFragment.class.getSimpleName());
        if (otpFragment != null && otpFragment.isVisible()) {
            otpFragment.setOtpDisplayed(otpToPass);
            otpFragment.setPurpose(purpose);
        } else {
            OtpScreenFragment otpScreenFragment = OtpScreenFragment.newInstance(otpToPass, purpose);
            switchFragments(otpScreenFragment);
        }
    }

    /**
     * Method that calls the registration REST service, and then shows the one time pin screen, or an error message
     * todo: just skip straight to login if the number exists
     **/

    @Override
    public void register(EditText user_name, EditText mobile_number) {
        requestRegister(user_name.getText().toString(), mobile_number.getText().toString());
    }

    private void requestRegister(final String et_userName, final String et_mobile_register) {

        progressDialog.show();
        GrassrootRestService.getInstance().getApi()
                .addUser(et_mobile_register, et_userName)
                .enqueue(new Callback<GenericResponse>() {
                    @Override
                    public void onResponse(Call<GenericResponse> call, Response<GenericResponse> response) {
                        progressDialog.dismiss();
                        if (response.isSuccessful()) {
                            final String otpToPass = BuildConfig.FLAVOR.equals(Constant.STAGING) ?
                                    (String) response.body().getData() : "";
                            mobileNumber = et_mobile_register;
                            setUpOtpScreen(otpToPass, REGISTER);
                        } else {
                            ErrorUtils.showSnackBar(rlStart, R.string.error_user_exists, Snackbar.LENGTH_SHORT);
                        }
                    }

                    @Override
                    public void onFailure(Call<GenericResponse> call, Throwable t) {
                        progressDialog.dismiss();
                        ErrorUtils.handleNetworkError(StartActivity.this, rlStart, t);
                    }
                });

    }

    /**
     * Verify that the code received by SMS is the OTP, for verification and hence registration
     *
     * @param mobileNumber Phone number attempting to log in or register
     * @param tokenCode    The token code entered by the user
     */
    private void verifyRegistration(final String mobileNumber, String tokenCode) {
        progressDialog.show();
        GrassrootRestService.getInstance().getApi()
                .verify(mobileNumber, tokenCode)
                .enqueue(new Callback<TokenResponse>() {
                    @Override
                    public void onResponse(Call<TokenResponse> call, Response<TokenResponse> response) {
                        progressDialog.dismiss();
                        if (response.isSuccessful()) {
                            Token token = response.body().getToken();
                            PreferenceUtils.setuser_token(StartActivity.this, token.getCode());
                            PreferenceUtils.setuser_mobilenumber(StartActivity.this, mobileNumber);
                            PreferenceUtils.setisLoggedIn(StartActivity.this, true);
                            PreferenceUtils.setuser_phonetoken(StartActivity.this, mobileNumber + "/" + token.getCode());
                            // PreferenceUtils.setuser_name(StartActivity.this, userName);

                            Log.d(TAG, "getPREF_Phone_Token is " + PreferenceUtils.getPREF_Phone_Token(StartActivity.this));
                            Intent gcmRegistrationIntent = new Intent(StartActivity.this, GcmRegistrationService.class);
                            gcmRegistrationIntent.putExtra("phoneNumber", mobileNumber);
                            startService(gcmRegistrationIntent);
                            Intent homeScreenIntent = new Intent(StartActivity.this, HomeScreenActivity.class);
                            startActivity(homeScreenIntent);
                            finish();
                        } else {
                            ErrorUtils.handleServerError(rlStart, StartActivity.this, response);
                            //showSnackBar(getApplicationContext(), "", getResources().getString(R.string.INVALID_TOKEN), "", 0, Snackbar.LENGTH_SHORT);
                        }
                    }

                    @Override
                    public void onFailure(Call<TokenResponse> call, Throwable t) {
                        progressDialog.dismiss();
                        ErrorUtils.handleNetworkError(StartActivity.this, rlStart, t);
                    }
                });
    }

    @Override
    public void login(final String number) {
        requestLogin(number);
    }

    /**
     * Call the login web service, sending the mobile number to the server and generating an OTP
     * todo: if the user is not registered, redirect to registration screen instead of just error
     *
     * @param mobile_number The number the user entered
     */
    private void requestLogin(String mobile_number) {

        Log.d(TAG, "inside StartActivity ... calling login");

        mobileNumber = mobile_number;
        progressDialog.show();

        GrassrootRestService.getInstance().getApi()
                .login(mobile_number)
                .enqueue(new Callback<GenericResponse>() {
                    @Override
                    public void onResponse(Call<GenericResponse> call, Response<GenericResponse> response) {
                        progressDialog.dismiss();
                        if (response.isSuccessful()) {
                            final String otpToPass = BuildConfig.FLAVOR.equals(Constant.STAGING) ?
                                    (String) response.body().getData() : "";
                            setUpOtpScreen(otpToPass, LOGIN);
                        } else {
                            ErrorUtils.showSnackBar(rlStart, getResources().getString(R.string.User_not_registered),
                                    Snackbar.LENGTH_INDEFINITE, "Register", new View.OnClickListener() {
                                        @Override
                                        public void onClick(View view) {
                                            setUpRegisterScreen();
                                        }
                                    });
                        }
                    }

                    @Override
                    public void onFailure(Call<GenericResponse> call, Throwable t) {
                        progressDialog.dismiss();
                        ErrorUtils.handleNetworkError(StartActivity.this, rlStart, t);
                    }
                });
    }

    /**
     * Authenticate a login, that the user with this mobile number should have this code (tbc)
     *
     * @param mobileNumber The mobile number entered
     * @param code         The code entered by the user
     */
    private void authenticateLogin(final String mobileNumber, String code) {
        progressDialog.show();
        GrassrootRestService.getInstance().getApi()
                .authenticate(mobileNumber, code)
                .enqueue(new Callback<TokenResponse>() {
                    @Override
                    public void onResponse(Call<TokenResponse> call, Response<TokenResponse> response) {
                        progressDialog.dismiss();
                        if (response.isSuccessful()) {
                            Token token = response.body().getToken();
                            PreferenceUtils.setuser_token(StartActivity.this, token.getCode());
                            PreferenceUtils.setuser_mobilenumber(StartActivity.this, mobileNumber);
                            PreferenceUtils.setisLoggedIn(StartActivity.this, true);
                            PreferenceUtils.setuser_phonetoken(StartActivity.this, mobileNumber + "/" + token.getCode());
                            Log.i(TAG, "getPREF_Phone_Token is " + PreferenceUtils.getPREF_Phone_Token(StartActivity.this));

                            Boolean hasGroups = response.body().getHasGroups();
                            String displayname = response.body().getDisplayName();

                            if (!PreferenceUtils.getIsGcmEnabled(StartActivity.this)) {
                                Intent gcmRegistrationIntent = new Intent(StartActivity.this, GcmRegistrationService.class);
                                gcmRegistrationIntent.putExtra("phoneNumber", mobileNumber);
                                startService(gcmRegistrationIntent);
                            }

                            if (hasGroups) {
                                PreferenceUtils.setisHasgroup(StartActivity.this, true);
                                PreferenceUtils.setuser_name(StartActivity.this, displayname);
                                Intent intent = new Intent(StartActivity.this, HomeScreenActivity.class);
                                startActivity(intent);
                                finish();
                            } else {
                                Intent intent = new Intent(StartActivity.this, HomeScreenActivity.class);
                                startActivity(intent);
                                finish();
                            }
                        } else {
                            ErrorUtils.showSnackBar(rlStart, R.string.INVALID_TOKEN, Snackbar.LENGTH_SHORT);
                        }
                    }

                    @Override
                    public void onFailure(Call<TokenResponse> call, Throwable t) {
                        progressDialog.dismiss();
                        ErrorUtils.handleNetworkError(StartActivity.this, rlStart, t);
                    }
                });

    }

    @Optional
    @OnClick(R.id.iv_back)
    public void onBackPressed() {
        try {
            InputMethodManager im = (InputMethodManager) getApplicationContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            im.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (getSupportFragmentManager().getBackStackEntryCount() == 0) {
            super.onBackPressed();

        } else {
            if (!(getSupportFragmentManager().findFragmentById(R.id.fl_content) instanceof OtpScreenFragment)) {
                rl_homelogo.animate().translationY((float) (-height / 6)).scaleX(1).scaleY(1);
            }
            getSupportFragmentManager().popBackStack();
            if (getSupportFragmentManager().getBackStackEntryCount() == 1) {
                ivBack.setVisibility(View.INVISIBLE);
            }
        }
    }

    @Override
    public void onRegisterButtonClick() {
        defaultHandler.postDelayed(new Runnable() {
            public void run() {
                rl_homelogo.animate().translationY((float) (-height / 3.5)).scaleX((float) 0.7).scaleY((float) 0.7);
                defaultHandler.postDelayed(
                        new Runnable() {
                            public void run() {
                                setUpRegisterScreen();
                            }
                        }, 300L); // todo: create central constants for these animation timings and scales
            }
        }, 300L);
    }

    @Override
    public void onLoginButtonRegisterClick() {
        defaultHandler.postDelayed(new Runnable() {
            public void run() {
                rl_homelogo.animate().translationY((float) (-height / 3.5)).scaleX((float) 0.7).scaleY((float) 0.7);
                defaultHandler.postDelayed(
                        new Runnable() {
                            public void run() {
                                setUpLoginScreen();
                            }
                        }, 300L);
            }
        }, 300L);
    }

    @Override
    public void onTextResendClick(String purpose) {
        if (LOGIN.equals(purpose)) {
            requestLogin(mobileNumber);
        } else if (REGISTER.equals(purpose)) {
            // need a call to resend OTP
        }
    }

    @Override
    public void onOtpSubmitButtonClick(String otp, String purpose) {
        if (LOGIN.equals(purpose)) {
            authenticateLogin(mobileNumber, otp);
        } else if (REGISTER.equals(purpose)) {
            verifyRegistration(mobileNumber, otp);
        }
    }

    private void handleNotificationIntent(Intent intent) {
        //may redundant to check for null, but just to be safe

        Log.e(TAG, "recieved notification");
        String notificationUid = getIntent().getStringExtra(Constant.NOTIFICATION_UID);
        String taskUid = intent.getExtras().getString(TaskConstants.TASK_UID_FIELD);
        String tasKType = intent.getExtras().getString(TaskConstants.TASK_TYPE_FIELD);

        Log.e(TAG, "notificationUid " + notificationUid + ", taskUid = " + taskUid + ", taskType = " + tasKType);

        txt_toolbar.setText(tasKType);
        ViewTaskFragment viewTaskFragment = ViewTaskFragment.newInstance(tasKType, taskUid);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fl_content, viewTaskFragment)
                .commit();

        int notificationCount = PreferenceUtils.getIsNotificationcounter(this);
        Log.e(TAG, "count " + notificationCount);
        NotificationUpdateService.updateNotificationStatus(this, notificationUid);
        PreferenceUtils.setIsNotificationcounter(this, --notificationCount);
        EventBus.getDefault().post(new NotificationEvent(--notificationCount));
    }

    private void switchFragments(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(R.anim.a_slide_in_right, R.anim.a_slide_out_left, R.anim.a_slide_in_left, R.anim.a_slide_out_right)
                .replace(R.id.fl_content, fragment, fragment.getClass().getSimpleName())
                .addToBackStack(fragment.getClass().getName())
                .commit();
    }
}