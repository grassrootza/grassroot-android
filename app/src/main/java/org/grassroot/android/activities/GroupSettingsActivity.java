package org.grassroot.android.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;

import org.grassroot.android.R;
import org.grassroot.android.fragments.GroupSettingsMainFragment;
import org.grassroot.android.interfaces.GroupConstants;
import org.grassroot.android.models.Group;
import org.grassroot.android.utils.Constant;
import org.grassroot.android.utils.MenuUtils;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by luke on 2016/07/15.
 */
public class GroupSettingsActivity extends PortraitActivity implements GroupSettingsMainFragment.GroupSettingsListener {

    private static final String TAG = GroupSettingsActivity.class.getSimpleName();

    private Group group;
    private GroupSettingsMainFragment mainFragment;

    @BindView(R.id.gsettings_toolbar) Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_settings);
        ButterKnife.bind(this);
        // EventBus.getDefault().register(this);

        Group groupPassed = getIntent().getParcelableExtra(GroupConstants.OBJECT_FIELD);

        if (groupPassed == null) {
            throw new UnsupportedOperationException("Error! Group settings activity called without valid group");
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
        Intent parent = MenuUtils.constructIntent(this, GroupTasksActivity.class, group);
        startActivity(parent);
        finish();
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // EventBus.getDefault().unregister(this);
    }

    @Override
    public void changeGroupPicture() {
        Log.e(TAG, "change group picture");
        // todo : handle result (and/or make the avatar thing a fragment)
        startActivityForResult(MenuUtils.constructIntent(this, GroupAvatarActivity.class, group),
                Constant.activityChangeGroupPicture);
    }

    @Override
    public void addOrganizer() {
        Log.e(TAG, "pick a new organizer");
    }

    @Override
    public void changePermissions() {
        Log.e(TAG, "change permissions");
    }

}