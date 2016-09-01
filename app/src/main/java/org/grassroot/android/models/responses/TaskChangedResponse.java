package org.grassroot.android.models.responses;

import org.grassroot.android.models.TaskModel;

import java.util.ArrayList;
import java.util.List;

import io.realm.RealmList;

public class TaskChangedResponse {
  RealmList<TaskModel> addedAndUpdated = new RealmList<>();
  List<String> removedUids = new ArrayList<>();

  public TaskChangedResponse() {

  }

  public RealmList<TaskModel> getAddedAndUpdated() {
    return addedAndUpdated;
  }

  public void setAddedAndUpdated(RealmList<TaskModel> addedAndUpdated) {
    this.addedAndUpdated = addedAndUpdated;
  }

  public List<String> getRemovedUids() {
    return removedUids;
  }

  public void setRemovedUids(List<String> removedUids) {
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
