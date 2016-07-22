package org.grassroot.android.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.grassroot.android.utils.NetworkUtils;

public class NetworkConnectivityRecevier extends BroadcastReceiver {

  @Override public void onReceive(final Context context, Intent intent) {
    if (NetworkUtils.isNetworkAvailable(context)) {

      try {
        Thread.sleep(3000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }

      NetworkUtils.syncLocalAndServer(context);
    }
  }



}