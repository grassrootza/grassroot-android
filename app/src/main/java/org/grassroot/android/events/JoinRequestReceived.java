package org.grassroot.android.events;

import org.grassroot.android.models.GroupJoinRequest;

/**
 * Created by luke on 2016/07/09.
 * Note : should only be posted when these are received in background, not after deliberate fetch
 */
public class JoinRequestReceived {

    public GroupJoinRequest request;

    public JoinRequestReceived(GroupJoinRequest request) {
        this.request = request;
    }
}
