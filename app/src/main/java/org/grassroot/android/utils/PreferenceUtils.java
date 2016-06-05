package org.grassroot.android.utils;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by user on 09-Oct-15.
 */
public class PreferenceUtils {

    // Shared Preferences
    SharedPreferences pref;

    // Shared pref file name
    private static final String PREF_NAME = "Grassroot";

    // All Shared Preferences Keys
    private static final String IS_LOGIN = "IsLoggedIn";
    private static final String IS_RATEUS = "IsRateus";
    private static final String IS_HASGROUP = "IsHasgroup";
    private static final String IS_GCM = "IsGCM";


    private static final String PREF_USER = "Pref_user_id";
    private static final String PREF_USER_name = "Pref_user_name";
    private static final String PREF_USER_mobile_number = "Pref_user_mobile_number";
    private static final String PREF_USER_token = "Pref_user_token";

    private static final String IS_NOTIFICATION_COUNTER = "IsNotificationCounter";
    private static final String PREF_GCM_token = "Pref_Gcm_token";
    private static final String PREF_GCM_SENT_TOKEN_TO_SERVER = "pref_Gcm_token_to_server";

    private static final String PREF_LANGUAGE = "language_preference";

    private static final String PREF_ALERT = "alert_preference";


    private static final String PREF_Phone_Token = "Pref_phone_token";

    private static final String PREF_OTP = "Pref_otp";


    public static SharedPreferences getPref(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Create login session
     */

    public static boolean clearAll(Context context)
    {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit().clear().commit();
       /* context.clear();
        context.apply();*/
    }


    public static void setisLoggedIn(Context context, Boolean value) {
        getPref(context).edit().putBoolean(IS_LOGIN, value).apply();

    }

    public static void setisRateus(Context context, Boolean value) {
        getPref(context).edit().putBoolean(IS_RATEUS, value).apply();

    }

    public static boolean getisLoggedIn(Context context) {
        return getPref(context).getBoolean(IS_LOGIN, false);

    }

    public  static boolean getisHasgroup(Context context)
    {
        return getPref(context).getBoolean(IS_HASGROUP,false);
    }

    public  static void setisHasgroup(Context context,Boolean value)
    {
        getPref(context).edit().putBoolean(IS_HASGROUP, value).apply();
    }


    public static void setuserid(Context context, String value) {

        getPref(context).edit().putString(PREF_USER, value).apply();
    }


    public static String getuserid(Context context) {
        return getPref(context).getString(PREF_USER, "");
    }


    public static void setuser_name(Context context, String value) {
        getPref(context).edit().putString(PREF_USER_name, value).apply();
    }

    public static String getuser_name(Context context) {
        return getPref(context).getString(PREF_USER_name, "");
    }

    public static void setuser_mobilenumber(Context context, String value) {
        getPref(context).edit().putString(PREF_USER_mobile_number, value).apply();
    }

    public static String getuser_mobilenumber(Context context) {
        return getPref(context).getString(PREF_USER_mobile_number, "");
    }

    public static void setuser_token(Context context, String value) {
        getPref(context).edit().putString(PREF_USER_token, value).apply();
    }

    public static String getuser_token(Context context) {
        return getPref(context).getString(PREF_USER_token, "");
    }

    public static void setuser_phonetoken(Context context, String value) {
        getPref(context).edit().putString(PREF_Phone_Token, value).apply();
    }
    public static void setPrefLanguage(Context context, String value){
        getPref(context).edit().putString(PREF_LANGUAGE, value).apply();
    }

    public static String getPrefLanguage(Context context){
        return getPref(context).getString(PREF_LANGUAGE,"");
    }


    public static void setPrefAlert(Context context, String value){
        getPref(context).edit().putString(PREF_ALERT, value);
    }

    public static String getPrefAlert(Context context){
        return getPref(context).getString(PREF_ALERT, "");
    }


    public static String getPREF_Phone_Token(Context context) {
        return getPref(context).getString(PREF_Phone_Token, "");
    }

    public static void setIsGcmEnabled(Context context, boolean value) {
        getPref(context).edit().putBoolean(IS_GCM, value).apply();
    }

    public static boolean  getIsGcmEnabled(Context context){
        return getPref(context).getBoolean(IS_GCM, false);
    }

    public static int getIsNotificationcounter(Context context) {
            return getPref(context).getInt(IS_NOTIFICATION_COUNTER, 0);
    }

    public static void setIsNotificationcounter(Context context, Integer value) {
        getPref(context).edit().putInt(IS_NOTIFICATION_COUNTER, value).apply();
    }

    public static void setPREF_Gcmtoken(Context context, String value) {
        getPref(context).edit().putString(PREF_GCM_token, value).apply();
    }

    public static String getPREF_Gcmtoken(Context context) {
        return getPref(context).getString(PREF_GCM_token, "");
    }

    public static void setPrefGcmSentTokenToServer(Context context, Boolean value) {
        getPref(context).edit().putBoolean(PREF_GCM_SENT_TOKEN_TO_SERVER, value).apply();
    }

    public static Boolean getPrefGcmSentTokenToServer(Context context) {
        return getPref(context).getBoolean(PREF_GCM_SENT_TOKEN_TO_SERVER, false);
    }

    public static void setPREF_OTP(Context context, String value) {
        getPref(context).edit().putString(PREF_OTP, value).apply();
    }

    public static String getPREF_OTP(Context context) {
        return getPref(context).getString(PREF_OTP, "");
    }

}
