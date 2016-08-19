// Decompiled by Jad v1.5.8e. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.geocities.com/kpdus/jad.html
// Decompiler options: braces fieldsfirst space lnc 

package org.grassroot.android.utils;

import android.telephony.PhoneNumberUtils;
import android.util.Log;

import org.grassroot.android.models.Member;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utilities {

    private static final String TAG = Utilities.class.getCanonicalName();

    private static final Pattern zaPhoneE164 = Pattern.compile("27[6,7,8]\\d{8}");
    private static final Pattern zaPhoneE164Plus = Pattern.compile("\\+27[6,7,8]\\d{8}"); // make the "+" more general
    private static final Pattern nationalRegex = Pattern.compile("0[6,7,8]\\d{8}");

    private static final Pattern alphaNumericRegex = Pattern.compile("[^a-z0-9 ]", Pattern.CASE_INSENSITIVE);

    public static long getCurrentTimeInMillisAtUTC() {
        return (new Date()).getTime();
    }

    // since google's libPhoneNumber is a mammoth 1mb JAR, not including, but hand rolling
    public static String formatNumberToE164(String phoneNumber) {

        String normalizedNumber = PhoneNumberUtils.stripSeparators(phoneNumber);

        final Matcher alreadyCorrect = zaPhoneE164.matcher(normalizedNumber);

        if (alreadyCorrect.find()) {
            return normalizedNumber;
        } else if (zaPhoneE164Plus.matcher(normalizedNumber).find()) { // remove plus sign and return
            return normalizedNumber.substring(1);
        } else if (nationalRegex.matcher(normalizedNumber).find()) { // remove 0 and return
            return "27" + normalizedNumber.substring(1);
        } else {
            // throwing an error here would be good, but may lead to unintended crashes, and server
            // provides a further line of defence to malformed numbers, so just log and return
            Log.d(TAG, "error! tried to reformat, couldn't, here is phone number = " + normalizedNumber);
            return normalizedNumber;
        }

    }

    public static boolean checkIfLocalNumber(String phoneNumber) {

        // might be able to do this much quicker if use Google overall libPhoneNumber, but whole lib for this is heavy
        final String normalized = PhoneNumberUtils.stripSeparators(phoneNumber);

        if(zaPhoneE164.matcher(normalized).matches()) {
            return true;
        } else if (zaPhoneE164Plus.matcher(normalized).matches()) {
            return true;
        } else if (nationalRegex.matcher(normalized).matches()) {
            return true;
        } else {
            return false;
        }
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
