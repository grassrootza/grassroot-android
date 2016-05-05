package com.techmorphosis.grassroot.services.model;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by luke on 2016/05/05.
 * todo: probably need to have roles, and various other things, in here too
 */
public class Member implements Parcelable {

    private String userUid;
    private String userPhoneNumber;
    private String userDisplayName;

    @Override
    public int describeContents() { return 0; }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.userUid);
        dest.writeString(this.userPhoneNumber);
        dest.writeString(this.userDisplayName);
    }

    protected Member(Parcel incoming) {
        userUid = incoming.readString();
        userPhoneNumber = incoming.readString();
        userDisplayName = incoming.readString();
    }

    public static final Creator<Member> CREATOR = new Creator<Member>() {
        @Override
        public Member createFromParcel(Parcel source) {
            return new Member(source);
        }

        @Override
        public Member[] newArray(int size) {
            return new Member[size];
        }
    };

    // GETTERS

    public String getUserUid() {
        return userUid;
    }

    public String getUserPhoneNumber() {
        return userPhoneNumber;
    }

    public String getUserDisplayName() {
        return userDisplayName;
    }
}
