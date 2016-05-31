// Decompiled by Jad v1.5.8e. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.geocities.com/kpdus/jad.html
// Decompiler options: braces fieldsfirst space lnc 

package org.grassroot.android.ui.fragments;


import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import org.grassroot.android.R;
import org.grassroot.android.interfaces.AlertDialogListener;


public class NotificationDialog extends android.support.v4.app.DialogFragment
{

    AlertDialogListener mAlertDialogListener;

    public NotificationDialog() {
    }

    public static NotificationDialog newInstance(String s, String s1, String s2, String s3, boolean flag)
    {
        NotificationDialog alertdialogfragment = new NotificationDialog();
        Bundle bundle = new Bundle();
        bundle.putString("title", s);
        bundle.putString("message", s1);
        bundle.putString("left_text", s2);
        bundle.putString("right_text", s3);
        bundle.putBoolean("ok_button", flag);
        alertdialogfragment.setArguments(bundle);
        return alertdialogfragment;
    }

    public Dialog onCreateDialog(Bundle bundle)
    {

      String  bundle1 = getArguments().getString("title");
        String s = getArguments().getString("message");
        Object obj = getArguments().getString("left_text");
        String s1 = getArguments().getString("right_text");
        boolean flag = getArguments().getBoolean("ok_button");
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(getActivity());
        View view = getActivity().getLayoutInflater().inflate(R.layout.alert_error_dialog, null);
        TextView textview = (TextView)view.findViewById(R.id.txt_error_title);
        TextView textview1 = (TextView)view.findViewById(R.id.txt_error_message);
        TextView textview2 = (TextView)view.findViewById(R.id.txt_error_ok);
        textview2.setText(((CharSequence) (obj)));
        obj = (TextView)view.findViewById(R.id.txt_error_try_again);
        ((TextView) (obj)).setText(s1);
        textview2.setVisibility(View.VISIBLE);
        if (flag)
        {
            textview2.setVisibility(View.GONE);
        }
        textview2.setOnClickListener(new View.OnClickListener() {

            final NotificationDialog this$0;

            public void onClick(View view1)
            {
                mAlertDialogListener.setLeftButton();
            }

            
            {
                this$0 = NotificationDialog.this;
                //super();
            }
        });
        ((TextView) (obj)).setOnClickListener(new View.OnClickListener() {

            final NotificationDialog this$0;

            public void onClick(View view1)
            {
                mAlertDialogListener.setRightButton();
            }

            
            {
                this$0 = NotificationDialog.this;
                //super();
            }
        });
        textview.setText(bundle1);
        textview1.setText(s);
        builder.setView(view);
        return builder.create();
    }

    public void setListener(AlertDialogListener alertdialoglistener)
    {
        mAlertDialogListener = alertdialoglistener;
    }


}
