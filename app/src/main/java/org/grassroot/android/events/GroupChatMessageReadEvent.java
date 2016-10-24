package org.grassroot.android.events;

import org.grassroot.android.models.Message;

/**
 * Created by paballo on 2016/10/24.
 */

public class GroupChatMessageReadEvent {

    private Message message;

    public GroupChatMessageReadEvent(Message message){
        this.message = message;
    }

    public Message getMessage() {
        return message;
    }
}
