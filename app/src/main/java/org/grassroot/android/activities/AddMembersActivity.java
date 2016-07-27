package org.grassroot.android.activities;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import io.realm.RealmResults;
import java.util.Map;
import java.util.UUID;
import org.grassroot.android.R;
import org.grassroot.android.fragments.ContactSelectionFragment;
import org.grassroot.android.fragments.MemberListFragment;
import org.grassroot.android.interfaces.GroupConstants;
import org.grassroot.android.models.Contact;
import org.grassroot.android.models.GroupResponse;
import org.grassroot.android.models.Member;
import org.grassroot.android.services.GrassrootRestService;
import org.grassroot.android.services.GroupService;
import org.grassroot.android.utils.Constant;
import org.grassroot.android.utils.ErrorUtils;
import org.grassroot.android.utils.NetworkUtils;
import org.grassroot.android.utils.PermissionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import org.grassroot.android.utils.RealmUtils;

import io.realm.RealmList;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import rx.functions.Action1;

/**
 * Created by luke on 2016/05/05.
 */
public class AddMembersActivity extends AppCompatActivity implements
        ContactSelectionFragment.ContactSelectionListener {

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

    ProgressDialog progressDialog;

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
        groupUid = extras.getString(GroupConstants.UID_FIELD);
        groupName = extras.getString(GroupConstants.NAME_FIELD);
        groupPosition = extras.getInt(Constant.INDEX_FIELD);

        contactSelectionFragment = new ContactSelectionFragment();
        membersFromContacts = new HashMap<>();
        manuallyAddedMembers = new ArrayList<>();
        onMainScreen = true;

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage(getString(R.string.txt_pls_wait));
        progressDialog.setIndeterminate(true);
    }

    @OnClick(R.id.am_add_member_options)
    public void toggleAddMenu() {
        fabAddMemberOptions.setImageResource(menuOpen ? R.drawable.ic_add : R.drawable.ic_add_45d);
        addMemberFromContacts.setVisibility(menuOpen ? View.GONE : View.VISIBLE);
        addMemberManually.setVisibility(menuOpen ? View.GONE : View.VISIBLE);
        menuOpen = !menuOpen;
    }

    private void setupExistingMemberRecyclerView() {
        existingMemberListFragment = MemberListFragment.newInstance(groupUid, false, false, false, null, null);
        RealmUtils.loadListFromDB(Member.class,"groupUid", groupUid).subscribe(
            new Action1<List<Member>>() {
                @Override public void call(List<Member> members) {
                    contactSelectionFragment = ContactSelectionFragment.newInstance(Contact.convertFromMembers(members), false);
                    getSupportFragmentManager().beginTransaction()
                        .add(R.id.am_existing_member_list_container, existingMemberListFragment)
                        .commit();
                }
            }); // todo : make sure this doesn't cause reference overwrite issues
    }

    private void setupNewMemberRecyclerView() {
        newMemberListFragment = MemberListFragment.newInstance(null, true, false, false, null, new MemberListFragment.MemberClickListener() {
            @Override
            public void onMemberClicked(int position, String memberUid) {
                newMemberContextMenu(position, memberUid);
            }

            @Override
            public void onMemberDismissed(int position, String memberUid) {

            }
        });
        getSupportFragmentManager().beginTransaction()
                .add(R.id.am_new_member_list_container, newMemberListFragment)
                .commit();
    }

    private void newMemberContextMenu(final int position, final String memberUid) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setItems(R.array.cg_member_popup, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which == 0) {
                    editNewMember(position, memberUid);
                } else {
                    newMemberListFragment.removeMember(position);
                }
            }
        });
        builder.create().show();
    }

    private void editNewMember(final int position, final String memberUid) {
        Member member = RealmUtils.loadObjectFromDB(Member.class, "memberUid", memberUid);
        Intent i = new Intent(AddMembersActivity.this, AddContactManually.class);
        i.putExtra(GroupConstants.MEMBER_OBJECT, member);
        i.putExtra(Constant.INDEX_FIELD, position);
        startActivityForResult(i, Constant.activityManualMemberEdit);
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
        if (resultCode == RESULT_OK) {
            if (requestCode == Constant.activityManualMemberEntry) {
                Member newMember = new Member(data.getStringExtra("selectedNumber"), data.getStringExtra("name"),
                        GroupConstants.ROLE_ORDINARY_MEMBER, -1);
                newMember.setGroupUid(groupUid);
                newMember.setMemberUid(UUID.randomUUID().toString());
                newMember.setMemberGroupUid();
                newMember.setLocal(!NetworkUtils.isOnline(getApplicationContext()));
                RealmUtils.saveDataToRealmWithSubscriber(newMember);
                manuallyAddedMembers.add(newMember);
                newMemberListFragment.addMembers(Collections.singletonList(newMember));
                setNewMembersVisible();
            } else if (requestCode == Constant.activityManualMemberEdit) {
                Member revisedMember = data.getParcelableExtra(GroupConstants.MEMBER_OBJECT);
                int position = data.getIntExtra(Constant.INDEX_FIELD, -1);
                newMemberListFragment.updateMember(position, revisedMember);
            }
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
                progressDialog.show();
                if(NetworkUtils.isOnline(getApplicationContext())) {
                   postNewMembersToGroup(membersToAdd);
               } else {
                   GroupService.getInstance().addGroupMembersLocally(groupUid, membersToAdd);
                   Intent i = new Intent();
                   i.putExtra(GroupConstants.UID_FIELD, groupUid);
                   i.putExtra(GroupConstants.NAME_FIELD, groupPosition);
                   setResult(RESULT_OK, i);
                   progressDialog.dismiss();
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
        final String mobileNumber =
            RealmUtils.loadPreferencesFromDB().getMobileNumber();
        final String sessionCode = RealmUtils.loadPreferencesFromDB().getToken();
        GrassrootRestService.getInstance().getApi()
                .addGroupMembers(groupUid, mobileNumber, sessionCode, membersToAdd)
                .enqueue(new Callback<GroupResponse>() {
                    @Override
                    public void onResponse(Call<GroupResponse> call, final Response<GroupResponse> response) {
                        if (response.isSuccessful()) {
                            Map<String,Object> map = new HashMap<String, Object>();
                            map.put("groupUid",groupUid);
                            //map.put("isLocal",true);
                            //todo return members here from API
                            RealmUtils.removeObjectsFromDatabase(Member.class,map);
                            RealmUtils.saveDataToRealm(response.body().getGroups()).subscribe(new Action1() {
                                @Override public void call(Object o) {
                                    System.out.println(response.body().getGroups().first().getMembers().size());
                                    for(Member m : response.body().getGroups().first().getMembers()){
                                        m.setMemberGroupUid();
                                        RealmUtils.saveDataToRealmWithSubscriber(m);
                                    }
                                    Intent i = new Intent();
                                    i.putExtra(GroupConstants.UID_FIELD, groupUid);
                                    i.putExtra(Constant.INDEX_FIELD, groupPosition);
                                    setResult(RESULT_OK, i);
                                    progressDialog.dismiss();
                                    finish();
                                }
                            });
                        } else {
                            progressDialog.dismiss();
                            ErrorUtils.showSnackBar(amRlRoot, R.string.error_wrong_number, Snackbar.LENGTH_SHORT);
                        }
                    }

                    @Override
                    public void onFailure(Call<GroupResponse> call, Throwable t) {
                        ErrorUtils.handleNetworkError(AddMembersActivity.this, amRlRoot, t);
                    }
                });
    }

}
