package com.techmorphosis.grassroot.adapters;

import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import com.techmorphosis.grassroot.ui.fragments.viewpager.FiveFragment;
import com.techmorphosis.grassroot.ui.fragments.viewpager.FourFragment;
import com.techmorphosis.grassroot.ui.fragments.viewpager.OneFragment;
import com.techmorphosis.grassroot.ui.fragments.viewpager.ThreeFragment;
import com.techmorphosis.grassroot.ui.fragments.viewpager.TwoFragment;


/**
 * Created by admin on 22-Mar-16.
 */public class MyPagerAdapter extends FragmentPagerAdapter {


    public MyPagerAdapter(FragmentManager fm) {
        super(fm);
    }

    public int getCount() {
        return 5;
    }

    public android.support.v4.app.Fragment getItem(int index) {

        switch (index) {
            case 0:
                return new OneFragment();

            case 1:
                return new TwoFragment();

            case 2:
                return new ThreeFragment();

            case 3:

                return new FourFragment();

            case 4:
                return new FiveFragment();


        }

        return null;
    }
}
