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
import android.telephony.PhoneNumberUtils;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.techmorphosis.grassroot.models.Contact;
import com.techmorphosis.grassroot.ui.activities.PhoneBookContactsActivity;
import com.techmorphosis.grassroot.ui.fragments.AlertDialogFragment;
import com.techmorphosis.grassroot.interfaces.AlertDialogListener;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Referenced classes of package com.techmorphosis.Utils:
//            AlertDialogListener, DeliveryScheduleListner

public class UtilClass {

    private static final String TAG = UtilClass.class.getCanonicalName();

    private static final Pattern zaPhoneE164 = Pattern.compile("27[6,7,8]\\d{8}");
    private static final Pattern zaPhoneE164Plus = Pattern.compile("\\+27[6,7,8]\\d{8}");
    private static final Pattern nationalRegex = Pattern.compile("0[6,7,8]\\d{8}");

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

    public void showsnackBar(View v, Context applicationContext, String message) {
        snackBar= Snackbar.make(v, message, Snackbar.LENGTH_SHORT);
        snackBar.show();
    }

    public static AlertDialogFragment showAlertDialog(FragmentManager manager, String message, String left, String right,
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

    public static void callPhoneBookActivity(Activity callingActivity, ArrayList<Contact> preSelectedList,
                                             ArrayList<Contact> removeList) {
        Intent phoneBookIntent = new Intent(callingActivity, PhoneBookContactsActivity.class);
        if (preSelectedList != null) phoneBookIntent.putParcelableArrayListExtra(Constant.filteredList, preSelectedList);
        if (removeList != null) phoneBookIntent.putParcelableArrayListExtra(Constant.doNotDisplayContacts, removeList);
        callingActivity.startActivityForResult(phoneBookIntent, Constant.activityContactSelection);
    }

    // since google's libPhoneNumber is a mammoth 1mb JAR, not including, but hand rolling
    public static String formatNumberToE164(String phoneNumber) {

        String normalizedNumber = PhoneNumberUtils.stripSeparators(phoneNumber);

        final Matcher alreadyCorrect = zaPhoneE164.matcher(normalizedNumber);
        if (alreadyCorrect.find()) return normalizedNumber;

        final Matcher removePlus = zaPhoneE164Plus.matcher(normalizedNumber);
        if (removePlus.find()) {
            return normalizedNumber.substring(1);
        }

        final Matcher reformat = nationalRegex.matcher(normalizedNumber);
        if (reformat.find()) {
            return "27" + normalizedNumber.substring(1);
        }

        // todo: throw an error, etc
        Log.d(TAG, "error! tried to reformat, couldn't, here is phone number = " + normalizedNumber);
        return normalizedNumber;
    }

}
