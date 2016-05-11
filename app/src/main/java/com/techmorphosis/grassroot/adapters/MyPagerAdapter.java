package com.techmorphosis.grassroot.adapters;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.ListFragment;

import com.techmorphosis.grassroot.ui.fragments.FiveFragment;
import com.techmorphosis.grassroot.ui.fragments.FourFragment;
import com.techmorphosis.grassroot.ui.fragments.OneFragment;
import com.techmorphosis.grassroot.ui.fragments.ThreeFragment;
import com.techmorphosis.grassroot.ui.fragments.TwoFragment;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by admin on 22-Mar-16.
 */public class MyPagerAdapter extends FragmentPagerAdapter {

    private List<Fragment> fragments;

    public MyPagerAdapter(FragmentManager fm) {
        super(fm);
        initList();
    }

    public int getCount() {
        return fragments.size();
    }

    public android.support.v4.app.Fragment getItem(int index) {
        return fragments.get(index);
    }

    private void initList(){

        fragments = new ArrayList<>();
        fragments.add(new OneFragment());
        fragments.add(new TwoFragment());
        fragments.add(new ThreeFragment());
        fragments.add(new FourFragment());
        fragments.add(new FiveFragment());
    }
}
