// Decompiled by Jad v1.5.8e. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.geocities.com/kpdus/jad.html
// Decompiler options: braces fieldsfirst space lnc 

package org.grassroot.android.utils;

import android.animation.ValueAnimator;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import org.grassroot.android.models.Member;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utilities {

    private static final String TAG = Utilities.class.getCanonicalName();

    private static final Pattern zaPhoneE164 = Pattern.compile("27[6,7,8]\\d{8}");
    private static final Pattern zaPhoneE164Plus = Pattern.compile("\\+27[6,7,8]\\d{8}"); // make the "+" more general
    private static final Pattern nationalRegex = Pattern.compile("0[6,7,8]\\d{8}");

    private static final Pattern alphaNumericRegex = Pattern.compile("[^a-z0-9 ]", Pattern.CASE_INSENSITIVE);

    public static String timeZone() {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT"), Locale.getDefault());
        String   timeZone = new SimpleDateFormat("Z").format(calendar.getTime());
        return timeZone.substring(0, 3) + ":"+ timeZone.substring(3, 5);
    }

    public static long getCurrentTimeInMillisAtUTC() {
        return (new Date()).getTime();
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

    public static String stripPrefixFromNumber(String phoneNumber) {
        String normalized = PhoneNumberUtils.stripSeparators(phoneNumber);
        if (nationalRegex.matcher(normalized).find()) {
            return phoneNumber;
        }

        if (zaPhoneE164.matcher(normalized).find()) {
            return "0" + normalized.substring(2);
        }

        if (zaPhoneE164Plus.matcher(normalized).find()) {
            return "0" + normalized.substring(3);
        }

        // give up and just return what we have
        return phoneNumber;
    }

    public static boolean checkIfLocalNumber(String phoneNumber) {

        // might be able to do this much quicker if use Google overall libPhoneNumber, but whole lib for this is heavy
        final String normalized = PhoneNumberUtils.stripSeparators(phoneNumber);
        Log.d(TAG, "checking number, normalized number : " + normalized);
        if(normalized.length() < 10){
            return false;
        }
        if (normalized.charAt(0) == '0' && normalized.length() == 10)
            return true;
        if (normalized.substring(0,3).equals("+27") || normalized.substring(0,2).equals("27"))
            return true;
        return false;
    }

    public static boolean checkForSpecialChars(String input) {
        return alphaNumericRegex.matcher(input).find();
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
