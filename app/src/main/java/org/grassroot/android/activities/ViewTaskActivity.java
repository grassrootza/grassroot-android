package org.grassroot.android.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import org.grassroot.android.R;
import org.grassroot.android.events.NotificationCountChangedEvent;
import org.grassroot.android.events.TaskCancelledEvent;
import org.grassroot.android.fragments.GiantMessageFragment;
import org.grassroot.android.fragments.ImageGridFragment;
import org.grassroot.android.fragments.ViewTaskFragment;
import org.grassroot.android.interfaces.GroupConstants;
import org.grassroot.android.interfaces.NavigationConstants;
import org.grassroot.android.interfaces.NotificationConstants;
import org.grassroot.android.models.Group;
import org.grassroot.android.models.PreferenceObject;
import org.grassroot.android.models.TaskModel;
import org.grassroot.android.services.NotificationUpdateService;
import org.grassroot.android.services.TaskService;
import org.grassroot.android.utils.ErrorUtils;
import org.grassroot.android.utils.RealmUtils;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

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
    private boolean showShareMenu;

    private boolean isCancelNotification;

    @BindView(R.id.vta_toolbar) Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_task);
        ButterKnife.bind(this);

        EventBus.getDefault().register(this);

        if (getIntent().getExtras() == null) {
            Log.e(TAG, "Error! View task activity started without arguments");
            startActivity(ErrorUtils.gracefulExitToTasks(this));
        }

        taskUid =  getIntent().getStringExtra(NotificationConstants.ENTITY_UID);
        taskType = getIntent().getStringExtra(NotificationConstants.ENTITY_TYPE);
        notificationUid = getIntent().getStringExtra(NotificationConstants.NOTIFICATION_UID);
        clickAction = getIntent().getStringExtra(NotificationConstants.CLICK_ACTION);
        messageBody = getIntent().getStringExtra(NotificationConstants.BODY);

        Log.d(TAG, "click action received : " + clickAction);

        if (TextUtils.isEmpty(taskUid) || TextUtils.isEmpty(taskType)) {
            Log.e(TAG, "Error! View task activity started with empty type or UID");
            startActivity(ErrorUtils.gracefulExitToTasks(this));
        }

        setUpToolbar();

        if (NotificationConstants.TASK_CANCELLED.equals(clickAction)) {
            // requires deleting & removing, hence handled differently to rest
            isCancelNotification = true;
            fragment = createCancelFragment(taskUid);
        } else {
            isCancelNotification = false;
            TaskService.getInstance()
                .fetchAndStoreTask(taskUid, taskType, null)
                .subscribe(); // done in background, so have it next time

            if (TextUtils.isEmpty(clickAction)) {
                fragment = ViewTaskFragment.newInstance(taskType, taskUid);
                showShareMenu = true;
            } else {
                // todo : possibly add share option to all / most of these ...
                switch (clickAction) {
                    case NotificationConstants.TASK_CHANGED:
                        fragment = createMessageFragment(R.string.vt_changed_header);
                        break;
                    case NotificationConstants.TASK_REMINDER:
                        fragment = createMessageFragment(R.string.vt_reminder_header);
                        break;
                    case NotificationConstants.TASK_RESULTS:
                        fragment = createMessageFragment(R.string.vt_results_header);
                        break;
                    default:
                        fragment = ViewTaskFragment.newInstance(taskType, taskUid);
                        showShareMenu = true;
                }
            }

        }

        getSupportFragmentManager().beginTransaction()
                .add(R.id.vta_fragment_holder, fragment)
                .commit();

        if (!TextUtils.isEmpty(notificationUid)) {
            processNotification();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // note : watch & debug all this, as sequence of onCreate and this method is unpredictable ...
        if (clickAction == null) {
            clickAction = getIntent().getStringExtra(NotificationConstants.CLICK_ACTION);
        }

        if (showShareMenu || NotificationConstants.VIEW_TASK.equals(clickAction)) {
            MenuInflater inflater = getMenuInflater();
            inflater.inflate(R.menu.menu_share_task, menu);
            return true;
        } else {
            return super.onCreateOptionsMenu(menu);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                if (!checkForImageFragment()) {
                    finish();
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private boolean checkForImageFragment() {
        Fragment imageFrag = getSupportFragmentManager().findFragmentByTag(ImageGridFragment.class.getCanonicalName());
        if (imageFrag != null && imageFrag.isVisible()) {
            getSupportFragmentManager().beginTransaction()
                    .remove(imageFrag)
                    .commit();
            return true;
        } else {
            return false;
        }
    }

    @Override
    protected void onDestroy() {
        EventBus.getDefault().unregister(this);
        super.onDestroy();
    }

    private Fragment createCancelFragment(final String taskUid) {;
        final Group parentGroup = getParentGroup();
        boolean canViewGroup = parentGroup != null;

        GiantMessageFragment.Builder builder = new GiantMessageFragment.Builder(R.string.vt_cancel_header)
            .setBody(messageBody);

        if (canViewGroup) {
            builder.setButtonOne(R.string.vt_cancel_group, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent viewGroup = new Intent(ViewTaskActivity.this, GroupTasksActivity.class);
                    viewGroup.putExtra(GroupConstants.OBJECT_FIELD, parentGroup);
                    startActivity(viewGroup);
                    finish();
                }
            });
        }

        builder.setButtonTwo(R.string.vt_cancel_upcoming, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent viewTasks = new Intent(ViewTaskActivity.this, HomeScreenActivity.class);
                viewTasks.putExtra(NavigationConstants.HOME_OPEN_ON_NAV, NavigationConstants.ITEM_TASKS);
                startActivity(viewTasks);
                finish();
            }
        });

        RealmUtils.removeObjectFromDatabase(TaskModel.class, "taskUid", taskUid);
        EventBus.getDefault().post(new TaskCancelledEvent(taskUid));
        setTitle(R.string.vt_cancel_title);

        return builder.build();
    }

    private Fragment createMessageFragment(final int headerResource) {
        final String messageBody = getIntent().getStringExtra(NotificationConstants.BODY);
        final Group parentGroup = getParentGroup();

        boolean canViewGroup = parentGroup != null;

        GiantMessageFragment.Builder builder = new GiantMessageFragment.Builder(headerResource)
            .setBody(messageBody)
            .setButtonOne(R.string.vt_changed_task, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchToViewTaskFragment();
            }
        });

        if (canViewGroup) {
            builder.setButtonTwo(R.string.vt_changed_group, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent viewGroup = new Intent(ViewTaskActivity.this, GroupTasksActivity.class);
                    viewGroup.putExtra(GroupConstants.OBJECT_FIELD, parentGroup);
                    startActivity(viewGroup);
                    finish();
                }
            });
        }

        return builder.build();
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

    private void setUpToolbar() {
        setTitle(taskType);
        setSupportActionBar(toolbar);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        toolbar.setNavigationIcon(R.drawable.btn_close_white);
    }

    private void processNotification() {
        PreferenceObject preferenceObject = RealmUtils.loadPreferencesFromDB();
        preferenceObject.decrementNotificationCounter();
        EventBus.getDefault().post(new NotificationCountChangedEvent(preferenceObject.getNotificationCounter()));
        RealmUtils.saveDataToRealmWithSubscriber(preferenceObject);

        Intent intent = new Intent(this, NotificationUpdateService.class);
        intent.putExtra(NotificationConstants.NOTIFICATION_UID, notificationUid);
        startService(intent);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(TaskCancelledEvent e) {
        if (!isCancelNotification && e.getTaskUid().equals(taskUid)) {
            finish();
        }
    }

}
