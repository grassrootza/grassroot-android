package com.techmorphosis.grassroot.models;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.List;

/**
 * Created by admin on 04-Apr-16.
 */
public class SingleContact implements Parcelable {

    public String addedBy;
    public boolean isSelected;
    public String name;
    public List<String> numbers;
    public String selectedNumber;
    public String contact_ID;


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.addedBy);
        dest.writeByte(isSelected ? (byte) 1 : (byte) 0);
        dest.writeString(this.name);
        dest.writeStringList(this.numbers);
        dest.writeString(this.selectedNumber);
        dest.writeString(this.contact_ID);
    }

    public SingleContact() {
    }

    protected SingleContact(Parcel in) {
        this.addedBy = in.readString();
        this.isSelected = in.readByte() != 0;
        this.name = in.readString();
        this.numbers = in.createStringArrayList();
        this.selectedNumber = in.readString();
        this.contact_ID = in.readString();
    }

    public static final Parcelable.Creator<SingleContact> CREATOR = new Parcelable.Creator<SingleContact>() {
        @Override
        public SingleContact createFromParcel(Parcel source) {
            return new SingleContact(source);
        }

        @Override
        public SingleContact[] newArray(int size) {
            return new SingleContact[size];
        }
    };

    @Override
    public String toString() {
        return "SingleContact{" +
                "name='" + name + '\'' +
                ", numbers=" + numbers +
                '}';
    }
}