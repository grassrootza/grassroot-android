package com.techmorphosis.grassroot.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.github.clans.fab.FloatingActionButton;
import com.github.clans.fab.FloatingActionMenu;
import com.techmorphosis.grassroot.R;
import com.techmorphosis.grassroot.ui.fragments.MemberListFragment;
import com.techmorphosis.grassroot.utils.Constant;
import com.techmorphosis.grassroot.utils.MenuUtils;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by luke on 2016/05/18.
 */
public class GroupMembersActivity extends PortraitActivity implements MemberListFragment.MemberListListener {

    private static final String TAG = GroupMembersActivity.class.getCanonicalName();

    private String groupUid;
    private String groupName;
    private String parentTag;

    private MemberListFragment memberListFragment;

    @BindView(R.id.lm_toolbar)
    Toolbar lmToolbar;
    @BindView(R.id.lm_tv_groupname)
    TextView tvGroupName;

    @BindView(R.id.lm_ic_floating_menu)
    FloatingActionMenu floatingMenu;
    @BindView(R.id.lm_fab_add_members)
    FloatingActionButton fabAddMembers;
    @BindView(R.id.lm_fab_new_task)
    FloatingActionButton fabNewTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group__listmembers);
        ButterKnife.bind(this);

        Bundle extras = getIntent().getExtras();
        if (extras == null) {
            throw new UnsupportedOperationException("Group member activity attempted without necessary arguments");
        } else {
            groupUid = extras.getString(Constant.GROUPUID_FIELD);
            groupName = extras.getString(Constant.GROUPNAME_FIELD);
            parentTag = extras.getString(Constant.PARENT_TAG_FIELD);
            setUpToolbar();
            setUpFloatingMenu();
            setUpMemberListFragment();
        }
    }

    private void setUpToolbar() {
        tvGroupName.setText(groupName);
        setSupportActionBar(lmToolbar);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    private void setUpFloatingMenu() {
        floatingMenu.setOnMenuToggleListener(new FloatingActionMenu.OnMenuToggleListener() {
            @Override
            public void onMenuToggle(boolean opened) {
                fabAddMembers.setVisibility(opened ? View.VISIBLE : View.GONE);
                fabNewTask.setVisibility(opened ? View.VISIBLE : View.GONE);
            }
        });
    }

    private void setUpMemberListFragment() {
        memberListFragment = new MemberListFragment();
        memberListFragment.setGroupUid(groupUid);
        memberListFragment.setShowSelected(false);
        memberListFragment.setCanDismissItems(false);

        getSupportFragmentManager().beginTransaction()
                .add(R.id.lm_member_list_container, memberListFragment)
                .commit();
    }

    @Override
    public void onMemberListInitiated(MemberListFragment fragment) {
        Log.d(TAG, "Member list fragment succesfully initiated");
        // todo: add an "onclick" listener
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // todo: check permissions & decide which to include
        getMenuInflater().inflate(R.menu.menu_group_members, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case (R.id.mi_add_members):
                Intent i = MenuUtils.constructIntent(this, AddMembersActivity.class, groupUid, groupName);
                startActivity(i);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @OnClick(R.id.lm_fab_add_members)
    public void launchAddMembers() {
        floatingMenu.close(true);
        Intent i = MenuUtils.constructIntent(this, AddMembersActivity.class, groupUid, groupName);
        startActivity(i);
    }

    @OnClick(R.id.lm_fab_new_task)
    public void launchNewTask() {
        floatingMenu.close(true);
        Intent i = MenuUtils.constructIntent(this, NewTaskMenuActivity.class, groupUid, groupName);
        // todo: tell new task menu to not include "add members"
        startActivity(i);
    }

    @Override
    public Intent getSupportParentActivityIntent() {
        return getParentActivityIntentImpl();
    }

    @Override
    public Intent getParentActivityIntent() {
        return getParentActivityIntentImpl();
    }

    private Intent getParentActivityIntentImpl() {
        Intent i = null;
        int flags = Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP;
        if (parentTag == null || parentTag.equals(HomeScreenActivity.class.getCanonicalName())) {
            i = new Intent(this, HomeScreenActivity.class);
            i.setFlags(flags);
        } else if (parentTag.equals(GroupTasksActivity.class.getCanonicalName())) {
            i = new Intent(this, GroupTasksActivity.class);
            i.setFlags(flags);
            i.putExtra(Constant.GROUPUID_FIELD, groupUid);
            i.putExtra(Constant.GROUPNAME_FIELD, groupName);
        }
        return i;
    }

}
