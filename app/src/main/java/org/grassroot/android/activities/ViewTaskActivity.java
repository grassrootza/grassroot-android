package org.grassroot.android.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.RelativeLayout;

import org.grassroot.android.R;
import org.grassroot.android.events.NotificationEvent;
import org.grassroot.android.events.TaskCancelledEvent;
import org.grassroot.android.fragments.GiantMessageFragment;
import org.grassroot.android.fragments.ViewTaskFragment;
import org.grassroot.android.interfaces.GroupConstants;
import org.grassroot.android.interfaces.NavigationConstants;
import org.grassroot.android.interfaces.NotificationConstants;
import org.grassroot.android.models.Group;
import org.grassroot.android.models.PreferenceObject;
import org.grassroot.android.models.TaskModel;
import org.grassroot.android.services.NotificationUpdateService;
import org.grassroot.android.services.TaskService;
import org.grassroot.android.utils.RealmUtils;
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
    private String clickAction;
    private String notificationUid;
    private String messageBody;

    private Fragment fragment;

    @BindView(R.id.vta_root_layout) RelativeLayout rootView; // todo : switch to coordinator layout at some point maybe
    @BindView(R.id.vta_toolbar) Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_task);
        ButterKnife.bind(this);

        if (getIntent().getExtras() == null) {
            throw new UnsupportedOperationException("Error! View task activity started without arguments");
        }

        taskUid =  getIntent().getStringExtra(NotificationConstants.ENTITY_UID);
        taskType = getIntent().getStringExtra(NotificationConstants.ENTITY_TYPE);
        notificationUid = getIntent().getStringExtra(NotificationConstants.NOTIFICATION_UID);
        clickAction = getIntent().getStringExtra(NotificationConstants.CLICK_ACTION);
        messageBody = getIntent().getStringExtra(NotificationConstants.BODY);

        Log.e(TAG, "click action received : " + clickAction);

        if (TextUtils.isEmpty(taskUid) || TextUtils.isEmpty(taskType)) {
            throw new UnsupportedOperationException("Error! View task activity started with empty type or UID");
        }

        setUpToolbar();
        if (NotificationConstants.TASK_CANCELLED.equals(clickAction)) {
            fragment = createCancelFragment(taskUid);
        } else {
            TaskService.getInstance()
                .fetchAndStoreTask(taskUid, taskType, null).subscribe(); // done in background, so have it next time
            if (NotificationConstants.TASK_CHANGED.equals(clickAction)) {
                fragment = createChangedFragment();
            } else {
                fragment = ViewTaskFragment.newInstance(taskType, taskUid);
                showSnackBar();
            }
        }

        getSupportFragmentManager().beginTransaction()
                .add(R.id.vta_fragment_holder, fragment)
                .commitAllowingStateLoss();

        if (!TextUtils.isEmpty(notificationUid)) {
            processNotification();
        }
    }

    private Fragment createCancelFragment(final String taskUid) {;
        final Group parentGroup = getParentGroup();
        boolean canViewGroup = parentGroup != null;

        GiantMessageFragment fragment = GiantMessageFragment.newInstance(R.string.vt_cancel_header,
            messageBody, canViewGroup, true);

        if (canViewGroup) {
            fragment.setButtonOne(R.string.vt_cancel_group, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent viewGroup = new Intent(ViewTaskActivity.this, GroupTasksActivity.class);
                    viewGroup.putExtra(GroupConstants.OBJECT_FIELD, parentGroup);
                    startActivity(viewGroup);
                    finish();
                }
            });
        }

        fragment.setButtonTwo(R.string.vt_cancel_upcoming, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent viewTasks = new Intent(ViewTaskActivity.this, HomeScreenActivity.class);
                viewTasks.putExtra(NavigationConstants.HOME_OPEN_ON_NAV, NavigationConstants.ITEM_TASKS);
                startActivity(viewTasks);
                finish();
            }
        });
        RealmUtils.removeObjectFromDatabase(TaskModel.class, "taskUid", taskUid);
        EventBus.getDefault().post(new TaskCancelledEvent());
        setTitle(R.string.vt_cancel_title);
        return fragment;
    }

    private Fragment createChangedFragment() {
        final String messageBody = getIntent().getStringExtra(NotificationConstants.BODY);
        final Group parentGroup = getParentGroup();

        boolean canViewGroup = parentGroup != null;

        GiantMessageFragment fragment = GiantMessageFragment.newInstance(R.string.vt_changed_header,
            messageBody, true, canViewGroup);
        fragment.setButtonOne(R.string.vt_changed_task, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchToViewTaskFragment();
            }
        });

        if (canViewGroup) {
            fragment.setButtonTwo(R.string.vt_changed_group, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent viewGroup = new Intent(ViewTaskActivity.this, GroupTasksActivity.class);
                    viewGroup.putExtra(GroupConstants.OBJECT_FIELD, parentGroup);
                    startActivity(viewGroup);
                    finish();
                }
            });
        }
        return fragment;
    }

    private void switchToViewTaskFragment() {
        fragment = ViewTaskFragment.newInstance(taskType, taskUid);
        getSupportFragmentManager().popBackStack();
        getSupportFragmentManager().beginTransaction()
            .replace(R.id.vta_fragment_holder, fragment)
            .commitAllowingStateLoss();
    }

    private Group getParentGroup() {
        TaskModel task = RealmUtils.loadObjectFromDB(TaskModel.class, "taskUid", taskUid);
        return (task != null) ? RealmUtils.loadGroupFromDB(task.getParentUid()) : null;
    }

    private void showSnackBar() {
        if (!TextUtils.isEmpty(messageBody)) {
            final int colon = messageBody.indexOf(':');
            final int startPoint = colon == -1 ? 0 : colon;
            final String message = messageBody.substring(startPoint);
            Snackbar.make(rootView, message, Snackbar.LENGTH_SHORT);
        }
    }

    private void setUpToolbar() {
        setTitle(taskType);
        setSupportActionBar(toolbar);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        toolbar.setNavigationIcon(R.drawable.btn_close_white);
    }

    private void processNotification() {
        PreferenceObject preferenceObject = RealmUtils.loadPreferencesFromDB();
        int notificationCount = preferenceObject.getNotificationCounter();
        Log.e(TAG, "notification count currently: " + notificationCount);
        NotificationUpdateService.updateNotificationStatus(this, notificationUid);
        if (notificationCount > 0) {
            preferenceObject.setNotificationCounter(--notificationCount);
            RealmUtils.saveDataToRealmWithSubscriber(preferenceObject);
        }
        EventBus.getDefault().post(new NotificationEvent(--notificationCount));
    }

}
