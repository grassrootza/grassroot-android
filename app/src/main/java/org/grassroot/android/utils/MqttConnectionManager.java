package org.grassroot.android.utils;

import android.os.Bundle;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.grassroot.android.events.GroupChatEvent;
import org.grassroot.android.events.GroupChatMessageReadEvent;
import org.grassroot.android.interfaces.GroupConstants;
import org.grassroot.android.interfaces.NotificationConstants;
import org.grassroot.android.models.Group;
import org.grassroot.android.models.Message;
import org.grassroot.android.models.RealmString;
import org.grassroot.android.services.ApplicationLoader;
import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.realm.RealmList;
import rx.functions.Action1;

import static org.grassroot.android.services.GcmListenerService.isAppIsInBackground;
import static org.grassroot.android.services.GcmListenerService.relayNotification;

/**
 * Created by paballo on 2016/11/01.
 */

public class MqttConnectionManager implements IMqttActionListener, MqttCallback {

    private static final String TAG = MqttConnectionManager.class.getSimpleName();

    private static MqttConnectionManager instance = null;

    private MqqtConnectionStatus mqqtConnectionStatus = MqqtConnectionStatus.NONE;
    private MqttAndroidClient mqttAndroidClient = null;
    private Gson gson;
    private String brokerUrl = Constant.brokerUrl;

    public enum MqqtConnectionStatus {
        CONNECTING,
        CONNECTED,
        DISCONNECTING,
        DISCONNECTED,
        ERROR,
        NONE
    }

    private MqttConnectionManager() {
        GsonBuilder builder = new GsonBuilder();

        builder.setExclusionStrategies(new AnnotationExclusionStrategy());
        builder.registerTypeAdapter(new TypeToken<RealmList<RealmString>>() {
        }.getType(), new RealmStringDeserializer());

        this.gson = builder.create();
    }

    public static MqttConnectionManager getInstance() {
        if (instance == null) {
            instance = new MqttConnectionManager();
        }
        return instance;
    }

    public boolean isConnected() {
        return mqttAndroidClient != null && mqttAndroidClient.isConnected();
    }

    public MqqtConnectionStatus getMqqtConnectionStatus() {
        return mqqtConnectionStatus;
    }

    public void connect() {
        Log.d(TAG, "connecting to mqtt broker at address: " + brokerUrl);

        final MqttConnectOptions options = new MqttConnectOptions();
        final String clientId = RealmUtils.loadPreferencesFromDB().getMobileNumber();
        options.setCleanSession(false);
        options.setKeepAliveInterval(60);
        options.setAutomaticReconnect(true);

        try {
            if (mqttAndroidClient == null) {
                mqttAndroidClient = new MqttAndroidClient(ApplicationLoader.applicationContext,
                        brokerUrl, clientId);
            }

            mqttAndroidClient.setCallback(this);
            if (!mqttAndroidClient.isConnected()) {
                IMqttToken token = mqttAndroidClient.connect(options);
                token.setActionCallback(new IMqttActionListener() {
                    @Override
                    public void onSuccess(IMqttToken asyncActionToken) {
                        mqqtConnectionStatus = MqqtConnectionStatus.CONNECTED;
                        subscribeToAllGroups();
                    }

                    @Override
                    public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                        mqqtConnectionStatus = MqqtConnectionStatus.ERROR;
                        Log.e(TAG, "Failure connecting! : " + exception.toString());
                    }
                });
            }

        } catch (MqttException e) {
            Log.e(TAG, e.getMessage());
        }
    }

    public void disconnect() {
        if (mqttAndroidClient != null && mqttAndroidClient.isConnected()) {
            try {
                mqttAndroidClient.disconnect();
                mqttAndroidClient = null;

            } catch (MqttException e) {
                Log.e(TAG, e.getMessage());
            }
        }
    }

    private void subscribeToAllGroups() {
        RealmUtils.loadGroupsSorted().subscribe(new Action1<List<Group>>() {
            @Override
            public void call(List<Group> groups) {
                subscribeToGroups(groups);
            }
        });
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

    private void subscribeToTopics(String[] topics, int[] qos) {
        if (mqttAndroidClient != null && mqttAndroidClient.isConnected()) {
            try {
                Log.e(TAG, "mqtt subscribing to topics: " + Arrays.toString(topics));
                IMqttToken token = mqttAndroidClient.subscribe(topics, qos);
                token.setActionCallback(this);
            } catch (MqttException e) {
                Log.e(TAG, e.getMessage());
            }
        }
    }


    public void subscribeToTopic(String topic, int qos) {
        if (mqttAndroidClient != null && mqttAndroidClient.isConnected()) {
            try {
                IMqttToken token = mqttAndroidClient.subscribe(topic, qos);
                token.setActionCallback(this);
            } catch (MqttException e) {
                Log.e(TAG, e.getMessage());
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
            }
        }
    }

    public void sendMessage(String topic, Message message) {
        try {
            Log.d(TAG, "publishing to topic " + topic);
            String jsonMessage = gson.toJson(message);
            MqttMessage mqttMessage = new MqttMessage(jsonMessage.getBytes());
            mqttMessage.setRetained(false);
            mqttMessage.setQos(1);
            IMqttToken token = mqttAndroidClient.publish(topic, mqttMessage);
            token.setActionCallback(this);
        } catch (MqttException e) {
            Log.e(TAG, e.getMessage());
        }
    }

    @Override
    public void connectionLost(Throwable cause) {
        mqqtConnectionStatus = MqqtConnectionStatus.DISCONNECTED;
        Log.d(TAG, "Disconnected from mqtt broker");

    }

    @Override
    public void messageArrived(String topic, MqttMessage mqttMessage) {
        Log.e(TAG, "received message from topic " + topic);
        String phoneNumber = RealmUtils.loadPreferencesFromDB().getMobileNumber();
        Message message = gson.fromJson(mqttMessage.toString(), Message.class);
        Log.e(TAG, "message " + message.toString());
        message.setDelivered(true);
        RealmUtils.saveDataToRealmSync(message);
        Bundle bundle = createBundleFromMessage(message);
        if (!message.getType().equals("ping")) {
            if (message.getType().equals("update_read_status")) {
                if (RealmUtils.hasMessage(message.getUid())) {
                    Message existingMessage = RealmUtils.loadMessage(message.getUid());
                    Log.e(TAG, "update exisiting message");
                    existingMessage.setRead(true);
                    RealmUtils.saveDataToRealmSync(existingMessage);
                    EventBus.getDefault().post(new GroupChatMessageReadEvent(existingMessage));
                }
            } else {
                if (message.getType().equals("sync"))
                    NetworkUtils.syncAndStartTasks(ApplicationLoader.applicationContext, true, true);
                if (isAppIsInBackground(ApplicationLoader.applicationContext) && !phoneNumber.equals(message.getPhoneNumber())) {
                    relayNotification(bundle);
                } else {
                    EventBus.getDefault().post(new GroupChatEvent(message.getGroupUid(), bundle, message));
                }
            }
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
        mqqtConnectionStatus = MqqtConnectionStatus.CONNECTED;
        Log.d(TAG, mqqtConnectionStatus.toString());
    }

    @Override
    public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
        Log.e(TAG, "error connecting: " + exception.toString());
        mqqtConnectionStatus = MqqtConnectionStatus.ERROR;
    }


}
