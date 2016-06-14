package org.grassroot.android.services;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import org.grassroot.android.models.GenericResponse;
import org.grassroot.android.utils.Constant;
import org.grassroot.android.utils.PreferenceUtils;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;



/**
 * Created by paballo on 2016/06/09.
 */
public class NotificationUpdateService extends IntentService {

    private static final String TAG = NotificationUpdateService.class.getCanonicalName();

    public NotificationUpdateService() {
        super(TAG);
    }


    @Override
    protected void onHandleIntent(Intent intent) {

        String uid = intent.getStringExtra(Constant.UID);
        update(uid);
    }

    private void update(final String uid) {

        String phoneNumber = PreferenceUtils.getuser_mobilenumber(this);
        String code = PreferenceUtils.getuser_token(this);

        GrassrootRestService.getInstance().getApi().updateRead(phoneNumber, code, uid).
                enqueue(new Callback<GenericResponse>() {
                    @Override
                    public void onResponse(Call<GenericResponse> call, Response<GenericResponse> response) {
                        if (response.isSuccessful()) {
                            Log.e(TAG, "updated notification with uid " + uid + "to read status");
                        }

                    }

                    @Override
                    public void onFailure(Call<GenericResponse> call, Throwable t) {
                        Log.e(TAG, "network failure");
                    }
                });
    }

    public static void updateNotificationStatus(Context context, String uid){
        Log.e(TAG, "starting notification update service");
        Intent intent = new Intent(context,NotificationUpdateService.class);
        intent.putExtra(Constant.UID, uid);
        context.startService(intent);

    }
}



