package org.grassroot.android.events;

/**
 * Created by luke on 2016/07/22.
 */
public class GroupDeletedEvent {
    public final String groupUid;

    public GroupDeletedEvent(String groupUid){
        this.groupUid = groupUid;
    }
}
