package org.grassroot.android.fragments;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import org.grassroot.android.R;
import org.grassroot.android.activities.ViewTaskActivity;
import org.grassroot.android.adapters.NotificationAdapter;
import org.grassroot.android.adapters.RecyclerTouchListener;
import org.grassroot.android.events.NotificationCountChangedEvent;
import org.grassroot.android.fragments.dialogs.NetworkErrorDialogFragment;
import org.grassroot.android.interfaces.ClickListener;
import org.grassroot.android.interfaces.NotificationConstants;
import org.grassroot.android.models.PreferenceObject;
import org.grassroot.android.models.TaskNotification;
import org.grassroot.android.models.responses.NotificationList;
import org.grassroot.android.services.GcmListenerService;
import org.grassroot.android.services.GrassrootRestService;
import org.grassroot.android.services.NotificationUpdateService;
import org.grassroot.android.services.SharingService;
import org.grassroot.android.utils.ErrorUtils;
import org.grassroot.android.utils.NetworkUtils;
import org.grassroot.android.utils.RealmUtils;
import org.grassroot.android.utils.Utilities;
import org.grassroot.android.utils.rxutils.SingleObserverFromConsumer;
import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NotificationCenterFragment extends Fragment {

    private static final String TAG = NotificationCenterFragment.class.getSimpleName();

    private NotificationAdapter notificationAdapter;
    private LinearLayoutManager viewLayoutManager;

    Unbinder unbinder;
    @BindView(R.id.notifications_root_view) ViewGroup rootView;
    @BindView(R.id.notification_recycler_view) RecyclerView recyclerView;
    @BindView(R.id.progressBar) ProgressBar progressBar;

    private int currentPage = 0;
    private int totalPages = 10; // just to init to a non-zero value while get screens
    final private int pageSize = 20;

    private List<TaskNotification> notifications = new ArrayList<>();

    private Set<String> notificationsToUpdate;
    private Set<Integer> positionsRead;

    private int itemsLaidOutSoFar, lastVisibileItem;
    private int cacheStoredFirstVisible, cacheStoredLastVisible;
    private boolean isLoading;
    private boolean isFiltering = false; // to prevent further calls if sort or filter is empty
    private boolean hasSavedInstanceState = false;

    private CharSequence[] sharingOptions;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View viewToReturn = inflater.inflate(R.layout.fragment_notification_center, container, false);
        unbinder = ButterKnife.bind(this, viewToReturn);
        setHasOptionsMenu(true);
        GcmListenerService.clearTaskNotifications(getContext()); // clears notifications in tray
        setUpRecyclerView();
        return viewToReturn;
    }

    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }

    @Override
    public void onResume() {
        super.onResume();
        sharingOptions = SharingService.itemsForMultiChoice(); // in case user navigated away to change prefs / install app etc
        hasSavedInstanceState = false;
    }

    @Override
    public void onStart() {
        super.onStart();
        hasSavedInstanceState = false;
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        if (menu.findItem(R.id.mi_icon_sort) != null)
            menu.findItem(R.id.mi_icon_sort).setVisible(false);
        if (menu.findItem(R.id.mi_icon_filter) != null)
            menu.findItem(R.id.mi_icon_filter).setVisible(false);
        if (menu.findItem(R.id.mi_share_default) != null)
            menu.findItem(R.id.mi_share_default).setVisible(false);
        if (menu.findItem(R.id.mi_only_unread) != null)
            menu.findItem(R.id.mi_only_unread).setVisible(true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.mi_refresh_screen:
                refreshNotificationList();
                return true;
            case R.id.mi_only_unread:
                if (!isFiltering) {
                    item.setTitle(R.string.menu_all_notis);
                    isFiltering = true;
                    notificationAdapter.filterByUnviewed().subscribe(new Consumer<Boolean>() {
                        @Override
                        public void accept(@NonNull Boolean aBoolean) {
                            Log.e(TAG, "resetting adapter ...");
                            notificationAdapter.notifyDataSetChanged();
                        }
                    }, new Consumer<Throwable>() {
                        @Override
                        public void accept(@NonNull Throwable throwable) {
                            throwable.printStackTrace();
                        }
                    });
                } else {
                    notificationAdapter.resetToStored();
                    item.setTitle(R.string.menu_unread);
                    isFiltering = false;
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void setUpRecyclerView() {

        notificationAdapter = new NotificationAdapter();
        recyclerView.setAdapter(notificationAdapter);
        recyclerView.setHasFixedSize(false);
        viewLayoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(viewLayoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());

        cacheStoredFirstVisible = -1;
        cacheStoredLastVisible = -1;

        notificationsToUpdate = new HashSet<>();
        positionsRead = new HashSet<>();

        getNotifications(0, pageSize);
        sharingOptions = SharingService.itemsForMultiChoice();

        recyclerView.addOnItemTouchListener(new RecyclerTouchListener(getContext(), recyclerView,
            new ClickListener() {
                @Override
                public void onClick(View view, int position) {
                    TaskNotification notification = notificationAdapter.getNotifications().get(position);
                    updateNotificationStatus(notification);

                    Intent openactivity = new Intent(getActivity(), ViewTaskActivity.class);
                    openactivity.putExtra(NotificationConstants.ENTITY_UID, notification.getEntityUid());
                    openactivity.putExtra(NotificationConstants.ENTITY_TYPE, notification.getEntityType());
                    openactivity.putExtra(NotificationConstants.NOTIFICATION_UID, notification.getUid());
                    startActivity(openactivity);
                }

                @Override
                public void onLongClick(View view, int position) {
                    final TaskNotification notification = notificationAdapter.getNotifications().get(position);
                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    builder.setTitle(R.string.share_title)
                        .setItems(sharingOptions, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            handleNotificationSharing(notification, which);
                        }
                    });
                    builder.create().show();
                }
            })
        );

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                itemsLaidOutSoFar = viewLayoutManager.getItemCount();
                lastVisibileItem = viewLayoutManager.findLastVisibleItemPosition();

                int firstCompletelyVisibleItem = viewLayoutManager.findFirstCompletelyVisibleItemPosition();
                int lastCompletelyVisibleItem = viewLayoutManager.findLastCompletelyVisibleItemPosition();

                handleNotificationUpdating(firstCompletelyVisibleItem, lastCompletelyVisibleItem);

                if (currentPage < totalPages && itemsLaidOutSoFar <= (lastVisibileItem + 10) && !isLoading) {
                    Log.d(TAG, "fetching more notifications ... current page = " + currentPage);
                    progressBar.setVisibility(View.VISIBLE);
                    currentPage++;
                    isLoading = true;
                    getNotifications(currentPage, pageSize);
                }
            }
        });
    }

    private void refreshNotificationList() {
        getNotifications(0, pageSize);
    }

    private void handleNotificationSharing(final TaskNotification notification, final int optionSelected) {
        final String sharePackage = SharingService.sharePackageFromItemSelected(optionSelected);
        Intent i = new Intent(getActivity(), SharingService.class);
        i.putExtra(SharingService.MESSAGE, String.format(getString(R.string.share_notification_format),
            notification.getTitle(), notification.getMessage()));
        i.putExtra(SharingService.APP_SHARE_TAG, sharePackage);
        i.putExtra(SharingService.ACTION_TYPE, SharingService.TYPE_SHARE);
        getActivity().startService(i);
    }

    // note : these following two we do on the main thread because changes can be very fast and might end
    // up with conflicts on background, plus they are _very_ simple / fast int & boolean operations
    // as soon as we are starting to process, though, we shift to background thread ...
    private void handleNotificationUpdating(final int firstCompletelyVisibleItem, final int lastCompletelyVisibleItem) {
        Log.v(TAG, String.format("handling notification updating : %d to %d", firstCompletelyVisibleItem, lastCompletelyVisibleItem));

        final boolean firstItemChanged = firstCompletelyVisibleItem != cacheStoredFirstVisible &&
            firstCompletelyVisibleItem != -1; // in case no items after filter
        final boolean lastItemChanged = lastCompletelyVisibleItem != cacheStoredLastVisible &&
            lastCompletelyVisibleItem != -1; // in case no items after filter

        if (firstItemChanged || lastItemChanged) {
            if (firstItemChanged && lastItemChanged) {
                handleAddingNotificationToBatchForUpdate(firstCompletelyVisibleItem, lastCompletelyVisibleItem);
                cacheStoredFirstVisible = firstCompletelyVisibleItem;
                cacheStoredLastVisible = lastCompletelyVisibleItem;
            } else if (firstItemChanged) {
                handleAddingNotificationToBatchForUpdate(firstCompletelyVisibleItem, firstCompletelyVisibleItem);
                cacheStoredFirstVisible = firstCompletelyVisibleItem;
            } else { // i.e., last item changed only
                handleAddingNotificationToBatchForUpdate(lastCompletelyVisibleItem, lastCompletelyVisibleItem);
                cacheStoredLastVisible = lastCompletelyVisibleItem;
            }
        }
    }

    private void handleAddingNotificationToBatchForUpdate(final int positionStart, final int positionEnd) {
        Log.v(TAG, String.format("handle adding notification ... %1d to %2d", positionStart, positionEnd));
        updateNotificationToRead(positionStart, positionEnd).subscribe(new Consumer<Integer>() {
            @Override
            public void accept(@NonNull Integer count) {
                PreferenceObject prefs = RealmUtils.loadPreferencesFromDB();
                int revisedCounter = Math.max(0, prefs.getNotificationCounter() - count);
                prefs.setNotificationCounter(revisedCounter);
                EventBus.getDefault().post(new NotificationCountChangedEvent(revisedCounter));
                RealmUtils.saveDataToRealm(prefs).subscribe();
                RealmUtils.saveDataToRealm(prefs).subscribe();
            }
        });
    }

    // returns how many were changed from not-viewed to viewed
    private Observable<Integer> updateNotificationToRead(final int positionStart, final int positionEnd) {
        return Observable.create(new ObservableOnSubscribe<Integer>() {
            @Override
            public void subscribe(ObservableEmitter<Integer> subscriber) {
                 // Log.e(TAG, String.format("entering the update batch ... %1d to %2d", positionStart, positionEnd));
                int changedToViewCounter = 0;
                for (int i = positionStart; i <= positionEnd; i++) {
                    if (!positionsRead.contains(i)) {
                        positionsRead.add(i);
                        TaskNotification notification = notificationAdapter.getItem(i);
                        notificationsToUpdate.add(notification.getUid());
                        if (!notification.isViewedAndroid()) {
                            changedToViewCounter++;
                            notification.setRead(true);
                            notification.setViewedAndroid(true); // don't call update on adapter, because don't want change to be instant (on next scroll / view)
                            notification.setToChangeOnServer(true);
                            RealmUtils.saveDataToRealm(notification).subscribe();
                        }
                    }
                }
                subscriber.onNext(changedToViewCounter);
                subscriber.onComplete();
            }
        }).subscribeOn(Schedulers.computation()).observeOn(AndroidSchedulers.mainThread());
    }

    // note : since we are using a hash set there is not duplication issue here, and little
    // performance hit on server, but with reliability on background server http call in doubt, etc.
    // we don't clear the set (in case user comes back, something went wrong in backend, so we still
    // have state of the notifications being view, but to reconsider this trade-off in future

    @Override
    public void onPause() {
        super.onPause();
        Intent intent = new Intent(getActivity(), NotificationUpdateService.class);
        intent.putExtra(NotificationUpdateService.ACTION_FIELD, NotificationUpdateService.UPDATE_BATCH);
        intent.putExtra(NotificationUpdateService.UIDS_SET, new ArrayList<>(notificationsToUpdate));
        getActivity().startService(intent);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        hasSavedInstanceState = true;
    }

    private void getNotifications(final int page, final int size) {
        final String phoneNumber = RealmUtils.loadPreferencesFromDB().getMobileNumber();
        final String code = RealmUtils.loadPreferencesFromDB().getToken();

        progressBar.setVisibility(View.VISIBLE);

        long lastTimeUpdated = RealmUtils.loadPreferencesFromDB().getLastTimeNotificationsFetched();

        Call<NotificationList> call;
        if (page == 0) {
            call = (lastTimeUpdated == 0) ?
                GrassrootRestService.getInstance().getApi().getUserNotifications(phoneNumber, code, page, size) :
                GrassrootRestService.getInstance().getApi().getUserNotificationsChangedSince(phoneNumber, code,lastTimeUpdated);
        } else {
            call = GrassrootRestService.getInstance().getApi().getUserNotifications(phoneNumber, code, page, size);
        }

        // receiving crash reports of null pointer errors in here, hence adding the checks
        call.enqueue(new Callback<NotificationList>() {
            @Override
            public void onResponse(Call<NotificationList> call, Response<NotificationList> response) {
                if (response.isSuccessful()) {
                    if (progressBar != null) {
                        progressBar.setVisibility(View.GONE);
                    }
                    notifications = response.body().getNotificationWrapper().getNotifications();
                    currentPage = response.body().getNotificationWrapper().getPageNumber();
                    totalPages = response.body().getNotificationWrapper().getTotalPages();

                    Log.d(TAG, "fetched the lists, total pages = " + totalPages);

                    if (recyclerView != null) {
                        recyclerView.setVisibility(View.VISIBLE);
                        if (currentPage > 1) {
                            notificationAdapter.addNotifications(notifications);
                        } else {
                            notificationAdapter.setToNotifications(notifications);
                        }
                        notificationAdapter.notifyDataSetChanged();
                    }

                    RealmUtils.saveNotificationsToRealm(notifications).subscribe();
                    PreferenceObject preference = RealmUtils.loadPreferencesFromDB();
                    preference.setLastTimeNotificationsFetched(Utilities.getCurrentTimeInMillisAtUTC());
                    RealmUtils.saveDataToRealm(preference).subscribe();
                    isLoading = false;
                } else {
                    if (progressBar != null) {
                        progressBar.setVisibility(View.GONE);
                        final String errorMessage = ErrorUtils.serverErrorText(response.errorBody());
                        Snackbar.make(recyclerView, errorMessage, Snackbar.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<NotificationList> call, Throwable t) {
                // as usual, have to make sure the call back doesn't trigger a null
                if (progressBar != null && !hasSavedInstanceState) {
                    progressBar.setVisibility(View.GONE);
                    notificationAdapter.setToNotifications(RealmUtils.loadNotificationsSorted());
                    recyclerView.setVisibility(View.VISIBLE);
                    NetworkErrorDialogFragment.newInstance(R.string.connect_error_notifications,
                        progressBar, new SingleObserverFromConsumer<>(new Consumer<String>() {
                            @Override
                            public void accept(@NonNull String s) {
                                progressBar.setVisibility(View.GONE);
                                if (s.equals(NetworkUtils.CONNECT_ERROR)) {
                                    Snackbar.make(recyclerView, R.string.connect_error_failed_retry, Snackbar.LENGTH_SHORT).show();
                                } else {
                                    getNotifications(page, size);
                                }
                            }
                        })).show(getFragmentManager(), "dialog");
                }
            }
        });
    }

    private void updateNotificationStatus(TaskNotification notification) {
        if (!notification.isRead()) {
            String uid = notification.getUid();
            notification.setIsRead();
            notificationAdapter.notifyDataSetChanged();
            RealmUtils.saveDataToRealm(notification).subscribe();
            int notificationCount = RealmUtils.loadPreferencesFromDB().getNotificationCounter();
            NotificationUpdateService.updateNotificationStatus(getContext(), uid);
            if(notificationCount >0){
                PreferenceObject object = RealmUtils.loadPreferencesFromDB();
            object.setNotificationCounter(--notificationCount);
                RealmUtils.saveDataToRealm(object);
            EventBus.getDefault().post(new NotificationCountChangedEvent(--notificationCount));
        }}
    }

    public void searchNotifications(String queryText) {
        if (TextUtils.isEmpty(queryText)) {
            notificationAdapter.resetToStored();
        } else {
            notificationAdapter.searchText(queryText)
                .subscribe(new Consumer<Boolean>() {
                @Override
                public void accept(@NonNull Boolean aBoolean) {
                    notificationAdapter.notifyDataSetChanged();
                }
            }, new Consumer<Throwable>() {
                @Override
                public void accept(@NonNull Throwable throwable) {
                    throwable.printStackTrace();
                }
            });
        }
    }


}
