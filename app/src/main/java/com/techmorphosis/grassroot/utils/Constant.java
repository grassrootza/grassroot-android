package com.techmorphosis.grassroot.utils;

import com.techmorphosis.grassroot.BuildConfig;

import java.text.SimpleDateFormat;

/**
 * Created by ravi on 7/4/16.
 */
public class Constant {

    // todo: implement good practice and use getters

    public static final String stagingUrl = "http://staging.grassroot.org.za/api/";
    public static final String localUrl = "http://10.0.2.2:8080/api/";

    public static final String restUrl = BuildConfig.BUILD_TYPE.equals("release") ? stagingUrl : localUrl;

    public static final String filteredList = "filteredList";
    public static final  String VotedmemberList="VotedmemberList"; // todo: as with others, probably not needed

    public static final String contactsAdded = "contactsAdded";
    public static final String contactsRemoved = "contactsRemoved";
    public static final String doNotDisplayContacts = "doNotDisplayContacts";

    public static final String GROUPUID_FIELD = "groupUid";
    public static final String GROUPNAME_FIELD = "groupName";
    public static final String INDEX_FIELD = "index";
    public static final String PARENT_TAG_FIELD = "parentTag";
    public static final String SELECTED_MEMBERS_FIELD = "selectedMembers";
    public static final String SELECT_FIELD = "select_enabled";
    public static final String SHOW_HEADER_FLAG = "show_header";
    public static final String SHOW_ACTION_BUTTON_FLAG = "show_action_button";
    public static final String SUCCESS_MESSAGE = "success_message";

    public static final String VOTE = "VOTE";
    public static final String MEETING = "MEETING";
    public static final String TODO = "TODO";

    public static final String ROLE_GROUP_ORGANIZER  = "ROLE_GROUP_ORGANIZER";
    public static final String ROLE_COMMITTEE_MEMBER = "ROLE_COMMITTEE_MEMBER";
    public static final String ROLE_ORDINARY_MEMBER  = "ROLE_ORDINARY_MEMBER";

    public static final String NO_GROUP_TASKS = "NO_GROUP_ACTIVITIES";

    public static final SimpleDateFormat isoDateTimeSDF = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

    /**
     * RESULT CODES FOR ACTIVITIES
     * todo : make an enum with to/from int
     */
    public static final int activityContactSelection = 1;
    public static final int activityManualMemberEntry = 2;
    public static final int activitySelectNumberFromContact = 3;
    public static final int activityAddMembersToGroup = 4;
    public static final int activityRemoveMembers = 5;
    public static final int activityCreateGroup = 6;
    public static final int activitySelectGroupMembers = 7;
    public static final int activityCallMeeting = 8;

    public static final int alertAskForContactPermission = 91;

    public static final double testLatitude = 31.215263;
    public static final double testLongitude = 121.476291;
}
