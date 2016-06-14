package org.grassroot.android.activities;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.github.clans.fab.FloatingActionButton;
import com.github.clans.fab.FloatingActionMenu;

import org.grassroot.android.R;
import org.grassroot.android.events.GroupCreatedEvent;
import org.grassroot.android.interfaces.GroupConstants;
import org.grassroot.android.models.Contact;
import org.grassroot.android.services.GrassrootRestService;
import org.grassroot.android.models.GroupResponse;
import org.grassroot.android.models.Member;
import org.grassroot.android.fragments.ContactSelectionFragment;
import org.grassroot.android.fragments.MemberListFragment;
import org.grassroot.android.utils.Constant;
import org.grassroot.android.utils.ErrorUtils;
import org.grassroot.android.utils.PermissionUtils;
import org.grassroot.android.utils.PreferenceUtils;
import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnTextChanged;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static butterknife.OnTextChanged.Callback.AFTER_TEXT_CHANGED;

public class CreateGroupActivity extends PortraitActivity implements
        MemberListFragment.MemberListListener, MemberListFragment.MemberClickListener, ContactSelectionFragment.ContactSelectionListener {

    private static final String TAG = CreateGroupActivity.class.getSimpleName();

    @BindView(R.id.rl_cg_root)
    RelativeLayout rlCgRoot;

    @BindView(R.id.cg_add_member_options)
    FloatingActionMenu addMemberOptions;
    @BindView(R.id.icon_add_member_manually)
    FloatingActionButton addMemberManually;
    @BindView(R.id.icon_add_from_contacts)
    FloatingActionButton addMemberFromContacts;

    @BindView(R.id.tv_counter)
    TextView tvCounter;
    @BindView(R.id.et_group_description)
    EditText et_group_description;

    @BindView(R.id.cg_txt_toolbar)
    TextView txtToolbar;
    @BindView(R.id.et_groupname)
    EditText et_groupname;
    @BindView(R.id.cg_iv_crossimage)
    ImageView ivCrossimage;

    private Map<String, Member> mapMembersContacts;
    private MemberListFragment memberListFragment;

    private ContactSelectionFragment contactSelectionFragment;
    private boolean onMainScreen;

    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create__group);
        ButterKnife.bind(this);

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Please wait...");

        init();
        setUpViews();
        setUpMemberList();
    }

    private void init() {
        memberListFragment = new MemberListFragment();
        contactSelectionFragment = new ContactSelectionFragment();
        mapMembersContacts = new HashMap<>();
        onMainScreen = true;
    }

    private void setUpViews() {
        addMemberOptions.setOnMenuToggleListener(new FloatingActionMenu.OnMenuToggleListener() {
            @Override
            public void onMenuToggle(boolean opened) {
                addMemberFromContacts.setVisibility(opened ? View.VISIBLE : View.GONE);
                addMemberManually.setVisibility(opened ? View.VISIBLE : View.GONE);
            }
        });
        addMemberOptions.setVisibility(View.VISIBLE);
    }

    private void setUpMemberList() {
        getSupportFragmentManager()
                .beginTransaction()
                .add(R.id.cg_new_member_list_container, memberListFragment)
                .commit();
    }

    @OnClick(R.id.cg_iv_crossimage)
    public void ivCrossimage() {
        if (!onMainScreen) {
            // note : this means we do not save / return the contacts on cross clicked
            getSupportFragmentManager().popBackStack();
            onMainScreen = true;
        } else {
            finish();
        }
    }

    @OnTextChanged(value = R.id.et_group_description, callback = AFTER_TEXT_CHANGED)
    public void changeLengthCounter(CharSequence s) {
        tvCounter.setText("" + s.length() + "/" + "160");
    }

    @OnClick(R.id.icon_add_from_contacts)
    public void icon_add_from_contacts() {
        addMemberOptions.close(true);
        if (!PermissionUtils.contactReadPermissionGranted(this)) {
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
        Set<Contact> preSelectedSet = new HashSet<>(Contact.convertFromMembers(memberListFragment.getSelectedMembers()));
        onMainScreen = false;
        contactSelectionFragment.setContactsToPreselect(preSelectedSet);
        getSupportFragmentManager()
                .beginTransaction()
                .add(R.id.cg_body_root, contactSelectionFragment)
                .addToBackStack(null)
                .commit();
    }

    private void closeContactSelectionFragment() {
        onMainScreen = true;
        getSupportFragmentManager()
                .beginTransaction()
                .remove(contactSelectionFragment)
                .commit();
    }

    @Override
    public void onContactSelectionComplete(List<Contact> contactsAdded, Set<Contact> contactsRemoved) {

        Log.e(TAG, "contacts added! these: " + contactsAdded.toString() + ", and removed: " + contactsRemoved.toString());

        List<Member> membersToAdd = new ArrayList<>();
        List<Member> membersToRemove = new ArrayList<>();

        for (Contact c : contactsAdded) {
            Member m = new Member(c.selectedNumber, c.name, GroupConstants.ROLE_ORDINARY_MEMBER, c.lookupKey, true);
            mapMembersContacts.put(c.lookupKey, m);
            membersToAdd.add(m);
        }

        for (Contact c : contactsRemoved) {
            Member m = mapMembersContacts.get(c.lookupKey);
            if (m != null) {
                membersToRemove.add(m);
            }
        }

        if (!membersToAdd.isEmpty())
            memberListFragment.addMembers(membersToAdd);
        if (!membersToRemove.isEmpty())
            memberListFragment.removeMembers(membersToRemove);

        closeContactSelectionFragment();

    }

    @OnClick(R.id.icon_add_member_manually)
    public void ic_edit_call() {
        addMemberOptions.close(true);
        startActivityForResult(new Intent(CreateGroupActivity.this, AddContactManually.class), Constant.activityManualMemberEntry);
    }

    @OnClick(R.id.cg_bt_save)
    public void save() {
        addMemberOptions.close(true);
        validate_allFields();
    }

    private void validate_allFields() {
        if (!(TextUtils.isEmpty(et_groupname.getText().toString().trim().replaceAll(Constant.regexAlphaNumeric, "")))) {
            createGroup();
        } else {
            ErrorUtils.showSnackBar(rlCgRoot, R.string.et_groupname, Snackbar.LENGTH_SHORT);
        }
    }

    private void createGroup(){

        showProgress();
        String mobileNumber = PreferenceUtils.getUserPhoneNumber(CreateGroupActivity.this);
        String code = PreferenceUtils.getAuthToken(CreateGroupActivity.this);
        String groupName = et_groupname.getText().toString().trim().replaceAll(Constant.regexAlphaNumeric, "");
        String groupDescription = et_group_description.getText().toString().trim();

        List<Member> groupMembers = memberListFragment.getSelectedMembers();

        GrassrootRestService.getInstance().getApi()
                .createGroup(mobileNumber, code, groupName, groupDescription, groupMembers)
                .enqueue(new Callback<GroupResponse>() {
                    @Override
                    public void onResponse(Call<GroupResponse> call, Response<GroupResponse> response) {
                        if (response.isSuccessful()) {
                            hideProgress();
                            PreferenceUtils.setUserHasGroups(getApplicationContext(), true);
                            Intent resultIntent = new Intent();
                            Log.e(TAG, "here's the response body: " + response.body().toString());
                            resultIntent.putExtra(GroupConstants.OBJECT_FIELD, response.body().getGroups().get(0));
                            setResult(RESULT_OK, resultIntent);
                            Log.e(TAG, "returning group created! with UID : " + response.body().getGroups().get(0).getGroupUid());
                            EventBus.getDefault().post(new GroupCreatedEvent());
                            finish();
                        } else {
                            hideProgress();
                            ErrorUtils.showSnackBar(rlCgRoot, R.string.error_generic, Snackbar.LENGTH_SHORT);
                        }
                    }

                    @Override
                    public void onFailure(Call<GroupResponse> call, Throwable t) {
                        hideProgress();
                        ErrorUtils.handleNetworkError(CreateGroupActivity.this, rlCgRoot, t);
                    }
                });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK && data != null) {
            if (requestCode == Constant.activityManualMemberEntry) {
                Member newMember = new Member(data.getStringExtra("selectedNumber"), data.getStringExtra("name"),
                        GroupConstants.ROLE_ORDINARY_MEMBER, null);
                memberListFragment.addMembers(Collections.singletonList(newMember));
            }
        }
    }

    private void showProgress(){
        progressDialog.show();
    }

    private void hideProgress(){
        progressDialog.dismiss();
    }

    @Override
    public void onMemberListInitiated(MemberListFragment fragment) {
        // todo: use this to handle fragment setting up & observation, instead of create at start...
        memberListFragment.setShowSelected(true);
        memberListFragment.setCanDismissItems(true);
    }

    @Override
    public void onMemberListPopulated(List<Member> memberList) {

    }

    @Override
    public void onMemberDismissed(int position, String memberUid) {
        // todo : deal with this (maybe)
    }

    @Override
    public void onMemberClicked(int position, String memberUid) {
        // todo : deal with this
    }
}
