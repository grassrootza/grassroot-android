package org.grassroot.android.fragments;

import android.annotation.SuppressLint;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;

import org.grassroot.android.R;
import org.grassroot.android.interfaces.SimpleTextWatcher;
import org.grassroot.android.utils.DialogUtils;


public class EditItemDialog extends DialogFragment {

	public static final String TAG = EditItemDialog.class.getSimpleName();
	public static final String ARG_ITEM = "username";
	public static final String ARG_ITEM_POSITION = "position";
	private String username;


	private int mPosition;

	public EditItemDialog() {
	}

	public static EditItemDialog newInstance(String username) {
		return newInstance(username, null);
	}

	public static EditItemDialog newInstance(String username, Fragment fragment) {
		EditItemDialog dialog = new EditItemDialog();
		Bundle args = new Bundle();
		args.putString(ARG_ITEM, username);
		dialog.setArguments(args);
		return dialog;
	}


	public interface OnEditItemListener {
		void onTitleModified(String newTitle);
	}


	@Override
	public void onStart() {
		super.onStart();
		getDialog().getWindow().setWindowAnimations(R.style.animation_slide_from_right);
	}

	@SuppressLint({"InflateParams", "HandlerLeak"})
	@Override
	public AlertDialog onCreateDialog(Bundle savedInstanceState) {
		//Pick up bundle parameters
		Bundle bundle;
		if (savedInstanceState == null) {
			bundle = getArguments();
		} else {
			bundle = savedInstanceState;
		}

		username = bundle.getString(ARG_ITEM);

		//Inflate custom view
		View dialogView = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_edit_item, null);

		final EditText editText = (EditText) dialogView.findViewById(R.id.text_edit_title);

		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());//, R.style.AppTheme_AlertDialog);
		builder.setTitle(R.string.pp_name_dialog)
				.setView(dialogView)
				.setNegativeButton(R.string.pp_Cancel, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						DialogUtils.hideSoftInputFrom(getActivity(), editText);
						dialog.dismiss();
					}
				})
				.setPositiveButton(R.string.pp_OK, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						getListener().onTitleModified(
								editText.getText().toString().trim());
						DialogUtils.hideSoftInputFrom(getActivity(), editText);
						dialog.dismiss();
					}
				});

		final AlertDialog editDialog = builder.create();

		editDialog.setOnShowListener(new DialogInterface.OnShowListener() {
			@Override
			public void onShow(DialogInterface dialog) {
				updateOkButtonState(editDialog, null);
			}
		});

		if (username != null) {
			editText.setText(username);
			editText.selectAll();
		}
		editText.requestFocus();

		editText.addTextChangedListener(new SimpleTextWatcher() {
			private final static long DELAY = 200L;
			private final static int TRIGGER = 1;

			private Handler mHandler = new Handler() {
				@Override
				public void handleMessage(Message msg) {
					if (msg.what == TRIGGER) {
						updateOkButtonState(editDialog, editText);
					}
				}
			};

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
				updateOkButtonState(editDialog, null);
			}

			@Override
			public void afterTextChanged(Editable s) {
				mHandler.removeMessages(TRIGGER);
				mHandler.sendEmptyMessageDelayed(TRIGGER, DELAY);
			}
		});

		editDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);

		return editDialog;
	}

	private OnEditItemListener getListener() {
		OnEditItemListener listener = (OnEditItemListener) getTargetFragment();
		if (listener==null)
		{
		listener= (OnEditItemListener) getActivity();
		}
		return listener;
	}

	private void updateOkButtonState(AlertDialog dialog, EditText editText) {
		Button buttonOK = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
		if (editText == null || (editText.getText().toString().trim()).length() == 0) {
			buttonOK.setEnabled(false);
			return;
		}
		if (username != null && !username.equalsIgnoreCase(editText.getText().toString().trim())) {
			buttonOK.setEnabled(true);
		} else {
			editText.setError(getActivity().getString(R.string.pp_err_no_edit));
			buttonOK.setEnabled(false);
		}

		return;
	}

}