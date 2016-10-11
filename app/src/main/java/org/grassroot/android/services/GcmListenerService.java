package org.grassroot.android.services;

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
import android.os.Handler;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import org.grassroot.android.R;
import org.grassroot.android.activities.JoinRequestNoticeActivity;
import org.grassroot.android.activities.MultiMessageNotificationActivity;
import org.grassroot.android.activities.ViewTaskActivity;
import org.grassroot.android.events.GroupChatEvent;
import org.grassroot.android.events.JoinRequestEvent;
import org.grassroot.android.events.NotificationCountChangedEvent;
import org.grassroot.android.events.NotificationEvent;
import org.grassroot.android.interfaces.GroupConstants;
import org.grassroot.android.interfaces.NotificationConstants;
import org.grassroot.android.models.GroupJoinRequest;
import org.grassroot.android.models.Message;
import org.grassroot.android.models.PreferenceObject;
import org.grassroot.android.models.responses.GenericResponse;
import org.grassroot.android.utils.Constant;
import org.grassroot.android.utils.RealmUtils;
import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Created by paballo on 2016/05/09.
 */
public class GcmListenerService extends com.google.android.gms.gcm.GcmListenerService {

    private static final String TAG = GcmListenerService.class.getSimpleName();
    private static final int TASKS = 100;
    private static final int CHATS = 200;
    private static final int JOIN_REQUESTS = 300;
    private static final List<String> notificationMessages = new ArrayList<>();
    private static final List<String> chatMessages = new ArrayList<>();
    private static final List<String> joinRequests = new ArrayList<>();

    private static int displayedNotificationCount = 0;
    private static int displayedMessagesCount = 0;
    private static int displayedJoinRequestCount = 0;
    private static Notification notification;


    @Override
    public void onMessageReceived(String from, Bundle data) {
        Log.d(TAG, "message received, from : " + from);
        incrementNotificationCounter();
        if (data.get(NotificationConstants.ENTITY_TYPE).equals(NotificationConstants.CHAT_MESSAGE)) {
            handleChatMessages(data, this);
        } else {
            relayNotification(data, this);
        }
    }

    @Override
    public void onMessageSent(String msgId) {
        super.onMessageSent(msgId);
        Log.d(TAG, "message with id " + msgId + " sent.");
    }

    @Override
    public void onSendError(String msgId, String error) {
        super.onSendError(msgId, error);
        Log.d(TAG, "message with id " + msgId + " not sent.");
    }


    private static void relayNotification(Bundle msg, Context context) {

        Log.d(TAG, "Received a push notification from server");

        NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);

        final String notificationUid = msg.getString(NotificationConstants.NOTIFICATION_UID);
        final String entityType = msg.getString(NotificationConstants.ENTITY_TYPE);
        final NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context);
        if (entityType.equals(NotificationConstants.CHAT_MESSAGE)) {
            String title = msg.getString(Constant.TITLE).concat(": ");
            chatMessages.add(title.concat(msg.getString(Constant.BODY)));
            displayedMessagesCount++;
        } else {
            notificationMessages.add(msg.getString(Constant.BODY));
            displayedNotificationCount++;
        }
        PendingIntent resultPendingIntent = generateResultIntent(msg, context);
        long when = System.currentTimeMillis();

        if (Build.VERSION.SDK_INT >= 21) {
            if ((!entityType.equals(NotificationConstants.CHAT_MESSAGE) && !entityType.equals(NotificationConstants.JOIN_REQUEST)) && displayedNotificationCount > 1) {
                mNotificationManager.cancel(TASKS);
                notification = generateMultiLineNotification(resultPendingIntent, context, notificationMessages, entityType, msg, displayedNotificationCount);
            } else if (entityType.equals(NotificationConstants.CHAT_MESSAGE) && displayedMessagesCount > 1) {
                mNotificationManager.cancel(CHATS);
                notification = generateMultiLineNotification(resultPendingIntent, context, chatMessages, entityType, msg, displayedMessagesCount);
            } else if (entityType.equals(NotificationConstants.JOIN_REQUEST) && displayedJoinRequestCount > 1) {
                mNotificationManager.cancel(JOIN_REQUESTS);
                notification = generateMultiLineNotification(resultPendingIntent, context, joinRequests, entityType, msg, displayedJoinRequestCount);
            } else {
                notification = mBuilder
                        .setTicker(msg.getString(Constant.TITLE))
                        .setWhen(when)
                        .setAutoCancel(true)
                        .setContentTitle(msg.getString(Constant.TITLE))
                        .setContentIntent(resultPendingIntent)
                        .setSmallIcon((Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                                ? R.drawable.ic_notification_icon : R.drawable.app_icon)
                        .setDefaults(Notification.DEFAULT_ALL)
                        .setSound(RingtoneManager.getActualDefaultRingtoneUri(context,
                                RingtoneManager.TYPE_NOTIFICATION))
                        .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.drawable.app_icon))
                        .setContentText(msg.getString(Constant.BODY))
                        .setGroupSummary(true)
                        .build();
            }
        } else {
            Log.d(TAG, "notification received, following KitKat branch");
            if ((!entityType.equals(NotificationConstants.CHAT_MESSAGE) && !entityType.equals(NotificationConstants.JOIN_REQUEST)) && displayedNotificationCount > 1) {
                mNotificationManager.cancel(TASKS);
                notification = generateMultiLineNotification(resultPendingIntent, context, notificationMessages, entityType, msg, displayedNotificationCount);
            } else if (entityType.equals(NotificationConstants.CHAT_MESSAGE) && displayedMessagesCount > 1) {
                mNotificationManager.cancel(CHATS);
                notification = generateMultiLineNotification(resultPendingIntent, context, chatMessages, entityType, msg, displayedMessagesCount);
            } else if (entityType.equals(NotificationConstants.JOIN_REQUEST) && displayedJoinRequestCount > 1) {
                mNotificationManager.cancel(JOIN_REQUESTS);
                notification = generateMultiLineNotification(resultPendingIntent, context, joinRequests, entityType, msg, displayedJoinRequestCount);
            } else {
                notification = mBuilder.setContentTitle(msg.getString(Constant.TITLE))
                        .setWhen(when)
                        .setContentText(msg.getString(Constant.BODY))
                        .setAutoCancel(true)
                        .setSmallIcon(R.drawable.app_icon)
                        .setContentIntent(resultPendingIntent)
                        .setDefaults(Notification.DEFAULT_ALL)
                        .setSound(RingtoneManager.getActualDefaultRingtoneUri(context,
                                RingtoneManager.TYPE_NOTIFICATION))
                        .setGroupSummary(true)
                        .build();
            }
            }

            notification.number = (entityType.equals(NotificationConstants.CHAT_MESSAGE))
                    ? displayedMessagesCount : displayedNotificationCount;
            mNotificationManager.notify(!entityType.equals(NotificationConstants.CHAT_MESSAGE) ? TASKS :
                    CHATS, notification);
            GrassrootRestService.getInstance()
                    .getApi()
                    .updateRead(RealmUtils.loadPreferencesFromDB().getMobileNumber(), RealmUtils.loadPreferencesFromDB().getToken(), notificationUid)
                    .enqueue(new Callback<GenericResponse>() {
                        @Override
                        public void onResponse(Call<GenericResponse> call, Response<GenericResponse> response) {
                            Log.d(TAG, response.isSuccessful() ? "Set message as read!" : "Failed: " + response.errorBody());
                        }

                        @Override
                        public void onFailure(Call<GenericResponse> call, Throwable t) {
                            Log.e(TAG, "Something went wrong updating it:");
                        }
                    });

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

    public void incrementNotificationCounter() {
        PreferenceObject preferenceObject = RealmUtils.loadPreferencesFromDB();

        final int currentCount = preferenceObject.getNotificationCounter();
        new Handler(getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                EventBus.getDefault().post(new NotificationCountChangedEvent(currentCount + 1));
            }
        });

        preferenceObject.setNotificationCounter(currentCount + 1);
        RealmUtils.saveDataToRealmSync(preferenceObject); // otherwise this prefs object and setting has groups may trip over each other
    }

    private static PendingIntent generateResultIntent(Bundle msg, Context context) {
        Log.d(TAG, "generateResultIntent called, with message bundle: " + msg.toString());

        final String entityType = msg.getString(NotificationConstants.ENTITY_TYPE);
        Intent resultIntent;

        if (displayedMessagesCount > 1 || displayedNotificationCount > 1 || displayedJoinRequestCount > 1) {
            if (!entityType.equals(NotificationConstants.CHAT_MESSAGE)) {
                resultIntent = new Intent(context, MultiMessageNotificationActivity.class);
                switch (entityType) {
                    case NotificationConstants.JOIN_REQUEST:
                        final String groupUid = msg.getString("group");
                        handleJoinRequestInDB(entityType, groupUid);
                        resultIntent.putExtra(NotificationConstants.CLICK_ACTION, NotificationConstants.JOIN_REQUEST_LIST);
                        EventBus.getDefault().post(new JoinRequestEvent(TAG));
                        break;
                    default:
                        resultIntent.putExtra(NotificationConstants.CLICK_ACTION, NotificationConstants.NOTIFICATION_LIST);
                        EventBus.getDefault().post(new NotificationEvent());
                        break;
                }
            } else {
                resultIntent = new Intent(context, MultiMessageNotificationActivity.class);
                resultIntent.putExtra(GroupConstants.UID_FIELD, msg.getString(GroupConstants.UID_FIELD));
                resultIntent.putExtra(GroupConstants.NAME_FIELD, msg.getString(GroupConstants.NAME_FIELD));
                resultIntent.putExtra(NotificationConstants.CLICK_ACTION, NotificationConstants.CHAT_LIST);
            }
        } else {
            switch (entityType) {
                case NotificationConstants.JOIN_REQUEST:
                    final String groupUid = msg.getString("group");
                    handleJoinRequestInDB(entityType, groupUid);
                    resultIntent = new Intent(context, JoinRequestNoticeActivity.class);
                    EventBus.getDefault().post(new JoinRequestEvent(TAG));
                    break;
                case NotificationConstants.CHAT_MESSAGE:
                    resultIntent = new Intent(context, MultiMessageNotificationActivity.class);
                    resultIntent.putExtra(GroupConstants.UID_FIELD, msg.getString(GroupConstants.UID_FIELD));
                    resultIntent.putExtra(GroupConstants.NAME_FIELD, msg.getString(GroupConstants.NAME_FIELD));
                    break;
                default:
                    resultIntent = new Intent(context, ViewTaskActivity.class);
                    resultIntent.putExtra(NotificationConstants.NOTIFICATION_UID, msg.getString(NotificationConstants.NOTIFICATION_UID));
                    resultIntent.putExtra(NotificationConstants.ENTITY_TYPE, msg.getString(NotificationConstants.ENTITY_TYPE));
                    break;
            }
            resultIntent.putExtra(NotificationConstants.ENTITY_UID, msg.getString(NotificationConstants.ENTITY_UID));
            resultIntent.putExtra(NotificationConstants.CLICK_ACTION, msg.getString(NotificationConstants.CLICK_ACTION));
            resultIntent.putExtra(NotificationConstants.BODY, msg.getString(NotificationConstants.BODY));
        }

        PendingIntent resultPendingIntent;
        resultPendingIntent = (entityType.equals(NotificationConstants.CHAT_MESSAGE)) ? PendingIntent.getActivity(context, 100, resultIntent,
                PendingIntent.FLAG_CANCEL_CURRENT) : PendingIntent.getActivity(context, 200, resultIntent,
                PendingIntent.FLAG_CANCEL_CURRENT);
        Log.d(TAG, "and generateResultIntent exits, with intent: " + resultPendingIntent.toString());
        return resultPendingIntent;

    }

    private static void handleJoinRequestInDB(final String entityType, final String groupUid) {
        if (entityType.equals(GroupConstants.JREQ_APPROVED)) {
            // note : do it via overall fetch function, which will anyway just send 'changed since', else changed since
            // logic will be messed with by a single group fetch
            GroupService.getInstance().fetchGroupList(Schedulers.immediate()).subscribe();
            GroupService.getInstance().setHasGroups(true);
            RealmUtils.removeObjectFromDatabase(GroupJoinRequest.class, "groupUid", groupUid);
        }

        if (entityType.equals(GroupConstants.JREQ_DENIED)) {
            RealmUtils.removeObjectFromDatabase(GroupJoinRequest.class, "groupUid", groupUid);
        }

        if (entityType.equals(GroupConstants.JREQ_RECEIVED)) {
            GroupService.getInstance().fetchGroupJoinRequests(Schedulers.immediate()).subscribe();
        }
    }

    private static void handleChatMessages(Bundle bundle, Context context) {
        Log.d(TAG, "Received a chat message from server, looks like: " + bundle.getString(Constant.TITLE));
        Message message = new Message(bundle);

        if (!message.getType().equals("ping")) {
            String phoneNumber = RealmUtils.loadPreferencesFromDB().getMobileNumber();
            RealmUtils.saveDataToRealmSync(message);
            if (!phoneNumber.equals(message.getPhoneNumber())) {
                if (isAppIsInBackground(context)) {
                    relayNotification(bundle, context);
                } else {
                    EventBus.getDefault().post(new GroupChatEvent(message.getGroupUid(), bundle, message));
                }
            }
        }
    }

    public static Observable showNotification(final Bundle bundle, final Context context) {
        return Observable.create(new Observable.OnSubscribe<Boolean>() {
            @Override
            public void call(Subscriber<? super Boolean> subscriber) {
                relayNotification(bundle, context);
            }
        }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread());
    }


    private static Notification generateMultiLineNotification(PendingIntent resultPendingIntent, Context context, List<String> messages, String entityType, Bundle msg, int count) {
        NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context);
        NotificationCompat.InboxStyle style = new NotificationCompat.InboxStyle();
        String summaryText;
        Collections.reverse(messages);
        switch (entityType) {
            case NotificationConstants.CHAT_MESSAGE:
                summaryText = Integer.toString(count).concat(context.getString(R.string.messages_new));
                break;
            case NotificationConstants.JOIN_REQUEST:
                summaryText = Integer.toString(count).concat(context.getString(R.string.notifications_new));
                break;
            default:
                summaryText = Integer.toString(count).concat(context.getString(R.string.join_requests_new));
                break;
        }
        for (String message : messages) {
            style.addLine(message);
        }
        style.setBigContentTitle(context.getString(R.string.app_name));
        style.setSummaryText(summaryText);

        notification = mBuilder.setTicker(msg.getString(Constant.TITLE))
                .setWhen(System.currentTimeMillis())
                .setAutoCancel(true)
                .setContentTitle(msg.getString(Constant.TITLE))
                .setStyle(inboxStyle)
                .setContentIntent(resultPendingIntent)
                .setSmallIcon((Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                        ? R.drawable.ic_notification_icon : R.drawable.app_icon)
                .setDefaults(Notification.DEFAULT_ALL)
                .setSound(RingtoneManager.getActualDefaultRingtoneUri(context,
                        RingtoneManager.TYPE_NOTIFICATION))
                .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.drawable.app_icon))
                .setContentText(msg.getString(Constant.BODY))
                .setGroupSummary(true)
                .setStyle(style)
                .build();
        return notification;

    }

    public static void clearTaskNotifications(Context context) {
        if(notification !=null) notification.number = notification.number - notificationMessages.size();
        displayedNotificationCount = 0;
        notificationMessages.clear();
        NotificationManager nMgr = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
        nMgr.cancel(TASKS);
    }

    public static void clearChatNotifications(Context context) {
        notification.number = notification.number - chatMessages.size();
        displayedMessagesCount = 0;
        chatMessages.clear();
        NotificationManager nMgr = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
        nMgr.cancel(CHATS);
    }

    public static void clearJoinRequestNotifications(Context context) {

        if(notification !=null) notification.number = notification.number - joinRequests.size();
        displayedJoinRequestCount = 0;
        joinRequests.clear();
        NotificationManager nMgr = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
        nMgr.cancel(JOIN_REQUESTS);
    }



}