package org.grassroot.android.interfaces;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;

import org.grassroot.android.fragments.dialogs.NetworkErrorDialogFragment;
import org.grassroot.android.utils.Constant;

/**
 * Created by paballo on 2016/06/02.
 */
public abstract class NetworkErrorDialogListener {

    private static final String TAG = NetworkErrorDialogFragment.class.getCanonicalName();


    public abstract void retryClicked() ;

    public void checkNetworkSettingsClicked(Activity activity){
        Log.e(TAG,activity.getLocalClassName());
        activity.startActivityForResult(new Intent(android.provider.Settings.ACTION_WIRELESS_SETTINGS), Constant.activityNetworkSettings);
    }

}
