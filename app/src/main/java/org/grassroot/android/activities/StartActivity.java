package org.grassroot.android.activities;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.grassroot.android.R;
import org.grassroot.android.events.NotificationEvent;
import org.grassroot.android.fragments.ViewTaskFragment;
import org.grassroot.android.interfaces.TaskConstants;
import org.grassroot.android.services.GcmRegistrationService;
import org.grassroot.android.services.GrassrootRestService;
import org.grassroot.android.models.GenericResponse;
import org.grassroot.android.models.Token;
import org.grassroot.android.models.TokenResponse;
import org.grassroot.android.fragments.HomeScreenViewFragment;
import org.grassroot.android.fragments.LoginScreenFragment;
import org.grassroot.android.fragments.OtpScreenFragment;
import org.grassroot.android.fragments.RegisterScreenFragment;
import org.grassroot.android.services.NotificationUpdateService;
import org.grassroot.android.utils.Constant;
import org.grassroot.android.utils.ErrorUtils;
import org.grassroot.android.utils.LocationUtils;
import org.grassroot.android.utils.NetworkUtils;
import org.grassroot.android.utils.PreferenceUtils;
import org.grassroot.android.utils.TopExceptionHandler;
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
    //will fix once we start with mvp implementation

    private Handler defaultHandler;
    private String TAG = StartActivity.class.getCanonicalName();

    private Snackbar snackBar;
    private DisplayMetrics displayMetrics;
    private int height;
    private boolean otpscreen = false;
    private boolean registerscreen = false;
    public boolean loginscreen = false;

    private String userName;
    private String mobileNumber;
    private String data;

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
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        Thread.setDefaultUncaughtExceptionHandler(new TopExceptionHandler(this));

        if (getIntent().hasExtra(Constant.NOTIFICATION_UID)) {
            setContentView(R.layout.notification_layout);
            ButterKnife.bind(this);
            handleNotificationIntent(getIntent());


        } else {
            if (!PreferenceUtils.getisLoggedIn(this)) {
                setContentView(R.layout.start);
                ButterKnife.bind(this);

                progressDialog = new ProgressDialog(this);
                progressDialog.setMessage(getResources().getString(R.string.txt_pls_wait));

                if (!NetworkUtils.isNetworkAvailable(StartActivity.this)) {
                    PreferenceUtils.setisLoggedIn(this, false);
                }

                defaultHandler = new Handler();
                displayMetrics = getApplicationContext().getResources().getDisplayMetrics();
                height = displayMetrics.heightPixels;
                showHomeScreen();
            } else {
                setContentView(R.layout.splashscreen);
                ButterKnife.bind(this);
                if (iv_splashlogo != null) {
                    iv_splashlogo.setVisibility(View.VISIBLE);
                }
                this.locationUtils = new LocationUtils(this);
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
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleNotificationIntent(intent);
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
        otpscreen = false;
        loginscreen = false;
        registerscreen = false;

        HomeScreenViewFragment homeScreenViewFragment = new HomeScreenViewFragment();
        getSupportFragmentManager().beginTransaction()
                .add(R.id.fl_content, homeScreenViewFragment)
                .commit();
    }

    private void setUpRegisterScreen() {
        data = "";
        registerscreen = true;

        otpscreen = false;
        loginscreen = false;
        ivBack.setVisibility(View.VISIBLE);
        switchFragments(RegisterScreenFragment.newInstance());
    }

    private void setUpLoginScreen() {
        data = "";
        loginscreen = true;

        otpscreen = false;
        registerscreen = false;
        ivBack.setVisibility(View.VISIBLE);
        switchFragments(new LoginScreenFragment());
    }

    private void setUpOtpScreen() {
        OtpScreenFragment otpScreenFragment = OtpScreenFragment.newInstance(data);
        switchFragments(otpScreenFragment);
        // this next block : huh??
        if (registerscreen) {
            otpscreen = true;
        } else if (loginscreen) {
            otpscreen = true;
        }
    }

    private void textResend() {
        if (registerscreen) {
            register(userName, mobileNumber);
        } else if (loginscreen) {
            login(mobileNumber);
        }
    }

    private void otpFormValidation(EditText et_otp) {
        if (et_otp.getText().toString().isEmpty()) {
            et_otp.setError(getResources().getString(R.string.OTP_empty));
        } else {
            if (loginscreen) {
                authenticateLogin(mobileNumber, et_otp.getText().toString());
            } else {
                verifyRegistration(mobileNumber, et_otp.getText().toString());
            }
        }
    }

    @Optional
    @OnClick(R.id.iv_back)
    public void onBackPressed() {

        try {
            InputMethodManager im = (InputMethodManager) getApplicationContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            im.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
            snackBar.dismiss();
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (getSupportFragmentManager().getBackStackEntryCount() == 0) {
            super.onBackPressed();

        } else {

            if (!(getVisibleFragment() instanceof OtpScreenFragment)) {
                rl_homelogo.animate().translationY((float) (-height / 6)).scaleX(1).scaleY(1);
            }
            getSupportFragmentManager().popBackStack();
            if (getSupportFragmentManager().getBackStackEntryCount() == 1) {
                ivBack.setVisibility(View.INVISIBLE);
            }

        }
    }

    /**
     * Method that calls the registration REST service, and then shows the one time pin screen, or an error message
     * todo: just skip straight to login if the number exists
     *
     * @param et_userName        The name the user has entered
     * @param et_mobile_register The phone number they wish to register
     */
    private void register(final String et_userName, final String et_mobile_register) {

        Log.d(TAG, "inside StartActivity ... calling register");
        progressDialog.show();
        registerscreen = true;
        GrassrootRestService.getInstance().getApi()
                .addUser(et_mobile_register, et_userName)
                .enqueue(new Callback<GenericResponse>() {
                    @Override
                    public void onResponse(Call<GenericResponse> call, Response<GenericResponse> response) {
                        progressDialog.dismiss();
                        if (response.isSuccessful()) {
                            data = (String) response.body().getData();
                            mobileNumber = et_mobile_register;
                            if (otpscreen) {
                                Log.d(TAG, "not calling setUpOtpScreen");
                                OtpScreenFragment otpScreenFragment = (OtpScreenFragment) getVisibleFragment();
                                otpScreenFragment.et_otp.setText(data);
                            } else {
                                Log.e(TAG, "calling setUpOtpScreen");
                                setUpOtpScreen();
                            }
                        } else {
                            showSnackBar(getApplicationContext(), "", getResources().getString(R.string.error_user_exists), "", 0, Snackbar.LENGTH_SHORT);
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
     * Call the login web service, sending the mobile number to the server and generating an OTP
     * todo: if the user is not registered, redirect to registration screen instead of just error
     *
     * @param mobile_number The number the user entered
     */
    private void login(String mobile_number) {

        Log.d(TAG, "inside StartActivity ... calling login");

        mobileNumber = mobile_number;
        progressDialog.show();
        loginscreen = true;

        GrassrootRestService.getInstance().getApi()
                .login(mobile_number)
                .enqueue(new Callback<GenericResponse>() {
                    @Override
                    public void onResponse(Call<GenericResponse> call, Response<GenericResponse> response) {
                        progressDialog.dismiss();
                        if (response.isSuccessful()) {
                            data = (String) response.body().getData(); // this is asking for trouble, refactor
                            if (otpscreen) {
                                Log.e(TAG, "not calling setUpOtpScreen");
                                OtpScreenFragment otpScreenFragment = (OtpScreenFragment) getVisibleFragment();
                                otpScreenFragment.et_otp.setText(data);
                            } else {
                                Log.e(TAG, "calling setUpOtpScreen");
                                setUpOtpScreen();
                            }
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
                        // in Retrofit2, this is only called if couldn't reach server, not if server sent back wrong request
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
                            PreferenceUtils.setuser_name(StartActivity.this, userName);

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
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

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
                            showSnackBar(getApplicationContext(), "", getResources().getString(R.string.INVALID_TOKEN), "", 0, Snackbar.LENGTH_SHORT);
                        }
                    }

                    @Override
                    public void onFailure(Call<TokenResponse> call, Throwable t) {
                        progressDialog.dismiss();
                        ErrorUtils.handleNetworkError(StartActivity.this, rlStart, t);
                    }
                });

    }

    /**
     * Displays snack bar with some text, and possibility to link to an action
     *
     * @param context   Context in which it is called
     * @param type      The type (currently "" in most calls...)
     * @param message   The message to display
     * @param textLabel A text label for taking an action
     * @param color     The color of the snackbar
     * @param length    The length of the snackbar
     */
    public void showSnackBar(Context context, final String type, String message, String textLabel, int color, int length) {

        snackBar = Snackbar.make(rlStart, message, length);
        snackBar.setActionTextColor(Color.RED);

        if (!textLabel.isEmpty()) { //show action button depending on Label
            snackBar.setAction(textLabel, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (type.equals("register")) {
                        register(userName, mobileNumber);
                        snackBar.dismiss();
                        // getNotification();
                    } else if (type.equals("login")) {
                        login(mobileNumber);
                        snackBar.dismiss();
                    } else if (type.equals("OtpWS")) {
                        snackBar.dismiss();
                    }
                }
            });
        }

        Log.i(TAG, "showing the snackbar! with this type= " + type);
        snackBar.show();
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
    public void register(EditText user_name, EditText mobile_number) {
        registerFormValidation(user_name, mobile_number);
    }

    @Override
    public void onTextResendClick() {
        textResend();
    }

    @Override
    public void onOtpSubmitButtonClick(EditText et_otp) {
        otpFormValidation(et_otp);
    }

    @Override
    public void login(EditText et_mobile_login) {
        loginFormValidation(et_mobile_login);
    }

    private Fragment getVisibleFragment() {
        List<Fragment> fragments = getSupportFragmentManager().getFragments();
        if (fragments != null) {
            for (Fragment fragment : fragments) {
                if (fragment != null && fragment.isVisible())
                    return fragment;
            }
        }
        return null;
    }

    private void handleNotificationIntent(Intent intent) {
        //may redundant to check for null, but just to be safe

                Log.e(TAG, "recieved notification");
                String notificationUid = getIntent().getStringExtra(Constant.NOTIFICATION_UID);
                String taskUid = intent.getExtras().getString(TaskConstants.TASK_UID_FIELD);
                String tasKType = intent.getExtras().getString(TaskConstants.TASK_TYPE_FIELD);

                Log.e(TAG, "notificationUid " + notificationUid);
               Log.e(TAG, "taskUid " + taskUid);
               Log.e(TAG, "tasktype " + tasKType);
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
                .replace(R.id.fl_content, fragment)
                .addToBackStack(fragment.getClass().getName()).commit();
    }

    private void loginFormValidation(EditText et_mobile_login) {
        if (et_mobile_login.getText().toString().isEmpty()) {
            et_mobile_login.requestFocus();
            et_mobile_login.setError(getResources().getString(R.string.Cellphone_number_empty));
        } else {
            if (et_mobile_login.getText().toString().length() != 10 && et_mobile_login.getText().toString().length() < 10) {
                et_mobile_login.requestFocus();
                et_mobile_login.setError(getResources().getString(R.string.Cellphone_number_invalid));
            } else {
                if (Integer.parseInt(String.valueOf(et_mobile_login.getText().toString().charAt(0))) != 0) {
                    et_mobile_login.requestFocus();
                    et_mobile_login.setError(getResources().getString(R.string.Cellphone_number_invalid));
                } else if (Integer.parseInt(String.valueOf(et_mobile_login.getText().toString().charAt(1))) == 0 || Integer.parseInt(String.valueOf(et_mobile_login.getText().toString().charAt(1))) == 9) {
                    et_mobile_login.requestFocus();
                    et_mobile_login.setError(getResources().getString(R.string.Cellphone_number_invalid));
                } else {
                    login(et_mobile_login.getText().toString());
                }
            }
        }
    }

    private void registerFormValidation(EditText et_userName, EditText et_mobile_register) {
        if (et_userName.getText().toString().trim().isEmpty() || et_mobile_register.getText().toString().isEmpty()) {
            if (et_userName.getText().toString().trim().isEmpty() && !et_mobile_register.getText().toString().isEmpty()) {
                et_userName.requestFocus();
                et_userName.setError(getResources().getString(R.string.Either_field_empty));
            } else if (et_mobile_register.getText().toString().isEmpty() && !et_userName.getText().toString().isEmpty()) {
                et_mobile_register.requestFocus();
                et_mobile_register.setError(getResources().getString(R.string.Cellphone_number_empty));
            } else {
                et_userName.setError(getResources().getString(R.string.Either_field_empty));
                et_mobile_register.setError(getResources().getString(R.string.Cellphone_number_empty));
                showSnackBar(getApplicationContext(), "", getResources().getString(R.string.Either_field_empty), "", 0, Snackbar.LENGTH_SHORT);
            }
        } else {
            if (et_mobile_register.getText().toString().length() != 10 && et_mobile_register.getText().toString().length() < 10) {
                et_mobile_register.requestFocus();
                et_mobile_register.setError(getResources().getString(R.string.Cellphone_number_invalid));
            } else {
                if (Integer.parseInt(String.valueOf(et_mobile_register.getText().toString().charAt(0))) != 0) {
                    et_mobile_register.requestFocus();
                    et_mobile_register.setError(getResources().getString(R.string.Cellphone_number_invalid));
                } else if (Integer.parseInt(String.valueOf(et_mobile_register.getText().toString().charAt(1))) == 0 ||
                        Integer.parseInt(String.valueOf(et_mobile_register.getText().toString().charAt(1))) == 9) {
                    et_mobile_register.requestFocus();
                    et_mobile_register.setError(getResources().getString(R.string.Cellphone_number_invalid));
                } else {
                    register(et_userName.getText().toString(), et_mobile_register.getText().toString());
                }
            }
        }
    }
}