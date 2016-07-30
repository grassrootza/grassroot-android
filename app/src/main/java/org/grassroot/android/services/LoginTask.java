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
public class LoginTask extends AsyncTask<Context, Void, Void> {

    private static final String TAG = LoginTask.class.getSimpleName();

    @Override
    protected Void doInBackground(Context... params) {
        final Context context = params[0];
		// note: don't need to call reset sync time here, as realm has been wiped, so last sync time will be 0
        if (NetworkUtils.isOnline(context)) {
            NetworkUtils.fetchEntitiesFromServer(context);
        }

        // since we only want to register if we are absolutely confident of connection ... (and fails above will/should switch to offline forced)
        if (NetworkUtils.isOnline(context)) {
            Log.d(TAG, "registering for GCM, on login ...");
            Intent gcmRegistrationIntent = new Intent(context, GcmRegistrationService.class);
            gcmRegistrationIntent.putExtra(NotificationConstants.ACTION, NotificationConstants.GCM_REGISTER);
            final String phoneNumber = RealmUtils.loadPreferencesFromDB().getMobileNumber();
            gcmRegistrationIntent.putExtra(NotificationConstants.PHONE_NUMBER, phoneNumber);
            context.startService(gcmRegistrationIntent);
        }

        LocationServices.getInstance().connect();
        return null;
    }
}