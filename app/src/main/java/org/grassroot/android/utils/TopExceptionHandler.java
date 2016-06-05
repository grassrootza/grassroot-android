package org.grassroot.android.utils;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import java.io.FileOutputStream;

/**
 * Created by luke on 2016/05/06.
 */
public class TopExceptionHandler implements Thread.UncaughtExceptionHandler {

    private Thread.UncaughtExceptionHandler defaultHandler;
    private Activity crashedActivity = null;

    public TopExceptionHandler(Activity crashedActivity) {
        this.defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        this.crashedActivity = crashedActivity;
    }

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        StackTraceElement[] arr = e.getStackTrace();
        String report = e.toString() + "\n\n";
        report += "----- STACK TRACE ----";
        for (int i = 0; i < arr.length; i++) {
            report += "  " + arr[i].toString() + "\n";
        }
        report += "----- STACK TRACE ----";

        report += "----- CAUSE -----";
        Throwable cause = e.getCause();
        if (cause != null) {
            report += cause.toString() + "\n\n";
            StackTraceElement[] arr2 = cause.getStackTrace();
            for (int i = 0; i < arr2.length; i++) {
                report += "  " + arr2[i].toString() + "\n";
            }
            report += "----- CAUSE -----";
        }

        try {
            FileOutputStream trace = crashedActivity.openFileOutput("stack.trace", Context.MODE_PRIVATE);
            trace.write(report.getBytes());
            trace.close();
        } catch (Exception exception) {
            Log.w("GRASSROOT CRASH: ", report);
        }

        defaultHandler.uncaughtException(t, e);

    }

}
