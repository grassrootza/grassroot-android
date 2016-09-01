package org.grassroot.android.models.responses;

import com.google.gson.annotations.SerializedName;

import org.grassroot.android.models.Group;

import io.realm.RealmList;

/**
 * Created by paballo on 2016/05/04.
 */
public class GroupResponse extends AbstractResponse {

    @SerializedName("data")
    private RealmList<Group> groups = new RealmList<>();

    public RealmList<Group> getGroups() {
        return groups;
    }

    @Override
    public String toString() {
        return "GroupResponse{" +
            "status='" + status + '\'' +
            ", code=" + code +
            ", message='" + message + '\'' +
            ", groups=" + groups.toString() +
            '}';
    }
}
