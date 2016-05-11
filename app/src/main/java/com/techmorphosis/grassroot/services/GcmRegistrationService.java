package com.techmorphosis.grassroot.services;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;
import com.techmorphosis.grassroot.R;
import com.techmorphosis.grassroot.services.model.GenericResponse;
import com.techmorphosis.grassroot.services.model.TaskResponse;
import com.techmorphosis.grassroot.utils.SettingPreference;

import java.io.IOException;
import java.util.Calendar;

import rx.Observer;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Created by paballo on 2016/05/09.
 */
public class GcmRegistrationService extends IntentService {

    private static final String TAG = GcmRegistrationService.class.getCanonicalName();
    private GrassrootRestService grassrootRestService = new GrassrootRestService();

    //default constructor required by Android
    public GcmRegistrationService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent)  {

        try {
            String token = InstanceID.getInstance(this).getToken(getString(R.string.project_id),
                    GoogleCloudMessaging.INSTANCE_ID_SCOPE, null);
            String phoneNumber = intent.getStringExtra("phoneNumber");
            String code = SettingPreference.getuser_token(this);
            Log.d(TAG, "Registering user for push messages");
            final String messageId = generateMessageId(phoneNumber);
            final Bundle data = new Bundle();
            data.putString("phoneNumber", phoneNumber);
            data.putString("action", "REGISTER");
            grassrootRestService.getApi().pushRegistration(phoneNumber,code, token ).subscribeOn(Schedulers.newThread())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Observer<GenericResponse>() {
                        @Override
                        public void onCompleted() {
                            SettingPreference.setIsGcmEnabled(GcmRegistrationService.this, true);
                        }
                        @Override
                        public void onError(Throwable e) {
                          Log.d(TAG, e.getMessage());
                          //Falling back to gcm upstream even though its more unreliable
                            try {
                                GoogleCloudMessaging.getInstance(GcmRegistrationService.this).send(getString(R.string.sender_id),messageId,data);
                            } catch (IOException e1) {
                                e1.printStackTrace();
                            }
                        }
                        @Override
                        public void onNext(GenericResponse response) {

                        }
                    });


        } catch (IOException e) {
            Log.e(TAG, "Push registration failed");
            e.printStackTrace();
        }

    }

    private String generateMessageId(String phoneNumber){
        return phoneNumber +"_".concat(String.valueOf(Calendar.getInstance().getTimeInMillis()));


    }
}
