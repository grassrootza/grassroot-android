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
import butterknife.Unbinder;

/**
 * Created by luke on 2016/08/08.
 */
public class RegisterPhoneFragment extends Fragment {

	RegisterPhoneListener listener;
	String preEnteredNumber;

	Unbinder unbinder;
	@BindView(R.id.fragment_display_phone_root) RelativeLayout rootView;
	@BindView(R.id.input_mobile_phone) TextInputEditText phoneInput;

	public interface RegisterPhoneListener {
		void phoneEntered(String mobileNumber);
	}

	public static RegisterPhoneFragment newInstance(RegisterPhoneListener listener, String preEnteredNumber) {
		RegisterPhoneFragment fragment = new RegisterPhoneFragment();
		fragment.listener = listener;
		fragment.preEnteredNumber = preEnteredNumber;
		return fragment;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_register_phone_number, container, false);
		ButterKnife.bind(this, view);
		if (!TextUtils.isEmpty(preEnteredNumber)) {
			phoneInput.setText(preEnteredNumber);
		}
		phoneInput.requestFocus();
		return view;
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		unbinder.unbind();
	}

	@OnClick(R.id.bt_register)
	public void onClickNext() {
		validateAndNext();
	}

	@OnEditorAction(R.id.input_mobile_phone)
	public boolean onTextNext(int actionId, KeyEvent keyEvent) {
		if (actionId == EditorInfo.IME_ACTION_NEXT && keyEvent.getAction() == KeyEvent.ACTION_DOWN) {
			validateAndNext();
		}
		return true;
	}

	private void validateAndNext() {
		final String phone = phoneInput.getText().toString().trim();
		if (TextUtils.isEmpty(phone)) {
			phoneInput.setError(getString(R.string.input_error_phone_empty));
			Snackbar.make(rootView, getString(R.string.input_error_phone_empty), Snackbar.LENGTH_SHORT).show();
		} else if (!Utilities.checkIfLocalNumber(phone)) {
			phoneInput.setError(getString(R.string.input_error_phone_invalid));
			Snackbar.make(rootView, R.string.input_error_phone_invalid, Snackbar.LENGTH_SHORT).show();
		} else {
			listener.phoneEntered(phone);
		}
	}

}
