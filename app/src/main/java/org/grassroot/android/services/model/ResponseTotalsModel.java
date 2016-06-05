package org.grassroot.android.services.model;

/**
 * Created by paballo on 2016/05/18.
 */
public class ResponseTotalsModel {

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
