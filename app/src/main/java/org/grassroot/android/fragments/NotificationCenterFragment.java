package org.grassroot.android.fragments;

import android.app.ProgressDialog;
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
import android.widget.RelativeLayout;

import org.grassroot.android.R;
import org.grassroot.android.activities.ViewTaskActivity;
import org.grassroot.android.adapters.NotificationAdapter;
import org.grassroot.android.adapters.RecyclerTouchListener;
import org.grassroot.android.events.NotificationEvent;
import org.grassroot.android.interfaces.ClickListener;
import org.grassroot.android.models.Notification;
import org.grassroot.android.models.NotificationList;
import org.grassroot.android.services.GcmListenerService;
import org.grassroot.android.services.GrassrootRestService;
import org.grassroot.android.services.NotificationUpdateService;
import org.grassroot.android.utils.Constant;
import org.grassroot.android.utils.ErrorUtils;
import org.grassroot.android.utils.PreferenceUtils;
import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

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
    private Integer pageSize = null;
    private NotificationAdapter notificationAdapter;
    private List<Notification> notifications = new ArrayList<>();
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
                        Notification notification = notificationAdapter.getNotifications().get(position);
                        updateNotificationStatus(notification);
                        Log.d(TAG, "clicked on item" + position + ", with message: " + notification.getMessage());

                        Intent openactivity = new Intent(getActivity(), ViewTaskActivity.class);
                        openactivity.putExtra(Constant.UID, notification.getEntityUid());
                        openactivity.putExtra(Constant.ENTITY_TYPE, notification.getEntityType());
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
        String phoneNumber = PreferenceUtils.getPhoneNumber();
        String code = PreferenceUtils.getAuthToken();

        progressDialog.show();

        GrassrootRestService.getInstance().getApi().getUserNotifications(phoneNumber, code, page, size).enqueue(new Callback<NotificationList>() {
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
                    notificationAdapter.notifyDataSetChanged();
                    isLoading = false;
                }

            }
            @Override
            public void onFailure(Call<NotificationList> call, Throwable t) {
                progressDialog.dismiss();
                ErrorUtils.handleNetworkError(getContext(), rlRootNc, t);
            }
        });
    }

    private void updateNotificationStatus(Notification notification) {
        if (!notification.isRead()) {
            String uid = notification.getUid();
            notification.setIsRead();
            notificationAdapter.notifyDataSetChanged();
            int notificationCount = PreferenceUtils.getNotificationCounter(getContext());
            Log.e(TAG, "notification count " + notificationCount);
            NotificationUpdateService.updateNotificationStatus(getContext(), uid);
            if(notificationCount >0){
            PreferenceUtils.setNotificationCounter(getContext(), --notificationCount);
            EventBus.getDefault().post(new NotificationEvent(--notificationCount));
        }}
    }
}
