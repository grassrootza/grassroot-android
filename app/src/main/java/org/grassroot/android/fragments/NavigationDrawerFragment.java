package org.grassroot.android.fragments;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
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
import android.widget.Toast;

import org.grassroot.android.R;
import org.grassroot.android.activities.GrassrootExtraActivity;
import org.grassroot.android.activities.GroupSearchActivity;
import org.grassroot.android.activities.ProfileSettingsActivity;
import org.grassroot.android.activities.StartActivity;
import org.grassroot.android.adapters.NavigationDrawerAdapter;
import org.grassroot.android.events.GroupCreatedEvent;
import org.grassroot.android.events.GroupsRefreshedEvent;
import org.grassroot.android.events.JoinRequestEvent;
import org.grassroot.android.events.NetworkFailureEvent;
import org.grassroot.android.events.NotificationCountChangedEvent;
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
import org.grassroot.android.models.exceptions.ApiCallException;
import org.grassroot.android.models.helpers.NavDrawerItem;
import org.grassroot.android.services.ApplicationLoader;
import org.grassroot.android.services.GcmRegistrationService;
import org.grassroot.android.services.MqttConnectionManager;
import org.grassroot.android.utils.Constant;
import org.grassroot.android.utils.LoginRegUtils;
import org.grassroot.android.utils.NetworkUtils;
import org.grassroot.android.utils.RealmUtils;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;

public class NavigationDrawerFragment extends Fragment implements NavigationDrawerAdapter.NavDrawerItemListener {

    private static final String TAG = NavigationDrawerFragment.class.getSimpleName();

    private static final String ITEM_SHOW_GROUPS = NavigationConstants.ITEM_SHOW_GROUPS;
    private static final String ITEM_TASKS = NavigationConstants.ITEM_TASKS;
    private static final String ITEM_NOTIFICATIONS = NavigationConstants.ITEM_NOTIFICATIONS;
    private static final String ITEM_JOIN_REQS = NavigationConstants.ITEM_JOIN_REQS;

    public static final String ITEM_GRASSROOT_EXTRA = "grassroot_extra";
    public static final String ITEM_FIND_GROUPS = "find_groups";
    public static final String ITEM_OFFLINE = "offline_online";
    public static final String ITEM_PROFILE = "profile_settings";
    public static final String ITEM_SHARE = "item_share";

    public static final String ITEM_FAQ = "item_faq";
    public static final String ITEM_RATE = "item_rate";
    public static final String ITEM_LOGOUT = "item_logout";

    private NavigationDrawerCallbacks mCallbacks;

    List<NavDrawerItem> itemList;
    private NavigationDrawerAdapter itemAdapter;
    Map<Integer, Integer> positionMap;

    private String defaultSelectedItemTag;
    private String currentlySelectedItemTag;

    private NavDrawerItem groups;
    private NavDrawerItem tasks;
    private NavDrawerItem notifications;
    private NavDrawerItem joinRequests;
    private NavDrawerItem onlineOffineSwitch;

    private boolean primaryItemsActive;
    private boolean onStartUp;

    Unbinder unbinder;
    @BindView(R.id.displayName) TextView displayName;
    @BindView(R.id.nav_items_primary) RecyclerView primaryItemsView;
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
            primaryItemsActive = RealmUtils.loadPreferencesFromDB().isHasGroups();
            setUpPrimaryItems();
            itemAdapter = new NavigationDrawerAdapter(getActivity(), itemList, positionMap, this);
        } catch (ClassCastException e) {
            throw new UnsupportedOperationException("Error! Activity must implement listener");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_navigation_drawer, container, false);
        unbinder = ButterKnife.bind(this, view);

        displayName.setText(RealmUtils.loadPreferencesFromDB().getUserName());
        // txtVersion.setText(String.format(getString(R.string.nav_bar_footer), BuildConfig.VERSION_NAME));

        primaryItemsView.setHasFixedSize(true);
        primaryItemsView.setItemViewCacheSize(10);
        primaryItemsView.setLayoutManager(new LinearLayoutManager(getContext()));
        primaryItemsView.setAdapter(itemAdapter);

        onStartUp = true;

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!onStartUp) {
            refreshCounts();
        } else {
            onStartUp = false;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
        // note : may need to update some counters etc even when views null, so don't unregister here (instead check for null in item change)
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    public void setSelectedItem(String itemTag) {
        if (itemAdapter == null || itemAdapter.getItemCount() == 0) {
            defaultSelectedItemTag = itemTag;
        } else {
            currentlySelectedItemTag = itemTag;
            switchPrimarySelected(itemTag);
        }
    }

    public void clearSelected() {
        switchPrimarySelected("");
    }

    public List<NavDrawerItem> setUpPrimaryItems() {

        itemList = new ArrayList<>();
        positionMap = new HashMap<>();
        int pos = 0;

        // if the user does not have groups, then the top four shouldn't have selection & counters (will just show toast)
        final int topFourItemType = primaryItemsActive ? NavigationDrawerAdapter.PRIMARY : NavigationDrawerAdapter.SECONDARY;

        final String defltItem = TextUtils.isEmpty(defaultSelectedItemTag) ? ITEM_SHOW_GROUPS : defaultSelectedItemTag;

        groups = new NavDrawerItem(ITEM_SHOW_GROUPS, getString(R.string.drawer_group_list), R.drawable.ic_groups_black, R.drawable.ic_groups_green,
            ITEM_SHOW_GROUPS.equals(defltItem), true);
        groups.setItemCount((int) RealmUtils.countObjectsInDB(Group.class));
        pos = addItemAndIncrement(pos, topFourItemType, groups);

        notifications = new NavDrawerItem(ITEM_NOTIFICATIONS, getString(R.string.drawer_notis), R.drawable.ic_exclamation_black, R.drawable.ic_excl_green,
            ITEM_NOTIFICATIONS.equals(defltItem), true);
        notifications.setItemCount(RealmUtils.loadPreferencesFromDB().getNotificationCounter());
        pos = addItemAndIncrement(pos, topFourItemType, notifications);

        tasks = new NavDrawerItem(ITEM_TASKS, getString(R.string.drawer_open_tasks), R.drawable.ic_tasks_black, R.drawable.ic_tasks_green,
            ITEM_TASKS.equals(defltItem), true);
        tasks.setItemCount((int) RealmUtils.countUpcomingTasksInDB());
        pos = addItemAndIncrement(pos, topFourItemType, tasks);

        joinRequests = new NavDrawerItem(ITEM_JOIN_REQS, getString(R.string.drawer_join_request), R.drawable.ic_join_black, R.drawable.ic_join_green,
            ITEM_JOIN_REQS.equals(defltItem), true);
        joinRequests.setItemCount((int) RealmUtils.countObjectsInDB(GroupJoinRequest.class));
        pos = addItemAndIncrement(pos, topFourItemType, joinRequests);

        pos = addItemAndIncrement(pos, NavigationDrawerAdapter.SEPARATOR, new NavDrawerItem());

        pos = addItemAndIncrement(pos, NavigationDrawerAdapter.SECONDARY,
            new NavDrawerItem(ITEM_FIND_GROUPS, getString(R.string.find_group_nav), R.drawable.ic_find_group_nav));

        pos = addItemAndIncrement(pos, NavigationDrawerAdapter.SECONDARY,
                new NavDrawerItem(ITEM_GRASSROOT_EXTRA, getString(R.string.drawer_grassroot_extra), R.drawable.ic_add_black_24dp));

        pos = addItemAndIncrement(pos, NavigationDrawerAdapter.SECONDARY,
                new NavDrawerItem(ITEM_SHARE, getString(R.string.drawer_share), R.drawable.ic_share));

        pos = addItemAndIncrement(pos, NavigationDrawerAdapter.SECONDARY,
            new NavDrawerItem(ITEM_PROFILE, getString(R.string.drawer_profile),R.drawable.ic_profile));

        setupOnlineSwitch();
        pos = addItemAndIncrement(pos, NavigationDrawerAdapter.SECONDARY, onlineOffineSwitch);

        pos = addItemAndIncrement(pos, NavigationDrawerAdapter.SEPARATOR, new NavDrawerItem());

        if (canRateApp()) {
            pos = addItemAndIncrement(pos, NavigationDrawerAdapter.SECONDARY,
                new NavDrawerItem(ITEM_RATE, getString(R.string.drawer_rate_us), R.drawable.ic_star));
        }

        addItemAndIncrement(pos, NavigationDrawerAdapter.SECONDARY,
            new NavDrawerItem(ITEM_LOGOUT, getString(R.string.drawer_logout),R.drawable.ic_logout));

        return itemList;
    }

    private int addItemAndIncrement(int currentPosition, int viewType, NavDrawerItem item) {
        itemList.add(item);
        positionMap.put(currentPosition, viewType);
        return currentPosition + 1;
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

        if (onlineOffineSwitch == null) {
            onlineOffineSwitch = new NavDrawerItem(ITEM_OFFLINE, getString(labelResource), R.drawable.ic_configure);
        } else {
            onlineOffineSwitch.setItemLabel(getString(labelResource));
            itemAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onItemClicked(final String tag) {
        switch (tag) {
            case ITEM_SHOW_GROUPS:
            case ITEM_TASKS:
            case ITEM_NOTIFICATIONS:
            case ITEM_JOIN_REQS:
                if (primaryItemsActive) {
                    setSelectedItem(tag);
                } else {
                    showNoGroupsToast();
                }
                break;
            case ITEM_OFFLINE:
                offlineSwitch();
                break;
            case ITEM_GRASSROOT_EXTRA:
                openGrassrootExtra();
                break;
            case ITEM_FIND_GROUPS:
                findGroups();
                break;
            case ITEM_SHARE:
                shareApp();
                break;
            case ITEM_PROFILE:
                startActivity(new Intent(getActivity(), ProfileSettingsActivity.class));
                break;
            case ITEM_RATE:
                try {
                    startActivity(createRateIntent());
                } catch (ActivityNotFoundException e) {
                    Toast.makeText(ApplicationLoader.applicationContext, R.string.local_error_no_rate_activity,
                        Toast.LENGTH_SHORT).show();
                }
                break;
            case ITEM_LOGOUT:
                logout();
                break;
            default:
                Log.e(TAG, "error! non-standard tag passed in navigation drawer callback");
        }
        mCallbacks.onNavigationDrawerItemSelected(tag);
    }

    private void switchPrimarySelected(String item) {
        try {
            groups.setIsChecked(ITEM_SHOW_GROUPS.equals(item));
            tasks.setIsChecked(ITEM_TASKS.equals(item));
            notifications.setIsChecked(ITEM_NOTIFICATIONS.equals(item));
            joinRequests.setIsChecked(ITEM_JOIN_REQS.equals(item));
            itemAdapter.notifyDataSetChanged();
        } catch (Exception e) {
            e.printStackTrace(); // means some weirdness with null pointers somewhere above
        }
    }

    // make sure to switch the flag before calling this
    private void switchPrimaryItemsActiveState() {
        final int newType = primaryItemsActive ? NavigationDrawerAdapter.PRIMARY : NavigationDrawerAdapter.SECONDARY;
        positionMap.put(0, newType);
        positionMap.put(1, newType);
        positionMap.put(2, newType);
        positionMap.put(3, newType);
        itemAdapter.switchItemTypes(positionMap);
    }

    private void showNoGroupsToast() {
        Toast.makeText(ApplicationLoader.applicationContext, R.string.toast_no_groups, Toast.LENGTH_SHORT)
            .show();
    }

    private void offlineSwitch() {
        final String currentStatus = RealmUtils.loadPreferencesFromDB().getOnlineStatus();
        int labelResource = currentStatus.equals(NetworkUtils.ONLINE_DEFAULT) ? R.string.online_go_offline
                : currentStatus.equals(NetworkUtils.OFFLINE_SELECTED) ? R.string.offline_go_online_delib : R.string.offline_go_online_failed;
        ConfirmCancelDialogFragment confirmDialog = ConfirmCancelDialogFragment.newInstance(labelResource, new ConfirmCancelDialogFragment.ConfirmDialogListener() {
                    @Override
                    public void doConfirmClicked() {
                        handleOnlineToggle();
                    }
                });
        confirmDialog.show(getFragmentManager(), "offline_online");
    }

    private void handleOnlineToggle() {
        NetworkUtils.toggleOnlineOfflineRx(getContext(), true, null).subscribe(new Observer<String>() {
            @Override
            public void onSubscribe(Disposable d) { }

            @Override
            public void onError(Throwable e) {
                if (e instanceof ApiCallException) {
                    switch(e.getMessage()) {
                        case NetworkUtils.CONNECT_ERROR:
                            showDoneDialog(R.string.go_online_failure_server);
                            break;
                        case NetworkUtils.NO_NETWORK:
                            showDoneDialog(R.string.go_online_failure_network);
                            break;
                    }
                }
            }

            @Override
            public void onNext(String s) {
                switch (s) {
                    case NetworkUtils.OFFLINE_SELECTED:
                        showDoneDialog(R.string.gone_offline);
                        break;
                    case NetworkUtils.ONLINE_DEFAULT:
                        showDoneDialog(R.string.go_online_success);
                        break;
                }
            }

            @Override
            public void onComplete() { }
        });
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

    private void openGrassrootExtra() {
        Intent i = new Intent(getContext(), GrassrootExtraActivity.class);
        startActivity(i);
    }

    private void findGroups() {
        Intent i = new Intent(getActivity(), GroupSearchActivity.class);
        i.putExtra(NavigationConstants.HOME_OPEN_ON_NAV, currentlySelectedItemTag);
        Log.e(TAG, "starting activity with tag string = " + currentlySelectedItemTag);
        startActivity(i);
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
                        final String mobileNumber = RealmUtils.loadPreferencesFromDB().getMobileNumber();
                        final String code = RealmUtils.loadPreferencesFromDB().getToken();
                        Log.e(TAG, "unsubscribing from everything ...");
                        MqttConnectionManager.getInstance()
                                .unsubscribeAllAndDisconnect(RealmUtils.loadGroupUidsSync());
                        Log.e(TAG, "mqtt cleaned up, proceeding ...");
                        unregisterGcm(); // maybe do preference switch off in log out?
                        LoginRegUtils.logOutUser(mobileNumber, code).subscribe();
                        EventBus.getDefault().post(new UserLoggedOutEvent());
                        LoginRegUtils.wipeAllButMessagesAndMsisdn();
                        Intent open = new Intent(getActivity(), StartActivity.class);
                        startActivity(open);
                    }
                });

        confirmDialog.show(getFragmentManager(), "logout");
    }

    private void unregisterGcm() {
        Intent gcmUnregister = new Intent(getActivity(), GcmRegistrationService.class);
        gcmUnregister.putExtra(NotificationConstants.ACTION, NotificationConstants.GCM_UNREGISTER);
        gcmUnregister.putExtra(NotificationConstants.PHONE_NUMBER, RealmUtils.loadPreferencesFromDB().getMobileNumber());
        gcmUnregister.putExtra(Constant.USER_TOKEN, RealmUtils.loadPreferencesFromDB().getToken());
        getActivity().startService(gcmUnregister);
    }

    private void refreshCounts() {
        refreshGroupCount();
        notifications.setItemCount(RealmUtils.loadPreferencesFromDB().getNotificationCounter()); // todo : switch to a DB count
        tasks.setItemCount((int) RealmUtils.countUpcomingTasksInDB());
        joinRequests.setItemCount((int) RealmUtils.countObjectsInDB(GroupJoinRequest.class));
        itemAdapter.notifyDataSetChanged();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onGroupsRefreshedEvent(GroupsRefreshedEvent e) {
        refreshGroupCount();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onGroupAdded(GroupCreatedEvent e) {
        refreshGroupCount();
    }

    public void refreshGroupCount() {
        if (!primaryItemsActive) {
            primaryItemsActive = RealmUtils.loadPreferencesFromDB().isHasGroups();
            switchPrimaryItemsActiveState();
        }
        groups.setItemCount((int) RealmUtils.countObjectsInDB(Group.class));
        safeItemChange(NavigationConstants.HOME_NAV_GROUPS);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onNewNotificationEvent(NotificationCountChangedEvent event) {
        notifications.setItemCount(event.getNotificationCount());
        safeItemChange(NavigationConstants.HOME_NAV_NOTIFICATIONS);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onTaskCreatedEvent(TaskAddedEvent e) {
        tasks.incrementItemCount();
        safeItemChange(NavigationConstants.HOME_NAV_TASKS);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onTaskCancelledEvent(TaskCancelledEvent e) {
        tasks.decrementItemCount();
        safeItemChange(NavigationConstants.HOME_NAV_TASKS);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onTasksRefreshedEvent(TasksRefreshedEvent e) {
        if (TextUtils.isEmpty(e.parentUid)) {
            tasks.setItemCount((int) RealmUtils.countUpcomingTasksInDB());
            safeItemChange(NavigationConstants.HOME_NAV_TASKS);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onJoinRequestsChanged(JoinRequestEvent event) {
        joinRequests.setItemCount((int) RealmUtils.countObjectsInDB(GroupJoinRequest.class));
        safeItemChange(NavigationConstants.HOME_NAV_JOIN_REQUESTS);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(OfflineActionsSent e) {
        setupOnlineSwitch();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(OnlineOfflineToggledEvent e) {
        setupOnlineSwitch();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(NetworkFailureEvent e) { setupOnlineSwitch(); }

    private void safeItemChange(final int itemPosition) {
        if (itemAdapter != null) {
            try {
                itemAdapter.notifyItemChanged(itemPosition);
            } catch (IllegalStateException e) {
                Log.d(TAG, "recycler view called while animating");
            }
        }
    }

    private Intent createRateIntent() {
        final Uri uri = Uri.parse("market://details?id=" + ApplicationLoader.applicationContext.getPackageName());
        return new Intent(Intent.ACTION_VIEW, uri);
    }

    private boolean canRateApp() {
        return !getActivity().getPackageManager().queryIntentActivities(createRateIntent(), 0).isEmpty();
    }

}