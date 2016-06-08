package org.grassroot.android.fragments;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.EditText;

import org.grassroot.android.R;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by paballo on 2016/04/26.
 */
public class RegisterScreenFragment extends Fragment {

    @BindView(R.id.et_userName)
    EditText et_userName;

    @BindView(R.id.et_mobile_register)
    EditText et_mobile_register;

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
        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        Activity activity = (Activity) context;
        try {
            onRegisterScreenInteractionListener = (OnRegisterScreenInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnRegisterScreenInteractionListener");
        }
    }


    @Override
    public void onDetach() {
        super.onDetach();
    }

    @OnClick(R.id.bt_register)
    public void onRegisteButtonClick(){
        onRegisterScreenInteractionListener.register(et_userName,et_mobile_register);
    }


    public interface OnRegisterScreenInteractionListener {
        void register(EditText user_name, EditText mobile_number);

    }
}