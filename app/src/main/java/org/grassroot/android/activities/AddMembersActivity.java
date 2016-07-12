package org.grassroot.android.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import io.realm.RealmList;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.grassroot.android.R;
import org.grassroot.android.fragments.ContactSelectionFragment;
import org.grassroot.android.fragments.MemberListFragment;
import org.grassroot.android.interfaces.GroupConstants;
import org.grassroot.android.models.Contact;
import org.grassroot.android.models.GenericResponse;
import org.grassroot.android.models.Member;
import org.grassroot.android.services.GrassrootRestService;
import org.grassroot.android.utils.Constant;
import org.grassroot.android.utils.ErrorUtils;
import org.grassroot.android.utils.NetworkUtils;
import org.grassroot.android.utils.PermissionUtils;
import org.grassroot.android.utils.PreferenceUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import org.grassroot.android.utils.RealmUtils;
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
    private HashMap<Integer, Member> membersFromContacts;
    private List<Member> manuallyAddedMembers;

    private boolean onMainScreen;
    private boolean menuOpen;

    @BindView(R.id.rl_am_root) RelativeLayout amRlRoot;
    @BindView(R.id.am_txt_toolbar) TextView toolbarTitle;

    @BindView(R.id.am_add_member_options) FloatingActionButton fabAddMemberOptions;
    @BindView(R.id.ll_add_member_contacts) LinearLayout addMemberFromContacts;
    @BindView(R.id.ll_add_member_manually) LinearLayout addMemberManually;

    @BindView(R.id.am_tv_groupname) TextView groupNameView;
    @BindView(R.id.tv_am_new_members_title) TextView newMembersTitle;

    @BindView(R.id.am_new_member_list_container) RelativeLayout newMemberContainer;
    @BindView(R.id.am_existing_member_list_container) RelativeLayout existingMemberContainer;

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
            setupExistingMemberRecyclerView();
            setupNewMemberRecyclerView();
        }
    }

    private void init(Bundle extras) {
        groupUid = extras.getString(Constant.GROUPUID_FIELD);
        groupName = extras.getString(Constant.GROUPNAME_FIELD);
        groupPosition = extras.getInt(Constant.INDEX_FIELD);

        contactSelectionFragment = new ContactSelectionFragment();
        membersFromContacts = new HashMap<>();
        manuallyAddedMembers = new ArrayList<>();
        onMainScreen = true;
    }

    @OnClick(R.id.am_add_member_options)
    public void toggleAddMenu() {
        fabAddMemberOptions.setImageResource(menuOpen ? R.drawable.ic_add : R.drawable.ic_add_45d);
        addMemberFromContacts.setVisibility(menuOpen ? View.GONE : View.VISIBLE);
        addMemberManually.setVisibility(menuOpen ? View.GONE : View.VISIBLE);
        menuOpen = !menuOpen;
    }

    private void setupExistingMemberRecyclerView() {
        existingMemberListFragment = MemberListFragment.newInstance(groupUid, false, false, this, this, null);
        getSupportFragmentManager().beginTransaction()
                .add(R.id.am_existing_member_list_container, existingMemberListFragment)
                .commit();
    }

    @Override
    public void onMemberListPopulated(List<Member> memberList) {
        contactSelectionFragment = ContactSelectionFragment.newInstance(Contact.convertFromMembers(memberList), false);
    }

    @Override
    public void onMemberListDone() {

    }

    private void setupNewMemberRecyclerView() {
        newMemberListFragment = MemberListFragment.newInstance(null, true, true, this, this, null);
        getSupportFragmentManager().beginTransaction()
                .add(R.id.am_new_member_list_container, newMemberListFragment)
                .commit();
    }

    @OnClick(R.id.ll_add_member_contacts)
    public void addFromContacts() {
        toggleAddMenu();
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
        if (contactSelectionFragment != null) { // just in case we are still waiting for member list to be populated
            onMainScreen = false;
            toolbarTitle.setText(R.string.cs_title);
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.am_body_container, contactSelectionFragment)
                    .addToBackStack(null)
                    .commit();
        }
    }

    private void closeContactSelectionFragment() {
        onMainScreen = true;
        toolbarTitle.setText(R.string.am_title);
        getSupportFragmentManager()
                .beginTransaction()
                .remove(contactSelectionFragment)
                .commit();
    }

    @OnClick(R.id.ll_add_member_manually)
    public void addMemberManually() {
        toggleAddMenu();
        Intent intent = new Intent(this, AddContactManually.class);
        startActivityForResult(intent, Constant.activityManualMemberEntry); // todo: filter so can't add existing member
    }

    @Override
    public void onContactSelectionComplete(List<Contact> contactsSelected) {
        Long start = SystemClock.currentThreadTimeMillis();

        List<Member> selectedMembers = new ArrayList<>(manuallyAddedMembers);
        for (Contact c : contactsSelected) {
            if (membersFromContacts.containsKey(c.id)) {
                selectedMembers.add(membersFromContacts.get(c.id));
            } else {
                Member m = new Member(c.selectedMsisdn, c.getDisplayName(), GroupConstants.ROLE_ORDINARY_MEMBER, c.id, true);
                membersFromContacts.put(c.id, m);
                selectedMembers.add(m);
            }
        }
        newMemberListFragment.transitionToMemberList(selectedMembers);

        Log.d(TAG, String.format("added contacts to fragment, in all took %d msecs", SystemClock.currentThreadTimeMillis() - start));
        closeContactSelectionFragment();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && requestCode == Constant.activityManualMemberEntry) {
            Member newMember = new Member(data.getStringExtra("selectedNumber"), data.getStringExtra("name"),
                    GroupConstants.ROLE_ORDINARY_MEMBER, -1);
            newMember.setGroupUid(groupUid);
            newMember.setMemberUid(UUID.randomUUID().toString());
            newMember.setMemberGroupUid();
            newMember.setLocal(!NetworkUtils.isNetworkAvailable(getApplicationContext()));
            manuallyAddedMembers.add(newMember);
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
               if(NetworkUtils.isNetworkAvailable(getApplicationContext())) {
                   postNewMembersToGroup(membersToAdd);
               }else{
                   RealmUtils.saveDataToRealm(membersToAdd);
                   Intent i = new Intent();
                   i.putExtra(Constant.GROUPUID_FIELD, groupUid);
                   i.putExtra(Constant.INDEX_FIELD, groupPosition);
                   setResult(RESULT_OK, i);
                   finish();
               }
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
        final String mobileNumber = PreferenceUtils.getUserPhoneNumber(getApplicationContext());
        final String sessionCode = PreferenceUtils.getAuthToken(getApplicationContext());
        GrassrootRestService.getInstance().getApi()
                .addGroupMembers(groupUid, mobileNumber, sessionCode, membersToAdd)
                .enqueue(new Callback<GenericResponse>() {
                    @Override
                    public void onResponse(Call<GenericResponse> call, Response<GenericResponse> response) {
                        if (response.isSuccessful()) {
                            // todo : maybe, maybe a progress dialog
                            Map<String,Object> map = new HashMap<String, Object>();
                            map.put("groupUid",groupUid);
                            map.put("isLocal",true);
                            //todo return members here from API
                            RealmUtils.removeObjectsFromDatabase(Member.class,map);
                            RealmUtils.saveDataToRealm(membersToAdd);
                            Intent i = new Intent();
                            i.putExtra(Constant.GROUPUID_FIELD, groupUid);
                            i.putExtra(Constant.INDEX_FIELD, groupPosition);
                            setResult(RESULT_OK, i);
                            finish();
                        } else {
                            ErrorUtils.showSnackBar(amRlRoot, R.string.error_wrong_number, Snackbar.LENGTH_SHORT);
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
    public void onMemberDismissed(int position, String memberUid) {
        // todo : deal with this (do we need to?)
    }

    @Override
    public void onMemberClicked(int position, String memberUid) {
        // todo : deal with this
    }
}
