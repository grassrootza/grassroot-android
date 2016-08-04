package org.grassroot.android.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.NavUtils;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import org.grassroot.android.R;
import org.grassroot.android.events.TaskCancelledEvent;
import org.grassroot.android.fragments.JoinCodeFragment;
import org.grassroot.android.fragments.NewTaskMenuFragment;
import org.grassroot.android.fragments.TaskListFragment;
import org.grassroot.android.fragments.ViewTaskFragment;
import org.grassroot.android.interfaces.GroupConstants;
import org.grassroot.android.models.Group;
import org.grassroot.android.utils.Constant;
import org.grassroot.android.utils.ErrorUtils;
import org.grassroot.android.utils.IntentUtils;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class GroupTasksActivity extends PortraitActivity implements NewTaskMenuFragment.NewTaskMenuListener, JoinCodeFragment.JoinCodeListener, TaskListFragment.TaskListListener {

    private static final String TAG = GroupTasksActivity.class.getCanonicalName();

    private Group groupMembership;
    private boolean canCreateTask;
    private TaskListFragment taskListFragment;
    private NewTaskMenuFragment newTaskMenuFragment;
    private JoinCodeFragment joinCodeFragment;

    private Menu thisMenu;

    @BindView(R.id.gta_toolbar)
    Toolbar toolbar;
    @BindView(R.id.gta_fab)
    FloatingActionButton actionButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_tasks);
        ButterKnife.bind(this);
        EventBus.getDefault().register(this);

        final Bundle extras = getIntent().getExtras();

        if (extras == null) {
            Log.e(TAG, "Error! Group tasks activity called without group passed");
            startActivity(ErrorUtils.gracefulExitToHome(this));
            finish();
            return;
        }

        groupMembership = extras.getParcelable(GroupConstants.OBJECT_FIELD);
        if (groupMembership == null) {
            Log.e(TAG, "Error! Group tasks activity called without group passed");
            startActivity(ErrorUtils.gracefulExitToHome(this));
            finish();
            return;
        }

        newTaskMenuFragment = NewTaskMenuFragment.newInstance(groupMembership, true);
        canCreateTask = groupMembership.canCallMeeting() || groupMembership.canCallVote() || groupMembership.canCreateTodo();

        setUpViews();
        setUpFragment();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.mi_view_join_code).setVisible(groupMembership.hasJoinCode());
        menu.findItem(R.id.mi_new_task).setVisible(groupMembership.hasCreatePermissions());
        menu.findItem(R.id.mi_add_members).setVisible(groupMembership.canAddMembers());
        menu.findItem(R.id.mi_remove_members).setVisible(groupMembership.canDeleteMembers());
        menu.findItem(R.id.mi_view_members).setVisible(groupMembership.canViewMembers());
        menu.findItem(R.id.mi_group_settings).setVisible(groupMembership.canEditGroup());
        this.thisMenu = menu;
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_group_tasks, menu);
        return true;
    }

    private void setUpViews() {
        setTitle(groupMembership.getGroupName());
        setSupportActionBar(toolbar);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        actionButton.setVisibility(canCreateTask ? View.VISIBLE : View.GONE);
    }

    private void setUpFragment() {
        taskListFragment = TaskListFragment.newInstance(groupMembership.getGroupUid(), null, this, false);
        getSupportFragmentManager().beginTransaction()
                .add(R.id.gta_fragment_holder, taskListFragment)
                .commit();
    }

    @OnClick(R.id.gta_fab)
    public void openNewTaskMenu() {
        openNewTaskMenu(true);
    }

    private void openNewTaskMenu(boolean showAddMembers) {
        newTaskMenuFragment.setShowAddMembers(showAddMembers);
        getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(R.anim.up_from_bottom, R.anim.down_from_top)
                .replace(R.id.gta_root_layout, newTaskMenuFragment)
                .addToBackStack(null)
                .commit();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final String groupUid = groupMembership.getGroupUid();
        final String groupName = groupMembership.getGroupName();
        switch (item.getItemId()) {
            case android.R.id.home:
                handleUpButton();
                return true;
            case R.id.mi_icon_filter:
                taskListFragment.filter();
                return true;
            case R.id.mi_new_task:
                openNewTaskMenu(false);
                return true;
            case R.id.mi_view_join_code:
                setUpJoinCodeFragment();
                return true;
            case R.id.mi_view_members:
                Intent viewMembers = IntentUtils.constructIntent(this, GroupMembersActivity.class, groupUid, groupName);
                viewMembers.putExtra(Constant.PARENT_TAG_FIELD, GroupTasksActivity.class.getCanonicalName());
                startActivity(viewMembers);
                return true;
            case R.id.mi_add_members:
                startActivity(IntentUtils.constructIntent(this, AddMembersActivity.class, groupUid, groupName));
                return true;
            case R.id.mi_remove_members:
                Intent removeMembers = IntentUtils.constructIntent(this, RemoveMembersActivity.class, groupUid, groupName);
                startActivity(removeMembers);
                return true;
            case R.id.mi_group_settings:
                Intent groupSettings = IntentUtils.constructIntent(this, GroupSettingsActivity.class, groupMembership);
                startActivity(groupSettings);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void handleUpButton() {
        if (!closeViewTaskFragment()) {
            Log.e(TAG, "handling up button with no view task ...");
            NavUtils.navigateUpFromSameTask(this);
        }
    }

    private void setUpJoinCodeFragment(){
        String joinCode = groupMembership.getJoinCode();
        joinCodeFragment = JoinCodeFragment.newInstance(joinCode);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.gta_root_layout, joinCodeFragment, JoinCodeFragment.class.getCanonicalName())
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void menuCloseClicked() {
        getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(R.anim.push_down_in, R.anim.push_down_out)
                .remove(newTaskMenuFragment)
                .commit();
    }

    @Override
    public void joinCodeClose() {
        getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(R.anim.push_down_in, R.anim.push_down_out)
                .remove(joinCodeFragment)
                .commit();
    }

    @Override
    public void onTaskLoaded(String taskName) {
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.btn_close_white);
        toggleMenuFilter(false);
        actionButton.setVisibility(View.GONE);
    }

    @Subscribe
    public void onTaskCancelled(TaskCancelledEvent e) {
        Log.e(TAG, "task cancelled ...");
        closeViewTaskFragment();
    }

    private boolean closeViewTaskFragment() {
        Fragment frag = getSupportFragmentManager().findFragmentByTag(ViewTaskFragment.class.getCanonicalName());
        if (frag != null && frag.isVisible()) {
            getSupportFragmentManager().beginTransaction()
                    .setCustomAnimations(R.anim.push_down_in, R.anim.push_down_out)
                    .remove(frag)
                    .commit();
            actionButton.setVisibility(canCreateTask ? View.VISIBLE : View.GONE);
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.btn_back_wt);
            toggleMenuFilter(true);
            return true;
        } else {
            return false;
        }
    }

    private void toggleMenuFilter(boolean showFilter) {
        MenuItem filter = thisMenu.findItem(R.id.mi_icon_filter);
        if (filter != null) {
            Log.e(TAG, "found menu item"); // note : for some reason, this is not hiding the filter
            if (showFilter) {
                filter.setVisible(true);
                this.invalidateOptionsMenu();
            } else {
                filter.setVisible(false);
                this.invalidateOptionsMenu();
            }
        }
    }
}
