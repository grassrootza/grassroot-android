package org.grassroot.android.fragments;

import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputEditText;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.RelativeLayout;

import org.grassroot.android.R;
import org.grassroot.android.utils.Utilities;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnEditorAction;

/**
 * Created by luke on 2016/08/08.
 */
public class RegisterNameFragment extends Fragment {

	RegisterNameListener listener;

	@BindView(R.id.fragment_display_name_root) RelativeLayout rootView;
	@BindView(R.id.input_display_name) TextInputEditText nameInput;

	public interface RegisterNameListener {
		void nameEntered(String nameEntered);
	}

	public static RegisterNameFragment newInstance(RegisterNameListener listener) {
		RegisterNameFragment fragment = new RegisterNameFragment();
		fragment.listener = listener;
		return fragment;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_register_display_name, container, false);
		ButterKnife.bind(this, view);
		nameInput.requestFocus();
		return view;
	}

	@OnClick(R.id.bt_next)
	public void onClickNext() {
		validateAndNext();
	}

	@OnEditorAction(R.id.input_display_name)
	public boolean onTextNext(int actionId, KeyEvent keyEvent) {
		if (actionId == EditorInfo.IME_ACTION_NEXT && keyEvent.getAction() == KeyEvent.ACTION_DOWN) {
			validateAndNext();
		}
		return true;
	}

	private void validateAndNext() {
		final String name = nameInput.getText().toString().trim();
		if (TextUtils.isEmpty(name)) {
			nameInput.setError(getString(R.string.input_error_no_display_name));
			Snackbar.make(rootView, R.string.input_error_no_display_name, Snackbar.LENGTH_SHORT).show();
		} else if (Utilities.checkForSpecialChars(name)) {
			nameInput.setError(getString(R.string.input_error_name_regex));
			Snackbar.make(rootView, R.string.input_error_name_regex, Snackbar.LENGTH_SHORT).show();
		} else {
			listener.nameEntered(name);
		}
	}

}
