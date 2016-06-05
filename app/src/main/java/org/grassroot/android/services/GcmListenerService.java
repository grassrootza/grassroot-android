package org.grassroot.android.services;

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
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;
import org.grassroot.android.R;
import org.grassroot.android.ui.activities.NotBuiltActivity;
import org.grassroot.android.ui.activities.ViewVoteActivity;
import org.grassroot.android.utils.Constant;
import org.grassroot.android.utils.PreferenceUtils;

import java.util.List;

/**
 * Created by paballo on 2016/05/09.
 */
public class GcmListenerService extends com.google.android.gms.gcm.GcmListenerService {

    private static final String TAG = GcmListenerService.class.getCanonicalName();
    private static final String STACK_KEY = "stack_key";


    @Override
    public void onMessageReceived(String from, Bundle data) {
        Log.e(TAG, "message received, from : " + from);
        incrementNotificationCounter();
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

        Log.e(TAG, "Received a push notification from server, looks like: + " + msg.toString());
        PendingIntent resultPendingIntent = generateResultIntent(msg);
        long when = System.currentTimeMillis();
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= 21) {

            NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();
            NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this);
            Notification notification = mBuilder.setSmallIcon((Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                    ? R.drawable.ic_notification_icon : R.drawable.app_icon).setTicker(msg.getString("title")).setWhen(when)
                    .setAutoCancel(true)
                    .setContentTitle(msg.getString(Constant.TITLE))
                    .setStyle(inboxStyle)
                    .setContentIntent(resultPendingIntent)
                    .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                    .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.app_icon))
                    .setContentText(msg.getString(Constant.BODY))
                    .setGroup(STACK_KEY)
                    .build();
            mNotificationManager.notify(msg.getString(Constant.UID), (int) System.currentTimeMillis(), notification);
        } else {
            Log.e(TAG, "notification received, following KitKat branch");
            NotificationCompat.Builder mNotifyBuilder = new NotificationCompat.Builder(this);
            Notification notification = mNotifyBuilder
                    .setContentTitle(msg.getString(Constant.TITLE))
                    .setWhen(when)
                    .setContentText(msg.getString(Constant.BODY))
                    .setAutoCancel(true)
                    .setSmallIcon(R.drawable.app_icon)
                    .setContentIntent(resultPendingIntent)
                    .setGroup(STACK_KEY)
                    .build();


            int defaults = 0;
            defaults = defaults | Notification.DEFAULT_LIGHTS;
            defaults = defaults | Notification.DEFAULT_SOUND;
            mNotifyBuilder.setDefaults(defaults);
            mNotificationManager.notify(msg.getString(Constant.UID), (int) System.currentTimeMillis(), notification);

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

        int notificationcount = PreferenceUtils.getIsNotificationcounter(this);
        Log.e(TAG, "b4 notificationcount is " + notificationcount);
        notificationcount++;
        PreferenceUtils.setIsNotificationcounter(this, notificationcount);
        Log.e(TAG, "after notificationcount is " + notificationcount);

    }


    private PendingIntent generateResultIntent(Bundle msg) {

        Log.e(TAG, "generateResultIntent called, with message bundle: " + msg.toString());

        Intent resultIntent;
        if (msg.getString(Constant.ENTITY_TYPE).equalsIgnoreCase("vote")) {
            resultIntent = new Intent(this, ViewVoteActivity.class);
            ;
        } else {
            resultIntent = new Intent(this, NotBuiltActivity.class);
        }
        resultIntent.putExtra(Constant.ENTITY_TYPE, msg.getString(Constant.ENTITY_TYPE));
        resultIntent.putExtra(Constant.TITLE, msg.getString(Constant.TITLE));
        resultIntent.putExtra(Constant.BODY, msg.getString(Constant.BODY));
        resultIntent.putExtra(Constant.UID, msg.getString(Constant.UID));
        resultIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK );

        PendingIntent resultPendingIntent = PendingIntent.getActivity(this, (int) System.currentTimeMillis(), resultIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        Log.e(TAG, "generateResultIntent exiting, with intent: " + resultPendingIntent.toString());

        return resultPendingIntent;


    }


}