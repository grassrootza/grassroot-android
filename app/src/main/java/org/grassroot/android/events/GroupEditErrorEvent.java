package org.grassroot.android.events;

import okhttp3.ResponseBody;

/**
 * Created by luke on 2016/07/17.
 */
public class GroupEditErrorEvent {

    public static final String REST_ERROR = "rest_error";
    public static final String IO_ERROR = "io_error";

    public final String type;
    public final ResponseBody errorBody;
    public final Throwable thrown;

    public GroupEditErrorEvent(ResponseBody errorBody) {
        this.type = REST_ERROR;
        this.errorBody = errorBody;
        this.thrown = null;
    }

    public GroupEditErrorEvent(Throwable t) {
        this.type = IO_ERROR;
        this.errorBody = null;
        this.thrown = t;
    }

}
