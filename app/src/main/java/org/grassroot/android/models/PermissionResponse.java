package org.grassroot.android.models;

import com.google.gson.annotations.SerializedName;

import java.util.List;

import io.realm.RealmList;

/**
 * Created by luke on 2016/07/18.
 */
public class PermissionResponse {

    private String status;
    private Integer code;
    private String message;
    @SerializedName("data")
    List<Permission> permissions = new RealmList<>();

    public PermissionResponse() { }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<Permission> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<Permission> permissions) {
        this.permissions = permissions;
    }
}
