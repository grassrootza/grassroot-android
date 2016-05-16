package com.techmorphosis.grassroot.utils;

import android.content.Context;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.view.View;

import com.techmorphosis.grassroot.services.NoConnectivityException;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;


/**
 * Created by luke on 2016/05/15.
 */
public class ErrorUtils {

    private static final String TAG = ErrorUtils.class.getCanonicalName();

    /**
     * Utility method to intercept and deal with common errors, in particular absence of a network
     * connection. Most / all display logic for error handling should be concentrated in here.
     * @param context The context where the error occurred
     * @param errorViewHolder A holder for where to display the snackbar or alert dialog
     * @param e The error thrown
     */
    public static void handleNetworkError(Context context, View errorViewHolder, Throwable e) {
        if (e instanceof NoConnectivityException) {
            connectivityError(errorViewHolder, (NoConnectivityException) e);
        } else if (e instanceof UnknownHostException) {
            hostError(errorViewHolder, (UnknownHostException) e);
        } else if (e instanceof SocketTimeoutException) {
            socketError(errorViewHolder, (SocketTimeoutException) e);
        } else if (e instanceof IOException) {
            otherIOError(errorViewHolder, (IOException) e);
        } else {
            Log.e(TAG, "Error! Should not be another kind of error here! Output: " + e.toString());
        }
    }

    public static void connectivityError(View holder, NoConnectivityException e) {
        // todo : all sorts of things for persisting, asking to turn on, etc
        final String errorText =  "Connectivity error! On calling URL: " + e.getUriAttempted();
        Snackbar snackBar = Snackbar.make(holder, errorText, Snackbar.LENGTH_INDEFINITE);
        snackBar.show();
    }

    public static void hostError(View holder, UnknownHostException e) {
        final String errorText = "Host error! With message: " + e.getMessage();
        Snackbar snackBar = Snackbar.make(holder, errorText, Snackbar.LENGTH_LONG);
        snackBar.show();
    }

    public static void otherIOError(View holder, IOException e) {
        final String errorText = "Other IO error! With message: " + e.getMessage();
        Snackbar snackBar = Snackbar.make(holder, errorText, Snackbar.LENGTH_LONG);
        snackBar.show();
    }

    public static void socketError(View holder, SocketTimeoutException e) {
        final String errorText = "Sorry! Our server may be down: " + e.getMessage();
        Snackbar snackBar = Snackbar.make(holder, errorText, Snackbar.LENGTH_LONG);
        snackBar.show();
    }

}
