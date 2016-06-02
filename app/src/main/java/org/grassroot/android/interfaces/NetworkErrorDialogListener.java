package org.grassroot.android.interfaces;

import android.app.Activity;
import android.content.Intent;

/**
 * Created by paballo on 2016/06/02.
 */
public abstract class NetworkErrorDialogListener {

    private static final int REQUEST_CODE = 333333;

    public abstract void retryClicked() ;

    public void checkNetworkSettingsClicked(Activity activity){
        activity.startActivityForResult(new Intent(android.provider.Settings.ACTION_WIRELESS_SETTINGS),REQUEST_CODE);
    }
}
