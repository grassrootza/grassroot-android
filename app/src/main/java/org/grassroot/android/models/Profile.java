package org.grassroot.android.models;

import io.realm.RealmObject;

/**
 * Created by paballo on 2016/05/19.
 */
public class Profile extends RealmObject {

    private String displayName;
    private String language;
    private String alertPreference;

    public String getDisplay_name() {
        return displayName;
    }

    public String getLanguage() {
        return language;
    }

    public String getAlertPreference() {
        return alertPreference;
    }
}
