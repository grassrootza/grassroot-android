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
import android.os.Handler;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import org.grassroot.android.R;
import org.grassroot.android.activities.ViewTaskActivity;
import org.grassroot.android.events.NotificationCountChangedEvent;
import org.grassroot.android.interfaces.NotificationConstants;
import org.grassroot.android.models.GenericResponse;
import org.grassroot.android.models.PreferenceObject;
import org.grassroot.android.utils.Constant;
import org.grassroot.android.utils.RealmUtils;
import org.greenrobot.eventbus.EventBus;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Created by paballo on 2016/05/09.
 */
public class GcmListenerService extends com.google.android.gms.gcm.GcmListenerService {

  private static final String TAG = GcmListenerService.class.getCanonicalName();
  private static final String STACK_KEY = "stack_key";

  @Override public void onMessageReceived(String from, Bundle data) {
    Log.e(TAG, "message received, from : " + from);
    incrementNotificationCounter();
    relayNotification(data);
  }

  @Override public void onMessageSent(String msgId) {
    super.onMessageSent(msgId);
    Log.d(TAG, "message with id " + msgId + " sent.");
  }

  @Override public void onSendError(String msgId, String error) {
    super.onSendError(msgId, error);
    Log.d(TAG, error);
  }

  private void relayNotification(Bundle msg) {

    Log.e(TAG, "Received a push notification from server, looks like: + " + msg.toString());
    PendingIntent resultPendingIntent = generateResultIntent(msg);
    long when = System.currentTimeMillis();

    NotificationManager mNotificationManager =
        (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

    final String entityReferencedUid = msg.getString(NotificationConstants.ENTITY_UID);
    final String notificationUid = msg.getString(NotificationConstants.NOTIFICATION_UID);

    if (Build.VERSION.SDK_INT >= 21) {
      NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();
      NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this);
      Notification notification = mBuilder.setTicker(msg.getString(Constant.TITLE))
          .setWhen(when)
          .setAutoCancel(true)
          .setContentTitle(msg.getString(Constant.TITLE))
          .setStyle(inboxStyle)
          .setContentIntent(resultPendingIntent).setSmallIcon((Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) ? R.drawable.ic_notification_icon
          : R.drawable.app_icon)
          .setDefaults(Notification.DEFAULT_ALL)
          .setSound(RingtoneManager.getActualDefaultRingtoneUri(getApplicationContext(),
              RingtoneManager.TYPE_NOTIFICATION))
          .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.app_icon))
          .setContentText(msg.getString(Constant.BODY))
          .setGroup(STACK_KEY)
          .setGroupSummary(true)
          .build();
        mNotificationManager.notify(entityReferencedUid, (int) System.currentTimeMillis(),
            notification);

    } else {
      Log.e(TAG, "notification received, following KitKat branch");
      NotificationCompat.Builder mNotifyBuilder = new NotificationCompat.Builder(this);
      Notification notification = mNotifyBuilder.setContentTitle(msg.getString(Constant.TITLE))
          .setWhen(when)
          .setContentText(msg.getString(Constant.BODY))
          .setAutoCancel(true)
          .setSmallIcon(R.drawable.app_icon)
          .setContentIntent(resultPendingIntent)
          .setDefaults(Notification.DEFAULT_ALL)
          .setSound(RingtoneManager.getActualDefaultRingtoneUri(getApplicationContext(),
              RingtoneManager.TYPE_NOTIFICATION))
          .build();
        mNotificationManager.notify(entityReferencedUid, (int) System.currentTimeMillis(),
            notification);

    }

    GrassrootRestService.getInstance()
        .getApi()
        .updateRead(RealmUtils.loadPreferencesFromDB().getMobileNumber(),
            RealmUtils.loadPreferencesFromDB().getToken(), notificationUid)
        .enqueue(new Callback<GenericResponse>() {
          @Override
          public void onResponse(Call<GenericResponse> call, Response<GenericResponse> response) {
            if (response.isSuccessful()) {
              Log.d(TAG, "Set message as read!");
            } else {
              Log.d(TAG, "Failed: " + response.errorBody());
            }
          }

          @Override public void onFailure(Call<GenericResponse> call, Throwable t) {
            Log.e(TAG, "Something went wrong updating it:");
          }
        });
  }

  public static boolean isAppIsInBackground(Context context) {
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

  public void incrementNotificationCounter() {
    PreferenceObject preferenceObject = RealmUtils.loadPreferencesFromDB();

    final int currentCount = preferenceObject.getNotificationCounter();
    new Handler(getMainLooper()).post(new Runnable() {
      @Override public void run() {
        EventBus.getDefault().post(new NotificationCountChangedEvent(currentCount + 1));
      }
    });
    preferenceObject.setNotificationCounter(currentCount + 1);
    RealmUtils.saveDataToRealm(preferenceObject).subscribe();
  }

  private PendingIntent generateResultIntent(Bundle msg) {

    Log.d(TAG, "generateResultIntent called, with message bundle: " + msg.toString());

    Intent resultIntent = new Intent(this, ViewTaskActivity.class);

    // todo : consider just passing the whole message?
    resultIntent.putExtra(NotificationConstants.ENTITY_TYPE, msg.getString(NotificationConstants.ENTITY_TYPE));
    resultIntent.putExtra(NotificationConstants.ENTITY_UID, msg.getString(NotificationConstants.ENTITY_UID));
    resultIntent.putExtra(NotificationConstants.NOTIFICATION_UID, msg.getString(NotificationConstants.NOTIFICATION_UID));
    resultIntent.putExtra(NotificationConstants.CLICK_ACTION, msg.getString(NotificationConstants.CLICK_ACTION));
    resultIntent.putExtra(NotificationConstants.BODY, msg.getString(NotificationConstants.BODY));

    PendingIntent resultPendingIntent = PendingIntent.getActivity(this, (int) System.currentTimeMillis(), resultIntent,
            PendingIntent.FLAG_CANCEL_CURRENT);

    Log.e(TAG, "and generateResultIntent exits, with intent: " + resultPendingIntent.toString());
    return resultPendingIntent;
  }

  public static void clearNotifications(Context context) {
    NotificationManager nMgr = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
    nMgr.cancelAll();
  }
}



