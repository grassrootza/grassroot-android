// Decompiled by Jad v1.5.8e. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.geocities.com/kpdus/jad.html
// Decompiler options: braces fieldsfirst space lnc 

package com.techmorphosis.grassroot.utils;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;


// Referenced classes of package com.techmorphosis.audioone.utils:
//            BUTTON_TYPE

public class UIUtils
{

    public UIUtils()
    {
    }

   /* public static AlertDialogFragment buildAlertDialog(Context context, String s, String s1, String s2, String s3, boolean flag, DialogButtonsListener dialogbuttonslistener)
    {
        context = (new AlertDialogFragment.Builder(context)).create();
        context.setMessage(s1);
        context.setTitle(s);
        context.setCancelable(flag);
        context.setButton(-1, s2, new DialogInterface.OnClickListener(dialogbuttonslistener) {

            final DialogButtonsListener val$buttonsListener;

            public void onClick(DialogInterface dialoginterface, int i)
            {
                buttonsListener.onPositiveButtonPress();
            }

            
            {
                buttonsListener = dialogbuttonslistener;
                super();
            }
        });
        context.setButton(-2, s3, new DialogInterface.OnClickListener(dialogbuttonslistener) {

            final DialogButtonsListener val$buttonsListener;

            public void onClick(DialogInterface dialoginterface, int i)
            {
                buttonsListener.onNegativeButtonPress();
                dialoginterface.dismiss();
            }

            
            {
                buttonsListener = dialogbuttonslistener;
                super();
            }
        });
        return context;
    }*/

   /* public static AlertDialogFragment buildAlertDialog(Context context, String s, String s1, String s2, boolean flag)
    {
        context = (new AlertDialogFragment.Builder(context)).create();
        context.setMessage(s1);
        context.setTitle(s);
        context.setCancelable(flag);
        context.setButton(-1, s2, new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialoginterface, int i)
            {
                dialoginterface.dismiss();
            }

        });
        return context;
    }*/

 /*   public static AlertDialogFragment buildAlertDialog(Context context, String s, String s1, String s2, boolean flag, DialogButtonsListener dialogbuttonslistener)
    {
        context = (new AlertDialogFragment.Builder(context)).create();
        context.setMessage(s1);
        context.setTitle(s);
        context.setCancelable(flag);
        context.setButton(-1, s2, new DialogInterface.OnClickListener(dialogbuttonslistener) {

            final DialogButtonsListener val$buttonsListener;

            public void onClick(DialogInterface dialoginterface, int i)
            {
                buttonsListener.onPositiveButtonPress();
            }

            
            {
                buttonsListener = dialogbuttonslistener;
                super();
            }
        });
        return context;
    }
*/
    public static void hideKeyboard(Activity activity)
    {
        activity.getWindow().setSoftInputMode(2);
    }

    public static void replaceView(ViewGroup viewgroup, View view, View view1)
    {
       // StartActivity.exit=true;
        viewgroup.addView(view);
        viewgroup.removeView(view1);
    }

   /* public static void setupToolbar(View view, String s)
    {
        ((TextView)view.findViewById(0x7f0d0067)).setText(s);
    }
*/
    /*public static void setupToolbar(View view, String s, BUTTON_TYPE button_type, View.OnClickListener onclicklistener)
    {
        ((TextView)view.findViewById(0x7f0d0067)).setText(s);
        static class _cls5
        {

            static final int $SwitchMap$com$techmorphosis$audioone$utils$BUTTON_TYPE[];

            static 
            {
                $SwitchMap$com$techmorphosis$audioone$utils$BUTTON_TYPE = new int[BUTTON_TYPE.values().length];
                try
                {
                    $SwitchMap$com$techmorphosis$audioone$utils$BUTTON_TYPE[BUTTON_TYPE.MENU.ordinal()] = 1;
                }
                catch (NoSuchFieldError nosuchfielderror1) { }
                try
                {
                    $SwitchMap$com$techmorphosis$audioone$utils$BUTTON_TYPE[BUTTON_TYPE.BACK.ordinal()] = 2;
                }
                catch (NoSuchFieldError nosuchfielderror)
                {
                    return;
                }
            }
        }

        switch (_cls5..SwitchMap.com.techmorphosis.audioone.utils.BUTTON_TYPE[button_type.ordinal()])
        {
        default:
            return;

        case 1: // '\001'
            view.findViewById(0x7f0d00bd).setOnClickListener(onclicklistener);
            return;

        case 2: // '\002'
            view.findViewById(0x7f0d0094).setOnClickListener(onclicklistener);
            break;
        }
    }*/
}
