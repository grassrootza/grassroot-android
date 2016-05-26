package com.techmorphosis.grassroot.ui.activities;

import android.app.ProgressDialog;
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
import com.techmorphosis.grassroot.services.model.GenericResponse;
import com.techmorphosis.grassroot.services.model.ProfileResponse;
import com.techmorphosis.grassroot.ui.DialogFragment.Profile.EditItemDialog;
import com.techmorphosis.grassroot.ui.DialogFragment.Profile.EditNameDialogFragment;
import com.techmorphosis.grassroot.ui.views.RecyclerTouchListener;
import com.techmorphosis.grassroot.utils.SettingPreference;


import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileSettings extends PortraitActivity implements EditItemDialog.OnEditItemListener, EditNameDialogFragment.OnEditlanguageListener, EditNameDialogFragment.OnEditNotificationsListener {

    private static final String TAG = "ProfileSettings";

    @BindView(R.id.rl_root)
    RelativeLayout rlRoot;
    @BindView(R.id.pp_toolbar)
    LinearLayout ppToolbar;
    @BindView(R.id.iv_pp_profile)
    ImageView ivPpProfile;
    @BindView(R.id.txt_pp_username)
    TextView txtPpUsername;
    @BindView(R.id.txt_pp_number)
    TextView txtPpNumber;
    @BindView(R.id.rc_pp)
    RecyclerView mRecyclerView;
    @BindView(R.id.bt_pp_update)
    Button btnupdate;
    @BindView(R.id.iv_pp_back)
    ImageView ivPpBack;

    private ProfileAdapter pAdapter;
    private Snackbar snackbar;
    private String username;
    private String language;
    private String alertPreference;
    private GrassrootRestService grassrootRestService;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_settings);
        ButterKnife.bind(this);
        grassrootRestService = new GrassrootRestService(this);
        getProfileSettings();

    }

    private void getProfileSettings() {

        final String phoneNumber = SettingPreference.getuser_mobilenumber(this);
        final String code = SettingPreference.getuser_token(this);
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


    }

    private void updateProfileSetting() {

        final ProgressDialog progressDialog = new ProgressDialog(this);
        String prgMessage = "Please Wait";
        progressDialog.setMessage(prgMessage);
        final String phoneNumber = SettingPreference.getuser_mobilenumber(this);
        final String code = SettingPreference.getuser_token(this);
        progressDialog.show();
        grassrootRestService.getApi().updateProfile(phoneNumber,code, username, language,alertPreference).enqueue(new Callback<GenericResponse>() {
            @Override
            public void onResponse(Call<GenericResponse> call, Response<GenericResponse> response) {
                progressDialog.dismiss();
                if (response.isSuccessful()) {
                    SettingPreference.setuser_name(ProfileSettings.this, username);
                    SettingPreference.setPrefLanguage(ProfileSettings.this, language);
                    SettingPreference.setPrefAlert(ProfileSettings.this, alertPreference);
                }
            }
            @Override
            public void onFailure(Call<GenericResponse> call, Throwable t) {
                 progressDialog.dismiss();
               //  ErrorUtils.
            }
        });



    }


    private void setAllViews() {
        txtPpUsername.setText(username);
        txtPpNumber.setText(SettingPreference.getuser_mobilenumber(ProfileSettings.this));
        mRecyclerView();
        btnupdate.setVisibility(View.VISIBLE);
    }


    @OnClick(R.id.iv_pp_back)
    public void onBack() {
        onBackPressed();

    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();

    }

    @OnClick(R.id.bt_pp_update)
    public void onUpdateButton() {
        updateProfileSetting();

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
                        getProfileSettings();
                    } else if (type.equalsIgnoreCase("Update")) {
                        updateProfileSetting();

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
