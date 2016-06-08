package org.grassroot.android.models;

import com.google.gson.annotations.SerializedName;

import java.util.Date;

/**
 * Created by paballo on 2016/05/18.
 */

public class EventModel {

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


    private boolean isCancelled;
    private boolean canEdit;
    @SerializedName("totals")
    private ResponseTotalsModel totals;

    public boolean isCancelled() {
        return isCancelled;
    }

    public boolean isCanEdit() {
        return canEdit;
    }


    public ResponseTotalsModel getResponseTotalsModel() {
        return totals;
    }


    public void setCancelled(boolean cancelled) {
        isCancelled = cancelled;
    }

    public void setCanEdit(boolean canEdit) {
        this.canEdit = canEdit;
    }

    public ResponseTotalsModel getTotals() {
        return totals;
    }

    public void setTotals(ResponseTotalsModel totals) {
        this.totals = totals;
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

    public void setType(String type) {
        this.type = type;
    }

    public String getParentName() {
        return parentName;
    }

    public void setParentName(String parentName) {
        this.parentName = parentName;
    }

    public String getDeadline() {
        return deadline;
    }

    public void setDeadline(String deadline) {
        this.deadline = deadline;
    }

    public String getDeadlineISO() {
        return deadlineISO;
    }

    public void setDeadlineISO(String deadlineISO) {
        this.deadlineISO = deadlineISO;
    }

    public Date getDeadlineDate() {
        return deadlineDate;
    }

    public void setDeadlineDate(Date deadlineDate) {
        this.deadlineDate = deadlineDate;
    }

    public Boolean getHasResponded() {
        return hasResponded;
    }

    public void setHasResponded(Boolean hasResponded) {
        this.hasResponded = hasResponded;
    }

    public Boolean getCanAction() {
        return canAction;
    }

    public void setCanAction(Boolean canAction) {
        this.canAction = canAction;
    }

    public String getReply() {
        return reply;
    }

    public void setReply(String reply) {
        this.reply = reply;
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

    public boolean isCanRespondYes() {
        return canRespondYes;
    }

    public void setCanRespondYes(boolean canRespondYes) {
        this.canRespondYes = canRespondYes;
    }

    public boolean isCanRespondNo() {
        return canRespondNo;
    }

    public void setCanRespondNo(boolean canRespondNo) {
        this.canRespondNo = canRespondNo;
    }

    @Override
    public String toString() {
        return "EventModel{" +
                "id='" + id + '\'' +
                ", title='" + title + '\'' +
                ", description='" + description + '\'' +
                ", name='" + name + '\'' +
                ", type='" + type + '\'' +
                ", parentName='" + parentName + '\'' +
                ", deadline='" + deadline + '\'' +
                ", deadlineISO='" + deadlineISO + '\'' +
                ", deadlineDate=" + deadlineDate +
                ", hasResponded=" + hasResponded +
                ", canAction=" + canAction +
                ", reply='" + reply + '\'' +
                ", wholeGroupAssigned=" + wholeGroupAssigned +
                ", memberCount=" + memberCount +
                ", completedYes='" + completedYes + '\'' +
                ", completedNo='" + completedNo + '\'' +
                ", canRespondYes=" + canRespondYes +
                ", canRespondNo=" + canRespondNo +
                ", isCancelled=" + isCancelled +
                ", canEdit=" + canEdit +
                ", totals=" + totals +
                '}';
    }
}
