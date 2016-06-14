package org.grassroot.android.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class PreferenceUtils {

    // Shared pref file name
    private static final String PREF_NAME = "org.grassroot.android";

    // All Shared Preferences Keys
    private static final String IS_LOGIN = "IsLoggedIn";
    private static final String IS_RATEUS = "IsRateus";
    private static final String IS_HASGROUP = "IsHasgroup";
    private static final String IS_GCM = "IsGCM";

    private static final String PREF_USER_name = "Pref_user_name";
    private static final String PREF_USER_mobile_number = "Pref_user_mobile_number";
    private static final String PREF_USER_token = "Pref_user_token";

    private static final String IS_NOTIFICATION_COUNTER = "IsNotificationCounter";

    private static final String PREF_LANGUAGE = "language_preference";

    private static final String PREF_ALERT = "alert_preference";

    public static SharedPreferences getPref(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Create login session
     */

    public static boolean clearAll(Context context) {
        SharedPreferences settings = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.clear();
        return editor.commit();
    }

    public static void setLoggedInStatus(Context context, Boolean value) {
        getPref(context).edit().putBoolean(IS_LOGIN, value).apply();
    }

    public static boolean getLoggedInStatus(Context context) {
        return getPref(context).getBoolean(IS_LOGIN, false);
    }

    public  static boolean userHasGroups(Context context) {
        return getPref(context).getBoolean(IS_HASGROUP,false);
    }

    public  static void setUserHasGroups(Context context, Boolean value) {
        getPref(context).edit().putBoolean(IS_HASGROUP, value).apply();
    }

    public static void setUserName(Context context, String value) {
        getPref(context).edit().putString(PREF_USER_name, value).apply();
    }

    public static String getUserName(Context context) {
        return getPref(context).getString(PREF_USER_name, "");
    }

    public static void setUserPhoneNumber(Context context, String value) {
        getPref(context).edit().putString(PREF_USER_mobile_number, value).apply();
    }

    public static String getUserPhoneNumber(Context context) {
        return getPref(context).getString(PREF_USER_mobile_number, "");
    }

    public static void setAuthToken(Context context, String value) {
        getPref(context).edit().putString(PREF_USER_token, value).apply();
    }

    public static String getAuthToken(Context context) {
        return getPref(context).getString(PREF_USER_token, "");
    }

    public static void setPrefLanguage(Context context, String value){
        getPref(context).edit().putString(PREF_LANGUAGE, value).apply();
    }

    public static String getPrefLanguage(Context context){
        return getPref(context).getString(PREF_LANGUAGE,"");
    }


    public static void setPrefAlert(Context context, String value){
        getPref(context).edit().putString(PREF_ALERT, value).apply();
    }

    public static String getPrefAlert(Context context){
        return getPref(context).getString(PREF_ALERT, "");
    }

    public static void setIsGcmEnabled(Context context, boolean value) {
        getPref(context).edit().putBoolean(IS_GCM, value).apply();
    }

    public static boolean  getIsGcmEnabled(Context context){
        return getPref(context).getBoolean(IS_GCM, false);
    }

    public static int getNotificationCounter(Context context) {
            return getPref(context).getInt(IS_NOTIFICATION_COUNTER, 0);
    }

    public static void setNotificationCounter(Context context, Integer value) {
        getPref(context).edit().putInt(IS_NOTIFICATION_COUNTER, value).apply();
    }

    public static void setisRateus(Context context, Boolean value) {
        getPref(context).edit().putBoolean(IS_RATEUS, value).apply();
    }

}
