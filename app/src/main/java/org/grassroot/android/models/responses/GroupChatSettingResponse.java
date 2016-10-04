package org.grassroot.android.models.responses;

import java.util.List;

/**
 * Created by paballo on 2016/09/15.
 */
public class GroupChatSettingResponse {

    private String groupUid;
    private String userUid;
    private boolean canSend;
    private boolean canReceive;
    private boolean userInitiated;
    private boolean active;
    private List<String> mutedUsersUids;

    public String getGroupUid() {
        return groupUid;
    }

    public String getUserUid() {
        return userUid;
    }

    public boolean isCanReceive() {
        return canReceive;
    }

    public boolean isCanSend() {
        return canSend;
    }

    public List<String> getMutedUsersUids() {
        return mutedUsersUids;
    }

    public boolean getUserInitiated() {
        return userInitiated;
    }


    @Override
    public String toString() {
        return "GroupChatSettingResponse{" +
                "groupUid='" + groupUid + '\'' +
                ", userUid='" + userUid + '\'' +
                '}';
    }
}
