package com.techmorphosis.grassroot.ui.fragments;


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.techmorphosis.grassroot.R;
import com.techmorphosis.grassroot.adapters.MyPagerAdapter;
import com.techmorphosis.grassroot.ui.activities.Join_Request;
import com.viewpagerindicator.CirclePageIndicator;
import com.viewpagerindicator.PageIndicator;


public class WelcomeFragment extends android.support.v4.app.Fragment {




    private ViewPager pager;
    private MyPagerAdapter adapter;
    private FragmentActivity ctx;
    private View view;
    private Toolbar toolbar;
    private TextView toolbarText;
    PageIndicator mIndicator;
    private FragmentCallbacks mCallbacks;
    private Button bt_joingroup;
    private Button bt_startgroup;


    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mCallbacks = (FragmentCallbacks) activity;
            Log.e("onAttach", "Attached");
        } catch (ClassCastException e) {
            throw new ClassCastException("Activity must implement Fragment One.");
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState)    {
        super.onActivityCreated(savedInstanceState);

        adapter = new MyPagerAdapter(getChildFragmentManager());
        pager.setAdapter(adapter);
        mIndicator.setViewPager(pager);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
         view = inflater.inflate(R.layout.welcome, container, false);
        findView();
        setUpToolbar();
        return view;
    }

    private void setUpToolbar()
    {
        toolbarText.setText("Welcome");
        toolbar.setNavigationIcon(getResources().getDrawable(R.drawable.btn_navigation));
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCallbacks.menuClick();
            }
        });
    }

    private void findView()
    {
        toolbar = (Toolbar) view.findViewById(R.id.toolbar);
        toolbarText = (TextView) toolbar.findViewById(R.id.txt_welcometitle);
        pager = (ViewPager) view.findViewById(R.id.pager);
        mIndicator = (CirclePageIndicator)view.findViewById(R.id.indicator);
        bt_joingroup=(Button)view.findViewById(R.id.bt_joingroup);
        bt_joingroup.setOnClickListener(joingroup());
        bt_startgroup=(Button)view.findViewById(R.id.bt_startgroup);
        bt_startgroup.setOnClickListener(startgroup());

    }

    private View.OnClickListener startgroup() {
            return  new View.OnClickListener() {
                @Override
                public void onClick(View v) {


                }
            };
    }

    private View.OnClickListener joingroup() {
        return  new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent joingroup= new Intent(getActivity(), Join_Request.class);
                startActivity(joingroup);
            }
        };
    }


    public static interface FragmentCallbacks {
        void menuClick();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallbacks = null;
        Log.e("onDetach", "Detached");
    }


}