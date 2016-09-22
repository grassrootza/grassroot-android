package org.grassroot.android.models;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.Date;
import java.util.UUID;

import io.realm.RealmList;
import io.realm.RealmObject;
import io.realm.annotations.Index;
import io.realm.annotations.PrimaryKey;

/**
 * Created by paballo on 2016/08/30.
 */
public class Message extends RealmObject implements Parcelable {

    @PrimaryKey
    private String id;

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


    public Message() {
    }

    public Message(String phoneNumber, String groupUid, String displayName, Date time, String text, boolean delivered, String groupName) {

        this.id = UUID.randomUUID().toString();
        this.phoneNumber = phoneNumber;
        this.groupUid = groupUid;
        this.groupName = groupName;
        this.displayName = displayName;
        this.text = text;
        this.time = time;
        this.delivered = delivered;

    }

    public Message(Bundle bundle) {
        this.id = bundle.getString("uid");
        this.phoneNumber = bundle.getString("phone_number");
        this.groupName = bundle.getString("groupName");
        this.groupIcon = bundle.getString("groupIcon");
        this.displayName = bundle.getString("title");
        this.groupUid = bundle.getString("groupUid");
        this.userUid = bundle.getString("userUid");
        this.time = new Date(bundle.getString("time"));
        this.text = bundle.getString("body");
        this.type = bundle.getString("type");
        this.delivered = true;

        if (bundle.containsKey("tokens")) {
            String tokenValues = bundle.getString("tokens");
            Log.e("Has tokens", "has tokens");
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


    public static final Creator<Message> creator = new Creator<Message>() {
        @Override
        public Message createFromParcel(Parcel source) {
            return new Message(source);
        }

        @Override
        public Message[] newArray(int size) {
            return new Message[size];
        }
    };

    protected Message(Parcel in) {
        id = in.readString();
        text = in.readString();
        groupUid = in.readString();
        groupName = in.readString();
        displayName = in.readString();
        phoneNumber = in.readString();
        long tmpDate = in.readLong();
        time = tmpDate == -1 ? null : new Date(tmpDate);

    }

    public static final Creator<Message> CREATOR = new Creator<Message>() {
        @Override
        public Message createFromParcel(Parcel in) {
            return new Message(in);
        }

        @Override
        public Message[] newArray(int size) {
            return new Message[size];
        }
    };

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

    public String getId() {
        return id;
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

    public boolean exceedsMaximumSendingAttempts(){
        return  noAttempts >4;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(text);
        dest.writeString(groupUid);
        dest.writeString(groupName);
        dest.writeString(displayName);
        dest.writeString(phoneNumber);
        dest.writeLong(time != null ? time.getTime() : -1);
    }

    @Override
    public String toString() {
        return "Message{" +
                "time='" + time + '\'' +
                ", uid = " + id + '\'' +
                ", groupUid='" + groupUid + '\'' +
                ", text='" + text + '\'' +
                ", phoneNumber='" + phoneNumber + '\'' +
                '}';
    }
}
