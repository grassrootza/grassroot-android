package org.grassroot.android.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import org.grassroot.android.utils.NetworkUtils;

/**
 * Created by luke on 2016/08/26.
 */
public class BatteryLevelReceiver extends BroadcastReceiver {

	private static final String TAG = BatteryLevelReceiver.class.getSimpleName();

	@Override
	public void onReceive(Context context, Intent intent) {
		if (intent.getAction().equals(Intent.ACTION_BATTERY_LOW)) {
			Log.d(TAG, "setting battery to low power");
			NetworkUtils.setBatteryLow(true);
		} else if (intent.getAction().equals(Intent.ACTION_BATTERY_OKAY)) {
			Log.d(TAG, "setting battery to okay power");
			NetworkUtils.setBatteryLow(false);
		}
	}
}
