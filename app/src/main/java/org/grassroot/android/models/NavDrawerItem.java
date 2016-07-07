package org.grassroot.android.models;

/**
 * Created by Ravi on 29/07/15.
 */
public class NavDrawerItem {

    public int icon;
    public String title;
    public int changeicon;
    private boolean isChecked;
    private String tag;
    private boolean showItemCount;
    private int itemCount;

    public  NavDrawerItem(String title, int icon, int changeicon, boolean isChecked, boolean showItemCount) {
        this.icon = icon;
        this.title = title;
        this.changeicon = changeicon;
        this.isChecked = isChecked;
        this.showItemCount = showItemCount;
        this.itemCount = 0;
    }

    public NavDrawerItem() {

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

    public boolean isChecked() {
        return isChecked;
    }

    public void setIsChecked(boolean isChecked) {
        this.isChecked = isChecked;
    }

    public String getTag() { return this.tag; }

    public boolean isShowItemCount() { return this.showItemCount; }

    public void setItemCount(int itemCount) { this.itemCount = itemCount; }

    public int getItemCount() { return itemCount; }

    public void incrementItemCount() { itemCount++; }

    public void decrementItemCount() { itemCount--; }
}
