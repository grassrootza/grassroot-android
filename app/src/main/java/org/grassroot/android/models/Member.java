package org.grassroot.android.models;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;
import org.grassroot.android.interfaces.GroupConstants;

/**
 * Created by luke on 2016/05/05.
 * todo: probably need to have roles, and various other things, in here too
 */
public class Member extends RealmObject implements Parcelable {

  private static final String TAG = Member.class.getCanonicalName();

  public static final String PKEY = "memberGroupUid";

  @PrimaryKey private String memberGroupUid;

  private String memberUid;
  private String groupUid;

  private String phoneNumber;
  private String displayName;
  private String roleName;

  private int contactId; // only set locally, if we retrieve member from contacts
  private boolean selected;
  private boolean isLocal;

  public boolean isLocal() {
    return isLocal;
  }

  public void setLocal(boolean local) {
    isLocal = local;
  }

  public Member() {
  }

  @Override public int describeContents() {
    return 0;
  }

  @Override public void writeToParcel(Parcel dest, int flags) {
    dest.writeString(this.memberUid);
    dest.writeString(this.phoneNumber);
    dest.writeString(this.displayName);
    dest.writeString(this.roleName);
    dest.writeString(this.groupUid);
    dest.writeString(memberGroupUid);
    dest.writeInt(selected ? 1 : 0);
    dest.writeInt(isLocal ? 1 : 0);
    dest.writeInt(this.contactId);
  }

  protected Member(Parcel incoming) {
    memberUid = incoming.readString();
    phoneNumber = incoming.readString();
    displayName = incoming.readString();
    roleName = incoming.readString();
    groupUid = incoming.readString();
    memberGroupUid = incoming.readString();
    selected = incoming.readInt() != 0;
    isLocal = incoming.readInt() != 0;
    contactId = incoming.readInt();
  }

  // todo : probably remove, soon-ish
  public Member(String phoneNumber, String displayName, String roleName, int contactId) {
    this.phoneNumber = phoneNumber;
    this.displayName = displayName;
    this.roleName = (roleName != null) ? roleName : GroupConstants.ROLE_ORDINARY_MEMBER;
    this.contactId = contactId;
    this.selected = true;
  }

  public Member(String phoneNumber, String displayName, String roleName, int contactId,
      boolean selected) {
    this.phoneNumber = phoneNumber;
    this.displayName = displayName;
    this.roleName = (roleName != null) ? roleName : GroupConstants.ROLE_ORDINARY_MEMBER;
    this.contactId = contactId;
    this.selected = selected;
  }

  public static final Creator<Member> CREATOR = new Creator<Member>() {
    @Override public Member createFromParcel(Parcel source) {
      return new Member(source);
    }

    @Override public Member[] newArray(int size) {
      return new Member[size];
    }
  };

  // GETTERS

  public String getMemberGroupUid() {
    return memberGroupUid;
  }

  public void setMemberGroupUid() {
    this.memberGroupUid = this.memberUid + this.groupUid;
  }

  public String getMemberUid() {
    return memberUid;
  }

  public void setMemberUid(String memberUid) {
    this.memberUid = memberUid;
  }

  public String getPhoneNumber() {
    return phoneNumber;
  }

  public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) { this.displayName = displayName; }

  public String getGroupUid() {
    return groupUid;
  }

  public String getRoleName() {
    return roleName;
  }

  public void setRoleName(String roleName) { this.roleName = roleName; }

  public int getContactId() {
    return contactId;
  }

  public void setContactId(int contactId) {
    this.contactId = contactId;
  }

  public boolean isSelected() {
    return selected;
  }

  public void setSelected(boolean selected) {
    this.selected = selected;
  }

  public void toggleSelected() {
    selected = !selected;
  }

  // toString etc

  @Override public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    Member member = (Member) o;

    if (memberUid != null ? !memberUid.equals(member.memberUid) : member.memberUid != null) {
      return false;
    }
    return contactId != -1 ? contactId == member.contactId : member.contactId == -1;
  }

  @Override public int hashCode() {
    int result = memberUid != null ? memberUid.hashCode() : 0;
    result = 31 * result + (contactId != -1 ? contactId : 0);
    return result;
  }

  @Override public String toString() {
    return "Member{" +
        "memberUid='" + memberUid + '\'' +
        ", phoneNumber='" + phoneNumber + '\'' +
        ", displayName='" + displayName + '\'' +
        ", contactId=" + contactId + '\'' +
        ", selected=" + selected + '\'' +
        '}';
  }

  public void setGroupUid(String groupUid) {
    this.groupUid = groupUid;
  }
}
