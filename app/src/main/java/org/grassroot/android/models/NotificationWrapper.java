package org.grassroot.android.models;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by paballo on 2016/05/20.
 */
public class NotificationWrapper {

    //todo I messed up in the backend, this class should not exist

    private List<Notification> notifications = new ArrayList<>();

    private Integer pageNumber;
    private Integer nextPage;
    private Integer totalPages;

    public List<Notification> getNotifications() {
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
