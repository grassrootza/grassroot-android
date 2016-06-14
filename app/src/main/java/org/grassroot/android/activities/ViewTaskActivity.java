package org.grassroot.android.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;

import org.grassroot.android.R;
import org.grassroot.android.events.NotificationEvent;
import org.grassroot.android.fragments.ViewTaskFragment;
import org.grassroot.android.services.NotificationUpdateService;
import org.grassroot.android.utils.Constant;
import org.grassroot.android.utils.PreferenceUtils;
import org.greenrobot.eventbus.EventBus;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by luke on 2016/06/11.
 */
public class ViewTaskActivity extends PortraitActivity {

    public static final String TAG = ViewTaskActivity.class.getSimpleName();

    private String taskUid;
    private String taskType;
    private String notificationUid;
    private ViewTaskFragment fragment;

    @BindView(R.id.vta_toolbar)
    Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_task);
        ButterKnife.bind(this);


        if (getIntent().getExtras() == null) {
            throw new UnsupportedOperationException("Error! View task activity started without arguments");
        }

        taskUid = getIntent().getStringExtra(Constant.UID);
        taskType = getIntent().getStringExtra(Constant.ENTITY_TYPE);
        notificationUid = getIntent().getStringExtra(Constant.NOTIFICATION_UID);

        if (TextUtils.isEmpty(taskUid) || TextUtils.isEmpty(taskType)) {
            throw new UnsupportedOperationException("Error! View task activity started with empty type or UID");
        }

        setUpToolbar();
        fragment = ViewTaskFragment.newInstance(taskType, taskUid);
        getSupportFragmentManager().beginTransaction()
                .add(R.id.vta_fragment_holder, fragment)
                .commit();

        if (!TextUtils.isEmpty(notificationUid)) {
            processNotification();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Log.e(TAG, "new intent called");
    }

    private void setUpToolbar() {
        setTitle(taskType);
        setSupportActionBar(toolbar);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        toolbar.setNavigationIcon(R.drawable.btn_close_white);
    }

    private void processNotification() {
        int notificationCount = PreferenceUtils.getNotificationCounter(this);
        Log.e(TAG, "notification count currently: " + notificationCount);
        NotificationUpdateService.updateNotificationStatus(this, notificationUid);
        PreferenceUtils.setNotificationCounter(this, --notificationCount);
        EventBus.getDefault().post(new NotificationEvent(--notificationCount));
    }

}
