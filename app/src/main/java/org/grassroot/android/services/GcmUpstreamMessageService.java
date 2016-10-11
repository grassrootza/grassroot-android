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
import org.grassroot.android.models.exceptions.NoGcmException;
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

    public  final static int MAX_RETRIES = 3;
    private final static int BACKOFF_INITIAL_DELAY = 3000;
    private final static int MAX_BACKOFF_DELAY = 60 * 1000;
    private final static Random random = new Random();
    private final static String TAG = GcmUpstreamMessageService.class.getCanonicalName();

    public static Observable<String> sendMessage(final Message message, final Context context, final Scheduler observingThread) {
        return Observable.create(new Observable.OnSubscribe<String>() {
            @Override
            public void call(Subscriber<? super String> subscriber) {
                if (!NetworkUtils.isNetworkAvailable(context) && !NetworkUtils.isOnline()) {
                    throw new ApiCallException(NetworkUtils.CONNECT_ERROR);
                } else {
                    boolean messageSent = false;
                    int backoff = BACKOFF_INITIAL_DELAY;
                    String senderId = BuildConfig.FLAVOR.equals(Constant.PROD) ?
                            context.getString(R.string.prod_sender_id) : context.getString(R.string.staging_sender_id);

                    Bundle data = new Bundle();
                    data.putString("action", "CHAT");
                    data.putString("message", message.getText());
                    data.putString("phoneNumber", message.getPhoneNumber());
                    data.putString(GroupConstants.UID_FIELD, message.getGroupUid());
                    data.putString("time", Constant.isoDateTimeSDF.format(message.getTime()));

                    for (int i = 1; i <= MAX_RETRIES; i++) {
                        if (isMessageSent(message.getUid())) {
                            messageSent = true;
                            break;
                        }

                        context.sendBroadcast(new Intent("com.google.android.intent.action.GTALK_HEARTBEAT"));
                        context.sendBroadcast(new Intent("com.google.android.intent.action.MCS_HEARTBEAT"));
                        message.setNoAttempts(i);
                        RealmUtils.saveDataToRealmSync(message);
                        try {
                            GoogleCloudMessaging.getInstance(context).send(senderId, message.getUid(), 0, data);
                            Log.d(TAG, "Attempt no " + i);
                        } catch (IOException|NullPointerException e) {
                            throw new NoGcmException();
                        }
                        backoff = exponentialBackoffSleep(backoff);
                    }

                    if (messageSent) {
                        subscriber.onNext(NetworkUtils.SENT_UPSTREAM);
                        subscriber.onCompleted();
                    } else {
                        Log.e(TAG, "message still not sent, assuming network error .. ");
                        throw new ApiCallException(NetworkUtils.CONNECT_ERROR);
                    }
                }
            }
        }).subscribeOn(Schedulers.io()).observeOn(observingThread);
    }


    public static boolean isMessageSent(String uid) {
        Message message = RealmUtils.loadObjectFromDB(Message.class, "uid", uid);
        if (message != null) {
            if (message.getNoAttempts() == MAX_RETRIES && !message.isDelivered()){
                EventBus.getDefault().post(new MessageNotSentEvent(message));
            }
            return message.isSent();
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