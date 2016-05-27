package com.techmorphosis.grassroot.ui.activities;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.techmorphosis.grassroot.R;
import com.techmorphosis.grassroot.adapters.NotificationAdapter;
import com.techmorphosis.grassroot.interfaces.ClickListener;
import com.techmorphosis.grassroot.services.GrassrootRestService;
import com.techmorphosis.grassroot.services.model.Notification;
import com.techmorphosis.grassroot.services.model.NotificationList;
import com.techmorphosis.grassroot.ui.views.RecyclerTouchListener;
import com.techmorphosis.grassroot.utils.ErrorUtils;
import com.techmorphosis.grassroot.ui.views.ProgressBarCircularIndeterminate;
import com.techmorphosis.grassroot.utils.PreferenceUtils;

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
    @BindView(R.id.prg_nc)
    ProgressBarCircularIndeterminate prgNc;
    @BindView(R.id.txt_prg_nc)
    TextView txtPrgNc;
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
    private Snackbar snackbar;
    private Integer pageNumber = 0;
    private Integer totalPages = 1;
    private Integer pageSize = 30;
    private GrassrootRestService grassrootRestService;
    private NotificationAdapter notificationAdapter;
    private List<Notification> notifications = new ArrayList<>();
    private int firstVisibleItem, totalItemCount;
    private int ItemLeftCount;
    private boolean isLoading;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification_center);
        ButterKnife.bind(this);
        setUpToolbar();
        setRecylerview();
        init();

    }

    private void init() {
        grassrootRestService = new GrassrootRestService(this);
        notificationAdapter = new NotificationAdapter(this);
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
                Notification notification = notifications.get(position);
                Log.d(TAG, notification.getMessage());
                Intent openactivity = null;
                switch (notification.getEntityType().toLowerCase()) {
                    case "vote":
                        openactivity = new Intent(NotificationCenter.this, ViewVote.class);
                        openactivity.putExtra("id", notification.getEntityUid());
                        break;
                    case "meeting":
                        openactivity = new Intent(NotificationCenter.this, NotBuiltActivity.class);
                        openactivity.putExtra("title", "Meeting");
                        break;
                    case "todo":
                        openactivity = new Intent(NotificationCenter.this, NotBuiltActivity.class);
                        openactivity.putExtra("title", "ToDo");
                        break;
                }
                startActivity(openactivity);

            }

            @Override
            public void onLongClick(View view, int position) {

            }
        }));

        rcNc.addOnScrollListener(new RecyclerView.OnScrollListener() {

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                totalItemCount = mLayoutManager.getItemCount();
                firstVisibleItem = mLayoutManager.findFirstVisibleItemPosition();
                ItemLeftCount = (totalItemCount - firstVisibleItem);

                Log.e(TAG, "firstVisibleItem is " + firstVisibleItem);
                Log.e(TAG, "Remaining is " + (totalItemCount - firstVisibleItem));
                Log.e(TAG, "isLoading is " + isLoading);
                Log.e(TAG, "totalPages" + totalPages);
                Log.e(TAG, "pageNumber" + pageNumber);


                if (pageNumber < totalPages && ItemLeftCount <= 10 && !isLoading) {
                    prgNcPaging.setVisibility(View.VISIBLE);
                    pageNumber = pageNumber++;
                    isLoading = true;
                    getNotifications(pageNumber, pageSize);

                }


            }
        });
    }

    private void getNotifications(Integer page, Integer size) {

        String phoneNumber = PreferenceUtils.getuser_mobilenumber(this);
        String code = PreferenceUtils.getuser_token(this);
        prgNc.setVisibility(View.VISIBLE);
        grassrootRestService.getApi().getUserNotifications(phoneNumber, code, page, size).enqueue(new Callback<NotificationList>() {
            @Override
            public void onResponse(Call<NotificationList> call, Response<NotificationList> response) {
                if (response.isSuccessful()) {
                    notifications = response.body().getNotificationWrapper().getNotifications();
                    pageNumber = response.body().getNotificationWrapper().getPageNumber();
                    totalPages = response.body().getNotificationWrapper().getTotalPages();
                    prgNc.setVisibility(View.GONE);
                    txtPrgNc.setVisibility(View.GONE);
                    prgNcPaging.setVisibility(View.GONE);

                    rcNc.setVisibility(View.VISIBLE);
                    if (pageNumber > 1) {
                        notificationAdapter.updateData(notifications);
                        notificationAdapter.notifyDataSetChanged();;
                        isLoading = false;

                    } else {
                        notificationAdapter.addData(notifications);

                    }

                }

            }

            @Override
            public void onFailure(Call<NotificationList> call, Throwable t) {
                prgNcPaging.setVisibility(View.INVISIBLE);
                ErrorUtils.handleNetworkError(NotificationCenter.this, errorLayout, t);
            }
        });


    }

    public void showSnackbar(String message, int length, String actionbuttontxt) {
        snackbar = Snackbar.make(rlRootNc, message, length);
        snackbar.setActionTextColor(Color.RED);
        if (!TextUtils.isEmpty(actionbuttontxt)) {
            snackbar.setAction(actionbuttontxt, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    getNotifications(0, 0);
                }
            });

        }

        snackbar.show();
    }

    @OnClick(R.id.error_layout)
    public void onClick(View v) {
        if (v == llNoInternet || v == llNoResult || v == llNoResult)
            getNotifications(null, null);

    }
}
