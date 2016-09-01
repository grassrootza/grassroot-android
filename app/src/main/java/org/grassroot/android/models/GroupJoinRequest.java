package org.grassroot.android.models;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import org.grassroot.android.utils.Constant;

import java.text.ParseException;
import java.util.Date;

import io.realm.Realm;
import io.realm.RealmObject;
import io.realm.annotations.Ignore;
import io.realm.annotations.PrimaryKey;

/**
 * Created by luke on 2016/07/09.
 */
public class GroupJoinRequest extends RealmObject implements Parcelable, Comparable<GroupJoinRequest> {

    public static final String SENT_REQUEST = "SENT_REQUEST";
    public static final String REC_REQUEST = "RECEIVED_REQUEST";

    @PrimaryKey
    private String requestUid;

    private String joinReqType;
    private String requestorName;
    private String requestorNumber;

    private String groupUid;
    private String groupName;

    private String requestDescription;

    private String createdDateTimeISO;
    @Ignore private Date createdDateTime;

    public GroupJoinRequest() {
    }

    public String getRequestUid() {
        return requestUid;
    }

    public String getRequestorName() {
        return requestorName;
    }

    public String getRequestorNumber() {
        if (TextUtils.isEmpty(requestorNumber)) {
            return "N/A";
        } else {
            return requestorNumber;
        }
    }

    public String getGroupUid() {
        return groupUid;
    }

    public String getGroupName() {
        return groupName;
    }

    public String getRequestDescription() {
        return requestDescription;
    }

    public String getCreatedDateTimeISO() {
        return createdDateTimeISO;
    }

    public String getJoinReqType() { return joinReqType; }

    public Date getCreatedDateTime() {
        if (createdDateTime == null) {
            try {
                createdDateTime = Constant.isoDateTimeSDF.parse(createdDateTimeISO);
            } catch (ParseException e) {
                createdDateTime = new Date();
                e.printStackTrace();
            }
        }
        return createdDateTime;
    }

    // todo : make nullsafe
    @Override
    public int compareTo(GroupJoinRequest another) {
        return getCreatedDateTime().compareTo(another.getCreatedDateTime());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    // including Realm in here, as was introduced to Group ... remove if not needed
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        Realm realm = Realm.getDefaultInstance();
        realm.beginTransaction();
        dest.writeString(requestUid);
        dest.writeString(requestorName);
        dest.writeString(requestorNumber);
        dest.writeString(groupUid);
        dest.writeString(groupName);
        dest.writeString(requestDescription);
        dest.writeString(createdDateTimeISO);
        realm.commitTransaction();
        realm.close();
    }

    protected GroupJoinRequest(Parcel in) {
        requestUid = in.readString();
        requestorName = in.readString();
        requestorNumber = in.readString();
        groupUid = in.readString();
        groupName = in.readString();
        requestDescription = in.readString();
        createdDateTimeISO = in.readString();
    }

    public static final Creator<GroupJoinRequest> CREATOR = new Creator<GroupJoinRequest>() {
        @Override
        public GroupJoinRequest createFromParcel(Parcel source) {
            return new GroupJoinRequest(source);
        }

        @Override
        public GroupJoinRequest[] newArray(int size) {
            return new GroupJoinRequest[size];
        }
    };
}
