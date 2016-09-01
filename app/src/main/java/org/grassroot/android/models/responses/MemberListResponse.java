package org.grassroot.android.models.responses;

import com.google.gson.annotations.SerializedName;

import org.grassroot.android.models.Member;

import java.util.ArrayList;
import java.util.List;

import io.realm.RealmList;

/**
 * Created by luke on 2016/05/05.
 */
public class MemberListResponse extends AbstractResponse {

    @SerializedName("data")
    private List<Member> members = new ArrayList<>();

    public List<Member> getMembers() {
        return members;
    }

    public void setMembers(RealmList<Member> members) { this.members = members; }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("MemberListResponse=");
        for (Member m : members)
            sb.append("Member=" + m.toString());
        return sb.toString();
    }
}
