package org.grassroot.android.services;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.media.RingtoneManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.util.Log;

import org.grassroot.android.R;
import org.grassroot.android.activities.GroupTasksActivity;
import org.grassroot.android.activities.JoinRequestNoticeActivity;
import org.grassroot.android.activities.MultiMessageNotificationActivity;
import org.grassroot.android.activities.ViewTaskActivity;
import org.grassroot.android.events.JoinRequestEvent;
import org.grassroot.android.events.NotificationCountChangedEvent;
import org.grassroot.android.events.NotificationEvent;
import org.grassroot.android.interfaces.GroupConstants;
import org.grassroot.android.models.GroupJoinRequest;
import org.grassroot.android.models.Message;
import org.grassroot.android.models.PreferenceObject;
import org.grassroot.android.models.responses.GenericResponse;
import org.grassroot.android.utils.Constant;
import org.grassroot.android.utils.RealmUtils;
import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

import static org.grassroot.android.interfaces.NotificationConstants.BODY;
import static org.grassroot.android.interfaces.NotificationConstants.CHAT_LIST;
import static org.grassroot.android.interfaces.NotificationConstants.CHAT_MESSAGE;
import static org.grassroot.android.interfaces.NotificationConstants.CLICK_ACTION;
import static org.grassroot.android.interfaces.NotificationConstants.ENTITY_TYPE;
import static org.grassroot.android.interfaces.NotificationConstants.ENTITY_UID;
import static org.grassroot.android.interfaces.NotificationConstants.JOIN_REQUEST;
import static org.grassroot.android.interfaces.NotificationConstants.JOIN_REQUEST_LIST;
import static org.grassroot.android.interfaces.NotificationConstants.NOTIFICATION_LIST;
import static org.grassroot.android.interfaces.NotificationConstants.NOTIFICATION_UID;
import static org.grassroot.android.interfaces.NotificationConstants.TASK_CREATED;

/**
 * Created by paballo on 2016/05/09.
 */
public class GcmListenerService extends com.google.android.gms.gcm.GcmListenerService {

    private static final String TAG = GcmListenerService.class.getSimpleName();

    private static final int TASKS = 100;
    private static final int CHATS = 200;
    private static final int JOIN_REQUESTS = 300;

    private static final List<String> notificationMessages = new ArrayList<>();
    private static final List<String> joinRequests = new ArrayList<>();

    private static final Map<String, ArrayList<String>> chatMessagesMap = new LinkedHashMap<>(); // so we can remove from group once viewed

    private static int displayedTasksCount = 0;
    private static int displayedChatsCount = 0;
    private static int displayedJoinRequestCount = 0;
    private static Notification notification;

    @Override
    public void onMessageReceived(String from, Bundle data) {
        incrementNotificationCounter();

        // this method is only called if app running, hence ignore chat messages
        if (!CHAT_MESSAGE.equals(data.get(ENTITY_TYPE))) {
            handleNotification(data);
        }

        if (!MqttConnectionManager.getInstance().isConnected()) {
            MqttConnectionManager.getInstance().connect();
        }
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

    public static void handleNotification(Bundle msg) {
        Context context = ApplicationLoader.applicationContext;

        final String notificationUid = msg.getString(NOTIFICATION_UID);
        final String entityType = msg.getString(ENTITY_TYPE);

        NotificationManager manager = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
        int notificationId;
        boolean multiNotifications;

        if (CHAT_MESSAGE.equals(entityType)) {
            addChatToList(msg);
            notificationId = CHATS;
            multiNotifications = displayedChatsCount > 1;
        } else {
            notificationMessages.add(msg.getString(Constant.BODY));
            if (JOIN_REQUEST.equals(entityType)) {
                displayedJoinRequestCount++;
                notificationId = JOIN_REQUESTS;
                multiNotifications = displayedJoinRequestCount > 1;
            } else {
                displayedTasksCount++;
                notificationId = TASKS;
                multiNotifications = displayedTasksCount > 1;
            }
        }

        Notification notification = multiNotifications ? generateMultiLineNotification(msg) : singleChatPerVersion(msg);

        manager.cancel(notificationId);
        manager.notify(notificationId, notification);
        notifyServerRead(notificationUid);
    }

    private static void addChatToList(Bundle message) {
        final String chatTitle = message.getString(Constant.TITLE) + ": " + message.getString(Constant.BODY);
        final String groupUid = message.getString(GroupConstants.UID_FIELD);
        if (chatMessagesMap.containsKey(groupUid)) {
            ArrayList<String> currentMessages = chatMessagesMap.get(groupUid);
            currentMessages.add(chatTitle);
        } else {
            ArrayList<String> groupMessages = new ArrayList<>();
            groupMessages.add(chatTitle);
            chatMessagesMap.put(groupUid, groupMessages);
        }
        displayedChatsCount++;
    }

    private static List<String> flattenChatMessages() {
        if (chatMessagesMap.isEmpty()) {
            return new ArrayList<>();
        } else if (chatMessagesMap.size() == 1) {
            return chatMessagesMap.entrySet().iterator().next().getValue();
        } else {
            List<String> chats = new ArrayList<>();
            for (Map.Entry<String, ArrayList<String>> entry : chatMessagesMap.entrySet()) {
                for (String msg : entry.getValue()) {
                    if (!TextUtils.isEmpty(msg)) {
                        chats.add(msg);
                    }
                }
            }
            return chats;
        }
    }

    private static Notification singleChatPerVersion(Bundle msg) {
        return Build.VERSION.SDK_INT >= 21 ? lollipopBranch(msg) : kitkatBranch(msg);
    }

    private static Notification lollipopBranch(Bundle msg) {
        Context context = ApplicationLoader.applicationContext;
        final NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context);
        return mBuilder
                .setTicker(msg.getString(Constant.TITLE))
                .setWhen(System.currentTimeMillis())
                .setAutoCancel(true)
                .setContentTitle(msg.getString(Constant.TITLE))
                .setContentIntent(generateResultIntent(msg, false))
                .setSmallIcon((Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                        ? R.drawable.ic_notification_icon : R.drawable.app_icon)
                .setDefaults(Notification.DEFAULT_ALL)
                .setSound(RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_NOTIFICATION))
                .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.drawable.app_icon))
                .setContentText(msg.getString(Constant.BODY))
                .setGroupSummary(true)
                .build();
    }

    private static Notification kitkatBranch(Bundle msg) {
        Context context = ApplicationLoader.applicationContext;
        final NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context);
        return mBuilder.setContentTitle(msg.getString(Constant.TITLE))
                    .setWhen(System.currentTimeMillis())
                    .setContentText(msg.getString(Constant.BODY))
                    .setAutoCancel(true)
                    .setSmallIcon(R.drawable.app_icon)
                    .setContentIntent(generateResultIntent(msg, false))
                    .setDefaults(Notification.DEFAULT_ALL)
                    .setSound(RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_NOTIFICATION))
                    .setGroupSummary(true)
                    .build();
    }

    private static Notification generateMultiLineNotification(Bundle msg) {
        Context context = ApplicationLoader.applicationContext;
        NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context);
        NotificationCompat.InboxStyle style = new NotificationCompat.InboxStyle();

        String summaryText;
        List<String> messages;

        final String entityType = msg.containsKey(ENTITY_TYPE) ? msg.getString(ENTITY_TYPE) : CHAT_MESSAGE;

        switch (entityType) {
            case CHAT_MESSAGE:
                summaryText = Integer.toString(displayedChatsCount).concat(context.getString(R.string.messages_new));
                messages = flattenChatMessages();
                break;
            case JOIN_REQUEST:
                summaryText = Integer.toString(displayedJoinRequestCount).concat(context.getString(R.string.join_requests_new));
                messages = notificationMessages;
                break;
            default:
                summaryText = Integer.toString(displayedTasksCount).concat(context.getString(R.string.notifications_new));
                messages = notificationMessages;
                break;
        }
        Collections.reverse(messages);

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
                .setContentIntent(generateResultIntent(msg, true))
                .setSmallIcon((Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) ? R.drawable.ic_notification_icon : R.drawable.app_icon)
                .setDefaults(Notification.DEFAULT_ALL)
                .setSound(RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_NOTIFICATION))
                .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.drawable.app_icon))
                .setContentText(msg.getString(Constant.BODY))
                .setGroupSummary(true)
                .setStyle(style)
                .build();

        return notification;
    }

    private static PendingIntent generateResultIntent(Bundle msg, boolean multiMessage) {
        final String entityType = msg.containsKey(ENTITY_TYPE) ? msg.getString(ENTITY_TYPE) : CHAT_MESSAGE;

        if (CHAT_MESSAGE.equals(entityType)) {
            return multiMessage ? multiChatList(msg) : openGroupChat(msg.getString(GroupConstants.UID_FIELD));
        } else {
            Intent resultIntent = JOIN_REQUEST.equals(entityType) ? joinRequestIntent(msg, multiMessage) : otherNotificationIntent(msg, multiMessage);
            return PendingIntent.getActivity(ApplicationLoader.applicationContext, 200, resultIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        }
    }

    private static Intent joinRequestIntent(Bundle msg, boolean multiple) {
        Intent resultIntent = new Intent(ApplicationLoader.applicationContext,
                multiple ? MultiMessageNotificationActivity.class : JoinRequestNoticeActivity.class);
        final String groupUid = msg.getString("group");
        handleJoinRequestInDB(JOIN_REQUEST, groupUid);
        if (multiple) {
            resultIntent.putExtra(CLICK_ACTION, JOIN_REQUEST_LIST);
        } else {
            resultIntent.putExtra(ENTITY_UID, msg.getString(ENTITY_UID));
            resultIntent.putExtra(CLICK_ACTION, msg.getString(CLICK_ACTION));
            resultIntent.putExtra(BODY, msg.getString(BODY));
        }
        EventBus.getDefault().post(new JoinRequestEvent(TAG));
        return resultIntent;
    }

    private static Intent otherNotificationIntent(Bundle msg, boolean multiple) {
        Intent resultIntent = new Intent(ApplicationLoader.applicationContext,
                multiple ? MultiMessageNotificationActivity.class : ViewTaskActivity.class);
        createChatMessageIfFromTask(msg);
        if (multiple) {
            resultIntent.putExtra(CLICK_ACTION, NOTIFICATION_LIST);
        } else {
            resultIntent.putExtra(NOTIFICATION_UID, msg.getString(NOTIFICATION_UID));
            resultIntent.putExtra(ENTITY_TYPE, msg.getString(ENTITY_TYPE));
            resultIntent.putExtra(ENTITY_UID, msg.getString(ENTITY_UID));
            resultIntent.putExtra(CLICK_ACTION, msg.getString(CLICK_ACTION));
            resultIntent.putExtra(BODY, msg.getString(BODY));
        }
        EventBus.getDefault().post(new NotificationEvent());
        return resultIntent;
    }

    private static PendingIntent openGroupChat(final String groupUid) {
        Intent resultIntent = new Intent(ApplicationLoader.applicationContext, GroupTasksActivity.class);
        resultIntent.putExtra(GroupConstants.UID_FIELD, groupUid);
        resultIntent.putExtra(GroupConstants.GROUP_OPEN_PAGE, GroupConstants.OPEN_ON_CHAT);
        return PendingIntent.getActivity(ApplicationLoader.applicationContext, 100, resultIntent, PendingIntent.FLAG_CANCEL_CURRENT);
    }

    private static PendingIntent multiChatList(Bundle msg) {
        if (chatMessagesMap.size() == 1) {
            return openGroupChat(chatMessagesMap.entrySet().iterator().next().getKey());
        } else {
            Intent resultIntent = new Intent(ApplicationLoader.applicationContext, MultiMessageNotificationActivity.class);
            resultIntent.putExtra(CLICK_ACTION, CHAT_LIST);
            resultIntent.putExtra(GroupConstants.UID_FIELD, msg != null ? msg.getString(GroupConstants.UID_FIELD) : "");
            resultIntent.putExtra(GroupConstants.NAME_FIELD, msg != null ? msg.getString(GroupConstants.NAME_FIELD) : "Chats");
            return PendingIntent.getActivity(ApplicationLoader.applicationContext, 100, resultIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        }
    }

    public static void clearGroupsChatNotifications(final String groupUid) {
        if (chatMessagesMap.containsKey(groupUid)) {
            displayedChatsCount = displayedChatsCount - chatMessagesMap.get(groupUid).size();
            chatMessagesMap.remove(groupUid);

            Log.e(TAG, "removed group chats, remaining = " + displayedChatsCount);

            NotificationManager manager = (NotificationManager) ApplicationLoader.applicationContext
                    .getSystemService(NOTIFICATION_SERVICE);
            Bundle replacement = new Bundle();
            replacement.putString(ENTITY_TYPE, CHAT_MESSAGE);

            if (chatMessagesMap.isEmpty()) {
                manager.cancel(CHATS);
            } else if (chatMessagesMap.size() == 1) {
                Map.Entry<String, ArrayList<String>> entry = chatMessagesMap.entrySet().iterator().next();
                if (entry.getValue().isEmpty()) {
                    manager.cancel(CHATS);
                } else {
                    final String title = RealmUtils.groupExistsInDB(entry.getKey()) ?
                            RealmUtils.loadGroupFromDB(entry.getKey()).getGroupName() :
                            ApplicationLoader.applicationContext.getString(R.string.app_name);
                    replacement.putString(Constant.TITLE, title);
                    replacement.putString(GroupConstants.UID_FIELD, entry.getKey());

                    Notification notification;
                    if (entry.getValue().size() == 1) {
                        replacement.putString(Constant.BODY, entry.getValue().get(0));
                        notification = singleChatPerVersion(replacement);
                    } else {
                        replacement.putString(Constant.BODY, entry.getValue().get(0)); // this is often not usedd
                        notification = generateMultiLineNotification(replacement);
                    }
                    manager.cancel(CHATS);
                    manager.notify(CHATS, notification);
                }
            } else {
                replacement.putString(Constant.TITLE, ApplicationLoader.applicationContext.getString(R.string.app_name));
                replacement.putString(Constant.BODY, flattenChatMessages().get(0));
                Notification notification = generateMultiLineNotification(replacement);
                manager.cancel(CHATS);
                manager.notify(CHATS, notification);
            }
        }

    }

    // note : get group via overall fetch function, which will anyway just send 'changed since', else changed since
    // logic will be messed with by a single group fetch
    private static void handleJoinRequestInDB(final String entityType, final String groupUid) {
        if (entityType.equals(GroupConstants.JREQ_APPROVED)) {
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

    private static void createChatMessageIfFromTask(Bundle bundle) {
        String clickAction = bundle.getString(CLICK_ACTION);
        if (clickAction != null && clickAction.equals(TASK_CREATED)) {
            final String groupUid = bundle.getString(GroupConstants.UID_FIELD);
            final String text = bundle.getString(BODY);
            Message message = new Message(groupUid, UUID.randomUUID().toString(), text, null);
            message.setToKeep(true);
            message.setSeen(true);
            RealmUtils.saveDataToRealmSync(message);
           // EventBus.getDefault().post(new GroupChatEvent(groupUid, bundle, message));
        }
    }

    public static Observable showNotification(final Bundle bundle) {
        return Observable.create(new Observable.OnSubscribe<Boolean>() {
            @Override
            public void call(Subscriber<? super Boolean> subscriber) {
                handleNotification(bundle);
            }
        }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread());
    }

    public static void clearTaskNotifications(Context context) {
        if (!(notification == null))
            notification.number = notification.number - notificationMessages.size();
        displayedTasksCount = 0;
        notificationMessages.clear();
        NotificationManager nMgr = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
        nMgr.cancel(TASKS);
    }

    public static void clearChatNotifications(Context context) {
        if (!(notification == null))
            notification.number = notification.number - chatMessagesMap.size();
        displayedChatsCount = 0;
        chatMessagesMap.clear();
        NotificationManager nMgr = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
        nMgr.cancel(CHATS);
    }

    public static void clearJoinRequestNotifications(Context context) {
        if (!(notification == null))
            notification.number = notification.number - joinRequests.size();
        displayedJoinRequestCount = 0;
        joinRequests.clear();
        NotificationManager nMgr = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
        nMgr.cancel(JOIN_REQUESTS);
    }

    private static void notifyServerRead(final String notificationUid) {
        GrassrootRestService.getInstance().getApi()
                .updateRead(RealmUtils.loadPreferencesFromDB().getMobileNumber(), RealmUtils.loadPreferencesFromDB().getToken(), notificationUid)
                .enqueue(new Callback<GenericResponse>() {
                    @Override
                    public void onResponse(Call<GenericResponse> call, Response<GenericResponse> response) {
                        Log.d(TAG, response.isSuccessful() ? "Set message as read!" : "Failed: " + response.errorBody());
                    }

                    @Override
                    public void onFailure(Call<GenericResponse> call, Throwable t) {
                        Log.e(TAG, "Something went wrong updating message: " + t.getMessage());
                    }
                });
    }

}