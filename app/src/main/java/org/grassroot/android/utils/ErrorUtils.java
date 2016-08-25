package org.grassroot.android.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import org.grassroot.android.R;
import org.grassroot.android.activities.HomeScreenActivity;
import org.grassroot.android.activities.StartActivity;
import org.grassroot.android.fragments.dialogs.NetworkErrorDialogFragment;
import org.grassroot.android.interfaces.NetworkErrorDialogListener;
import org.grassroot.android.models.Member;
import org.grassroot.android.models.ServerErrorModel;
import org.grassroot.android.models.exceptions.ApiCallException;
import org.grassroot.android.services.ApplicationLoader;
import org.grassroot.android.services.GrassrootRestService;
import org.grassroot.android.services.NoConnectivityException;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Converter;
import retrofit2.Response;


/**
 * Created by luke on 2016/05/15.
 */
public class ErrorUtils {

    private static final String TAG = ErrorUtils.class.getSimpleName();

    // server errors
    public static final String GENERIC_ERROR = "BAD_REQUEST";
    public static final String USER_INVALID_MSISDN = "INVALID_MSISDN";
    public static final String USER_EXISTS = "USER_ALREADY_EXISTS";
    public static final String USER_DOESNT_EXIST = "USER_DOES_NOT_EXIST";
    public static final String WRONG_OTP = "INVALID_OTP";
    public static final String OTP_EARLY_REQ = "OTP_REQ_BEFORE_ADD";
    public static final String GROUP_CREATE_ERROR = "GROUP_NOT_CREATED";
    public static final String PERMISSION_DENIED = "PERMISSION_DENIED";
    public static final String PART_GROUP = "USER_ALREADY_PART_OF_GROUP";
    public static final String CANT_APPROVE_JOIN = "APPROVER_PERMISSIONS_CHANGED";
    public static final String GROUP_MEMBER_INVALID_PHONE = "GROUP_BAD_PHONE_NUMBER";
    public static final String IMAGE_ERROR = "BAD_PICTURE_FORMAT";
    public static final String NO_IMAGE_SENT = "PICTURE_NOT_RECEIVED";
    public static final String DATE_IN_PAST = "TIME_CANNOT_BE_IN_THE_PAST";
    public static final String MEETING_CANCELLED = "MEETING_ALREADY_CANCELLED";
    public static final String MEETING_OVER = "PAST_DUE";
    public static final String VOTE_DONE = "USER_HAS_ALREADY_VOTED";
    public static final String VOTE_CLOSED = "VOTE_CLOSED";
    public static final String VOTE_CANCELLED = "VOTE_ALREADY_CANCELLED";
    public static final String TODO_DONE = "TODO_ALREADY_COMPLETED";
    public static final String NOTIFICATIONS_DONE = "NOTIFICATIONS_FINISHED";

    public static String serverErrorText(final String restMessage) {
        final Context context = ApplicationLoader.applicationContext;
        if (TextUtils.isEmpty(restMessage)) {
            return context.getString(R.string.server_error_general);
        } else {
            switch (restMessage) {
                case USER_INVALID_MSISDN:
                    return context.getString(R.string.server_error_invalid_msisdn);
                case USER_EXISTS:
                    return context.getString(R.string.server_error_user_exists);
                case USER_DOESNT_EXIST:
                    return context.getString(R.string.server_error_user_not_exist);
                case WRONG_OTP:
                    return context.getString(R.string.server_error_otp_wrong);
                case OTP_EARLY_REQ:
                    return context.getString(R.string.server_error_otp_early);
                case GROUP_CREATE_ERROR:
                    return context.getString(R.string.server_error_group_create);
                case GROUP_MEMBER_INVALID_PHONE:
                    return context.getString(R.string.server_error_phone_number);
                case PERMISSION_DENIED:
                    return context.getString(R.string.server_error_perms_denied);
                case IMAGE_ERROR:
                    return context.getString(R.string.server_error_image_error);
                case NO_IMAGE_SENT:
                    return context.getString(R.string.server_error_image_none);
                case PART_GROUP:
                    return context.getString(R.string.server_error_part_of_group);
                case CANT_APPROVE_JOIN:
                    return context.getString(R.string.server_error_cant_approve_join);
                case DATE_IN_PAST:
                    return context.getString(R.string.server_error_date_in_past);
                case MEETING_CANCELLED:
                    return context.getString(R.string.server_error_meeting_cancelled);
                case MEETING_OVER:
                    return context.getString(R.string.server_error_meeting_over);
                case VOTE_DONE:
                    return context.getString(R.string.server_error_already_voted);
                case VOTE_CLOSED:
                    return context.getString(R.string.server_error_vote_closed);
                case VOTE_CANCELLED:
                    return context.getString(R.string.server_error_vote_cancelled);
                case TODO_DONE:
                    return context.getString(R.string.server_error_todo_completed);
                case NOTIFICATIONS_DONE:
                    return context.getString(R.string.server_error_notifications_done);
                default:
                    return context.getString(R.string.server_error_general);
            }
        }
    }

    // todo : use this in an uncaught exception handler
    public static Intent gracefulExitToHome(Activity callingActivity) {
        Toast.makeText(ApplicationLoader.applicationContext,
            ApplicationLoader.applicationContext.getText(R.string.application_crash_error),
            Toast.LENGTH_SHORT).show();
        return new Intent(callingActivity, HomeScreenActivity.class);
    }

    // remember : conversion will consume the JSON, so can call this at most once in a given flow
    public static ServerErrorModel convertErrorBody(ResponseBody errorBody) {
        Converter<ResponseBody, ServerErrorModel> converter = GrassrootRestService.getInstance()
            .getRetrofit().responseBodyConverter(ServerErrorModel.class, new Annotation[0]);
        try {
            ServerErrorModel errorModel = converter.convert(errorBody);
            return errorModel;
        } catch (Exception e) {
            Log.d(TAG, "something went wrong converting error model... printing stacktrace");
            e.printStackTrace();
            return null;
        }
    }

    public static String getRestMessage(final ResponseBody errorBody) {
        final ServerErrorModel errorModel = convertErrorBody(errorBody);
        return errorModel != null ? errorModel.getMessage() : GENERIC_ERROR;
    }

    public static String serverErrorText(ResponseBody errorBody, final Context context) {
        return serverErrorText(getRestMessage(errorBody));
    }

    public static String serverErrorText(Throwable e) {
        final String restMsg = (e instanceof ApiCallException) ? ((ApiCallException) e).errorTag : GENERIC_ERROR;
        return serverErrorText(restMsg);
    }

    /*
    Utility method to retrieve a list of members from a string of bad phone numbers
     */
    public static List<Member> findMembersFromListOfNumbers(final String listOfInvalidNumbers,
                                                            final List<Member> referenceList) {
        List<String> invalidNumbers = TextUtils.isEmpty(listOfInvalidNumbers) ? null :
            Arrays.asList(listOfInvalidNumbers.split("\\s+"));
        return findMembersFromListOfNumbers(invalidNumbers, referenceList);
    }

    public static List<Member> findMembersFromListOfNumbers(final List<String> listOfInvalidNumbers,
                                                            final List<Member> referenceList) {
        List<Member> foundMembers = new ArrayList<>();
        if (listOfInvalidNumbers != null && referenceList != null) {
            for (String phoneNum : listOfInvalidNumbers) {
                final String msisdn = Utilities.formatNumberToE164(phoneNum);
                for (Member m : referenceList) {
                    if (m.getPhoneNumber().equals(msisdn) || m.getPhoneNumber().equals(phoneNum)) {
                        foundMembers.add(m);
                        break;
                    }
                }
            }
        }
        return foundMembers;
    }

    /**
     * Utility method to intercept and deal with common errors, in particular absence of a network
     * connection. Most / all display logic for error handling should be concentrated in here.
     *  @param errorViewHolder A holder for where to display the snackbar or alert dialog
     * @param e               The error thrown
     */
    public static void handleNetworkError(View errorViewHolder, Throwable e) {
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

    // todo : somehow intercept this everywhere ...
    public static void handleBadAuthToken(final View holder, final Activity activity, final Response response) {
        showSnackBar(holder, activity.getString(R.string.INVALID_TOKEN), Snackbar.LENGTH_LONG,
            activity.getString(R.string.LOG_OUT), new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    RealmUtils.deleteAllObjects();
                    Intent open = new Intent(activity, StartActivity.class);
                    activity.startActivity(open);
                    activity.finish();
                }
            });
    }

    // todo : double check if we still want / replace
    public static void connectivityError(View holder, NoConnectivityException e) {
        // todo : all sorts of things for persisting, asking to turn on, etc
        final String errorText = "Connectivity error! On calling URL: " + e.getUriAttempted();
        Snackbar snackBar = Snackbar.make(holder, errorText, Snackbar.LENGTH_INDEFINITE);
        snackBar.show();
    }

    public static void connectivityError(Activity context, int message, NetworkErrorDialogListener networkErrorDialogListener) {
        NetworkErrorDialogFragment networkErrorDialogFragment = NetworkErrorDialogFragment.newInstance(message, networkErrorDialogListener);
        networkErrorDialogFragment.show(((FragmentActivity) context).getSupportFragmentManager(), "error_dialog");
    }

    public static void networkErrorSnackbar(ViewGroup container, int message, View.OnClickListener action) {
        Snackbar snackbar = Snackbar.make(container, message, Snackbar.LENGTH_LONG);
        snackbar.setActionTextColor(Color.RED);
        snackbar.setAction(R.string.snackbar_try_again, action);
    }

    public static void hostError(View holder, UnknownHostException e) {
        final String errorText = "Host error! With message: " + e.getMessage();
        Snackbar.make(holder, errorText, Snackbar.LENGTH_LONG).show();
    }

    public static void otherIOError(View holder, IOException e) {
        final String errorText = "Other IO error! With message: " + e.getMessage();
        Snackbar.make(holder, errorText, Snackbar.LENGTH_LONG).show();
    }

    public static void socketError(View holder, SocketTimeoutException e) {
        final String errorText = "Sorry! Our server may be down: " + e.getMessage();
        try {
            Snackbar.make(holder, errorText, Snackbar.LENGTH_LONG).show();
        } catch (Exception e1) {
            Log.e(TAG, "Socket error on startup");
        }
    }

    // todo : consolidate these
    public static void snackBarWithAction(View holder, final int message, final int actionText, View.OnClickListener actionToTake) {
        Snackbar snackbar = Snackbar.make(holder, message, Snackbar.LENGTH_LONG);
        snackbar.setActionTextColor(Color.RED);
        snackbar.setAction(actionText, actionToTake);
        snackbar.show();
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