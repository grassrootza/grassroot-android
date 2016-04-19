package com.techmorphosis.grassroot.Interface;

import android.view.View;

/**
 * Created by ravi on 6/4/16.
 */
public  interface ClickListener {
    public void onClick(View view, int position);

    public void onLongClick(View view, int position);
}