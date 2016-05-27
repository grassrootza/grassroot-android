package com.techmorphosis.grassroot.ui.fragments;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.techmorphosis.grassroot.R;
import com.techmorphosis.grassroot.ui.activities.CreateGroupActivity;
import com.techmorphosis.grassroot.ui.activities.GroupJoinActivity;
import com.viewpagerindicator.PageIndicator;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;


public class WelcomeFragment extends android.support.v4.app.Fragment {

    @BindView(R.id.toolbar)
    Toolbar toolbar;
    //@BindView(R.id.txt_welcometitle)
    //TextView toolbarText;
    @BindView(R.id.bt_joingroup)
    Button bt_joingroup;
    @BindView(R.id.bt_startgroup)
    Button bt_startgroup;

    private FragmentCallbacks mCallbacks;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        Activity activity = (Activity) context;
        try {
            mCallbacks = (FragmentCallbacks) activity;
            Log.e("onAttach", "Attached");
        } catch (ClassCastException e) {
            throw new ClassCastException("Activity must implement Fragment One.");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.welcome, container, false);
        ButterKnife.bind(this, view);
        setUpToolbar();
        return view;
    }

    private void setUpToolbar() {
        // toolbarText.setText(getResources().getString(R.string.txt_welcome_short));
        toolbar.setNavigationIcon(ContextCompat.getDrawable(getActivity(), R.drawable.btn_navigation));
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCallbacks.menuClick();
            }
        });
    }

    @OnClick(R.id.bt_startgroup)
    public void startgroup() {
        startActivity(new Intent(getActivity(), CreateGroupActivity.class));
    }

    @OnClick(R.id.bt_joingroup)
    public void joingroup() {
        startActivity(new Intent(getActivity(), GroupJoinActivity.class));
    }

    public interface FragmentCallbacks {
        void menuClick();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallbacks = null;
        Log.e("onDetach", "Detached");
    }
}