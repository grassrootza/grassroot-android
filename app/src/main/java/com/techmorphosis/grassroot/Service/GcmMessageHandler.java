/**
 * Copyright 2015 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.techmorphosis.grassroot.Service;

import android.app.ActivityManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.android.gms.gcm.GcmListenerService;
import com.techmorphosis.grassroot.R;
import com.techmorphosis.grassroot.ui.activities.HomeScreen;
import com.techmorphosis.grassroot.ui.activities.ViewVote;
import com.techmorphosis.grassroot.utils.SettingPreffrence;

import java.util.List;

public class GcmMessageHandler extends GcmListenerService {

    private static final String TAG = "GcmMessageHandler";
    private NotificationUtils notificationUtils;

    /**
     * Called when message is received.
     *
     * @param from SenderID of the sender.
     * @param data Data bundle containing message data as key/value pairs.
     *             For Set of keys use data.keySet().
     */
    // [START receive_message]
    @Override
    public void onMessageReceived(String from, Bundle data) {
        String title = data.getString("title");
        String body = data.getString("body");
        String id = data.getString("id");
        String entity_type = data.getString("entity_type");

        Log.e(TAG, "From: " + from);
        Log.e(TAG, "body: " + body);
        Log.e(TAG, "title: " + title);
        Log.e(TAG, "id : " + id);
        Log.e(TAG, "entity_type : " + entity_type);
        Log.e(TAG, "data is : " + data.toString());


        if (SettingPreffrence.getisLoggedIn(getBaseContext())) {
            if (entity_type.equalsIgnoreCase("Vote"))
            {
                Intent resultIntent;

                if (isAppIsInBackground(getBaseContext())) {//Notification
                    Log.e(TAG, "App close : ");
                    resultIntent = new Intent(getBaseContext(),ViewVote.class);
                    notificationCounter();
                }
                else//Alert
                {
                    Log.e(TAG, "App open : ");
                     resultIntent = new Intent(getBaseContext(),HomeScreen.class);
                }
                showNotificationMessage(getBaseContext(),data,resultIntent);

            }
        }

        // sendNotification(title);




    }

    private void notificationCounter() {

        int notificationcount = SettingPreffrence.getIsNotificationcounter(getApplicationContext());
        Log.e(TAG, "b4 notificationcount is " + notificationcount);


        notificationcount++;
        SettingPreffrence.setIsNotificationcounter(getApplicationContext(), notificationcount);
        Log.e(TAG, "after notificationcount is " + notificationcount);

        /*
        if (notificationcount == 0)
        {
            SettingPreffrence.setIsRateuscounter(getApplicationContext(), 1);
            Log.e(TAG, "counter is firsttime");
            Log.e(TAG, "after increase notificationcount is " + notificationcount);

        }
        else
        {
            notificationcount++;
            SettingPreffrence.setIsRateuscounter(getApplicationContext(), notificationcount);

        }
*/
    }
    // [END receive_message]

    /**
     * Create and show a simple notification containing the received GCM message.
     *
     * @param message GCM message received.
     *//*
    private void sendNotification(String message) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0 *//* Request code *//*, intent,
                PendingIntent.FLAG_ONE_SHOT);

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.app_icon)
                .setContentTitle("GCM Message")
                .setContentText(message)
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify(0 *//* ID of notification *//*, notificationBuilder.build());
    }*/

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


    /**
     * Shows the notification message in the notification bar
     * If the app is in background, launches the app
     */
    private void showNotificationMessage(Context context,  Bundle data,Intent intent) {

        notificationUtils = new NotificationUtils(context);

        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        notificationUtils.showNotificationMessage(data, intent);
    }


}
