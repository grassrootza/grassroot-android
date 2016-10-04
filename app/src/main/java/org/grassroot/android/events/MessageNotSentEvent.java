package org.grassroot.android.events;

import org.grassroot.android.models.Message;

/**
 * Created by paballo on 2016/09/22.
 */

public class MessageNotSentEvent {

    private Message message;


    public MessageNotSentEvent(Message message){
        this.message = message;

    }

    public Message getMessage() {
        return message;
    }
}
