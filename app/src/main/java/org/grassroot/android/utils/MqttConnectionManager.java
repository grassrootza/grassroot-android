package org.grassroot.android.utils;

import android.content.Context;
import android.util.Log;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.grassroot.android.models.Message;
import org.json.JSONObject;

/**
 * Created by paballo on 2016/11/01.
 */

public class MqttConnectionManager implements IMqttActionListener, MqttCallback {

    private static final String TAG = MqttConnectionManager.class.getCanonicalName();
    private static MqttConnectionManager instance = null;
    private MqqtConnectionStatus mqqtConnectionStatus = MqqtConnectionStatus.NONE;
    private MqttAndroidClient mqttAndroidClient = null;
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
    }

    public static MqttConnectionManager getInstance(Context context) {
        if (instance == null) {
            instance = new MqttConnectionManager(context);
        }
        return instance;
    }
    public boolean isConnected() {
        return mqttAndroidClient.isConnected();
    }

    public void connect() {

        final MqttConnectOptions options = new MqttConnectOptions();
        final String clientId = RealmUtils.loadPreferencesFromDB().getMobileNumber();
        options.setCleanSession(false);
        options.setKeepAliveInterval(0);

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

    public void disconnect(){
        if(mqttAndroidClient != null && mqttAndroidClient.isConnected()){
            try {
                mqttAndroidClient.disconnect();
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

    public void subscribeToTopic(String topic, int qos){
        if (mqttAndroidClient != null && mqttAndroidClient.isConnected()) {
            try {
                IMqttToken token = mqttAndroidClient.subscribe(topic, qos);
                token.setActionCallback(this);
            } catch (MqttException e) {
                Log.e(TAG, e.getMessage());
            }
        }
    }

    public void unsubscribeFromTopic(String topic){
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
            MqttMessage mqttMessage = new MqttMessage(SerializationUtils.serialize(message));
            mqttMessage.setRetained(false);
            mqttMessage.setQos(1);
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
        Log.e(TAG, cause.getMessage());
        //todo reconnect maybe?
    }

    @Override
    public void messageArrived(String topic, MqttMessage mqttMessage) throws Exception {
        Message message = new Message(new JSONObject(mqttMessage.toString()));
        RealmUtils.saveDataToRealmSync(message);
        Log.e(TAG, "message received");

    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        //todo will be useful once we start sending messages upstream via mqtt

    }


}
