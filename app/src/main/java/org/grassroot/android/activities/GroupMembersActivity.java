package org.grassroot.android.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.crashlytics.android.Crashlytics;

import org.grassroot.android.R;
import org.grassroot.android.fragments.EditTaskFragment;
import org.grassroot.android.fragments.MemberListFragment;
import org.grassroot.android.fragments.NewTaskMenuFragment;
import org.grassroot.android.interfaces.GroupConstants;
import org.grassroot.android.models.Group;
import org.grassroot.android.models.Member;
import org.grassroot.android.services.GroupService;
import org.grassroot.android.utils.Constant;
import org.grassroot.android.utils.ErrorUtils;
import org.grassroot.android.utils.IntentUtils;
import org.grassroot.android.utils.NetworkUtils;
import org.grassroot.android.utils.RealmUtils;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Consumer;

/**
 * Created by luke on 2016/05/18.
 */
public class GroupMembersActivity extends PortraitActivity implements NewTaskMenuFragment.NewTaskMenuListener {

    private static final String TAG = GroupMembersActivity.class.getCanonicalName();

    private Group group;
    private String groupUid;
    private String groupName;
    private String parentTag;

    private boolean selectMembers;
    private ArrayList<Member> membersSelected; // better Java practice is declare interface, but Android.

    private MemberListFragment memberListFragment;
    private NewTaskMenuFragment newTaskMenuFragment;

    @BindView(R.id.lm_toolbar) Toolbar lmToolbar;
    @BindView(R.id.lm_tv_groupname) TextView tvGroupName;
    @BindView(R.id.lm_member_list_container) SwipeRefreshLayout memberListContainer;
    @BindView(R.id.lm_tv_existing_members_title) TextView tvExistingMembers;

    private boolean menuOpen;
    private boolean showNewTaskFab;
    private boolean showAddMemberFab;

    @BindView(R.id.lm_ic_floating_menu) FloatingActionButton floatingMenu;
    @BindView(R.id.lm_fab_add_members) LinearLayout fabAddMembers;
    @BindView(R.id.lm_fab_new_task) LinearLayout fabNewTask;

    @BindView(R.id.lm_ll_check_clear_all) LinearLayout llCheckAllClearAll;
    @BindView(R.id.lm_btn_done) Button btnDone;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group__listmembers);
        ButterKnife.bind(this);

        Bundle extras = getIntent().getExtras();
        if (extras == null) {
            Crashlytics.logException(new UnsupportedOperationException("Group member activity attempted without necessary arguments"));
            startActivity(ErrorUtils.gracefulExitToHome(this));
            finish();
        }

        group = extras.getParcelable(GroupConstants.OBJECT_FIELD);
        if (group == null) {
            groupUid = getIntent().getStringExtra(GroupConstants.UID_FIELD);
            groupName = getIntent().getStringExtra(GroupConstants.NAME_FIELD);
            group = RealmUtils.loadGroupFromDB(groupUid);
        } else {
            groupUid = group.getGroupUid();
            groupName = group.getGroupName();
            Log.e(TAG, "inside group view members, entity looks like : " + group.toString());
        }

        if (groupUid == null) {
            throw new UnsupportedOperationException("Error! Group member activity called without group details");
        }

        parentTag = extras.getString(Constant.PARENT_TAG_FIELD);
        selectMembers = extras.getBoolean(Constant.SELECT_FIELD, false);

        boolean showGroupHeader = extras.getBoolean(Constant.SHOW_HEADER_FLAG, true);
        boolean showActionButton = extras.getBoolean(Constant.SHOW_ACTION_BUTTON_FLAG, true);

        showNewTaskFab = group.hasCreatePermissions();
        showAddMemberFab = group.canAddMembers();

        floatingMenu.setVisibility(showActionButton && (showNewTaskFab || showAddMemberFab)
            ? View.VISIBLE : View.GONE);

        if (selectMembers) {
            membersSelected = extras.getParcelableArrayList(Constant.SELECTED_MEMBERS_FIELD);
        }

        btnDone.setVisibility(showActionButton ? View.GONE : View.VISIBLE);
        tvGroupName.setVisibility(showGroupHeader ? View.VISIBLE : View.GONE);
        tvExistingMembers.setVisibility(showGroupHeader ? View.VISIBLE : View.GONE);
        llCheckAllClearAll.setVisibility(showGroupHeader ? View.GONE : View.VISIBLE);

        if (showNewTaskFab) {
            newTaskMenuFragment = NewTaskMenuFragment.newInstance(group, false);
        }

        setUpToolbar();
        setUpMemberListFragment();
    }

    private void setUpToolbar() {
        tvGroupName.setText(groupName);
        setSupportActionBar(lmToolbar);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @OnClick(R.id.lm_ic_floating_menu)
    public void toggleActionMenu() {
        floatingMenu.setImageResource(menuOpen ? R.drawable.ic_add : R.drawable.ic_add_45d);
        fabAddMembers.setVisibility(menuOpen ? View.GONE : (showAddMemberFab) ? View.VISIBLE : View.GONE);
        fabNewTask.setVisibility(menuOpen ? View.GONE : (showNewTaskFab) ? View.VISIBLE : View.GONE);
        menuOpen = !menuOpen;
    }

    private void setUpMemberListFragment() {
        memberListContainer.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refreshMembers();
            }
        });

        memberListFragment = MemberListFragment.newInstance(groupUid, selectMembers, selectMembers, membersSelected, true, null);
        getSupportFragmentManager().beginTransaction()
                .add(R.id.lm_member_list_container, memberListFragment)
                .commit();
    }

    private void refreshMembers() {
        GroupService.getInstance().refreshGroupMembers(groupUid).subscribe(new Consumer<String>() {
            @Override
            public void accept(@NonNull String s) {
                Log.e(TAG, "got a response back! looks like: " + s);
                memberListContainer.setRefreshing(false);
                if (NetworkUtils.FETCHED_SERVER.equals(s)) {
                    memberListFragment.refreshMembersToDb();
                }
            }
        }, new Consumer<Throwable>() {
            @Override
            public void accept(@NonNull Throwable e) {
                memberListContainer.setRefreshing(false);
                if (NetworkUtils.SERVER_ERROR.equals(e.getMessage())) {
                    Snackbar.make(memberListContainer, ErrorUtils.serverErrorText(e), Snackbar.LENGTH_SHORT).show();
                } else if (NetworkUtils.CONNECT_ERROR.equals(e.getMessage())) {
                    Snackbar.make(memberListContainer, R.string.connect_error_offline_home, Snackbar.LENGTH_SHORT).show();
                }
            }
        });
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
        getMenuInflater().inflate(R.menu.menu_group_members, menu);
        menu.findItem(R.id.mi_add_members).setVisible(group != null && group.canAddMembers());
        menu.findItem(R.id.mi_remove_members).setVisible(group != null && group.canDeleteMembers());
        menu.findItem(R.id.mi_group_settings).setVisible(group != null && group.canEditGroup());
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case (R.id.mi_refresh_members):
                memberListContainer.setRefreshing(true);
                refreshMembers();
                return true;
            case (R.id.mi_add_members):
                Intent i = IntentUtils.constructIntent(this, AddMembersActivity.class, groupUid, groupName);
                startActivity(i);
                return true;
            case (R.id.mi_remove_members):
                Intent i2 = IntentUtils.constructIntent(this, RemoveMembersActivity.class, groupUid, groupName);
                startActivity(i2);
                return true;
            case (R.id.mi_group_settings):
                Intent i3 = IntentUtils.constructIntent(this, GroupSettingsActivity.class, group);
                startActivity(i3);
                return true;
            case (android.R.id.home):
                finish(); // maybe return the selection ... check w user feedback
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @OnClick(R.id.lm_fab_add_members)
    public void launchAddMembers() {
        toggleActionMenu();
        // the button should not event display, so this shouldn't be possible, but usual Android / phone
        // trust issues mean prudent to add another layer of protection
        if (group.canAddMembers()) {
            Intent i = IntentUtils.constructIntent(this, AddMembersActivity.class, groupUid, groupName);
            startActivity(i);
        } else {
            Snackbar.make(fabAddMembers, R.string.permissions_error_new_task, Snackbar.LENGTH_SHORT).show();
        }
    }

    @OnClick(R.id.lm_fab_new_task)
    public void launchNewTask() {
        toggleActionMenu();
        if (group.hasCreatePermissions()) {
            getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(R.anim.up_from_bottom, R.anim.down_from_top)
                .add(R.id.rl_lm_root, newTaskMenuFragment)
                .addToBackStack(null)
                .commit();
        } else {
            Snackbar.make(fabNewTask, R.string.permissions_error_new_task, Snackbar.LENGTH_SHORT).show();
        }
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
            i = IntentUtils.constructIntent(this, GroupTasksActivity.class, groupUid, groupName);
            i.setFlags(flags);
        } else if (parentTag.equals(CreateMeetingActivity.class.getCanonicalName())) {
            i = IntentUtils.constructIntent(this, CreateMeetingActivity.class, groupUid, groupName);
            i.setFlags(flags);
        } else if (parentTag.equals(EditTaskFragment.class.getCanonicalName())) {
            // aaah ... need to maybe rethink how handling all of this .. need to go back, without recreating ...
        }
        return i;
    }

    @Override
    public void menuCloseClicked() {
        getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(R.anim.push_down_in, R.anim.push_down_out)
                .remove(newTaskMenuFragment)
                .commit();
    }
}