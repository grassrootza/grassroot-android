package org.grassroot.android.services;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import org.grassroot.android.interfaces.NotificationConstants;
import org.grassroot.android.models.GenericResponse;
import org.grassroot.android.utils.Constant;
import org.grassroot.android.utils.RealmUtils;

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
        String uid = intent.getStringExtra(NotificationConstants.NOTIFICATION_UID);
        update(uid);
    }

    private void update(final String notificationUid) {
      String phoneNumber = RealmUtils.loadPreferencesFromDB().getMobileNumber();
      String userToken = RealmUtils.loadPreferencesFromDB().getToken();

        GrassrootRestService.getInstance().getApi().updateRead(phoneNumber, userToken, notificationUid).
                enqueue(new Callback<GenericResponse>() {
                    @Override
                    public void onResponse(Call<GenericResponse> call, Response<GenericResponse> response) {
                        if (response.isSuccessful()) {
                            Log.d(TAG, "updated notification with uid " + notificationUid + "to read status");
                        }

                    }

                    @Override
                    public void onFailure(Call<GenericResponse> call, Throwable t) {
                        Log.e(TAG, "network failure");
                    }
                });
    }

    public static void updateNotificationStatus(Context context, String notificationUid){
        Intent intent = new Intent(context,NotificationUpdateService.class);
        intent.putExtra(NotificationConstants.NOTIFICATION_UID, notificationUid);
        context.startService(intent);

    }
}



