package org.grassroot.android.models;

/**
 * Created by Ravi on 29/07/15.
 */
public class NavDrawerItem {

    public int defaultIcon;
    public String itemLabel;
    public int selectedIcon;

    private boolean isChecked = false;
    private boolean showItemCount = false;
    private int itemCount;

    private String tag;

    public  NavDrawerItem(String tag, String itemLabel, int defaultIcon, int selectedIcon, boolean isChecked, boolean showItemCount) {
        this.tag = tag;
        this.defaultIcon = defaultIcon;
        this.itemLabel = itemLabel;
        this.selectedIcon = selectedIcon;
        this.isChecked = isChecked;
        this.showItemCount = showItemCount;
        this.itemCount = 0;
    }

    public NavDrawerItem() {

    }

    public int getDefaultIcon() {
        return defaultIcon;
    }

    public void setDefaultIcon(int defaultIcon) {
        this.defaultIcon = defaultIcon;
    }

    public String getItemLabel() {
        return itemLabel;
    }

    public void setItemLabel(String itemLabel) {
        this.itemLabel = itemLabel;
    }

    public int getSelectedIcon() {
        return selectedIcon;
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
