package org.grassroot.android.activities;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputEditText;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnTextChanged;
import io.realm.Realm;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.grassroot.android.R;
import org.grassroot.android.events.GroupCreatedEvent;
import org.grassroot.android.fragments.ContactSelectionFragment;
import org.grassroot.android.fragments.MemberListFragment;
import org.grassroot.android.interfaces.GroupConstants;
import org.grassroot.android.models.Contact;
import org.grassroot.android.models.Group;
import org.grassroot.android.models.GroupResponse;
import org.grassroot.android.models.Member;
import org.grassroot.android.services.GrassrootRestService;
import org.grassroot.android.utils.Constant;
import org.grassroot.android.utils.ErrorUtils;
import org.grassroot.android.utils.NetworkUtils;
import org.grassroot.android.utils.PermissionUtils;
import org.grassroot.android.utils.PreferenceUtils;
import org.greenrobot.eventbus.EventBus;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static butterknife.OnTextChanged.Callback.AFTER_TEXT_CHANGED;

public class CreateGroupActivity extends PortraitActivity
    implements MemberListFragment.MemberListListener, MemberListFragment.MemberClickListener,
    ContactSelectionFragment.ContactSelectionListener {

  private static final String TAG = CreateGroupActivity.class.getSimpleName();

  @BindView(R.id.rl_cg_root) RelativeLayout rlCgRoot;

  @BindView(R.id.cg_add_member_options) FloatingActionButton addMemberOptions;
  @BindView(R.id.ll_add_member_manually) LinearLayout addMemberManually;
  @BindView(R.id.ll_add_member_contacts) LinearLayout addMemberFromContacts;

  @BindView(R.id.tv_counter) TextView tvCounter;
  @BindView(R.id.et_groupname) TextInputEditText et_groupname;
  @BindView(R.id.et_group_description) TextInputEditText et_group_description;

  private List<Member> manuallyAddedMembers;
  private Map<Integer, Member> mapMembersContacts;
  private MemberListFragment memberListFragment;

  private ContactSelectionFragment contactSelectionFragment;
  private boolean onMainScreen;
  private boolean menuOpen;

  private ProgressDialog progressDialog;

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_create__group);
    ButterKnife.bind(this);

    progressDialog = new ProgressDialog(this);
    progressDialog.setMessage(getString(R.string.txt_pls_wait));
    progressDialog.setIndeterminate(true);

    init();
    setUpMemberList();
  }

  private void init() {
    memberListFragment = MemberListFragment.newInstance(null, false, false, this, this, null);
    contactSelectionFragment = ContactSelectionFragment.newInstance(null, false);
    mapMembersContacts = new HashMap<>();
    manuallyAddedMembers = new ArrayList<>();
    onMainScreen = true;
  }

  @OnClick(R.id.cg_add_member_options)
  public void toggleAddMenu() {
    addMemberOptions.setImageResource(menuOpen ? R.drawable.ic_add : R.drawable.ic_add_45d);
    addMemberFromContacts.setVisibility(menuOpen ? View.GONE : View.VISIBLE);
    addMemberManually.setVisibility(menuOpen ? View.GONE : View.VISIBLE);
    menuOpen = !menuOpen;
  }

  private void setUpMemberList() {
    getSupportFragmentManager().beginTransaction()
        .add(R.id.cg_new_member_list_container, memberListFragment)
        .commit();
  }

  @OnClick(R.id.cg_iv_crossimage) public void ivCrossimage() {
    if (!onMainScreen) {
      // note : this means we do not save / return the contacts on cross clicked
      getSupportFragmentManager().popBackStack();
      onMainScreen = true;
    } else {
      progressDialog.dismiss();
      finish();
    }
  }

  @OnTextChanged(value = R.id.et_group_description, callback = AFTER_TEXT_CHANGED)
  public void changeLengthCounter(CharSequence s) {
    tvCounter.setText("" + s.length() + "/" + "160");
  }

  @OnClick(R.id.ll_add_member_contacts) public void icon_add_from_contacts() {
    toggleAddMenu();
    if (!PermissionUtils.contactReadPermissionGranted(this)) {
      PermissionUtils.requestReadContactsPermission(this);
    } else {
      launchContactSelectionFragment();
    }
  }

  @Override public void onRequestPermissionsResult(int requestCode, String[] permissions,
      int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    if (PermissionUtils.checkContactsPermissionGranted(requestCode, grantResults)) {
      launchContactSelectionFragment();
    }
  }

  private void launchContactSelectionFragment() {
    onMainScreen = false;
    getSupportFragmentManager().beginTransaction()
        .add(R.id.cg_body_root, contactSelectionFragment)
        .addToBackStack(null)
        .commitAllowingStateLoss(); // todo : clean this up in a less hacky way (known issue w/ support lib and Android 6+, need to do an onResume check or similar)
  }

  private void closeContactSelectionFragment() {
    onMainScreen = true;
    getSupportFragmentManager().beginTransaction().remove(contactSelectionFragment).commit();
  }

  @Override public void onContactSelectionComplete(List<Contact> contactsSelected) {
    progressDialog.show();
    List<Member> selectedMembers = new ArrayList<>(manuallyAddedMembers);
    for (Contact c : contactsSelected) {
      if (mapMembersContacts.containsKey(c.id)) {
        selectedMembers.add(mapMembersContacts.get(c.id));
      } else {
        Member m =
            new Member(c.selectedMsisdn, c.getDisplayName(), GroupConstants.ROLE_ORDINARY_MEMBER,
                c.id, true);
        mapMembersContacts.put(c.id, m);
        selectedMembers.add(m);
      }
    }
    memberListFragment.transitionToMemberList(selectedMembers);
    closeContactSelectionFragment();
    progressDialog.hide();
  }

  @OnClick(R.id.ll_add_member_manually) public void ic_edit_call() {
    toggleAddMenu();
    startActivityForResult(new Intent(CreateGroupActivity.this, AddContactManually.class),
        Constant.activityManualMemberEntry);
  }

  @OnClick(R.id.cg_bt_save) public void save() {
    if (menuOpen) {
      toggleAddMenu();
    }
    validate_allFields();
  }

  private void validate_allFields() {
    if (!(TextUtils.isEmpty(
        et_groupname.getText().toString().trim().replaceAll(Constant.regexAlphaNumeric, "")))) {
      createGroup();
    } else {
      ErrorUtils.showSnackBar(rlCgRoot, R.string.error_group_name_blank, Snackbar.LENGTH_SHORT);
    }
  }

  private void createGroup() {
    String mobileNumber = PreferenceUtils.getUserPhoneNumber(CreateGroupActivity.this);
    String code = PreferenceUtils.getAuthToken(CreateGroupActivity.this);
    String groupName =
        et_groupname.getText().toString().trim().replaceAll(Constant.regexAlphaNumeric, "");
    String groupDescription = et_group_description.getText().toString().trim();

    List<Member> groupMembers = memberListFragment.getSelectedMembers();

    if (!NetworkUtils.isNetworkAvailable(getApplicationContext())) {
      Realm realm = Realm.getDefaultInstance();
      Group group = new Group();
      group.setGroupName(groupName);
      group.setDescription(groupDescription);
      group.setIsLocal(true);
      group.setGroupCreator(PreferenceUtils.getUserName(getApplicationContext()));
      group.setGroupUid(UUID.randomUUID().toString());
      group.setLastChangeType(GroupConstants.GROUP_CREATED);
      group.setGroupMemberCount(1);
      group.setDate(new Date());
      group.setDateTimeStringISO(group.getDateTimeStringISO());
      realm.beginTransaction();
      realm.copyToRealmOrUpdate(group);
      realm.commitTransaction();
      realm.close();
      setResultIntent(group);
      finish();
    } else {
      showProgress();
      GrassrootRestService.getInstance()
          .getApi()
          .createGroup(mobileNumber, code, groupName, groupDescription, groupMembers)
          .enqueue(new Callback<GroupResponse>() {
            @Override
            public void onResponse(Call<GroupResponse> call, Response<GroupResponse> response) {
              if (response.isSuccessful()) {
                hideProgress();
                PreferenceUtils.setUserHasGroups(getApplicationContext(), true);
                setResultIntent(response.body().getGroups().first());
                Log.d(TAG, "returning group created! with UID : " + response.body()
                    .getGroups()
                    .get(0)
                    .getGroupUid());
                EventBus.getDefault().post(new GroupCreatedEvent());
                finish();
              } else {
                hideProgress();
                ErrorUtils.showSnackBar(rlCgRoot, R.string.error_generic, Snackbar.LENGTH_SHORT);
              }
            }

            @Override public void onFailure(Call<GroupResponse> call, Throwable t) {
              hideProgress();
              ErrorUtils.handleNetworkError(CreateGroupActivity.this, rlCgRoot, t);
            }
          });
    }
  }

  @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (resultCode == Activity.RESULT_OK && data != null) {
      if (requestCode == Constant.activityManualMemberEntry) {
        Member newMember =
            new Member(data.getStringExtra("selectedNumber"), data.getStringExtra("name"),
                GroupConstants.ROLE_ORDINARY_MEMBER, -1);
        manuallyAddedMembers.add(newMember);
        memberListFragment.addMembers(Collections.singletonList(newMember));
      }
    }
  }

  private void showProgress() {
    progressDialog.show();
  }

  private void hideProgress() {
    progressDialog.dismiss();
  }

  @Override public void onMemberListInitiated(MemberListFragment fragment) {
    // todo: use this to handle fragment setting up & observation, instead of create at start...
    // memberListFragment.setShowSelected(true);
    // memberListFragment.setCanDismissItems(true);
  }

  @Override public void onMemberListPopulated(List<Member> memberList) {

  }

  @Override public void onMemberListDone() {

  }

  @Override public void onMemberDismissed(int position, String memberUid) {
    // todo : deal with this (maybe)
  }

  @Override public void onMemberClicked(int position, String memberUid) {
    // todo : deal with this
  }

  private void setResultIntent(Group group) {
    Intent resultIntent = new Intent();
    resultIntent.putExtra(GroupConstants.OBJECT_FIELD, group);
    setResult(RESULT_OK, resultIntent);
  }
}
