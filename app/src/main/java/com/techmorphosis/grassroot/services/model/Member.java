package com.techmorphosis.grassroot.services.model;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.techmorphosis.grassroot.utils.Constant;

/**
 * Created by luke on 2016/05/05.
 * todo: probably need to have roles, and various other things, in here too
 */
public class Member implements Parcelable {

    private static final String TAG = Member.class.getCanonicalName();

    private String memberUid;
    private String memberPhoneNumber;
    private String memberDisplayName;
    private String memberRoleName;
    private String groupUid;

    @Override
    public int describeContents() { return 0; }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.memberUid);
        dest.writeString(this.memberPhoneNumber);
        dest.writeString(this.memberDisplayName);
        dest.writeString(this.memberRoleName);
        dest.writeString(this.groupUid);
    }

    protected Member(Parcel incoming) {
        Log.d(TAG, "inside Member, constructing with incoming parcel : " + incoming.toString());
        memberUid = incoming.readString();
        memberPhoneNumber = incoming.readString();
        memberDisplayName = incoming.readString();
        memberRoleName = incoming.readString();
        groupUid = incoming.readString();
    }

    public Member(String memberPhoneNumber, String memberDisplayName, String memberRoleName) {
        this.memberPhoneNumber = memberPhoneNumber;
        this.memberDisplayName = memberDisplayName;
        this.memberRoleName = (memberRoleName != null) ? memberRoleName : Constant.ROLE_ORDINARY_MEMBER;
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

    public String getMemberUid() {
        return memberUid;
    }

    public String getMemberPhoneNumber() {
        return memberPhoneNumber;
    }

    public String getMemberDisplayName() {
        return memberDisplayName;
    }

    public String getGroupUid() {
        return groupUid;
    }

    public String getMemberRoleName() {
        return memberRoleName;
    }

    // toString etc


    @Override
    public String toString() {
        return "Member{" +
                "memberUid='" + memberUid + '\'' +
                ", memberPhoneNumber='" + memberPhoneNumber + '\'' +
                ", memberDisplayName='" + memberDisplayName + '\'' +
                '}';
    }
}
