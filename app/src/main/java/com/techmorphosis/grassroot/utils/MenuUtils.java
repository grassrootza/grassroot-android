package com.techmorphosis.grassroot.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

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

}
