package org.grassroot.android.services;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;

import org.grassroot.android.interfaces.NotificationConstants;
import org.grassroot.android.utils.NetworkUtils;
import org.grassroot.android.utils.RealmUtils;

/**
 * Created by luke on 2016/07/22.
 */
public class StartUpTask extends AsyncTask<Context, Void, Void> {

    private static final String TAG = StartUpTask.class.getSimpleName();

    @Override
    protected Void doInBackground(Context... params) {
        LocationServices.getInstance().connect();
        final Context context = params[0];
        if (NetworkUtils.isOnline(context)) {
            NetworkUtils.syncLocalAndServer(context);
        }
        // since we only want to register if we are absolutely confident of connection ... (and fails above will/should switch to offline forced)
        if (NetworkUtils.isOnline(context)) {
            Intent gcmRegistrationIntent = new Intent(context, GcmRegistrationService.class);
            gcmRegistrationIntent.putExtra(NotificationConstants.ACTION, NotificationConstants.GCM_REGISTER);
            final String phoneNumber = RealmUtils.loadPreferencesFromDB().getMobileNumber();
            gcmRegistrationIntent.putExtra(NotificationConstants.PHONE_NUMBER, phoneNumber);
            Log.e(TAG, "sending intent to GCM registration ...");
            context.startService(gcmRegistrationIntent);
        }
        return null;
    }
}