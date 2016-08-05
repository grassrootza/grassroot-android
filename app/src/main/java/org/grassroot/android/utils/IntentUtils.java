package org.grassroot.android.utils;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import org.grassroot.android.activities.ActionCompleteActivity;
import org.grassroot.android.interfaces.GroupConstants;
import org.grassroot.android.models.Group;
import org.grassroot.android.models.Member;
import org.grassroot.android.activities.GroupMembersActivity;

import java.util.ArrayList;

/**
 * Created by luke on 2016/05/18.
 */
public final class IntentUtils {

    public static Intent constructIntent(Context callingContext, Class toActivityClass, String groupUid, String groupName, boolean isLocal) {
        Intent i = new Intent(callingContext, toActivityClass);
        i.putExtra(GroupConstants.UID_FIELD, groupUid);
        i.putExtra(GroupConstants.NAME_FIELD, groupName);
        i.putExtra(Constant.GROUP_LOCAL,isLocal);
        return i;
    }

    public static Intent constructIntent(Context callingContext, Class toActivityClass, String groupUid, String groupName) {
        Intent i = new Intent(callingContext, toActivityClass);
        i.putExtra(GroupConstants.UID_FIELD, groupUid);
        i.putExtra(GroupConstants.NAME_FIELD, groupName);
        return i;
    }

    public static Intent constructIntent(Context callingContext, Class toActivityClass, Group group) {
        Intent i = new Intent(callingContext, toActivityClass);
        i.putExtra(GroupConstants.OBJECT_FIELD, group);
        return i;
    }

    public static Intent offlineMessageIntent(Context callingContext, int header, String body, boolean showTaskButtons, boolean showOfflineOptions) {
        Intent i = new Intent(callingContext, ActionCompleteActivity.class);
        i.putExtra(ActionCompleteActivity.HEADER_FIELD, header);
        i.putExtra(ActionCompleteActivity.BODY_FIELD, body);
        i.putExtra(ActionCompleteActivity.TASK_BUTTONS, showTaskButtons);
        i.putExtra(ActionCompleteActivity.OFFLINE_BUTTONS, showOfflineOptions);
        i.putExtra(ActionCompleteActivity.ACTION_INTENT, ActionCompleteActivity.HOME_SCREEN); // todo : take from whatever triggered this
        return i;
    }

    public static Intent memberSelectionIntent(Context callingContext, String groupUid, String TAG,
                                               ArrayList<Member> preSelectedMembers) {
        Intent pickMember = constructIntent(callingContext, GroupMembersActivity.class, groupUid, "",false);
        pickMember.putExtra(Constant.PARENT_TAG_FIELD, TAG);
        pickMember.putExtra(Constant.SELECT_FIELD, true);
        pickMember.putExtra(Constant.SHOW_ACTION_BUTTON_FLAG, false);
        pickMember.putExtra(Constant.SHOW_HEADER_FLAG, false);
        pickMember.putParcelableArrayListExtra(Constant.SELECTED_MEMBERS_FIELD, preSelectedMembers);
        return pickMember;
    }

}