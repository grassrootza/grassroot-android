package org.grassroot.android.events;

import android.os.Bundle;

import org.grassroot.android.models.Message;

/**
 * Created by paballo on 2016/09/02.
 */
public class GroupChatEvent {
    private String groupUid;
    private Bundle bundle;
    private Message message;

    public GroupChatEvent(String groupUid, Message message) {
        this.groupUid = groupUid;
        this.message = message;
    }

    public GroupChatEvent(String groupUid, Bundle bundle, Message message){
        this.groupUid = groupUid;
        this.bundle = bundle;
        this.message = message;
    }

    public String getGroupUid() {
        return groupUid;
    }

    public Bundle getBundle() {
        return bundle;
    }

    public Message getMessage() {
        return message;
    }
}
