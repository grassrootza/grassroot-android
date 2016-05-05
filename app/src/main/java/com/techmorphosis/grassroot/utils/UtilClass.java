// Decompiled by Jad v1.5.8e. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.geocities.com/kpdus/jad.html
// Decompiler options: braces fieldsfirst space lnc 

package com.techmorphosis.grassroot.utils;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.view.View;
import android.widget.Toast;

import com.techmorphosis.grassroot.models.ContactsModel;
import com.techmorphosis.grassroot.ui.activities.PhoneBookContactsActivity;
import com.techmorphosis.grassroot.ui.fragments.AlertDialogFragment;
import com.techmorphosis.grassroot.utils.listener.AlertDialogListener;

import java.util.ArrayList;

// Referenced classes of package com.techmorphosis.Utils:
//            AlertDialogListener, DeliveryScheduleListner

public class UtilClass {

 public static String MAKE;
    public static String MODEL;
    public static String OS;
    FragmentTransaction mTransaction;
    public Snackbar snackBar;

    public UtilClass() {
        OS = Build.VERSION.RELEASE;
        MAKE = Build.MANUFACTURER;
        MODEL = Build.MODEL;
    }

    public void showToast(Context context, String s) {
        Toast.makeText(context, s, Toast.LENGTH_SHORT).show();
    }

    public void showsnackBar(View v,Context applicationContext, String message) {
        snackBar= Snackbar.make(v, message, Snackbar.LENGTH_SHORT);
        snackBar.show();
    }

    public AlertDialogFragment showAlertDialog(FragmentManager manager, String message, String left, String right,
                                               Boolean flag, AlertDialogListener alertDialogListener) {
        AlertDialogFragment ss2 = new AlertDialogFragment();
        Bundle b = new Bundle();
        b.putString("message", message);
        b.putBoolean("cancelable", flag);
        b.putString("left", left);
        b.putString("right",right);
        ss2.setArguments(b);
        ss2.setListener(alertDialogListener);
        ss2.show(manager,null);
        return ss2;
    }

    public static void callPhoneBookActivity(Activity callingActivity, ArrayList<ContactsModel> filterList) {
        Intent phoneBookIntent = new Intent(callingActivity, PhoneBookContactsActivity.class);
        phoneBookIntent.putParcelableArrayListExtra(Constant.filteredList, filterList);
        callingActivity.startActivityForResult(phoneBookIntent, Constant.activityContactSelection);
    }

}
