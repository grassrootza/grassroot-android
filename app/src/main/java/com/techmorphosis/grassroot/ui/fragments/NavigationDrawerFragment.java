package com.techmorphosis.grassroot.ui.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.techmorphosis.grassroot.BuildConfig;
import com.techmorphosis.grassroot.Interface.ClickListener;
import com.techmorphosis.grassroot.R;
import com.techmorphosis.grassroot.RecyclerView.RecyclerTouchListener;
import com.techmorphosis.grassroot.adapters.NavigationDrawerAdapter;
import com.techmorphosis.grassroot.models.NavDrawerItem;
import com.techmorphosis.grassroot.utils.SettingPreffrence;

import java.util.ArrayList;


public class NavigationDrawerFragment extends Fragment {

    private View mFragmentContainerView;
    private NavigationDrawerCallbacks mCallbacks;


    private int mCurrentSelectedPosition=0;
    private boolean mFromSavedInstanceState;
    private static final String STATE_SELECTED_POSITION = "selected_navigation_drawer_position";
    private View view;
    private RecyclerView mDrawerRecyclerView;
    ArrayList draweritems;
    //public  static int mSelectedItem=0;
    private NavigationDrawerAdapter drawerAdapter;
    private DrawerLayout mDrawerLayout;
    private TextView txtVersion;
    private TextView displayName;


    public NavigationDrawerFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            mCurrentSelectedPosition = savedInstanceState.getInt(STATE_SELECTED_POSITION);
            mFromSavedInstanceState = true;
        } else 
        {
           // selectItem(mCurrentSelectedPosition);
        }
    }



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        view= inflater.inflate(R.layout.fragment_navigation_drawer, container, false);

        findView();
        txtVersion.setText("v " + BuildConfig.VERSION_NAME);


        drawerAdapter = new NavigationDrawerAdapter(getActivity(),getData());

        mDrawerRecyclerView.setHasFixedSize(true);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getActivity());
        mDrawerRecyclerView.setLayoutManager(mLayoutManager);
        mDrawerRecyclerView.setAdapter(drawerAdapter);
        mDrawerRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mDrawerRecyclerView.addOnItemTouchListener(new RecyclerTouchListener(getActivity(), mDrawerRecyclerView, new ClickListener() {
            @Override
            public void onClick(View view, int position) {
               // Log.e("drawer ", "onItemClick");
                // mSelectedItem = position;

                for (int i = 0; i < draweritems.size(); i++) {
                    NavDrawerItem item = (NavDrawerItem) draweritems.get(i);
                    // item.setIsChecked(true);
                    if (position == i) {
                        item.setIsChecked(true);
                    } else {
                        item.setIsChecked(false);
                    }

                }
                drawerAdapter.notifyDataSetChanged();
                selectItem(position);

            }

            @Override
            public void onLongClick(View view, int position) {

            }
        }));

        return view ;
    }

    private void findView()
    {

        mDrawerRecyclerView = (RecyclerView) view.findViewById(R.id.rv_nav_items);
        txtVersion = (TextView) view.findViewById(R.id.txt_version);
        displayName = (TextView) view.findViewById(R.id.displayName);
        displayName.setText(SettingPreffrence.getuser_name(getActivity()));
    }



    public  ArrayList<NavDrawerItem> getData()
    {
        draweritems = new ArrayList<NavDrawerItem>();
        draweritems.add(new NavDrawerItem(getString(R.string.Profile),R.drawable.ic_profile,R.drawable.ic_profile_green,false));
        draweritems.add(new NavDrawerItem(getString(R.string.FAQs),R.drawable.ic_faq,R.drawable.ic_faq_green,false));
        draweritems.add(new NavDrawerItem(getString(R.string.Notifications),R.drawable.ic_notification,R.drawable.ic_notification_green,false));
        draweritems.add(new NavDrawerItem(getString(R.string.Share),R.drawable.ic_share,R.drawable.ic_share_green,false));
        draweritems.add(new NavDrawerItem(getString(R.string.Rate_App),R.drawable.ic_rate_us,R.drawable.ic_rate_us_green,false));
        draweritems.add(new NavDrawerItem(getString(R.string.Logout),R.drawable.ic_logout,R.drawable.ic_logout_green,false));


        return draweritems;
    }


    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mCallbacks= (NavigationDrawerCallbacks) activity;
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }


    public interface NavigationDrawerCallbacks
    {

        void onNavigationDrawerItemSelected(int position);
    }

    public void setUp(int fragmentId, DrawerLayout drawerLayout) {
        // Log.e("drawer ", "setUp");

        mFragmentContainerView = getActivity().findViewById(fragmentId);
        mDrawerLayout = drawerLayout;

    }

    private void selectItem(int position)
    {
        mCurrentSelectedPosition = position;
        // Log.e("drawer ", "selectItem");

        if (mDrawerRecyclerView != null) {
           // mDrawerRecyclerView.setItemChecked(position, true);
        }
        if (mDrawerLayout != null) {
            mDrawerLayout.closeDrawer(mFragmentContainerView);
        }
        if (mCallbacks != null) {
            //    Log.e("drawer ", "selectItem mCallbacks != null");

            mCallbacks.onNavigationDrawerItemSelected(position);

        }
    }


    public  void clearNotificationDrawersSelection()
    {

        //displayName.setText(SettingPreffrence.getuser_name(getActivity()));

        for (int i = 0; i < draweritems.size(); i++)
        {
            NavDrawerItem item= (NavDrawerItem) draweritems.get(i);
            item.setIsChecked(false);
        }
        drawerAdapter.notifyDataSetChanged();
    }

    public  void updateNotificationDrawersname()
    {

        displayName.setText(SettingPreffrence.getuser_name(getActivity()));

}


}
