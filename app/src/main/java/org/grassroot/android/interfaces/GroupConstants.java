package org.grassroot.android.interfaces;

/**
 * Created by luke on 2016/06/01.
 */
public interface GroupConstants {

    // todo : migrate to these from strings in generic Constant class

    String OBJECT_FIELD = "groupObject";
    String UID_FIELD = "groupUid";
    String NAME_FIELD = "groupName";

    String NO_JOIN_CODE = "NONE";
    String JOIN_CODE = "joinCode";

    String MEETING_CALLED = "MEETING";
    String VOTE_CALLED = "VOTE";
    String TODO_CREATED = "TODO";

    String GROUP_CREATED = "CREATED";
    String MEMBER_ADDED = "MEMBER_ADDED";
    String GROUP_MOD_OTHER = "OTHER_CHANGE";

    String ROLE_GROUP_ORGANIZER  = "ROLE_GROUP_ORGANIZER";
    String ROLE_COMMITTEE_MEMBER = "ROLE_COMMITTEE_MEMBER";
    String ROLE_ORDINARY_MEMBER  = "ROLE_ORDINARY_MEMBER";

    String NO_GROUP_TASKS = "NO_GROUP_ACTIVITIES";

    String PERM_CREATE_MTG = "GROUP_PERMISSION_CREATE_GROUP_MEETING";
    String PERM_CALL_VOTE = "GROUP_PERMISSION_CREATE_GROUP_VOTE";
    String PERM_CREATE_TODO = "GROUP_PERMISSION_CREATE_LOGBOOK_ENTRY";

    String PERM_ADD_MEMBER = "GROUP_PERMISSION_ADD_GROUP_MEMBER";
    String PERM_VIEW_MEMBERS = "GROUP_PERMISSION_SEE_MEMBER_DETAILS";
    String PERM_GROUP_SETTNGS = "GROUP_PERMISSION_UPDATE_GROUP_DETAILS";
    String PERM_DEL_MEMBER = "GROUP_PERMISSION_DELETE_GROUP_MEMBER";

}
