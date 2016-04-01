package com.techmorphosis.grassroot.models;

/**
 * Created by Ravi on 29/07/15.
 */
public class NavDrawerItem {
    public int icon;
    public String title;
    public int changeicon;
    private boolean isChecked;



    public  NavDrawerItem(String title,int icon,int changeicon,boolean isChecked)
    {
        this.icon = icon;
        this.title = title;
        this.changeicon = changeicon;
        this.isChecked = isChecked;

    }

    public int getIcon() {
        return icon;
    }

    public void setIcon(int icon) {
        this.icon = icon;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getChangeicon() {
        return changeicon;
    }

    public void setChangeicon(int changeicon) {
        this.changeicon = changeicon;
    }

    public boolean isChecked() {
        return isChecked;
    }

    public void setIsChecked(boolean isChecked) {
        this.isChecked = isChecked;
    }
}
