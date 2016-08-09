package org.grassroot.android.fragments.dialogs;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import org.grassroot.android.R;
import org.grassroot.android.models.Member;
import org.grassroot.android.services.GroupService;

import java.util.Collections;

import rx.Observable;

/**
 * Created by luke on 2016/08/09.
 */
public class FixMemberPhoneDialog extends DialogFragment {

	private static final String TAG = FixMemberPhoneDialog.class.getSimpleName();

	private String groupUid;
	private Member memberToFix;
	private EditText inputBox;

	public static FixMemberPhoneDialog newInstance(final String groupUid, Member memberToFix) {
		FixMemberPhoneDialog dialog = new FixMemberPhoneDialog();
		dialog.groupUid = groupUid;
		dialog.memberToFix = memberToFix;
		return dialog;
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		Log.e(TAG, "calling onCreateDialog ... ");
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		LayoutInflater inflater = getActivity().getLayoutInflater();

		builder
			.setView(inflater.inflate(R.layout.dialog_fix_member_phone, null))
			.setPositiveButton(R.string.pp_OK, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					fixSingleMemberDo();
				}
			})
			.setNegativeButton(R.string.pp_Cancel, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					FixMemberPhoneDialog.this.getDialog().cancel();
				}
			});

		return builder.create();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		Log.e(TAG, "calling onCreateView ... ");
		View view = inflater.inflate(R.layout.dialog_fix_member_phone, container, false);
		TextView message = (TextView) view.findViewById(R.id.dialog_label);
		Log.e(TAG, "do I have the right message ? text : " + message.getText());
		final String msgText = String.format(getString(R.string.input_error_member_phone_single),
			memberToFix.getDisplayName());
		Log.e(TAG, "setting message to ... " + msgText);
		message.setText(msgText);

		inputBox = (EditText) view.findViewById(R.id.new_number);
		inputBox.setHint(memberToFix.getPhoneNumber());

		return view;
	}

	private void fixSingleMemberDo() {
		memberToFix.setPhoneNumber(inputBox.getText().toString());
		GroupService.getInstance().addMembersToGroup(groupUid, Collections.singletonList(memberToFix), false)
			.subscribe(new Observable.OnSubscribe() {
				@Override
				public void call(Object o) {
					Log.e(TAG, "okay, it is done ... tell a listener ...");
				}
			});
	}

}
