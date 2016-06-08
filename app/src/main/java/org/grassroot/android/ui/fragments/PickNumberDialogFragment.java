package org.grassroot.android.ui.fragments;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.TextUtils;

import org.grassroot.android.R;
import org.grassroot.android.models.Contact;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by luke on 2016/06/07.
 */
public class PickNumberDialogFragment extends DialogFragment {

    public interface PickNumberListener {
        void onNumberPicked(int contactPosition, CharSequence number);
    }

    private int contactPosition;
    private Contact contact;
    private int defaultNumberIndex;
    private PickNumberListener listener;

    public void setUp(final Contact contact, final int contactPosition, final PickNumberListener listener) {
        this.contact = contact;
        this.contactPosition = contactPosition;
        this.listener = listener;
        this.defaultNumberIndex = TextUtils.isEmpty(contact.selectedNumber) ? 0 : contact.numbers.indexOf(contact.selectedNumber);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (contact == null)
            throw new UnsupportedOperationException("Error! Dialog created without valid contact");

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        final CharSequence[] numbers = contact.numbers.toArray(new CharSequence[contact.numbers.size()]);
        final List<CharSequence> selectedNumber = Collections.singletonList(numbers[defaultNumberIndex]); // if just a single string, run into inner class issues (must be better way?)

        builder.setTitle("Pick a number")
                .setSingleChoiceItems(numbers, defaultNumberIndex, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        selectedNumber.add(numbers[i]);
                    }
                })
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        listener.onNumberPicked(contactPosition, selectedNumber.get(0));
                    }
                });

        return builder.create();
    }

}
