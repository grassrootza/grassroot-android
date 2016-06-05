package org.grassroot.android.services.model;

import com.google.gson.annotations.SerializedName;

/**
 * Created by paballo on 2016/05/20.
 */
public class EventResponse {
    private String status;
    private Integer code;
    private String message;
    @SerializedName("data")
    private EventModel eventModel;


    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public EventModel getEventModel() {
        return eventModel;
    }

    public void setEventModel(EventModel eventModel) {
        this.eventModel = eventModel;
    }

    @Override
    public String toString() {
        return "EventResponse{" +
                "status='" + status + '\'' +
                ", code=" + code +
                ", message='" + message + '\'' +
                ", eventModel=" + eventModel +
                '}';
    }
}
