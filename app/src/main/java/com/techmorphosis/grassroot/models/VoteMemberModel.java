package com.techmorphosis.grassroot.models;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by ravi on 20/5/16.
 */
public class VoteMemberModel implements Parcelable {
   public String memberUid;
    public String displayName;
    public   String groupUid;
    public String phoneNumber;
    public String roleName;
    public Boolean isSelected;

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.memberUid);
        dest.writeString(this.displayName);
        dest.writeString(this.groupUid);
        dest.writeString(this.phoneNumber);
        dest.writeString(this.roleName);
        dest.writeValue(this.isSelected);
    }

    public VoteMemberModel() {
    }

    protected VoteMemberModel(Parcel in) {
        this.memberUid = in.readString();
        this.displayName = in.readString();
        this.groupUid = in.readString();
        this.phoneNumber = in.readString();
        this.roleName = in.readString();
        this.isSelected = (Boolean) in.readValue(Boolean.class.getClassLoader());
    }

    public static final Parcelable.Creator<VoteMemberModel> CREATOR = new Parcelable.Creator<VoteMemberModel>() {
        @Override
        public VoteMemberModel createFromParcel(Parcel source) {
            return new VoteMemberModel(source);
        }

        @Override
        public VoteMemberModel[] newArray(int size) {
            return new VoteMemberModel[size];
        }
    };
}
