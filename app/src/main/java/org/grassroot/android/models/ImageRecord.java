package org.grassroot.android.models;

import io.realm.RealmObject;

/**
 * Created by luke on 2017/02/25.
 */

public class ImageRecord extends RealmObject {

    private String key;
    private String actionLogType;
    private String taskUid;
    private String bucket;
    private Long creationTime;
    private Long storageTime;
    private String md5;

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
}
