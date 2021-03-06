package org.grassroot.android.models;

import android.support.annotation.NonNull;

import java.util.UUID;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

/**
 * Created by paballo on 2016/05/05.
 */
public class PublicGroupModel extends RealmObject {

    @NonNull
    @PrimaryKey
    private String id;

    private String groupName;
    private String description;
    private String groupCreator;
    private Integer memberCount;

    private boolean termInName;
    private boolean hasOpenRequest;
    private boolean isJoinReqLocal;

    private String createdDate;

    public PublicGroupModel() {
        id = UUID.randomUUID().toString(); // to enforce non-null behaviour, but this should be sent from server
    }

    @NonNull public String getId() {
        return id;
    }

    public void setId(@NonNull String id) {
        this.id = id;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getGroupCreator() {
        return groupCreator;
    }

    public void setGroupCreator(String groupCreator) {
        this.groupCreator = groupCreator;
    }

    public Integer getMemberCount() {
        return memberCount;
    }

    public void setMemberCount(Integer memberCount) {
        this.memberCount = memberCount;
    }

    public boolean isJoinReqLocal() {
        return isJoinReqLocal;
    }

    public void setJoinReqLocal(boolean joinReqLocal) {
        isJoinReqLocal = joinReqLocal;
    }

    public boolean isTermInName() {
        return termInName;
    }

    public void setTermInName(boolean termInName) {
        this.termInName = termInName;
    }

    public boolean isHasOpenRequest() {
        return hasOpenRequest;
    }

    public void setHasOpenRequest(boolean hasOpenRequest) {
        this.hasOpenRequest = hasOpenRequest;
    }

    public String getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(String createdDate) {
        this.createdDate = createdDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PublicGroupModel that = (PublicGroupModel) o;

        return id.equals(that.id);

    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "PublicGroupModel{" +
            "groupName='" + groupName + '\'' +
            ", description='" + description + '\'' +
            '}';
    }
}