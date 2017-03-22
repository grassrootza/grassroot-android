package org.grassroot.android.models;

import android.os.Parcel;
import android.os.Parcelable;

import io.realm.RealmObject;

/**
 * Created by luke on 2017/02/25.
 */

public class ImageRecord extends RealmObject implements Parcelable {

    private String key;
    private String actionLogType;
    private String taskUid;
    private String bucket;
    private Long creationTime;
    private Long storageTime;
    private String md5;
    private String userDisplayName;
    private String userPhoneNumber;
    private Double latitude;
    private Double longitude;
    private boolean analyzed;
    private Integer numberFaces;
    private boolean countModified;
    private Integer revisedFaces;

    public ImageRecord() {
        // for Realm
    }

    protected ImageRecord(Parcel in) {
        key = in.readString();
        actionLogType = in.readString();
        taskUid = in.readString();
        bucket = in.readString();
        md5 = in.readString();
        userDisplayName = in.readString();
        userPhoneNumber = in.readString();
        analyzed = in.readByte() != 0;
        countModified = in.readByte() != 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(key);
        dest.writeString(actionLogType);
        dest.writeString(taskUid);
        dest.writeString(bucket);
        dest.writeString(md5);
        dest.writeString(userDisplayName);
        dest.writeString(userPhoneNumber);
        dest.writeByte((byte) (analyzed ? 1 : 0));
        dest.writeByte((byte) (countModified ? 1 : 0));
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<ImageRecord> CREATOR = new Creator<ImageRecord>() {
        @Override
        public ImageRecord createFromParcel(Parcel in) {
            return new ImageRecord(in);
        }

        @Override
        public ImageRecord[] newArray(int size) {
            return new ImageRecord[size];
        }
    };

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getActionLogType() {
        return actionLogType;
    }

    public void setActionLogType(String actionLogType) {
        this.actionLogType = actionLogType;
    }

    public String getTaskUid() {
        return taskUid;
    }

    public void setTaskUid(String taskUid) {
        this.taskUid = taskUid;
    }

    public String getBucket() {
        return bucket;
    }

    public void setBucket(String bucket) {
        this.bucket = bucket;
    }

    public Long getCreationTime() {
        return creationTime;
    }

    public void setCreationTime(Long creationTime) {
        this.creationTime = creationTime;
    }

    public Long getStorageTime() {
        return storageTime;
    }

    public void setStorageTime(Long storageTime) {
        this.storageTime = storageTime;
    }

    public String getMd5() {
        return md5;
    }

    public void setMd5(String md5) {
        this.md5 = md5;
    }

    public String getUserDisplayName() {
        return userDisplayName;
    }

    public void setUserDisplayName(String userDisplayName) {
        this.userDisplayName = userDisplayName;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public void setAnalyzed(boolean analyzed) { this.analyzed = analyzed; }

    public boolean isAnalyzed() { return analyzed; }

    public void setNumberFaces(Integer numberFaces) { this.numberFaces = numberFaces; }

    public Integer getNumberFaces() { return numberFaces; }

    public boolean hasFoundFaces() {
        return analyzed && numberFaces != null && numberFaces > 0;
    }

    public String getUserPhoneNumber() {
        return userPhoneNumber;
    }

    public void setUserPhoneNumber(String userPhoneNumber) {
        this.userPhoneNumber = userPhoneNumber;
    }

    public boolean isCountModified() {
        return countModified;
    }

    public void setCountModified(boolean countModified) {
        this.countModified = countModified;
    }

    public Integer getRevisedFaces() {
        return revisedFaces;
    }

    public void setRevisedFaces(Integer revisedFaces) {
        this.revisedFaces = revisedFaces;
    }
}
