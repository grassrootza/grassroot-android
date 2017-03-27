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
import android.widget.Button;
import android.widget.TextView;

import org.grassroot.android.R;

import io.reactivex.Single;
import io.reactivex.SingleEmitter;
import io.reactivex.SingleOnSubscribe;

/**
 * Created by luke on 2016/07/13.
 */
public class MultiLineTextDialog extends DialogFragment {

    private int titleRes;
    private String bodyText;
    private int hintRes;
    private int okayBtnRes;

    private SingleEmitter<String> subscriber;

    public static Single<String> showMultiLineDialog(final FragmentManager fragmentManager, final int titleRes,
                                                     final String bodyText, final int hintRes, final int okayBtnRes) {
        return Single.create(new SingleOnSubscribe<String>() {
            @Override
            public void subscribe(SingleEmitter<String> subscriber) {
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

        builder.setPositiveButton(okayBtnRes, null);
        builder.setNegativeButton(R.string.alert_cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        final AlertDialog createdDialog = builder.create();
        createdDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                Button b = createdDialog.getButton(AlertDialog.BUTTON_POSITIVE);
                b.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (TextUtils.isEmpty(textEdit.getText())) {
                            textEdit.setError(getString(R.string.gs_dialog_message_error));
                            textEdit.requestFocus();
                        } else {
                            subscriber.onSuccess(textEdit.getText().toString().trim());
                            createdDialog.dismiss();
                        }
                    }
                });
            }
        });

        return createdDialog;
    }

}
