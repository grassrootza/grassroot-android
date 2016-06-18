package org.grassroot.android.services;

import android.app.Application;
import android.content.Context;

/**
 * Created by luke on 2016/06/17.
 */
public class ApplicationLoader extends Application {

    public static volatile Context applicationContext;

    @Override
    public void onCreate() {
        super.onCreate();

        applicationContext = getApplicationContext();

    }

    // todo : turn location utils into a service started here

}
