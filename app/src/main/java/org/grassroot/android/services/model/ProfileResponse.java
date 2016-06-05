package org.grassroot.android.services.model;

import com.google.gson.annotations.SerializedName;

/**
 * Created by paballo on 2016/05/19.
 */
public class ProfileResponse {


    private String status;
    private Integer code;
    private String message;

    @SerializedName("data")
    private Profile profile;

    public Profile getProfile() {
        return profile;
    }

    public String getStatus() {
        return status;
    }

    public Integer getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

}
