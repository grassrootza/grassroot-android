package org.grassroot.android.services;

import android.content.Context;
import android.os.AsyncTask;

import org.grassroot.android.utils.NetworkUtils;

/**
 * Created by luke on 2016/07/22.
 */
public class StartUpTask extends AsyncTask<Context, Void, Void> {

    private static final String TAG = StartUpTask.class.getSimpleName();

    @Override
    protected Void doInBackground(Context... params) {
        LocationServices.getInstance().connect();
        final Context context = params[0];
        if (NetworkUtils.isNetworkAvailable(context)) {
            NetworkUtils.syncLocalAndServer(context);
        }
        return null;
    }
}