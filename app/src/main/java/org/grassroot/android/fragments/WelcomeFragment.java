package org.grassroot.android.fragments;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import org.grassroot.android.R;
import org.grassroot.android.activities.CreateGroupActivity;
import org.grassroot.android.activities.GroupSearchActivity;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;


public class WelcomeFragment extends android.support.v4.app.Fragment {

    Unbinder unbinder;

    @BindView(R.id.toolbar) Toolbar toolbar;
    @BindView(R.id.bt_joingroup) Button bt_joingroup;
    @BindView(R.id.bt_startgroup) Button bt_startgroup;

    private WelcomeFragmentListener listener;

    public interface WelcomeFragmentListener {
        void menuClick();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        Activity activity = (Activity) context;
        try {
            listener = (WelcomeFragmentListener) activity;
            Log.e("onAttach", "Attached");
        } catch (ClassCastException e) {
            throw new ClassCastException("Activity must implement callbacks.");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.welcome, container, false);
        unbinder = ButterKnife.bind(this, view);
        setUpToolbar();
        return view;
    }

    private void setUpToolbar() {
        toolbar.setNavigationIcon(ContextCompat.getDrawable(getActivity(), R.drawable.btn_navigation));
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.menuClick();
            }
        });
    }

    @OnClick(R.id.bt_startgroup)
    public void startgroup() {
        startActivity(new Intent(getActivity(), CreateGroupActivity.class));
    }

    @OnClick(R.id.bt_joingroup)
    public void joingroup() {
        startActivity(new Intent(getActivity(), GroupSearchActivity.class));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
    }
}