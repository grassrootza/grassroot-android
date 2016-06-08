package org.grassroot.android.services;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;

import org.grassroot.android.R;
import org.grassroot.android.models.GenericResponse;
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
    protected void onHandleIntent(Intent intent)  {

        Log.e(TAG, "registering with project ID: " + getString(R.string.project_id));

        try {


            String token = InstanceID.getInstance(this).getToken(getString(R.string.project_id),
                    GoogleCloudMessaging.INSTANCE_ID_SCOPE, null);
            String phoneNumber = intent.getStringExtra("phoneNumber");
            String code = PreferenceUtils.getuser_token(this);
            Log.d(TAG, "Registering user for push messages");
            final String messageId = generateMessageId(phoneNumber);
            final Bundle data = new Bundle();
            data.putString("phoneNumber", phoneNumber);
            data.putString("action", "REGISTER");

            GrassrootRestService.getInstance().getApi()
                    .pushRegistration(phoneNumber,code, token)
                    .enqueue(new Callback<GenericResponse>() {
                        @Override
                        public void onResponse(Call<GenericResponse> call, Response<GenericResponse> response) {
                            if (response.isSuccessful()) {
                                PreferenceUtils.setIsGcmEnabled(GcmRegistrationService.this, true);
                            } else {
                                try {
                                    GoogleCloudMessaging.getInstance(GcmRegistrationService.this).send(getString(R.string.sender_id),messageId,data);
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
            Log.e(TAG, "Push registration failed, for project ID: " + R.string.project_id + ", and scope: "
                + GoogleCloudMessaging.INSTANCE_ID_SCOPE);
            e.printStackTrace();
        }

    }

    private String generateMessageId(String phoneNumber){
        return phoneNumber +"_".concat(String.valueOf(Calendar.getInstance().getTimeInMillis()));


    }
}
