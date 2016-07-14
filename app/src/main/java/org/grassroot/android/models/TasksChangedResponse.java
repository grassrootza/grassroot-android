package org.grassroot.android.models;

import io.realm.RealmList;
import io.realm.RealmObject;

/**
 * Created by luke on 2016/07/14.
 */
public class TasksChangedResponse extends RealmObject {

    RealmList<TaskModel> addedAndUpdated = new RealmList<>();
    RealmList<RealmString> removedUids = new RealmList<>();

    public TasksChangedResponse() { }

    public RealmList<TaskModel> getAddedAndUpdated() {
        return addedAndUpdated;
    }

    public void setAddedAndUpdated(RealmList<TaskModel> addedAndUpdated) {
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
        return "TasksChangedResponse{" +
                "addedAndUpdated=" + addedAndUpdated +
                ", removedUids=" + removedUids +
                '}';
    }
}
