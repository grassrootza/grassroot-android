package org.grassroot.android.services;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;

import org.grassroot.android.BuildConfig;
import org.grassroot.android.R;
import org.grassroot.android.interfaces.NotificationConstants;
import org.grassroot.android.models.GenericResponse;
import org.grassroot.android.utils.Constant;
import org.grassroot.android.utils.PreferenceUtils;

import java.io.IOException;
import java.util.Calendar;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Created by paballo on 2016/05/09.
 */
public class GcmRegistrationService extends IntentService {

    private static final String TAG = GcmRegistrationService.class.getCanonicalName();

    //default constructor required by Android
    public GcmRegistrationService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        final String action = intent.getStringExtra(NotificationConstants.ACTION);
        if (NotificationConstants.GCM_UNREGISTER.equals(action)) {
            unRegister(intent.getStringExtra(NotificationConstants.PHONE_NUMBER),
                    intent.getStringExtra(Constant.USER_TOKEN));
        } else {
            register(intent);
        }
    }

    private String generateMessageId(String phoneNumber) {
        return phoneNumber + "_".concat(String.valueOf(Calendar.getInstance().getTimeInMillis()));
    }

    private void register(Intent intent) {

        final String senderId = BuildConfig.FLAVOR.equals(Constant.PROD) ?
                getString(R.string.prod_sender_id) : getString(R.string.staging_sender_id);
        final String projectId = BuildConfig.FLAVOR.equals(Constant.PROD) ?
                getString(R.string.prod_project_id) : getString(R.string.staging_project_id);

        try {
            Log.d(TAG, "Registering user for push messages");

            String gcmToken = InstanceID.getInstance(this)
                    .getToken(projectId, GoogleCloudMessaging.INSTANCE_ID_SCOPE, null);

            final String phoneNumber = intent.getStringExtra(NotificationConstants.PHONE_NUMBER);
            final String code = PreferenceUtils.getAuthToken(getApplicationContext());
            final String messageId = generateMessageId(phoneNumber);

            GrassrootRestService.getInstance().getApi()
                    .pushRegistration(phoneNumber, code, gcmToken)
                    .enqueue(new Callback<GenericResponse>() {
                        @Override
                        public void onResponse(Call<GenericResponse> call, Response<GenericResponse> response) {
                            if (response.isSuccessful()) {
                                PreferenceUtils.setIsGcmEnabled(getApplicationContext(), true);
                            } else {
                                try {
                                    final Bundle data = new Bundle();
                                    data.putString(NotificationConstants.PHONE_NUMBER, phoneNumber);
                                    data.putString(NotificationConstants.ACTION, NotificationConstants.REGISTER);
                                    GoogleCloudMessaging.getInstance(GcmRegistrationService.this)
                                            .send(senderId, messageId, data);
                                } catch (IOException e1) {
                                    e1.printStackTrace();
                                }
                            }
                        }

                        @Override
                        public void onFailure(Call<GenericResponse> call, Throwable t) {
                            Log.e(TAG, "Error! Got a network error in service, and can't do anything with it");
                        }
                    });
        } catch (IOException e) {
            Log.e(TAG, "Push registration failed, for project ID: " + projectId + ", and scope: "
                    + GoogleCloudMessaging.INSTANCE_ID_SCOPE);
            e.printStackTrace();
        }
    }

    // have to pass these as arguments, since pref utils may be cleared by time get here
    private void unRegister(final String phoneNumber, final String code) {
        try {
            InstanceID.getInstance(this).deleteInstanceID();
            Log.e(TAG, "unregistering user ... with phone number ... " + phoneNumber);
            GrassrootRestService.getInstance().getApi()
                    .pushUnregister(phoneNumber, code)
                    .enqueue(new Callback<GenericResponse>() {
                        @Override
                        public void onResponse(Call<GenericResponse> call, Response<GenericResponse> response) {
                            if (response.isSuccessful()) {
                                Log.e(TAG, "Gcm unregistration successful");
                                PreferenceUtils.setIsGcmEnabled(getApplicationContext(), false);
                            }
                        }

                        @Override
                        public void onFailure(Call<GenericResponse> call, Throwable t) {
                            Log.d(TAG, "this is a background call, cant show user anything.  Fail silently");
                        }
                    });
        } catch (IOException e) {
            Log.e(TAG, "Push unregistration failed, for project ID: " + getString(R.string.staging_project_id) +
                    ", and scope: " + GoogleCloudMessaging.INSTANCE_ID_SCOPE);
        }
    }


}