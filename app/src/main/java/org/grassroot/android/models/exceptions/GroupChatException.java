package org.grassroot.android.models.exceptions;

/**
 * Created by luke on 2016/12/10.
 */

public class GroupChatException extends RuntimeException {

    public static final String MQTT_EXCEPTION = "mqtt_exception";
    public static final String MISC_EXCEPTION = "misc_exception";

    private final String errorType;

    public GroupChatException(String errorType, String message) {
        super(message);
        this.errorType = errorType;
    }

    public String getErrorType() { return errorType; }

}
