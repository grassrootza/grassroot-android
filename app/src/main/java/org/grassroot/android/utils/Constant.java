package org.grassroot.android.utils;

import org.grassroot.android.BuildConfig;

import java.text.SimpleDateFormat;

/**
 * Created by ravi on 7/4/16.
 */
public class Constant {

    public static final String STAGING = "staging";
    public static final String PROD = "production";

    public static long mediumDelay = 500L;
    public static final SimpleDateFormat isoDateTimeSDF = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

    public static final String productionUrl = "https://app.grassroot.org.za/api/";
    public static final String stagingUrl = "https://staging.grassroot.org.za/api/";
    public static final String localUrl = "http://10.0.2.2:8080/api/";

    public static final String restUrl = BuildConfig.BUILD_TYPE.equals("debug") ? localUrl
            : BuildConfig.FLAVOR.equals(PROD) ? productionUrl : stagingUrl;

    public static final double testLatitude = 31.215263;
    public static final double testLongitude = 121.476291;

	/**
	 * CONSTANTS FOR INTENT PASSING
     */

    public static final String USER_TOKEN = "user_token";
    public static final String INDEX_FIELD = "index";
    public static final String PARENT_TAG_FIELD = "parentTag";
    public static final String SELECTED_MEMBERS_FIELD = "selectedMembers";
    public static final String SELECT_FIELD = "select_enabled";
    public static final String SHOW_HEADER_FLAG = "show_header";
    public static final String SHOW_ACTION_BUTTON_FLAG = "show_action_button";
    public static final String SUCCESS_MESSAGE = "success_message";
    public static final String TITLE = "title";
    public static final String BODY = "body";


}
