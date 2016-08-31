package org.grassroot.android.fragments.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.text.TextUtils;

import org.grassroot.android.R;
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
    private int startingIndex;
    private int selectedIndex;

    private PickNumberListener listener;

    public static PickNumberDialogFragment newInstance(@NonNull final Contact contact, final int contactPosition,
                                                       @NonNull final PickNumberListener listener) {
        PickNumberDialogFragment fragment = new PickNumberDialogFragment();
        Bundle args = new Bundle();
        args.putParcelable("contact", contact);
        args.putInt("contactPosition", contactPosition);
        fragment.setArguments(args);
        fragment.listener = listener;
        return fragment;
    }

    @Override
    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        contact = getArguments().getParcelable("contact");
        contactPosition = getArguments().getInt("contactPosition");

        if (contact == null)
            throw new UnsupportedOperationException("Error! Dialog created without valid contact");

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        final CharSequence[] numbers = contact.numbers.toArray(new CharSequence[contact.numbers.size()]);

        // in theory, the second check should not be necessary, but don't fully trust the number handling on many of our user's phones
        startingIndex = (TextUtils.isEmpty(contact.selectedMsisdn)) ? 0 :
            !contact.msisdns.contains(contact.selectedMsisdn) ? 0 :
                contact.msisdns.indexOf(contact.selectedMsisdn);

        builder.setTitle(R.string.contact_number_pick)
                .setSingleChoiceItems(numbers, startingIndex, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        selectedIndex = i;
                    }
                })
                .setPositiveButton(R.string.okay_button, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        listener.onNumberPicked(contactPosition, selectedIndex);
                    }
                });

        return builder.create();
    }
}