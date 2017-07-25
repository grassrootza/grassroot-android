package org.grassroot.android.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

import org.grassroot.android.interfaces.GroupConstants;
import org.grassroot.android.interfaces.NavigationConstants;
import org.grassroot.android.interfaces.TaskConstants;
import org.grassroot.android.services.ApplicationLoader;

/**
 * Created by luke on 2016/05/05.
 * Currently mixes up system permissions & app permissions, probably not the best idea, to fix in future
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
        ActivityCompat.requestPermissions(callingActivity, new String[]{Manifest.permission.READ_CONTACTS},
            NavigationConstants.ASK_CONTACT_PERMISSION);
    }

    public static boolean checkContactsPermissionGranted(int requestCode, int[] grantResults) {
        return (requestCode == NavigationConstants.ASK_CONTACT_PERMISSION) &&
                (grantResults.length > 0) &&
                (grantResults[0] == PackageManager.PERMISSION_GRANTED);
    }

    public static boolean checkFilePermissions() {
        return checkFileWritePermission(ApplicationLoader.applicationContext) &&
                checkFileReadPermission(ApplicationLoader.applicationContext);
    }

    private static boolean checkFileReadPermission(Context context) {
        return Build.VERSION.SDK_INT < 23 || genericPermissionCheck(context, Manifest.permission.READ_EXTERNAL_STORAGE);
    }

    private static boolean checkFileWritePermission(Context context) {
        return Build.VERSION.SDK_INT < 23 || genericPermissionCheck(context, Manifest.permission.WRITE_EXTERNAL_STORAGE);
    }

    public static void requestFilePermissions(Activity callingActivity) {
        if (Build.VERSION.SDK_INT >= 16) {
            ActivityCompat.requestPermissions(callingActivity,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    NavigationConstants.ASK_FILE_PERMISSION);
        }
    }

    public static boolean checkFilePermissionsGranted(int requestCode, int[] grantResults) {
        return Build.VERSION.SDK_INT < 23 || ((requestCode == NavigationConstants.ASK_FILE_PERMISSION) &&
                (grantResults.length > 0) &&
                (grantResults[0] == PackageManager.PERMISSION_GRANTED));
    }

    public static String permissionForTaskType(String taskType) {
        switch (taskType) {
            case TaskConstants.MEETING:
                return GroupConstants.PERM_CREATE_MTG;
            case TaskConstants.VOTE:
                return GroupConstants.PERM_CALL_VOTE;
            case TaskConstants.TODO:
                return GroupConstants.PERM_CREATE_TODO;
            default:
                throw new UnsupportedOperationException("Error! Unknown task type");
        }
    }

}