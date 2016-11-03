package org.grassroot.android.services;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.grassroot.android.utils.Constant;

/**
 * Created by paballo on 2016/10/18.
 */

public class NotificationService extends Service {

    private static final String TAG = NotificationService.class.getCanonicalName();
    private static NotificationService instance = null;
    //todo return different value for each build variant
    private static final String brokerUrl = Constant.stagingBrokerUrl;
    private static MqttAndroidClient client;


    @Override
    public void onCreate() {
        instance = this;
        ApplicationLoader.initPlayServices();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        instance = null;
        Intent intent = new Intent("org.grassroot.start");
        sendBroadcast(intent);
    }

    public static boolean isNotificationServiceRunning() {
        return instance != null;
    }



}
