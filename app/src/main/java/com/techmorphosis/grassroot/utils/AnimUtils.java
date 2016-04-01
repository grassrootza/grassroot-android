// Decompiled by Jad v1.5.8e. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.geocities.com/kpdus/jad.html
// Decompiler options: braces fieldsfirst space lnc 

package com.techmorphosis.grassroot.utils;

import android.content.Context;
import android.view.View;
import android.view.animation.AnimationUtils;

import com.techmorphosis.grassroot.R;


public class AnimUtils
{

    public AnimUtils()
    {
    }

    public static void backwardAnimation(Context context, View view, View view1)
    {
        view.startAnimation(AnimationUtils.loadAnimation(context, R.anim.a_slide_in_left));
        view1.startAnimation(AnimationUtils.loadAnimation(context, R.anim.a_slide_out_right));
    }

    public static void forwardAnimation(Context context, View view, View view1)
    {
        view.startAnimation(AnimationUtils.loadAnimation(context, R.anim.a_slide_in_right));
        view1.startAnimation(AnimationUtils.loadAnimation(context, R.anim.a_slide_out_left));
    }
}
