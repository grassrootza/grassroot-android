package org.grassroot.android.fragments.dialogs;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.design.widget.TextInputEditText;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;

import org.grassroot.android.R;
import org.grassroot.android.models.PublicGroupModel;

/**
 * Created by luke on 2016/07/13.
 */
public class SendJoinRequestFragment extends DialogFragment {

    private SendJoinRequestListener listener;
    private PublicGroupModel groupModel;

    public interface SendJoinRequestListener {
        void requestConfirmed(PublicGroupModel groupModel);
    }

    public static SendJoinRequestFragment newInstance(PublicGroupModel groupModel, SendJoinRequestListener listener) {
        SendJoinRequestFragment fragment = new SendJoinRequestFragment();
        fragment.groupModel = groupModel;
        fragment.listener = listener;
        return fragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        LayoutInflater inflater = getActivity().getLayoutInflater();

        View dialogView = inflater.inflate(R.layout.dialog_send_join_request, null);
        final TextInputEditText textEdit = (TextInputEditText) dialogView.findViewById(R.id.text_edit_message);

        builder.setView(dialogView)
                .setTitle(R.string.gs_dialog_title)
                .setPositiveButton(R.string.gs_dialog_send, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (TextUtils.isEmpty(textEdit.getText())) {
                            textEdit.setError(getString(R.string.gs_dialog_message_error));
                        } else {
                            groupModel.setDescription(textEdit.getText().toString().trim());
                            listener.requestConfirmed(groupModel);
                        }
                    }
                })
                .setNegativeButton(R.string.gs_dialog_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        SendJoinRequestFragment.this.getDialog().cancel();
                    }
                });

        return builder.create();
    }

}
