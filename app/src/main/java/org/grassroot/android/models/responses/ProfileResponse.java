package org.grassroot.android.models.responses;

import com.google.gson.annotations.SerializedName;

import org.grassroot.android.models.Profile;

/**
 * Created by paballo on 2016/05/19.
 */
public class ProfileResponse extends AbstractResponse {

    @SerializedName("data")
    private Profile profile;

    public Profile getProfile() {
        return profile;
    }

}
