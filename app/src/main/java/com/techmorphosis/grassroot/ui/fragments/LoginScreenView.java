package com.techmorphosis.grassroot.ui.fragments;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.EditText;

import com.techmorphosis.grassroot.R;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by paballo on 2016/04/26.
 */
public class LoginScreenView extends Fragment {

    @BindView(R.id.et_mobile_login)
    EditText et_mobile_login;


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


    @Override
    public void onDetach() {
        super.onDetach();
    }


    public interface OnLoginScreenInteractionListener {

    }
}




