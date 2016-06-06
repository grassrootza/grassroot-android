package org.grassroot.android.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import org.grassroot.android.R;
import org.grassroot.android.interfaces.GroupConstants;
import org.grassroot.android.services.model.Group;
import org.grassroot.android.ui.fragments.TaskListFragment;
import org.grassroot.android.utils.Constant;
import org.grassroot.android.utils.MenuUtils;

import java.util.HashSet;
import java.util.Set;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class GroupTasksActivity extends PortraitActivity {

    private static final String TAG = GroupTasksActivity.class.getCanonicalName();

    private Group groupMembership;
    private TaskListFragment taskListFragment;

    @BindView(R.id.gta_root_layout)
    RelativeLayout rootLayout;
    @BindView(R.id.gta_toolbar)
    Toolbar toolbar;
    @BindView(R.id.gta_fragment_holder)
    FrameLayout fragmentHolder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_tasks);
        ButterKnife.bind(this);
        final Bundle extras = getIntent().getExtras();

        if (extras == null) {
            throw new UnsupportedOperationException("Error! Group tasks activity called without group passed");
        }

        groupMembership = extras.getParcelable(GroupConstants.OBJECT_FIELD);
        setUpViews();
        setUpFragment();
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        final Set<String> perms = new HashSet<>(groupMembership.getPermissions());
        menu.findItem(R.id.mi_add_members).setVisible(perms.contains(GroupConstants.PERM_ADD_MEMBER));
        menu.findItem(R.id.mi_remove_members).setVisible(perms.contains(GroupConstants.PERM_DEL_MEMBER));
        menu.findItem(R.id.mi_view_members).setVisible(perms.contains(GroupConstants.PERM_VIEW_MEMBERS));
        menu.findItem(R.id.mi_group_settings).setVisible(perms.contains(GroupConstants.PERM_GROUP_SETTNGS));
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
    }

    private void setUpFragment() {
        taskListFragment = new TaskListFragment();
        Bundle args = new Bundle();
        args.putString(GroupConstants.UID_FIELD, groupMembership.getGroupUid());
        taskListFragment.setArguments(args);

        getSupportFragmentManager().beginTransaction()
                .add(R.id.gta_fragment_holder, taskListFragment)
                .commit();
    }

    @OnClick(R.id.gta_fab)
    public void openNewTaskMenu() {
        // todo : use / pass permissions
        startActivity(MenuUtils.constructIntent(GroupTasksActivity.this, NewTaskMenuActivity.class,
                groupMembership.getGroupUid(), groupMembership.getGroupName()));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final String groupUid = groupMembership.getGroupUid();
        final String groupName = groupMembership.getGroupName();
        switch (item.getItemId()) {
            case R.id.mi_icon_filter:
                taskListFragment.filter();
                return true;
            case R.id.mi_view_members:
                Intent viewMembers = MenuUtils.constructIntent(this, GroupMembersActivity.class, groupUid, groupName);
                viewMembers.putExtra(Constant.PARENT_TAG_FIELD, GroupTasksActivity.class.getCanonicalName());
                startActivity(viewMembers);
                return true;
            case R.id.mi_add_members:
                startActivity(MenuUtils.constructIntent(this, AddMembersActivity.class, groupUid, groupName));
                return true;
            case R.id.mi_remove_members:
                Intent removeMembers = MenuUtils.constructIntent(this, RemoveMembersActivity.class, groupUid, groupName);
                startActivity(removeMembers);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

}
