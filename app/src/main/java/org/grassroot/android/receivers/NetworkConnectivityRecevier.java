package org.grassroot.android.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import org.grassroot.android.utils.NetworkUtils;

public class NetworkConnectivityRecevier extends BroadcastReceiver {

  private static final String TAG = NetworkConnectivityRecevier.class.getSimpleName();

  @Override public void onReceive(final Context context, Intent intent) {
    if (NetworkUtils.isOnline(context)) {

      try {
        Thread.sleep(3000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }

      Log.e(TAG, "syncing local and server from receiver ...");
      NetworkUtils.syncLocalAndServer(context); // todo : move this to background
    }
  }



}