package org.grassroot.android.fragments.dialogs;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;

/**
 * Created by luke on 2016/06/09.
 */
public class RadioSelectDialogFragment extends DialogFragment {

    public interface RadioChoiceListener {
        void radioButtonPicked(int positionPicked, String tag);
    }

    private RadioChoiceListener listener;

    public static RadioSelectDialogFragment newInstance(int message, int optionArrayReference,
                                                        int defaultPosition, RadioChoiceListener listener) {
        RadioSelectDialogFragment frag = new RadioSelectDialogFragment();
        Bundle args = new Bundle();
        args.putInt("message", message);
        args.putInt("options", optionArrayReference);
        args.putInt("default_position", defaultPosition);
        frag.setArguments(args);
        frag.setListener(listener);
        return frag;
    }

    public void setListener(RadioChoiceListener listener) {
        this.listener = listener;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        int message = getArguments().getInt("message");
        int options = getArguments().getInt("options");
        int default_pos = getArguments().getInt("default_position");

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(message);
        builder.setSingleChoiceItems(options, default_pos, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                listener.radioButtonPicked(i, getTag());
            }
        });

        return builder.create();
    }

}
