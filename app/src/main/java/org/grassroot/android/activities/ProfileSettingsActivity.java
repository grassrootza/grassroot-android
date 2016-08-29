package org.grassroot.android.activities;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import org.grassroot.android.R;
import org.grassroot.android.adapters.ProfileAdapter;
import org.grassroot.android.adapters.RecyclerTouchListener;
import org.grassroot.android.fragments.dialogs.EditTextDialogFragment;
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

public class ProfileSettingsActivity extends PortraitActivity  {

  private static final String TAG = ProfileSettingsActivity.class.getSimpleName();

  @BindView(R.id.profile_collapsing) CollapsingToolbarLayout collapsingToolbar;
  @BindView(R.id.profile_toolbar) Toolbar toolbar;
  @BindView(R.id.rc_pp) RecyclerView updateCardsView;
  @BindView(R.id.bt_pp_update) Button update;
  @BindView(R.id.progressBar) ProgressBar progressBar;

  private String username;
  private String language;
  private String alertPreference;

  private final int[] cardTitles = { R.string.pp_txt_Updatename, R.string.pp_txt_language,
      R.string.pp_txt_notifications, R.string.default_share };
  private final int[] cardIcons = { R.drawable.ic_update_name, R.drawable.ic_language,
      R.drawable.ic_configure, R.drawable.ic_share };

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_profile_settings);
    ButterKnife.bind(this);

    collapsingToolbar.setTitle(RealmUtils.loadPreferencesFromDB().getUserName());

    setSupportActionBar(toolbar);
    if (getSupportActionBar() != null) {
      getSupportActionBar().setHomeButtonEnabled(true);
      getSupportActionBar().setDisplayHomeAsUpEnabled(true);
      getSupportActionBar().setHomeAsUpIndicator(R.drawable.btn_close_white);
    }

    getProfileSettings();
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    if (item.getItemId() == android.R.id.home) {
      if (!RealmUtils.loadPreferencesFromDB().isHasGroups()) {
        startActivity(new Intent(this, NoGroupWelcomeActivity.class));
      } else {
        NavUtils.navigateUpFromSameTask(this);
      }
      return true;
    } else {
      return super.onOptionsItemSelected(item);
    }
  }

  private void getProfileSettings() {

    final String phoneNumber = RealmUtils.loadPreferencesFromDB().getMobileNumber();
    final String code = RealmUtils.loadPreferencesFromDB().getToken();

    progressBar.setVisibility(View.VISIBLE);
    GrassrootRestService.getInstance()
        .getApi()
        .getUserProfile(phoneNumber, code)
        .enqueue(new Callback<ProfileResponse>() {
          @Override
          public void onResponse(Call<ProfileResponse> call, Response<ProfileResponse> response) {
            progressBar.setVisibility(View.GONE);
            if (response.isSuccessful()) {
              username = response.body().getProfile().getDisplayName();
              language = response.body().getProfile().getLanguage();
              alertPreference = response.body().getProfile().getAlertPreference();
              update.setVisibility(View.VISIBLE);
              setUpRecyclerView();
            }
          }

          @Override public void onFailure(Call<ProfileResponse> call, Throwable t) {
            progressBar.setVisibility(View.GONE);
            Toast.makeText(ProfileSettingsActivity.this, t.getMessage(), Toast.LENGTH_LONG).show();
          }
        });
  }

  private void setUpRecyclerView() {

    updateCardsView.setAdapter(new ProfileAdapter(cardTitles, cardIcons));
    updateCardsView.setLayoutManager(new LinearLayoutManager(this));
    updateCardsView.setHasFixedSize(true);

    updateCardsView.addOnItemTouchListener(
        new RecyclerTouchListener(getApplicationContext(), updateCardsView, new ClickListener() {
          @Override public void onClick(View view, int position) {
            switch (position) {
              case 0:
                changeName();
                break;

              case 1:// todo : add "confirm" button
                changeLanguage();
                break;

              case 2:
                changeNotifyPriority();
                break;

              case 3:
                changeSharingApp();
                break;
            }
          }

          @Override public void onLongClick(View view, int position) { }
        }));
  }

  private void changeName() {
    final String currentName = RealmUtils.loadPreferencesFromDB().getUserName();
    EditTextDialogFragment.newInstance(R.string.pp_name_dialog,
        currentName, new EditTextDialogFragment.EditTextDialogListener() {
          @Override
          public void confirmClicked(String textEntered) {
            username = textEntered;
          }
        }).show(getSupportFragmentManager(), "displayname");
  }

  private void changeLanguage() {
    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    builder.setTitle(R.string.pp_language_dialog_title)
        .setSingleChoiceItems(R.array.language, 0, new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
            language = getResources().getStringArray(R.array.languagekey)[which]; // uh : actually set this
          }
        })
        .setPositiveButton(R.string.okay_button, new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
            updateProfileSetting();
          }
        });

    builder.setCancelable(true)
        .create()
        .show();
  }

  private void changeNotifyPriority() {
    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    builder.setTitle(R.string.pp_notifications_dialog_title)
        .setSingleChoiceItems(R.array.Notifications, 0, new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
            // do something with this
          }
        })
        .setPositiveButton(R.string.okay_button, new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
            updateProfileSetting();
          }
        });

    builder.setCancelable(true)
        .create()
        .show();
  }

  private void changeSharingApp() {
    PreferenceObject object = RealmUtils.loadPreferencesFromDB();
    RealmList<ShareModel> arr = object.getAppsToShare();
    List<String> apps = new ArrayList<>();
    for(int i = 0; i < arr.size(); i++) apps.add(arr.get(i).getAppName());
    String[] array = apps.toArray(new String[apps.size()]);

    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    builder.setTitle(R.string.default_share)
        .setSingleChoiceItems(R.array.SharingApps, 0, new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
            PreferenceObject preferenceObject = RealmUtils.loadPreferencesFromDB();
            RealmList<ShareModel> arr = preferenceObject.getAppsToShare();
            preferenceObject.setDefaultSharePackage(arr.get(which).getPackageName());
            preferenceObject.setHasSelectedDefaultPackage(true);
            RealmUtils.saveDataToRealmSync(preferenceObject);
          }
        });

    builder
        .setCancelable(true)
        .create()
        .show();
  }

  @OnClick(R.id.bt_pp_update)
  public void updateProfileSetting() {

    final String phoneNumber = RealmUtils.loadPreferencesFromDB().getMobileNumber();
    final String code = RealmUtils.loadPreferencesFromDB().getToken();

    progressBar.setVisibility(View.VISIBLE);
    GrassrootRestService.getInstance()
        .getApi()
        .updateProfile(phoneNumber, code, username, language, alertPreference)
        .enqueue(new Callback<GenericResponse>() {
          @Override
          public void onResponse(Call<GenericResponse> call, Response<GenericResponse> response) {
            progressBar.setVisibility(View.GONE);
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
            progressBar.setVisibility(View.GONE);
            Toast.makeText(ProfileSettingsActivity.this, t.getMessage(), Toast.LENGTH_LONG).show();
          }
        });
  }

}
