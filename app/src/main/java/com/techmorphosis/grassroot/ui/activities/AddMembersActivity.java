package com.techmorphosis.grassroot.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.github.clans.fab.FloatingActionButton;
import com.github.clans.fab.FloatingActionMenu;
import com.techmorphosis.grassroot.R;
import com.techmorphosis.grassroot.models.Contact;
import com.techmorphosis.grassroot.services.GrassrootRestService;
import com.techmorphosis.grassroot.services.model.GenericResponse;
import com.techmorphosis.grassroot.services.model.Member;
import com.techmorphosis.grassroot.ui.fragments.MemberListFragment;
import com.techmorphosis.grassroot.utils.Constant;
import com.techmorphosis.grassroot.utils.ErrorUtils;
import com.techmorphosis.grassroot.utils.PermissionUtils;
import com.techmorphosis.grassroot.utils.SettingPreference;
import com.techmorphosis.grassroot.utils.UtilClass;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Created by luke on 2016/05/05.
 */
public class AddMembersActivity extends AppCompatActivity implements MemberListFragment.MemberListListener {

    private static final String TAG = AddMembersActivity.class.getSimpleName();
    private static final String EXISTING_ID = "existingMembers";
    private static final String NEW_ID = "newMembers";

    private String groupUid;
    private String groupName;
    private List<Member> membersToAdd;
    private boolean newMemberListStarted;

    private GrassrootRestService grassrootRestService;
    private MemberListFragment existingMemberListFragment;
    private MemberListFragment newMemberListFragment;

    @BindView(R.id.rl_am_root)
    RelativeLayout amRlRoot;

    @BindView(R.id.am_add_member_options)
    FloatingActionMenu addMemberOptions;
    @BindView(R.id.icon_add_from_contacts)
    FloatingActionButton addMemberFromContacts;
    @BindView(R.id.icon_add_member_manually)
    FloatingActionButton addMemberManually;

    @BindView(R.id.am_tv_groupname)
    TextView groupNameView;

    @BindView(R.id.am_new_member_list_container)
    RelativeLayout newMemberContainer;
    @BindView(R.id.tv_am_new_members_title)
    TextView newMembersTitle;

    @BindView(R.id.am_existing_member_list_container)
    RelativeLayout existingMemberContainer;
    @BindView(R.id.tv_am_existing_members_title)
    TextView existingMembersTitle;

    @BindView(R.id.am_bt_save)
    Button btnSave;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group__addmembers);
        ButterKnife.bind(this);

        Bundle extras = getIntent().getExtras();
        if (extras == null) {
            Log.d(TAG, "ERROR! Null extras passed to add members activity, cannot execute, aborting");
            finish();
            return;
        } else {
            Log.d(TAG, "inside addMembersActivity ... passed extras bundle = " + extras.toString());
            init(extras);
            groupNameView.setText(groupName); // todo: handle long group names
            setupFloatingActionButtons();
            setupExistingMemberRecyclerView();
        }
    }

    private void init(Bundle extras) {
        this.groupUid = extras.getString(Constant.GROUPUID_FIELD);
        this.groupName = extras.getString(Constant.GROUPNAME_FIELD);
        this.membersToAdd = new ArrayList<>();
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

    private void setupExistingMemberRecyclerView() {
        existingMemberListFragment = new MemberListFragment();
        existingMemberListFragment.setGroupUid(groupUid);
        existingMemberListFragment.setID(EXISTING_ID);
        getSupportFragmentManager().beginTransaction()
                .add(R.id.am_existing_member_list_container, existingMemberListFragment)
                .commit();
    }

    private void setupNewMemberRecyclerView() {
        newMemberListFragment = new MemberListFragment();
        newMemberListFragment.setGroupUid(null);
        newMemberListFragment.setID(NEW_ID);
        getSupportFragmentManager().beginTransaction()
                .add(R.id.am_new_member_list_container, newMemberListFragment)
                .commit();
    }

    @OnClick(R.id.am_iv_crossimage)
    public void closeMenu() { finish(); }

    @OnClick(R.id.icon_add_from_contacts)
    public void addFromContacts() {
        addMemberOptions.close(true);
        if (!PermissionUtils.contactReadPermissionGranted(getApplicationContext())) {
            PermissionUtils.requestReadContactsPermission(this);
        } else {
            // todo: pass back the added members as well
            UtilClass.callPhoneBookActivity(this, new ArrayList<Contact>(),
                    new ArrayList<>(Contact.convertFromMembers(existingMemberListFragment.getMemberList(), false)));
        }
    }

    @OnClick(R.id.icon_add_member_manually)
    public void addMemberManually() {
        addMemberOptions.close(true);
        Intent intent = new Intent(this, AddContactManually.class);
        startActivityForResult(intent, Constant.activityManualMemberEntry); // todo: filter so can't add existing member
    }

    @OnClick(R.id.am_bt_save)
    public void commitResultsAndExit() {
        if (membersToAdd != null && membersToAdd.size() > 0) {
            postNewMembersToGroup();
            Log.d(TAG, "Exiting with these members to add: " + membersToAdd.toString());
        } else {
            Log.d(TAG, "Exited with no members to add!");
        }
        finish();
    }

    private void postNewMembersToGroup() {
        grassrootRestService = new GrassrootRestService(this);
        String mobileNumber = SettingPreference.getuser_mobilenumber(getApplicationContext());
        String sessionCode = SettingPreference.getuser_token(getApplicationContext());

        grassrootRestService.getApi()
                .addGroupMembers(groupUid, mobileNumber, sessionCode, membersToAdd)
                .enqueue(new Callback<GenericResponse>() {
                    @Override
                    public void onResponse(Call<GenericResponse> call, Response<GenericResponse> response) {
                        if (response.isSuccessful()) {
                            Log.d(TAG, "Finished adding members");
                            // todo: refresh the task list intelligently
                        } else {
                            // todo: handle error gracefully
                        }
                    }

                    @Override
                    public void onFailure(Call<GenericResponse> call, Throwable t) {
                        ErrorUtils.handleNetworkError(AddMembersActivity.this, amRlRoot, t);
                    }
                });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == Constant.alertAskForContactPermission && grantResults.length > 0) {
            PermissionUtils.checkPermGrantedAndLaunchPhoneBook(this, grantResults[0], new ArrayList<Contact>());
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == Constant.activityContactSelection && data != null) {
                ArrayList<Contact> returnedContacts = data.getParcelableArrayListExtra(Constant.selectedContacts);
                Log.d(TAG, "retrieved contacts from activity! number of user selected = " + returnedContacts.size());
                addContactsToMembers(returnedContacts);
            } else if (requestCode == Constant.activityManualMemberEntry) {
                Log.d(TAG, "got contact from manual entry!");
                Member newMember = new Member(data.getStringExtra("selectedNumber"), data.getStringExtra("name"),
                        Constant.ROLE_ORDINARY_MEMBER, null);
                membersToAdd.add(newMember);
            }
        }

        // todo: handle duplication & change
        if (!newMemberListStarted && membersToAdd.size() > 0) {
            setupNewMemberRecyclerView();
            newMembersTitle.setVisibility(View.VISIBLE);
            newMemberContainer.setVisibility(View.VISIBLE);
            existingMemberContainer.setVisibility(View.VISIBLE);
        }
    }

    private void addContactsToMembers(List<Contact> contacts) {
        // todo: handle removal ...
        for (Contact contact : contacts) {
            Member newMember = new Member(contact.selectedNumber, contact.name, Constant.ROLE_ORDINARY_MEMBER, contact.contact_ID);
            membersToAdd.add(newMember);
        }
    }

    @Override
    public void onMemberListInitiated(MemberListFragment fragment) {
        if (NEW_ID.equals(fragment.getID())) {
            newMemberListFragment.setMemberList(membersToAdd);
            newMemberListStarted = true;
        } else if (EXISTING_ID.equals(fragment.getID())) {

        } else {
            // todo: maybe use this for the other one too?
            throw new UnsupportedOperationException("This should only be caring about the memberlists in the activity");
        }
    }
}
