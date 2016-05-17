package com.techmorphosis.grassroot.ui.activities;

import android.graphics.Color;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;


import com.techmorphosis.grassroot.R;
import com.techmorphosis.grassroot.adapters.ProfileAdapter;
import com.techmorphosis.grassroot.interfaces.ClickListener;
import com.techmorphosis.grassroot.services.GrassrootRestService;
import com.techmorphosis.grassroot.services.model.EventModel;
import com.techmorphosis.grassroot.services.model.GenericResponse;
import com.techmorphosis.grassroot.services.model.ProfileResponse;
import com.techmorphosis.grassroot.ui.DialogFragment.Profile.EditItemDialog;
import com.techmorphosis.grassroot.ui.DialogFragment.Profile.EditNameDialogFragment;
import com.techmorphosis.grassroot.ui.views.RecyclerTouchListener;
import com.techmorphosis.grassroot.utils.SettingPreference;


import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileSettings extends PortraitActivity implements EditItemDialog.OnEditItemListener, EditNameDialogFragment.OnEditlanguageListener, EditNameDialogFragment.OnEditNotificationsListener {

    private RelativeLayout rlRoot;
    private LinearLayout ppToolbar;
    private ImageView icEdit;
    private ImageView ivPpProfile;
    private TextView txtPpUsername;
    private TextView txtPpNumber;
    private RecyclerView mRecyclerView;
    private static final String TAG = "ProfileSettings";
    private ProfileAdapter pAdapter;
    private ArrayList<String> titlelist;
    private ImageView ivPpBack;
    public Snackbar snackbar;
    private String username;
    private String language;
    private String alertPreference;
    private String selected_language;
    private String selected_notifications;
    private Button btnupdate;
    private GrassrootRestService grassrootRestService;
    private String phoneNumber;
    private String code;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_settings);
        grassrootRestService = new GrassrootRestService(this);
        findAllViews();
     //   Log.e(TAG, "SettingPreffrence.getuser_name(getActivity()) is " + SettingPreffrence.getuser_name(ProfileSettings.this));
        init();
        ProfileSettingWS();

    }

    private void ProfileSettingWS() {


        final String phoneNumber = SettingPreference.getuser_mobilenumber(this);
        String code = SettingPreference.getuser_token(this);
        grassrootRestService.getApi().getUserProfile(phoneNumber,code).enqueue(new Callback<ProfileResponse>() {
            @Override
            public void onResponse(Call<ProfileResponse> call, Response<ProfileResponse> response) {
                if (response.isSuccessful()) {
                    username = response.body().getProfile().getDisplay_name();
                    language = response.body().getProfile().getLanguage();
                    alertPreference = response.body().getProfile().getAlertPreference();
                    setAllViews();
                }

            }
            @Override
            public void onFailure(Call<ProfileResponse> call, Throwable t) {

                //  ErrorUtils.handleNetworkError(ViewVote.this, errorLayout, t);
            }
        });

        String prgMessage = "Please Wait";
        //doInBackground
   /*     NetworkCall networkCall = new NetworkCall
                (
                        ProfileSettings.this,
                        new ResponseListenerVolley() {
                            @Override
                            public void onSuccess(String s) {

                                try {
                                    JSONObject jsonObject = new JSONObject(s);
                                    if (jsonObject.getString("status").equalsIgnoreCase("SUCCESS")) {
                                        JSONObject dataObject = jsonObject.getJSONObject("data");
                                        username = dataObject.getString("displayName");
                                        language = dataObject.getString("language");
                                        alertPreference = dataObject.getString("alertPreference");
                                        setAllViews();

                                    }

                                } catch (JSONException e) {
                                    e.printStackTrace();
                                    showSnackbar(getString(R.string.Unknown_error), snackbar.LENGTH_SHORT, "", "");
                                }

                            }
                        },
                        new ErrorListenerVolley() {
                            @Override
                            public void onError(VolleyError volleyError) {
                                Log.e(TAG, "ErrorListenerVolley inside " + volleyError);

                                if (volleyError instanceof NoConnectionError || volleyError instanceof TimeoutError) {
                                    showSnackbar(getString(R.string.No_network), snackbar.LENGTH_INDEFINITE, getString(R.string.Retry), "Profile");
                                } else if (volleyError instanceof ServerError) {
                                    try {
                                        String response = new String(volleyError.networkResponse.data, "utf-8");
                                        Log.e(TAG, "response is " + response);
                                    } catch (UnsupportedEncodingException e) {
                                        e.printStackTrace();
                                    }
                                    showSnackbar(getString(R.string.Unknown_error), snackbar.LENGTH_SHORT, "", "");
                                } else if (volleyError instanceof AuthFailureError) {
                                    showSnackbar(getString(R.string.INVALID_TOKEN), snackbar.LENGTH_INDEFINITE, "", "");
                                } else {
                                    showSnackbar(getString(R.string.Unknown_error), snackbar.LENGTH_SHORT, "", "");

                                }
                            }
                        },
                        AllLinsks.ProfileSetting + SettingPreffrence.getPREF_Phone_Token(ProfileSettings.this),
                        prgMessage,
                        true
                );
        networkCall.makeStringRequest_GET();*/
    }

    private void UpdateProfileSettingWS() {

        String prgMessage = "Please Wait";

        grassrootRestService.getApi().updateProfile(phoneNumber,code, username, language,alertPreference).enqueue(new Callback<GenericResponse>() {
            @Override
            public void onResponse(Call<GenericResponse> call, Response<GenericResponse> response) {
                if (response.isSuccessful()) {
                    SettingPreference.setuser_name(ProfileSettings.this, username);
                    SettingPreference.setPrefLanguage(ProfileSettings.this, language);
                    SettingPreference.setPrefAlert(ProfileSettings.this, alertPreference);
                }

            }
            @Override
            public void onFailure(Call<GenericResponse> call, Throwable t) {

                //  ErrorUtils.handleNetworkError(ViewVote.this, errorLayout, t);
            }
        });


   /*     //doInBackground
        NetworkCall networkCall = new NetworkCall
                (
                        ProfileSettings.this,
                        new ResponseListenerVolley() {
                            @Override
                            public void onSuccess(String s) {

                                try {
                                    JSONObject jsonObject = new JSONObject(s);
                                    if (jsonObject.getString("status").equalsIgnoreCase("SUCCESS")) {
                                        SettingPreffrence.setuser_name(ProfileSettings.this, username);
                                        SettingPreffrence.setPREF_Language(ProfileSettings.this, language);
                                        SettingPreffrence.setPREF_alertPreference(ProfileSettings.this, alertPreference);
                                        SettingPreffrence.setPREF_HAS_Update(ProfileSettings.this, true);
                                        finish();
                                    }
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                    showSnackbar(getString(R.string.Unknown_error), snackbar.LENGTH_SHORT, "", "");
                                }

                            }
                        },
                        new ErrorListenerVolley() {
                            @Override
                            public void onError(VolleyError volleyError) {
                                if (volleyError instanceof NoConnectionError || volleyError instanceof TimeoutError) {
                                    showSnackbar(getString(R.string.No_network), snackbar.LENGTH_INDEFINITE, getString(R.string.Retry), "Profile");
                                } else if (volleyError instanceof ServerError) {
                                    try {
                                        String response = new String(volleyError.networkResponse.data, "utf-8");
                                        Log.e(TAG, "response is " + response);
                                    } catch (UnsupportedEncodingException e) {
                                        e.printStackTrace();
                                    }
                                    showSnackbar(getString(R.string.Unknown_error), snackbar.LENGTH_SHORT, "", "");
                                } else if (volleyError instanceof AuthFailureError) {
                                    showSnackbar(getString(R.string.INVALID_TOKEN), snackbar.LENGTH_INDEFINITE, "", "");
                                } else {
                                    showSnackbar(getString(R.string.Unknown_error), snackbar.LENGTH_SHORT, "", "");
                                }

                            }
                        },
                        AllLinsks.UpdateProfileSetting + SettingPreffrence.getPREF_Phone_Token(ProfileSettings.this),
                        prgMessage,
                        true
                );
        HashMap<String, String> hashMap = new HashMap<>();
        hashMap.put("displayName", username);
        hashMap.put("language", language);
        hashMap.put("alertPreference", alertPreference);
        networkCall.makeStringRequest_POST(hashMap);*/
    }


    private void setAllViews() {
        txtPpUsername.setText(username);
        txtPpNumber.setText(SettingPreference.getuser_mobilenumber(ProfileSettings.this));

        mRecyclerView();
        btnupdate.setVisibility(View.VISIBLE);
    }

    private void init() {

    }

    private void findAllViews() {
        rlRoot = (RelativeLayout) findViewById(R.id.rl_root);
        ppToolbar = (LinearLayout) findViewById(R.id.pp_toolbar);
        ivPpBack = (ImageView) findViewById(R.id.iv_pp_back);
        ivPpProfile = (ImageView) findViewById(R.id.iv_pp_profile);
        txtPpUsername = (TextView) findViewById(R.id.txt_pp_username);
        txtPpNumber = (TextView) findViewById(R.id.txt_pp_number);
        mRecyclerView = (RecyclerView) findViewById(R.id.rc_pp);
        btnupdate = (Button) findViewById(R.id.bt_pp_update);
        btnupdate.setOnClickListener(button_update());
        ivPpBack.setOnClickListener(onBack());
    }

    private View.OnClickListener onBack() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        };
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();

    }

    private View.OnClickListener button_update() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
             Log.d(TAG, "Updating profile");
                UpdateProfileSettingWS();
            }
        };
    }

    private void mRecyclerView() {
        this.mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        pAdapter = new ProfileAdapter();
        this.mRecyclerView.setAdapter(this.pAdapter);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.addOnItemTouchListener(new RecyclerTouchListener(getApplicationContext(), mRecyclerView, new ClickListener() {
            @Override
            public void onClick(View view, int position) {

                switch (position) {
                    case 0://UpdateName
                        EditItemDialog.newInstance(SettingPreference.getuser_name(ProfileSettings.this)).show(getFragmentManager(), EditItemDialog.TAG);
                        break;

                    case 1://language
                        EditNameDialogFragment.newInstance("language", language).show(getFragmentManager(), EditNameDialogFragment.TAG);

                        break;

                    case 2://notifications
                        EditNameDialogFragment.newInstance("Notifications", alertPreference).show(getFragmentManager(), EditNameDialogFragment.TAG);

                        break;

                    case 3://Settings

                        break;


                }

            }

            @Override
            public void onLongClick(View view, int position) {

            }
        }));
    }


    public void showSnackbar(String message, int length, String actionbuttontxt, final String type) {
        Log.e(TAG, "showSnackbar is " + message);
        snackbar = Snackbar.make(rlRoot, message, length);

        snackbar.setActionTextColor(Color.RED);

        if (!actionbuttontxt.isEmpty()) {

            snackbar.setAction(actionbuttontxt, new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    if (type.equalsIgnoreCase("Profile")) {
                        ProfileSettingWS();
                    } else if (type.equalsIgnoreCase("Update")) {
                        UpdateProfileSettingWS();

                    }
                }
            });

        }
        snackbar.show();


    }

    @Override
    public void onLanguage(String selected_language) {
        language = selected_language;
        // Toast.makeText(ProfileSettings.this, "language is " + selected_language, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onNotifications(String selected_notifications) {

        alertPreference = selected_notifications;
        //Toast.makeText(ProfileSettings.this, "notifications is " + selected_notifications, Toast.LENGTH_SHORT).show();

    }

    @Override
    public void onTitleModified(String newTitle) {

        txtPpUsername.setText(newTitle);
        username = newTitle;

    }
}
