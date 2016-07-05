package org.grassroot.android.models;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;

import io.realm.Realm;
import org.grassroot.android.R;
import org.grassroot.android.interfaces.GroupConstants;
import org.grassroot.android.utils.Constant;
import org.grassroot.android.utils.RealmUtils;

import java.text.ParseException;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;

import io.realm.RealmList;
import io.realm.RealmObject;
import io.realm.annotations.Ignore;
import io.realm.annotations.PrimaryKey;

/**
 * Created by paballo on 2016/05/04.
 */
public class Group extends RealmObject implements Parcelable, Comparable<Group> {

    @PrimaryKey
    private String groupUid;
    private String groupName;
    private String groupCreator;
    private String role;
    private Integer groupMemberCount;

    private String joinCode;
    private String lastChangeType;
    private String description;
    private boolean isLocal;

    public boolean getIsLocal() {
        return isLocal;
    }

    public void setIsLocal(boolean isLocal) {
        this.isLocal = isLocal;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    @Ignore
    private Date date;
    private DateTime dateTime; // used in JSON conversion
    private String dateTimeStringISO;

    private boolean hasTasks;

    public void setLastChangeType(String lastChangeType) {
        this.lastChangeType = lastChangeType;
    }

    public Group() {
    }


    private RealmList<RealmString> permissions = new RealmList<>(); // todo: convert this to a set so can do fast hashing

    public String getGroupUid() {
        return groupUid;
    }

    public void setGroupUid(String groupUid) {
        this.groupUid = groupUid;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getGroupCreator() {
        return groupCreator;
    }

    public String getRole() {
        return role;
    }

    public Integer getGroupMemberCount() {
        return groupMemberCount;
    }

    public String getJoinCode() {
        return joinCode;
    }

    public boolean isHasTasks() {
        return hasTasks;
    }

    public void setHasTasks(boolean hasTasks) {
        this.hasTasks = hasTasks;
    }

    public String getDateTimeStringISO() {
        if (dateTimeStringISO == null || dateTimeStringISO.equals("")) {
            dateTimeStringISO = Constant.isoDateTimeSDF.format(getDate());
            return dateTimeStringISO;
        } else {
            return dateTimeStringISO;
        }
    }

    public String getLastChangeType() {
        return lastChangeType;
    }

    public Date getDate() {
        if (date == null) {
            if (dateTime != null) {
                constructDate();
                return date;
            } else if (dateTimeStringISO != null && !dateTimeStringISO.equals("")) {
                try {
                    date = Constant.isoDateTimeSDF.parse(dateTimeStringISO);
                    return date;
                } catch (ParseException e) {
                    e.printStackTrace();
                    return null;
                }
            } else {
                throw new UnsupportedOperationException("Error! Group needs at least one source of a datetime");
            }
        } else {
            return date;
        }
    }

    public RealmList<RealmString> getPermissions() {
        return permissions;
    }

    public void setPermissions(RealmList<RealmString> permissions) {
        this.permissions = permissions;
    }

    /* Helper methods to centralize checking permissions */

    public boolean canCallMeeting() {
        return permissions.contains(GroupConstants.PERM_CREATE_MTG);
    }

    public boolean canCallVote() {
        return permissions.contains(GroupConstants.PERM_CALL_VOTE);
    }

    public boolean canCreateTodo() {
        return permissions.contains(GroupConstants.PERM_CREATE_TODO);
    }

    public boolean canAddMembers() {
        return permissions.contains(GroupConstants.PERM_ADD_MEMBER);
    }

    public boolean hasCreatePermissions() {
        return permissions.contains(GroupConstants.PERM_CREATE_MTG) || permissions.contains(GroupConstants.PERM_CALL_VOTE)
            || permissions.contains(GroupConstants.PERM_CREATE_TODO) || permissions.contains(GroupConstants.PERM_ADD_MEMBER);
    }

    public boolean canViewMembers() {
        return permissions.contains(GroupConstants.PERM_VIEW_MEMBERS);
    }

    public boolean canDeleteMembers() {
        return permissions.contains(GroupConstants.PERM_DEL_MEMBER);
    }

    public boolean canEditGroup() {
        return permissions.contains(GroupConstants.PERM_GROUP_SETTNGS);
    }

    private void constructDate() {
        Calendar calendar = Calendar.getInstance();

        // NB: Java 7 datetime requires these to be set in order (argh, for Joda/Java8)
        calendar.set(Calendar.YEAR, dateTime.getYear());
        calendar.set(Calendar.MONTH, dateTime.getMonthValue() - 1);
        calendar.set(Calendar.DAY_OF_MONTH, dateTime.getDayOfMonth());

        calendar.set(Calendar.HOUR_OF_DAY, dateTime.getHour());
        calendar.set(Calendar.MINUTE, dateTime.getMinute());
        calendar.set(Calendar.SECOND, dateTime.getSecond());
        this.date = calendar.getTime();
    }

    public String constructChangeType(Context context) {
        switch (lastChangeType) {
            case GroupConstants.MEETING_CALLED:
                return (getDate().after(new Date()) ? context.getString(R.string.future_meeting_prefix)
                        : context.getString(R.string.past_meeting_prefix));
            case GroupConstants.VOTE_CALLED:
                return (getDate().after(new Date())) ? context.getString(R.string.future_vote_prefix)
                        : context.getString(R.string.past_vote_prefix);
            case GroupConstants.GROUP_CREATED:
                return context.getString(R.string.group_created_prefix);
            case GroupConstants.MEMBER_ADDED:
                return context.getString(R.string.member_added_prefix);
            case GroupConstants.GROUP_MOD_OTHER:
                return context.getString(R.string.group_other_prefix);
            default:
                throw new UnsupportedOperationException("Error! Should only be one of standard change types");
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        Realm realm = Realm.getDefaultInstance();
        realm.beginTransaction();
        dest.writeString(this.groupUid);
        dest.writeString(this.groupName);
        dest.writeString(this.description);
        dest.writeString(this.groupCreator);
        dest.writeString(this.role);
        dest.writeInt(this.groupMemberCount);
        dest.writeString(getDateTimeStringISO());
        dest.writeString(this.lastChangeType);
        dest.writeString(this.joinCode);
        dest.writeInt(hasTasks ? 1 : 0);
        dest.writeStringList(RealmUtils.convertListOfRealmStringInListOfString(this.permissions));
        realm.commitTransaction();
        realm.close();
    }

    protected Group(Parcel in) {
        groupUid = in.readString();
        groupName = in.readString();
        description = in.readString();
        groupCreator = in.readString();
        role = in.readString();
        groupMemberCount = in.readInt();
        dateTimeStringISO = in.readString();
        lastChangeType = in.readString();
        joinCode = in.readString();
        hasTasks = in.readInt() != 0;
        permissions = RealmUtils.convertListOfStringInRealmListOfString(in.createStringArrayList());
    }

    public static final Creator<Group> CREATOR = new Creator<Group>() {
        @Override
        public Group createFromParcel(Parcel in) {
            return new Group(in);
        }

        @Override
        public Group[] newArray(int size) {
            return new Group[size];
        }
    };

    @Override
    public int compareTo(Group g2) {
        return this.getDate().compareTo(g2.getDate());
    }

    public static Comparator<Group> GroupRoleComparator = new Comparator<Group>() {
        @Override
        public int compare(Group group, Group t1) {
            int compareRoles = compareRoleNames(group.getRole(), t1.getRole());
            if (compareRoles != 0)
                return compareRoles;
            else
                return group.compareTo(t1);
        }

        private int compareRoleNames(String roleFirst, String roleSecond) {
            if (roleFirst.equals(roleSecond)) {
                return 0;
            } else if (roleFirst.equals(GroupConstants.ROLE_ORDINARY_MEMBER)) {
                return -1;
            } else if (roleFirst.equals(GroupConstants.ROLE_COMMITTEE_MEMBER) &&
                roleSecond.equals(GroupConstants.ROLE_GROUP_ORGANIZER)) {
                return -1;
            } else {
                return 1;
            }
        }
    };

    @Override
    public String toString() {
        return "Group{" +
            "groupUid='" + groupUid + '\'' +
            ", lastChangeType='" + lastChangeType + '\'' +
            ", groupName='" + groupName + '\'' +
            '}';
    }

    public void setDateTimeStringISO(String dateTimeStringISO) {
        this.dateTimeStringISO = dateTimeStringISO;
    }

    public void setGroupCreator(String groupCreator) {
        this.groupCreator = groupCreator;
    }

    public void setGroupMemberCount(Integer groupMemberCount) {
        this.groupMemberCount = groupMemberCount;
    }
}