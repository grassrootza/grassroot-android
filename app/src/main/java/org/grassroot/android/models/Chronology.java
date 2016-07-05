package org.grassroot.android.models;

import io.realm.RealmObject;

/**
 * Created by paballo on 2016/05/04.
 */
public class Chronology extends RealmObject{

    private String calendarType;
    private String id;

    /**
     *
     * @return
     * The calendarType
     */
    public String getCalendarType() {
        return calendarType;
    }

    /**
     *
     * @param calendarType
     * The calendarType
     */
    public void setCalendarType(String calendarType) {
        this.calendarType = calendarType;
    }

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

}
