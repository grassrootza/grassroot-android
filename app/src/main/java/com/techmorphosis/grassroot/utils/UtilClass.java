// Decompiled by Jad v1.5.8e. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.geocities.com/kpdus/jad.html
// Decompiler options: braces fieldsfirst space lnc 

package com.techmorphosis.grassroot.utils;

import android.animation.ValueAnimator;
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
import android.view.ViewGroup;

import com.techmorphosis.grassroot.models.Contact;
import com.techmorphosis.grassroot.services.model.Member;
import com.techmorphosis.grassroot.ui.fragments.NotificationDialog;
import com.techmorphosis.grassroot.ui.activities.PhoneBookContactsActivity;
import com.techmorphosis.grassroot.ui.fragments.AlertDialogFragment;
import com.techmorphosis.grassroot.interfaces.AlertDialogListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;
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

    public Snackbar snackBar;

    public UtilClass() {
        OS = Build.VERSION.RELEASE;
        MAKE = Build.MANUFACTURER;
        MODEL = Build.MODEL;
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

    public static NotificationDialog showAlertDialog(android.support.v4.app.FragmentManager fragmentmanager, String s, String s1, String s2, String s3, boolean flag, AlertDialogListener alertdialoglistener)
    {
        NotificationDialog ss1 = NotificationDialog.newInstance(s, s1, s2, s3, flag);
        ss1.setListener(alertdialoglistener);
        ss1.setCancelable(false);
        ss1.show(fragmentmanager, null);
        return ss1;
    }


    public static ValueAnimator createSlidingAnimator(final ViewGroup viewGroup, boolean expand) {
        final int widthSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        final int heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);

        viewGroup.measure(widthSpec, heightSpec);
        int height = viewGroup.getMeasuredHeight();

        ValueAnimator animator = expand ? ValueAnimator.ofInt(0, height) : ValueAnimator.ofInt(height, 0);

        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                ViewGroup.LayoutParams params = viewGroup.getLayoutParams();
                params.height = (Integer) valueAnimator.getAnimatedValue();
                viewGroup.setLayoutParams(params);
            }
        });

        return animator;
    }

    public static String timeZone() {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT"), Locale.getDefault());
        String   timeZone = new SimpleDateFormat("Z").format(calendar.getTime());
        return timeZone.substring(0, 3) + ":"+ timeZone.substring(3, 5);
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

    public static Set<String> convertMembersToUids(Set<Member> members) {
        Set<String> uids = new HashSet<>();
        for (Member m : members)
            uids.add(m.getMemberUid());
        return uids;
    }

    public static Set<String> convertMemberListToUids(List<Member> members) {
        Set<String> uids = new HashSet<>();
        if (members != null) {
            for (Member m : members) {
                uids.add(m.getMemberUid());
            }
        }
        return uids;
    }

}
