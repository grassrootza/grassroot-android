package com.techmorphosis.grassroot.models;

/**
 * Created by admin on 26-Mar-16.
 */
public class Join_RequestModel
{

    public String Groupname;
    public String Group_owner_name;
    public String Group_describe;
    public String groupCreator;
    public  String id;
    public String count;


    public String getGroupCreator() {
        return groupCreator;
    }

    public void setGroupCreator(String groupCreator) {
        this.groupCreator = groupCreator;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCount() {
        return count;
    }

    public void setCount(String count) {
        this.count = count;
    }



    public String getGroupname() {
        return Groupname;
    }

    public void setGroupname(String groupname) {
        Groupname = groupname;
    }

    public String getGroup_owner_name() {
        return Group_owner_name;
    }

    public void setGroup_owner_name(String group_owner_name) {
        Group_owner_name = group_owner_name;
    }

    public String getGroup_describe() {
        return Group_describe;
    }

    public void setGroup_describe(String group_describe) {
        Group_describe = group_describe;
    }
}
