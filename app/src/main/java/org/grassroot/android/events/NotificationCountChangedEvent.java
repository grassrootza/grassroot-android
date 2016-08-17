package org.grassroot.android.events;

/**
 * Created by paballo on 2016/06/08.
 */
public class NotificationCountChangedEvent {

    private final  Integer notificationCount;

    public NotificationCountChangedEvent(int notificationCount){
        this.notificationCount = notificationCount;
    }

    public Integer getNotificationCount() {
        return notificationCount;
    }
}
