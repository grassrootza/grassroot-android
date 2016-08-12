package org.grassroot.android.models;

import io.realm.RealmObject;

public class ShareModel extends RealmObject{

    private String appName;
    private String packageName;

    public ShareModel(){}

    public ShareModel(String appName, String packageName){
        this.appName = appName;
        this.packageName = packageName;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }
}
