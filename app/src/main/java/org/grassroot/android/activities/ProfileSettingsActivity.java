package org.grassroot.android.activities;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
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
import org.grassroot.android.services.ApplicationLoader;
import org.grassroot.android.services.GrassrootRestService;
import org.grassroot.android.utils.ErrorUtils;
import org.grassroot.android.utils.RealmUtils;

import java.util.Arrays;

import butterknife.BindView;
import butterknife.ButterKnife;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileSettingsActivity extends PortraitActivity  {

  private static final String TAG = ProfileSettingsActivity.class.getSimpleName();

  @BindView(R.id.profile_root_layout) CoordinatorLayout rootLayout;
  @BindView(R.id.profile_collapsing) CollapsingToolbarLayout collapsingToolbar;
  @BindView(R.id.profile_toolbar) Toolbar toolbar;
  @BindView(R.id.rc_pp) RecyclerView updateCardsView;
  @BindView(R.id.progressBar) ProgressBar progressBar;

  private String username;
  private String language;
  private String alertPreference;
  private int alertPreferenceIndex = -1;

  private String phoneNumber;
  private String token;

  private final int[] cardTitles = { R.string.pp_txt_Updatename, R.string.pp_txt_notifications,
      R.string.pp_txt_language };

  private final int[] cardIcons = { R.drawable.ic_update_name, R.drawable.ic_configure,
      R.drawable.ic_language };

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_profile_settings);
    ButterKnife.bind(this);

    collapsingToolbar.setTitle(RealmUtils.loadPreferencesFromDB().getUserName());
    phoneNumber = RealmUtils.loadPreferencesFromDB().getMobileNumber();
    token = RealmUtils.loadPreferencesFromDB().getToken();

    setSupportActionBar(toolbar);
    if (getSupportActionBar() != null) {
      getSupportActionBar().setHomeButtonEnabled(true);
      getSupportActionBar().setDisplayHomeAsUpEnabled(true);
      getSupportActionBar().setHomeAsUpIndicator(R.drawable.btn_close_white);
    }

    fetchSettingsFromServer();
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

  private void fetchSettingsFromServer() {
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

              case 1:
                changeNotifyPriority();
                break;

              case 2:
                changeLanguage();
                break;

            }
          }

          @Override public void onLongClick(View view, int position) { }
        }));
  }

  private void changeName() {
    final String currentName = RealmUtils.loadPreferencesFromDB().getUserName();
    EditTextDialogFragment.newInstance(R.string.pp_name_dialog_title,
        currentName, new EditTextDialogFragment.EditTextDialogListener() {
          @Override
          public void confirmClicked(String textEntered) {
            updateNameServer(textEntered.trim());
          }
        }).show(getSupportFragmentManager(), "displayname");
  }

  private void updateNameServer(final String newName) {
    progressBar.setVisibility(View.VISIBLE);
    GrassrootRestService.getInstance().getApi()
        .renameUser(phoneNumber, token, newName).enqueue(new Callback<GenericResponse>() {
      @Override
      public void onResponse(Call<GenericResponse> call, Response<GenericResponse> response) {
        if (response.isSuccessful()) {
          progressBar.setVisibility(View.GONE);
          collapsingToolbar.setTitle(newName);
          Toast.makeText(ApplicationLoader.applicationContext, "Done", Toast.LENGTH_SHORT).show();
          PreferenceObject preferenceObject = RealmUtils.loadPreferencesFromDB();
          preferenceObject.setUserName(newName);
          RealmUtils.saveDataToRealmWithSubscriber(preferenceObject);
        } else {
          Snackbar.make(rootLayout, R.string.user_rename_server_error, Snackbar.LENGTH_SHORT).show();
        }
      }

      @Override
      public void onFailure(Call<GenericResponse> call, Throwable t) {
        progressBar.setVisibility(View.GONE);
        // note : since *134*1994# makes it easy to do any of these, for the moment using snackbar instead of heavier dialog approach
        ErrorUtils.networkErrorSnackbar(rootLayout, R.string.profile_settings_connect_error,
            new View.OnClickListener() {
              @Override
              public void onClick(View v) {
                updateNameServer(newName);
              }
            });
      }
    });
  }

  private void changeNotifyPriority() {
    // reset this on each call but don't update inside single choice items listener, so it saves for cancel
    final int alertPreferenceIndex = Arrays.asList(getResources().getStringArray(R.array.notification_setting_calls)).contains(alertPreference)
        ? Arrays.asList(getResources().getStringArray(R.array.notification_setting_calls)).indexOf(alertPreference) : 1;
    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    builder.setTitle(R.string.pp_notifications_dialog_title)
        .setCancelable(true)
        .setSingleChoiceItems(R.array.notification_setting_descriptions, alertPreferenceIndex, new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
            alertPreference = getResources().getStringArray(R.array.notification_setting_calls)[which];
          }
        })
        .setPositiveButton(R.string.okay_button, new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
            updateNotifyPriorityServer(alertPreference, alertPreferenceIndex);
          }
        })
        .setNegativeButton(R.string.alert_cancel, new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
            dialog.cancel();
          }
        })
        .setOnCancelListener(new DialogInterface.OnCancelListener() {
          @Override
          public void onCancel(DialogInterface dialog) {
            alertPreference = getResources().getStringArray(R.array.notification_setting_calls)[alertPreferenceIndex];
          }
        })
        .create()
        .show();
  }

  private void updateNotifyPriorityServer(final String newPreference, final int fallbackIndex) {
    progressBar.setVisibility(View.VISIBLE);
    GrassrootRestService.getInstance().getApi().changeNotifyPriority(phoneNumber, token, newPreference)
        .enqueue(new Callback<GenericResponse>() {
          @Override
          public void onResponse(Call<GenericResponse> call, Response<GenericResponse> response) {
            progressBar.setVisibility(View.GONE);
            if (response.isSuccessful()) {
              Toast.makeText(ApplicationLoader.applicationContext, R.string.user_notify_pref_done, Toast.LENGTH_SHORT).show();
              PreferenceObject prefs = RealmUtils.loadPreferencesFromDB();
              prefs.setAlert(newPreference);
              RealmUtils.saveDataToRealmWithSubscriber(prefs);
              alertPreference = newPreference; // just to make sure, in case we have done a round trip via connect failure
            } else {
              alertPreference = getResources().getStringArray(R.array.notification_setting_calls)[fallbackIndex];
              Snackbar.make(rootLayout, ErrorUtils.serverErrorText(response.errorBody()), Snackbar.LENGTH_SHORT)
                  .show();
            }
          }

          @Override
          public void onFailure(Call<GenericResponse> call, Throwable t) {
            alertPreference = getResources().getStringArray(R.array.notification_setting_calls)[fallbackIndex];
            ErrorUtils.networkErrorSnackbar(rootLayout, R.string.profile_settings_connect_error,
                new View.OnClickListener() {
                  @Override
                  public void onClick(View v) {
                    updateNotifyPriorityServer(newPreference, fallbackIndex);
                  }
                });
          }
        });
  }

  private void changeLanguage() {
    final int currIndex = Arrays.asList(getResources().getStringArray(R.array.language_keys)).contains(language)
        ? Arrays.asList(getResources().getStringArray(R.array.language_keys)).indexOf(language) : 0;

    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    builder.setTitle(R.string.pp_language_dialog_title)
        .setCancelable(true)
        .setSingleChoiceItems(R.array.language_descriptions, currIndex, new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
            language = getResources().getStringArray(R.array.language_keys)[which];
          }
        })
        .setPositiveButton(R.string.okay_button, new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
            sendLanguageChangeToServer(language, currIndex);
          }
        })
        .setNegativeButton(R.string.alert_cancel, new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
            dialog.cancel();
          }
        })
        .setOnCancelListener(new DialogInterface.OnCancelListener() {
          @Override
          public void onCancel(DialogInterface dialog) {
            language = getResources().getStringArray(R.array.language_keys)[currIndex];
          }
        })
        .create()
        .show();
  }

  private void sendLanguageChangeToServer(final String newLanguage, final int fallbackIndex) {
    progressBar.setVisibility(View.VISIBLE);
    GrassrootRestService.getInstance().getApi().changeUserLanguage(phoneNumber, token, newLanguage)
        .enqueue(new Callback<GenericResponse>() {
          @Override
          public void onResponse(Call<GenericResponse> call, Response<GenericResponse> response) {
            progressBar.setVisibility(View.GONE);
            if (response.isSuccessful()) {
              Toast.makeText(ApplicationLoader.applicationContext, R.string.user_language_pref_done, Toast.LENGTH_SHORT).show();
              PreferenceObject prefs = RealmUtils.loadPreferencesFromDB();
              prefs.setLanguagePreference(newLanguage);
              RealmUtils.saveDataToRealmWithSubscriber(prefs);
              language = newLanguage;
            } else {
              language = getResources().getStringArray(R.array.language_keys)[fallbackIndex];
              Snackbar.make(rootLayout, ErrorUtils.serverErrorText(response.errorBody()), Snackbar.LENGTH_SHORT).show();
            }
          }

          @Override
          public void onFailure(Call<GenericResponse> call, Throwable t) {
            progressBar.setVisibility(View.VISIBLE);
            language = getResources().getStringArray(R.array.language_keys)[fallbackIndex];
            ErrorUtils.networkErrorSnackbar(rootLayout, R.string.profile_settings_connect_error,
                new View.OnClickListener() {
                  @Override
                  public void onClick(View v) {
                    sendLanguageChangeToServer(newLanguage, fallbackIndex);
                  }
                });
          }
        });
  }

}
