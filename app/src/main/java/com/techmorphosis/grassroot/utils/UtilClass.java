// Decompiled by Jad v1.5.8e. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.geocities.com/kpdus/jad.html
// Decompiler options: braces fieldsfirst space lnc 

package com.techmorphosis.grassroot.utils;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.view.View;
import android.widget.Toast;

import com.techmorphosis.grassroot.ui.DialogFragment.AlertDialogFragment;
import com.techmorphosis.grassroot.ui.DialogFragment.NotificationDialog;
import com.techmorphosis.grassroot.utils.listener.AlertDialogListener;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

// Referenced classes of package com.techmorphosis.Utils:
//            AlertDialogListener, DeliveryScheduleListner

public class UtilClass
{

 public static String MAKE;
    public static String MODEL;
    public static String OS;
    FragmentTransaction mTransaction;
    public Snackbar snackBar;

    public UtilClass()
    {
        OS = Build.VERSION.RELEASE;
        MAKE = Build.MANUFACTURER;
        MODEL = Build.MODEL;

    }


    public void showToast(Context context, String s)
    {
        Toast.makeText(context, s, Toast.LENGTH_SHORT).show();
    }

    public void showsnackBar(View v,Context applicationContext, String message)
    {

        snackBar= Snackbar.make(v, message, Snackbar.LENGTH_SHORT);
        snackBar.show();
    }

    public AlertDialogFragment showAlerDialog(FragmentManager manager,String message,String left,String right,Boolean flag,AlertDialogListener alertDialogListener)
    {
        AlertDialogFragment ss2= new AlertDialogFragment();
        Bundle b= new Bundle();
        b.putString("message", message);
        b.putBoolean("cancelable", flag);
        b.putString("left", left);
        b.putString("right",right);
        ss2.setArguments(b);
        ss2.setListener(alertDialogListener);
        ss2.show(manager,null);
        return ss2;

    }

    public NotificationDialog showAlertDialog1(android.support.v4.app.FragmentManager fragmentmanager, String s, String s1, String s2, String s3, boolean flag, AlertDialogListener alertdialoglistener)
    {
        NotificationDialog ss1 = NotificationDialog.newInstance(s, s1, s2, s3, flag);
        ss1.setListener(alertdialoglistener);
        ss1.setCancelable(false);
        ss1.show(fragmentmanager, null);
        return ss1;
    }

    public  String timeZone()
    {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT"), Locale.getDefault());
        String   timeZone = new SimpleDateFormat("Z").format(calendar.getTime());
        return timeZone.substring(0, 3) + ":"+ timeZone.substring(3, 5);
    }






}
