package org.grassroot.android.fragments.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

import org.grassroot.android.models.Contact;

/**
 * Created by luke on 2016/06/07.
 */
public class PickNumberDialogFragment extends DialogFragment {

    private static final String TAG = PickNumberDialogFragment.class.getSimpleName();

    public interface PickNumberListener {
        void onNumberPicked(int contactPosition, final int numberIndex);
    }

    private int contactPosition;
    private Contact contact;
    private int defaultNumberIndex;
    private int selectedIndex;

    private PickNumberListener listener;

    public static PickNumberDialogFragment newInstance(final Contact contact, final int contactPosition, final PickNumberListener listener) {
        PickNumberDialogFragment fragment = new PickNumberDialogFragment();
        // todo : switch these to args
        fragment.contact = contact;
        fragment.contactPosition = contactPosition;
        fragment.listener = listener;
        return fragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (contact == null)
            throw new UnsupportedOperationException("Error! Dialog created without valid contact");

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        final CharSequence[] numbers = contact.numbers.toArray(new CharSequence[contact.numbers.size()]);
        selectedIndex = defaultNumberIndex;

        builder.setTitle("Pick a number")
                .setSingleChoiceItems(numbers, defaultNumberIndex, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        selectedIndex = i;
                    }
                })
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        listener.onNumberPicked(contactPosition, selectedIndex);
                    }
                });

        return builder.create();
    }
}