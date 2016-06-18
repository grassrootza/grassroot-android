package org.grassroot.android.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.design.widget.TextInputEditText;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;

import org.grassroot.android.R;
import org.grassroot.android.utils.Utilities;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by paballo on 2016/04/26.
 */
public class LoginScreenFragment extends Fragment {

    @BindView(R.id.et_mobile_login)
    TextInputEditText etNumberInput;

    public interface LoginFragmentListener {
        void requestLogin(String mobileNumber);
    }

    private LoginFragmentListener listener;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.container_login, container, false);
        ButterKnife.bind(this, view);
        view.startAnimation(AnimationUtils.loadAnimation(getActivity(), R.anim.fade_in));
        view.setLayoutParams(new ViewGroup.LayoutParams(-1, -1));
        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            listener = (LoginFragmentListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement LoginFragmentListener");
        }
    }

    @OnClick(R.id.bt_login)
    public void onLoginButtonClick() {
        final String number = etNumberInput.getText().toString();
        if (TextUtils.isEmpty(number)) {
            etNumberInput.requestFocus();
            etNumberInput.setError(getResources().getString(R.string.Cellphone_number_empty));
        } else if (!Utilities.checkIfLocalNumber(number)) {
            etNumberInput.requestFocus();
            etNumberInput.setError(getResources().getString(R.string.Cellphone_number_invalid));
        } else {
            listener.requestLogin(number);
        }
    }
}