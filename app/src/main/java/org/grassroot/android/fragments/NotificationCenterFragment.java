package org.grassroot.android.fragments;

import android.app.Notification;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import org.grassroot.android.R;
import org.grassroot.android.activities.ViewTaskActivity;
import org.grassroot.android.adapters.NotificationAdapter;
import org.grassroot.android.adapters.RecyclerTouchListener;
import org.grassroot.android.events.NotificationEvent;
import org.grassroot.android.interfaces.ClickListener;
import org.grassroot.android.interfaces.NotificationConstants;
import org.grassroot.android.models.NotificationList;
import org.grassroot.android.models.PreferenceObject;
import org.grassroot.android.models.TaskNotification;
import org.grassroot.android.services.GcmListenerService;
import org.grassroot.android.services.GrassrootRestService;
import org.grassroot.android.services.NotificationUpdateService;
import org.grassroot.android.utils.Constant;
import org.grassroot.android.utils.ErrorUtils;
import org.grassroot.android.utils.RealmUtils;
import org.grassroot.android.utils.Utilities;
import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.realm.Realm;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import rx.functions.Action1;

public class NotificationCenterFragment extends Fragment {

    private static final String TAG = NotificationCenterFragment.class.getSimpleName();

    @BindView(R.id.rc_nc)
    RecyclerView rcNc;
    @BindView(R.id.rl_root_nc)
    RelativeLayout rlRootNc;

    private ProgressDialog progressDialog;

    private LinearLayoutManager mLayoutManager;
    private Integer pageNumber = 0;
    private Integer totalPages = 0;
    private Integer pageSize = 100;
    private NotificationAdapter notificationAdapter;
    private List<TaskNotification> notifications = new ArrayList<>();
    private int firstVisibleItem, totalItemCount, lastVisibileItem;
    private boolean isLoading;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                Bundle savedInstanceState) {
        View viewToReturn = inflater.inflate(R.layout.activity_notification_center, container, false);
        ButterKnife.bind(this, viewToReturn);
        GcmListenerService.clearNotifications(getContext());
        setRecylerview();
        init();
        return viewToReturn;
    }

    private void init() {
        notificationAdapter = new NotificationAdapter();
        rcNc.setAdapter(notificationAdapter);
        progressDialog = new ProgressDialog(getActivity());
        progressDialog.setMessage(getString(R.string.txt_pls_wait));
        progressDialog.setIndeterminate(true);
        getNotifications(null, null);
    }

    private void setRecylerview() {

        rcNc.setHasFixedSize(false);
        mLayoutManager = new LinearLayoutManager(getActivity());
        rcNc.setLayoutManager(mLayoutManager);
        rcNc.setItemAnimator(new DefaultItemAnimator());

        rcNc.addOnItemTouchListener(new RecyclerTouchListener(getContext(), rcNc, new ClickListener() {
                    @Override
                    public void onClick(View view, int position) {
                        TaskNotification notification = notificationAdapter.getNotifications().get(position);
                        updateNotificationStatus(notification);
                        Log.d(TAG, "clicked on item" + position + ", with message: " + notification.getMessage());

                        Intent openactivity = new Intent(getActivity(), ViewTaskActivity.class);
                        openactivity.putExtra(NotificationConstants.ENTITY_UID, notification.getEntityUid());
                        openactivity.putExtra(NotificationConstants.ENTITY_TYPE, notification.getEntityType());
                        openactivity.putExtra(NotificationConstants.NOTIFICATION_UID, notification.getUid());
                        startActivity(openactivity);
                    }

                    @Override
                    public void onLongClick(View view, int position) {

                    }
                })

        );

        rcNc.addOnScrollListener(new RecyclerView.OnScrollListener() {

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                totalItemCount = mLayoutManager.getItemCount();
                firstVisibleItem = mLayoutManager.findFirstVisibleItemPosition();
                lastVisibileItem = mLayoutManager.findLastVisibleItemPosition();

                Log.e(TAG, "firstVisibleItem is " + firstVisibleItem);
                Log.e(TAG, "Remaining is " + (totalItemCount - firstVisibleItem));
                Log.e(TAG, "isLoading is " + isLoading);
                Log.e(TAG, "totalPages" + totalPages);
                Log.e(TAG, "pageNumber" + pageNumber);
                Log.e(TAG, "totalItemCount"+totalItemCount);

                if (pageNumber <totalPages &&  totalItemCount <= (lastVisibileItem + 10) && !isLoading) {
                    progressDialog.show();
                    if(pageNumber ==1 ){
                      pageNumber++;
                    }
                    isLoading = true;
                    getNotifications(pageNumber, pageSize);

                }

            }
        });
    }

    private void getNotifications(Integer page, Integer size) {
        final String phoneNumber = RealmUtils.loadPreferencesFromDB().getMobileNumber();
        final String code = RealmUtils.loadPreferencesFromDB().getToken();

        progressDialog.show();
        long lastTimeUpdated = RealmUtils.loadPreferencesFromDB().getLastTimeNotificationsFetched();
        Call<NotificationList> call = lastTimeUpdated == 0 ? GrassrootRestService.getInstance().getApi().getUserNotifications(phoneNumber, code, page, size) : GrassrootRestService.getInstance().getApi().getUserNotificationsChangedSince(phoneNumber, code,lastTimeUpdated);
        call.enqueue(new Callback<NotificationList>() {
            @Override
            public void onResponse(Call<NotificationList> call, Response<NotificationList> response) {
                if (response.isSuccessful()) {
                    progressDialog.dismiss();
                    notifications = response.body().getNotificationWrapper().getNotifications();
                    pageNumber = response.body().getNotificationWrapper().getPageNumber();
                    totalPages = response.body().getNotificationWrapper().getTotalPages();
                    rcNc.setVisibility(View.VISIBLE);
                    if (pageNumber > 1) {
                        notificationAdapter.updateData(notifications);
                    } else {
                        notificationAdapter.addData(notifications);
                    }
                    RealmUtils.saveDataToRealm(notifications).subscribe();
                    PreferenceObject preference = RealmUtils.loadPreferencesFromDB();
                    preference.setLastTimeNotificationsFetched(Utilities.getCurrentTimeInMillisAtUTC());
                    RealmUtils.saveDataToRealm(preference).subscribe();
                    isLoading = false;
                    notificationAdapter.notifyDataSetChanged();
                }

            }
            @Override
            public void onFailure(Call<NotificationList> call, Throwable t) {
                progressDialog.dismiss();
                notificationAdapter.addData(RealmUtils.loadListFromDB(TaskNotification.class));
                rcNc.setVisibility(View.VISIBLE);
                ErrorUtils.handleNetworkError(getContext(), rlRootNc, t);
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
            Log.e(TAG, "notification count " + notificationCount);
            NotificationUpdateService.updateNotificationStatus(getContext(), uid);
            if(notificationCount >0){
                PreferenceObject object = RealmUtils.loadPreferencesFromDB();
            object.setNotificationCounter(--notificationCount);
                RealmUtils.saveDataToRealm(object);
            EventBus.getDefault().post(new NotificationEvent(--notificationCount));
        }}
    }

    public void filterNotifications(String filterText) {
        if (TextUtils.isEmpty(filterText)) {
            notificationAdapter.resetToStored();
        } else {
            notificationAdapter.filter(filterText);
        }
    }
}
