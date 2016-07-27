package org.grassroot.android.fragments;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
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
import org.grassroot.android.events.ConnectionFailedEvent;
import org.grassroot.android.events.GroupCreatedEvent;
import org.grassroot.android.events.GroupsRefreshedEvent;
import org.grassroot.android.events.NotificationEvent;
import org.grassroot.android.events.OfflineActionsSent;
import org.grassroot.android.events.OnlineOfflineToggledEvent;
import org.grassroot.android.events.TaskAddedEvent;
import org.grassroot.android.events.TaskCancelledEvent;
import org.grassroot.android.events.TasksRefreshedEvent;
import org.grassroot.android.events.UserLoggedOutEvent;
import org.grassroot.android.fragments.dialogs.ConfirmCancelDialogFragment;
import org.grassroot.android.interfaces.NavigationConstants;
import org.grassroot.android.interfaces.NotificationConstants;
import org.grassroot.android.models.Group;
import org.grassroot.android.models.GroupJoinRequest;
import org.grassroot.android.models.NavDrawerItem;
import org.grassroot.android.services.GcmRegistrationService;
import org.grassroot.android.services.TaskService;
import org.grassroot.android.utils.Constant;
import org.grassroot.android.utils.NetworkUtils;
import org.grassroot.android.utils.RealmUtils;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class NavigationDrawerFragment extends Fragment implements NavigationDrawerAdapter.NavDrawerItemListener, NetworkUtils.NetworkListener {

    private static final String TAG = NavigationDrawerFragment.class.getSimpleName();

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

    private String defaultSelectedItemTag;
    private NavDrawerItem groups;
    private NavDrawerItem tasks;
    private NavDrawerItem notifications;
    private NavDrawerItem joinRequests;

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

    public void setSelectedItem(String itemTag) {
        if (primaryAdapter == null || primaryAdapter.getItemCount() == 0) {
            defaultSelectedItemTag = itemTag;
        } else {
            switchPrimarySelected(itemTag);
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

        return view;
    }

    public List<NavDrawerItem> setUpPrimaryItems() {
        primaryItems = new ArrayList<>();

        final String defltItem = TextUtils.isEmpty(defaultSelectedItemTag) ? ITEM_SHOW_GROUPS : defaultSelectedItemTag;

        groups = new NavDrawerItem(ITEM_SHOW_GROUPS, getString(R.string.drawer_group_list), R.drawable.ic_groups_black, R.drawable.ic_groups_green,
            ITEM_SHOW_GROUPS.equals(defltItem), true);
        groups.setItemCount((int) RealmUtils.countObjectsInDB(Group.class));
        primaryItems.add(groups);

        tasks = new NavDrawerItem(ITEM_TASKS, getString(R.string.drawer_open_tasks), R.drawable.ic_tasks_black, R.drawable.ic_tasks_green,
            ITEM_TASKS.equals(defltItem), true);
        tasks.setItemCount(RealmUtils.loadUpcomingTasksFromDB().size());
        primaryItems.add(tasks);

        notifications = new NavDrawerItem(ITEM_NOTIFICATIONS, getString(R.string.drawer_notis), R.drawable.ic_exclamation_black, R.drawable.ic_excl_green,
            ITEM_NOTIFICATIONS.equals(defltItem), true);
        notifications.setItemCount(RealmUtils.loadPreferencesFromDB().getNotificationCounter());
        primaryItems.add(notifications);

        joinRequests = new NavDrawerItem(ITEM_JOIN_REQS, getString(R.string.drawer_join_request), R.drawable.ic_join_black, R.drawable.ic_join_green,
            ITEM_JOIN_REQS.equals(defltItem), true);
        joinRequests.setItemCount((int) RealmUtils.countObjectsInDB(GroupJoinRequest.class));
        primaryItems.add(joinRequests);

        return primaryItems;
    }

    public List<NavDrawerItem> setUpSecondaryItems() {
        secondaryItems = new ArrayList<>();

        setupOnlineSwitch();
        secondaryItems.add(new NavDrawerItem(ITEM_SHARE, getString(R.string.drawer_share), R.drawable.ic_share));
        secondaryItems.add(new NavDrawerItem(ITEM_PROFILE, getString(R.string.drawer_profile),R.drawable.ic_profile));
        secondaryItems.add(new NavDrawerItem(ITEM_FAQ, getString(R.string.drawer_faqs),R.drawable.ic_faq));
        secondaryItems.add(new NavDrawerItem(ITEM_LOGOUT, getString(R.string.drawer_logout),R.drawable.ic_logout));

        return secondaryItems;
    }

    private void setupOnlineSwitch() {
        final String currentStatus = RealmUtils.loadPreferencesFromDB().getOnlineStatus();
        int labelResource;
        switch (currentStatus) {
            case NetworkUtils.ONLINE_DEFAULT:
                labelResource = R.string.drawer_offline;
                break;
            case NetworkUtils.OFFLINE_ON_FAIL:
                labelResource = R.string.drawer_online_failed;
                break;
            case NetworkUtils.OFFLINE_SELECTED:
                labelResource = R.string.drawer_online_delib;
                break;
            default:
                return;
        }

        if (secondaryItems.isEmpty()) {
            secondaryItems.add(new NavDrawerItem(ITEM_OFFLINE, getString(labelResource), R.drawable.ic_configure));
        } else {
            secondaryItems.get(0).setItemLabel(getString(labelResource));
            secondaryAdapter.notifyItemChanged(0);
        }
    }

    @Override
    public void onItemClicked(final String tag) {
        // handle common & reusable things here, pass back more complex or context-dependent to activity
        switch (tag) {
            case ITEM_SHOW_GROUPS:
            case ITEM_TASKS:
            case ITEM_NOTIFICATIONS:
            case ITEM_JOIN_REQS:
                break;
            case ITEM_OFFLINE:
                offlineSwitch();
                break;
            case ITEM_SHARE:
                shareApp();
                break;
            case ITEM_PROFILE:
                startActivity(new Intent(getActivity(), ProfileSettingsActivity.class));
                break;
            case ITEM_FAQ:
                startActivity(new Intent(getActivity(), FAQActivity.class));
                break;
            case ITEM_LOGOUT:
                logout();
                break;
            default:
                // todo : put in handling non-standard items
        }
        mCallbacks.onNavigationDrawerItemSelected(tag);
    }

    private void switchPrimarySelected(String item) {
        try {
            groups.setIsChecked(ITEM_SHOW_GROUPS.equals(item));
            tasks.setIsChecked(ITEM_TASKS.equals(item));
            notifications.setIsChecked(ITEM_NOTIFICATIONS.equals(item));
            joinRequests.setIsChecked(ITEM_JOIN_REQS.equals(item));
            primaryAdapter.notifyDataSetChanged();
        } catch (Exception e) {
            e.printStackTrace(); // means some weirdness with null pointers somewhere above
        }
    }

    private void offlineSwitch() {
        final String currentStatus = RealmUtils.loadPreferencesFromDB().getOnlineStatus();
        int labelResource = currentStatus.equals(NetworkUtils.ONLINE_DEFAULT) ? R.string.online_go_offline
                : currentStatus.equals(NetworkUtils.OFFLINE_SELECTED) ? R.string.offline_go_online_delib : R.string.offline_go_online_failed;
        ConfirmCancelDialogFragment confirmDialog = ConfirmCancelDialogFragment.newInstance(labelResource, new ConfirmCancelDialogFragment.ConfirmDialogListener() {
                    @Override
                    public void doConfirmClicked() {
                        NetworkUtils.toggleOnlineOffline(getContext(), true, NavigationDrawerFragment.this);
                    }
                });
        confirmDialog.show(getFragmentManager(), "offline_online");
    }

    @Override
    public void connectionEstablished() {
        showDoneDialog(R.string.go_online_success);
    }

    @Override
    public void networkAvailableButConnectFailed(String failureType) {
        showDoneDialog(R.string.go_online_failure_server);
    }

    @Override
    public void networkNotAvailable() {
        showDoneDialog(R.string.go_online_failure_network);
    }

    @Override
    public void setOffline() {
        showDoneDialog(R.string.gone_offline);
    }

    private void showDoneDialog(int message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setMessage(message);
        builder.setPositiveButton(R.string.online_offline_done, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.create().show();
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

    @Subscribe
    public void onTasksRefreshedEvent(TasksRefreshedEvent e) {
        tasks.setItemCount(RealmUtils.loadUpcomingTasksFromDB().size());
    }

    @Subscribe
    public void onEvent(ConnectionFailedEvent e) {
        setupOnlineSwitch();
    }

    @Subscribe
    public void onEvent(OfflineActionsSent e) {
        setupOnlineSwitch();
    }

    @Subscribe
    public void onEvent(OnlineOfflineToggledEvent e) {
        setupOnlineSwitch();
    }

}