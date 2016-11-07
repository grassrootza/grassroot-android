package org.grassroot.android.utils;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.grassroot.android.R;
import org.grassroot.android.events.GroupChatEvent;
import org.grassroot.android.events.GroupChatMessageReadEvent;
import org.grassroot.android.interfaces.GroupConstants;
import org.grassroot.android.interfaces.NotificationConstants;
import org.grassroot.android.models.Message;
import org.grassroot.android.models.RealmString;
import org.greenrobot.eventbus.EventBus;

import java.lang.reflect.Type;
import java.util.Date;

import io.realm.RealmList;

import static org.grassroot.android.services.GcmListenerService.isAppIsInBackground;
import static org.grassroot.android.services.GcmListenerService.relayNotification;

/**
 * Created by paballo on 2016/11/01.
 */

public class MqttConnectionManager implements IMqttActionListener, MqttCallback {

    private static final String TAG = MqttConnectionManager.class.getCanonicalName();
    private static MqttConnectionManager instance = null;
    private MqqtConnectionStatus mqqtConnectionStatus = MqqtConnectionStatus.NONE;
    private MqttAndroidClient mqttAndroidClient = null;
    private Gson serverMessageDeserializer;
    private String brokerUrl = Constant.stagingBrokerUrl;
    private Context context;

    public enum MqqtConnectionStatus {
        CONNECTING,
        CONNECTED,
        DISCONNECTING,
        DISCONNECTED,
        ERROR,
        NONE
    }

    public MqttConnectionManager(Context context) {
        this.context = context.getApplicationContext();
        GsonBuilder builder = new GsonBuilder();
        builder.setExclusionStrategies(new AnnotationExclusionStrategy());

        builder.registerTypeAdapter(Date.class, new JsonDeserializer<Date>() {
            public Date deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
                return new Date(json.getAsJsonPrimitive().getAsLong());
            }
        });
        builder.registerTypeAdapter(new TypeToken<RealmList<RealmString>>() {
        }.getType(), new RealmStringDeserializer());
        serverMessageDeserializer = builder.create();
    }

    public static MqttConnectionManager getInstance(Context context) {
        if (instance == null) {
            instance = new MqttConnectionManager(context);
        }
        return instance;
    }

    public boolean isConnected() {
        return mqttAndroidClient != null && mqttAndroidClient.isConnected();
    }

    public void connect() {

        final MqttConnectOptions options = new MqttConnectOptions();
        final String clientId = RealmUtils.loadPreferencesFromDB().getMobileNumber();
        options.setCleanSession(false);
        options.setKeepAliveInterval(0);
        options.setAutomaticReconnect(true);


        if (mqttAndroidClient == null) {
            mqttAndroidClient = new MqttAndroidClient(context, brokerUrl,
                    clientId);
            mqttAndroidClient.setCallback(this);
        }
        if (!mqttAndroidClient.isConnected()) {
            try {
                IMqttToken token = mqttAndroidClient.connect(options);
                token.setActionCallback(this);


            } catch (MqttException e) {
                Log.e(TAG, e.getMessage());
            }
        }
    }

    public void disconnect() {
        if (mqttAndroidClient != null && mqttAndroidClient.isConnected()) {
            try {
                mqttAndroidClient.disconnect();
                mqttAndroidClient = null;
            } catch (MqttException e) {

            }
        }
    }

    public void subscribeToTopics(String[] topics, int[] qos) {
        if (mqttAndroidClient != null && mqttAndroidClient.isConnected()) {
            try {
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
            String jsonMessage = serverMessageDeserializer.toJson(message);
            MqttMessage mqttMessage = new MqttMessage(jsonMessage.getBytes());
            mqttMessage.setRetained(false);
            mqttMessage.setQos(1);
            Log.e(TAG, "publishing to topic " + topic);
            IMqttToken token = mqttAndroidClient.publish(topic, mqttMessage);
            token.setActionCallback(this);

        } catch (MqttException e) {
            Log.e(TAG, e.getMessage());
        }
    }


    @Override
    public void onSuccess(IMqttToken asyncActionToken) {
        mqqtConnectionStatus = MqqtConnectionStatus.CONNECTED;
        Log.e(TAG, mqqtConnectionStatus.toString());

    }

    @Override
    public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
        mqqtConnectionStatus = MqqtConnectionStatus.ERROR;
    }

    @Override
    public void connectionLost(Throwable cause) {
        mqqtConnectionStatus = MqqtConnectionStatus.DISCONNECTED;
        Log.e(TAG, "Disconnected from broker");

    }

    @Override
    public void messageArrived(String topic, MqttMessage mqttMessage) {
        Log.e(TAG, "receive message from broker");
        Log.e(TAG, "received from topic " + topic);
        String phoneNumber = RealmUtils.loadPreferencesFromDB().getMobileNumber();
        Message message;
        try {
            message = serverMessageDeserializer.fromJson(mqttMessage.toString(), Message.class);
            Log.e(TAG, message.getTokens().toString());
        } catch (JsonParseException e) {
            GsonBuilder builder = new GsonBuilder();
            builder.setExclusionStrategies(new AnnotationExclusionStrategy());
            Gson gson = builder.create();
            message = gson.fromJson(mqttMessage.toString(), Message.class);
            Log.e(TAG, message.getTokens().toString());
        }
        message.setDelivered(true);
        RealmUtils.saveDataToRealmSync(message);
        Bundle bundle = createBundleFromMessage(message);

        if (!message.getType().equals("ping")) {
            if (message.getType().equals("update_read_status")) {
                if (RealmUtils.hasMessage(message.getUid())) {
                    Message existingMessage = RealmUtils.loadMessage(message.getUid());
                    existingMessage.setRead(true);
                    existingMessage.setSent(true);
                    existingMessage.setDelivered(true);
                    RealmUtils.saveDataToRealmSync(existingMessage);
                    EventBus.getDefault().post(new GroupChatMessageReadEvent(existingMessage));
                }
            } else {
                if (isAppIsInBackground(context) && !phoneNumber.equals(message.getPhoneNumber())) {
                    relayNotification(bundle);
                } else {

                    EventBus.getDefault().post(new GroupChatEvent(message.getGroupUid(), bundle, message));
                }
            }
        }


}

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        Log.e(TAG, "message sent");
        try {

            GsonBuilder builder = new GsonBuilder();
            builder.setExclusionStrategies(new AnnotationExclusionStrategy());
            Gson gson = builder.create();

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
        bundle.putString(NotificationConstants.ENTITY_TYPE, NotificationConstants.CHAT_MESSAGE);

        return bundle;


    }


}
