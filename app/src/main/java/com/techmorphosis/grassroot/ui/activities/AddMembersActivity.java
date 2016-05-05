package com.techmorphosis.grassroot.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.github.clans.fab.FloatingActionButton;
import com.github.clans.fab.FloatingActionMenu;
import com.techmorphosis.grassroot.R;
import com.techmorphosis.grassroot.adapters.MemberListAdapter;
import com.techmorphosis.grassroot.models.ContactsModel;
import com.techmorphosis.grassroot.services.GrassrootRestService;
import com.techmorphosis.grassroot.services.model.MemberList;
import com.techmorphosis.grassroot.utils.Constant;
import com.techmorphosis.grassroot.utils.PermissionUtils;
import com.techmorphosis.grassroot.utils.SettingPreference;
import com.techmorphosis.grassroot.utils.UtilClass;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import rx.Observer;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Created by luke on 2016/05/05.
 */
public class AddMembersActivity extends AppCompatActivity {

    private static final String TAG = AddMembersActivity.class.getSimpleName();

    private String groupUid;
    private String groupName;

    private GrassrootRestService grassrootRestService;

    private MemberListAdapter memberListAdapter;
    private ArrayList<ContactsModel> memberList;

    @BindView(R.id.am_members_list)
    RecyclerView memberRecyclerView;

    @BindView(R.id.am_add_member_options)
    FloatingActionMenu addMemberOptions;
    @BindView(R.id.icon_add_from_contacts)
    FloatingActionButton addMemberFromContacts;
    @BindView(R.id.icon_add_member_manually)
    FloatingActionButton addMemberManually;

    @BindView(R.id.am_tv_groupname)
    TextView groupNameView;

    @BindView(R.id.am_bt_save)
    Button btnSave;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group__addmembers);
        ButterKnife.bind(this);

        Bundle extras = getIntent().getExtras();
        if (extras == null) {
            Log.e(TAG, "ERROR! Null extras passed to add members activity, cannot execute, aborting");
            finish();
            return;
        } else {
            Log.e(TAG, "inside addMembersActivity ... passed extras bundle = " + extras.toString());
            init(extras);
            groupNameView.setText(groupName); // todo: handle long group names
            setupFloatingActionButtons();
            setupMemberRecyclerView();
            Log.d(TAG, "inside addMembersActivity ... created it!");
        }
    }

    private void init(Bundle extras) {
        this.groupUid = extras.getString(Constant.GROUPUID_FIELD);
        this.groupName = extras.getString(Constant.GROUPNAME_FIELD);
        this.grassrootRestService = new GrassrootRestService();
        this.memberList = new ArrayList<>();
    }

    private void setupFloatingActionButtons() {
        addMemberOptions.setOnMenuToggleListener(new FloatingActionMenu.OnMenuToggleListener() {
            @Override
            public void onMenuToggle(boolean opened) {
                addMemberFromContacts.setVisibility(opened ? View.VISIBLE : View.GONE);
                addMemberManually.setVisibility(opened ? View.VISIBLE : View.GONE);
            }
        });
    }

    private void setupMemberRecyclerView() {
        populateMemberList();
        this.memberRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        this.memberListAdapter = new MemberListAdapter(memberList, getApplicationContext());
        memberRecyclerView.setAdapter(memberListAdapter);
    }

    @OnClick(R.id.iv_crossimage)
    public void closeMenu() { finish(); }

    @OnClick(R.id.icon_add_from_contacts)
    public void addFromContacts() {
        addMemberOptions.close(true);
        if (!PermissionUtils.contactReadPermissionGranted(getApplicationContext())) {
            PermissionUtils.requestReadContactsPermission(this);
        } else {
            UtilClass.callPhoneBookActivity(this, memberList);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == Constant.alertAskForContactPermission && grantResults.length > 0) {
            PermissionUtils.checkPermGrantedAndLaunchPhoneBook(this, grantResults[0], memberList);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

    }

    private void populateMemberList() {

        String userPhoneNumber = SettingPreference.getuser_mobilenumber(this);
        String userSessionCode = SettingPreference.getuser_token(this);

        Log.d(TAG, "inside addMembersActivity, about to call API, phone number = " + userPhoneNumber);

        grassrootRestService.getApi()
                .getGroupMembers(groupUid, userPhoneNumber, userSessionCode)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<MemberList>() {
                    @Override
                    public void onCompleted() {
                        Log.d(TAG, "inside AddMembersActivity ... Response call worked!");
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.d(TAG, "inside AddMembersActivity ... Response call didn't work!");

                    }

                    @Override
                    public void onNext(MemberList memberList) {
                        Log.d(TAG, "inside AddMembersActivity ... got a response!");

                    }
                });
    }

}
