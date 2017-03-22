package org.grassroot.android.models;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import org.grassroot.android.R;
import org.grassroot.android.interfaces.GroupConstants;
import org.grassroot.android.models.helpers.DateTime;
import org.grassroot.android.models.helpers.RealmString;
import org.grassroot.android.utils.Constant;
import org.grassroot.android.utils.image.LocalImageUtils;
import org.grassroot.android.utils.RealmUtils;

import java.text.ParseException;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import io.realm.RealmList;
import io.realm.RealmObject;
import io.realm.annotations.Ignore;
import io.realm.annotations.PrimaryKey;

/**
 * Created by paballo on 2016/05/04.
 */
public class Group extends RealmObject implements Parcelable, Comparable<Group> {

  @PrimaryKey private String groupUid;
  private String groupName;
  private String groupCreator;
  private String role;
  private Integer groupMemberCount;

  private String imageUrl;
  private String defaultImage;
  private int defaultImageRes;

  private long lastMajorChangeMillis;
  private boolean fetchedTasks;
  private String lastTimeTasksFetched;

  private String joinCode;
  private boolean discoverable;
  private String lastChangeType;
  private String lastChangeDescription;
  private String description;

  private boolean isLocal; // i.e., is created local but not sent to server yet
  private boolean isEditedLocal; // i.e., has local changes (members etc) that aren't yet on server

  private boolean openOnChat = false;
  private boolean paidFor = false;

  @Ignore private Date date;
  private DateTime dateTime; // used in JSON conversion
  private String dateTimeStringISO;

  private RealmList<RealmString> permissions = new RealmList<>();
  @Ignore private List<String> permissionsList;

  @Ignore private RealmList<Member> members = new RealmList<>();

  @Ignore private List<String> invalidNumbers; // only used on group create or add member, no need to cache/persist

  public Group() {
  }

  public boolean isFetchedTasks() {
    return fetchedTasks;
  }

  public void setFetchedTasks(boolean fetchedTasks) {
    this.fetchedTasks = fetchedTasks;
  }

  // NB : only ever call this to get members from a group returned from server, for any other purpose, fetch from Realm
  public RealmList<Member> getMembers() {
    return members;
  }

  public boolean getIsLocal() {
    return isLocal;
  }

  public void setIsLocal(boolean isLocal) {
    this.isLocal = isLocal;
  }

  public boolean isEditedLocal() { return isEditedLocal; }

  public void setEditedLocal(boolean isEditedLocal) { this.isEditedLocal = isEditedLocal; }

  public void setDate(Date date) {
    this.date = date;
  }

  public Group(String groupUid) {
    this.groupUid = groupUid;
  }

  public List<String> getPermissionsList() {
    if (permissionsList == null) {
      permissionsList = RealmUtils.convertListOfRealmStringInListOfString(permissions);
    }
    return permissionsList;
  }

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

  public String getLastChangeDescription() { return lastChangeDescription; }

  public void setLastChangeDescription(String lastChangeDescription) { this.lastChangeDescription = lastChangeDescription; }

  public String getGroupCreator() {
    return groupCreator;
  }

  public String getRole() {
    return role;
  }

  public Integer getGroupMemberCount() {
    return groupMemberCount;
  }

  public void setLastChangeType(String lastChangeType) {
    this.lastChangeType = lastChangeType;
  }

  public long getLastMajorChangeMillis() { return lastMajorChangeMillis; }

  public void setLastMajorChangeMillis(long lastMajorChangeMillis) { this.lastMajorChangeMillis = lastMajorChangeMillis; }

  public String getJoinCode() {
    return joinCode;
  }

  public void setJoinCode(String joinCode) { this.joinCode = joinCode; }

  public boolean hasJoinCode() {
    return !(TextUtils.isEmpty(joinCode) || GroupConstants.NO_JOIN_CODE.equals(joinCode)) ;
  }

  public String getLastTimeTasksFetched() { return lastTimeTasksFetched; }

  public void setLastTimeTasksFetched(String lastTimeTasksFetched) { this.lastTimeTasksFetched = lastTimeTasksFetched; }

  public boolean isDiscoverable() { return this.discoverable; }

  public void setDiscoverable(boolean isPublic) { this.discoverable = isPublic; }

  public boolean isOpenOnChat() {
    return openOnChat;
  }

  public void setOpenOnChat(boolean openOnChat) {
    this.openOnChat = openOnChat;
  }

  public boolean isPaidFor() {
    return paidFor;
  }

  public void setPaidFor(boolean paidFor) {
    this.paidFor = paidFor;
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

  public String getDefaultImage() { return defaultImage; }

  public void setDefaultImage(String defaultImage) {
    this.defaultImage = defaultImage;
    updateDefaultImageRes();
  }

  public int getDefaultImageRes() {
    if (defaultImageRes == 0) {
      updateDefaultImageRes();
    }
    return defaultImageRes;
  }

  private void updateDefaultImageRes() {
    defaultImageRes = LocalImageUtils.convertDefaultImageTypeToResource(defaultImage);
  }

  public void setDefaultImageRes(int defaultImageRes) { this.defaultImageRes = defaultImageRes; }

  public String getImageUrl() {
    return imageUrl;
  }

  public void setImageUrl(String imageUrl) {
    this.imageUrl = imageUrl;
  }

  public boolean hasCustomImage() {
    return !TextUtils.isEmpty(imageUrl);
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

  public List<String> getInvalidNumbers() { return invalidNumbers; }

  public void setInvalidNumbers(List<String> invalidNumbers) { this.invalidNumbers = invalidNumbers; }

  public RealmList<RealmString> getPermissions() {
    return permissions;
  }

  public void setPermissions(RealmList<RealmString> permissions) {
    this.permissions = permissions;
  }

  /* Helper methods to centralize checking permissions */

  public boolean canCallMeeting() {
    return getPermissionsList().contains(GroupConstants.PERM_CREATE_MTG);
  }

  public boolean canCallVote() {
    return getPermissionsList().contains(GroupConstants.PERM_CALL_VOTE);
  }

  public boolean canCreateTodo() {
    return getPermissionsList().contains(GroupConstants.PERM_CREATE_TODO);
  }

  public boolean canAddMembers() {
    return getPermissionsList().contains(GroupConstants.PERM_ADD_MEMBER);
  }

  public boolean hasCreatePermissions() {
    return getPermissionsList().contains(GroupConstants.PERM_CREATE_MTG)
        || getPermissionsList().contains(GroupConstants.PERM_CALL_VOTE)
        || getPermissionsList().contains(GroupConstants.PERM_CREATE_TODO)
        || getPermissionsList().contains(GroupConstants.PERM_ADD_MEMBER);
  }

  public boolean canViewMembers() {
    return getPermissionsList().contains(GroupConstants.PERM_VIEW_MEMBERS);
  }

  public boolean canDeleteMembers() {
    return getPermissionsList().contains(GroupConstants.PERM_DEL_MEMBER);
  }

  public boolean canEditGroup() {
    return getPermissionsList().contains(GroupConstants.PERM_GROUP_SETTNGS);
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

  public int getChangePrefix() {
    switch (lastChangeType) {
      case GroupConstants.MEETING_CALLED:
        return getDate().after(new Date()) ? R.string.future_meeting_prefix : R.string.past_meeting_prefix;
      case GroupConstants.VOTE_CALLED:
        return getDate().after(new Date()) ? R.string.future_vote_prefix : R.string.past_vote_prefix;
      case GroupConstants.GROUP_CREATED:
        return R.string.group_created_prefix;
      case GroupConstants.MEMBER_ADDED:
        return R.string.member_added_prefix;
      case GroupConstants.GROUP_MOD_OTHER:
        return R.string.group_other_prefix;
      default:
        return R.string.group_other_prefix;
    }
  }

  // note : not including lastChangeDescription or will create confusing results for eg term 'meeting'
  public boolean containsQueryText(final String queryText) {
    return groupName.toLowerCase().contains(queryText)
        || groupCreator.toLowerCase().contains(queryText)
        || (description != null && description.toLowerCase().contains(queryText));
  }

  @Override public int describeContents() {
    return 0;
  }

  @Override public void writeToParcel(Parcel dest, int flags) {
    dest.writeString(this.groupUid);
    dest.writeString(this.groupName);
    dest.writeString(this.description);
    dest.writeString(this.groupCreator);
    dest.writeString(this.role);
    dest.writeInt(this.groupMemberCount);
    dest.writeString(this.imageUrl);
    dest.writeString(getDateTimeStringISO());
    dest.writeString(this.lastChangeType);
    dest.writeString(this.joinCode);
    dest.writeStringList(RealmUtils.convertListOfRealmStringInListOfString(this.permissions));
    dest.writeInt(isLocal ? 1 : 0);
    dest.writeInt(discoverable ? 1 : 0);
    dest.writeList(members);
    dest.writeString(lastTimeTasksFetched);
    dest.writeLong(lastMajorChangeMillis);
    dest.writeInt(defaultImageRes);
    dest.writeString(lastChangeDescription);
    dest.writeInt(openOnChat ? 1 : 0);
    dest.writeInt(paidFor ? 1 : 0);
  }

  protected Group(Parcel in) {
    groupUid = in.readString();
    groupName = in.readString();
    description = in.readString();
    groupCreator = in.readString();
    role = in.readString();
    groupMemberCount = in.readInt();
    imageUrl = in.readString();
    dateTimeStringISO = in.readString();
    lastChangeType = in.readString();
    joinCode = in.readString();
    permissions = RealmUtils.convertListOfStringInRealmListOfString(in.createStringArrayList());
    isLocal = in.readInt() != 0;
    discoverable = in.readInt() != 0;
    in.readList(members,Member.class.getClassLoader());
    lastTimeTasksFetched = in.readString();
    lastMajorChangeMillis = in.readLong();
    defaultImageRes = in.readInt();
    lastChangeDescription = in.readString();
    openOnChat = in.readInt() != 0;
    paidFor = in.readInt() != 0;
  }

  public static final Creator<Group> CREATOR = new Creator<Group>() {
    @Override public Group createFromParcel(Parcel in) {
      return new Group(in);
    }

    @Override public Group[] newArray(int size) {
      return new Group[size];
    }
  };

  // note : this is going to sort like Java Dates usually do, i.e., from earliest to latest, so for most cases, use reverse order
  @Override public int compareTo(@NonNull Group g2) {
    return (this.lastMajorChangeMillis > g2.lastMajorChangeMillis) ? 1
            : (this.lastMajorChangeMillis < g2.lastMajorChangeMillis) ? -1 :
            this.getDate().compareTo(g2.getDate()); // last one just in case both are zero for some reason
  }

  public static Comparator<Group> GroupLastChangeComparator = new Comparator<Group>() {
    @Override
    public int compare(Group lhs, Group rhs) {
      return lhs.compareTo(rhs);
    }
  };

  public static Comparator<Group> GroupTaskDateComparator = new Comparator<Group>() {
    @Override
    public int compare(Group lhs, Group rhs) {
      return lhs.getDate().compareTo(rhs.getDate());
    }
  };

  public static Comparator<Group> GroupSizeComparator = new Comparator<Group>() {
    @Override
    public int compare(Group lhs, Group rhs) {
      return lhs.groupMemberCount > rhs.groupMemberCount ? 1
          : lhs.groupMemberCount < rhs.groupMemberCount ? -1 : 0;
    }
  };

  public static Comparator<Group> GroupNameComparator = new Comparator<Group>() {
    @Override
    public int compare(Group lhs, Group rhs) {
      return lhs.getGroupName().compareTo(rhs.getGroupName());
    }
  };

  public static Comparator<Group> GroupRoleComparator = new Comparator<Group>() {
    @Override public int compare(Group group, Group t1) {
      int compareRoles = compareRoleNames(group.getRole(), t1.getRole());
      if (compareRoles != 0) {
        return compareRoles;
      } else {
        return group.compareTo(t1);
      }
    }

    private int compareRoleNames(String roleFirst, String roleSecond) {
      if (roleFirst.equals(roleSecond)) {
        return 0;
      } else if (roleFirst.equals(GroupConstants.ROLE_ORDINARY_MEMBER)) {
        return -1;
      } else if (roleFirst.equals(GroupConstants.ROLE_COMMITTEE_MEMBER) && roleSecond.equals(
          GroupConstants.ROLE_GROUP_ORGANIZER)) {
        return -1;
      } else {
        return 1;
      }
    }
  };

  @Override public String toString() {
    return "Group{" +
        "groupUid='" + groupUid + '\'' +
        ", isLocal='" + isLocal + '\'' +
        ", lastChangeType='" + lastChangeType + '\'' +
        ", lastMajorChangeTime ='" + lastMajorChangeMillis + '\'' +
        ", groupName='" + groupName + '\'' +
        ", lastFetchedTasks='" + lastTimeTasksFetched + '\'' +
        ", discoverable='" + discoverable + '\'' +
        '}';
  }
}