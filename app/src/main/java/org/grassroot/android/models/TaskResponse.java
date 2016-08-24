package org.grassroot.android.models;

import com.google.gson.annotations.SerializedName;

import io.realm.RealmList;
import io.realm.RealmObject;

/**
 * Created by paballo on 2016/05/05.
 */
public class TaskResponse extends RealmObject {

  private String status;
  private Integer code;
  private String message;
  @SerializedName("data")
  private RealmList<TaskModel> tasks = new RealmList<>();

  /**
   *
   * @return
   * The status
   */
  public String getStatus() {
    return status;
  }

  /**
   *
   * @param status
   * The status
   */
  public void setStatus(String status) {
    this.status = status;
  }

  /**
   *
   * @return
   * The code
   */
  public Integer getCode() {
    return code;
  }

  /**
   *
   * @param code
   * The code
   */
  public void setCode(Integer code) {
    this.code = code;
  }

  /**
   *
   * @return
   * The message
   */
  public String getMessage() {
    return message;
  }

  /**
   *
   * @param message
   * The message
   */
  public void setMessage(String message) {
    this.message = message;
  }

  /**
   *
   * @return
   * The data
   */
  public RealmList<TaskModel> getTasks() {
    return tasks;
  }

  /**
   *
   * @param tasks
   * The data
   */
  public void setTasks(RealmList<TaskModel> tasks) {
    this.tasks = tasks;
  }

}