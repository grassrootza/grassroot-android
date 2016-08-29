package org.grassroot.android.fragments;


import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.grassroot.android.R;
import org.grassroot.android.activities.CreateGroupActivity;
import org.grassroot.android.activities.GroupSearchActivity;

import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;


public class WelcomeFragment extends android.support.v4.app.Fragment {

    Unbinder unbinder;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_welcome_no_groups, container, false);
        unbinder = ButterKnife.bind(this, view);
        return view;
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
    }
}