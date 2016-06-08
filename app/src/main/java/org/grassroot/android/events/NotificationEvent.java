package org.grassroot.android.events;

/**
 * Created by paballo on 2016/06/08.
 */
public class NotificationEvent {

    private final  Integer notificationCount;

    public NotificationEvent(int notificationCount){
        this.notificationCount =notificationCount;
    }

    public Integer getNotificationCount() {
        return notificationCount;
    }
}
