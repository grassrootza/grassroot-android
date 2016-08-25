package org.grassroot.android.models;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

/**
 * Created by paballo on 2016/05/18.
 */
public class TaskNotification extends RealmObject {

    @PrimaryKey
    private String uid;

    private String entityUid;
    private String title;
    private String message;

    private String groupUid;
    private String defaultImage;
    private String imageUrl;

    private String createdDateTime;
    private String notificationType;
    private String entityType;

    private String clickAction;
    private int priority;

    private boolean read;
    private boolean delivered;
    private boolean viewedAndroid;

    private boolean toChangeOnServer;


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

    public String getGroupUid() { return groupUid; }

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

    public void setRead(boolean read) { this.read = read; }

    public void setIsRead(){
        read =true;
    }

    public boolean isViewedAndroid() {
        return viewedAndroid;
    }

    public void setViewedAndroid(boolean viewedAndroid) {
        this.viewedAndroid = viewedAndroid;
    }

    public void setToChangeOnServer(boolean toChangeOnServer) { this.toChangeOnServer = toChangeOnServer; }

    public boolean isToChangeOnServer() { return toChangeOnServer; }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getDefaultImage() {
        return defaultImage;
    }

    // note : assumes query text has been shifted to lower case prior to passing
    public boolean containsText(String queryText) {
        return entityType.toLowerCase().contains(queryText) ||
            title.toLowerCase().contains(queryText) ||
            message.toLowerCase().contains(queryText);
    }

}
