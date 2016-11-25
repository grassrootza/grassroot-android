package org.grassroot.android.utils;

import org.grassroot.android.BuildConfig;
import org.grassroot.android.R;
import org.grassroot.android.services.ApplicationLoader;

import java.text.SimpleDateFormat;

/**
 * Created by ravi on 7/4/16.
 */
public class Constant {

    public static final String STAGING = "staging";
    public static final String PROD = "production";

    public static long mediumDelay = 500L;
    public static long serverSyncDelay = 3000L; // leave a decent margin (but also get optimistic locking more robust on server)

    public static final SimpleDateFormat isoDateTimeSDF = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

    private static final String productionUrl = ApplicationLoader.applicationContext
        .getString(R.string.production_url);
    private static final String stagingUrl = ApplicationLoader.applicationContext
        .getString(R.string.staging_url);
    private static final String localUrl = ApplicationLoader.applicationContext
        .getString(R.string.local_url);

    private static final String localBrokerUrl = ApplicationLoader.applicationContext
            .getString(R.string.localBrokerUrl);
    public static final String stagingBrokerUrl = ApplicationLoader.applicationContext
            .getString(R.string.stagingBrokerUrl);
    private static final String productionBrokerUrl = ApplicationLoader.applicationContext
            .getString(R.string.productionBrokerUrl);

    private static final String localGcmSender = ApplicationLoader.applicationContext.getString(R.string.local_sender_id);
    private static final String localGcmProject = ApplicationLoader.applicationContext.getString(R.string.local_project_id);
    private static final String stagingGcmSender = ApplicationLoader.applicationContext.getString(R.string.staging_sender_id);
    private static final String stagingGcmProject = ApplicationLoader.applicationContext.getString(R.string.staging_project_id);
    private static final String prodGcmSender = ApplicationLoader.applicationContext.getString(R.string.prod_sender_id);
    private static final String prodGcmProject = ApplicationLoader.applicationContext.getString(R.string.prod_project_id);

    public static final String restUrl = BuildConfig.BUILD_TYPE.equals("debug") ? stagingUrl
            : BuildConfig.FLAVOR.equals(PROD) ? productionUrl : stagingUrl;

    public static final String brokerUrl = BuildConfig.BUILD_TYPE.equals("debug") ? stagingBrokerUrl
            : BuildConfig.FLAVOR.equals(PROD) ? productionBrokerUrl : productionBrokerUrl;

    public static final String gcmProjectId = BuildConfig.BUILD_TYPE.equals("debug") ? stagingGcmProject
            : BuildConfig.FLAVOR.equals(PROD) ? prodGcmProject : stagingGcmProject;

    public static final String gcmSenderId = BuildConfig.BUILD_TYPE.equals("debug") ? stagingGcmSender
            : BuildConfig.FLAVOR.equals(PROD) ? prodGcmSender : stagingGcmSender;

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
