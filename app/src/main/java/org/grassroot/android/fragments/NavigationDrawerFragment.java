package org.grassroot.android.fragments;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
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
import org.grassroot.android.interfaces.NavigationConstants;
import org.grassroot.android.interfaces.NotificationConstants;
import org.grassroot.android.models.Group;
import org.grassroot.android.models.NavDrawerItem;
import org.grassroot.android.services.GcmRegistrationService;
import org.grassroot.android.services.GroupService;
import org.grassroot.android.services.TaskService;
import org.grassroot.android.utils.Constant;
import org.grassroot.android.utils.RealmUtils;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class NavigationDrawerFragment extends Fragment implements NavigationDrawerAdapter.NavDrawerItemListener {

    private static final String TAG = NavigationDrawerFragment.class.getCanonicalName();

    public static final String ITEM_SHOW_GROUPS = "show_groups";
    public static final String ITEM_TASKS = "upcoming_tasks";
    public static final String ITEM_NOTIFICATIONS = "notifications";
    public static final String ITEM_JOIN_REQS = "join_requests";

    public static final String ITEM_OFFLINE = "offline_online";
    public static final String ITEM_PROFILE = "profile_settings";
    public static final String ITEM_SHARE = "item_share";
    public static final String ITEM_FAQ = "item_faq";

    public static final String ITEM_LOGOUT = "item_logout";

    private NavigationDrawerCallbacks mCallbacks;

    List<NavDrawerItem> primaryItems;
    private NavigationDrawerAdapter primaryAdapter;

    List<NavDrawerItem> secondaryItems;
    private NavigationDrawerAdapter secondaryAdapter;

    private int currentlySelectedItem = NavigationConstants.HOME_NAV_GROUPS;

    NavDrawerItem groups;
    NavDrawerItem tasks;
    NavDrawerItem notifications;
    NavDrawerItem joinRequests;

    @BindView(R.id.displayName) TextView displayName;
    @BindView(R.id.nav_items_primary) RecyclerView primaryItemsView;
    @BindView(R.id.nav_items_secondary) RecyclerView secondaryItemsView;
    @BindView(R.id.nav_tv_footer) TextView txtVersion;

    public interface NavigationDrawerCallbacks {
        void onNavigationDrawerItemSelected(final String tag);
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

        displayName.setText(RealmUtils.loadPreferencesFromDB().getUserName());
        txtVersion.setText(String.format(getString(R.string.nav_bar_footer), BuildConfig.VERSION_NAME));

        primaryAdapter = new NavigationDrawerAdapter(getActivity(), setUpPrimaryItems(), true, true, this);
        primaryItemsView.setHasFixedSize(true);
        primaryItemsView.setItemViewCacheSize(4);
        primaryItemsView.setLayoutManager(new LinearLayoutManager(getContext()));
        primaryItemsView.setAdapter(primaryAdapter);

        secondaryAdapter = new NavigationDrawerAdapter(getActivity(), setUpSecondaryItems(), false, false, this);
        secondaryItemsView.setHasFixedSize(true);
        secondaryItemsView.setItemViewCacheSize(5);
        secondaryItemsView.setLayoutManager(new LinearLayoutManager(getContext()));
        secondaryItemsView.setAdapter(secondaryAdapter);

        return view ;
    }

    public List<NavDrawerItem> setUpPrimaryItems() {
        primaryItems = new ArrayList<>();

        groups = new NavDrawerItem(ITEM_SHOW_GROUPS, getString(R.string.drawer_group_list), R.drawable.ic_groups_general, R.drawable.ic_groups_general, true, true);
        groups.setItemCount((int) RealmUtils.countObjectsInDB(Group.class));
        Log.d(TAG, "on set up ... size of groups loaded: " + groups.getItemCount());
        primaryItems.add(groups);

        tasks = new NavDrawerItem(ITEM_TASKS, getString(R.string.drawer_open_tasks), R.drawable.ic_task_green, R.drawable.ic_task_green, false, true); // todo: fix icon
        tasks.setItemCount(TaskService.getInstance().upcomingTasks.size());
        primaryItems.add(tasks);

        notifications = new NavDrawerItem(ITEM_NOTIFICATIONS, getString(R.string.Notifications), R.drawable.ic_exclamation_small, R.drawable.ic_exclamation_small, false, true);
        notifications.setItemCount(RealmUtils.loadPreferencesFromDB().getNotificationCounter());
        primaryItems.add(notifications);

        joinRequests = new NavDrawerItem(ITEM_JOIN_REQS, getString(R.string.drawer_join_request), R.drawable.ic_notification, R.drawable.ic_notification_green, false, true);
        joinRequests.setItemCount(GroupService.getInstance().loadRequestsFromDB().size());
        primaryItems.add(joinRequests);

        return primaryItems;
    }

    public List<NavDrawerItem> setUpSecondaryItems() {
        secondaryItems = new ArrayList<>();

        secondaryItems.add(new NavDrawerItem(ITEM_SHARE, getString(R.string.Share), R.drawable.ic_share, R.drawable.ic_share_green, false, false));
        secondaryItems.add(new NavDrawerItem(ITEM_PROFILE, getString(R.string.Profile),R.drawable.ic_profile,R.drawable.ic_profile_green,false, false));
        secondaryItems.add(new NavDrawerItem(ITEM_FAQ, getString(R.string.FAQs),R.drawable.ic_faq,R.drawable.ic_faq_green,false, false));
        secondaryItems.add(new NavDrawerItem(ITEM_LOGOUT, getString(R.string.Logout),R.drawable.ic_logout,R.drawable.ic_logout_green,false, false));

        return secondaryItems;
    }

    @Override
    public void onItemClicked(final String tag) {
        // handle common & reusable things here, pass back more complex or context-dependent to activity
        boolean changeItemSelected = true;
        switch (tag) {
            // note: first four are handed back to home screen activity to handle fragment switching
            case ITEM_SHOW_GROUPS:
            case ITEM_TASKS:
            case ITEM_NOTIFICATIONS:
            case ITEM_JOIN_REQS:
                break;
            case ITEM_SHARE:
                changeItemSelected = false;
                shareApp();
                break;
            case ITEM_PROFILE:
                changeItemSelected = false; // until switch that to fragmet w/nav bar instead of back
                startActivity(new Intent(getActivity(), ProfileSettingsActivity.class));
                break;
            case ITEM_FAQ:
                changeItemSelected = false; // as above
                startActivity(new Intent(getActivity(), FAQActivity.class));
                break;
            case ITEM_LOGOUT:
                logout();
                break;
            default:
                // todo : put in handling non-standard items
        }
        if (changeItemSelected) {
            // swap it ...
        }
        mCallbacks.onNavigationDrawerItemSelected(tag);
    }

    private void switchSelectedState(final int selectedItem) {
        primaryItems.get(currentlySelectedItem).setIsChecked(false);
        primaryItems.get(selectedItem).setIsChecked(true);
        currentlySelectedItem = selectedItem;
        primaryAdapter.notifyDataSetChanged();
    }

    private void updateTaskCount() {
        tasks.setItemCount(TaskService.getInstance().upcomingTasks.size());
        if (primaryAdapter != null) {
            primaryAdapter.notifyItemChanged(NavigationConstants.HOME_NAV_TASKS);
        }
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
        Log.d(TAG, "unregistering from GCM ...");
        Intent gcmUnregister = new Intent(getActivity(), GcmRegistrationService.class);
        gcmUnregister.putExtra(NotificationConstants.ACTION, NotificationConstants.GCM_UNREGISTER);
        gcmUnregister.putExtra(NotificationConstants.PHONE_NUMBER, RealmUtils.loadPreferencesFromDB().getMobileNumber());
        gcmUnregister.putExtra(Constant.USER_TOKEN, RealmUtils.loadPreferencesFromDB().getToken());
        getActivity().startService(gcmUnregister);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    public void refreshGroupCount() {
        groups.setItemCount((int) RealmUtils.countObjectsInDB(Group.class));
        primaryAdapter.notifyDataSetChanged();
        Log.d(TAG, "group count refreshed ... now : " + groups.getItemCount());
    }

    @Subscribe
    public void onGroupsRefreshedEvent(GroupsRefreshedEvent e) {
        refreshGroupCount();
    }

    @Subscribe
    public void onGroupAdded(GroupCreatedEvent e) {
        refreshGroupCount();
    }

    @Subscribe
    public void onNewNotificationEvent(NotificationEvent event) {
        int notificationCount = event.getNotificationCount();
        Log.d(TAG, "notification count" + notificationCount);
        primaryAdapter.notifyDataSetChanged();
    }

    @Subscribe
    public void onTaskCreatedEvent(TaskAddedEvent e) {
        tasks.incrementItemCount();
        primaryAdapter.notifyItemChanged(NavigationConstants.HOME_NAV_TASKS);
    }

    @Subscribe
    public void onTaskCancelledEvent(TaskCancelledEvent e) {
        tasks.decrementItemCount();
        primaryAdapter.notifyItemChanged(NavigationConstants.HOME_NAV_TASKS);
    }

}