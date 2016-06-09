package org.grassroot.android.models;

/**
 * Created by paballo on 2016/05/18.
 */
public class Notification {
    private String uid;
    private String entityUid;
    private String title;
    private String message;
    private String createdDateTime; //todo change to iso datetime server side
    private String notificationType;
    private String entityType;
    private boolean read;
    private boolean delivered;


    public String getUid() {
        return uid;
    }

    public String getEntityUid() {
        return entityUid;
    }
    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }

    public String getCreatedDateTime() {
        return createdDateTime;
    }

    public String getNotificationType() {
        return notificationType;
    }

    public String getEntityType() {
        return entityType;
    }

    public boolean isRead() {
        return read;
    }

    public boolean isDelivered() {
        return delivered;
    }

    //to update the moel locally
    public void setIsRead(){
        read =true;
    }
}
