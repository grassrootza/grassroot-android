package org.grassroot.android.fragments;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputEditText;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;

import org.grassroot.android.R;
import org.grassroot.android.utils.ErrorUtils;
import org.grassroot.android.utils.Utilities;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by paballo on 2016/04/26.
 */
public class RegisterScreenFragment extends Fragment {

    @BindView(R.id.et_userName)
    TextInputEditText etUserName;

    @BindView(R.id.et_mobile_register)
    TextInputEditText etMobilePhone;

    private String presetNumber;

    private ViewGroup container;

    private RegisterListener listener;

    public interface RegisterListener {
        void requestRegistration(String userName, String mobileNumber);
    }

    public static RegisterScreenFragment newInstance() {
        RegisterScreenFragment registerScreenFragment = new RegisterScreenFragment();
        return registerScreenFragment;
    }

    public static RegisterScreenFragment newInstance(String presetNumber) {
        RegisterScreenFragment fragment = new RegisterScreenFragment();
        fragment.presetNumber = presetNumber;
        return fragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        Activity activity = (Activity) context;
        try {
            listener = (RegisterListener) activity;
        } catch (ClassCastException e) {
            throw new UnsupportedOperationException(activity.toString()
                    + " must implement RegisterListener");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.container_register, container, false);
        ButterKnife.bind(this, view);
        view.startAnimation(AnimationUtils.loadAnimation(getActivity(), R.anim.fade_in));
        view.setLayoutParams(new ViewGroup.LayoutParams(-1, -1));
        this.container = container;

        if (!TextUtils.isEmpty(presetNumber)) {
            etMobilePhone.setText(presetNumber);
        }

        return view;
    }

    @OnClick(R.id.bt_register)
    public void onRegisterButtonClick() {
        final String phone = etMobilePhone.getText().toString();
        final String name = etUserName.getText().toString().trim();
        final boolean nameEmpty = name.isEmpty();
        final boolean phoneEmpty = phone.isEmpty();

        if (nameEmpty || phoneEmpty) {
            if (nameEmpty) etUserName.setError(getString(R.string.Either_field_empty));
            if (phoneEmpty) etMobilePhone.setError(getString(R.string.Cellphone_number_empty));

            if (nameEmpty && !phoneEmpty)
                etUserName.requestFocus();
            else if (phoneEmpty)
                etMobilePhone.requestFocus();
            else
                ErrorUtils.showSnackBar(container, R.string.Either_field_empty, Snackbar.LENGTH_SHORT);

        } else if (!Utilities.checkIfLocalNumber(phone)) {
            etMobilePhone.requestFocus();
            etMobilePhone.setError(getString(R.string.Cellphone_number_invalid));
        } else {
            listener.requestRegistration(etUserName.getText().toString(), etMobilePhone.getText().toString());
        }
    }
}