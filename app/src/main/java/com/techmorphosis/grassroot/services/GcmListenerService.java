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
import com.techmorphosis.grassroot.utils.PreferenceUtils;

import java.util.List;

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
        Intent resultIntent = generateResultIntent(msg);
        PendingIntent resultPendingIntent = PendingIntent.getActivity(getBaseContext(), 0,
                resultIntent,PendingIntent.FLAG_CANCEL_CURRENT);

        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
      if (Build.VERSION.SDK_INT >= 21) {

            NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();
            NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(getApplicationContext());
            Notification notification = mBuilder.setSmallIcon((Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                    ? R.drawable.ic_notification_icon : R.drawable.app_icon).setTicker(msg.getString("title")).setWhen(0)
                    .setAutoCancel(true)
                    .setContentTitle(msg.getString("title"))
                    .setStyle(inboxStyle)
                    .setContentIntent(resultPendingIntent)
                    .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                    .setLargeIcon(BitmapFactory.decodeResource(getBaseContext().getResources(), R.drawable.app_icon))
                    .setContentText(msg.getString("body"))
                    .setGroup(STACK_KEY)
                    .build();

            mNotificationManager.notify(msg.getString("id"), 0, notification);

        } else {
            NotificationCompat.Builder mNotifyBuilder = new NotificationCompat.Builder(getBaseContext())
                    .setContentTitle(msg.getString("title"))
                    .setContentText(msg.getString("body"))
                    .setAutoCancel(true)
                    .setSmallIcon(R.drawable.app_icon)
                    .setContentIntent(resultPendingIntent)
                    .setGroup(STACK_KEY);

            int defaults = 0;
            defaults = defaults | Notification.DEFAULT_LIGHTS;
            defaults = defaults | Notification.DEFAULT_SOUND;
            mNotifyBuilder.setDefaults(defaults);
            mNotificationManager.notify(msg.getString("id"), 0, mNotifyBuilder.build());

        }


    }

    public boolean isAppIsInBackground(Context context) {
        boolean isInBackground = true;
        ActivityManager am = (ActivityManager) this.getSystemService(Context.ACTIVITY_SERVICE);
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


    public void incrementNotificationCounter() {

        int notificationcount = PreferenceUtils.getIsNotificationcounter(getApplicationContext());
        Log.e(TAG, "b4 notificationcount is " + notificationcount);
        notificationcount++;
        PreferenceUtils.setIsNotificationcounter(getApplicationContext(), notificationcount);
        Log.e(TAG, "after notificationcount is " + notificationcount);

    }


    private Intent generateResultIntent(Bundle msg) {
        Intent resultIntent = new Intent(getBaseContext(), HomeScreenActivity.class);
        if (msg.getString("entity_type").equalsIgnoreCase("vote")) {
            resultIntent = new Intent(getBaseContext(), ViewVote.class);
            resultIntent.putExtra("id", msg.getString("id"));
        } else {
            resultIntent = new Intent(getBaseContext(), NotBuiltActivity.class);

        }
        resultIntent.putExtra("entity_type", msg.getString("entity_type"));
        Log.d(TAG, msg.getString("entity_type"));
        resultIntent.putExtra("title", msg.getString("title"));
        resultIntent.putExtra("body", msg.getString("body"));
        resultIntent.putExtra("id", msg.getString("id"));

        return resultIntent;


    }
}
