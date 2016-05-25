package com.techmorphosis.grassroot.services;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.media.RingtoneManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;


import com.techmorphosis.grassroot.R;
import com.techmorphosis.grassroot.ui.activities.HomeScreenActivity;
import com.techmorphosis.grassroot.ui.activities.NotBuiltActivity;
import com.techmorphosis.grassroot.ui.activities.ViewVote;
import com.techmorphosis.grassroot.utils.SettingPreference;

import java.util.List;

/**
 * Created by paballo on 2016/05/09.
 */
public class GcmListenerService extends com.google.android.gms.gcm.GcmListenerService {

    private static final String TAG = GcmListenerService.class.getCanonicalName();
    private static final String STACK_KEY = "stack_key";


    @Override
    public void onMessageReceived(String from, Bundle data) {

        Intent resultIntent = null;
        switch (data.getString("entity_type").toLowerCase()){
            case "vote":
                resultIntent = new Intent(this, ViewVote.class);
                break;
            case "todo":
                resultIntent = new Intent(this,NotBuiltActivity.class );
                break;
            case "meeting":
                resultIntent = new Intent(this, NotBuiltActivity.class);
                break;
            default:
                resultIntent = new Intent(this, HomeScreenActivity.class);
        }

        if (SettingPreference.getisLoggedIn(this)) {
            if (isAppIsInBackground(this)) {
                notificationCounter();
                sendNotification(data);

            } else {

                String title = data.getString("title");
                String body = data.getString("body");
                String id = data.getString("id");
                String entity_type = data.getString("entity_type");
                resultIntent.putExtra("entity_type", entity_type);
                resultIntent.putExtra("title", title);
                resultIntent.putExtra("body", body);
                resultIntent.putExtra("id", id);
                resultIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP  | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                this.startActivity(resultIntent);

            }

        }


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
        Intent resultIntent = null;
        switch (msg.getString("entity_type").toLowerCase()){
            case "vote":
                resultIntent = new Intent(this, ViewVote.class);
                resultIntent.putExtra("id", msg.getString("id"));
                break;
            case "todo":
                resultIntent = new Intent(this,NotBuiltActivity.class );
                break;
            case "meeting":
                resultIntent = new Intent(this, NotBuiltActivity.class);
                break;
            default:
                resultIntent = new Intent(this, HomeScreenActivity.class);
        }

        PendingIntent resultPendingIntent = PendingIntent.getActivity(this, 0,
                resultIntent, PendingIntent.FLAG_ONE_SHOT);


        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= 21) {
            Log.e(TAG, "lollipop");

            NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();

            NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this);

            Notification notification = mBuilder.setSmallIcon((Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                    ? R.drawable.ic_notification_icon : R.drawable.app_icon).setTicker(msg.getString("title")).setWhen(0)
                    .setAutoCancel(true)
                    .setContentTitle(msg.getString("title"))
                    .setStyle(inboxStyle)
                    .setContentIntent(resultPendingIntent)
                    .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                    .setLargeIcon(BitmapFactory.decodeResource(this.getResources(), R.drawable.app_icon))
                    .setContentText(msg.getString("body"))
                    .build();


            mNotificationManager.notify(msg.getString("id"), 0, notification);

        } else {

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

    public boolean isAppIsInBackground(Context context) {
        boolean isInBackground = true;
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT_WATCH) {
            List<ActivityManager.RunningAppProcessInfo> runningProcesses = am.getRunningAppProcesses();
            for (ActivityManager.RunningAppProcessInfo processInfo : runningProcesses) {
                if (processInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                    for (String activeProcess : processInfo.pkgList) {
                        if (activeProcess.equals(context.getPackageName())) {
                            isInBackground = false;
                        }
                    }
                }
            }
        } else {
            List<ActivityManager.RunningTaskInfo> taskInfo = am.getRunningTasks(1);
            ComponentName componentInfo = taskInfo.get(0).topActivity;
            if (componentInfo.getPackageName().equals(context.getPackageName())) {
                isInBackground = false;
            }
        }

        return isInBackground;
    }


    private void notificationCounter() {

        int notificationcount = SettingPreference.getIsNotificationcounter(getApplicationContext());
        Log.e(TAG, "b4 notificationcount is " + notificationcount);
        notificationcount++;
        SettingPreference.setIsNotificationcounter(getApplicationContext(), notificationcount);
        Log.e(TAG, "after notificationcount is " + notificationcount);

    }


}
