package org.grassroot.android.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import org.grassroot.android.services.ApplicationLoader;

public class NetworkUtils {

	public static boolean isNetworkAvailable() {
		return isNetworkAvailable(ApplicationLoader.applicationContext);
	}

	public static boolean isNetworkAvailable(Context context) {
		ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo ni = cm.getActiveNetworkInfo();
		return (ni != null && ni.isAvailable() && ni.isConnected());
	}
}
