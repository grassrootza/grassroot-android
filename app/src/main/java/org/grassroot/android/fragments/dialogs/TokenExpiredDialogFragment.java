package org.grassroot.android.fragments.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.widget.Toast;

import org.grassroot.android.R;
import org.grassroot.android.utils.LoginRegUtils;
import org.grassroot.android.utils.NetworkUtils;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.SingleEmitter;
import io.reactivex.SingleOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by luke on 2017/05/10.
 */

public class TokenExpiredDialogFragment extends DialogFragment {

    private static final String TAG = TokenExpiredDialogFragment.class.getSimpleName();

    private Observable actionOnSuccess;
    private Consumer<?> successConsumer;
    private Consumer<Throwable> errorConsumer;

    private FragmentManager fragmentManager;
    private Integer overrideMessage;

    private int numberOtpAttempts;
    private EditTextDialogFragment.EditTextDialogListener otpListener;

    public static Single showTokenExpiredDialogs(final FragmentManager fragmentManager,
                                                 final Integer overrideMessageRes,
                                                 final Observable actionOnSuccess,
                                                 final Consumer<?> successConsumer,
                                                 final Consumer<Throwable> errorConsumer) {
        return Single.create(new SingleOnSubscribe() {
            @Override
            public void subscribe(SingleEmitter subscriber) throws Exception {
                TokenExpiredDialogFragment fragment = new TokenExpiredDialogFragment();
                fragment.actionOnSuccess = actionOnSuccess;
                fragment.fragmentManager = fragmentManager;
                fragment.overrideMessage = overrideMessageRes;
                fragment.numberOtpAttempts = 0;
                fragment.successConsumer = successConsumer;
                fragment.errorConsumer = errorConsumer;
                fragment.show(fragmentManager, "first_dialog");
            }
        });
    }

    public static Single showTokenExpiredDialogs(final FragmentManager fragmentManager,
                                                 final Observable actionOnSuccess) {
        return showTokenExpiredDialogs(fragmentManager, null, actionOnSuccess, null, null);
    }

    @Override
    @android.support.annotation.NonNull
    @SuppressWarnings("unchecked")
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());

        final String otpHint = getString(R.string.token_expired_enter_otp);
        final String otpOut = getString(R.string.token_expired_otp_error_expired);
        final Context context = getContext();

        builder.setMessage(overrideMessage == null ? R.string.token_expired_dialog : overrideMessage);

        builder.setNegativeButton(R.string.action_logout, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                LoginRegUtils.logout(getActivity());
                dialogInterface.dismiss();
            }
        });


        builder.setPositiveButton(R.string.token_expired_sms_otp, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                LoginRegUtils.requestTokenRefreshOTP()
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new Consumer<String>() {
                            @Override
                            public void accept(@NonNull String s) throws Exception {
                                if (NetworkUtils.SERVER_ERROR.equals(s) || NetworkUtils.CONNECT_ERROR.equals(s)) {
                                    Log.e(TAG, "Error requesting token refresh, recreating dialog with just logout");
                                    TokenExpiredDialogFragment.showTokenExpiredDialogs(fragmentManager,
                                            R.string.token_expired_connect_error, actionOnSuccess, successConsumer, errorConsumer);
                                } else {
                                    requestOtpEntry(otpHint);
                                }
                            }
                        });
                dialogInterface.dismiss();
            }
        });

        otpListener = new EditTextDialogFragment.EditTextDialogListener() {
            @Override
            public void confirmClicked(String textEntered) {
                LoginRegUtils.verifyOtpForNewToken(textEntered)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new Consumer<String>() {
                            @Override
                            public void accept(@NonNull String s) throws Exception {
                                if (LoginRegUtils.AUTH_REFRESHED.equals(s)) {
                                    Toast.makeText(context, R.string.token_refreshed_success,
                                            Toast.LENGTH_SHORT).show();
                                    if (successConsumer != null && errorConsumer != null) {
                                        Log.e(TAG, "consumers not null, hence triggering with them ...");
                                        actionOnSuccess.subscribe(successConsumer, errorConsumer);
                                    } else {
                                        actionOnSuccess.subscribe();
                                    }
                                } else {
                                    Log.e(TAG, "error!");
                                    if (numberOtpAttempts < 2) {
                                        numberOtpAttempts++;
                                        requestOtpEntry(otpHint);
                                    } else {
                                        Toast.makeText(context, otpOut, Toast.LENGTH_LONG).show();
                                        dismiss();
                                    }
                                }
                            }
                        });
            }
        };

        builder.setCancelable(true);

        return builder.create();
    }

    // Android, being Android, throws a fatal exception if we call getString in here, hence
    private void requestOtpEntry(String otpHint) {
        Log.e(TAG, "asking for OTP entry, number of attempts so far = " + numberOtpAttempts);
        final EditTextDialogFragment dialog = EditTextDialogFragment.newInstance(
                numberOtpAttempts == 0 ? R.string.token_expired_enter_otp_title : R.string.token_expired_otp_error,
                otpHint, otpListener);
        dialog.show(fragmentManager, "otp_entry");
    }

}
