package org.grassroot.android.utils;

import android.content.Context;
import android.content.Intent;

import org.grassroot.android.services.model.Member;
import org.grassroot.android.ui.activities.GroupMembersActivity;

import java.util.ArrayList;

/**
 * Created by luke on 2016/05/18.
 */
public final class MenuUtils {

    public static Intent constructIntent(Context callingContext, Class toActivityClass, String groupUid, String groupName) {
        Intent i = new Intent(callingContext, toActivityClass);
        i.putExtra(Constant.GROUPUID_FIELD, groupUid);
        i.putExtra(Constant.GROUPNAME_FIELD, groupName);
        return i;
    }

    public static Intent memberSelectionIntent(Context callingContext, String groupUid, String TAG,
                                               ArrayList<Member> preSelectedMembers) {
        Intent pickMember = constructIntent(callingContext, GroupMembersActivity.class, groupUid, "");
        pickMember.putExtra(Constant.PARENT_TAG_FIELD, TAG);
        pickMember.putExtra(Constant.SELECT_FIELD, true);
        pickMember.putExtra(Constant.SHOW_ACTION_BUTTON_FLAG, false);
        pickMember.putExtra(Constant.SHOW_HEADER_FLAG, false);
        pickMember.putParcelableArrayListExtra(Constant.SELECTED_MEMBERS_FIELD, preSelectedMembers);
        return pickMember;
    }

}
