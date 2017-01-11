package org.grassroot.android.events;

/**
 * Created by luke on 2016/12/10.
 */

public class GroupChatErrorEvent {

    public final static String CONNECT_ERROR = "connect";
    public final static String SEND_ERROR = "send";
    public final static String MISC_ERROR = "misc";

    private final String type;
    private final String description;

    public GroupChatErrorEvent(String type, String description) {
        this.type = type;
        this.description = description;
    }

    public String getType() {
        return type;
    }

    public String getDescription() {
        return description;
    }
}
