package com.techmorphosis.grassroot.models;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;

/**
 * Created by ravi on 9/4/16.
 */
public class Group_Homepage_Model implements Parcelable {

    public String id;
    public String groupName;
    public String description;
    public String groupCreator;
    public String role;
    public String groupMemberCount;
    public String dateTimefull;
    public String dateTimeshort;

    /*Date*/
    public String dayOfMonth;
    public  String monthValue;
    public String year;

    /*time*/
    public  String hour;
    public  String minute;
    public  String second;
    public  String nano;

    public ArrayList<String> permissionsList;


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.id);
        dest.writeString(this.groupName);
        dest.writeString(this.description);
        dest.writeString(this.groupCreator);
        dest.writeString(this.role);
        dest.writeString(this.groupMemberCount);
        dest.writeString(this.dateTimefull);
        dest.writeString(this.dateTimeshort);
        dest.writeString(this.dayOfMonth);
        dest.writeString(this.monthValue);
        dest.writeString(this.year);
        dest.writeString(this.hour);
        dest.writeString(this.minute);
        dest.writeString(this.second);
        dest.writeString(this.nano);
        dest.writeStringList(this.permissionsList);
    }

    public Group_Homepage_Model() {
    }

    protected Group_Homepage_Model(Parcel in) {
        this.id = in.readString();
        this.groupName = in.readString();
        this.description = in.readString();
        this.groupCreator = in.readString();
        this.role = in.readString();
        this.groupMemberCount = in.readString();
        this.dateTimefull = in.readString();
        this.dateTimeshort = in.readString();
        this.dayOfMonth = in.readString();
        this.monthValue = in.readString();
        this.year = in.readString();
        this.hour = in.readString();
        this.minute = in.readString();
        this.second = in.readString();
        this.nano = in.readString();
        this.permissionsList = in.createStringArrayList();
    }

    public static final Parcelable.Creator<Group_Homepage_Model> CREATOR = new Parcelable.Creator<Group_Homepage_Model>() {
        @Override
        public Group_Homepage_Model createFromParcel(Parcel source) {
            return new Group_Homepage_Model(source);
        }

        @Override
        public Group_Homepage_Model[] newArray(int size) {
            return new Group_Homepage_Model[size];
        }
    };
}
