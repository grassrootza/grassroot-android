package org.grassroot.android.models;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import com.google.gson.JsonParseException;

import org.grassroot.android.interfaces.GroupConstants;
import org.grassroot.android.interfaces.NotificationConstants;
import org.grassroot.android.services.GroupChatService;
import org.grassroot.android.utils.Constant;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

import io.realm.RealmList;
import io.realm.RealmObject;
import io.realm.annotations.Ignore;
import io.realm.annotations.Index;
import io.realm.annotations.PrimaryKey;

/**
 * Created by paballo on 2016/08/30.
 */
public class Message extends RealmObject implements Serializable {

    @PrimaryKey
    private String uid;
    private String phoneNumber;
    private String text;
    @Index
    private String groupUid;
    private String userUid;
    private String displayName;
    private String groupName;
    private String groupIcon;

    private Date time;
    private Date actionDateTime;

    private boolean sending;
    private boolean sent;
    private boolean delivered; // this is to server
    private int noAttempts;

    private boolean seen;


    private boolean read;
    private String type;

    private boolean server;
    private boolean toKeep;

    private RealmList<RealmString> tokens;

    @Ignore
    SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

    public Message() {
    }

    public Message(String phoneNumber, String groupUid, String displayName, Date time, String text, String groupName) {
        this.uid = UUID.randomUUID().toString().concat(phoneNumber); //concating phone number as uid is only unique per system
        this.phoneNumber = phoneNumber;
        this.groupUid = groupUid;
        this.groupName = groupName;
        this.displayName = displayName;
        this.text = text;
        this.time = time;
        this.type = "normal";
        this.seen=true;
        this.sending = false;
        this.sent = false;
        this.delivered = false;
        this.server = false;
    }

    public Message(String groupUid, String messageUid, String text) {
        this.uid = messageUid;
        this.server = true;
        this.groupUid = groupUid;
        this.text = text;
        this.time = new Date();
        this.type = "server";
        this.sent = true;
        this.delivered = true;
    }

    public Message(JSONObject map) throws JSONException {
        this.uid = String.valueOf(map.get("messageUid"));
        this.phoneNumber = String.valueOf(map.get("phone_number"));
        this.groupName = String.valueOf(map.get(GroupConstants.NAME_FIELD));
        this.groupIcon = String.valueOf(map.get("groupIcon"));
        this.displayName = String.valueOf(map.get(Constant.TITLE));
        this.groupUid = String.valueOf(map.get(GroupConstants.UID_FIELD));
        this.userUid = String.valueOf(map.get("userUid"));

        try {
            this.time = formatter.parse(String.valueOf(map.get("time")));
        } catch (ParseException e) {
            Log.e("date parserror", e.toString());
        }

        this.text = String.valueOf(map.get(Constant.BODY));
        this.type = String.valueOf(map.get("type"));
        this.sending = false;
        this.sent = true;
        this.delivered = true;
        this.noAttempts = -1;

        this.server = phoneNumber == null;

        if (this.server && map.has(NotificationConstants.TASK_DATE_TIME)) {
            try {
                this.actionDateTime = formatter.parse(String.valueOf(map.get(NotificationConstants.TASK_DATE_TIME)));
            } catch (ParseException|NullPointerException e) {
                e.printStackTrace();
                this.actionDateTime = null;
            }
        }

        if (map.has("tokens")) {
            String tokenValues = String.valueOf(map.get("tokens"));
            try {
                JSONArray jsonArray = new JSONArray(tokenValues);
                tokens = new RealmList<>();
                for (int i = 0; i < jsonArray.length(); i++) {
                    tokens.add(new RealmString(jsonArray.getString(i)));

                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

    }

    public Message(Bundle bundle) {
        this.uid = bundle.getString("messageUid");
        this.phoneNumber = bundle.getString("phone_number");
        this.groupName = bundle.getString(GroupConstants.NAME_FIELD);
        this.groupIcon = bundle.getString("groupIcon");
        this.displayName = bundle.getString(Constant.TITLE);
        this.groupUid = bundle.getString(GroupConstants.UID_FIELD);
        this.userUid = bundle.getString("userUid");
        Log.e("datetime", bundle.getString("time"));

        try {
            this.time = formatter.parse(bundle.getString("time"));
        } catch (ParseException e) {
            Log.e("date parserror", e.toString());
        }

        this.text = bundle.getString(Constant.BODY);
        this.type = bundle.getString("type");

        // by definition, since this is assembled from an incoming GCM packet, it is delivered to the topic
        this.sending = false;
        this.sent = true;
        this.delivered = true;
        this.noAttempts = -1;

        this.server = phoneNumber == null;

        if (this.server && bundle.containsKey(NotificationConstants.TASK_DATE_TIME)) {
            try {
                this.actionDateTime = formatter.parse(bundle.getString(NotificationConstants.TASK_DATE_TIME));
            } catch (ParseException|NullPointerException e) {
                e.printStackTrace();
                this.actionDateTime = null;
            }
        }

        if (bundle.containsKey("tokens")) {
            String tokenValues = bundle.getString("tokens");
            try {
                JSONArray jsonArray = new JSONArray(tokenValues);
                tokens = new RealmList<>();
                for (int i = 0; i < jsonArray.length(); i++) {
                    tokens.add(new RealmString(jsonArray.getString(i)));

                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

    }

    public String getText() {
        return text;
    }

    public void setText(String text) { this.text = text; }

    public String getGroupUid() {
        return groupUid;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getUid() {
        return uid;
    }

    public Date getTime() {
        return time;
    }

    public boolean isSending() { return sending; }

    public boolean isSent() { return sent; }

    public boolean isDelivered() {
        return delivered;
    }

    public void setSeen(boolean seen) {
        this.seen = seen;
    }

    public String getGroupName() {
        return groupName;
    }

    public String getUserUid() {
        return userUid;
    }

    public boolean isSeen() {
        return seen;
    }

    public boolean isRead() {
        return read;
    }

    public RealmList<RealmString> getTokens() {
        return tokens;
    }

    public String getType() {
        return type;
    }

    public int getNoAttempts() {
        return noAttempts;
    }

    public void setNoAttempts(int noAttempts) {
        this.noAttempts = noAttempts;
    }

    public void setSending(boolean sending) { this.sending = sending; }

    public void setSent(boolean sent) { this.sent = sent; }

    public void setDelivered(boolean delivered) {
        this.delivered = delivered;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public boolean isToKeep() {
        return toKeep;
    }

    public void setToKeep(boolean toKeep) {
        this.toKeep = toKeep;
    }

    public boolean isServer() {
        return server;
    }

    public void setServer(boolean server) {
        this.server = server;
    }

    public Date getActionDateTime() {
        return actionDateTime != null ? actionDateTime : new Date(); // to avoid null pointer errors if something corrupted
    }

    public String getDeadlineISO() {
        return formatter.format(getActionDateTime());
    }

    public boolean exceedsMaximumSendingAttempts() {
        return noAttempts == GroupChatService.MAX_RETRIES;
    }

    public boolean isServerMessage() {
        return TextUtils.isEmpty(phoneNumber);
    }

    @Override
    public String toString() {
        return "Message{" +
                "time='" + time + '\'' +
                ", uid = " + uid + '\'' +
                ", groupUid='" + groupUid + '\'' +
                ", text='" + text + '\'' +
                ", phoneNumber='" + phoneNumber + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Message)) return false;

        Message message = (Message) o;

        return uid.equals(message.uid);

    }

    @Override
    public int hashCode() {
        return uid.hashCode();
    }
}


