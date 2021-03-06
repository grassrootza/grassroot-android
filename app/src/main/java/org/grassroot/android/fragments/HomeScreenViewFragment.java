package org.grassroot.android.fragments;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.Button;

import org.grassroot.android.R;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;

/**
 * Created by paballo on 2016/04/26.
 */
public class HomeScreenViewFragment extends Fragment {

    Unbinder unbinder;
    @BindView(R.id.amm_bt_add) Button bt_register;
    @BindView(R.id.bt_login) Button bt_login;

    OnHomeScreenInteractionListener onHomeScreenInteractionListener;

    public interface OnHomeScreenInteractionListener {
        void onRegisterButtonClick();
        void onLoginButtonRegisterClick();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.container_home, container, false);
        unbinder = ButterKnife.bind(this, view);
        view.startAnimation(AnimationUtils.loadAnimation(getActivity(), R.anim.fade_in));
        view.setLayoutParams(new ViewGroup.LayoutParams(-1, -1));
        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        Activity activity = (Activity)context;
        try {
            onHomeScreenInteractionListener = (OnHomeScreenInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnHomeListInteractionListener");
        }
    }

    @OnClick(R.id.amm_bt_add)
    public void onRegisterButtonClick(View view){
        onHomeScreenInteractionListener.onRegisterButtonClick();
    }

    @OnClick(R.id.bt_login)
    public void onLoginButtonClick(View view){
        onHomeScreenInteractionListener.onLoginButtonRegisterClick();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }

    @Override
    public void onDetach() {
        super.onDetach(); // note : watch for memory leaks here and quite likely set listener to null ..
    }


}