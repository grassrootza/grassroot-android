package org.grassroot.android.services;

import android.util.Log;

import org.grassroot.android.models.Message;
import org.grassroot.android.models.exceptions.ApiCallException;
import org.grassroot.android.utils.MqttConnectionManager;
import org.grassroot.android.utils.NetworkUtils;

import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Created by paballo on 2016/09/01.
 */
public class GroupChatService {

    private final static String TAG = GroupChatService.class.getSimpleName();

    private static GroupChatService instance = null;
    public  final static int MAX_RETRIES = 3;


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

    public Observable<String> sendMessageViaMQTT(final Message message) {
        return Observable.create(new Observable.OnSubscribe<String>() {
            @Override
            public void call(Subscriber<? super String> subscriber) {
                if (!NetworkUtils.isOnline()) {
                    throw new ApiCallException(NetworkUtils.CONNECT_ERROR);
                } else {
                    Log.e(TAG, "sending message via MQTT ... ");
                    try {
                        if (isCommand(message)){
                            Log.e(TAG, "sending a command");
                            MqttConnectionManager.getInstance().sendMessage("Grassroot", message);
                        } else {
                            MqttConnectionManager.getInstance().sendMessage(message.getGroupUid(), message);
                        }
                    } catch (Exception e) {
                        throw new ApiCallException(NetworkUtils.CONNECT_ERROR);
                    }

                }
            }
        }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread());
    }

    private static boolean isCommand(Message message){
        return message.getText().startsWith("/");
    }

}