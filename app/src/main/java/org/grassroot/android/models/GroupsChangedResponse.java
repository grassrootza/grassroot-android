package org.grassroot.android.models;

import io.realm.RealmList;
import io.realm.RealmObject;

/**
 * Created by luke on 2016/07/13.
 */
public class GroupsChangedResponse extends RealmObject {

    RealmList<Group> addedAndUpdated = new RealmList<>();
    RealmList<RealmString> removedUids = new RealmList<>();

    public GroupsChangedResponse() {

    }

    public RealmList<Group> getAddedAndUpdated() {
        return addedAndUpdated;
    }

    public void setAddedAndUpdated(RealmList<Group> addedAndUpdated) {
        this.addedAndUpdated = addedAndUpdated;
    }

    public RealmList<RealmString> getRemovedUids() {
        return removedUids;
    }

    public void setRemovedUids(RealmList<RealmString> removedUids) {
        this.removedUids = removedUids;
    }

    @Override
    public String toString() {
        return "GroupsChangedResponse{" +
                "addedAndUpdated=" + addedAndUpdated +
                ", removedUids=" + removedUids +
                '}';
    }
}
