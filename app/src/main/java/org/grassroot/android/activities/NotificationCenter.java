package org.grassroot.android.activities;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.grassroot.android.R;
import org.grassroot.android.adapters.NotificationAdapter;
import org.grassroot.android.events.NotificationEvent;
import org.grassroot.android.interfaces.ClickListener;
import org.grassroot.android.interfaces.TaskConstants;
import org.grassroot.android.models.Notification;
import org.grassroot.android.models.NotificationList;
import org.grassroot.android.services.GcmListenerService;
import org.grassroot.android.services.GrassrootRestService;
import org.grassroot.android.services.NotificationUpdateService;
import org.grassroot.android.ui.views.RecyclerTouchListener;
import org.grassroot.android.utils.ErrorUtils;
import org.grassroot.android.utils.PreferenceUtils;
import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NotificationCenter extends PortraitActivity {

    private static final String TAG = "NotificationCenter";
    @BindView(R.id.tlb_nc)
    Toolbar tlbNc;
    @BindView(R.id.rc_nc)
    RecyclerView rcNc;
    @BindView(R.id.txt_tlb_nc)
    TextView txtTlbNc;
    @BindView(R.id.rl_root_nc)
    RelativeLayout rlRootNc;

    private ProgressDialog progressDialog;

    @BindView(R.id.prg_nc_paging)
    ProgressBar prgNcPaging;

    @BindView(R.id.error_layout)
    View errorLayout;
    @BindView(R.id.ll_no_result)
    LinearLayout llNoResult;
    @BindView(R.id.ll_no_internet)
    LinearLayout llNoInternet;
    @BindView(R.id.ll_server_error)
    LinearLayout llServerError;
    @BindView(R.id.ll_invalid_token)
    LinearLayout llInvalidToken;
    private LinearLayoutManager mLayoutManager;
    private Integer pageNumber = 0;
    private Integer totalPages = 0;
    private Integer pageSize = null;
    private NotificationAdapter notificationAdapter;
    private List<Notification> notifications = new ArrayList<>();
    private int firstVisibleItem, totalItemCount, lastVisibileItem;
    private boolean isLoading;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification_center);
        ButterKnife.bind(this);
        GcmListenerService.clearNotifications(this);
        setUpToolbar();
        setRecylerview();
        init();

    }

    private void init() {

        notificationAdapter = new NotificationAdapter();
        rcNc.setAdapter(notificationAdapter);
        getNotifications(null, null);
    }


    private void setUpToolbar() {
        tlbNc.setNavigationIcon(R.drawable.btn_back_wt);
        tlbNc.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
    }

    private void setRecylerview() {

        rcNc.setHasFixedSize(false);
        mLayoutManager = new LinearLayoutManager(getApplicationContext());
        rcNc.setLayoutManager(mLayoutManager);
        rcNc.setItemAnimator(new DefaultItemAnimator());

        rcNc.addOnItemTouchListener(new RecyclerTouchListener(NotificationCenter.this, rcNc, new ClickListener() {
                    @Override
                    public void onClick(View view, int position) {
                        Notification notification = notificationAdapter.getNotifications().get(position);
                        updateNotificationStatus(notification);
                        Log.d(TAG, "clicked on item" + position + ", with message: " + notification.getMessage());

                        Intent openactivity = new Intent(NotificationCenter.this, ViewTaskActivity.class);
                        openactivity.putExtra(TaskConstants.TASK_UID_FIELD, notification.getEntityUid());
                        openactivity.putExtra(TaskConstants.TASK_TYPE_FIELD, notification.getEntityType());
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
                    prgNcPaging.setVisibility(View.VISIBLE);
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
        String phoneNumber = PreferenceUtils.getuser_mobilenumber(this);
        String code = PreferenceUtils.getuser_token(this);
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage(getString(R.string.prg_message));
        progressDialog.show();
        GrassrootRestService.getInstance().getApi().getUserNotifications(phoneNumber, code, page, size).enqueue(new Callback<NotificationList>() {
            @Override
            public void onResponse(Call<NotificationList> call, Response<NotificationList> response) {
                if (response.isSuccessful()) {

                    hideProgess();
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
                hideProgess();
                ErrorUtils.handleNetworkError(NotificationCenter.this, errorLayout, t);
            }
        });

    }

    private void hideProgess(){
        progressDialog.hide();
        prgNcPaging.setVisibility(View.GONE);
    }


    @OnClick(R.id.error_layout)
    public void onClick(View v) {
        if (v == llNoInternet || v == llNoResult || v == llNoResult)
            getNotifications(null, null);

    }

    private void updateNotificationStatus(Notification notification) {
        if (!notification.isRead()) {
            String uid = notification.getUid();
            notification.setIsRead();
            notificationAdapter.notifyDataSetChanged();
            int notificationCount = PreferenceUtils.getIsNotificationcounter(this);
            NotificationUpdateService.updateNotificationStatus(this, uid);
            if(notificationCount >0){
            PreferenceUtils.setIsNotificationcounter(this, --notificationCount);
            EventBus.getDefault().post(new NotificationEvent(--notificationCount));
        }}
    }
}
