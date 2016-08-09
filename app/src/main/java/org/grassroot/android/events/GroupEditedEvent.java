package org.grassroot.android.events;

/**
 * Created by luke on 2016/07/17.
 */
public class GroupEditedEvent {

    public static final String MEMBERS_ADDED = "members_added";
    public static final String MEMBERS_REMOVED = "members_removed";
    public static final String RENAMED = "renamed";
    public static final String IMAGE_UPLOADED = "image_uploaded";
    public static final String IMAGE_TO_DEFAULT = "image_removed";
    public static final String PUBLIC_STATUS_CHANGED = "changed_public";
    public static final String JOIN_CODE_OPENED = "join_opened";
    public static final String JOIN_CODE_CLOSED = "join_closed";
    public static final String ORGANIZER_ADDED = "organizer_added";
    public static final String ROLE_CHANGED = "role_changed";
    public static final String MULTIPLE_TO_SERVER = "member_to_server";

    public final String editAction;
    public String typeOfSave;
    public final String groupUid;
    public final String auxString;

    public GroupEditedEvent(String editAction, String groupUid) {
        this.editAction = editAction;
        this.groupUid = groupUid;
        this.typeOfSave = "";
        this.auxString = "";
    }

    public GroupEditedEvent(String editAction, String typeOfSave, String groupUid, String auxString) {
        this.editAction = editAction;
        this.typeOfSave = typeOfSave;
        this.groupUid = groupUid;
        this.auxString = auxString;
    }

    public void setTypeOfSave(String typeOfSave) {
        this.typeOfSave = typeOfSave;
    }

    @Override
    public String toString() {
        return "GroupEditedEvent{" +
                "editAction='" + editAction + '\'' +
                ", typeOfSave='" + typeOfSave + '\'' +
                ", auxString='" + auxString + '\'' +
                ", groupUid='" + groupUid + '\'' +
                '}';
    }
}
