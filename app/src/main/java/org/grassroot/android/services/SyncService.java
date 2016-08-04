package org.grassroot.android.services;

import android.content.Intent;
import android.util.Log;

import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.GcmTaskService;
import com.google.android.gms.gcm.TaskParams;

import org.grassroot.android.models.PreferenceObject;
import org.grassroot.android.receivers.TaskManagerReceiver;
import org.grassroot.android.utils.NetworkUtils;
import org.grassroot.android.utils.RealmUtils;

public class SyncService extends GcmTaskService implements NetworkUtils.NetworkListener{

   private static final String TAG = SyncService.class.getName();
    @Override
    public int onRunTask(TaskParams taskParams) {
        Log.d(TAG, "onRunTask: " + taskParams.getTag());

        String tag = taskParams.getTag();

        // Default result is success.
        int result = GcmNetworkManager.RESULT_SUCCESS;

        // Choose method based on the tag.
        if (TaskManagerReceiver.TASK_TAG_PERIODIC.equals(tag)) {
            PreferenceObject object = RealmUtils.loadPreferencesFromDB();
            switch (object.getOnlineStatus()){
                case NetworkUtils.ONLINE_DEFAULT:
                    result = doPeriodicTask();
                    break;
                case NetworkUtils.OFFLINE_SELECTED:
                    result = GcmNetworkManager.RESULT_RESCHEDULE;
                    break;
                case NetworkUtils.OFFLINE_ON_FAIL:
                    NetworkUtils.trySwitchToOnline(this, true, this);
                    result = GcmNetworkManager.RESULT_RESCHEDULE;
                    break;
            }
        }

        // Create Intent to broadcast the task information.
        Intent intent = new Intent();
        intent.setAction(TaskManagerReceiver.ACTION_DONE);
        sendBroadcast(intent);

        return result;
    }

    private int doPeriodicTask(){
        NetworkUtils.syncLocalAndServer(this);
        return GcmNetworkManager.RESULT_RESCHEDULE;
    }

    @Override
    public void connectionEstablished() {
        doPeriodicTask();
    }

    @Override
    public void networkAvailableButConnectFailed(String failureType) {
        Log.d(TAG, "networkAvailableButConnectFailed: " + failureType);
    }

    @Override
    public void networkNotAvailable() {
        Log.d(TAG, "networkNotAvailable");

    }

    @Override
    public void setOffline() {
        Log.d(TAG, "setOffline");

    }
}
