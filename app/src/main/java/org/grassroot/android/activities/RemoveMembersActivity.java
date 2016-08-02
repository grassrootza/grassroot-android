package org.grassroot.android.activities;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.NavUtils;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.grassroot.android.R;
import org.grassroot.android.fragments.MemberListFragment;
import org.grassroot.android.fragments.dialogs.ConfirmCancelDialogFragment;
import org.grassroot.android.interfaces.GroupConstants;
import org.grassroot.android.models.GenericResponse;
import org.grassroot.android.models.Group;
import org.grassroot.android.services.GrassrootRestService;
import org.grassroot.android.services.GroupService;
import org.grassroot.android.utils.Constant;
import org.grassroot.android.utils.ErrorUtils;

import java.util.HashSet;
import java.util.Set;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import org.grassroot.android.utils.MenuUtils;
import org.grassroot.android.utils.RealmUtils;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import rx.Subscriber;

/**
 * Created by luke on 2016/05/18.
 */
public class RemoveMembersActivity extends PortraitActivity implements MemberListFragment.MemberClickListener {

    private static final String TAG = RemoveMembersActivity.class.getCanonicalName();

    private String groupUid;
    private String groupName;
    private int groupPosition;

    private MemberListFragment memberListFragment;

    private Set<String> membersToRemove;

    private ProgressDialog progressDialog;

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

        groupUid = extras.getString(GroupConstants.UID_FIELD);
        groupName = extras.getString(GroupConstants.NAME_FIELD);
        groupPosition = extras.getInt(Constant.INDEX_FIELD);

        membersToRemove = new HashSet<>();
        progressDialog = new ProgressDialog(this);
        progressDialog.setIndeterminate(true);
        progressDialog.setMessage(getString(R.string.txt_pls_wait));
        // todo: permissions, of course

        setUpToolbar();
        setUpMemberListFragment();
    }

    private void setUpToolbar() {
        tvHeader.setText(groupName);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setHomeButtonEnabled(true);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                navigateUp();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void setUpMemberListFragment() {
        memberListFragment = MemberListFragment.newInstance(groupUid, true, true, true, null, this);
        memberListFragment.setSelectedByDefault(true);

        getSupportFragmentManager().beginTransaction()
                .add(R.id.rm_member_list_container, memberListFragment)
                .commit();
    }

    @OnClick(R.id.rm_bt_cancel)
    public void cancel() {
        navigateUp();
    }

    @OnClick(R.id.rm_bt_save)
    public void saveAndExit() {
        // todo: localize
        if (membersToRemove.isEmpty()) {
            navigateUp();
        } else {
            final String message = String.format(getString(R.string.rm_confirm_number), membersToRemove.size());
            ConfirmCancelDialogFragment.newInstance(message, new ConfirmCancelDialogFragment.ConfirmDialogListener() {
                @Override
                public void doConfirmClicked() {
                    saveRemoval();
                }
            }).show(getSupportFragmentManager(), "confirm");
        }
    }

    private void navigateUp() {
        Intent intent = NavUtils.getParentActivityIntent(this);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        NavUtils.navigateUpTo(this, intent);
    }

    private void saveRemoval() {
        progressDialog.show();
        GroupService.getInstance().removeGroupMembers(groupUid, membersToRemove).subscribe(new Subscriber() {
            @Override
            public void onCompleted() {
                progressDialog.dismiss();
            }

            @Override
            public void onError(Throwable e) {
                Snackbar.make(root, "Error!", Snackbar.LENGTH_SHORT);
            }

            @Override
            public void onNext(Object o) {
                Intent i = new Intent();
                i.putExtra(GroupConstants.UID_FIELD, groupUid);
                i.putExtra(Constant.INDEX_FIELD, groupPosition);
                setResult(RESULT_OK, i);
                finish();
            }
        });

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