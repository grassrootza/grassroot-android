package org.grassroot.android.models;

import com.google.gson.annotations.SerializedName;

import io.realm.RealmList;
import io.realm.RealmObject;

/**
 * Created by paballo on 2016/05/04.
 */
public class GroupResponse extends RealmObject {
    private String status;
    private Integer code;
    private String message;
    @SerializedName("data")
    private RealmList<Group> groups = new RealmList<>();

    public GroupResponse() {
    }

    /**
     * @return The status
     */
    public String getStatus() {
        return status;
    }

    /**
     * @param status The status
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * @return The code
     */
    public Integer getCode() {
        return code;
    }

    /**
     * @param code The code
     */
    public void setCode(Integer code) {
        this.code = code;
    }

    /**
     * @return The message
     */
    public String getMessage() {
        return message;
    }

    /**
     * @param message The message
     */
    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * @return groups
     */
    public RealmList<Group> getGroups() {
        return groups;
    }

    /**
     * @param groups Set groups
     */
    public void setGroups(RealmList<Group> groups) {
        this.groups = groups;
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
