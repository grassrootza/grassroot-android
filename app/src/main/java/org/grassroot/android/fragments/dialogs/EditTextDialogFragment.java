package org.grassroot.android.fragments.dialogs;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.design.widget.TextInputEditText;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;

import org.grassroot.android.R;

/**
 * Created by luke on 2016/06/10.
 */
public class EditTextDialogFragment extends DialogFragment {

    private EditTextDialogListener listener;

    public interface EditTextDialogListener {
        void confirmClicked(String textEntered);
    }

    public static EditTextDialogFragment newInstance(int titleString, String textHint, EditTextDialogListener listener) {
        EditTextDialogFragment frag = new EditTextDialogFragment();
        Bundle args = new Bundle();
        args.putInt("title", titleString);
        args.putString("hint", textHint);
        frag.setArguments(args);
        frag.setListener(listener);
        return frag;
    }

    public void setListener(EditTextDialogListener listener) { this.listener = listener; }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        LayoutInflater inflater = getActivity().getLayoutInflater();

        final int title = getArguments().getInt("title");
        final String hint = getArguments().getString("hint");

        View dialogView = inflater.inflate(R.layout.dialog_edit_item, null);
        final TextInputEditText textEdit = (TextInputEditText) dialogView.findViewById(R.id.text_edit_title);
        textEdit.setHint(hint);

        builder.setView(dialogView)
                .setTitle(title)
                .setPositiveButton(R.string.pp_OK, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        listener.confirmClicked(textEdit.getText().toString().trim());
                    }
                })
                .setNegativeButton(R.string.pp_Cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        EditTextDialogFragment.this.getDialog().cancel();
                    }
                });

        return builder.create();
    }

}
