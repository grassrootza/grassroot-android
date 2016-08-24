package org.grassroot.android.models;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import org.grassroot.android.interfaces.TaskConstants;
import org.grassroot.android.utils.Constant;

import java.text.ParseException;
import java.util.Comparator;
import java.util.Date;

import io.realm.RealmList;
import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

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

  private boolean isParentLocal;
  private String parentUid;
  private String parentName;

  private long updateTime;
  private String deadline;
  private String deadlineISO;
  private Date deadlineDate;

  private boolean hasResponded;
  private boolean canAction;
  private boolean canEdit;
  private boolean createdByUser;

  private String reply;

  private boolean wholeGroupAssigned;
  private int assignedMemberCount;

  private String completedYes;
  private String completedNo;
  private boolean canRespondYes;
  private boolean canRespondNo;
  private boolean canMarkCompleted;
  private int minutes;

  private boolean isLocal;
  private boolean isEdited;
  private boolean isActionLocal;

  private RealmList<RealmString> memberUIDS;

  public TaskModel() {
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

  @Override
  public String toString() {
    return "TaskModel{" +
        "title='" + title + '\'' +
        ", type='" + type + '\'' +
        ", hasResponded=" + hasResponded +
        ", canAction=" + canAction +
        ", canEdit=" + canEdit +
        ", reply='" + reply + '\'' +
        '}';
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
    dest.writeString(this.parentName);
    dest.writeString(this.createdByUserName);
    dest.writeString(this.type);
    dest.writeString(this.deadline);
    dest.writeString(this.deadlineISO);
    dest.writeLong(this.deadlineDate.getTime());
    dest.writeInt(this.hasResponded ? 1 : 0);
    dest.writeInt(this.canAction ? 1 : 0);
    dest.writeInt(this.canEdit ? 1 : 0);
    dest.writeInt(this.wholeGroupAssigned ? 1 : 0);
    dest.writeInt(this.assignedMemberCount);
    dest.writeInt(this.canEdit ? 1 : 0);
    dest.writeString(this.reply);
    dest.writeString(this.completedYes);
    dest.writeString(this.completedNo);
    dest.writeInt(this.isLocal ? 1 : 0);
    dest.writeInt(this.isEdited ? 1 : 0);
    dest.writeInt(this.isParentLocal ? 1 : 0);
  }

  protected TaskModel(Parcel in) {
    this.taskUid = in.readString();
    this.title = in.readString();
    this.description = in.readString();
    this.location = in.readString();
    this.parentUid = in.readString();
    this.parentName = in.readString();
    this.createdByUserName = in.readString();
    this.type = in.readString();
    this.deadline = in.readString();
    this.deadlineISO = in.readString();
    this.deadlineDate = new Date(in.readLong());
    this.hasResponded = in.readInt() != 0;
    this.canAction = in.readInt() != 0;
    this.canEdit = in.readInt() != 0;
    this.wholeGroupAssigned = in.readInt() != 0;
    this.assignedMemberCount = in.readInt();
    this.canEdit = in.readInt() != 0;
    this.reply = in.readString();
    this.completedYes = in.readString();
    this.completedNo = in.readString();
    this.isLocal = in.readInt() != 0;
    this.isEdited = in.readInt() != 0;
    this.isParentLocal = in.readInt() != 0;
    resetResponseFlags();
  }

  public void resetResponseFlags() {
    this.canRespondYes = canAction && !(hasResponded && "YES".equalsIgnoreCase(reply));
    this.canRespondNo = canAction && !(hasResponded && "NO".equalsIgnoreCase(reply));
    this.canMarkCompleted = canAction && !(hasResponded && "COMPLETED".equalsIgnoreCase(reply));
  }

  private Date parseDateFromIsoString() {
    if (deadlineDate == null) {
      try {
        deadlineDate = Constant.isoDateTimeSDF.parse(deadlineISO);
      } catch (ParseException e) {
        Log.e(TAG, "error obtaining date");
      }
    }
    return deadlineDate;
  }

  // assumes searchTerm is lower case (may be an issue once have unicode weird chars, at which point may need expensive regex ...)
  public boolean containsString(final String searchTerm) {
    return type.toLowerCase().contains(searchTerm)
        || title.toLowerCase().contains(searchTerm)
        || createdByUserName.toLowerCase().contains(searchTerm)
        || parentName.toLowerCase().contains(searchTerm)
        || (description != null && description.toLowerCase().contains(searchTerm))
        || (location != null && location.toLowerCase().contains(searchTerm));
  }

  public RealmList<RealmString> getMemberUIDS() {
    return memberUIDS;
  }

  public void setMemberUIDS(RealmList<RealmString> memberUIDS) {
    this.memberUIDS = memberUIDS;
  }

  public boolean isEdited() {
    return isEdited;
  }

  public void setEdited(boolean edited) {
    isEdited = edited;
  }

  public boolean isLocal() {
    return isLocal;
  }

  public void setLocal(boolean local) {
    isLocal = local;
  }

  public int getMinutes() {
    return minutes;
  }

  public void setMinutes(int minutes) {
    this.minutes = minutes;
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

  public String getParentName() { return parentName; }

  public String getType() {
    return type;
  }

  public boolean isActionLocal() {
    return isActionLocal;
  }

  public void setActionLocal(boolean actionLocal) {
    isActionLocal = actionLocal;
  }

  public void calcDeadlineDate() {
    if (deadlineDate == null) {
      deadlineDate = parseDateFromIsoString();
    }
  }

  public Date getDeadlineDate() {
    if (deadlineDate == null) {
      deadlineDate = parseDateFromIsoString();
    }
    return deadlineDate;
  }

  public boolean isInFuture() {
    return getDeadlineDate().after(new Date());
  }

  public boolean hasResponded() {
    return hasResponded;
  }

  public boolean canAction() {
    return canAction;
  }

  public boolean isCanEdit() {
    return canEdit;
  }

  public String getReply() {
    return reply;
  }

  public boolean getWholeGroupAssigned() {
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

  public long getUpdateTime() {
    return updateTime;
  }

  public void setUpdateTime(long updateTime) {
    this.updateTime = updateTime;
  }

  public boolean isParentLocal() {
    return isParentLocal;
  }

  public void setParentLocal(boolean parentLocal) {
    isParentLocal = parentLocal;
  }

  public void setDeadlineDate(Date deadlineDate) { this.deadlineDate = deadlineDate; }

  public void setDeadlineISO(String deadlineISO) {
    this.deadlineISO = deadlineISO;
  }

  public void setTaskUid(String taskUid) {
    this.taskUid = taskUid;
  }

  public void setLocation(String location) {
    this.location = location;
  }

  public void setCreatedByUserName(String createdByUserName) {
    this.createdByUserName = createdByUserName;
  }

  public void setType(String type) {
    this.type = type;
  }

  public void setParentUid(String parentUid) {
    this.parentUid = parentUid;
  }

  public void setParentName(String parentName) { this.parentName = parentName; }

  public void setDeadline(String deadline) {
    this.deadline = deadline;
  }

  public String getDeadlineISO() {
    return deadlineISO;
  }

  public void setReply(String reply) {
    this.reply = reply;
  }

  public String getCreatedByUserName() {
    return createdByUserName;
  }

  public boolean isHasResponded() {
    return hasResponded;
  }

  public void setHasResponded(boolean hasResponded) {
    this.hasResponded = hasResponded;
  }

  public boolean isCanAction() {
    return canAction;
  }

  public void setCanAction(boolean canAction) {
    this.canAction = canAction;
  }

  public void setCanEdit(boolean canEdit) {
    this.canEdit = canEdit;
  }

  public void setCreatedByUser(boolean createdByUser) {
    this.createdByUser = createdByUser;
  }

  public boolean isWholeGroupAssigned() {
    return wholeGroupAssigned;
  }

  public void setWholeGroupAssigned(boolean wholeGroupAssigned) {
    this.wholeGroupAssigned = wholeGroupAssigned;
  }

  public void setAssignedMemberCount(int assignedMemberCount) {
    this.assignedMemberCount = assignedMemberCount;
  }

  public String getCompletedYes() {
    return completedYes;
  }

  public void setCompletedYes(String completedYes) {
    this.completedYes = completedYes;
  }

  public String getCompletedNo() {
    return completedNo;
  }

  public void setCompletedNo(String completedNo) {
    this.completedNo = completedNo;
  }

  public void setCanRespondYes(boolean canRespondYes) {
    this.canRespondYes = canRespondYes;
  }

  public void setCanRespondNo(boolean canRespondNo) {
    this.canRespondNo = canRespondNo;
  }

  public void setCanMarkCompleted(boolean canMarkCompleted) {
    this.canMarkCompleted = canMarkCompleted;
  }
}