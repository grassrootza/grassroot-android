package org.grassroot.android.ui.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.grassroot.android.BuildConfig;
import org.grassroot.android.R;
import org.grassroot.android.adapters.NavigationDrawerAdapter;
import org.grassroot.android.events.NotificationEvent;
import org.grassroot.android.interfaces.ClickListener;
import org.grassroot.android.models.NavDrawerItem;
import org.grassroot.android.ui.views.RecyclerTouchListener;
import org.grassroot.android.utils.PreferenceUtils;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;


public class NavigationDrawerFragment extends Fragment {

    public static final String TAG = NavigationDrawerFragment.class.getCanonicalName();

    private View mFragmentContainerView;
    private NavigationDrawerCallbacks mCallbacks;
    private int mCurrentSelectedPosition=0;
    private boolean mFromSavedInstanceState;

    private static final String STATE_SELECTED_POSITION = "selected_navigation_drawer_position";
    ArrayList draweritems;

    private NavigationDrawerAdapter drawerAdapter;
    private DrawerLayout mDrawerLayout;

    @BindView(R.id.rv_nav_items)
    RecyclerView mDrawerRecyclerView;
    @BindView(R.id.txt_version)
    TextView txtVersion;
    @BindView(R.id.displayName)
    TextView displayName;

    public NavigationDrawerFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            mCurrentSelectedPosition = savedInstanceState.getInt(STATE_SELECTED_POSITION);
            mFromSavedInstanceState = true;
        } else {
           // selectItem(mCurrentSelectedPosition);
        }
        EventBus.getDefault().register(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_navigation_drawer, container, false);
        ButterKnife.bind(this, view);

        displayName.setText(PreferenceUtils.getuser_name(getActivity()));
        txtVersion.setText("v " + BuildConfig.VERSION_NAME);

        drawerAdapter = new NavigationDrawerAdapter(getActivity(),getData());

        mDrawerRecyclerView.setHasFixedSize(true);
        mDrawerRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mDrawerRecyclerView.setAdapter(drawerAdapter);
        mDrawerRecyclerView.setItemAnimator(new DefaultItemAnimator());

        mDrawerRecyclerView.addOnItemTouchListener(new RecyclerTouchListener(getActivity(), mDrawerRecyclerView, new ClickListener() {
            @Override
            public void onClick(View view, int position) {
                Log.e("drawer ", "onItemClick");

                for (int i = 0; i < draweritems.size(); i++) {
                    NavDrawerItem item= (NavDrawerItem) draweritems.get(i);
                    if (position==i) {
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

    public  ArrayList<NavDrawerItem> getData() {
        draweritems = new ArrayList<>();
        draweritems.add(new NavDrawerItem(getString(R.string.Profile),R.drawable.ic_profile,R.drawable.ic_profile_green,false));
        draweritems.add(new NavDrawerItem(getString(R.string.FAQs),R.drawable.ic_faq,R.drawable.ic_faq_green,false));
        draweritems.add(new NavDrawerItem(getString(R.string.Notifications),R.drawable.ic_notification,R.drawable.ic_notification_green,false));
        draweritems.add(new NavDrawerItem(getString(R.string.Logout),R.drawable.ic_logout,R.drawable.ic_logout_green,false));
        return draweritems;
    }


    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mCallbacks= (NavigationDrawerCallbacks) activity;
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    public interface NavigationDrawerCallbacks {
        void onNavigationDrawerItemSelected(int position);
    }

    public void setUp(int fragmentId, DrawerLayout drawerLayout) {
        mFragmentContainerView = getActivity().findViewById(fragmentId);
        mDrawerLayout = drawerLayout;
    }

    private void selectItem(int position) {
        mCurrentSelectedPosition = position;

        if (mDrawerLayout != null) {
            mDrawerLayout.closeDrawer(mFragmentContainerView);
        }
        if (mCallbacks != null) {
            mCallbacks.onNavigationDrawerItemSelected(position);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    @Subscribe
    public void onNewNotificationEvent(NotificationEvent event) {
        Log.e(TAG, "redraw navigation drawer");
        int notificationCount = event.getNotificationCount();
        Log.e(TAG, "notification count" +notificationCount);
        drawerAdapter.notifyDataSetChanged();
    }

}
