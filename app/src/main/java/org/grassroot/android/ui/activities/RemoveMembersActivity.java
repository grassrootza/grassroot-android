package org.grassroot.android.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.grassroot.android.R;
import org.grassroot.android.interfaces.AlertDialogListener;
import org.grassroot.android.services.GrassrootRestService;
import org.grassroot.android.services.model.GenericResponse;
import org.grassroot.android.ui.fragments.AlertDialogFragment;
import org.grassroot.android.ui.fragments.MemberListFragment;
import org.grassroot.android.utils.Constant;
import org.grassroot.android.utils.ErrorUtils;
import org.grassroot.android.utils.PreferenceUtils;
import org.grassroot.android.utils.UtilClass;

import java.util.HashSet;
import java.util.Set;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Created by luke on 2016/05/18.
 */
public class RemoveMembersActivity extends PortraitActivity implements MemberListFragment.MemberListListener,
        MemberListFragment.MemberClickListener {

    private static final String TAG = RemoveMembersActivity.class.getCanonicalName();

    private String groupUid;
    private String groupName;
    private int groupPosition;

    private MemberListFragment memberListFragment;
    private AlertDialogFragment dialogFragment;

    private GrassrootRestService grassrootRestService;
    private Set<String> membersToRemove;

    @BindView(R.id.rl_rm_root)
    RelativeLayout root;
    @BindView(R.id.rm_toolbar)
    Toolbar toolbar;
    @BindView(R.id.rm_tv_groupname)
    TextView tvHeader;
    @BindView(R.id.rm_bt_cancel)
    Button btCancel;
    @BindView(R.id.rm_bt_save)
    Button btSave;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group__removemembers);
        ButterKnife.bind(this);

        Bundle extras = getIntent().getExtras();
        if (extras == null)
            throw new UnsupportedOperationException("Must pass extras to remove members activity");

        groupUid = extras.getString(Constant.GROUPUID_FIELD);
        groupName = extras.getString(Constant.GROUPNAME_FIELD);
        groupPosition = extras.getInt(Constant.INDEX_FIELD);

        membersToRemove = new HashSet<>();
        // todo: permissions, of course

        setUpToolbar();
        setUpMemberListFragment();

        grassrootRestService = new GrassrootRestService(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // todo: check for permissions
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_group_members, menu);
        return true;
    }

    private void setUpToolbar() {
        tvHeader.setText(groupName);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setHomeButtonEnabled(true);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    private void setUpMemberListFragment() {
        memberListFragment = new MemberListFragment();
        memberListFragment.setGroupUid(groupUid);
        memberListFragment.setCanDismissItems(true);
        memberListFragment.setShowSelected(true);
        memberListFragment.setSelectedByDefault(true);

        getSupportFragmentManager().beginTransaction()
                .add(R.id.rm_member_list_container, memberListFragment)
                .commit();
    }

    @OnClick(R.id.rm_bt_cancel)
    public void cancel() {
        // todo: maybe use proper intent/going up
        finish();
    }

    @OnClick(R.id.rm_bt_save)
    public void saveAndExit() {
        // todo: localize
        if (membersToRemove.isEmpty()) {
            finish();
        } else {
            final String message = String.format("You are about to remove %d members. Are you sure?", membersToRemove.size());
            dialogFragment = UtilClass.showAlertDialog(getFragmentManager(),getString(R.string.Confirm_Removal), message, "Cancel", "Confirm", true,
                    new AlertDialogListener() {
                        @Override
                        public void setLeftButton() {
                            dialogFragment.dismiss();
                        }

                        @Override
                        public void setRightButton() {
                            saveRemoval();
                            dialogFragment.dismiss();
                        }
                    });
        }
    }

    private void saveRemoval() {
        final String phoneNumber = PreferenceUtils.getuser_mobilenumber(this);
        final String code = PreferenceUtils.getuser_token(this);
        grassrootRestService.getApi().removeGroupMembers(phoneNumber, code, groupUid, membersToRemove)
                .enqueue(new Callback<GenericResponse>() {
                    @Override
                    public void onResponse(Call<GenericResponse> call, Response<GenericResponse> response) {
                        Intent i = new Intent();
                        i.putExtra(Constant.GROUPUID_FIELD, groupUid);
                        i.putExtra(Constant.INDEX_FIELD, groupPosition);
                        setResult(RESULT_OK, i);
                        finish();
                        // todo: display on next snack bar
                    }

                    @Override
                    public void onFailure(Call<GenericResponse> call, Throwable t) {
                        ErrorUtils.handleNetworkError(RemoveMembersActivity.this, root, t);
                    }
                });
    }

    @Override
    public void onMemberListInitiated(MemberListFragment fragment) {
        // todo : reexamine the case for this listener
        Log.e(TAG, "Fragment initiated ...");
    }

    @Override
    public void onMemberDismissed(int position, String memberUid) {
        membersToRemove.add(memberUid);
    }

    @Override
    public void onMemberClicked(int position, String memberUid) {
        if (membersToRemove.contains(memberUid)) {
            membersToRemove.remove(memberUid);
        } else {
            membersToRemove.add(memberUid);
        }
    }

}
