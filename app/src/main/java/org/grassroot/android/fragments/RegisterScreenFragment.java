package org.grassroot.android.fragments;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.EditText;

import org.grassroot.android.R;
import org.grassroot.android.utils.ErrorUtils;
import org.grassroot.android.utils.UtilClass;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by paballo on 2016/04/26.
 */
public class RegisterScreenFragment extends Fragment {

    @BindView(R.id.et_userName)
    EditText etUserName;

    @BindView(R.id.et_mobile_register)
    EditText etMobilePhone;

    private ViewGroup container;

    private OnRegisterScreenInteractionListener onRegisterScreenInteractionListener;

    public static RegisterScreenFragment newInstance(){
        RegisterScreenFragment registerScreenFragment = new RegisterScreenFragment();
        return registerScreenFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.container_register, container, false);
        ButterKnife.bind(this, view);
        view.startAnimation(AnimationUtils.loadAnimation(getActivity(), R.anim.fade_in));
        view.setLayoutParams(new ViewGroup.LayoutParams(-1, -1));
        this.container = container;
        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        Activity activity = (Activity) context;
        try {
            onRegisterScreenInteractionListener = (OnRegisterScreenInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new UnsupportedOperationException(activity.toString()
                    + " must implement OnRegisterScreenInteractionListener");
        }
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

        } else if (!UtilClass.checkIfLocalNumber(phone)) {
            etMobilePhone.requestFocus();
            etMobilePhone.setError(getString(R.string.Cellphone_number_invalid));
        } else {
            onRegisterScreenInteractionListener.register(etUserName,etMobilePhone);
        }
    }

    public interface OnRegisterScreenInteractionListener {
        void register(EditText user_name, EditText mobile_number);
    }
}