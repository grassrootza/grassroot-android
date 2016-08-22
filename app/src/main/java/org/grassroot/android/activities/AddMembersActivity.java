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

import org.grassroot.android.R;
import org.grassroot.android.fragments.ContactSelectionFragment;
import org.grassroot.android.fragments.MemberListFragment;
import org.grassroot.android.interfaces.GroupConstants;
import org.grassroot.android.interfaces.NavigationConstants;
import org.grassroot.android.models.Contact;
import org.grassroot.android.models.Member;
import org.grassroot.android.models.exceptions.ApiCallException;
import org.grassroot.android.models.exceptions.InvalidNumberException;
import org.grassroot.android.services.ContactService;
import org.grassroot.android.services.GroupService;
import org.grassroot.android.utils.Constant;
import org.grassroot.android.utils.ErrorUtils;
import org.grassroot.android.utils.IntentUtils;
import org.grassroot.android.utils.NetworkUtils;
import org.grassroot.android.utils.PermissionUtils;
import org.grassroot.android.utils.RealmUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
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

    private List<Contact> existingMemberContacts;

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
            // cannot proceed, so abort
            finish();
        } else {
            init(extras);
            groupNameView.setText(groupName);
            existingMemberListFragment = MemberListFragment.newInstance(groupUid, false, false, null, true, null);
            loadMembersIntoExistingMemberFragment(true);
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

    private void loadMembersIntoExistingMemberFragment(final boolean addFragment) {
        Map<String, Object> validMemberMap = new HashMap<>();
        validMemberMap.put("groupUid", groupUid);
        validMemberMap.put("isNumberInvalid", false);

        RealmUtils.loadListFromDB(Member.class, validMemberMap).subscribe(
            new Action1<List<Member>>() {
                @Override public void call(List<Member> members) {
                    existingMemberContacts = Contact.convertFromMembers(members);
                    if (addFragment) {
                        getSupportFragmentManager().beginTransaction()
                            .add(R.id.am_existing_member_list_container, existingMemberListFragment)
                            .commit();
                    } else {
                        existingMemberListFragment.transitionToMemberList(members);
                    }
                }
            });
    }

    private void setupNewMemberRecyclerView() {
        newMemberListFragment = MemberListFragment.newInstance(null, true, false, null, false,
            new MemberListFragment.MemberClickListener() {
            @Override
            public void onMemberClicked(int position, String memberUid) {
                newMemberContextMenu(position, memberUid);
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

        // don't fully trust Android callbacks, so adding a check on position to avoid uncaught exception
        if (member == null && position < newMemberListFragment.getSelectedMembers().size()) {
            member = newMemberListFragment.getSelectedMembers().get(position);
        }

        Intent i = new Intent(AddMembersActivity.this, AddContactManually.class);
        i.putExtra(GroupConstants.MEMBER_OBJECT, member);
        i.putExtra(Constant.INDEX_FIELD, position);
        startActivityForResult(i, NavigationConstants.MANUAL_MEMBER_EDIT);
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
        if (existingMemberContacts != null) {
            ContactService.getInstance().syncContactList(existingMemberContacts, false, AndroidSchedulers.mainThread())
                .subscribe(new Action1<Boolean>() {
                    @Override
                    public void call(Boolean aBoolean) {
                        onMainScreen = false;
                        toolbarTitle.setText(R.string.cs_title);
                        getSupportFragmentManager()
                            .beginTransaction()
                            .add(R.id.am_body_container, contactSelectionFragment)
                            .addToBackStack(null)
                            .commit();
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        // todo : show a snackbar or a dialogue with an error
                    }
                });
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
        startActivityForResult(intent, NavigationConstants.MANUAL_MEMBER_ENTRY); // todo: filter so can't add existing member
    }

    @Override
    public void onContactSelectionComplete(List<Contact> contactsSelected) {
        Long start = SystemClock.currentThreadTimeMillis();
        List<Member> selectedMembers = new ArrayList<>(manuallyAddedMembers);
        for (Contact c : contactsSelected) {
            if (membersFromContacts.containsKey(c.id)) {
                selectedMembers.add(membersFromContacts.get(c.id));
            } else {
                Member m = new Member(UUID.randomUUID().toString(), groupUid, c.selectedMsisdn, c.getDisplayName(),
                    GroupConstants.ROLE_ORDINARY_MEMBER, c.id, c.version, true);
                m.setLocal(true);
                membersFromContacts.put(c.id, m);
                selectedMembers.add(m);
                RealmUtils.saveDataToRealm(m);
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
            if (requestCode == NavigationConstants.MANUAL_MEMBER_ENTRY) {
                final String newUid = UUID.randomUUID().toString();
                Member newMember = new Member(newUid, groupUid, data.getStringExtra("selectedNumber"),
                    data.getStringExtra("name"), GroupConstants.ROLE_ORDINARY_MEMBER, -1, -1, true);
                newMember.setLocal(true);
                manuallyAddedMembers.add(newMember);
                newMemberListFragment.addMembers(Collections.singletonList(newMember));
                setNewMembersVisible();
            } else if (requestCode == NavigationConstants.MANUAL_MEMBER_EDIT) {
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
                GroupService.getInstance().addMembersToGroup(groupUid, membersToAdd, false).subscribe(
                    new Subscriber<String>() {
                        @Override
                        public void onNext(String s) {
                            if (NetworkUtils.SAVED_SERVER.equals(s)) {
                                Intent i = new Intent();
                                i.putExtra(GroupConstants.UID_FIELD, groupUid);
                                i.putExtra(Constant.INDEX_FIELD, groupPosition);
                                setResult(RESULT_OK, i);
                                progressDialog.dismiss();
                                finish();
                            } else if (NetworkUtils.SAVED_OFFLINE_MODE.equals(s)) {
                                Intent i = IntentUtils.offlineMessageIntent(AddMembersActivity.this, R.string.am_offline_header, getString(R.string.am_offline_body_delib), true, false);
                                progressDialog.dismiss();
                                startActivity(i);
                                finish();
                            } else {
                                // means some numbers were returned as incorrect
                                handleInvalidNumbers(s);
                                loadMembersIntoExistingMemberFragment(false);
                                progressDialog.dismiss();
                            }
                        }

                        @Override
                        public void onError(Throwable e) {
                            Intent i = null;
                            switch (e.getMessage()) {
                                case NetworkUtils.SERVER_ERROR:
                                    if (e instanceof InvalidNumberException) {
                                        handleInvalidNumbers((String) ((InvalidNumberException) e).data);
                                    } else if (e instanceof ApiCallException) {
                                        final String errorMsg = ErrorUtils.serverErrorText(((ApiCallException) e).errorTag);
                                        Snackbar.make(amRlRoot, errorMsg, Snackbar.LENGTH_LONG).show();
                                    } else {
                                        final String body = getString(R.string.am_server_other);
                                        i = IntentUtils.offlineMessageIntent(AddMembersActivity.this, R.string.am_server_error_header, body, false, false);
                                    }
                                    break;
                                case NetworkUtils.CONNECT_ERROR:
                                    i = IntentUtils.offlineMessageIntent(AddMembersActivity.this, R.string.am_offline_header, getString(R.string.am_offline_body_error), false, true);
                                    break;
                                default:
                                    Log.e(TAG, "received strange error : " + e.toString());
                                    i = IntentUtils.offlineMessageIntent(AddMembersActivity.this, R.string.am_server_error_header, getString(R.string.am_server_other), false, true);
                            }
                            progressDialog.dismiss();
                            if (i != null) {
                                startActivity(i);
                                finish();
                            }
                        }

                        @Override
                        public void onCompleted() { }

                    });
            } else {
                Log.d(TAG, "Exited with no members to add!");
                finish();
            }
        }
    }

    private void handleInvalidNumbers(String listOfNumbers) {
        List<Member> invalidMembers = ErrorUtils.findMembersFromListOfNumbers(listOfNumbers,
            newMemberListFragment.getSelectedMembers());

        for (Member m : invalidMembers) {
            m.setNumberInvalid(true);
        }

        newMemberListFragment.transitionToMemberList(invalidMembers);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.input_error_phone_title)
            .setMessage(R.string.input_error_member_phone_some)
            .setCancelable(true)
            .create()
            .show();
    }

    @Override
    public void onDestroy() {
        GroupService.getInstance().cleanInvalidNumbersOnExit(groupUid, null).subscribe();
        super.onDestroy();
    }

}