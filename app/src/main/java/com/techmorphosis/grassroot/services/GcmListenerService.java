package com.techmorphosis.grassroot.services;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;


import com.techmorphosis.grassroot.R;
import com.techmorphosis.grassroot.ui.activities.NotBuiltActivity;

/**
 * Created by paballo on 2016/05/09.
 */
public class GcmListenerService extends com.google.android.gms.gcm.GcmListenerService {

    private static final String TAG = GcmListenerService.class.getCanonicalName();
    private static final String STACK_KEY = "stack_key";


    @Override
    public void onMessageReceived(String from, Bundle data) {
        sendNotification(data);

    }

    @Override
    public void onMessageSent(String msgId) {
        super.onMessageSent(msgId);
        Log.d(TAG, "message with id " + msgId + " sent.");

    }

    @Override
    public void onSendError(String msgId, String error) {
        super.onSendError(msgId, error);
        Log.d(TAG, error);
    }

    private void sendNotification(Bundle msg) {


        Log.d(TAG, "Received a push notification from server");
        Intent resultIntent = new Intent(this, NotBuiltActivity.class);
        resultIntent.putExtra("title",String.valueOf(msg.get( "entity_type")));
        PendingIntent resultPendingIntent = PendingIntent.getActivity(this, 0,
                resultIntent, PendingIntent.FLAG_ONE_SHOT);

        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.Builder mNotifyBuilder = new NotificationCompat.Builder(this)
                .setContentTitle(msg.getString("title"))
                .setContentText(msg.getString("body"))
                .setSmallIcon(R.drawable.app_icon)
                .setContentIntent(resultPendingIntent)
                .setGroup(STACK_KEY);
        Log.d(TAG, msg.getString("title"));

        Log.d(TAG, msg.getString("id"));
        Log.d(TAG, msg.toString());


        int defaults = 0;
        defaults = defaults | Notification.DEFAULT_LIGHTS;
        defaults = defaults | Notification.DEFAULT_SOUND;

        mNotifyBuilder.setDefaults(defaults);
        mNotifyBuilder.setAutoCancel(true);
        mNotificationManager.notify(msg.getString("id"), 0, mNotifyBuilder.build());


    }

}
