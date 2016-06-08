package org.grassroot.android.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.github.clans.fab.FloatingActionButton;
import com.github.clans.fab.FloatingActionMenu;
import org.grassroot.android.R;
import org.grassroot.android.services.model.Member;
import org.grassroot.android.ui.fragments.MemberListFragment;
import org.grassroot.android.utils.Constant;
import org.grassroot.android.utils.MenuUtils;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by luke on 2016/05/18.
 */
public class GroupMembersActivity extends PortraitActivity implements MemberListFragment.MemberListListener,
        MemberListFragment.MemberClickListener {

    private static final String TAG = GroupMembersActivity.class.getCanonicalName();

    private String groupUid;
    private String groupName;
    private String parentTag;

    private boolean selectMembers;
    private ArrayList<Member> membersSelected; // better Java practice is declare interface, but Android.

    private MemberListFragment memberListFragment;

    @BindView(R.id.lm_toolbar)
    Toolbar lmToolbar;
    @BindView(R.id.lm_tv_groupname)
    TextView tvGroupName;
    @BindView(R.id.lm_tv_existing_members_title)
    TextView tvExistingMembers;

    @BindView(R.id.lm_ic_floating_menu)
    FloatingActionMenu floatingMenu;
    @BindView(R.id.lm_fab_add_members)
    FloatingActionButton fabAddMembers;
    @BindView(R.id.lm_fab_new_task)
    FloatingActionButton fabNewTask;

    @BindView(R.id.lm_ll_check_clear_all)
    LinearLayout llCheckAllClearAll;
    @BindView(R.id.lm_btn_done)
    Button btnDone;

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

            selectMembers = extras.getBoolean(Constant.SELECT_FIELD, false);
            boolean showGroupHeader = extras.getBoolean(Constant.SHOW_HEADER_FLAG, true);
            boolean showActionButton = extras.getBoolean(Constant.SHOW_ACTION_BUTTON_FLAG, true);

            if (selectMembers) {
                membersSelected = extras.getParcelableArrayList(Constant.SELECTED_MEMBERS_FIELD);
            }

            floatingMenu.setVisibility(showActionButton ? View.VISIBLE : View.GONE);
            btnDone.setVisibility(showActionButton ? View.GONE : View.VISIBLE);

            tvGroupName.setVisibility(showGroupHeader ? View.VISIBLE : View.GONE);
            tvExistingMembers.setVisibility(showGroupHeader ? View.VISIBLE : View.GONE);
            llCheckAllClearAll.setVisibility(showGroupHeader ? View.GONE : View.VISIBLE);

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
        memberListFragment.setShowSelected(selectMembers);
        memberListFragment.setCanDismissItems(false);

        getSupportFragmentManager().beginTransaction()
                .add(R.id.lm_member_list_container, memberListFragment)
                .commit();
    }

    @Override
    public void onMemberListInitiated(MemberListFragment fragment) {
        Log.d(TAG, "Member list fragment succesfully initiated");
        if (selectMembers && membersSelected != null) {
            memberListFragment.addMembers(membersSelected);
        }
    }

    @Override
    public void onMemberListPopulated(List<Member> memberList) {

    }

    @Override
    public void onMemberDismissed(int position, String memberUid) {

    }

    @Override
    public void onMemberClicked(int position, String memberUid) {

    }

    @OnClick(R.id.lm_btn_check_all)
    public void selectAllMembers() {
        memberListFragment.selectAllMembers();
    }

    @OnClick(R.id.lm_btn_clear_all)
    public void clearAllMembers() {
        memberListFragment.unselectAllMembers();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // todo: check permissions & decide which to include
        // todo: do not include if this is in "selection" mode
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

    @OnClick(R.id.lm_btn_done)
    public void returnSelectedMembers() {
        assembleReturnIntent();
        finish();
    }

    private Intent assembleReturnIntent() {
        Intent i = new Intent();
        membersSelected = new ArrayList<>(memberListFragment.getSelectedMembers());
        i.putParcelableArrayListExtra(Constant.SELECTED_MEMBERS_FIELD, membersSelected);
        setResult(RESULT_OK,i);
        return i;
    }


    private Intent getParentActivityIntentImpl() {
        Intent i = null;
        int flags = Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP;
        if (parentTag == null || parentTag.equals(HomeScreenActivity.class.getCanonicalName())) {
            i = new Intent(this, HomeScreenActivity.class);
            i.setFlags(flags);
        } else if (parentTag.equals(GroupTasksActivity.class.getCanonicalName())) {
            i = MenuUtils.constructIntent(this, GroupTasksActivity.class, groupUid, groupName);
            i.setFlags(flags);
        } else if (parentTag.equals(CreateMeetingActivity.class.getCanonicalName())) {
            i = MenuUtils.constructIntent(this, CreateMeetingActivity.class, groupUid, groupName);
            i.setFlags(flags);
        }
        return i;
    }

}
