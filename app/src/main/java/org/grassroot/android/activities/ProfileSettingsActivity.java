package org.grassroot.android.activities;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.grassroot.android.R;
import org.grassroot.android.adapters.ProfileAdapter;
import org.grassroot.android.fragments.EditItemDialog;
import org.grassroot.android.fragments.RadioSelectDialogFragment;
import org.grassroot.android.interfaces.ClickListener;
import org.grassroot.android.models.GenericResponse;
import org.grassroot.android.models.ProfileResponse;
import org.grassroot.android.services.GrassrootRestService;
import org.grassroot.android.ui.views.RecyclerTouchListener;
import org.grassroot.android.utils.PreferenceUtils;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileSettingsActivity extends PortraitActivity implements
        EditItemDialog.OnEditItemListener, RadioSelectDialogFragment.RadioChoiceListener {

    private static final String TAG = ProfileSettingsActivity.class.getSimpleName();

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
    private String username;
    private String language;
    private String alertPreference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_settings);
        ButterKnife.bind(this);
        getProfileSettings();
    }

    private void getProfileSettings() {

        final String phoneNumber = PreferenceUtils.getuser_mobilenumber(this);
        final String code = PreferenceUtils.getuser_token(this);
        GrassrootRestService.getInstance().getApi().getUserProfile(phoneNumber,code).enqueue(new Callback<ProfileResponse>() {
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
                Toast.makeText(ProfileSettingsActivity.this, t.getMessage(), Toast.LENGTH_LONG);
            }
        });

    }

    private void updateProfileSetting() {

        final ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage(getString(R.string.wait_message));
        final String phoneNumber = PreferenceUtils.getuser_mobilenumber(this);
        final String code = PreferenceUtils.getuser_token(this);
        progressDialog.show();

        GrassrootRestService.getInstance().getApi().updateProfile(phoneNumber, code, username, language, alertPreference).enqueue(new Callback<GenericResponse>() {
            @Override
            public void onResponse(Call<GenericResponse> call, Response<GenericResponse> response) {
                progressDialog.dismiss();
                if (response.isSuccessful()) {
                    PreferenceUtils.setuser_name(ProfileSettingsActivity.this, username);
                    PreferenceUtils.setPrefLanguage(ProfileSettingsActivity.this, language);
                    PreferenceUtils.setPrefAlert(ProfileSettingsActivity.this, alertPreference);
                    Toast.makeText(ProfileSettingsActivity.this, R.string.profile_updated, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<GenericResponse> call, Throwable t) {
                progressDialog.dismiss();
                Toast.makeText(ProfileSettingsActivity.this, t.getMessage(), Toast.LENGTH_LONG);
            }
        });


    }

    private void setAllViews() {
        txtPpUsername.setText(username);
        txtPpNumber.setText(PreferenceUtils.getuser_mobilenumber(ProfileSettingsActivity.this));
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
                        EditItemDialog.newInstance(PreferenceUtils.getuser_name(ProfileSettingsActivity.this)).show(getFragmentManager(), EditItemDialog.TAG);
                        break;

                    case 1://language : todo : add "confirm" button
                        RadioSelectDialogFragment.newInstance(R.string.pp_language_dialog_title, R.array.language, 0, ProfileSettingsActivity.this)
                                .show(getSupportFragmentManager(), "language");
                        break;

                    case 2://notifications
                        RadioSelectDialogFragment.newInstance(R.string.pp_notifications_dialog_title, R.array.Notifications, 0, ProfileSettingsActivity.this)
                                .show(getSupportFragmentManager(), "notifications");
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

    @Override
    public void onTitleModified(String newTitle) {
        txtPpUsername.setText(newTitle);
        username = newTitle;

    }

    @Override
    public void radioButtonPicked(int position, String identifier) {
        language = getResources().getStringArray(R.array.languagekey)[position]; // uh : actually set this
    }
}
