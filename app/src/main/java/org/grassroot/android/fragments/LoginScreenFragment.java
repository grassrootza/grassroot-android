package org.grassroot.android.fragments;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;

import org.grassroot.android.R;
import org.grassroot.android.utils.UtilClass;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by paballo on 2016/04/26.
 */
public class LoginScreenFragment extends Fragment {

    @BindView(R.id.et_mobile_login)
    EditText etNumberInput;

    @BindView(R.id.bt_login)
    Button bt_login;

    private OnLoginScreenInteractionListener onLoginScreenInteractionListener;

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

        Activity activity = (Activity) context;
        try {
            onLoginScreenInteractionListener = (OnLoginScreenInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnLoginScreenInteractionListener");
        }
    }

    @OnClick(R.id.bt_login)
    public void onLoginButtonClick() {
        final String number = etNumberInput.getText().toString();
        if (TextUtils.isEmpty(number)) {
            etNumberInput.requestFocus();
            etNumberInput.setError(getResources().getString(R.string.Cellphone_number_empty));
        } else if (!UtilClass.checkIfLocalNumber(number)) {
            etNumberInput.requestFocus();
            etNumberInput.setError(getResources().getString(R.string.Cellphone_number_invalid));
        } else {
            onLoginScreenInteractionListener.login(number);
        }
    }

    public interface OnLoginScreenInteractionListener {
        void login(String mobileNumber);
    }
}