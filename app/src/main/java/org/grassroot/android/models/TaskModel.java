package org.grassroot.android.models;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import org.grassroot.android.interfaces.TaskConstants;
import org.grassroot.android.utils.Constant;

import java.text.ParseException;
import java.util.Comparator;
import java.util.Date;

/**
 * Created by paballo on 2016/05/05.
 */
public class TaskModel implements Parcelable, Comparable<TaskModel> {

    private static final String TAG = TaskModel.class.getCanonicalName();

    private String taskUid;
    private String title;
    private String location;
    private String description;
    private String name;
    private String type;

    private String parentUid;
    private String parentName;

    private String deadline;
    private String deadlineISO;
    private Date deadlineDate;

    private Boolean hasResponded;
    private Boolean canAction;
    private boolean canEdit;
    private String reply;

    private Boolean wholeGroupAssigned;
    private Integer memberCount;

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

    public String getLocation() { return location; }

    public String getName() {
        return name;
    }

    public String getParentUid() { return parentUid; }

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

    public boolean isCanEdit() { return canEdit; }

    public String getReply() {
        return reply;
    }

    public Boolean getWholeGroupAssigned() {
        return wholeGroupAssigned;
    }

    public void setWholeGroupAssigned(Boolean wholeGroupAssigned) {
        this.wholeGroupAssigned = wholeGroupAssigned;
    }

    public Integer getMemberCount() {
        return memberCount;
    }

    public void setMemberCount(Integer memberCount) {
        this.memberCount = memberCount;
    }

    public void setCanRespondYes(boolean b) { this.canRespondYes = b; }
    public boolean isCanRespondYes() { return canRespondYes; }

    public void setCanRespondNo(boolean b) { this.canRespondNo = b; }
    public boolean isCanRespondNo() { return canRespondNo; }

    public void setCanMarkCompleted(boolean b) { this.canMarkCompleted = b; }
    public boolean isCanMarkCompleted() { return canMarkCompleted; }

    public boolean respondedYes() {
        return hasResponded && TaskConstants.RESPONSE_YES.equals(reply);
    }

    public boolean respondedNo() {
        return hasResponded && TaskConstants.RESPONSE_NO.equals(reply);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.taskUid);
        dest.writeString(this.title);
        dest.writeString(this.description);
        dest.writeString(this.location);
        dest.writeString(this.parentUid);
        dest.writeString(this.name);
        dest.writeString(this.type);
        dest.writeString(this.deadline);
        dest.writeString(this.deadlineISO);
        dest.writeInt(this.hasResponded ? 1 : 0);
        dest.writeInt(this.canAction ? 1 : 0);
        dest.writeInt(this.canEdit ? 1 : 0);
        dest.writeInt(this.wholeGroupAssigned?1:0);
        dest.writeString(this.reply);
        dest.writeString(this.completedYes);
        dest.writeString(this.completedNo);
    }

    protected TaskModel(Parcel in) {
        Log.e(TAG, "Assembling from parcel!");
        this.taskUid = in.readString();
        this.title = in.readString();
        this.description = in.readString();
        this.location = in.readString();
        this.parentUid = in.readString();
        this.name = in.readString();
        this.type = in.readString();
        this.deadline = in.readString();
        this.deadlineISO = in.readString();
        this.hasResponded = in.readInt() != 0;
        this.canAction = in.readInt() != 0;
        this.canEdit = in.readInt() != 0;
        this.wholeGroupAssigned = in.readInt() != 0;
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
            try { deadlineDate = Constant.isoDateTimeSDF.parse(deadlineISO); }
            catch (ParseException e) { throw new UnsupportedOperationException("Sorry, couldn't overcome the crapness"); }
        }
        return deadlineDate;
    }

    public static final Creator<TaskModel> CREATOR = new Creator<TaskModel>() {
        @Override
        public TaskModel createFromParcel(Parcel in) {
            return new TaskModel(in);
        }

        @Override
        public TaskModel[] newArray(int size) {
            return new TaskModel[size];
        }
    };

    @Override
    public int compareTo(TaskModel task2) {
        return getDeadlineDate().compareTo(task2.getDeadlineDate());
    }

    public static Comparator<TaskModel> canRespondComparator = new Comparator<TaskModel>() {
        @Override
        public int compare(TaskModel taskModel, TaskModel t1) {
            return (!taskModel.hasResponded && t1.hasResponded) ? 1 :
                    (taskModel.hasResponded && !t1.hasResponded) ? -1
                            : -(taskModel.compareTo(t1)); // so it's reverse order on the date
        }
    };

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TaskModel taskModel = (TaskModel) o;

        return taskUid != null ? taskUid.equals(taskModel.taskUid) : taskModel.taskUid == null;

    }

    @Override
    public int hashCode() {
        return taskUid != null ? taskUid.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "TaskModel{" +
                "type='" + type + '\'' +
                ", canEdit=" + canEdit +
                '}';
    }
}