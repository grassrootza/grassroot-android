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
    private String groupUid;

    private String phoneNumber;
    private String displayName;
    private String roleName;

    private String contactId; // only set locally, if we retrieve member from contacts
    private boolean selected;

    @Override
    public int describeContents() { return 0; }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.memberUid);
        dest.writeString(this.phoneNumber);
        dest.writeString(this.displayName);
        dest.writeString(this.roleName);
        dest.writeString(this.groupUid);
    }

    protected Member(Parcel incoming) {
        Log.d(TAG, "inside Member, constructing with incoming parcel : " + incoming.toString());
        memberUid = incoming.readString();
        phoneNumber = incoming.readString();
        displayName = incoming.readString();
        roleName = incoming.readString();
        groupUid = incoming.readString();
        selected = true;
    }

    public Member(String phoneNumber, String displayName, String roleName, String contactId) {
        this.phoneNumber = phoneNumber;
        this.displayName = displayName;
        this.roleName = (roleName != null) ? roleName : Constant.ROLE_ORDINARY_MEMBER;
        this.contactId = contactId;
        this.selected = true;
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

    public void setMemberUid(String memberUid) { this.memberUid = memberUid; }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getGroupUid() {
        return groupUid;
    }

    public String getRoleName() {
        return roleName;
    }

    public String getContactId() { return contactId; }

    public void setContactId(String contactId) { this.contactId = contactId; }

    public boolean isSelected() { return selected; }

    public void setSelected(boolean selected) { this.selected = selected; }

    public void toggleSelected() { selected = !selected; }

    // toString etc

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Member member = (Member) o;

        if (memberUid != null ? !memberUid.equals(member.memberUid) : member.memberUid != null)
            return false;
        return contactId != null ? contactId.equals(member.contactId) : member.contactId == null;
    }

    @Override
    public int hashCode() {
        int result = memberUid != null ? memberUid.hashCode() : 0;
        result = 31 * result + (contactId != null ? contactId.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Member{" +
                "memberUid='" + memberUid + '\'' +
                ", phoneNumber='" + phoneNumber + '\'' +
                ", displayName='" + displayName + '\'' +
                ", contactId=" + contactId + '\'' +
                ", selected=" + selected + '\'' +
                '}';
    }
}
