package org.grassroot.android.services;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.gcm.GoogleCloudMessaging;

import org.grassroot.android.BuildConfig;
import org.grassroot.android.R;
import org.grassroot.android.events.MessageNotSentEvent;
import org.grassroot.android.interfaces.GroupConstants;
import org.grassroot.android.models.Message;
import org.grassroot.android.models.exceptions.ApiCallException;
import org.grassroot.android.utils.Constant;
import org.grassroot.android.utils.NetworkUtils;
import org.grassroot.android.utils.RealmUtils;
import org.greenrobot.eventbus.EventBus;

import java.io.IOException;
import java.util.Random;

import rx.Observable;
import rx.Scheduler;
import rx.Subscriber;
import rx.schedulers.Schedulers;

/**
 * Created by paballo on 2016/09/01.
 */
public class GcmUpstreamMessageService {

    private final static int MAX_RETRIES = 10;
    private final static int BACKOFF_INITIAL_DELAY = 3000;
    private final static int MAX_BACKOFF_DELAY = 60 * 1000;
    private static final Random random = new Random();
    private static final String TAG = GcmUpstreamMessageService.class.getCanonicalName();

    public static Observable<String>
    sendMessage(final Message message, final Context context, final Scheduler observingThread) {
        return Observable.create(new Observable.OnSubscribe<String>() {
            @Override
            public void call(Subscriber<? super String> subscriber) {
                if (!NetworkUtils.isNetworkAvailable(context) && !NetworkUtils.isOnline()) {
                    throw new ApiCallException(NetworkUtils.CONNECT_ERROR);
                } else {
                    int noAttempts = 0;
                    int backoff = BACKOFF_INITIAL_DELAY;
                    String senderId = BuildConfig.FLAVOR.equals(Constant.PROD) ?
                            context.getString(R.string.prod_sender_id) : context.getString(R.string.staging_sender_id);
                    do {
                        try {
                            noAttempts++;
                            Bundle data = new Bundle();
                            data.putString("action", "CHAT");
                            data.putString("message", message.getText());
                            data.putString("phoneNumber", message.getPhoneNumber());
                            data.putString(GroupConstants.UID_FIELD, message.getGroupUid());
                            data.putString("time", Constant.isoDateTimeSDF.format(message.getTime()));
                            context.sendBroadcast(new Intent("com.google.android.intent.action.GTALK_HEARTBEAT"));
                            context.sendBroadcast(new Intent("com.google.android.intent.action.MCS_HEARTBEAT"));
                            message.setNoAttempts(noAttempts);
                            RealmUtils.saveDataToRealmSync(message);
                            GoogleCloudMessaging.getInstance(context).send(senderId, message.getUid(), 0, data);
                            Log.d(TAG, "Attempt no" + noAttempts);

                        } catch (IOException e) {
                            Log.d(TAG, "Failed to send message");
                        }
                        backoff = exponentialBackoffSleep(backoff);
                    } while ((!isMessageSent(message.getUid()) && noAttempts < MAX_RETRIES));
                }
            }
        }).subscribeOn(Schedulers.io()).observeOn(observingThread);
    }


    public static boolean isMessageSent(String uid) {
        Message message = RealmUtils.loadObjectFromDB(Message.class, "uid", uid);
        if (message != null) {
            if(message.getNoAttempts() == MAX_RETRIES && !message.isDelivered()){
                EventBus.getDefault().post(new MessageNotSentEvent(message));
            }
            return message.isDelivered();
        }
        return false;
    }


    private static int exponentialBackoffSleep(int backoff) {
        try {
            int sleepTime = backoff / 2 + random.nextInt(backoff);
            Thread.sleep(sleepTime);
            if (2 * backoff < MAX_BACKOFF_DELAY) {
                backoff *= 2;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        return backoff;
    }



}