package org.grassroot.android.models;

import android.os.Parcel;
import android.os.Parcelable;
import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;
import java.text.ParseException;
import java.util.Comparator;
import java.util.Date;
import org.grassroot.android.interfaces.TaskConstants;
import org.grassroot.android.utils.Constant;

/**
 * Created by paballo on 2016/05/05.
 */
public class TaskModel extends RealmObject implements Parcelable, Comparable<TaskModel> {

  private static final String TAG = TaskModel.class.getCanonicalName();
  @PrimaryKey private String taskUid;
  private String title;
  private String location;
  private String description;
  private String createdByUserName;
  private String type;

  private String parentUid;

  private String deadline;
  private String deadlineISO;
  private Date deadlineDate;

  private Boolean hasResponded;
  private Boolean canAction;
  private boolean canEdit;
  private boolean createdByUser;

  private String reply;

  private Boolean wholeGroupAssigned;
  private int assignedMemberCount;

  private String completedYes;
  private String completedNo;
  private boolean canRespondYes;
  private boolean canRespondNo;
  private boolean canMarkCompleted;

  public TaskModel() {
  }

  public String getTaskUid() {
    return taskUid;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getLocation() {
    return location;
  }

  public String getName() {
    return createdByUserName;
  }

  public String getParentUid() {
    return parentUid;
  }

  public String getType() {
    return type;
  }

  public Date getDeadlineDate() {
    return overcomeJava7AndroidDateCrapness();
  }

  public boolean isInFuture() {
    return getDeadlineDate().after(new Date());
  }

  public Boolean hasResponded() {
    return hasResponded;
  }

  public Boolean canAction() {
    return canAction;
  }

  public boolean isCanEdit() {
    return canEdit;
  }

  public String getReply() {
    return reply;
  }

  public Boolean getWholeGroupAssigned() {
    return wholeGroupAssigned;
  }

  public int getAssignedMemberCount() {
    return assignedMemberCount;
  }

  public boolean isCanRespondYes() {
    return canRespondYes;
  }

  public boolean isCanRespondNo() {
    return canRespondNo;
  }

  public boolean isCanMarkCompleted() {
    return canMarkCompleted;
  }

  public boolean respondedYes() {
    return hasResponded && TaskConstants.RESPONSE_YES.equals(reply);
  }

  public boolean respondedNo() {
    return hasResponded && TaskConstants.RESPONSE_NO.equals(reply);
  }

  public boolean isCreatedByUser() {
    return createdByUser;
  }

  @Override public int describeContents() {
    return 0;
  }

  @Override public void writeToParcel(Parcel dest, int flags) {
    dest.writeString(this.taskUid);
    dest.writeString(this.title);
    dest.writeString(this.description);
    dest.writeString(this.location);
    dest.writeString(this.parentUid);
    dest.writeString(this.createdByUserName);
    dest.writeString(this.type);
    dest.writeString(this.deadline);
    dest.writeString(this.deadlineISO);
    dest.writeInt(this.hasResponded ? 1 : 0);
    dest.writeInt(this.canAction ? 1 : 0);
    dest.writeInt(this.canEdit ? 1 : 0);
    dest.writeInt(this.wholeGroupAssigned ? 1 : 0);
    dest.writeInt(this.assignedMemberCount);
    dest.writeInt(this.canEdit ? 1 : 0);
    dest.writeString(this.reply);
    dest.writeString(this.completedYes);
    dest.writeString(this.completedNo);
  }

  protected TaskModel(Parcel in) {
    this.taskUid = in.readString();
    this.title = in.readString();
    this.description = in.readString();
    this.location = in.readString();
    this.parentUid = in.readString();
    this.createdByUserName = in.readString();
    this.type = in.readString();
    this.deadline = in.readString();
    this.deadlineISO = in.readString();
    this.hasResponded = in.readInt() != 0;
    this.canAction = in.readInt() != 0;
    this.canEdit = in.readInt() != 0;
    this.wholeGroupAssigned = in.readInt() != 0;
    this.assignedMemberCount = in.readInt();
    this.canEdit = in.readInt() != 0;
    this.reply = in.readString();
    this.completedYes = in.readString();
    this.completedNo = in.readString();
    resetResponseFlags();
  }

  public void resetResponseFlags() {
    this.canRespondYes = canAction && !(hasResponded && "YES".equalsIgnoreCase(reply));
    this.canRespondNo = canAction && !(hasResponded && "NO".equalsIgnoreCase(reply));
    this.canMarkCompleted = canAction && !(hasResponded && "COMPLETED".equalsIgnoreCase(reply));
  }

  private Date overcomeJava7AndroidDateCrapness() {
    if (deadlineDate == null) {
      try {
        deadlineDate = Constant.isoDateTimeSDF.parse(deadlineISO);
      } catch (ParseException e) {
        throw new UnsupportedOperationException("Sorry, couldn't overcome the crapness");
      }
    }
    return deadlineDate;
  }

  public static final Creator<TaskModel> CREATOR = new Creator<TaskModel>() {
    @Override public TaskModel createFromParcel(Parcel in) {
      return new TaskModel(in);
    }

    @Override public TaskModel[] newArray(int size) {
      return new TaskModel[size];
    }
  };

  @Override public int compareTo(TaskModel task2) {
    return getDeadlineDate().compareTo(task2.getDeadlineDate());
  }

  public static Comparator<TaskModel> canRespondComparator = new Comparator<TaskModel>() {
    @Override public int compare(TaskModel taskModel, TaskModel t1) {
      return (!taskModel.hasResponded && t1.hasResponded) ? 1
          : (taskModel.hasResponded && !t1.hasResponded) ? -1
              : -(taskModel.compareTo(t1)); // so it's reverse order on the date
    }
  };

  @Override public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    TaskModel taskModel = (TaskModel) o;

    return taskUid != null ? taskUid.equals(taskModel.taskUid) : taskModel.taskUid == null;
  }

  @Override public int hashCode() {
    return taskUid != null ? taskUid.hashCode() : 0;
  }

  @Override public String toString() {
    return "TaskModel{" +
        "type='" + type + '\'' +
        ", canEdit=" + canEdit +
        '}';
  }
}