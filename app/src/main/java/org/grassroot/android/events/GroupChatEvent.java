package org.grassroot.android.events;

import android.os.Bundle;

/**
 * Created by paballo on 2016/09/02.
 */
public class GroupChatEvent {
    private String groupUid;
    private Bundle bundle;

    public GroupChatEvent(String groupUid, Bundle bundle){
        this.groupUid = groupUid;
        this.bundle = bundle;
    }

    public String getGroupUid() {
        return groupUid;
    }

    public Bundle getBundle() {
        return bundle;
    }
}
