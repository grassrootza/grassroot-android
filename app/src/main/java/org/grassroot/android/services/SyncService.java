package org.grassroot.android.services;

import android.content.Intent;
import android.util.Log;

import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.GcmTaskService;
import com.google.android.gms.gcm.TaskParams;

import org.grassroot.android.models.exceptions.ApiCallException;
import org.grassroot.android.models.PreferenceObject;
import org.grassroot.android.receivers.TaskManagerReceiver;
import org.grassroot.android.utils.NetworkUtils;
import org.grassroot.android.utils.RealmUtils;

import rx.Subscriber;
import rx.schedulers.Schedulers;

public class SyncService extends GcmTaskService {

	private static final String TAG = SyncService.class.getSimpleName();

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
                    result = GcmNetworkManager.RESULT_SUCCESS;
                    break;
                case NetworkUtils.OFFLINE_ON_FAIL:
                    tryConnect();
                    result = GcmNetworkManager.RESULT_SUCCESS;
                    break;
            }
        }

        // Create Intent to broadcast the task information.
        Intent intent = new Intent();
        intent.setAction(TaskManagerReceiver.ACTION_DONE);
        sendBroadcast(intent);

        return result;
    }

	private void tryConnect() {
		NetworkUtils.trySwitchToOnlineRx(this, true, Schedulers.immediate())
			.subscribe(new Subscriber<String>() {
				@Override
				public void onError(Throwable e) {
					if (e instanceof ApiCallException) {
						if (NetworkUtils.NO_NETWORK.equals(e.getMessage())) {
							Log.d(TAG, "networkNotAvailable");
						} else {
							Log.d(TAG, "networkAvailableButConnectFailed");
						}
					}
				}

				@Override
				public void onNext(String s) {
					if (NetworkUtils.ONLINE_DEFAULT.equals(s)) {
						doPeriodicTask();
					}
				}

				@Override
				public void onCompleted() { }
			});
	}

    private int doPeriodicTask() {
        NetworkUtils.syncLocalAndServer(this);
        return GcmNetworkManager.RESULT_SUCCESS;
    }
}
