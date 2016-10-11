package org.grassroot.android.models;

import android.os.Bundle;
import android.util.Log;

import org.grassroot.android.interfaces.GroupConstants;
import org.grassroot.android.services.GcmUpstreamMessageService;
import org.grassroot.android.utils.Constant;
import org.json.JSONArray;
import org.json.JSONException;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

import io.realm.RealmList;
import io.realm.RealmObject;
import io.realm.annotations.Ignore;
import io.realm.annotations.Index;
import io.realm.annotations.PrimaryKey;

/**
 * Created by paballo on 2016/08/30.
 */
public class Message extends RealmObject {

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
    private boolean delivered;
    private boolean read;
    private String type;
    private int noAttempts;
    private RealmList<RealmString> tokens;
    @Ignore
    SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

    public Message() {
    }

    public Message(String phoneNumber, String groupUid, String displayName, Date time, String text, boolean delivered, String groupName) {
        this.uid = UUID.randomUUID().toString().concat(phoneNumber); //concating phone number as uid is only unique per system
        this.phoneNumber = phoneNumber;
        this.groupUid = groupUid;
        this.groupName = groupName;
        this.displayName = displayName;
        this.text = text;
        this.time = time;
        this.type="normal";
        this.delivered = delivered;

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
        this.delivered = true;

        if (bundle.containsKey("tokens")) {
            String tokenValues = bundle.getString("tokens");
            try {
                JSONArray jsonArray = new JSONArray(tokenValues);
                jsonArray.toString();
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

    public boolean isDelivered() {
        return delivered;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public String getGroupName() {
        return groupName;
    }

    public String getUserUid() {
        return userUid;
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

    public void setDelivered(boolean delivered) {
        this.delivered = delivered;
    }

    public boolean exceedsMaximumSendingAttempts() {
        return noAttempts == GcmUpstreamMessageService.MAX_RETRIES;
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


