package org.grassroot.android.services;

import android.os.Bundle;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.grassroot.android.events.GroupChatErrorEvent;
import org.grassroot.android.events.GroupChatEvent;
import org.grassroot.android.events.GroupChatMessageReadEvent;
import org.grassroot.android.interfaces.GroupConstants;
import org.grassroot.android.interfaces.NotificationConstants;
import org.grassroot.android.models.Group;
import org.grassroot.android.models.Message;
import org.grassroot.android.models.exceptions.ApiCallException;
import org.grassroot.android.models.exceptions.GroupChatException;
import org.grassroot.android.models.helpers.RealmString;
import org.grassroot.android.utils.Constant;
import org.grassroot.android.utils.NetworkUtils;
import org.grassroot.android.utils.RealmUtils;
import org.grassroot.android.utils.Utilities;
import org.grassroot.android.utils.chat.AnnotationExclusionStrategy;
import org.grassroot.android.utils.chat.RealmStringDeserializer;
import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import io.realm.RealmList;

import static org.grassroot.android.services.ApplicationLoader.isAppIsInBackground;
import static org.grassroot.android.services.GcmListenerService.handleNotification;

/**
 * Created by paballo on 2016/11/01.
 */

public class MqttConnectionManager implements IMqttActionListener, MqttCallback {

    private static final String TAG = MqttConnectionManager.class.getSimpleName();

    private static MqttConnectionManager instance = null;

    private static final String CONNECTED = "connected";
    private static final String ERROR = "error";
    private static final String DISCONNECTED = "disconnected";

    private static final String CHAT_SENT = "chat_sent";

    private Gson gson;
    private String mqqtConnectionStatus;
    private MqttAndroidClient mqttAndroidClient;

    private MqttConnectionManager() {
        GsonBuilder builder = new GsonBuilder();

        builder.setExclusionStrategies(new AnnotationExclusionStrategy());
        builder.registerTypeAdapter(
                new TypeToken<RealmList<RealmString>>() {}.getType(),
                new RealmStringDeserializer());
        builder.setDateFormat("yyyy-MM-dd'T'HH:mm:ss");

        this.gson = builder.create();
    }

    public static MqttConnectionManager getInstance() {
        MqttConnectionManager methodInstance = instance;
        if (methodInstance == null) {
            synchronized (MqttConnectionManager.class) {
                methodInstance = instance;
                if (methodInstance == null) {
                    instance = methodInstance = new MqttConnectionManager();
                }
            }
        }
        return methodInstance;
    }

    public boolean isConnected() {
        return CONNECTED.equals(mqqtConnectionStatus) || (mqttAndroidClient != null && mqttAndroidClient.isConnected());
    }

    public void connect() {
        Log.e(TAG, "connecting to mqtt broker at address: " + Constant.brokerUrl);

        final MqttConnectOptions options = new MqttConnectOptions();
        final String clientId = RealmUtils.loadPreferencesFromDB().getMobileNumber();
        options.setCleanSession(false);
        options.setKeepAliveInterval(240);
        options.setConnectionTimeout(30);
        options.setAutomaticReconnect(true);

        try {
            Log.e(TAG, "trying to connect ...");
            if (mqttAndroidClient == null) {
                mqttAndroidClient = new MqttAndroidClient(ApplicationLoader.applicationContext,
                        Constant.brokerUrl, clientId);
            }

            Log.e(TAG, "set up client");

            mqttAndroidClient.setCallback(this);
            if (!mqttAndroidClient.isConnected()) {
                Log.e(TAG, "not connected, trying to connect ...");
                mqttAndroidClient.connect(options, null, new IMqttActionListener() {
                    @Override
                    public void onSuccess(IMqttToken asyncActionToken) {
                        Log.e(TAG, "success in mqtt connection");
                        mqqtConnectionStatus = CONNECTED;
                        subscribeToAllGroups();
                        subscribeToUserTopic();
                    }

                    @Override
                    public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                        Log.e(TAG, "back in onfailure event ... calling handleFailure");
                        mqqtConnectionStatus = ERROR;
                        handleFailure(GroupChatErrorEvent.CONNECT_ERROR, asyncActionToken, exception);
                    }
                });
            }
        } catch (MqttException e) {
            Log.e(TAG, e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void disconnect() {
        if (mqttAndroidClient != null && mqttAndroidClient.isConnected()) {
            try {
                mqttAndroidClient.disconnect();
                mqttAndroidClient = null;
            } catch (MqttException e) {
                Log.e(TAG, e.getMessage());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void subscribeToAllGroups() {
        List<String> groupUids = RealmUtils.loadGroupUidsSync();
        int[] qosList = new int[groupUids.size()];
        Arrays.fill(qosList, 1);
        subscribeToTopics(groupUids.toArray(new String[0]), qosList);
    }

    public void subscribeToGroups(List<Group> groups) {
        List<String> groupUids = new ArrayList<>();
        List<Integer> qosList = new ArrayList<>();

        for (Group g : groups) {
            groupUids.add(g.getGroupUid());
            qosList.add(1);
        }

        subscribeToTopics(groupUids.toArray(new String[groupUids.size()]),
                Utilities.convertIntegerArrayToPrimitiveArray(qosList));
    }

    private void subscribeToUserTopic() {
        subscribeToTopics(new String[] { RealmUtils.loadPreferencesFromDB().getMobileNumber() },
                new int[] { 1 });
    }

    private void subscribeToTopics(String[] topics, int[] qos) {
        if (mqttAndroidClient != null && mqttAndroidClient.isConnected()) {
            try {
                IMqttToken token = mqttAndroidClient.subscribe(topics, qos);
                token.setActionCallback(this);
            } catch (MqttException e) {
                Log.e(TAG, e.getMessage());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void unsubscribeFromTopic(String topic) {
        if (mqttAndroidClient != null && mqttAndroidClient.isConnected()) {
            try {
                IMqttToken token = mqttAndroidClient.unsubscribe(topic);
                token.setActionCallback(this);
            } catch (MqttException e) {
                Log.e(TAG, e.getMessage());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void unsubscribeAllAndDisconnect(final List<String> topicsToUnsubscribe) {
        if (mqttAndroidClient != null && mqttAndroidClient.isConnected()) {
            try {
                topicsToUnsubscribe.add(RealmUtils.loadPreferencesFromDB().getMobileNumber());
                IMqttToken token = mqttAndroidClient.unsubscribe(topicsToUnsubscribe.toArray(new String[0]));
                token.setActionCallback(new IMqttActionListener() {
                    @Override
                    public void onSuccess(IMqttToken asyncActionToken) {
                        Log.e(TAG, "done! all groups unsubscribed");
                        disconnect();
                    }

                    @Override
                    public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                        disconnect();
                    }
                });
            } catch (MqttException e) {
                disconnect();
                Log.e(TAG, e.getMessage());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public Observable<String> sendMessageInBackground(final Message message) {
        return Observable.create(new ObservableOnSubscribe<String>() {
            @Override
            public void subscribe(final ObservableEmitter<String> subscriber) {
                if (!NetworkUtils.isOnline()) {
                    throw new ApiCallException(NetworkUtils.CONNECT_ERROR);
                } else {
                    final String topic = isCommand(message) ? "Grassroot" : message.getGroupUid();
                    Log.d(TAG, "publishing to topic " + topic);
                    String jsonMessage = gson.toJson(message);
                    MqttMessage mqttMessage = new MqttMessage(jsonMessage.getBytes());
                    mqttMessage.setRetained(false);
                    mqttMessage.setQos(1);
                    try {
                        mqttAndroidClient.publish(topic, mqttMessage, null, new IMqttActionListener() {
                            @Override
                            public void onSuccess(IMqttToken asyncActionToken) {
                                subscriber.onNext(CHAT_SENT);
                            }

                            // note : as currently implemented, this returns with no real information, hence handle in broadcast receiver
                            @Override
                            public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                                Log.e(TAG, "error inside send message token");
                                subscriber.onError(new GroupChatException(GroupChatException.MQTT_EXCEPTION, NetworkUtils.CONNECT_ERROR));
                            }
                        });
                    } catch (MqttException e) {
                        throw new GroupChatException(GroupChatException.MQTT_EXCEPTION, e.getMessage());
                    } catch (Exception e) {
                        throw new GroupChatException(GroupChatException.MISC_EXCEPTION, e.getMessage());
                    }
                }
            }
        }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread());
    }

    private static boolean isCommand(Message message){
        return message.getText().startsWith("/");
    }

    @Override
    public void connectionLost(Throwable cause) {
        mqqtConnectionStatus = DISCONNECTED;
        Log.d(TAG, "Disconnected from mqtt broker");

    }

    @Override
    public void messageArrived(String topic, MqttMessage mqttMessage) {
        Log.e(TAG, "received message from topic " + topic + ", looks like : " + mqttMessage.toString());
        String phoneNumber = RealmUtils.loadPreferencesFromDB().getMobileNumber();
        try {
            Message message = gson.fromJson(mqttMessage.toString(), Message.class);
            Log.d(TAG, "message " + message.toString());

            message.setDelivered(true);
            Bundle bundle = createBundleFromMessage(message);
            switch (message.getType()) {
                case "ping":
                    break;
                case "sync":
                    NetworkUtils.syncAndStartTasks(ApplicationLoader.applicationContext, true, true);
                    break;
                case "update_read_status":
                    Message existingMessage = RealmUtils.hasMessage(message.getUid()) ?
                            RealmUtils.loadMessage(message.getUid()) : message;
                    existingMessage.setRead(true);
                    RealmUtils.saveDataToRealmSync(existingMessage);
                    EventBus.getDefault().post(new GroupChatMessageReadEvent(existingMessage));
                    break;
                default:
                    message.setSeen(RealmUtils.hasMessage(message.getUid())); // so a message we sent is not marked as seen
                    RealmUtils.saveDataToRealmSync(message);
                    if (isAppIsInBackground(ApplicationLoader.applicationContext) && !phoneNumber.equals(message.getPhoneNumber())) {
                        handleNotification(bundle);
                    } else {
                        Log.e(TAG, "received a message, relaying it ...");
                        EventBus.getDefault().post(new GroupChatEvent(message.getGroupUid(), bundle, message));
                    }
            }
        } catch (JsonParseException e) {
            Log.e(TAG, "JSON parse error! " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        try {
            Message message = gson.fromJson(token.getMessage().toString(), Message.class);
            message.setSent(true);
            message.setDelivered(true);
            RealmUtils.saveDataToRealmSync(message);
            EventBus.getDefault().post(new GroupChatEvent(message.getGroupUid(),
                    createBundleFromMessage(message), message));
        } catch (MqttException e) {
            Log.e(TAG, e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Bundle createBundleFromMessage(Message message) {
        Bundle bundle = new Bundle();
        bundle.putString("messageUid", message.getUid());
        bundle.putString("phone_number", message.getPhoneNumber());
        bundle.putString(GroupConstants.NAME_FIELD, message.getGroupName());
        bundle.putString(Constant.TITLE, message.getDisplayName());
        bundle.putString(GroupConstants.UID_FIELD, message.getGroupUid());
        bundle.putString(Constant.BODY, message.getText());
        bundle.putString("userUid", message.getUserUid());
        bundle.putString(NotificationConstants.CLICK_ACTION, NotificationConstants.CHAT_MESSAGE);
        bundle.putString(NotificationConstants.ENTITY_TYPE, NotificationConstants.CHAT_MESSAGE);

        return bundle;
    }

    @Override
    public void onSuccess(IMqttToken asyncActionToken) {
        mqqtConnectionStatus = CONNECTED;
    }

    @Override
    public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
        mqqtConnectionStatus = ERROR;
        handleFailure(GroupChatErrorEvent.MISC_ERROR, asyncActionToken, exception);
    }

    private void handleFailure(final String type, IMqttToken asyncActionToken, Throwable exception) {
        final String description = exception == null ? exception.getMessage() :
                asyncActionToken != null && asyncActionToken.getException() != null
                        ? asyncActionToken.getException().getMessage() : "No message included";
        EventBus.getDefault().post(new GroupChatErrorEvent(type, description));
    }


}
