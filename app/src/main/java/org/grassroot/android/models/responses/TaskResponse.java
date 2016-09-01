package org.grassroot.android.models.responses;

import com.google.gson.annotations.SerializedName;

import org.grassroot.android.models.TaskModel;

import io.realm.RealmList;

/**
 * Created by paballo on 2016/05/05.
 */
public class TaskResponse extends AbstractResponse {

  @SerializedName("data")
  private RealmList<TaskModel> tasks = new RealmList<>();

  public RealmList<TaskModel> getTasks() {
    return tasks;
  }

  public void setTasks(RealmList<TaskModel> tasks) {
    this.tasks = tasks;
  }

}