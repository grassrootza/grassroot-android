package org.grassroot.android.models.responses;

import org.grassroot.android.models.TaskNotification;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by paballo on 2016/05/20.
 */
public class NotificationWrapper {

    private List<TaskNotification> notifications = new ArrayList<>();

    private Integer pageNumber;
    private Integer nextPage;
    private Integer totalPages;

    public List<TaskNotification> getNotifications() {
        return notifications;
    }

    public Integer getPageNumber() {
        return pageNumber;
    }

    public Integer getTotalPages() {
        return totalPages;
    }

    public Integer getNextPage() {
        return nextPage;
    }
}
