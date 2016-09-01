package org.grassroot.android.models.responses;

import com.google.gson.annotations.SerializedName;

import org.grassroot.android.models.Permission;

import java.util.List;

import io.realm.RealmList;

/**
 * Created by luke on 2016/07/18.
 */
public class PermissionResponse extends AbstractResponse {

    @SerializedName("data")
    List<Permission> permissions = new RealmList<>();

    public List<Permission> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<Permission> permissions) {
        this.permissions = permissions;
    }
}
