package org.grassroot.android.models;

import io.realm.RealmObject;

/**
 * Created by paballo on 2016/05/18.
 */
public class TaskNotification extends RealmObject {

    private String uid;
    private String entityUid;

    private String title;
    private String message;

    private String createdDateTime; //todo change to iso datetime server side
    private String notificationType;
    private String entityType;

    private String clickAction;
    private int priority;

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

    public String getClickAction() { return clickAction; }

    public int getPriority() { return priority; }

    public boolean isRead() {
        return read;
    }

    public boolean isDelivered() {
        return delivered;
    }

    // to update the model locally
    public void setIsRead(){
        read =true;
    }

}
