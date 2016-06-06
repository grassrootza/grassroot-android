package org.grassroot.android.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;

import org.grassroot.android.R;
import org.grassroot.android.interfaces.AlertDialogListener;
import org.grassroot.android.interfaces.NetworkErrorDialogListener;
import org.grassroot.android.services.NoConnectivityException;
import org.grassroot.android.ui.activities.StartActivity;
import org.grassroot.android.ui.fragments.AlertDialogFragment;
import org.grassroot.android.ui.fragments.NetworkErrorDialogFragment;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import retrofit2.Response;


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

    public static void handleServerError(View holder, final Activity activity, Response response){

        switch (response.code()){
            case Constant.UNAUTHORISED:
                showSnackBar(holder,activity.getString(R.string.INVALID_TOKEN),Snackbar.LENGTH_INDEFINITE,"Log Out", new View.OnClickListener(){
                    @Override
                    public void onClick(View v) {
                        PreferenceUtils.clearAll(activity.getApplicationContext());
                        Intent open = new Intent(activity, StartActivity.class);
                        activity.startActivity(open);
                        activity.finish();
                    }
                });
                break;
            case Constant.CONFLICT:
                Snackbar.make(holder, R.string.Alert_Already_Responded, Snackbar.LENGTH_LONG).show();
                break;

            case Constant.INTERNAL_SERVER_ERROR:
                Snackbar.make(holder, "Something went wrong.  Please try again later...", Snackbar.LENGTH_INDEFINITE).show();
                break;


        }
    }

    public static void connectivityError(View holder, NoConnectivityException e) {
        // todo : all sorts of things for persisting, asking to turn on, etc
        final String errorText =  "Connectivity error! On calling URL: " + e.getUriAttempted();
        Snackbar snackBar = Snackbar.make(holder, errorText, Snackbar.LENGTH_INDEFINITE);
        snackBar.show();
    }


    public static void connectivityError(Activity context, int message, NetworkErrorDialogListener networkErrorDialogListener){
        NetworkErrorDialogFragment networkErrorDialogFragment = NetworkErrorDialogFragment.newInstance(message, networkErrorDialogListener);
        networkErrorDialogFragment.show(context.getFragmentManager(), "error_dialog");

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
        try {
            Snackbar snackBar = Snackbar.make(holder, errorText, Snackbar.LENGTH_LONG);
            snackBar.show();
        } catch (Exception e1) {
            Log.e(TAG, "Socket error on startup");
        }
    }

    public static void showSnackBar(View holder, final String message, int length, final String actionText,
                                    View.OnClickListener actionToTake) {
        Snackbar snackbar = Snackbar.make(holder, message, length);
        if (actionText != null && actionToTake != null) {
            snackbar.setActionTextColor(Color.RED);
            snackbar.setAction(actionText, actionToTake);
        }
        snackbar.show();
    }

    public static void showSnackBar(View holder, final int messageRes, int length) {
        Snackbar snackbar = Snackbar.make(holder, messageRes, length);
        snackbar.show();
    }

}
