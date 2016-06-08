package org.grassroot.android.ui.fragments;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
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
        frag.setArguments(args);
        frag.setListener(listener);
        return frag;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        int message = getArguments().getInt("message");

        return new AlertDialog.Builder(getActivity())
                .setMessage(message)
                .setPositiveButton(R.string.alert_confirm, new DialogInterface.OnClickListener() {
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
                })
                .create();
    }

}
