package org.grassroot.android.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.v7.app.AlertDialog;

import org.grassroot.android.R;
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

	/*public static AlertDialog offlineDialog(Context context) {
		AlertDialog.Builder builder = new AlertDialog.Builder(context);

	}*/
}
