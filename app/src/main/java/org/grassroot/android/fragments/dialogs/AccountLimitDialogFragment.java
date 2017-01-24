package org.grassroot.android.fragments.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;

import org.grassroot.android.R;

import rx.Observable;
import rx.Subscriber;

/**
 * Created by luke on 2017/01/16.
 */

public class AccountLimitDialogFragment extends DialogFragment {

    public static final String GO_TO_GR = "go_to_gr";
    public static final String ABORT = "abort";

    private int bodyTextRes;
    private Subscriber<? super String> subscriber;

    public static Observable<String> showAccountLimitDialog(final FragmentManager fragmentManager,
                                                            final int bodyTextRes) {
        return Observable.create(new Observable.OnSubscribe<String>() {
            @Override
            public void call(Subscriber<? super String> subscriber) {
                AccountLimitDialogFragment fragment = new AccountLimitDialogFragment();
                fragment.bodyTextRes = bodyTextRes;
                fragment.subscriber = subscriber;
                fragment.show(fragmentManager, "dialog");
            }
        });
    }

    @Override
    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());

        builder.setMessage(bodyTextRes)
                .setPositiveButton(R.string.account_limit_gotogr, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        subscriber.onNext(GO_TO_GR);
                    }
                })
                .setNegativeButton(R.string.account_limit_abort, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        subscriber.onNext(ABORT);
                    }
                })
                .setCancelable(true);

        return builder.create();
    }

}
