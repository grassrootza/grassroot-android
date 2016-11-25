package org.grassroot.android.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.PeriodicTask;
import com.google.android.gms.gcm.Task;

import org.grassroot.android.services.ApplicationLoader;
import org.grassroot.android.services.GcmListenerService;
import org.grassroot.android.services.SyncService;

public class TaskManagerReceiver extends BroadcastReceiver {

    private static final String TAG = TaskManagerReceiver.class.getName();

    private static GcmNetworkManager gcmNetworkManager;
    public static final String ACTION_START = "org.grassroot.android.ACTION_START";
    public static final String ACTION_DONE = "org.grassroot.android.ACTION_DONE";
    public static final String TASK_TAG_PERIODIC = "TASK_TAG_PERIODIC";

    private static final long REFRESH_PERIOD_NORMAL = 15*60; // roughly four times an hour

    @Override
    public void onReceive(Context context, Intent intent) {
        switch (intent.getAction()) {
            case ACTION_START:
                gcmNetworkManager = GcmNetworkManager.getInstance(context);
                if (gcmNetworkManager != null) {
                    gcmNetworkManager.cancelTask(TASK_TAG_PERIODIC, SyncService.class);
                    Task task = createPeriodicTask();
                    gcmNetworkManager.schedule(task);
                    Log.d(TAG, "Starting tasks");
                }
                break;
            case ACTION_DONE:
                if(ApplicationLoader.isAppIsInBackground(context) && gcmNetworkManager != null){
                    gcmNetworkManager.cancelTask(TASK_TAG_PERIODIC,SyncService.class);
                    Log.d(TAG,"App in background, cancelling tasks");
                }
                break;
            default:
              break;
        }
    }

    private Task createPeriodicTask() {
        PeriodicTask task = new PeriodicTask.Builder()
            .setService(SyncService.class)
            .setTag(TASK_TAG_PERIODIC)
            .setPeriod(REFRESH_PERIOD_NORMAL)
            .setFlex(30)
            .setUpdateCurrent(true)
            .setRequiredNetwork(Task.NETWORK_STATE_ANY)
            .build();
        return task;
    }

}
