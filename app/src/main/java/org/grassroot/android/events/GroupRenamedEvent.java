package org.grassroot.android.events;

/**
 * Created by luke on 2016/07/17.
 */
public class GroupRenamedEvent {

    public String groupUid;
    public String groupName;

    public GroupRenamedEvent(String groupUid, String groupName) {
        this.groupUid = groupUid;
        this.groupName = groupName;
    }

}
