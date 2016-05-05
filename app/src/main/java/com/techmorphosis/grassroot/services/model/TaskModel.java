package com.techmorphosis.grassroot.services.model;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by paballo on 2016/05/05.
 */
public class TaskModel implements Parcelable{

    private String id;
    private String title;
    private String description;
    private String name;
    private String type;
    private String parentName;
    private String deadline;
    private String deadlineISO;
    private Boolean hasResponded;
    private Boolean canAction;
    private String reply;
    private Boolean wholeGroupAssigned;
    private Integer memberCount;
    private String completedYes;
    private String completedNo;
    private String thumbsUp;
    private String thumbsDown;


    /**
     *
     * @return
     * The id
     */
    public String getId() {
        return id;
    }

    /**
     *
     * @param id
     * The id
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     *
     * @return
     * The title
     */
    public String getTitle() {
        return title;
    }

    /**
     *
     * @param title
     * The title
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     *
     * @return
     * The description
     */
    public String getDescription() {
        return description;
    }

    /**
     *
     * @param description
     * The description
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     *
     * @return
     * The name
     */
    public String getName() {
        return name;
    }

    /**
     *
     * @param name
     * The name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     *
     * @return
     * The type
     */
    public String getType() {
        return type;
    }

    /**
     *
     * @param type
     * The type
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     *
     * @return
     * The parentName
     */
    public String getParentName() {
        return parentName;
    }

    /**
     *
     * @param parentName
     * The parentName
     */
    public void setParentName(String parentName) {
        this.parentName = parentName;
    }

    /**
     *
     * @return
     * The deadline
     */
    public String getDeadline() {
        return deadline;
    }

    /**
     *
     * @param deadline
     * The deadline
     */
    public void setDeadline(String deadline) {
        this.deadline = deadline;
    }

    /**
     *
     * @return
     * The deadlineISO
     */
    public String getDeadlineISO() {
        return deadlineISO;
    }

    /**
     *
     * @param deadlineISO
     * The deadlineISO
     */
    public void setDeadlineISO(String deadlineISO) {
        this.deadlineISO = deadlineISO;
    }

    /**
     *
     * @return
     * The hasResponded
     */
    public Boolean getHasResponded() {
        return hasResponded;
    }

    /**
     *
     * @param hasResponded
     * The hasResponded
     */
    public void setHasResponded(Boolean hasResponded) {
        this.hasResponded = hasResponded;
    }

    /**
     *
     * @return
     * The canAction
     */
    public Boolean getCanAction() {
        return canAction;
    }

    /**
     *
     * @param canAction
     * The canAction
     */
    public void setCanAction(Boolean canAction) {
        this.canAction = canAction;
    }

    /**
     *
     * @return
     * The reply
     */
    public String getReply() {
        return reply;
    }

    /**
     *
     * @param reply
     * The reply
     */
    public void setReply(String reply) {
        this.reply = reply;
    }

    /**
     *
     * @return
     * The wholeGroupAssigned
     */
    public Boolean getWholeGroupAssigned() {
        return wholeGroupAssigned;
    }

    /**
     *
     * @param wholeGroupAssigned
     * The wholeGroupAssigned
     */
    public void setWholeGroupAssigned(Boolean wholeGroupAssigned) {
        this.wholeGroupAssigned = wholeGroupAssigned;
    }

    /**
     *
     * @return
     * The memberCount
     */
    public Integer getMemberCount() {
        return memberCount;
    }

    /**
     *
     * @param memberCount
     * The memberCount
     */
    public void setMemberCount(Integer memberCount) {
        this.memberCount = memberCount;
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


    public String getThumbsUp() {
        return thumbsUp;
    }

    public void setThumbsUp(String thumbsUp) {
        this.thumbsUp = thumbsUp;
    }

    public String getThumbsDown() {
        return thumbsDown;
    }

    public void setThumbsDown(String thumbsDown) {
        this.thumbsDown = thumbsDown;
    }

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
        dest.writeValue(this.hasResponded);
        dest.writeValue(this.canAction);
        dest.writeString(this.reply);
        dest.writeString(this.thumbsUp);
        dest.writeString(this.thumbsDown);
        dest.writeString(this.completedYes);
        dest.writeString(this.completedNo);

    }
    protected TaskModel(Parcel in) {
        this.id = in.readString();
        this.title = in.readString();
        this.description = in.readString();
        this.name = in.readString();
        this.type = in.readString();
        this.deadline = in.readString();
        this.hasResponded = (Boolean) in.readValue(Boolean.class.getClassLoader());
        this.canAction = (Boolean) in.readValue(Boolean.class.getClassLoader());
        this.reply = in.readString();
        this.thumbsUp = in.readString();
        this.thumbsDown = in.readString();
        this.completedYes = in.readString();
        this.completedNo = in.readString();
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
}