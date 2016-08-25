package org.grassroot.android.activities;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import org.grassroot.android.R;
import org.grassroot.android.fragments.GroupPermissionsFragment;
import org.grassroot.android.fragments.GroupSettingsMainFragment;
import org.grassroot.android.fragments.MemberListFragment;
import org.grassroot.android.fragments.dialogs.ConfirmCancelDialogFragment;
import org.grassroot.android.interfaces.GroupConstants;
import org.grassroot.android.models.Group;
import org.grassroot.android.models.Member;
import org.grassroot.android.models.Permission;
import org.grassroot.android.services.ApplicationLoader;
import org.grassroot.android.services.GroupService;
import org.grassroot.android.utils.ErrorUtils;
import org.grassroot.android.utils.IntentUtils;
import org.grassroot.android.utils.NetworkUtils;
import org.grassroot.android.utils.RealmUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;

/**
 * Created by luke on 2016/07/15.
 */
public class GroupSettingsActivity extends PortraitActivity implements
        GroupSettingsMainFragment.GroupSettingsListener {

    private static final String TAG = GroupSettingsActivity.class.getSimpleName();

    private Group group;
    private GroupSettingsMainFragment mainFragment;

    @BindView(R.id.gsettings_fragment_holder) ViewGroup container;
    @BindView(R.id.gsettings_toolbar) Toolbar toolbar;
    @BindView(R.id.progressBar) ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_settings);
        ButterKnife.bind(this);

        Group groupPassed = getIntent().getParcelableExtra(GroupConstants.OBJECT_FIELD);

        if (groupPassed == null) {
            Log.e(TAG, "Error! Group settings activity called without valid group");
            startActivity(ErrorUtils.gracefulExitToHome(this));
            return;
        }

        if (!groupPassed.getPermissionsList().contains(GroupConstants.PERM_GROUP_SETTNGS)) {
            // don't crash, as server will guard against it, just handle error messages
            Log.e(TAG, "Error! Loaded without permission (stale local cache), will cause errors on calls");
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
        if (menuItem.getItemId() == android.R.id.home) {
            if (mainFragment != null && mainFragment.isAdded() && mainFragment.isVisible()) {
                Intent upIntent = NavUtils.getParentActivityIntent(this);
                // have to do next line to make sure we return with whatever is newest (else pointer mixups mean GTA launched with wrong v of group)
                upIntent.putExtra(GroupConstants.OBJECT_FIELD, RealmUtils.loadGroupFromDB(group.getGroupUid()));
                NavUtils.navigateUpTo(this, upIntent);
            } else {
                getSupportFragmentManager().popBackStack();
            }
            return true;
        }
        return super.onOptionsItemSelected(menuItem);
    }

    @Override
    public void changeGroupPicture() {
        // todo : after completion come back here instead (currently goes to home screen)
        startActivity(IntentUtils.constructIntent(this, GroupAvatarActivity.class, group));
    }

    @Override
    public void addOrganizer() {
        Map<String, Object> organizerMap = new HashMap<>();
        organizerMap.put("groupUid", group.getGroupUid());
        organizerMap.put("roleName", GroupConstants.ROLE_GROUP_ORGANIZER);
        RealmUtils.loadListFromDB(Member.class, organizerMap).subscribe(new Action1<List<Member>>() {
            @Override
            public void call(List<Member> organizers) {
                MemberListFragment memberListFragment = MemberListFragment.newInstance(group, false, false, organizers,
                    new MemberListFragment.MemberClickListener() {
                        @Override
                        public void onMemberClicked(int position, final String memberUid) {
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
        });
    }

    private void addOrganizer(final String memberUid) {
        progressBar.setVisibility(View.VISIBLE);
        GroupService.getInstance().addOrganizer(group.getGroupUid(), memberUid, AndroidSchedulers.mainThread())
            .subscribe(new Action1<String>() {
                @Override
                public void call(String s) {
                    progressBar.setVisibility(View.GONE);

                    getSupportFragmentManager().beginTransaction()
                        .replace(R.id.gsettings_fragment_holder, mainFragment, "main")
                        .commit();

                    if (s.equals(NetworkUtils.SAVED_SERVER)) {
                        Toast.makeText(ApplicationLoader.applicationContext, R.string.gset_organizer_done,
                            Toast.LENGTH_SHORT).show();
                    } else {
                        Snackbar.make(container, R.string.gset_offline_generic, Snackbar.LENGTH_LONG)
                            .show();
                    }
                }
            }, new Action1<Throwable>() {
                @Override
                public void call(Throwable throwable) {
                    progressBar.setVisibility(View.GONE);
                    Snackbar.make(container, ErrorUtils.serverErrorText(throwable), Snackbar.LENGTH_SHORT)
                        .show();
                }
            });
    }

    @Override
    public void changePermissions(final String role) {
        progressBar.setVisibility(View.VISIBLE);
        GroupService.getInstance().fetchGroupPermissions(group.getGroupUid(), role)
            .subscribe(new Action1<List<Permission>>() {
                @Override
                public void call(List<Permission> permissions) {
                    progressBar.setVisibility(View.GONE);
                    launchPermissionsFragment(group.getGroupUid(), role, permissions);
                }
            }, new Action1<Throwable>() {
                @Override
                public void call(Throwable e) {
                    progressBar.setVisibility(View.GONE);
                    if (NetworkUtils.CONNECT_ERROR.equals(e.getMessage())) {
                        ErrorUtils.snackBarWithAction(container, R.string.gset_perms_error_connect,
                            R.string.snackbar_try_connect, new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    changePermissions(role);
                                }
                            });
                    } else {
                        Snackbar.make(container, ErrorUtils.serverErrorText(e), Snackbar.LENGTH_SHORT)
                            .show();
                    }
                }
            });

    }

    // using a subscriber here was an experiment that worked well, consider replacing eventbus elsewhere that the link is so direct
    private void launchPermissionsFragment(final String groupUid, final String roleName, List<Permission> permissions) {
        GroupPermissionsFragment fragment = GroupPermissionsFragment.newInstance(groupUid, roleName, permissions,
            new Subscriber<String>() {
                @Override
                public void onCompleted() { }

                @Override
                public void onError(Throwable e) {
                    // means it couldn't be handled within fragment
                    getSupportFragmentManager().popBackStack();
                    Snackbar.make(container, R.string.gset_error_unknown, Snackbar.LENGTH_SHORT);
                }

                @Override
                public void onNext(String s) {
                    Toast.makeText(ApplicationLoader.applicationContext, R.string.gset_perms_done,
                        Toast.LENGTH_LONG).show();
                    getSupportFragmentManager().popBackStack();
                }
            });

        getSupportFragmentManager().beginTransaction()
            .replace(R.id.gsettings_fragment_holder, fragment)
            .addToBackStack(null)
            .commit();
    }

}
