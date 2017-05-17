package org.grassroot.android.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.support.design.widget.Snackbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import org.grassroot.android.R;
import org.grassroot.android.activities.HomeScreenActivity;
import org.grassroot.android.activities.NoGroupWelcomeActivity;
import org.grassroot.android.activities.StartActivity;
import org.grassroot.android.interfaces.NavigationConstants;
import org.grassroot.android.models.Member;
import org.grassroot.android.models.exceptions.ApiCallException;
import org.grassroot.android.models.responses.ServerErrorModel;
import org.grassroot.android.services.ApplicationLoader;
import org.grassroot.android.services.GrassrootRestService;
import org.grassroot.android.services.TaskService;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import okhttp3.ResponseBody;
import retrofit2.Converter;

/**
 * Created by luke on 2016/05/15.
 */
public class ErrorUtils {

    private static final String TAG = ErrorUtils.class.getSimpleName();

    public static final String TOKEN_EXPIRED = "TOKEN_EXPIRED";
    public static final String INVALID_TOKEN = "INVALID_TOKEN";

    // server errors
    private static final String GENERIC_ERROR = "BAD_REQUEST";
    private static final String USER_INVALID_MSISDN = "INVALID_MSISDN";
    public static final String USER_EXISTS = "USER_ALREADY_EXISTS";
    public static final String USER_DOESNT_EXIST = "USER_DOES_NOT_EXIST";
    public static final String WRONG_OTP = "INVALID_OTP";
    private static final String OTP_EARLY_REQ = "OTP_REQ_BEFORE_ADD";

    private static final String GROUP_CREATE_ERROR = "GROUP_NOT_CREATED";
    public static final String PERMISSION_DENIED = "PERMISSION_DENIED";
    private static final String PART_GROUP = "USER_ALREADY_PART_OF_GROUP";
    private static final String CANT_APPROVE_JOIN = "APPROVER_PERMISSIONS_CHANGED";
    public static final String GROUP_MEMBER_INVALID_PHONE = "GROUP_BAD_PHONE_NUMBER";
    private static final String IMAGE_ERROR = "BAD_PICTURE_FORMAT";
    private static final String NO_IMAGE_SENT = "PICTURE_NOT_RECEIVED";
    private static final String DATE_IN_PAST = "TIME_CANNOT_BE_IN_THE_PAST";
    private static final String MEETING_CANCELLED = "MEETING_ALREADY_CANCELLED";
    private static final String MEETING_OVER = "PAST_DUE";
    private static final String VOTE_DONE = "USER_HAS_ALREADY_VOTED";
    private static final String VOTE_CLOSED = "VOTE_CLOSED";
    private static final String VOTE_CANCELLED = "VOTE_ALREADY_CANCELLED";
    private static final String TODO_DONE = "TODO_ALREADY_COMPLETED";
    private static final String NOTIFICATIONS_DONE = "NOTIFICATIONS_FINISHED";
    private static final String ALREADY_LEFT = "MEMBER_ALREADY_LEFT";
    private static final String MESSAGE_SETTING_NOT_FOUND = "MESSAGE_SETTING_NOT_FOUND";

    public static final String JREQ_NOT_FOUND = "GROUP_JOIN_REQUEST_NOT_FOUND";

    public static final String TODO_LIMIT_REACHED = "TODO_LIMIT_REACHED";
    public static final String GROUP_SIZE_LIMIT = "GROUP_SIZE_LIMIT";

    private static final String ACCOUNT_GROUPS_EXHAUSTED = "ACCOUNT_GROUPS_EXHAUSTED";
    private static final String GROUP_ALREADY_PAID_FOR = "GROUP_ALREADY_PAID_FOR";
    private static final String GROUP_ACCOUNT_WRONG = "GROUP_ACCOUNT_WRONG";
    private static final String GROUP_NOT_PAID_FOR = "GROUP_NOT_PAID_FOR";
    private static final String FREE_FORM_EXHAUSTED = "FREE_FORM_EXHAUSTED";
    private static final String MUST_REMOVE_PAID_GROUPS = "MUST_REMOVE_PAID_GROUPS";
    private static final String ERROR_REMOVING_GROUPS = "ERROR_REMOVING_GROUPS";


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
                case TOKEN_EXPIRED:
                    return context.getString(R.string.server_error_expired_auth);
                case INVALID_TOKEN:
                    return context.getString(R.string.server_error_invalid_auth);
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
                case ALREADY_LEFT:
                    return context.getString(R.string.server_error_unsubscribe_error);
                case MESSAGE_SETTING_NOT_FOUND:
                    return context.getString(R.string.server_error_no_chat_settings);
                case ACCOUNT_GROUPS_EXHAUSTED:
                    return context.getString(R.string.account_no_more_groups);
                case GROUP_ALREADY_PAID_FOR:
                    return context.getString(R.string.account_group_already_paid);
                case GROUP_ACCOUNT_WRONG:
                    return context.getString(R.string.account_group_mismatch);
                case GROUP_NOT_PAID_FOR:
                    return context.getString(R.string.account_group_not_paid_for);
                case FREE_FORM_EXHAUSTED:
                    return context.getString(R.string.account_no_more_fform);
                case MUST_REMOVE_PAID_GROUPS:
                    return context.getString(R.string.account_must_remove_groups);
                case ERROR_REMOVING_GROUPS:
                    return context.getString(R.string.account_group_remove_error);
                default:
                    return context.getString(R.string.server_error_general);
            }
        }
    }

    public static Intent gracefulExitToHome(Activity callingActivity) {
        Toast.makeText(ApplicationLoader.applicationContext,
            ApplicationLoader.applicationContext.getText(R.string.application_crash_error),
            Toast.LENGTH_SHORT).show();
        try {
            return RealmUtils.loadPreferencesFromDB().isHasGroups() ?
                new Intent(callingActivity, HomeScreenActivity.class) :
                new Intent(callingActivity, NoGroupWelcomeActivity.class);
        } catch (Exception e) {
            // just in case there is some error with Realm that makes this happen
            return new Intent(callingActivity, StartActivity.class);
        }
    }

    public static Intent gracefulExitToTasks(Activity callingActivity) {
        Intent i = new Intent(callingActivity, HomeScreenActivity.class);
        i.putExtra(NavigationConstants.HOME_OPEN_ON_NAV, NavigationConstants.ITEM_TASKS);
        Toast.makeText(ApplicationLoader.applicationContext, R.string.application_notify_crash, Toast.LENGTH_SHORT).show();
        TaskService.getInstance().fetchUpcomingTasks(AndroidSchedulers.mainThread()).subscribe(new Consumer<String>() {
            @Override
            public void accept(String s) {
                Log.e(TAG, "At least the network call for refresh succeeded");
            }
        }, new Consumer<Throwable>() {
            @Override
            public void accept(Throwable throwable) {
                Log.e(TAG, "Error! The call to refresh tasks also failed");
            }
        });
        return i;
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

    public static String serverErrorText(ResponseBody errorBody) {
        return serverErrorText(getRestMessage(errorBody));
    }

    public static String serverErrorText(Throwable e) {
        final String restMsg = (e instanceof ApiCallException) ? ((ApiCallException) e).errorTag : GENERIC_ERROR;
        return serverErrorText(restMsg);
    }

    public static boolean isAccessDeniedError(Throwable e) {
        return e instanceof ApiCallException && PERMISSION_DENIED.equals(((ApiCallException) e).errorTag);
    }

    public static void networkErrorSnackbar(ViewGroup container, final int message, View.OnClickListener action) {
        Snackbar snackbar = Snackbar.make(container, message, Snackbar.LENGTH_LONG);
        snackbar.setActionTextColor(Color.RED);
        snackbar.setAction(R.string.snackbar_try_again, action);
        snackbar.show();
    }

    public static void snackBarWithAction(View holder, final int message, final int actionText, View.OnClickListener actionToTake) {
        Snackbar snackbar = Snackbar.make(holder, message, Snackbar.LENGTH_LONG);
        snackbar.setActionTextColor(Color.RED);
        snackbar.setAction(actionText, actionToTake);
        snackbar.show();
    }

    /*
    Wrap intent calls in this to avoid crashes
     */
    public static boolean isCallable(Intent intent) {
        List<ResolveInfo> list = ApplicationLoader.applicationContext.getPackageManager()
            .queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
        return !list.isEmpty();
    }

    /*
    Convenience method to work out if error is from token problems
     */
    public static boolean isTokenError(Throwable throwable) {
        if (throwable instanceof ApiCallException) {
            ApiCallException e = (ApiCallException) throwable;
            return TOKEN_EXPIRED.equals(e.errorTag) || INVALID_TOKEN.equals(e.errorTag);
        } else {
            return false;
        }
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

}