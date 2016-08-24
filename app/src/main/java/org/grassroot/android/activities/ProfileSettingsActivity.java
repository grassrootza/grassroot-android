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
import org.grassroot.android.adapters.RecyclerTouchListener;
import org.grassroot.android.fragments.dialogs.EditTextDialogFragment;
import org.grassroot.android.fragments.dialogs.RadioSelectDialogFragment;
import org.grassroot.android.interfaces.ClickListener;
import org.grassroot.android.models.GenericResponse;
import org.grassroot.android.models.PreferenceObject;
import org.grassroot.android.models.ProfileResponse;
import org.grassroot.android.models.ShareModel;
import org.grassroot.android.services.GrassrootRestService;
import org.grassroot.android.utils.RealmUtils;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.realm.RealmList;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileSettingsActivity extends PortraitActivity
    implements EditTextDialogFragment.EditTextDialogListener,
    RadioSelectDialogFragment.RadioChoiceListener {

  private static final String TAG = ProfileSettingsActivity.class.getSimpleName();

  @BindView(R.id.rl_root) RelativeLayout rlRoot;
  @BindView(R.id.pp_toolbar) LinearLayout ppToolbar;
  @BindView(R.id.iv_pp_profile) ImageView ivPpProfile;
  @BindView(R.id.txt_pp_username) TextView txtPpUsername;
  @BindView(R.id.txt_pp_number) TextView txtPpNumber;
  @BindView(R.id.rc_pp) RecyclerView mRecyclerView;
  @BindView(R.id.bt_pp_update) Button btnupdate;
  @BindView(R.id.iv_pp_back) ImageView ivPpBack;

  private ProfileAdapter pAdapter;
  private String username;
  private String language;
  private String alertPreference;

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_profile_settings);
    ButterKnife.bind(this);
    getProfileSettings();
  }

  private void getProfileSettings() {

    final String phoneNumber = RealmUtils.loadPreferencesFromDB().getMobileNumber();
    final String code = RealmUtils.loadPreferencesFromDB().getToken();
    GrassrootRestService.getInstance()
        .getApi()
        .getUserProfile(phoneNumber, code)
        .enqueue(new Callback<ProfileResponse>() {
          @Override
          public void onResponse(Call<ProfileResponse> call, Response<ProfileResponse> response) {
            if (response.isSuccessful()) {
              username = response.body().getProfile().getDisplay_name();
              language = response.body().getProfile().getLanguage();
              alertPreference = response.body().getProfile().getAlertPreference();
              setAllViews();
            }
          }

          @Override public void onFailure(Call<ProfileResponse> call, Throwable t) {
            Toast.makeText(ProfileSettingsActivity.this, t.getMessage(), Toast.LENGTH_LONG);
          }
        });
  }

  private void updateProfileSetting() {

    final ProgressDialog progressDialog = new ProgressDialog(this);
    progressDialog.setMessage(getString(R.string.wait_message));
    final String phoneNumber = RealmUtils.loadPreferencesFromDB().getMobileNumber();
    final String code = RealmUtils.loadPreferencesFromDB().getToken();
    progressDialog.show();

    GrassrootRestService.getInstance()
        .getApi()
        .updateProfile(phoneNumber, code, username, language, alertPreference)
        .enqueue(new Callback<GenericResponse>() {
          @Override
          public void onResponse(Call<GenericResponse> call, Response<GenericResponse> response) {
            progressDialog.dismiss();
            if (response.isSuccessful()) {
              PreferenceObject object = RealmUtils.loadPreferencesFromDB();
              object.setUserName(username);
              object.setLanguagePreference(language);
              object.setAlert(alertPreference);
              Toast.makeText(ProfileSettingsActivity.this, R.string.profile_updated,
                  Toast.LENGTH_SHORT).show();
            }
          }

          @Override public void onFailure(Call<GenericResponse> call, Throwable t) {
            progressDialog.dismiss();
            Toast.makeText(ProfileSettingsActivity.this, t.getMessage(), Toast.LENGTH_LONG);
          }
        });
  }

  private void setAllViews() {
    txtPpUsername.setText(username);
    txtPpNumber.setText(RealmUtils.loadPreferencesFromDB().getMobileNumber());
    mRecyclerView();
    btnupdate.setVisibility(View.VISIBLE);
  }

  @OnClick(R.id.iv_pp_back) public void onBack() {
    onBackPressed();
  }

  @Override public void onBackPressed() {
    super.onBackPressed();
    finish();
  }

  @OnClick(R.id.bt_pp_update) public void onUpdateButton() {
    updateProfileSetting();
  }

  private void mRecyclerView() {

    this.mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
    pAdapter = new ProfileAdapter();
    this.mRecyclerView.setAdapter(this.pAdapter);
    mRecyclerView.setHasFixedSize(true);

    mRecyclerView.addOnItemTouchListener(
        new RecyclerTouchListener(getApplicationContext(), mRecyclerView, new ClickListener() {
          @Override public void onClick(View view, int position) {

            switch (position) {
              case 0://UpdateName
                final String currentName = RealmUtils.loadPreferencesFromDB().getUserName();
                EditTextDialogFragment.newInstance(R.string.pp_name_dialog, currentName,
                    ProfileSettingsActivity.this).show(getSupportFragmentManager(), "displayname");
                break;

              case 1://language : todo : add "confirm" button
                RadioSelectDialogFragment.newInstance(R.string.pp_language_dialog_title,
                    R.array.language, null,0, ProfileSettingsActivity.this)
                    .show(getSupportFragmentManager(), "language");
                break;

              case 2://notifications
                RadioSelectDialogFragment.newInstance(R.string.pp_notifications_dialog_title,
                    R.array.Notifications, null,0, ProfileSettingsActivity.this)
                    .show(getSupportFragmentManager(), "notifications");
                break;

              case 3://Settings
                PreferenceObject object = RealmUtils.loadPreferencesFromDB();
                RealmList<ShareModel> arr = object.getAppsToShare();
                List<String> apps = new ArrayList<>();
                for(int i = 0; i < arr.size(); i++) apps.add(arr.get(i).getAppName());
                String[] array = apps.toArray(new String[apps.size()]);
                RadioSelectDialogFragment.newInstance(R.string.default_share,
                        R.array.SharingApps, array,0, ProfileSettingsActivity.this)
                        .show(getSupportFragmentManager(), "share");
                break;
            }
          }

          @Override public void onLongClick(View view, int position) {

          }
        }));
  }

  @Override public void radioButtonPicked(int position, String identifier) {
    switch (identifier){
      case "share":
        PreferenceObject preferenceObject = RealmUtils.loadPreferencesFromDB();
        RealmList<ShareModel> arr = preferenceObject.getAppsToShare();
        preferenceObject.setDefaultSharePackage(arr.get(position).getPackageName());
        preferenceObject.setHasSelectedDefaultPackage(true);
        RealmUtils.saveDataToRealmSync(preferenceObject);
        break;
      case  "language":
        language =
                getResources().getStringArray(R.array.languagekey)[position]; // uh : actually set this
        break;
    }
  }

  @Override public void confirmClicked(String textEntered) {
    txtPpUsername.setText(textEntered);
    username = textEntered;
  }
}
