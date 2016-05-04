package com.techmorphosis.grassroot.ui.activities;

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
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.crashlytics.android.Crashlytics;
import com.techmorphosis.grassroot.Network.NetworkCheck;
import com.techmorphosis.grassroot.R;
import com.techmorphosis.grassroot.services.GrassrootRestService;
import com.techmorphosis.grassroot.services.model.GenericResponse;
import com.techmorphosis.grassroot.services.model.Token;
import com.techmorphosis.grassroot.services.model.TokenResponse;
import com.techmorphosis.grassroot.ui.fragments.HomeScreenViewFragment;
import com.techmorphosis.grassroot.ui.fragments.LoginScreenView;
import com.techmorphosis.grassroot.ui.fragments.OtpScreenFragment;
import com.techmorphosis.grassroot.ui.fragments.RegisterScreenFragment;
import com.techmorphosis.grassroot.utils.SettingPreference;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Optional;
import io.fabric.sdk.android.Fabric;
import retrofit.RetrofitError;
import rx.Observer;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

//import butterknife.BindView;

/**
 * Created by admin on 22-Dec-15.
 */
public class StartActivity extends PortraitActivity implements HomeScreenViewFragment.OnHomeScreenInteractionListener,
        RegisterScreenFragment.OnRegisterScreenInteractionListener, LoginScreenView.OnLoginScreenInteractionListener,
        OtpScreenFragment.OnOtpScreenFragmentListener {
    //will fix once we start with mvp implementation

    public boolean exit;
    public static int SCREEN_TIMEOUT = 2000;
    private Handler defaultHandler;
    private String TAG = StartActivity.class.getSimpleName();

    private Snackbar snackBar;
    private DisplayMetrics displayMetrics;
    private int height;
    private boolean otpscreen = false;
    private boolean registerscreen = false;
    public boolean loginscreen = false;
    private ProgressDialog progressDialog;


    @Nullable
    @BindView(R.id.fl_content)
    public FrameLayout flContainer;

    @Nullable
    @BindView(R.id.iv_back)
    public ImageView ivBack;

    @Nullable
    @BindView(R.id.iv_splashlogo)
    public ImageView iv_splashlogo;

    @Nullable
    @BindView(R.id.rl_homelogo)
    RelativeLayout rl_homelogo;

    @Nullable
    @BindView(R.id.rl_start)
    RelativeLayout rlStart;

    GrassrootRestService grassrootRestService = new GrassrootRestService();


    private String userName;
    private String mobileNumber;
    private String data;


    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        Fabric.with(this, new Crashlytics());
        if (!SettingPreference.getisLoggedIn(this)) {
            setContentView(R.layout.start);
            progressDialog = new ProgressDialog(this);
            progressDialog.setMessage("Please Wait..");
            ButterKnife.bind(this);
            if (NetworkCheck.isNetworkAvailable(StartActivity.this)) {
            } else {
                SettingPreference.setisLoggedIn(this, false);
            }
            init();
            displayMetrics = getApplicationContext().getResources().getDisplayMetrics();
            height = displayMetrics.heightPixels;
            start();
        } else {
            setContentView(R.layout.spashscreen);
            ButterKnife.bind(this);
            iv_splashlogo.setVisibility(View.VISIBLE);
            // Animation animFadeIn;
            // animFadeIn = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fade_in);
            // iv_splashlogo.startAnimation(animFadeIn);

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    Intent intent;
                    intent = new Intent(StartActivity.this, HomeScreen.class);

                    startActivity(intent);
                    finish();

                }
            }, 300L);

        }


    }


    private void init() {
        defaultHandler = new Handler();

    }

    private void start() {
        showHomeScreen();
    }

    private void showHomeScreen() {

        if (SettingPreference.getisLoggedIn(this)) {

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    Intent intent = new Intent(StartActivity.this, HomeScreen.class);
                    startActivity(intent);
                    finish();
                }
            }, SCREEN_TIMEOUT);


        } else {
            defaultHandler.postDelayed(
                    new Runnable() {


                        public void run() {

                            rl_homelogo.animate().translationY((float) (-height / 6)).setDuration(500);

                            defaultHandler.postDelayed(
                                    new Runnable() {
                                        public void run() {
                                            setUpHomeScreen();
                                        }

                                    }, 1000L);
                        }


                    }, 500L);
        }
    }

    private void setUpHomeScreen() {

        otpscreen = false;
        loginscreen = false;
        registerscreen = false;
        HomeScreenViewFragment homeScreenViewFragment = new HomeScreenViewFragment();
        getSupportFragmentManager().beginTransaction().add(R.id.fl_content,
                homeScreenViewFragment).commit();

    }

    private void setUpRegisterScreen() {
        data = "";
        otpscreen = false;
        registerscreen = true;
        loginscreen = false;
        ivBack.setVisibility(View.VISIBLE);
        switchFragments(RegisterScreenFragment.newInstance());

    }

    private void setUpLoginScreen() {
        data = "";
        otpscreen = false;
        registerscreen = false;
        loginscreen = true;
        ivBack.setVisibility(View.VISIBLE);
        switchFragments(new LoginScreenView());
    }

    private void setUpOtpScreen() {
        OtpScreenFragment otpScreenFragment = OtpScreenFragment.newInstance(data);
        switchFragments(otpScreenFragment);
        if (registerscreen) {
            otpscreen = true;

        } else if (loginscreen) {
            otpscreen = true;
        }
    }
    private void textResend() {
        if (registerscreen) {
            registerWS(userName, mobileNumber);
        } else if (loginscreen) {
            loginWS(mobileNumber);
        }
    }

    private void otpFormValidation(EditText et_otp) {
        if (et_otp.getText().toString().isEmpty()) {
            et_otp.setError(getResources().getString(R.string.OTP_empty));
        } else {
            if(loginscreen) {
                authenticate(mobileNumber, et_otp.getText().toString());
            }
            else{
                verify(mobileNumber,et_otp.getText().toString());
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
            if(getSupportFragmentManager().getBackStackEntryCount() == 1){
                ivBack.setVisibility(View.INVISIBLE);
            }

        }
    }
    private void registerWS(final String et_userName, final String et_mobile_register) {


        Log.e(TAG, "loginWS");
        progressDialog.show();
        registerscreen =true;
        grassrootRestService.getApi()
                .addUser(et_mobile_register,et_userName)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<GenericResponse>() {
                    @Override
                    public void onCompleted() {
                        progressDialog.dismiss();
                    }
                    @Override
                    public void onError(Throwable e) {
                        progressDialog.dismiss();
                        showSnackBar(getApplicationContext(), "", getResources().getString(R.string.User_not_registered), "", 0, Snackbar.LENGTH_SHORT);
                    }
                    @Override
                    public void onNext(GenericResponse response) {
                        if(response.getStatus().contentEquals("SUCCESS")){
                            data = (String)response.getData();
                            mobileNumber = et_mobile_register;
                        }
                        if (otpscreen) {
                            Log.e(TAG, "not calling setUpOtpScreen");
                            OtpScreenFragment otpScreenFragment = (OtpScreenFragment) getVisibleFragment();
                            otpScreenFragment.et_otp.setText(data);

                        } else {
                            Log.e(TAG, "calling setUpOtpScreen");
                            setUpOtpScreen();
                        }

                    }
                });

    }

    private void loginWS(String mobile_number) {

        Log.e(TAG, "loginWS");
        mobileNumber = mobile_number;
        progressDialog.show();
        loginscreen =true;

        grassrootRestService.getApi()
                .login(mobile_number)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<GenericResponse>() {
                    @Override
                    public void onCompleted() {
                        progressDialog.dismiss();
                    }
                    @Override
                    public void onError(Throwable e) {
                        progressDialog.dismiss();
                        showSnackBar(getApplicationContext(), "", getResources().getString(R.string.User_not_registered), "", 0, Snackbar.LENGTH_SHORT);
                    }
                    @Override
                    public void onNext(GenericResponse response) {
                        if(response.getStatus().contentEquals("SUCCESS")){
                            data = (String)response.getData();
                        }
                        if (otpscreen) {
                            Log.e(TAG, "not calling setUpOtpScreen");
                            OtpScreenFragment otpScreenFragment = (OtpScreenFragment) getVisibleFragment();
                            otpScreenFragment.et_otp.setText(data);
                        } else {
                            Log.e(TAG, "calling setUpOtpScreen");
                            setUpOtpScreen();
                        }
                    }
                });
    }

    private void verify(final String mobileNumber, String tokenCode){
        progressDialog.show();
        grassrootRestService.getApi()
                .verify(mobileNumber,tokenCode)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<TokenResponse>() {
                    @Override
                    public void onCompleted() {
                        progressDialog.dismiss();
                    }
                    @Override
                    public void onError(Throwable e) {
                        progressDialog.dismiss();
                        showSnackBar(getApplicationContext(), "", getResources().getString(R.string.INVALID_TOKEN), "", 0, Snackbar.LENGTH_SHORT);
                    }
                    @Override
                    public void onNext(TokenResponse response) {
                        if(response.getStatus().contentEquals("SUCCESS")){
                            Token token = response.getToken();
                            SettingPreference.setuser_token(StartActivity.this, token.getCode());
                            SettingPreference.setuser_mobilenumber(StartActivity.this, mobileNumber);
                            SettingPreference.setisLoggedIn(StartActivity.this, true);
                            SettingPreference.setuser_phonetoken(StartActivity.this, mobileNumber + "/" + token.getCode());
                            SettingPreference.setuser_name(StartActivity.this, userName);

                            Log.e(TAG, "getPREF_Phone_Token is " + SettingPreference.getPREF_Phone_Token(StartActivity.this));
                            Intent intent = new Intent(StartActivity.this, HomeScreen.class);
                            startActivity(intent);
                            finish();
                        }

                    }
                });
    }

    private void authenticate(final String mobileNumber, String code){
        progressDialog.show();
        grassrootRestService.getApi()
                .authenticate(mobileNumber,code)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<TokenResponse>() {
                    @Override
                    public void onCompleted() {
                        progressDialog.dismiss();
                    }
                    @Override
                    public void onError(Throwable e) {
                        RetrofitError error = (RetrofitError)e;
                        Log.e(TAG, String.valueOf(error.getResponse().getStatus()));
                        progressDialog.dismiss();
                        showSnackBar(getApplicationContext(), "", getResources().getString(R.string.INVALID_TOKEN), "", 0, Snackbar.LENGTH_SHORT);
                    }

                    @Override
                    public void onNext(TokenResponse response) {
                        if(response.getStatus().contentEquals("SUCCESS")){
                            Token token = response.getToken();
                            SettingPreference.setuser_token(StartActivity.this, token.getCode());
                            SettingPreference.setuser_mobilenumber(StartActivity.this, mobileNumber);
                            SettingPreference.setisLoggedIn(StartActivity.this, true);
                            SettingPreference.setuser_phonetoken(StartActivity.this, mobileNumber + "/" + token.getCode());
                            Log.e(TAG, "getPREF_Phone_Token is " + SettingPreference.getPREF_Phone_Token(StartActivity.this));

                            Boolean hasGroups = response.getHasGroups();
                            String displayname = response.getDisplayName();

                            Log.e(TAG, "hasGroups is " + hasGroups);
                            Log.e(TAG, "displayname is " + displayname);
                            if (hasGroups) {
                                SettingPreference.setisHasgroup(StartActivity.this, true);
                               SettingPreference.setuser_name(StartActivity.this, displayname);
                                Intent intent = new Intent(StartActivity.this, HomeScreen.class);
                                startActivity(intent);
                                finish();

                            } else {
                                Intent intent = new Intent(StartActivity.this, HomeScreen.class);
                                startActivity(intent);
                                finish();

                            }

                        }
                        }


                });

    }

    public void showSnackBar(Context context, final String type, String message, String textLabel, int color, int length) {

        snackBar = Snackbar.make(rlStart, message, length);
        View view = snackBar.getView();

        snackBar.setActionTextColor(Color.RED);
        if (!textLabel.isEmpty())//show action button depending on Label
        {
            snackBar.setAction(textLabel, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (type.equals("registerWS"))//take action depending on type
                    {
                         registerWS(userName,mobileNumber);
                        snackBar.dismiss();
                        // getNotification();

                    } else if (type.equals("loginWS")) {

                          loginWS(mobileNumber);
                        snackBar.dismiss();

                    } else if (type.equals("OtpWS")) {
                     //   OT
                        snackBar.dismiss();

                    }

                }
            });

        }
        Log.e(TAG, "show");

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

                        }, 500L);
            }

        }, 500L);
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

                        }, 500L);
            }

        }, 500L);
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
    
    private void switchFragments(Fragment fragment){
        getSupportFragmentManager().beginTransaction().setCustomAnimations(R.anim.a_slide_in_right,
                R.anim.a_slide_out_left,
                R.anim.a_slide_in_left, R.anim.a_slide_out_right).replace(R.id.fl_content, fragment)
                .addToBackStack(fragment.getClass().getName()).commit();

    }

    private void loginFormValidation(EditText et_mobile_login) {
        if (et_mobile_login.getText().toString().isEmpty()) {
            et_mobile_login.requestFocus();
            et_mobile_login.setError(getResources().getString(R.string.Cellphone_numbr_empty));
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
                    loginWS(et_mobile_login.getText().toString());

                }
            }

        }
    }

    private void registerFormValidation(EditText et_userName, EditText et_mobile_register) {
        if (et_userName.getText().toString().trim().isEmpty() || et_mobile_register.getText().toString().isEmpty()) {
            if(et_userName.getText().toString().trim().isEmpty() && !et_mobile_register.getText().toString().isEmpty()) {
                et_userName.requestFocus();
                et_userName.setError(getResources().getString(R.string.Name_Empty));
            }else if(et_mobile_register.getText().toString().isEmpty()  &&  !et_userName.getText().toString().isEmpty()){
                et_mobile_register.requestFocus();
                et_mobile_register.setError(getResources().getString(R.string.Cellphone_numbr_empty));
            }
            else{
                et_userName.setError(getResources().getString(R.string.Name_Empty));
                et_mobile_register.setError(getResources().getString(R.string.Cellphone_numbr_empty));
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
                   registerWS(et_userName.getText().toString(), et_mobile_register.getText().toString());


                }
            }
        }
    }

}