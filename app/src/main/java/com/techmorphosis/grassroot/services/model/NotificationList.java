package com.techmorphosis.grassroot.services.model;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by paballo on 2016/05/18.
 */
public class NotificationList {
    private String status;
    private Integer code;
    private String message;
    @SerializedName("data")
    private NotificationWrapper notificationWrapper;

    public String getStatus() {
        return status;
    }

    public Integer getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }



    public NotificationWrapper getNotificationWrapper() {
        return notificationWrapper;
    }


}
