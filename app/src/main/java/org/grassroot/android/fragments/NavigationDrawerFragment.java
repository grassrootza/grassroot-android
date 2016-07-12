package org.grassroot.android.fragments;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
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
import org.grassroot.android.activities.FAQActivity;
import org.grassroot.android.activities.HomeScreenActivity;
import org.grassroot.android.activities.ProfileSettingsActivity;
import org.grassroot.android.activities.StartActivity;
import org.grassroot.android.adapters.NavigationDrawerAdapter;
import org.grassroot.android.events.GroupCreatedEvent;
import org.grassroot.android.events.GroupsRefreshedEvent;
import org.grassroot.android.events.NotificationEvent;
import org.grassroot.android.events.TaskAddedEvent;
import org.grassroot.android.events.TaskCancelledEvent;
import org.grassroot.android.events.UserLoggedOutEvent;
import org.grassroot.android.fragments.dialogs.ConfirmCancelDialogFragment;
import org.grassroot.android.interfaces.ClickListener;
import org.grassroot.android.interfaces.NavigationConstants;
import org.grassroot.android.interfaces.NotificationConstants;
import org.grassroot.android.models.Group;
import org.grassroot.android.models.NavDrawerItem;
import org.grassroot.android.services.ApplicationLoader;
import org.grassroot.android.services.GcmRegistrationService;
import org.grassroot.android.adapters.RecyclerTouchListener;
import org.grassroot.android.services.GroupService;
import org.grassroot.android.services.TaskService;
import org.grassroot.android.utils.Constant;
import org.grassroot.android.utils.PreferenceUtils;
import org.grassroot.android.utils.RealmUtils;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import okhttp3.ResponseBody;


public class NavigationDrawerFragment extends Fragment implements TaskService.TaskServiceListener {

    public static final String TAG = NavigationDrawerFragment.class.getCanonicalName();

    private NavigationDrawerCallbacks mCallbacks;

    List<NavDrawerItem> draweritems;
    private NavigationDrawerAdapter drawerAdapter;
    private int currentlySelectedItem = NavigationConstants.HOME_NAV_GROUPS;

    NavDrawerItem groups;
    NavDrawerItem tasks;
    NavDrawerItem notifications;

    @BindView(R.id.displayName) TextView displayName;
    @BindView(R.id.rv_nav_items) RecyclerView mDrawerRecyclerView;
    @BindView(R.id.nav_tv_footer) TextView txtVersion;

    public interface NavigationDrawerCallbacks {
        void onNavigationDrawerItemSelected(int position);
    }

    public NavigationDrawerFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EventBus.getDefault().register(this);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            mCallbacks = (NavigationDrawerCallbacks) context;
        } catch (ClassCastException e) {
            throw new UnsupportedOperationException("Error! Activity must implement listener");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_navigation_drawer, container, false);
        ButterKnife.bind(this, view);

        displayName.setText(PreferenceUtils.getUserName(ApplicationLoader.applicationContext));
        txtVersion.setText(String.format(getString(R.string.nav_bar_footer), BuildConfig.VERSION_NAME));

        drawerAdapter = new NavigationDrawerAdapter(getActivity(), getData());

        mDrawerRecyclerView.setHasFixedSize(true);
        mDrawerRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mDrawerRecyclerView.setAdapter(drawerAdapter);
        mDrawerRecyclerView.setItemAnimator(new DefaultItemAnimator());

        mDrawerRecyclerView.addOnItemTouchListener(new RecyclerTouchListener(getActivity(), mDrawerRecyclerView, new ClickListener() {
            @Override
            public void onClick(View view, int position) {
                selectItem(position);
            }

            @Override
            public void onLongClick(View view, int position) {
            }
        }));

        return view ;
    }

    public void resetToDefault() {
        switchSelectedState(NavigationConstants.HOME_NAV_GROUPS);
    }

    public List<NavDrawerItem> getData() {
        draweritems = new ArrayList<>();

        groups = new NavDrawerItem(getString(R.string.drawer_group_list), R.drawable.ic_home, R.drawable.ic_home_green, true, true);
        groups.setItemCount(RealmUtils.loadListFromDB(Group.class).size());
        Log.e(TAG, "size of groups loaded: " + groups.getItemCount());
        draweritems.add(groups);

        tasks = new NavDrawerItem(getString(R.string.drawer_open_tasks), R.drawable.ic_star_gray, R.drawable.ic_star_green, false, true); // todo: fix icon
        tasks.setItemCount(TaskService.getInstance().upcomingTasks.size());
        TaskService.getInstance().fetchUpcomingTasks(this);
        draweritems.add(tasks);

        notifications = new NavDrawerItem(getString(R.string.Notifications),R.drawable.ic_notification,R.drawable.ic_notification_green, false, true);
        notifications.setItemCount(PreferenceUtils.getNotificationCounter(getContext()));
        draweritems.add(notifications);

        draweritems.add(new NavDrawerItem(getString(R.string.Share), R.drawable.ic_share, R.drawable.ic_share_green, false, false));
        draweritems.add(new NavDrawerItem(getString(R.string.Profile),R.drawable.ic_profile,R.drawable.ic_profile_green,false, false));
        draweritems.add(new NavDrawerItem(getString(R.string.FAQs),R.drawable.ic_faq,R.drawable.ic_faq_green,false, false));
        draweritems.add(new NavDrawerItem(getString(R.string.Logout),R.drawable.ic_logout,R.drawable.ic_logout_green,false, false));
        return draweritems;
    }

    private void selectItem(int position) {
        // handle common & reusable things here, pass back more complex or context-dependent to activity
        int itemToSetSelected = position;
        boolean changeItemSelected = true;
        switch (position) {
            // note: first three are handed back to home screen activity to handle fragment switching
            case NavigationConstants.HOME_NAV_GROUPS:
            case NavigationConstants.HOME_NAV_TASKS:
            case NavigationConstants.HOME_NAV_NOTIFICATIONS:
                break;
            case NavigationConstants.HOME_NAV_SHARE:
                changeItemSelected = false;
                shareApp();
                break;
            case NavigationConstants.HOME_NAV_PROFILE:
                changeItemSelected = false; // until switch that to fragmet w/nav bar instead of back
                startActivity(new Intent(getActivity(), ProfileSettingsActivity.class));
                break;
            case NavigationConstants.HOME_NAV_FAQ:
                changeItemSelected = false; // as above
                startActivity(new Intent(getActivity(), FAQActivity.class));
                break;
            case NavigationConstants.HOME_NAV_LOGOUT:
                itemToSetSelected = NavigationConstants.HOME_NAV_GROUPS; // in case activity restored, on homegrouplist
                logout();
                break;
            default:
                // todo : put in handling non-standard items
        }

        if (changeItemSelected) {
            switchSelectedState(itemToSetSelected);
        }
        mCallbacks.onNavigationDrawerItemSelected(position);
    }

    private void switchSelectedState(final int selectedItem) {
        draweritems.get(currentlySelectedItem).setIsChecked(false);
        draweritems.get(selectedItem).setIsChecked(true);
        currentlySelectedItem = selectedItem;
        drawerAdapter.notifyDataSetChanged();
    }

    @Override
    public void tasksLoadedFromServer() {
        updateTaskCount();
    }

    @Override
    public void taskLoadingFromServerFailed(ResponseBody errorBody) {
        // todo : do something with this, maybe
    }

    @Override
    public void tasksLoadedFromDB() {
        updateTaskCount();
    }

    private void updateTaskCount() {
        tasks.setItemCount(TaskService.getInstance().upcomingTasks.size());
        if (drawerAdapter != null) {
            drawerAdapter.notifyItemChanged(NavigationConstants.HOME_NAV_TASKS);
        }
    }

    public void updateGroupCount(int groupCount) {
        groups.setItemCount(groupCount);
        drawerAdapter.notifyItemChanged(NavigationConstants.HOME_NAV_GROUPS);
    }

    private void shareApp() {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, getString(R.string.share_app_text));
        startActivity(shareIntent);
    }

    private void logout() {
        ConfirmCancelDialogFragment confirmDialog = ConfirmCancelDialogFragment.newInstance(R.string.logout_message, new ConfirmCancelDialogFragment.ConfirmDialogListener() {
                    @Override
                    public void doConfirmClicked() {
                        unregisterGcm();
                        PreferenceUtils.clearAll(getActivity().getApplicationContext());
                        EventBus.getDefault().post(new UserLoggedOutEvent());
                        RealmUtils.deleteAllObjects();
                        Intent open = new Intent(getActivity(), StartActivity.class);
                        startActivity(open);
                    }
                });

        confirmDialog.show(getFragmentManager(), "logout");
    }

    // todo : move this onto a background thread?
    private void unregisterGcm() {
        Log.e(TAG, "unregistering from GCM ...");
        final Context context = getActivity().getApplicationContext();
        Intent gcmUnregister = new Intent(getActivity(), GcmRegistrationService.class);
        gcmUnregister.putExtra(NotificationConstants.ACTION, NotificationConstants.GCM_UNREGISTER);
        gcmUnregister.putExtra(NotificationConstants.PHONE_NUMBER, PreferenceUtils.getUserPhoneNumber(context));
        gcmUnregister.putExtra(Constant.USER_TOKEN, PreferenceUtils.getAuthToken(context));
        getActivity().startService(gcmUnregister);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    @Subscribe
    public void onGroupsRefreshedEvent(GroupsRefreshedEvent e) {
        int groupCount = GroupService.getInstance().getGroups().size();
        groups.setItemCount(groupCount);
        drawerAdapter.notifyItemChanged(NavigationConstants.HOME_NAV_GROUPS);
    }

    @Subscribe
    public void onNewNotificationEvent(NotificationEvent event) {
        int notificationCount = event.getNotificationCount();
        Log.e(TAG, "notification count" + notificationCount);
        drawerAdapter.notifyDataSetChanged();
    }

    @Subscribe
    public void onTaskCreatedEvent(TaskAddedEvent e) {
        tasks.incrementItemCount();
        drawerAdapter.notifyItemChanged(NavigationConstants.HOME_NAV_TASKS);
    }

    @Subscribe
    public void onTaskCancelledEvent(TaskCancelledEvent e) {
        tasks.decrementItemCount();
        drawerAdapter.notifyItemChanged(NavigationConstants.HOME_NAV_TASKS);
    }

    @Subscribe
    public void onGroupAdded(GroupCreatedEvent e) {
        groups.incrementItemCount();
        drawerAdapter.notifyItemChanged(NavigationConstants.HOME_NAV_GROUPS);
    }

}