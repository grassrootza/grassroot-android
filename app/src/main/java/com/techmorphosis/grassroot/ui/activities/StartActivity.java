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
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

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
import com.techmorphosis.grassroot.ui.fragments.RegisterScreenFragment;
import com.techmorphosis.grassroot.utils.AnimUtils;
import com.techmorphosis.grassroot.utils.SettingPreffrence;
import com.techmorphosis.grassroot.utils.UIUtils;
import com.techmorphosis.grassroot.utils.UtilClass;
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
import io.fabric.sdk.android.Fabric;

/**
 * Created by admin on 22-Dec-15.
 */
public class StartActivity extends PortraitActivity implements HomeScreenViewFragment.OnHomeScreenInteractionListener,
        RegisterScreenFragment.OnRegisterScreenInteractionListener, LoginScreenView.OnLoginScreenInteractionListener {
       //will fix once we star with mvp implementation

    public boolean exit;
    public static int SCREEN_TIMEOUT = 2000;
    private Handler defaultHandler;
    @BindView(R.id.fl_content)
    public FrameLayout flContainer;
    @BindView(R.id.iv_back)
    public ImageView ivBack;

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
    // @BindView(R.id.et_mobile_register)
    EditText et_mobile_register;
    //   @BindView(R.id.et_userName)
    EditText et_userName;
    @Nullable
    @BindView(R.id.iv_splashlogo)
    ImageView iv_splashlogo;
    @BindView(R.id.rl_homelogo)
    RelativeLayout rl_homelogo;

    EditText et_otp;

    EditText et_mobile_login;
    @BindView(R.id.rl_start)
    RelativeLayout rlStart;
    //  @BindView(R.id.txt_resend)
    TextView txtResend;


    private FragmentManager fragmentManager = getSupportFragmentManager();

    private HomeScreenViewFragment homeScreenViewFragment;
    private RegisterScreenFragment registerScreenFragment;
    private LoginScreenView loginScreenView;

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
        ;
        fragmentManager.beginTransaction().add(R.id.fl_content, homeScreenViewFragment).commit();


    }

    private void setUpRegisterScreen() {

        data = "";
        exit = false;
        homescreen = false;
        otpscreen = false;
        registerscreen = true;
        loginscreen = false;


//        vRegisterScreen.findViewById(R.id.bt_register).setOnClickListener(buttonRegisterClickListener(et_userName, et_mobile_register));
        ivBack.setVisibility(View.VISIBLE);

        registerScreenFragment = new RegisterScreenFragment();
        fragmentManager.beginTransaction().replace(R.id.fl_content, registerScreenFragment).commit();

        // fragmentTransaction.commit();

        //   AnimUtils.forwardAnimation(this, vRegisterScreen, vHomeScreen);

    }

    private void setUpLoginScreen() {
        data = "";
        exit = false;

        homescreen = false;
        otpscreen = false;
        registerscreen = false;
        loginscreen = true;

        ivBack.setVisibility(View.VISIBLE);
        //  vLoginScreen.findViewById(R.id.bt_login).setOnClickListener(buttonLoginClickListener(et_mobile_login));
        //   AnimUtils.forwardAnimation(this, vLoginScreen, vHomeScreen);
        loginScreenView = new LoginScreenView();
        fragmentManager.beginTransaction().replace(R.id.fl_content, loginScreenView).commit();
    }

    private void setUpOtpScreen() {


        exit = false;//


        vOtpScreen = getLayoutInflater().inflate(R.layout.container_otp, null);
        vOtpScreen.setLayoutParams(new ViewGroup.LayoutParams(-1, -1));
        et_otp = (EditText) vOtpScreen.findViewById(R.id.et_otp);
        txtResend = (TextView) vOtpScreen.findViewById(R.id.txt_resend);
        et_otp.setText(data);

        ivBack.setVisibility(View.VISIBLE);

       //vOtpScreen.findViewById(R.id.bt_submit_otp).setOnClickListener(buttonOtpsubmitClickListener(data));
        txtResend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (registerscreen) {
                    RegisterWS();
                } else if (loginscreen) {
                    LoginWS();
                }
            }
        });

        flContainer.addView(vOtpScreen);
        if (registerscreen) {
            // flContainer.removeView(vRegisterScreen);
            //AnimUtils.forwardAnimation(this, vOtpScreen, vRegisterScreen);
            otpscreen = true;

        } else if (loginscreen) {
            //  flContainer.removeView(vLoginScreen);
            //   AnimUtils.forwardAnimation(this, vOtpScreen, vLoginScreen);
            otpscreen = true;
        }


    }


    private View.OnClickListener buttonRegisterClickListener(final EditText et_userName, final EditText et_mobile_register) {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                //setUpOtpScreen();
                registerFormValidation();
            }
        };
    }

    private View.OnClickListener buttonLoginClickListener(final EditText et_mobile_login) {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //setUpOtpScreen();
                LoginFormValidation(et_mobile_login);
            }
        };
    }


    private void buttonOtpsubmitClickListener() {

                otpFormValidation();



    }

    private void otpFormValidation() {

        if (et_otp.getText().toString().isEmpty()) {
            // utilClass.showToast(getApplicationContext(),"empty");
            showSnackBar(getApplicationContext(), "", getResources().getString(R.string.OTP_empty), "", 0, Snackbar.LENGTH_SHORT);

        } else {
            // utilClass.showToast(getApplicationContext(),"OtpWS WS");
            OtpWS();
            // showSnackBar(getApplicationContext(), "", "Otp success", "", 0, Snackbar.LENGTH_SHORT);


        }

    }

    private void LoginFormValidation(EditText et_mobile_login) {

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

                    //utilClass.showToast(getApplicationContext(),"Register WS");
                    // showSnackBar(getApplicationContext(), "", "Login success", "", 0, Snackbar.LENGTH_SHORT);
                    // setUpOtpScreen();
                    //   RegisterWS(et_userName,et_mobile_login);
                    LoginWS();
                }
            }

        }
    }

    private void registerFormValidation() {


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

                    //utilClass.showToast(getApplicationContext(),"Register WS");
                    //showSnackBar(getApplicationContext(), "", "Register success", "", 0, Snackbar.LENGTH_SHORT);
                    //setUpOtpScreen();

                    /*Replace all spaces from name */
                    /*RegisterWS(et_userName.getText().toString().replaceAll(" ", "%20"), et_mobile_register.getText().toString());*/
                    RegisterWS();

                }
            }
        }
    }


    private View.OnClickListener LoginScreenClickListener() {
        return new View.OnClickListener() {


            public void onClick(View view) {


                rl_homelogo.animate()
                        .translationY((float) (-height / 3.5))
                        .scaleX((float) 0.7)
                        .scaleY((float) 0.7);
                       /* .setListener(new Animator.AnimatorListener() {
                            @Override
                            public void onAnimationStart(Animator animation) {

                            }

                            @Override
                            public void onAnimationEnd(Animator animation) {
                                setUpLoginScreen();
                            }

                            @Override
                            public void onAnimationCancel(Animator animation) {

                            }

                            @Override
                            public void onAnimationRepeat(Animator animation) {

                            }
                        });*/

                defaultHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        setUpLoginScreen();

                    }
                }, 500L);

            }


        };
    }


    private View.OnClickListener setUpRegisterScreenClick() {
        return new View.OnClickListener() {


            public void onClick(View view) {
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


        };
    }


 /*   private View.OnClickListener backPressListener() {
        return new View.OnClickListener() {


            public void onClick(View view) {


                try {
                    InputMethodManager im = (InputMethodManager) getApplicationContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                    im.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);

                    snackBar.dismiss();

                } catch (Exception e) {
                    e.printStackTrace();
                }


                onBackPressed();


            }


        };
    }*/

  /*  @OnClick(R.id.iv_back)
    public void onBackPress() {
        try {
            InputMethodManager im = (InputMethodManager) getApplicationContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            im.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
            snackBar.dismiss();
        } catch (Exception e) {
            e.printStackTrace();
        }
        onBackPressed();

    }*/

    @OnClick(R.id.iv_back)
    public void onBackPressed() {

        try {
            InputMethodManager im = (InputMethodManager) getApplicationContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            im.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
            snackBar.dismiss();
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (exit) {
            super.onBackPressed();

        } else {

            if (otpscreen) {
                if (registerscreen) {
                    //  UIUtils.replaceView(flContainer, vRegisterScreen, vOtpScreen);
                    //  AnimUtils.backwardAnimation(StartActivity.this, vRegisterScreen, vOtpScreen);
                    otpscreen = false;
                } else if (loginscreen) {
                    //  UIUtils.replaceView(flContainer, vLoginScreen, vOtpScreen);
                    //  AnimUtils.backwardAnimation(StartActivity.this, vLoginScreen, vOtpScreen);
                    otpscreen = false;

                }


            } else if (loginscreen) {
                rl_homelogo.animate().translationY((float) (-height / 6)).scaleX(1).scaleY(1);
                //   UIUtils.replaceView(flContainer, vHomeScreen, vLoginScreen);
                //  AnimUtils.backwardAnimation(StartActivity.this, vHomeScreen, vLoginScreen);
                ivBack.setVisibility(View.INVISIBLE);
                exit = true;

            } else if (registerscreen) {


                rl_homelogo.animate().translationY((float) (-height / 6)).scaleX(1).scaleY(1);
                // UIUtils.replaceView(flContainer, vHomeScreen, vRegisterScreen);
                //    AnimUtils.backwardAnimation(StartActivity.this, vHomeScreen, vRegisterScreen);
                ivBack.setVisibility(View.INVISIBLE);
                exit = true;
            }


        }
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

    private void RegisterWS() {

        Log.e(TAG, "RegisterWS");


        String prgMessage = "Please Wait..";
        boolean prgboolean = true;

        try {
            Log.e(TAG, "link is " + AllLinsks.register + URLEncoder.encode(StartActivity.this.et_mobile_register.getText().toString(), "UTF-8") + "/" + URLEncoder.encode(StartActivity.this.et_userName.getText().toString(), "UTF-8"));
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

                                   /* et_userName.setText("");
                                    et_mobile_register.setText("");*/

                                    if (otpscreen) {
                                        Log.e(TAG, "not calling setUpOtpScreen");
                                        et_otp.setText(data);

                                    } else {
                                        Log.e(TAG, "calling setUpOtpScreen");
                                        setUpOtpScreen();
                                    }

                                } /*else if (status.equalsIgnoreCase("Failure")) {
                                    Log.e(TAG, "failure");
                                    Log.e(TAG, "code is " + code);
                                    Log.e(TAG, "message is " + message);
                                    showSnackBar(getApplicationContext(), "", message, "", 0, Snackbar.LENGTH_SHORT);


                                }*/


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
                    AllLinsks.register + URLEncoder.encode(StartActivity.this.et_mobile_register.getText().toString(), "UTF-8") + "/" + URLEncoder.encode(StartActivity.this.et_userName.getText().toString(), "UTF-8"),
                    prgMessage,
                    prgboolean
            );
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        networkCall.makeStringRequest_GET();

        Log.e(TAG, "End");

    }

    private void LoginWS() {


        Log.e(TAG, "LoginWS");


        String prgMessage = "Please Wait..";
        boolean prgboolean = true;

        try {
            Log.e(TAG, "link is " + AllLinsks.login + URLEncoder.encode(StartActivity.this.et_mobile_login.getText().toString(), "UTF-8"));
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

                                   /* et_userName.setText("");
                                    et_mobile_login.setText("");*/
                                    showSnackBar(getApplicationContext(), "", getResources().getString(R.string.Otp_success), "", 0, Snackbar.LENGTH_SHORT);

                                    if (otpscreen) {
                                        Log.e(TAG, "not calling setUpOtpScreen");
                                        et_otp.setText(data);

                                    } else {
                                        Log.e(TAG, "calling setUpOtpScreen");

                                        setUpOtpScreen();
                                    }

                                } /*else if (status.equalsIgnoreCase("Failure")) {
                                    Log.e(TAG, "failure");
                                    Log.e(TAG, "code is " + code);
                                    Log.e(TAG, "message is " + message);
                                    showSnackBar(getApplicationContext(), "", message, "", 0, Snackbar.LENGTH_SHORT);


                                }*/


                            } catch (JSONException e) {
                                e.printStackTrace();
                            }

                        }
                    },
                    new ErrorListenerVolley() {
                        @Override
                        public void onError(VolleyError volleyError) {

                            if ((volleyError instanceof NoConnectionError) || (volleyError instanceof TimeoutError)) {
                                showSnackBar(getApplicationContext(), "LoginWS", getResources().getString(R.string.No_network), getString(R.string.Retry), 0, Snackbar.LENGTH_INDEFINITE);
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


                           /* if ((volleyError instanceof NoConnectionError) || (volleyError instanceof TimeoutError))
                            {
                                showSnackBar(getApplicationContext(), "LoginWS", getResources().getString(R.string.No_network), getString(R.string.Retry), 0, Snackbar.LENGTH_INDEFINITE);

                            }
                            else if ((volleyError instanceof ServerError) || (volleyError instanceof AuthFailureError))
                            {
                                showSnackBar(getApplicationContext(), "", getResources().getString(R.string.Unknown_error), "", 0, Snackbar.LENGTH_SHORT);
                            }*/

                        }
                    },
                    AllLinsks.login + URLEncoder.encode(StartActivity.this.et_mobile_login.getText().toString(), "UTF-8"),
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
                Log.e(TAG, "link is " + AllLinsks.verify + URLEncoder.encode(StartActivity.this.et_mobile_register.getText().toString(), "UTF-8") + "/" + data);
                OTPLink = AllLinsks.verify + URLEncoder.encode(StartActivity.this.et_mobile_register.getText().toString(), "UTF-8") + "/" + data;
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }

        } else if (loginscreen) {
            try {
                Log.e(TAG, "link is " + AllLinsks.authenticate + URLEncoder.encode(StartActivity.this.et_mobile_login.getText().toString(), "UTF-8") + "/" + data);
                OTPLink = AllLinsks.authenticate + URLEncoder.encode(StartActivity.this.et_mobile_login.getText().toString(), "UTF-8") + "/" + data;
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
                                    SettingPreffrence.setuser_mobilenumber(StartActivity.this, et_mobile_register.getText().toString());
                                    SettingPreffrence.setisLoggedIn(StartActivity.this, true);
                                    SettingPreffrence.setuser_phonetoken(StartActivity.this, et_mobile_register.getText().toString() + "/" + token_code);
                                    SettingPreffrence.setuser_name(StartActivity.this, et_userName.getText().toString());

                                    Log.e(TAG, "getPREF_Phone_Token is " + SettingPreffrence.getPREF_Phone_Token(StartActivity.this));

                                    Intent intent = new Intent(StartActivity.this, HomeScreen.class);
                                    startActivity(intent);
                                    finish();

                                } else if (loginscreen) {

                                    SettingPreffrence.setuser_token(StartActivity.this, token_code);
                                    SettingPreffrence.setuser_mobilenumber(StartActivity.this, et_mobile_login.getText().toString());
                                    SettingPreffrence.setisLoggedIn(StartActivity.this, true);
                                    SettingPreffrence.setuser_phonetoken(StartActivity.this, et_mobile_login.getText().toString() + "/" + token_code);
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

                                // et_otp.setText("");


                            }/* else if (status.equalsIgnoreCase("Failure")) {
                                Log.e(TAG, "failure");
                                Log.e(TAG, "code is " + code);
                                Log.e(TAG, "message is " + message);
                                showSnackBar(getApplicationContext(), "", message, "", 0, Snackbar.LENGTH_SHORT);


                            }
*/

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


                      /*  if ((volleyError instanceof NoConnectionError) || (volleyError instanceof TimeoutError))
                        {
                            showSnackBar(getApplicationContext(), "OtpWS", getResources().getString(R.string.No_network), getString(R.string.Retry), 0, Snackbar.LENGTH_INDEFINITE);

                        }
                        else if ((volleyError instanceof ServerError) || (volleyError instanceof AuthFailureError))
                        {
                            showSnackBar(getApplicationContext(), "", getResources().getString(R.string.Unknown_error), "", 0, Snackbar.LENGTH_SHORT);
                        }
*/
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
        //  view.setBackgroundColor(getResources().getColor(R.color.red));
      /*  TextView tv=(TextView)view.findViewById(android.support.design.R.id.snackbar_text);
        tv.setTextColor(getResources().getColor(R.color.red));*/

        snackBar.setActionTextColor(Color.RED);
        if (!textLabel.isEmpty())//show action button depending on Label
        {
            snackBar.setAction(textLabel, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (type.equals("RegisterWS"))//take action depending on type
                    {
                        RegisterWS();
                        snackBar.dismiss();
                        // getNotification();

                    } else if (type.equals("LoginWS")) {

                        LoginWS();
                        snackBar.dismiss();

                    } else if (type.equals("OtpWS")) {
                        OtpWS();
                        snackBar.dismiss();

                    }

                }
            });

        }
        Log.e(TAG, "show");
        //utilClass.showToast(getApplicationContext(),"showSnackBar");

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


}
