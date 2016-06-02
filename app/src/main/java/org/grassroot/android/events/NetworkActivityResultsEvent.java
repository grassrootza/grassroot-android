package org.grassroot.android.events;

import android.content.Intent;

import org.grassroot.android.ui.fragments.NetworkErrorDialogFragment;

/**
 * Created by paballo on 2016/06/02.
 */
public class NetworkActivityResultsEvent {
    private int requestCode;
    private int resultCode;
    private Intent data;
    public NetworkActivityResultsEvent(int requestCode, int resultCode, Intent data){
        this.requestCode =requestCode;
        this.resultCode = resultCode;
         this.data =data;

    }


    public int getRequestCode() {
        return requestCode;
    }

    public int getResultCode() {
        return resultCode;
    }

    public Intent getData() {
        return data;
    }
}
