package com.techmorphosis.grassroot.services.model;

import android.os.Parcel;
import android.os.Parcelable;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Created by paballo on 2016/05/04.
 */
public class Group implements Parcelable {

    private String id;
    private String groupName;
    private String description;
    private String groupCreator;
    private String role;
    private Integer groupMemberCount;
    private DateTime dateTime;
    private String dateTimefull;
    private String dateTimeShort;
    private List<String> permissions = new ArrayList<>(); // todo: convert this to a set so can do fast hashing

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
     * The groupName
     */
    public String getGroupName() {
        return groupName;
    }

    /**
     *
     * @param groupName
     * The groupName
     */
    public void setGroupName(String groupName) {
        this.groupName = groupName;
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
     * The groupCreator
     */
    public String getGroupCreator() {
        return groupCreator;
    }

    /**
     *
     * @param groupCreator
     * The groupCreator
     */
    public void setGroupCreator(String groupCreator) {
        this.groupCreator = groupCreator;
    }

    /**
     *
     * @return
     * The role
     */
    public String getRole() {
        return role;
    }

    /**
     *
     * @param role
     * The role
     */
    public void setRole(String role) {
        this.role = role;
    }

    /**
     *
     * @return
     * The groupMemberCount
     */
    public Integer getGroupMemberCount() {
        return groupMemberCount;
    }

    /**
     *
     * @param groupMemberCount
     * The groupMemberCount
     */
    public void setGroupMemberCount(Integer groupMemberCount) {
        this.groupMemberCount = groupMemberCount;
    }

    /**
     *
     * @return
     * The dateTime
     */
    public DateTime getDateTime() {
        return dateTime;
    }

    /**
     *
     * @param dateTime
     * The dateTime
     */
    public void setDateTime(DateTime dateTime) {
        this.dateTime = dateTime;
    }

    /**
     *
     * @return
     * The permissions
     */
    public List<String> getPermissions() {
        return permissions;
    }

    /**
     *
     * @param permissions
     * The permissions
     */
    public void setPermissions(List<String> permissions) {
        this.permissions = permissions;
    }



    public String getDateTimefull() {


        //get the current date as Calendar object
        Calendar calendar = Calendar.getInstance();

        /*Date*/
        calendar.set(Calendar.DAY_OF_MONTH, dateTime.getDayOfMonth());
        calendar.set(Calendar.MONTH, dateTime.getMonthValue() - 1);
        calendar.set(Calendar.YEAR, dateTime.getYear());

        /*Time*/
        calendar.set(Calendar.HOUR, dateTime.getHour());
        calendar.set(Calendar.MINUTE, dateTime.getMinute());
        calendar.set(Calendar.SECOND, dateTime.getSecond());
        Date date = calendar.getTime();

        SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yy:HH:mm:SS");
        dateTimefull = formatter.format(date);

        // Log.e(TAG,"dateString " + dateString);
        return  dateTimefull;
    }

    public String getDateTimeShort() {

        //get the current date as Calendar object
        Calendar calendar = Calendar.getInstance();

        /*Date*/
        calendar.set(Calendar.DAY_OF_MONTH, dateTime.getDayOfMonth());
        calendar.set(Calendar.MONTH, dateTime.getMonthValue() - 1);
        calendar.set(Calendar.YEAR, dateTime.getYear());

        /*Time*/
        calendar.set(Calendar.HOUR, dateTime.getHour());
        calendar.set(Calendar.MINUTE, dateTime.getMinute());
        calendar.set(Calendar.SECOND, dateTime.getSecond());
        Date date = calendar.getTime();

        SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy");
        dateTimeShort = formatter.format(date);

        return dateTimeShort;


    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.id);
        dest.writeString(this.groupName);
        dest.writeString(this.description);
        dest.writeString(this.groupCreator);
        dest.writeString(this.role);
        dest.writeInt(this.groupMemberCount);
        dest.writeString(this.dateTimefull);
        dest.writeString(this.dateTimeShort);
        dest.writeStringList(this.permissions);
    }

    protected Group(Parcel in) {
        id = in.readString();
        groupName = in.readString();
        description = in.readString();
        groupCreator = in.readString();
        role = in.readString();
        dateTimefull = in.readString();
        dateTimeShort = in.readString();
        permissions = in.createStringArrayList();
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
}
