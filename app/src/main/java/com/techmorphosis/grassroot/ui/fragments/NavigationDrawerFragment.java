package com.techmorphosis.grassroot.ui.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.techmorphosis.grassroot.BuildConfig;
import com.techmorphosis.grassroot.R;
import com.techmorphosis.grassroot.adapters.NavigationDrawerAdapter;
import com.techmorphosis.grassroot.models.NavDrawerItem;

import java.util.ArrayList;


public class NavigationDrawerFragment extends Fragment {

    private View mFragmentContainerView;
    private NavigationDrawerCallbacks mCallbacks;

    private String mParam1;
    private String mParam2;

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
            selectItem(mCurrentSelectedPosition);
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
                Log.e("drawer ", "onItemClick");
               // mSelectedItem = position;

                for (int i = 0; i < draweritems.size(); i++) {
                    NavDrawerItem item= (NavDrawerItem) draweritems.get(i);
                   // item.setIsChecked(true);
                    if (position==i)
                    {
                        item.setIsChecked(true);
                    }
                    else
                    {
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
    }

    public  void Adapter()
    {
          }

    public  ArrayList<NavDrawerItem> getData()
    {
        draweritems = new ArrayList<NavDrawerItem>();
        draweritems.add(new NavDrawerItem(getString(R.string.Profile),R.drawable.ic_profile,R.drawable.ic_profile_green,true));
        draweritems.add(new NavDrawerItem(getString(R.string.Settings),R.drawable.ic_settings,R.drawable.ic_settings_green,false));
        draweritems.add(new NavDrawerItem(getString(R.string.FAQs),R.drawable.ic_faq,R.drawable.ic_faq_green,false));
        draweritems.add(new NavDrawerItem(getString(R.string.Logout),R.drawable.ic_logout,R.drawable.ic_logout_green,false));
        draweritems.add(new NavDrawerItem(getString(R.string.Share),R.drawable.ic_share,R.drawable.ic_share_green,false));
        draweritems.add(new NavDrawerItem(getString(R.string.Rate_App),R.drawable.ic_rate_us,R.drawable.ic_rate_us_green,false));


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

    public static interface ClickListener {
        public void onClick(View view, int position);

        public void onLongClick(View view, int position);
    }


    static class RecyclerTouchListener implements RecyclerView.OnItemTouchListener {

        private GestureDetector gestureDetector;
        private ClickListener clickListener;

        public RecyclerTouchListener(Context context, final RecyclerView recyclerView, final ClickListener clickListener) {
            this.clickListener = clickListener;
            gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
                @Override
                public boolean onSingleTapUp(MotionEvent e) {
                    return true;
                }

                @Override
                public void onLongPress(MotionEvent e) {
                    View child = recyclerView.findChildViewUnder(e.getX(), e.getY());
                    if (child != null && clickListener != null) {
                        clickListener.onLongClick(child, recyclerView.getChildPosition(child));
                    }
                }
            });
        }

        @Override
        public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent e) {

            View child = rv.findChildViewUnder(e.getX(), e.getY());
            if (child != null && clickListener != null && gestureDetector.onTouchEvent(e)) {
                clickListener.onClick(child, rv.getChildPosition(child));
            }
            return false;
        }

        @Override
        public void onTouchEvent(RecyclerView rv, MotionEvent e) {
        }

        @Override
        public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {

        }


    }

}
