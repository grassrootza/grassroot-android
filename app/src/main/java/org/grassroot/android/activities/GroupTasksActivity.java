package org.grassroot.android.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import org.grassroot.android.R;
import org.grassroot.android.events.TaskCancelledEvent;
import org.grassroot.android.fragments.ImageGridFragment;
import org.grassroot.android.fragments.JoinCodeFragment;
import org.grassroot.android.fragments.NewTaskMenuFragment;
import org.grassroot.android.fragments.TaskListFragment;
import org.grassroot.android.fragments.ViewTaskFragment;
import org.grassroot.android.fragments.dialogs.ConfirmCancelDialogFragment;
import org.grassroot.android.fragments.dialogs.MultiLineTextDialog;
import org.grassroot.android.fragments.dialogs.NetworkErrorDialogFragment;
import org.grassroot.android.interfaces.GroupConstants;
import org.grassroot.android.models.Group;
import org.grassroot.android.services.ApplicationLoader;
import org.grassroot.android.services.GroupService;
import org.grassroot.android.utils.Constant;
import org.grassroot.android.utils.ErrorUtils;
import org.grassroot.android.utils.IntentUtils;
import org.grassroot.android.utils.NetworkUtils;
import org.grassroot.android.utils.RealmUtils;
import org.grassroot.android.utils.rxutils.SingleObserverFromConsumer;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Consumer;

public class GroupTasksActivity extends PortraitActivity implements NewTaskMenuFragment.NewTaskMenuListener, JoinCodeFragment.JoinCodeListener,
    TaskListFragment.TaskListListener {

    private static final String TAG = GroupTasksActivity.class.getSimpleName();

    private Group groupMembership;
    private JoinCodeFragment joinCodeFragment;
    private NewTaskMenuFragment newTaskMenuFragment;

    private TaskListFragment taskListFragment;

    private boolean showDescOption;
    private int descOptionText;

    @BindView(R.id.gta_root_layout) ViewGroup rootLayout;
    @BindView(R.id.gta_toolbar) Toolbar toolbar;
    @BindView(R.id.progressBar) ProgressBar progressBar;

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
            try {
                groupMembership = RealmUtils.loadGroupFromDB(extras.getString(GroupConstants.UID_FIELD));
            } catch (Exception e) {
                Log.e(TAG, "Error! Group tasks activity called without group passed or valid group UID");
                startActivity(ErrorUtils.gracefulExitToHome(this));
                finish();
                return;
            }
        }

        setUpViews();
        setUpFragment();
    }

    private void setUpFragment() {
        taskListFragment = TaskListFragment.newInstance(groupMembership.getGroupUid(), this);
        getSupportFragmentManager().beginTransaction()
                .add(R.id.gta_fragment_holder, taskListFragment)
                .commit();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (newTaskMenuFragment != null) {
            getSupportFragmentManager()
                .beginTransaction()
                .remove(newTaskMenuFragment)
                .commit();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_group_tasks, menu);
        final MenuItem searchItem = menu.findItem(R.id.action_search);
        final SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchItem);
        if (searchView != null) {
            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    taskListFragment.searchStringChanged(query);
                    return true;
                }

                @Override
                public boolean onQueryTextChange(String newText) {
                    taskListFragment.searchStringChanged(newText);
                    return true;
                }
            });
        }
        return true;
    }

    private void setUpViews() {
        setTitle(groupMembership.getGroupName());

        // if don't have permission to change, we just display
        showDescOption = groupMembership.canEditGroup() || !TextUtils.isEmpty(groupMembership.getDescription());
        descOptionText = TextUtils.isEmpty(groupMembership.getDescription()) ? R.string.gset_desc_add
            : groupMembership.canEditGroup() ? R.string.gta_menu_change_desc : R.string.gta_menu_view_desc;

        setSupportActionBar(toolbar);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.mi_change_desc).setVisible(showDescOption);
        menu.findItem(R.id.mi_change_desc).setTitle(descOptionText);
        menu.findItem(R.id.mi_view_join_code).setVisible(groupMembership.hasJoinCode());
        menu.findItem(R.id.mi_add_members).setVisible(groupMembership.canAddMembers());
        menu.findItem(R.id.mi_remove_members).setVisible(groupMembership.canDeleteMembers());
        menu.findItem(R.id.mi_view_members).setVisible(groupMembership.canViewMembers());
        menu.findItem(R.id.mi_group_settings).setVisible(groupMembership.canEditGroup());
        menu.findItem(R.id.mi_group_unsubscribe).setVisible(!groupMembership.canEditGroup()); // organizers can't leave (refine in future)
        menu.findItem(R.id.mi_share_default).setVisible(false);
        menu.findItem(R.id.mi_delete_messages).setVisible(false);
        menu.findItem(R.id.mi_group_mute).setVisible(false);
        return true;
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
            case R.id.mi_change_desc:
                viewOrChangeDescription();
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
            case R.id.mi_group_unsubscribe:
                unsubscribePrompt();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackPressed() {
        if (!closeViewTaskFragment()) {
            super.onBackPressed();
        }
    }

    private void handleUpButton() {
        if (!closeViewTaskFragment()) {
            startActivity(new Intent(this, HomeScreenActivity.class));
        }
    }

    private void viewOrChangeDescription() {
        if (groupMembership.canEditGroup()) {
            changeGroupDescDialog(TextUtils.isEmpty(groupMembership.getDescription()));
        } else if (showDescOption) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.gta_desc_title)
                .setMessage(groupMembership.getDescription())
                .setCancelable(true)
                .show();
        }
    }

    private void changeGroupDescDialog(final boolean isEmptyDesc) {
        final String message = isEmptyDesc ? getString(R.string.gset_no_description) :
            getString(R.string.gset_has_desc_body, groupMembership.getDescription());
        MultiLineTextDialog.showMultiLineDialog(getSupportFragmentManager(), -1, message,
            R.string.gset_desc_dialog_hint, R.string.gset_desc_dialog_done).subscribe(new Consumer<String>() {
            @Override
            public void accept(String s) {
                progressBar.setVisibility(View.VISIBLE);
                serviceCallChangeDesc(s);
            }
        });
    }

    private void serviceCallChangeDesc(final String newDescription) {
        GroupService.getInstance().changeGroupDescription(groupMembership.getGroupUid(), newDescription,
            AndroidSchedulers.mainThread()).subscribe(new Consumer<String>() {
            @Override
            public void accept(@NonNull String s) {
                progressBar.setVisibility(View.GONE);
                if (s.equals(NetworkUtils.SAVED_SERVER)) {
                    Toast.makeText(GroupTasksActivity.this, R.string.gset_desc_change_done, Toast.LENGTH_SHORT).show();
                } else {
                    Snackbar.make(rootLayout, R.string.gset_desc_offline, Snackbar.LENGTH_SHORT).show();
                }
                groupMembership.setDescription(newDescription);
            }
        }, new Consumer<Throwable>() {
            @Override
            public void accept(@NonNull Throwable throwable) {
                progressBar.setVisibility(View.GONE);
                Snackbar.make(rootLayout, ErrorUtils.serverErrorText(throwable), Snackbar.LENGTH_SHORT).show();
            }
        });
    }

    private void unsubscribePrompt() {
        ConfirmCancelDialogFragment.newInstance(getString(R.string.gta_unsub_message, groupMembership.getGroupName()),
            new ConfirmCancelDialogFragment.ConfirmDialogListener() {
                @Override
                public void doConfirmClicked() {
                    unsubscribeAndExit();
                }
            }).show(getSupportFragmentManager(), "dialog");
    }


    private void unsubscribeAndExit() {
        progressBar.setVisibility(View.VISIBLE);
        GroupService.getInstance().unsubscribeFromGroup(groupMembership.getGroupUid(), AndroidSchedulers.mainThread())
            .subscribe(new Consumer<String>() {
                @Override
                public void accept(@NonNull String s) {
                    progressBar.setVisibility(View.GONE);
                    if (!RealmUtils.loadPreferencesFromDB().isHasGroups()) {
                        startActivity(new Intent(GroupTasksActivity.this, NoGroupWelcomeActivity.class));
                    } else {
                        startActivity(new Intent(GroupTasksActivity.this, HomeScreenActivity.class));
                    }
                    Toast.makeText(ApplicationLoader.applicationContext, R.string.gta_unsub_done, Toast.LENGTH_SHORT)
                            .show();
                    finish();
                }
            }, new Consumer<Throwable>() {
                @Override
                public void accept(@NonNull Throwable e) {
                    progressBar.setVisibility(View.GONE);
                    if (NetworkUtils.SERVER_ERROR.equals(e.getMessage())) {
                        Snackbar.make(rootLayout, ErrorUtils.serverErrorText(e), Snackbar.LENGTH_SHORT).show();
                    } else {
                        final int dialogMsg = NetworkUtils.OFFLINE_SELECTED.equals(e.getMessage()) ?
                            R.string.connect_error_unsub_offline : R.string.connect_error_group_unsubscribe;
                        NetworkErrorDialogFragment.newInstance(dialogMsg, progressBar, new SingleObserverFromConsumer<>(new Consumer<String>() {
                                @Override
                                public void accept(String s) {
                                    progressBar.setVisibility(View.GONE);
                                    if (s.equals(NetworkUtils.CONNECT_ERROR)) {
                                        Snackbar.make(rootLayout, R.string.connect_error_failed_retry, Snackbar.LENGTH_SHORT).show();
                                    } else {
                                        unsubscribeAndExit();
                                    }
                                }
                            })).show(getSupportFragmentManager(), "dialog");
                    }
                }

            });
    }

    private void setUpJoinCodeFragment(){
        String joinCode = groupMembership.getJoinCode();
        joinCodeFragment = JoinCodeFragment.newInstance(joinCode);
        getSupportFragmentManager().beginTransaction()
            .setCustomAnimations(R.anim.flyin_fast, R.anim.flyout_fast)
            .replace(R.id.gta_root_layout, joinCodeFragment, JoinCodeFragment.class.getCanonicalName())
            .addToBackStack(null)
            .commit();
    }

    @Override
    public void joinCodeClose() {
        getSupportFragmentManager().beginTransaction()
            .setCustomAnimations(R.anim.flyin_fast, R.anim.flyout_fast)
                .remove(joinCodeFragment)
                .commit();
    }

    @Override
    public void loadSingleTask(String taskUid, String taskType) {
        ViewTaskFragment taskFragment = ViewTaskFragment.newInstance(taskType, taskUid);

        getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(R.anim.flyin_fast, R.anim.flyout_fast)
                .add(R.id.gta_fragment_holder, taskFragment, ViewTaskFragment.class.getCanonicalName())
                .addToBackStack(null)
                .commit();

        if (getSupportActionBar() != null) {
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.btn_close_white);
        }
    }

    @Override
    public void onFabClicked() {
        if (newTaskMenuFragment == null) {
            newTaskMenuFragment = NewTaskMenuFragment.newInstance(groupMembership, true);
        }

        if (groupMembership != null && groupMembership.hasCreatePermissions()) {
            getSupportFragmentManager() .beginTransaction()
                .setCustomAnimations(R.anim.flyin_fast, R.anim.flyout_fast)
                .replace(R.id.gta_root_layout, newTaskMenuFragment)
                .addToBackStack(null)
                .commit();
        }
    }

    @Override
    public void menuCloseClicked() {
        getSupportFragmentManager().beginTransaction()
            .setCustomAnimations(R.anim.flyin_fast, R.anim.flyout_fast)
            .remove(newTaskMenuFragment)
            .commit();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onTaskCancelled(TaskCancelledEvent e) {
        closeViewTaskFragment();
    }

    private boolean closeViewTaskFragment() {
        boolean closedSubFrag = false;
        Fragment imageFrag = getSupportFragmentManager().findFragmentByTag(ImageGridFragment.class.getCanonicalName());
        Fragment taskFrag = getSupportFragmentManager().findFragmentByTag(ViewTaskFragment.class.getCanonicalName());
        if (imageFrag != null && imageFrag.isVisible()) {
            // don't need to change title, and don't both with animation
            getSupportFragmentManager().beginTransaction()
                    .remove(imageFrag)
                    .commit();
            closedSubFrag = true;
        } else if (taskFrag != null && taskFrag.isVisible()) {
            getSupportFragmentManager().beginTransaction()
                    .setCustomAnimations(R.anim.push_down_in, R.anim.push_down_out)
                    .remove(taskFrag)
                    .commit();

            // keep null checks in place in case subscriber triggered after view destroyed
            if (getSupportActionBar() != null) {
                getSupportActionBar().setHomeAsUpIndicator(R.drawable.btn_back_wt);
            }
            setTitle(groupMembership.getGroupName());
            closedSubFrag = true;
        }
        return closedSubFrag;
    }



}
