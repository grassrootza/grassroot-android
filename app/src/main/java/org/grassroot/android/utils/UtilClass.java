// Decompiled by Jad v1.5.8e. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.geocities.com/kpdus/jad.html
// Decompiler options: braces fieldsfirst space lnc 

package org.grassroot.android.utils;

import android.animation.ValueAnimator;
import android.telephony.PhoneNumberUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import org.grassroot.android.models.Member;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UtilClass {

    private static final String TAG = UtilClass.class.getCanonicalName();

    private static final Pattern zaPhoneE164 = Pattern.compile("27[6,7,8]\\d{8}");
    private static final Pattern zaPhoneE164Plus = Pattern.compile("\\+27[6,7,8]\\d{8}");
    private static final Pattern nationalRegex = Pattern.compile("0[6,7,8]\\d{8}");

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

    public static boolean checkIfLocalNumber(String phoneNumber) {

        // todo: might be able to do this much quicker if use Google overall libPhoneNumber, but whole lib for this is heavy
        final String normalized = PhoneNumberUtils.stripSeparators(phoneNumber);
        Log.d(TAG, "inside contact list, normalized number : " + normalized);
        if(normalized.length() < 10){
            return false;
        }
        if (normalized.charAt(0) == '0' && normalized.length() == 10)
            return true;
        if (normalized.substring(0,3).equals("+27") || normalized.substring(0,2).equals("27"))
            return true;
        return false;
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
