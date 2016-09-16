package org.grassroot.android.models.responses;

/**
 * Created by paballo on 2016/09/15.
 */
public class MessengerSetting {

    private String groupUid;
    private String userUid;
    private boolean canSend;
    private boolean canReceive;
    private boolean userInitiated;
    private boolean active;

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

    public boolean getUserInitiated() {
        return userInitiated;
    }

    @Override
    public String toString() {
        return "MessengerSetting{" +
                "groupUid='" + groupUid + '\'' +
                ", userUid='" + userUid + '\'' +
                ", canSend=" + canSend +
                ", canReceive=" + canReceive +
                ", userInitiated=" + userInitiated +
                ", active=" + active +
                '}';
    }
}
