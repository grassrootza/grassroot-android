package org.grassroot.android.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.github.clans.fab.FloatingActionButton;
import com.github.clans.fab.FloatingActionMenu;
import org.grassroot.android.R;
import org.grassroot.android.interfaces.GroupConstants;
import org.grassroot.android.models.Contact;
import org.grassroot.android.services.GrassrootRestService;
import org.grassroot.android.models.GenericResponse;
import org.grassroot.android.models.Member;
import org.grassroot.android.fragments.ContactSelectionFragment;
import org.grassroot.android.fragments.MemberListFragment;
import org.grassroot.android.utils.Constant;
import org.grassroot.android.utils.ErrorUtils;
import org.grassroot.android.utils.PermissionUtils;
import org.grassroot.android.utils.PreferenceUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Created by luke on 2016/05/05.
 */
public class AddMembersActivity extends AppCompatActivity implements
        MemberListFragment.MemberListListener, MemberListFragment.MemberClickListener, ContactSelectionFragment.ContactSelectionListener {

    private static final String TAG = AddMembersActivity.class.getSimpleName();

    private String groupUid;
    private String groupName;
    private int groupPosition; // in case called from a list, so can update selectively

    private boolean newMemberListStarted;
    private MemberListFragment existingMemberListFragment;
    private MemberListFragment newMemberListFragment;

    private ContactSelectionFragment contactSelectionFragment;
    private Set<Contact> contactsToFilter;
    private HashMap<String, Member> membersFromContacts;
    private boolean onMainScreen;

    @BindView(R.id.rl_am_root)
    RelativeLayout amRlRoot;
    @BindView(R.id.am_txt_toolbar)
    TextView toolbarTitle;

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

        this.contactSelectionFragment = new ContactSelectionFragment();
        this.membersFromContacts = new HashMap<>();
        this.contactsToFilter = new HashSet<>();
        this.onMainScreen = true;
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
        getSupportFragmentManager().beginTransaction()
                .add(R.id.am_existing_member_list_container, existingMemberListFragment)
                .commit();
    }

    private void setupNewMemberRecyclerView() {
        newMemberListFragment = new MemberListFragment();
        newMemberListFragment.setGroupUid(null);
        newMemberListFragment.setCanDismissItems(true);
        newMemberListFragment.setShowSelected(true);

        getSupportFragmentManager().beginTransaction()
                .add(R.id.am_new_member_list_container, newMemberListFragment)
                .commit();
    }

    @OnClick(R.id.icon_add_from_contacts)
    public void addFromContacts() {
        addMemberOptions.close(true);
        if (!PermissionUtils.contactReadPermissionGranted(getApplicationContext())) {
            PermissionUtils.requestReadContactsPermission(this);
        } else {
            launchContactSelectionFragment();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (PermissionUtils.checkContactsPermissionGranted(requestCode, grantResults)) {
            launchContactSelectionFragment();
        }
    }

    private void launchContactSelectionFragment() {
        Set<Contact> contactsSelected = !newMemberListStarted ? new HashSet<Contact>() :
                new HashSet<>(Contact.convertFromMembers(newMemberListFragment.getSelectedMembers()));

        contactSelectionFragment.setContactsToPreselect(contactsSelected);
        Log.e(TAG, "setting contacts to filter ... set of " + contactsToFilter.size() + " elements");
        contactSelectionFragment.setContactsToFilter(contactsToFilter);

        onMainScreen = false;
        toolbarTitle.setText(R.string.cs_title);

        getSupportFragmentManager()
                .beginTransaction()
                .add(R.id.am_body_container, contactSelectionFragment)
                .addToBackStack(null)
                .commit();
    }

    private void closeContactSelectionFragment() {
        onMainScreen = true;
        toolbarTitle.setText(R.string.am_title);
        getSupportFragmentManager()
                .beginTransaction()
                .remove(contactSelectionFragment)
                .commit();
    }

    @OnClick(R.id.icon_add_member_manually)
    public void addMemberManually() {
        addMemberOptions.close(true);
        Intent intent = new Intent(this, AddContactManually.class);
        startActivityForResult(intent, Constant.activityManualMemberEntry); // todo: filter so can't add existing member
    }

    @Override
    public void onContactSelectionComplete(List<Contact> contactsAdded, Set<Contact> contactsRemoved) {
        Long start = SystemClock.currentThreadTimeMillis();

        if (!contactsAdded.isEmpty()) {
            setNewMembersVisible();
            List<Member> newMembers = new ArrayList<>();
            for (Contact contact : contactsAdded) {
                Member m = new Member(contact.selectedNumber, contact.name,
                        GroupConstants.ROLE_ORDINARY_MEMBER, contact.contact_ID);
                membersFromContacts.put(contact.contact_ID, m);
                newMembers.add(m);
            }
            newMemberListFragment.addMembers(newMembers);
            Log.d(TAG, String.format("added contacts to fragment, in all took %d msecs", SystemClock.currentThreadTimeMillis() - start));
        }

        if (!contactsRemoved.isEmpty()) {
            Log.e(TAG, "removing contacts! these ones : " + contactsRemoved.toString());
            for (Contact contact : contactsRemoved) {
                newMemberListFragment.removeMember(membersFromContacts.get(contact.contact_ID));
            }
        }

        closeContactSelectionFragment();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && requestCode == Constant.activityManualMemberEntry) {
            Member newMember = new Member(data.getStringExtra("selectedNumber"), data.getStringExtra("name"),
                    GroupConstants.ROLE_ORDINARY_MEMBER, null);
            newMemberListFragment.addMembers(Collections.singletonList(newMember));
            setNewMembersVisible();
        }
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

    @OnClick(R.id.am_iv_crossimage)
    public void closeMenu() {
        if (onMainScreen) {
            finish();
        } else {
            getSupportFragmentManager().popBackStack();
            onMainScreen = true;
            toolbarTitle.setText(R.string.am_title);
        }
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
            Log.d(TAG, "Exited with no members to add!");
            finish();
        }
    }

    private void postNewMembersToGroup(final List<Member> membersToAdd) {
        final String mobileNumber = PreferenceUtils.getuser_mobilenumber(getApplicationContext());
        final String sessionCode = PreferenceUtils.getuser_token(getApplicationContext());
        GrassrootRestService.getInstance().getApi()
                .addGroupMembers(groupUid, mobileNumber, sessionCode, membersToAdd)
                .enqueue(new Callback<GenericResponse>() {
                    @Override
                    public void onResponse(Call<GenericResponse> call, Response<GenericResponse> response) {
                        if (response.isSuccessful()) {
                            // todo : maybe, maybe a progress dialog
                            Intent i = new Intent();
                            i.putExtra(Constant.GROUPUID_FIELD, groupUid);
                            i.putExtra(Constant.INDEX_FIELD, groupPosition);
                            setResult(RESULT_OK, i);
                            finish();
                        } else {
                            ErrorUtils.showSnackBar(amRlRoot, R.string.generic_error, Snackbar.LENGTH_SHORT);
                        }
                    }

                    @Override
                    public void onFailure(Call<GenericResponse> call, Throwable t) {
                        ErrorUtils.handleNetworkError(AddMembersActivity.this, amRlRoot, t);
                    }
                });
    }

    @Override
    public void onMemberListInitiated(MemberListFragment fragment) {

    }

    @Override
    public void onMemberListPopulated(List<Member> memberList) {
        Log.e(TAG, "returned members: " + memberList);
        contactsToFilter = new HashSet<>(Contact.convertFromMembers(memberList));
        Log.e(TAG, "contacts created: " + contactsToFilter);
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
