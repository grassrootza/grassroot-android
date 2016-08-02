package org.grassroot.android.services;

import android.content.Context;
import android.os.AsyncTask;

import org.grassroot.android.utils.NetworkUtils;

/**
 * Created by luke on 2016/08/01.
 */
public class NetworkReceiverTask extends AsyncTask<Context, Void, Void> {

	@Override
	protected Void doInBackground(Context... params) {
		LocationServices.getInstance().connect();
		final Context context = params[0];
		NetworkUtils.syncLocalAndServer(context);
		return null;
	}
}
