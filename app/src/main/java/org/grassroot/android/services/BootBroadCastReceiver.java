package org.grassroot.android.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Created by paballo on 2016/10/18.
 */

public class BootBroadCastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        Intent i = new Intent(context, GcmRegistrationService.class);
        context.startService(i);
    }
}


