package org.grassroot.android.services.model;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import org.grassroot.android.utils.Constant;

import java.text.ParseException;
import java.util.Comparator;
import java.util.Date;

/**
 * Created by paballo on 2016/05/05.
 */
public class TaskModel implements Parcelable, Comparable<TaskModel> {

    private static final String TAG = TaskModel.class.getCanonicalName();

    private String id;
    private String title;
    private String description;
    private String name;
    private String type;
    private String parentName;

    private String deadline;
    private String deadlineISO;
    private Date deadlineDate;

    private Boolean hasResponded;
    private Boolean canAction;
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

    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
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

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public String getDeadline() {
        return deadline;
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

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.id);
        dest.writeString(this.title);
        dest.writeString(this.description);
        dest.writeString(this.name);
        dest.writeString(this.type);
        dest.writeString(this.deadline);
        dest.writeString(this.deadlineISO);
        dest.writeValue(this.hasResponded);
        dest.writeValue(this.canAction);
        dest.writeString(this.reply);
        dest.writeString(this.completedYes);
        dest.writeString(this.completedNo);
    }

    protected TaskModel(Parcel in) {
        Log.e(TAG, "Assembling from parcel!");
        this.id = in.readString();
        this.title = in.readString();
        this.description = in.readString();
        this.name = in.readString();
        this.type = in.readString();
        this.deadline = in.readString();
        this.deadlineISO = in.readString();
        this.hasResponded = (Boolean) in.readValue(Boolean.class.getClassLoader());
        this.canAction = (Boolean) in.readValue(Boolean.class.getClassLoader());
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

        return id != null ? id.equals(taskModel.id) : taskModel.id == null;

    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}