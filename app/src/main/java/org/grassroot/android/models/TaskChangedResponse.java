package org.grassroot.android.models;

import io.realm.RealmList;

public class TaskChangedResponse {
  RealmList<TaskModel> addedAndUpdated = new RealmList<>();
  RealmList<RealmString> removedUids = new RealmList<>();

  public TaskChangedResponse() {

  }

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
    return "TaskChangedResponse{" +
        "addedAndUpdated=" + addedAndUpdated +
        ", removedUids=" + removedUids +
        '}';
  }
}
