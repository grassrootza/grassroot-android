package com.techmorphosis.grassroot.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
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
import com.techmorphosis.grassroot.utils.PreferenceUtils;
import com.techmorphosis.grassroot.utils.UtilClass;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
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
public class AddMembersActivity extends AppCompatActivity implements MemberListFragment.MemberListListener, MemberListFragment.MemberClickListener {

    private static final String TAG = AddMembersActivity.class.getSimpleName();
    private static final String EXISTING_ID = "existingMembers";
    private static final String NEW_ID = "newMembers";

    private String groupUid;
    private String groupName;
    private int groupPosition; // in case called from a list, so can update selectively

    private boolean newMemberListStarted;
    private GrassrootRestService grassrootRestService;
    private MemberListFragment existingMemberListFragment;
    private MemberListFragment newMemberListFragment;
    private HashMap<String, Member> membersFromContacts;

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
        } else {
            Log.d(TAG, "inside addMembersActivity ... passed extras bundle = " + extras.toString());
            init(extras);
            groupNameView.setText(groupName); // todo: handle long group names
            setupFloatingActionButtons();
            setupExistingMemberRecyclerView();
            setupNewMemberRecyclerView();
        }
    }

    private void init(Bundle extras) {
        this.groupUid = extras.getString(Constant.GROUPUID_FIELD);
        this.groupName = extras.getString(Constant.GROUPNAME_FIELD);
        this.groupPosition = extras.getInt(Constant.INDEX_FIELD);
        this.membersFromContacts = new HashMap<>();
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
        newMemberListFragment.setCanDismissItems(true);
        newMemberListFragment.setShowSelected(true);

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
            ArrayList<Contact> contactsSelected = !newMemberListStarted ? new ArrayList<Contact>() :
                    new ArrayList<>(Contact.convertFromMembers(newMemberListFragment.getSelectedMembers()));
            UtilClass.callPhoneBookActivity(this, contactsSelected,
                    new ArrayList<>(Contact.convertFromMembers(existingMemberListFragment.getMemberList())));
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
        if (newMemberListFragment != null) {
            final List<Member> membersToAdd = newMemberListFragment.getSelectedMembers();
            if (membersToAdd != null && membersToAdd.size() > 0) {
                postNewMembersToGroup(membersToAdd);
            } else {
                // todo :show a snack bar or something
                finish();
            }
        } else {
            Log.i(TAG, "Exited with no members to add!");
            finish();
        }
    }

    private void postNewMembersToGroup(final List<Member> membersToAdd) {
        grassrootRestService = new GrassrootRestService(this);
        String mobileNumber = PreferenceUtils.getuser_mobilenumber(getApplicationContext());
        String sessionCode = PreferenceUtils.getuser_token(getApplicationContext());

        grassrootRestService.getApi()
                .addGroupMembers(groupUid, mobileNumber, sessionCode, membersToAdd)
                .enqueue(new Callback<GenericResponse>() {
                    @Override
                    public void onResponse(Call<GenericResponse> call, Response<GenericResponse> response) {
                        if (response.isSuccessful()) {
                            // todo : maybe, maybe a progress bar
                            Log.i(TAG, "Finished adding these members: " + membersToAdd.toString());
                            Intent i = new Intent();
                            i.putExtra(Constant.GROUPUID_FIELD, groupUid);
                            i.putExtra(Constant.INDEX_FIELD, groupPosition);
                            setResult(RESULT_OK, i);
                            finish();
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
                addContactsToMembers(data);
            } else if (requestCode == Constant.activityManualMemberEntry) {
                processManualMemberResult(data);
            }
        }
    }

    private void addContactsToMembers(Intent data) {
        Long start = SystemClock.currentThreadTimeMillis();
        ArrayList<Contact> returnedContacts = data.getParcelableArrayListExtra(Constant.contactsAdded);
        if (!returnedContacts.isEmpty()) {
            setNewMembersVisible();
            List<Member> newMembers = new ArrayList<>();
            for (Contact contact : returnedContacts) {
                Member m = new Member(contact.selectedNumber, contact.name, Constant.ROLE_ORDINARY_MEMBER, contact.contact_ID);
                membersFromContacts.put(contact.contact_ID, m);
                newMembers.add(m);
            }
            newMemberListFragment.addMembers(newMembers);
            Log.d(TAG, String.format("added contacts to fragment, in all took %d msecs", SystemClock.currentThreadTimeMillis() - start));
        }

        ArrayList<Contact> removedContacts = data.getParcelableArrayListExtra(Constant.contactsRemoved);
        if (!removedContacts.isEmpty()) {
            Log.e(TAG, "removing contacts! these ones : " + removedContacts.toString());
            for (Contact contact : removedContacts) {
                newMemberListFragment.removeMember(membersFromContacts.get(contact.contact_ID));
            }
        }
    }

    private void processManualMemberResult(Intent data) {
        Member newMember = new Member(data.getStringExtra("selectedNumber"), data.getStringExtra("name"),
                Constant.ROLE_ORDINARY_MEMBER, null);
        newMemberListFragment.addMembers(Collections.singletonList(newMember));
        setNewMembersVisible();
    }

    private void setNewMembersVisible() {
        if (!newMemberListStarted) {
            newMembersTitle.setVisibility(View.VISIBLE);
            newMemberContainer.setVisibility(View.VISIBLE);
            existingMemberContainer.setVisibility(View.VISIBLE);
            newMemberListStarted = true;
        }
    }

    private void setNewMembersInvisible() {
        if (newMemberListStarted) {
            newMembersTitle.setVisibility(View.GONE);
            newMemberContainer.setVisibility(View.GONE);
            newMemberListStarted = false;
        }
    }

    @Override
    public void onMemberListInitiated(MemberListFragment fragment) {

    }

    @Override
    public void onMemberDismissed(int position, String memberUid) {
        // todo : deal with this (do we need to?)
    }

    @Override
    public void onMemberClicked(int position, String memberUid) {
        // todo : deal with this
    }
}
