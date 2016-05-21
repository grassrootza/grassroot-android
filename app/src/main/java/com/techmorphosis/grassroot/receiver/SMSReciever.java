package com.techmorphosis.grassroot.receiver;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.telephony.SmsMessage;
import android.util.Log;

import com.techmorphosis.grassroot.Service.QuickstartPreferences;
import com.techmorphosis.grassroot.utils.SettingPreffrence;

import java.util.List;


/**
 * Created by karthik on 08-12-2015.
 */
public class SMSReciever extends BroadcastReceiver {

    public static final String TAG="SMSReciever";


    @Override
    public void onReceive(Context context, Intent intent) {

        final Bundle bundle=intent.getExtras();
        if (bundle!=null){
            Object[] pdusObj=(Object[])bundle.get("pdus");
            for (Object objPdus:pdusObj){
                SmsMessage currentMessage=SmsMessage.createFromPdu((byte[])objPdus);
                String senderAddress=currentMessage.getDisplayOriginatingAddress();
                String message=currentMessage.getDisplayMessageBody();

                if (!senderAddress.contains("Grassroot")){
                    return;
                }

                String otp=getOTP(message);

                SettingPreffrence.setPREF_OTP(context,otp);

                Intent registrationComplete = new Intent(QuickstartPreferences.REGISTRATION_COMPLETE);
                LocalBroadcastManager.getInstance(context).sendBroadcast(registrationComplete);


                /*if (isAppIsInBackground(context)){
                    OtpActivity.setOtp(otp);
                }
                else if (!isAppIsInBackground(context)) {
                    OtpActivity.setOtp(otp);
                }*/
                Log.e(TAG,"SMSReciever otp = "+otp);
            }
        }
    }

    private String getOTP(String message) {

        String code=null;
        int index=message.indexOf(":");
        if (index!=-1){
            int start=index+2;
            int end=4;
            code=message.substring(start,start+end);
            return code;
        }

        return code;
    }

    public static boolean isAppIsInBackground(Context context) {
        boolean isInBackground = true;
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT_WATCH) {
            List<ActivityManager.RunningAppProcessInfo> runningProcesses = am.getRunningAppProcesses();
            for (ActivityManager.RunningAppProcessInfo processInfo : runningProcesses) {
                if (processInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                    for (String activeProcess : processInfo.pkgList) {
                        if (activeProcess.equals(context.getPackageName())) {
                            isInBackground = false;
                        }
                    }
                }
            }
        } else {
            List<ActivityManager.RunningTaskInfo> taskInfo = am.getRunningTasks(1);
            ComponentName componentInfo = taskInfo.get(0).topActivity;
            if (componentInfo.getPackageName().equals(context.getPackageName())) {
                isInBackground = false;
            }
        }
        return isInBackground;
    }
}
