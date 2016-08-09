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
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.grassroot.android.R;
import org.grassroot.android.fragments.ContactSelectionFragment;
import org.grassroot.android.fragments.MemberListFragment;
import org.grassroot.android.fragments.dialogs.FixMemberPhoneDialog;
import org.grassroot.android.interfaces.GroupConstants;
import org.grassroot.android.models.exceptions.ApiCallException;
import org.grassroot.android.models.Contact;
import org.grassroot.android.models.Member;
import org.grassroot.android.models.exceptions.InvalidNumberException;
import org.grassroot.android.services.GroupService;
import org.grassroot.android.utils.Constant;
import org.grassroot.android.utils.ErrorUtils;
import org.grassroot.android.utils.IntentUtils;
import org.grassroot.android.utils.NetworkUtils;
import org.grassroot.android.utils.PermissionUtils;
import org.grassroot.android.utils.RealmUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import rx.Observable;
import rx.Subscriber;
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
                Member m = new Member(UUID.randomUUID().toString(), groupUid, c.selectedMsisdn, c.getDisplayName(),
                    GroupConstants.ROLE_ORDINARY_MEMBER, c.id, true);
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
            if (requestCode == Constant.activityManualMemberEntry) {
                final String newUid = UUID.randomUUID().toString();
                Member newMember = new Member(newUid, groupUid, data.getStringExtra("selectedNumber"), data.getStringExtra("name"),
                        GroupConstants.ROLE_ORDINARY_MEMBER, -1, true);
                newMember.setLocal(true);
                // RealmUtils.saveDataToRealmWithSubscriber(newMember); // todo : figure out safe place to do this
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
                GroupService.getInstance().addMembersToGroup(groupUid, membersToAdd, false).subscribe(new Subscriber<String>() {
                    @Override
                    public void onNext(String s) {
                        if (NetworkUtils.SAVED_SERVER.equals(s)) {
                            Intent i = new Intent();
                            i.putExtra(GroupConstants.UID_FIELD, groupUid);
                            i.putExtra(Constant.INDEX_FIELD, groupPosition);
                            setResult(RESULT_OK, i);
                            progressDialog.dismiss();
                            finish();
                        } else {
                            Intent i = IntentUtils.offlineMessageIntent(AddMembersActivity.this, R.string.am_offline_header, getString(R.string.am_offline_body_delib), true, false);
                            progressDialog.dismiss();
                            startActivity(i);
                            finish();
                        }
                    }

                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
                        Intent i = null;
                        switch (e.getMessage()) {
                            case NetworkUtils.SERVER_ERROR:
                                if (e instanceof InvalidNumberException) {
                                    handleServerError((ApiCallException) e);
                                } else if (e instanceof ApiCallException) {
                                    handleServerError((ApiCallException) e);
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
                });
            } else {
                Log.d(TAG, "Exited with no members to add!");
                finish();
            }
        }
    }

    private void handleServerError(ApiCallException e) {
        // todo : add a "try again to save" message (make this an error dialog ...)
        if (e instanceof InvalidNumberException) {
            handleInvalidNumbers((InvalidNumberException) e);
            /* final String errorNums = (String) e.data;
            if (!TextUtils.isEmpty(errorNums)) {
                List<String> errorNumbers = Arrays.asList(errorNums.split("\\s+"));
                Log.e(TAG, "here are the split numbers : " + errorNumbers);
                List<Member> errorMembers = newMemberListFragment.getMembersFromNumbers(errorNumbers);
                Log.e(TAG, "got this many members with those numbers: " + errorMembers);
                if (errorMembers != null && !errorMembers.isEmpty()) {
                    final Member errorMember = errorMembers.get(0);
                    final String message = String.format(getString(R.string.input_error_member_phone),
                        errorMember.getDisplayName(), errorMember.getPhoneNumber());
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setMessage(message);
                    builder.create().show();
                } else {

                }
            } else {

            }*/
        } else {
            final String errorMsg = ErrorUtils.serverErrorText(e.errorTag, this);
            Snackbar.make(amRlRoot, errorMsg, Snackbar.LENGTH_LONG).show();
        }
    }

    private void handleInvalidNumbers(InvalidNumberException e) {
        final String errorNums = (String) e.data;
        if (!TextUtils.isEmpty(errorNums)) {
            List<String> errorNumbers = Arrays.asList(errorNums.split("\\s+"));
            List<Member> errorMembers = newMemberListFragment.getMembersFromNumbers(errorNumbers);
            int numberError = errorMembers.size();
            if (numberError == 1) {
                Log.e(TAG, "one member is wrong, showing the dialog box ...");
                fixSingleMemberDialog(errorMembers.get(0));
            } else {
                int numberOkay = newMemberListFragment.getSelectedMembers().size() - numberError;

            }
        }
    }

    private void fixSingleMemberDialog(final Member errorMember) {
        Log.e(TAG, "okay let's try this ...");
        FixMemberPhoneDialog.newInstance(groupUid, errorMember)
            .show(getSupportFragmentManager(), null);
    }
        /*AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final EditText textInput = new EditText(this);
        textInput.setHint(errorMember.getPhoneNumber());
        final String message = String.format(getString(R.string.input_error_member_phone_single),
            errorMember.getDisplayName());

        builder.setMessage(R.string.input_error_member_phone_single)
            .setView(textInput)
            .setPositiveButton(R.string.pp_OK, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    final String txt = textInput.getText().toString();
                    Log.e(TAG, "dialog confirmed ... with text = " + txt);
                    fixSingleMemberDo(errorMember, textInput.getText().toString());
                }
            });
        builder.create().show();
    }

    private void fixSingleMemberDo(Member member, final String newNumber) {
        member.setPhoneNumber(newNumber);
        GroupService.getInstance().addMembersToGroup(groupUid, Collections.singletonList(member), false)
            .subscribe(new Observable.OnSubscribe() {
                @Override
                public void call(Object o) {
                    Log.e(TAG, "okay, it is done ...");
                }
            });
    }*/

}