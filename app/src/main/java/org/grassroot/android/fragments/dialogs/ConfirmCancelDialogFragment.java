package org.grassroot.android.fragments.dialogs;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;

import org.grassroot.android.R;

/**
 * Created by luke on 2016/05/31.
 */
public class ConfirmCancelDialogFragment extends DialogFragment {

    public interface ConfirmDialogListener {
        void doConfirmClicked();
    }

    private ConfirmDialogListener mListener;

    public void setListener(ConfirmDialogListener listener) {
        this.mListener = listener;
    }

    public static ConfirmCancelDialogFragment newInstance(int message, ConfirmDialogListener listener) {
        ConfirmCancelDialogFragment frag = new ConfirmCancelDialogFragment();
        Bundle args = new Bundle();
        args.putInt("message", message);
        args.putBoolean("custom", false);
        frag.setArguments(args);
        frag.setListener(listener);
        return frag;
    }

    public static ConfirmCancelDialogFragment newInstance(String message, ConfirmDialogListener listener) {
        ConfirmCancelDialogFragment frag = new ConfirmCancelDialogFragment();
        Bundle args = new Bundle();
        args.putString("message", message);
        args.putBoolean("custom", true);
        frag.setArguments(args);
        frag.setListener(listener);
        return frag;
    }

    @Override
    public @NonNull Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        Bundle args = getArguments();

        if (args.getBoolean("custom")) {
            builder.setTitle(args.getString("message"));
        } else {
            builder.setTitle(args.getInt("message"));
        }

        builder.setPositiveButton(R.string.alert_confirm, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        mListener.doConfirmClicked();
                    }
                })
                .setNegativeButton(R.string.alert_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        ConfirmCancelDialogFragment.this.getDialog().cancel();
                    }
                });

        return builder.create();
    }

}
