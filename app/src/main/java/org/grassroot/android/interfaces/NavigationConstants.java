package org.grassroot.android.interfaces;

/**
 * Created by luke on 2016/06/03.
 */
public interface NavigationConstants {

    String HOME_OPEN_ON_NAV = "nav_index";

    String ITEM_SHOW_GROUPS = "show_groups";
    String ITEM_TASKS = "upcoming_tasks";
    String ITEM_NOTIFICATIONS = "notifications";
    String ITEM_JOIN_REQS = "join_requests";

    // note : the ints correspond to 0-indexed place in nav drawer, i.e., change values if change order
    int HOME_NAV_GROUPS = 0;
    int HOME_NAV_NOTIFICATIONS = 1;
    int HOME_NAV_TASKS = 2;
    int HOME_NAV_JOIN_REQUESTS = 3;

    // activity result codes

    int MANUAL_MEMBER_ENTRY = 101;
    int MANUAL_MEMBER_EDIT = 102;
    int SELECT_MEMBERS = 103;
    int ASK_CONTACT_PERMISSION = 104;
    int NETWORK_SETTINGS_DIALOG = 105;

    public static final int activityNetworkSettings = 20; //todo request code for network, will have to do it robustly

    public static final int alertAskForContactPermission = 91;

}
