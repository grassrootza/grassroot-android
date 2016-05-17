package com.techmorphosis.grassroot.adapters;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import com.techmorphosis.grassroot.ui.fragments.walkthrough.FiveFragment;
import com.techmorphosis.grassroot.ui.fragments.walkthrough.FourFragment;
import com.techmorphosis.grassroot.ui.fragments.walkthrough.OneFragment;
import com.techmorphosis.grassroot.ui.fragments.walkthrough.ThreeFragment;
import com.techmorphosis.grassroot.ui.fragments.walkthrough.TwoFragment;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by admin on 22-Mar-16.
 */public class WelcomePagerAdapter extends FragmentPagerAdapter {

    private List<Fragment> fragments;

    public WelcomePagerAdapter(FragmentManager fm) {
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
