package org.grassroot.android.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.grassroot.android.services.NotificationService;

/**
 * Created by paballo on 2016/10/18.
 */

public class BootBroadCastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Intent serviceIntent = new Intent(context, NotificationService.class);
        context.startService(serviceIntent);
    }
}

