package com.techmorphosis.grassroot.models;

/**
 * Created by ravi on 12/5/16.
 */
public class NotificationModel {

    String uid;
    String entityUid;
    String title;
    String message;
    String createdDatetime;
    String notificationType;
    String entityType;
    String delivered;
    String read;

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getEntityUid() {
        return entityUid;
    }

    public void setEntityUid(String entityUid) {
        this.entityUid = entityUid;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getCreatedDatetime() {
        return createdDatetime;
    }

    public void setCreatedDatetime(String createdDatetime) {
        this.createdDatetime = createdDatetime;
    }

    public String getNotificationType() {
        return notificationType;
    }

    public void setNotificationType(String notificationType) {
        this.notificationType = notificationType;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public String getDelivered() {
        return delivered;
    }

    public void setDelivered(String delivered) {
        this.delivered = delivered;
    }

    public String getRead() {
        return read;
    }

    public void setRead(String read) {
        this.read = read;
    }
}
