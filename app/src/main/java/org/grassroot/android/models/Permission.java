package org.grassroot.android.models;

import io.realm.RealmObject;

/**
 * Created by luke on 2016/07/18.
 */
public class Permission extends RealmObject {

    private String groupUid;
    private String forRole;

    private String permissionName;
    private String permissionLabel;
    private String permissionDesc;
    private boolean permissionEnabled;

    public String getGroupUid() {
        return groupUid;
    }

    public void setGroupUid(String groupUid) {
        this.groupUid = groupUid;
    }

    public String getForRole() {
        return forRole;
    }

    public void setForRole(String forRole) {
        this.forRole = forRole;
    }

    public String getPermissionName() {
        return permissionName;
    }

    public void setPermissionName(String permissionName) {
        this.permissionName = permissionName;
    }

    public String getPermissionLabel() {
        return permissionLabel;
    }

    public void setPermissionLabel(String permissionLabel) {
        this.permissionLabel = permissionLabel;
    }

    public String getPermissionDesc() {
        return permissionDesc;
    }

    public void setPermissionDesc(String permissionDesc) {
        this.permissionDesc = permissionDesc;
    }

    public boolean isPermissionEnabled() {
        return permissionEnabled;
    }

    public void setPermissionEnabled(boolean permissionEnabled) {
        this.permissionEnabled = permissionEnabled;
    }

    @Override
    public String toString() {
        return "Permission{" +
                "groupUid='" + groupUid + '\'' +
                ", forRole='" + forRole + '\'' +
                ", permissionName='" + permissionName + '\'' +
                ", permissionLabel='" + permissionLabel + '\'' +
                ", permissionEnabled=" + permissionEnabled +
                '}';
    }
}
