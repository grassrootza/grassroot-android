package org.grassroot.android.models;

import io.realm.RealmObject;

/**
 * Created by paballo on 2016/05/18.
 */
public class ResponseTotalsModel extends RealmObject {

    private int yes;
    private int no;
    private int maybe;
    private int abstained;
    private int invalid;
    private int numberOfUsers;
    private int numberOfRsvp;

    public int getYes() {
        return yes;
    }

    public int getNo() {
        return no;
    }

    public int getMaybe() {
        return maybe;
    }

    public int getAbstained() {
        return abstained;
    }

    public int getInvalid() {
        return invalid;
    }

    public int getNumberOfUsers() {
        return numberOfUsers;
    }

    public int getNumberOfRsvp() {
        return numberOfRsvp;
    }

    public int getNumberNoReply() { return numberOfUsers - (yes + no + abstained); }

    @Override
    public String toString() {
        return "ResponseTotalsModel{" +
                "yes=" + yes +
                ", no=" + no +
                ", maybe=" + maybe +
                ", abstained=" + abstained +
                ", invalid=" + invalid +
                ", numberOfUsers=" + numberOfUsers +
                ", numberOfRsvp=" + numberOfRsvp +
                '}';
    }
}
