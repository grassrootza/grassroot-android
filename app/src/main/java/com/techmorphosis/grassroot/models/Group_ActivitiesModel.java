package com.techmorphosis.grassroot.models;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by ravi on 15/4/16.
 */
public class Group_ActivitiesModel implements Parcelable {
    public String id;
    public String title;
    public String description;
    public String name;
    public String type;
    public String deadline;
    public Boolean hasResponded;
    public Boolean canAction;
    public String reply;
    public String Thumpsup;
    public String Thumpsdown;
    public  String completedyes;
    public  String completedno;

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.id);
        dest.writeString(this.title);
        dest.writeString(this.description);
        dest.writeString(this.name);
        dest.writeString(this.type);
        dest.writeString(this.deadline);
        dest.writeValue(this.hasResponded);
        dest.writeValue(this.canAction);
        dest.writeString(this.reply);
        dest.writeString(this.Thumpsup);
        dest.writeString(this.Thumpsdown);
        dest.writeString(this.completedyes);
        dest.writeString(this.completedno);
    }

    public Group_ActivitiesModel() {
    }

    protected Group_ActivitiesModel(Parcel in) {
        this.id = in.readString();
        this.title = in.readString();
        this.description = in.readString();
        this.name = in.readString();
        this.type = in.readString();
        this.deadline = in.readString();
        this.hasResponded = (Boolean) in.readValue(Boolean.class.getClassLoader());
        this.canAction = (Boolean) in.readValue(Boolean.class.getClassLoader());
        this.reply = in.readString();
        this.Thumpsup = in.readString();
        this.Thumpsdown = in.readString();
        this.completedyes = in.readString();
        this.completedno = in.readString();
    }

    public static final Parcelable.Creator<Group_ActivitiesModel> CREATOR = new Parcelable.Creator<Group_ActivitiesModel>() {
        @Override
        public Group_ActivitiesModel createFromParcel(Parcel source) {
            return new Group_ActivitiesModel(source);
        }

        @Override
        public Group_ActivitiesModel[] newArray(int size) {
            return new Group_ActivitiesModel[size];
        }
    };
}
