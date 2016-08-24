package org.grassroot.android.activities;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;

import org.grassroot.android.R;
import org.grassroot.android.fragments.GroupPermissionsFragment;
import org.grassroot.android.fragments.GroupSettingsMainFragment;
import org.grassroot.android.fragments.MemberListFragment;
import org.grassroot.android.fragments.dialogs.ConfirmCancelDialogFragment;
import org.grassroot.android.interfaces.GroupConstants;
import org.grassroot.android.models.Group;
import org.grassroot.android.models.Member;
import org.grassroot.android.services.GroupService;
import org.grassroot.android.utils.IntentUtils;

import java.util.Collections;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import rx.Subscriber;

/**
 * Created by luke on 2016/07/15.
 */
public class GroupSettingsActivity extends PortraitActivity implements
        GroupSettingsMainFragment.GroupSettingsListener {

    private static final String TAG = GroupSettingsActivity.class.getSimpleName();

    private Group group;
    private GroupSettingsMainFragment mainFragment;

    @BindView(R.id.gsettings_toolbar) Toolbar toolbar;

    ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_settings);
        ButterKnife.bind(this);
        // EventBus.getDefault().register(this);

        Group groupPassed = getIntent().getParcelableExtra(GroupConstants.OBJECT_FIELD);

        if (groupPassed == null) {
            throw new UnsupportedOperationException("Error! Group settings activityo called without valid group");
        }

        if (!groupPassed.getPermissionsList().contains(GroupConstants.PERM_GROUP_SETTNGS)) {
            throw new UnsupportedOperationException("Error! Group settings activity called without permissions");
        }

        this.group = groupPassed;
        setTitle(R.string.gset_main_title);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.btn_back_wt); // todo : insert group avatar
            getSupportActionBar().setHomeButtonEnabled(true);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        mainFragment = GroupSettingsMainFragment.newInstance(group, this);
        getSupportFragmentManager().beginTransaction()
                .add(R.id.gsettings_fragment_holder, mainFragment, "main")
                .commit();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        // todo : figure out how to do this without triggering group call ...
        if (menuItem.getItemId() == android.R.id.home) {
            if (mainFragment != null && mainFragment.isAdded() && mainFragment.isVisible()) {
                Intent upIntent = NavUtils.getParentActivityIntent(this);
                upIntent.putExtra(GroupConstants.OBJECT_FIELD, group);
                NavUtils.navigateUpTo(this, upIntent);
            } else {
                getSupportFragmentManager().popBackStack();
            }
            return true;
        }
        return super.onOptionsItemSelected(menuItem);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // EventBus.getDefault().unregister(this);
    }

    @Override
    public void changeGroupPicture() {
        // todo : use event bus to switch picture
        startActivity(IntentUtils.constructIntent(this, GroupAvatarActivity.class, group));
    }

    @Override
    public void addOrganizer() {
        List<Member> existingOrganizers = Collections.emptyList(); // todo : get organizers
        MemberListFragment memberListFragment = MemberListFragment.newInstance(group, false, false, existingOrganizers,
                new MemberListFragment.MemberClickListener() {
                    @Override
                    public void onMemberClicked(int position, final String memberUid) {
                        Log.e(TAG, "member clicked! at this position : " + position);
                        ConfirmCancelDialogFragment.newInstance(R.string.gset_organizer_confirm, new ConfirmCancelDialogFragment.ConfirmDialogListener() {
                            @Override
                            public void doConfirmClicked() {
                                addOrganizer(memberUid);
                            }
                        }).show(getSupportFragmentManager(), null);
                    }
                });
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.gsettings_fragment_holder, memberListFragment, "member_pick")
                .addToBackStack(null)
                .commit();
    }

    private void addOrganizer(final String memberUid) {
        showProgress();
        GroupService.getInstance().addOrganizer(group.getGroupUid(), memberUid, null)
            .subscribe(new Subscriber<String>() {
            @Override
            public void onCompleted() { }

            @Override
            public void onError(Throwable e) {
                dismissProgress();
                // todo : show a snackbar
            }

            @Override
            public void onNext(String s) {
                dismissProgress();
                getSupportFragmentManager().beginTransaction()
                    .replace(R.id.gsettings_fragment_holder, mainFragment, "main")
                    .commit();
            }
        });
    }

    // todo : move the dialog into the main fragment?
    @Override
    public void changePermissions() {
        Log.e(TAG, "change permissions");
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.gset_perms_title)
                .setItems(R.array.gset_roles, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0:
                                launchPermissionsFragment(GroupConstants.ROLE_GROUP_ORGANIZER);
                                break;
                            case 1:
                                launchPermissionsFragment(GroupConstants.ROLE_COMMITTEE_MEMBER);
                                break;
                            case 2:
                                launchPermissionsFragment(GroupConstants.ROLE_ORDINARY_MEMBER);
                                break;
                        }
                    }
                });
        builder.create().show();
    }

    private void launchPermissionsFragment(final String role) {
        GroupPermissionsFragment fragment = GroupPermissionsFragment.newInstance(group, role);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.gsettings_fragment_holder, fragment)
                .addToBackStack(null)
                .commit();
    }

    private void showProgress() {
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(this, R.style.AppTheme);
            progressDialog.setMessage(getString(R.string.txt_pls_wait));
            progressDialog.setIndeterminate(true);
        }
        progressDialog.show();
    }

    private void dismissProgress() {
        if (progressDialog != null) {
            progressDialog.dismiss();
        }
    }

}
