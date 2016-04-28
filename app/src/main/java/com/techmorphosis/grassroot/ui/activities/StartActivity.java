package com.techmorphosis.grassroot.ui.activities;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkError;
import com.android.volley.NoConnectionError;
import com.android.volley.ParseError;
import com.android.volley.ServerError;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.crashlytics.android.Crashlytics;
import com.techmorphosis.grassroot.Network.AllLinsks;
import com.techmorphosis.grassroot.Network.NetworkCall;
import com.techmorphosis.grassroot.Network.NetworkCheck;
import com.techmorphosis.grassroot.R;
import com.techmorphosis.grassroot.ui.fragments.HomeScreenViewFragment;
import com.techmorphosis.grassroot.ui.fragments.LoginScreenView;
import com.techmorphosis.grassroot.ui.fragments.OtpScreenFragment;
import com.techmorphosis.grassroot.ui.fragments.RegisterScreenFragment;
import com.techmorphosis.grassroot.utils.SettingPreffrence;
import com.techmorphosis.grassroot.utils.listener.ErrorListenerVolley;
import com.techmorphosis.grassroot.utils.listener.ResponseListenerVolley;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Optional;
import io.fabric.sdk.android.Fabric;

/**
 * Created by admin on 22-Dec-15.
 */
public class StartActivity extends PortraitActivity implements HomeScreenViewFragment.OnHomeScreenInteractionListener,
        RegisterScreenFragment.OnRegisterScreenInteractionListener, LoginScreenView.OnLoginScreenInteractionListener, OtpScreenFragment.OnOtpScreenFragmentListener {
    //will fix once we start with mvp implementation

    public boolean exit;
    public static int SCREEN_TIMEOUT = 2000;
    private Handler defaultHandler;
    private String TAG = StartActivity.class.getSimpleName();

    private Snackbar snackBar;
    private DisplayMetrics displayMetrics;
    private int width;
    private int height;
    private View vOtpScreen;
    private boolean otpscreen = false;
    private boolean homescreen = false;
    private boolean registerscreen = false;
    public boolean loginscreen = false;

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

    private HomeScreenViewFragment homeScreenViewFragment;
    private RegisterScreenFragment registerScreenFragment;
    private LoginScreenView loginScreenView;
    private String userName;
    private String mobileNumber;
    private String data;


    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        Fabric.with(this, new Crashlytics());
        if (!SettingPreffrence.getisLoggedIn(this)) {
            setContentView(R.layout.start);
            ButterKnife.bind(this);
            if (NetworkCheck.isNetworkAvailable(StartActivity.this)) {
            } else {
                SettingPreffrence.setisLoggedIn(this, false);
            }
            init();
            displayMetrics = getApplicationContext().getResources().getDisplayMetrics();
            width = displayMetrics.widthPixels;
            height = displayMetrics.heightPixels;
            start();

        } else {
            setContentView(R.layout.spashscreen);
            ButterKnife.bind(this);
            iv_splashlogo.setVisibility(View.VISIBLE);

            Animation animFadeIn;
            animFadeIn = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fade_in);
            iv_splashlogo.startAnimation(animFadeIn);

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    Intent intent;
                    intent = new Intent(StartActivity.this, HomeScreen.class);

                    startActivity(intent);
                    finish();

                }
            }, 2000L);

        }


    }


    private void init() {
        defaultHandler = new Handler();

    }

    private void start() {
        showHomeScreen();
    }

    private void showHomeScreen() {

        if (SettingPreffrence.getisLoggedIn(this)) {

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

        exit = true;
        homescreen = true;
        otpscreen = false;
        loginscreen = false;
        registerscreen = false;
        homeScreenViewFragment = new HomeScreenViewFragment();

        getSupportFragmentManager().beginTransaction().add(R.id.fl_content,
                homeScreenViewFragment).commit();

    }

    private void setUpRegisterScreen() {

        data = "";
        exit = false;
        homescreen = false;
        otpscreen = false;
        registerscreen = true;
        loginscreen = false;
        ivBack.setVisibility(View.VISIBLE);
        registerScreenFragment = new RegisterScreenFragment();
        switchFragments(registerScreenFragment);



    }

    private void setUpLoginScreen() {
        data = "";
        exit = false;

        homescreen = false;
        otpscreen = false;
        registerscreen = false;
        loginscreen = true;
        ivBack.setVisibility(View.VISIBLE);
        loginScreenView = new LoginScreenView();
        switchFragments(loginScreenView);
    }

    private void setUpOtpScreen() {


        exit = false;//
        vOtpScreen = getLayoutInflater().inflate(R.layout.container_otp, null);
        vOtpScreen.setLayoutParams(new ViewGroup.LayoutParams(-1, -1));

        ivBack.setVisibility(View.VISIBLE);

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
            RegisterWS(userName, mobileNumber);
        } else if (loginscreen) {
            loginWS(mobileNumber);
        }
    }


    private void otpFormValidation(EditText et_otp) {

        if (et_otp.getText().toString().isEmpty()) {
            // utilClass.showToast(getApplicationContext(),"empty");
            showSnackBar(getApplicationContext(), "", getResources().getString(R.string.OTP_empty), "", 0, Snackbar.LENGTH_SHORT);

        } else {
            // utilClass.showToast(getApplicationContext(),"OtpWS WS");
            OtpWS();
            // showSnackBar(getApplicationContext(), "", "Otp success", "", 0, Snackbar.LENGTH_SHORT);


        }

    }

    private void loginFormValidation(EditText et_mobile_login) {

        if (et_mobile_login.getText().toString().isEmpty()) {
            //utilClass.showToast(getApplicationContext(),"et_mobile_login");
            showSnackBar(getApplicationContext(), "", getResources().getString(R.string.Either_field_empty), "", 0, Snackbar.LENGTH_SHORT);

        } else {
            if (et_mobile_login.getText().toString().length() != 10 && et_mobile_login.getText().toString().length() < 10) {

                //utilClass.showToast(getApplicationContext(),"not valid");
                showSnackBar(getApplicationContext(), "", getResources().getString(R.string.Cellphone_number_invalid), "", 0, Snackbar.LENGTH_SHORT);

            } else {

                if (Integer.parseInt(String.valueOf(et_mobile_login.getText().toString().charAt(0))) != 0) {
                    //utilClass.showToast(getApplicationContext(),"incorrect " + et_mobile_login.getText().toString().charAt(0));
                    showSnackBar(getApplicationContext(), "", getResources().getString(R.string.Cellphone_number_invalid), "", 0, Snackbar.LENGTH_SHORT);

                } else if (Integer.parseInt(String.valueOf(et_mobile_login.getText().toString().charAt(1))) == 0 || Integer.parseInt(String.valueOf(et_mobile_login.getText().toString().charAt(1))) == 9) {
                    showSnackBar(getApplicationContext(), "", getResources().getString(R.string.Cellphone_number_invalid), "", 0, Snackbar.LENGTH_SHORT);

                } else {
                    loginWS(et_mobile_login.getText().toString());
                }
            }

        }
    }

    private void registerFormValidation(EditText et_userName, EditText et_mobile_register) {


        if (et_userName.getText().toString().trim().isEmpty() || et_mobile_register.getText().toString().isEmpty()) {
            // utilClass.showToast(getApplicationContext(),"both");
            showSnackBar(getApplicationContext(), "", getResources().getString(R.string.Either_field_empty), "", 0, Snackbar.LENGTH_SHORT);

        } else {
            if (et_mobile_register.getText().toString().length() != 10 && et_mobile_register.getText().toString().length() < 10) {
                //utilClass.showToast(getApplicationContext(),"not valid");
                showSnackBar(getApplicationContext(), "", getResources().getString(R.string.Cellphone_number_invalid), "", 0, Snackbar.LENGTH_SHORT);

            } else {

                if (Integer.parseInt(String.valueOf(et_mobile_register.getText().toString().charAt(0))) != 0) {
                    //utilClass.showToast(getApplicationContext(),"incorrect " + et_mobile_register.getText().toString().charAt(0));
                    showSnackBar(getApplicationContext(), "", getResources().getString(R.string.Cellphone_number_invalid), "", 0, Snackbar.LENGTH_SHORT);

                } else if (Integer.parseInt(String.valueOf(et_mobile_register.getText().toString().charAt(1))) == 0 || Integer.parseInt(String.valueOf(et_mobile_register.getText().toString().charAt(1))) == 9) {
                    showSnackBar(getApplicationContext(), "", getResources().getString(R.string.Cellphone_number_invalid), "", 0, Snackbar.LENGTH_SHORT);

                } else {

                    RegisterWS(et_userName.getText().toString(), et_mobile_register.getText().toString());

                }
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

        if (getSupportFragmentManager().getBackStackEntryCount() < 1 || exit) {
            super.onBackPressed();

        } else {

            if (!(getVisibleFragment() instanceof OtpScreenFragment)) {
                rl_homelogo.animate().translationY((float) (-height / 6)).scaleX(1).scaleY(1);
            }

            getSupportFragmentManager().popBackStack();
            if(getSupportFragmentManager().getBackStackEntryCount() == 1){
                ivBack.setVisibility(View.INVISIBLE);
            }

            exit = true;


        }
    }




    private void RegisterWS(String et_userName, String et_mobile_register) {
        Log.e(TAG, "RegisterWS");


        String prgMessage = "Please Wait..";
        boolean prgboolean = true;
        userName = et_userName;
        mobileNumber = et_mobile_register;


        try {
            Log.e(TAG, "link is " + AllLinsks.register + URLEncoder.encode(et_mobile_register, "UTF-8") + "/" + URLEncoder.encode(et_userName, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }


        NetworkCall networkCall = null;
        try {
            networkCall = new NetworkCall(StartActivity.this,

                    new ResponseListenerVolley() {

                        @Override

                        public void onSuccess(String s) {
                            Log.e(TAG, " onSuccess " + s);
                            try {
                                String status, message, code = null;
                                JSONObject register = new JSONObject(s);

                                status = register.getString("status");
                                message = register.getString("message");

                                if (status.equalsIgnoreCase("SUCCESS")) {
                                    Log.e(TAG, "success");

                                    code = register.getString("code");
                                    message = register.getString("message");
                                    data = register.getString("data");

                                    Log.e(TAG, "code is " + code);
                                    Log.e(TAG, "message is " + message);
                                    Log.e(TAG, "data is " + data);

                                    showSnackBar(getApplicationContext(), "", getResources().getString(R.string.Otp_success), "", 0, Snackbar.LENGTH_SHORT);


                                    if (otpscreen) {
                                        Log.e(TAG, "not calling setUpOtpScreen");
                                        OtpScreenFragment otpScreenFragment = (OtpScreenFragment) getVisibleFragment();
                                        otpScreenFragment.et_otp.setText(data);

                                    } else {
                                        Log.e(TAG, "calling setUpOtpScreen");
                                        setUpOtpScreen();
                                    }

                                }

                            } catch (JSONException e) {
                                e.printStackTrace();
                            }

                        }
                    },
                    new ErrorListenerVolley() {
                        @Override
                        public void onError(VolleyError volleyError) {


                            if (volleyError instanceof NetworkError) {
                                Log.e(TAG, "NetworkError");

                            } else if (volleyError instanceof ServerError) {

                                Log.e(TAG, "ServerError");

                            } else if (volleyError instanceof AuthFailureError) {
                                Log.e(TAG, "AuthFailureError");

                            } else if (volleyError instanceof ParseError) {
                                Log.e(TAG, "ParseError");

                            } else if (volleyError instanceof NoConnectionError) {
                                Log.e(TAG, "NoConnectionError");

                            } else if (volleyError instanceof TimeoutError) {
                                Log.e(TAG, "TimeoutError");

                            }


                            if ((volleyError instanceof NoConnectionError) || (volleyError instanceof TimeoutError)) {
                                showSnackBar(getApplicationContext(), "RegisterWS", getResources().getString(R.string.No_network), getString(R.string.Retry), 0, Snackbar.LENGTH_INDEFINITE);
                            } else {
                                try {
                                    String responseBody = new String(volleyError.networkResponse.data, "utf-8");
                                    Log.e(TAG, "responseBody " + responseBody);
                                    String status, message, code = null;
                                    JSONObject register = new JSONObject(responseBody);
                                    status = register.getString("status");
                                    message = register.getString("message");

                                    if (status.equalsIgnoreCase("Failure")) {
                                        Log.e(TAG, "failure");
                                        Log.e(TAG, "code is " + code);
                                        Log.e(TAG, "message is " + message);
                                        showSnackBar(getApplicationContext(), "", getResources().getString(R.string.USER_ALREADY_EXISTS), "", 0, Snackbar.LENGTH_SHORT);
                                    }

                                } catch (UnsupportedEncodingException error) {

                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }



                            /*if ((volleyError instanceof NoConnectionError) || (volleyError instanceof TimeoutError))
                            {
                                showSnackBar(getApplicationContext(), "RegisterWS", getResources().getString(R.string.No_network), getString(R.string.Retry), 0, Snackbar.LENGTH_INDEFINITE);

                            }
                            else if ((volleyError instanceof ServerError) || (volleyError instanceof AuthFailureError))
                            {
                                showSnackBar(getApplicationContext(), "", getResources().getString(R.string.Unknown_error), "", 0, Snackbar.LENGTH_SHORT);
                            }*/

                        }
                    },
                    AllLinsks.register + URLEncoder.encode(et_mobile_register, "UTF-8") + "/" + URLEncoder.encode(et_userName, "UTF-8"),
                    prgMessage,
                    prgboolean
            );
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        networkCall.makeStringRequest_GET();

        Log.e(TAG, "End");

    }

    private void loginWS(String mobile_number) {

        Log.e(TAG, "loginWS");
        String prgMessage = "Please Wait..";
        boolean prgboolean = true;
        mobileNumber = mobile_number;

        try {
            Log.e(TAG, "link is " + AllLinsks.login + URLEncoder.encode(mobile_number, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        NetworkCall networkCall = null;
        try {
            networkCall = new NetworkCall(StartActivity.this,

                    new ResponseListenerVolley() {

                        @Override

                        public void onSuccess(String s) {
                            Log.e(TAG, " onSuccess " + s);
                            try {
                                String status, message, code = null;
                                JSONObject login = new JSONObject(s);

                                status = login.getString("status");
                                message = login.getString("message");

                                if (status.equalsIgnoreCase("SUCCESS")) {
                                    Log.e(TAG, "success");

                                    code = login.getString("code");
                                    message = login.getString("message");
                                    data = login.getString("data");

                                    Log.e(TAG, "code is " + code);
                                    Log.e(TAG, "message is " + message);
                                    Log.e(TAG, "data is " + data);

                                    showSnackBar(getApplicationContext(), "", getResources().getString(R.string.Otp_success), "", 0, Snackbar.LENGTH_SHORT);

                                    if (otpscreen) {
                                        Log.e(TAG, "not calling setUpOtpScreen");
                                        OtpScreenFragment otpScreenFragment = (OtpScreenFragment) getVisibleFragment();
                                        otpScreenFragment.et_otp.setText(data);


                                    } else {
                                        Log.e(TAG, "calling setUpOtpScreen");

                                        setUpOtpScreen();
                                    }

                                }

                            } catch (JSONException e) {
                                e.printStackTrace();
                            }

                        }
                    },
                    new ErrorListenerVolley() {
                        @Override
                        public void onError(VolleyError volleyError) {

                            if ((volleyError instanceof NoConnectionError) || (volleyError instanceof TimeoutError)) {
                                showSnackBar(getApplicationContext(), "loginWS", getResources().getString(R.string.No_network), getString(R.string.Retry), 0, Snackbar.LENGTH_INDEFINITE);
                            } else {
                                try {
                                    String responseBody = new String(volleyError.networkResponse.data, "utf-8");
                                    Log.e(TAG, "responseBody " + responseBody);
                                    String status, message, code = null;
                                    JSONObject login = new JSONObject(responseBody);
                                    status = login.getString("status");
                                    message = login.getString("message");

                                    if (status.equalsIgnoreCase("Failure")) {
                                        Log.e(TAG, "failure");
                                        Log.e(TAG, "code is " + code);
                                        Log.e(TAG, "message is " + message);
                                        showSnackBar(getApplicationContext(), "", getResources().getString(R.string.User_not_registered), "", 0, Snackbar.LENGTH_SHORT);


                                    }

                                } catch (UnsupportedEncodingException error) {

                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }


                        }
                    },
                    AllLinsks.login + URLEncoder.encode(mobile_number, "UTF-8"),
                    prgMessage,
                    prgboolean
            );
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        networkCall.makeStringRequest_GET();

        Log.e(TAG, "End");

    }

    private void OtpWS() {

        Log.e(TAG, "OtpWS");


        String prgMessage = "Please Wait..";
        boolean prgboolean = true;
        String OTPLink = null;

        if (registerscreen) {
            try {
                Log.e(TAG, "link is " + AllLinsks.verify + URLEncoder.encode(userName, "UTF-8") + "/" + data);
                OTPLink = AllLinsks.verify + URLEncoder.encode(mobileNumber, "UTF-8") + "/" + data;
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }

        } else if (loginscreen) {
            try {
                Log.e(TAG, "link is " + AllLinsks.authenticate + URLEncoder.encode(mobileNumber, "UTF-8") + "/" + data);
                OTPLink = AllLinsks.authenticate + URLEncoder.encode(mobileNumber, "UTF-8") + "/" + data;
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }


        NetworkCall networkCall = null;
        networkCall = new NetworkCall(StartActivity.this,

                new ResponseListenerVolley() {

                    @Override

                    public void onSuccess(String s) {
                        Log.e(TAG, " onSuccess " + s);
                        try {
                            String status, message, code = null;
                            JSONObject jsonobject = new JSONObject(s);
                            JSONObject jsonobject2;

                            status = jsonobject.getString("status");
                            message = jsonobject.getString("message");

                            if (status.equalsIgnoreCase("SUCCESS")) {
                                Log.e(TAG, "success");

                                code = jsonobject.getString("code");
                                message = jsonobject.getString("message");
                                String customData = jsonobject.getString("data");
                                JSONObject jsonObject2 = new JSONObject(customData);
                                String token_code = jsonObject2.getString("code");
                                String createdDateTime = jsonObject2.getString("createdDateTime");
                                String expiryDateTime = jsonObject2.getString("expiryDateTime");


                                Log.e(TAG, "code is " + code);
                                Log.e(TAG, "message is " + message);
                                Log.e(TAG, "customData is " + data);
                                Log.e(TAG, "token_code is " + token_code);
                                Log.e(TAG, "createdDateTime is " + createdDateTime);
                                Log.e(TAG, "expiryDateTime is " + expiryDateTime);


                                if (registerscreen) {
                                    SettingPreffrence.setuser_token(StartActivity.this, token_code);
                                    SettingPreffrence.setuser_mobilenumber(StartActivity.this, mobileNumber);
                                    SettingPreffrence.setisLoggedIn(StartActivity.this, true);
                                    SettingPreffrence.setuser_phonetoken(StartActivity.this, mobileNumber + "/" + token_code);
                                    SettingPreffrence.setuser_name(StartActivity.this, userName);

                                    Log.e(TAG, "getPREF_Phone_Token is " + SettingPreffrence.getPREF_Phone_Token(StartActivity.this));

                                    Intent intent = new Intent(StartActivity.this, HomeScreen.class);
                                    startActivity(intent);
                                    finish();

                                } else if (loginscreen) {

                                    SettingPreffrence.setuser_token(StartActivity.this, token_code);
                                    SettingPreffrence.setuser_mobilenumber(StartActivity.this, mobileNumber);
                                    SettingPreffrence.setisLoggedIn(StartActivity.this, true);
                                    SettingPreffrence.setuser_phonetoken(StartActivity.this, mobileNumber + "/" + token_code);
                                    Log.e(TAG, "getPREF_Phone_Token is " + SettingPreffrence.getPREF_Phone_Token(StartActivity.this));

                                    Boolean hasGroups = jsonobject.getBoolean("hasGroups");
                                    String displayname = jsonobject.getString("displayName");

                                    Log.e(TAG, "hasGroups is " + hasGroups);
                                    Log.e(TAG, "displayname is " + displayname);
                                    if (hasGroups) {
                                        SettingPreffrence.setisHasgroup(StartActivity.this, true);
                                        SettingPreffrence.setuser_name(StartActivity.this, displayname);
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

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                    }
                },
                new ErrorListenerVolley() {
                    @Override
                    public void onError(VolleyError volleyError) {

                        if ((volleyError instanceof NoConnectionError) || (volleyError instanceof TimeoutError)) {
                            showSnackBar(getApplicationContext(), "OtpWS", getResources().getString(R.string.No_network), getString(R.string.Retry), 0, Snackbar.LENGTH_INDEFINITE);
                        } else {
                            try {
                                String responseBody = new String(volleyError.networkResponse.data, "utf-8");
                                Log.e(TAG, "responseBody " + responseBody);
                                String status, message, code = null;
                                JSONObject otp = new JSONObject(responseBody);
                                status = otp.getString("status");
                                message = otp.getString("message");

                                if (status.equalsIgnoreCase("Failure")) {
                                    Log.e(TAG, "failure");
                                    Log.e(TAG, "code is " + code);
                                    Log.e(TAG, "message is " + message);
                                    showSnackBar(getApplicationContext(), "", getResources().getString(R.string.INVALID_TOKEN), "", 0, Snackbar.LENGTH_SHORT);
                                }

                            } catch (UnsupportedEncodingException error) {

                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }

                    }
                },
                OTPLink,
                prgMessage,
                prgboolean
        );

        networkCall.makeStringRequest_GET();

        Log.e(TAG, "End");


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
                    if (type.equals("RegisterWS"))//take action depending on type
                    {
                        // RegisterWS();
                        snackBar.dismiss();
                        // getNotification();

                    } else if (type.equals("loginWS")) {

                        //  loginWS(et_mobile_register);
                        snackBar.dismiss();

                    } else if (type.equals("OtpWS")) {
                        OtpWS();
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
        FragmentManager fragmentManager = getSupportFragmentManager();
        List<Fragment> fragments = fragmentManager.getFragments();
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
}