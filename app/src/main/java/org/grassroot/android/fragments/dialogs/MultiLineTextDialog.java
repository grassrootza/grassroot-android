package org.grassroot.android.fragments.dialogs;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputEditText;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import org.grassroot.android.R;

import rx.Observable;
import rx.Subscriber;

/**
 * Created by luke on 2016/07/13.
 */
public class MultiLineTextDialog extends DialogFragment {

    private int titleRes;
    private String bodyText;
    private int hintRes;
    private int okayBtnRes;

    private Subscriber<? super String> subscriber;

    public static Observable<String> showMultiLineDialog(final FragmentManager fragmentManager, final int titleRes,
                                                         final String bodyText, final int hintRes, final int okayBtnRes) {
        return Observable.create(new Observable.OnSubscribe<String>() {
            @Override
            public void call(Subscriber<? super String> subscriber) {
                MultiLineTextDialog fragment = new MultiLineTextDialog();
                fragment.titleRes = titleRes;
                fragment.bodyText = bodyText;
                fragment.hintRes = hintRes;
                fragment.okayBtnRes = okayBtnRes;

                fragment.subscriber = subscriber;
                fragment.show(fragmentManager, "dialog");
            }
        });
    }

    @Override
    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        LayoutInflater inflater = getActivity().getLayoutInflater();

        View dialogView = inflater.inflate(R.layout.dialog_multiline_text, null);
        TextView descriptionView = (TextView) dialogView.findViewById(R.id.dialog_body_text);
        descriptionView.setText(bodyText);

        final TextInputEditText textEdit = (TextInputEditText) dialogView.findViewById(R.id.text_edit_message);
        textEdit.setHint(hintRes);

        builder.setView(dialogView);

        if (titleRes != -1) {
            builder.setTitle(titleRes);
        }

        builder.setPositiveButton(okayBtnRes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (TextUtils.isEmpty(textEdit.getText())) {
                    textEdit.setError(getString(R.string.gs_dialog_message_error));
                } else {
                    subscriber.onNext(textEdit.getText().toString().trim());
                    subscriber.onCompleted();
                }
            }
        });

        builder.setNegativeButton(R.string.alert_cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        return builder.create();
    }

}
