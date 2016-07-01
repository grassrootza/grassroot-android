package org.grassroot.android.models;

import com.google.gson.annotations.SerializedName;

import io.realm.RealmList;
import io.realm.RealmObject;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by luke on 2016/05/05.
 * todo: superclass some of these, like status, code
 */
public class MemberList extends RealmObject{
    private String status;
    private Integer code;
    private String message;
    @SerializedName("data")
    private RealmList<Member> members = new RealmList<>();

    // GETTERS

    public String getStatus() {
        return status;
    }

    public Integer getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public List<Member> getMembers() {
        return members;
    }

    public void setMembers(RealmList<Member> members) { this.members = members; }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("MemberList=");
        for (Member m : members)
            sb.append("Member=" + m.toString());
        return sb.toString();
    }
}
