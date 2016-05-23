 package com.techmorphosis.grassroot.Service;

 import android.app.ActivityManager;
 import android.app.Notification;
 import android.app.NotificationManager;
 import android.app.PendingIntent;
 import android.content.ComponentName;
 import android.content.Context;
 import android.content.Intent;
 import android.graphics.BitmapFactory;
 import android.media.RingtoneManager;
 import android.os.Build;
 import android.os.Bundle;
 import android.support.v4.app.NotificationCompat;
 import android.util.Log;

 import com.techmorphosis.grassroot.R;

 import java.util.List;


 /**
  * Created by Ravi on 01/06/15.
  */
 public class NotificationUtils {

     private String TAG = NotificationUtils.class.getSimpleName();

     private Context mContext;
     private int Not_id;

     public NotificationUtils() {
     }

     public NotificationUtils(Context mContext) {
         this.mContext = mContext;
     }

     public void showNotificationMessage(Bundle data,Intent intent) {

         String title = data.getString("title");
         String body = data.getString("body");
         String id = data.getString("id");
         String entity_type = data.getString("entity_type");

         intent.putExtra("entity_type", entity_type);
         intent.putExtra("title", title);
         intent.putExtra("body", body);
         intent.putExtra("id", id);

         if (isAppIsInBackground(mContext)) {//appbackround=true  //show notification only
             int largeicon = R.drawable.app_icon;

             Log.e(TAG, "body: " + body);
             Log.e(TAG, "title: " + title);
             Log.e(TAG, "id : " + id);
             Log.e(TAG, "entity_type : " + entity_type);
             Log.e(TAG, "data is : " + data.toString());



//             Not_id = (int) Long.parseLong(id.replaceAll("\\D", ""));

              Not_id=(int) (Math.random() * Integer.parseInt("1"));
             Log.e(TAG, "i is " + Not_id);

             PendingIntent resultPendingIntent = PendingIntent.getActivity(mContext, Not_id, intent, PendingIntent.FLAG_CANCEL_CURRENT
             );


                 if (Build.VERSION.SDK_INT >= 21)
                 {
                     Log.e(TAG, "lollipop");

                     NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();

                     NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(mContext);

                     Notification noti = mBuilder.setSmallIcon(getNotificationIcon()).setTicker(title).setWhen(0)
                             .setAutoCancel(true)
                             .setContentTitle(title)
                             .setStyle(inboxStyle)
                             .setContentIntent(resultPendingIntent)
                             .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                             .setLargeIcon(BitmapFactory.decodeResource(mContext.getResources(), largeicon))
                             .setContentText(body)
                             .build();

                     NotificationManager notificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);

                     notificationManager.notify(Not_id, noti);

                 }
                 else
                 {
                     Log.e(TAG,"pre lollipop");

                    NotificationCompat.Builder noti = new NotificationCompat.Builder(mContext)
                         .setContentTitle(title)
                         .setContentText(body)
                         .setSmallIcon(largeicon)
                         .setContentIntent(resultPendingIntent)
                         .setAutoCancel(true)
                         .setDefaults(Notification.DEFAULT_ALL);

                     NotificationManager notificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);

                     notificationManager.notify(Not_id, noti.build());

                 }







         } else {//appbackround=false  //open activity only

             intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP  | Intent.FLAG_ACTIVITY_CLEAR_TOP);
             mContext.startActivity(intent);

         }
     }

     /**
      * Method checks if the app is in background or not
      *
      * @param context
      * @return
      */
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

     private int getNotificationIcon() {
         boolean useWhiteIcon = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP);
         return useWhiteIcon ? R.drawable.ic_notification_icon : R.drawable.app_icon;
     }

 }
