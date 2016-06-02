package org.grassroot.android.interfaces;

import android.app.Activity;
import android.content.Intent;
import android.support.v4.app.Fragment;

/**
 * Created by paballo on 2016/06/02.
 */
public abstract class NetworkErrorDialogListener {

    private static final int REQUEST_CODE = 20;

    public abstract void retryClicked() ;

    public void checkNetworkSettingsClicked(Activity activity){
        activity.startActivityForResult(new Intent(android.provider.Settings.ACTION_WIRELESS_SETTINGS),REQUEST_CODE);
    }

    //for fragments
    public void checkNetworkSettingsClicked(Fragment fragment){
        fragment.startActivityForResult(new Intent(android.provider.Settings.ACTION_WIRELESS_SETTINGS),REQUEST_CODE);
    }
}
