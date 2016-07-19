package org.grassroot.android.events;

/**
 * Created by luke on 2016/07/17.
 */
public class GroupEditedEvent {

    public static final String RENAMED = "renamed";
    public static final String JOIN_CODE_OPENED = "join_opened";
    public static final String JOIN_CODE_CLOSED = "join_closed";
    public static final String ORGANIZER_ADDED = "organizer_added";
    public static final String ROLE_CHANGED = "role_changed";

    public static final String CHANGED_ONLINE = "changed_online";
    public static final String CHANGED_OFFLINE = "changed_offline";

    public final String editAction;
    public final String typeOfSave;
    public final String groupUid;
    public final String groupName;

    public GroupEditedEvent(String editAction, String typeOfSave, String groupUid, String groupName) {
        this.editAction = editAction;
        this.typeOfSave = typeOfSave;
        this.groupUid = groupUid;
        this.groupName = groupName;
    }

}
