package org.grassroot.android.services;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.grassroot.android.BuildConfig;
import org.grassroot.android.R;
import org.grassroot.android.events.MessageNotSentEvent;
import org.grassroot.android.interfaces.GroupConstants;
import org.grassroot.android.models.Message;
import org.grassroot.android.models.exceptions.ApiCallException;
import org.grassroot.android.models.exceptions.NoGcmException;
import org.grassroot.android.models.responses.GenericResponse;
import org.grassroot.android.utils.AnnotationExclusionStrategy;
import org.grassroot.android.utils.Constant;
import org.grassroot.android.utils.ErrorUtils;
import org.grassroot.android.utils.MqttConnectionManager;
import org.grassroot.android.utils.NetworkUtils;
import org.grassroot.android.utils.RealmUtils;
import org.greenrobot.eventbus.EventBus;

import java.io.IOException;
import java.util.Random;

import retrofit2.Response;
import rx.Observable;
import rx.Scheduler;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Created by paballo on 2016/09/01.
 */
public class GroupChatService {

    private final static String TAG = GroupChatService.class.getSimpleName();

    private static GroupChatService instance = null;

    private String phoneNumber;
    private String apiToken;
    private String gcmRegistrationId;

    public  final static int MAX_RETRIES = 3;
    private final static int BACKOFF_INITIAL_DELAY = 3000;
    private final static int MAX_BACKOFF_DELAY = 60 * 1000;
    private final static Random random = new Random();


    protected GroupChatService() {
        // for singleton
    }

    public static GroupChatService getInstance() {
        GroupChatService methodInstance = instance;
        if (methodInstance == null) {
            synchronized (GroupChatService.class) {
                methodInstance = instance;
                if (methodInstance == null) {
                    instance = methodInstance = new GroupChatService();
                }
            }
        }
        return methodInstance;
    }

    public void setUserDetails() {
        phoneNumber = RealmUtils.loadPreferencesFromDB().getMobileNumber();
        apiToken = RealmUtils.loadPreferencesFromDB().getToken();
        gcmRegistrationId = RealmUtils.loadPreferencesFromDB().getGcmRegistrationId();

    }

    public Observable<String> sendMessageViaGR(final Message message) {
        return Observable.create(new Observable.OnSubscribe<String>() {
            @Override
            public void call(Subscriber<? super String> subscriber) {
                if (!NetworkUtils.isOnline()) {
                    throw new ApiCallException(NetworkUtils.CONNECT_ERROR);
                } else {
                    Log.e(TAG, "okay sending via HTTP ... ");
                    try {
                        if(phoneNumber == null) setUserDetails();
                        Response<GenericResponse> response = GrassrootRestService.getInstance().getApi()
                            .sendChatMessage(phoneNumber, apiToken, message.getGroupUid(), message.getText(),
                                message.getUid(), gcmRegistrationId).execute();

                        if (response.isSuccessful()) {
                            message.setSending(false);
                            message.setSent(true);
                            RealmUtils.saveDataToRealmSync(message);
                            subscriber.onNext(NetworkUtils.SENT_UPSTREAM);
                            subscriber.onCompleted();
                        } else {
                            throw new ApiCallException(NetworkUtils.SERVER_ERROR, ErrorUtils.getRestMessage(response.errorBody()));
                        }
                    } catch (IOException e) {
                        throw new ApiCallException(NetworkUtils.CONNECT_ERROR);
                    }

                }
            }
        }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread());
    }


    public Observable<String> sendMessageViaMQTT(final Message message) {
        return Observable.create(new Observable.OnSubscribe<String>() {
            @Override
            public void call(Subscriber<? super String> subscriber) {
                if (!NetworkUtils.isOnline()) {
                    throw new ApiCallException(NetworkUtils.CONNECT_ERROR);
                } else {
                    Log.e(TAG, "okay sending via MQTT ... ");
                    try {
                        if(phoneNumber == null) setUserDetails();
                        if(isCommand(message)){
                            MqttConnectionManager.getInstance(ApplicationLoader.applicationContext).sendMessage("Grassroot", message);
                        }else{
                            MqttConnectionManager.getInstance(ApplicationLoader.applicationContext).sendMessage(message.getGroupUid(), message);
                        }
                    } catch (Exception e) {
                        throw new ApiCallException(NetworkUtils.CONNECT_ERROR);
                    }

                }
            }
        }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread());
    }



    public static boolean isMessageSent(String uid) {
        Message message = RealmUtils.loadObjectFromDB(Message.class, "uid", uid);
        if (message != null) {
            if (message.getNoAttempts() == MAX_RETRIES && !message.isSent()){
                EventBus.getDefault().post(new MessageNotSentEvent(message));
            }
            return message.isSent();
        }
        return false;
    }

    private static boolean isCommand(Message message){
        return message.getText().startsWith("/");
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