package org.grassroot.android.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

/**
 * Created by luke on 2016/05/05.
 */
public class PermissionUtils {

    private static final String TAG = PermissionUtils.class.getCanonicalName();

    public static boolean genericPermissionCheck(Context context, String permission) {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED;
    }

    public static boolean contactReadPermissionGranted(Context context) {
        return genericPermissionCheck(context, Manifest.permission.READ_CONTACTS);
    }

    public static void requestReadContactsPermission(Activity callingActivity) {
        ActivityCompat.requestPermissions(callingActivity, new String[]{Manifest.permission.READ_CONTACTS}, Constant.alertAskForContactPermission);
    }

    public static boolean checkContactsPermissionGranted(int requestCode, int[] grantResults) {
        return (requestCode == Constant.alertAskForContactPermission) &&
                (grantResults.length > 0) &&
                (grantResults[0] == PackageManager.PERMISSION_GRANTED);
    }

}