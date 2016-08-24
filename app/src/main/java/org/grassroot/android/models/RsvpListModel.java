package org.grassroot.android.models;

import java.util.HashMap;
import java.util.LinkedHashMap;

/**
 * Created by luke on 2016/06/09.
 */
public class RsvpListModel {

    private String meetingUid;

    private int numberInvited;
    private int numberYes;
    private int numberNo;
    private int numberNoReply;

    private boolean canViewRsvps;

    private LinkedHashMap<String, String> rsvpResponses;

    public String getMeetingUid() {
        return meetingUid;
    }

    public int getNumberInvited() {
        return numberInvited;
    }

    public int getNumberYes() {
        return numberYes;
    }

    public int getNumberNo() {
        return numberNo;
    }

    public int getNumberNoReply() {
        return numberNoReply;
    }

    public LinkedHashMap<String, String> getRsvpResponses() {
        return rsvpResponses;
    }

    public boolean isCanViewRsvps() {
        return canViewRsvps;
    }

    @Override
    public String toString() {
        return "RsvpListModel{" +
                "meetingUid='" + meetingUid + '\'' +
                ", numberInvited=" + numberInvited +
                ", numberYes=" + numberYes +
                ", numberNo=" + numberNo +
                ", numberNoReply=" + numberNoReply +
                '}';
    }
}
