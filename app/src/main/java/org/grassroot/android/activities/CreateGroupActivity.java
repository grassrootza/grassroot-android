package org.grassroot.android.activities;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputEditText;
import android.support.v7.app.AlertDialog;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.grassroot.android.R;
import org.grassroot.android.events.GroupCreatedEvent;
import org.grassroot.android.fragments.ContactSelectionFragment;
import org.grassroot.android.fragments.MemberListFragment;
import org.grassroot.android.fragments.dialogs.ConfirmCancelDialogFragment;
import org.grassroot.android.interfaces.GroupConstants;
import org.grassroot.android.models.Contact;
import org.grassroot.android.models.Group;
import org.grassroot.android.models.GroupResponse;
import org.grassroot.android.models.Member;
import org.grassroot.android.models.PreferenceObject;
import org.grassroot.android.services.GroupService;
import org.grassroot.android.utils.Constant;
import org.grassroot.android.utils.ErrorUtils;
import org.grassroot.android.utils.PermissionUtils;
import org.grassroot.android.utils.RealmUtils;
import org.greenrobot.eventbus.EventBus;
import retrofit2.Response;

import static butterknife.OnTextChanged.Callback.AFTER_TEXT_CHANGED;

public class CreateGroupActivity extends PortraitActivity implements ContactSelectionFragment.ContactSelectionListener {

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

  private String groupUid = UUID.randomUUID().toString();;
  private Group cachedGroup;

  private String descCharCounter;

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
    memberListFragment = MemberListFragment.newInstance(null, true, false, false, null,
            new MemberListFragment.MemberClickListener() {
      @Override
      public void onMemberClicked(int position, String memberUid) {
        memberContextMenu(position, memberUid);
      }

      @Override
      public void onMemberDismissed(int position, String memberUid) { }
    });
    contactSelectionFragment = ContactSelectionFragment.newInstance(null, false);
    mapMembersContacts = new HashMap<>();
    manuallyAddedMembers = new ArrayList<>();
    onMainScreen = true;
    descCharCounter = getString(R.string.generic_160_char_counter);
  }

  private void setUpMemberList() {
    getSupportFragmentManager().beginTransaction()
        .add(R.id.cg_new_member_list_container, memberListFragment)
        .commit();
  }

  // we do this so the user can always come back to it ... being extra careful on list fragment though
  private void cacheWipGroup() {
    final String currentName = et_groupname.getText().toString().trim();
    if (!TextUtils.isEmpty(currentName)) {
      final String currentDesc = et_groupname.getText().toString().trim();
      final List<Member> currentMembers = memberListFragment.getSelectedMembers();
      if (cachedGroup == null) {
        cachedGroup = GroupService.getInstance().createGroupLocally(groupUid,
                currentName,
                currentDesc,
                currentMembers);
      } else {
        cachedGroup = GroupService.getInstance().updateLocalGroup(cachedGroup,
                currentName,
                currentDesc,
                currentMembers);
      }
    }
  }

  @OnClick(R.id.cg_iv_crossimage) public void ivCrossimage() {
    if (!onMainScreen) {
      // note : this means we do not saveGroupIfNamed / return the contacts on cross clicked
      getSupportFragmentManager().popBackStack();
      onMainScreen = true;
    } else {
      progressDialog.dismiss();
      deleteLocalCreatedGroup();
      finish();
    }
  }

  @OnClick(R.id.cg_add_member_options) public void toggleAddMenu() {
    addMemberOptions.setImageResource(menuOpen ? R.drawable.ic_add : R.drawable.ic_add_45d);
    addMemberFromContacts.setVisibility(menuOpen ? View.GONE : View.VISIBLE);
    addMemberManually.setVisibility(menuOpen ? View.GONE : View.VISIBLE);
    menuOpen = !menuOpen;
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
        Member m = new Member(c.selectedMsisdn, c.getDisplayName(), GroupConstants.ROLE_ORDINARY_MEMBER,
                c.id, true);
        m.setGroupUid(groupUid);
        m.setMemberUid(UUID.randomUUID().toString());
        m.setMemberGroupUid();
        RealmUtils.saveDataToRealm(m);
        selectedMembers.add(m);
        mapMembersContacts.put(c.id, m);
      }
    }
    memberListFragment.transitionToMemberList(selectedMembers);
    closeContactSelectionFragment();
    progressDialog.hide();
    cacheWipGroup();
  }

  @OnClick(R.id.ll_add_member_manually) public void ic_edit_call() {
    toggleAddMenu();
    startActivityForResult(new Intent(CreateGroupActivity.this, AddContactManually.class),
        Constant.activityManualMemberEntry);
  }

  private void memberContextMenu(final int position, final String memberUid) {
    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    builder.setItems(R.array.cg_member_popup, new DialogInterface.OnClickListener() {
              @Override
              public void onClick(DialogInterface dialog, int which) {
                if (which == 0) {
                  editMember(position, memberUid);
                } else {
                  memberListFragment.removeMember(position);;
                }
              }
            });
    builder.create().show();
  }

  private void editMember(final int position, final String memberUid) {
    Member member = RealmUtils.loadObjectFromDB(Member.class, "memberUid", memberUid);
    Intent i = new Intent(CreateGroupActivity.this, AddContactManually.class);
    i.putExtra(GroupConstants.MEMBER_OBJECT, member);
    i.putExtra(Constant.INDEX_FIELD, position);
    startActivityForResult(i, Constant.activityManualMemberEdit);
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (resultCode == Activity.RESULT_OK && data != null) {
      if (requestCode == Constant.activityManualMemberEntry) {
        Member newMember = new Member(data.getStringExtra("selectedNumber"), data.getStringExtra("name"),
                        GroupConstants.ROLE_ORDINARY_MEMBER, -1);
        //added memberId here
        newMember.setMemberUid(UUID.randomUUID().toString());
        newMember.setGroupUid(groupUid);
        newMember.setMemberGroupUid();
        manuallyAddedMembers.add(newMember);
        memberListFragment.addMembers(Collections.singletonList(newMember));

      } else if (requestCode == Constant.activityManualMemberEdit) {
        Member revisedMember = data.getParcelableExtra(GroupConstants.MEMBER_OBJECT);
        int position = data.getIntExtra(Constant.INDEX_FIELD, -1);
        memberListFragment.updateMember(position, revisedMember);
        Log.e(TAG, "at position: " + position + ", member received back : " + revisedMember);
      }
    }
    cacheWipGroup();
  }

  @OnClick(R.id.cg_bt_save)
  public void saveGroupIfNamed() {
    if (menuOpen) {
      toggleAddMenu();
    }


    if (TextUtils.isEmpty(et_groupname.getText())) {
      ErrorUtils.showSnackBar(rlCgRoot, R.string.error_group_name_blank, Snackbar.LENGTH_SHORT);
    } else {
      createGroup();
    }
  }

  private void createGroup() {
    String groupName =
        et_groupname.getText().toString().trim().replaceAll(Constant.regexAlphaNumeric, "");
    String groupDescription = et_group_description.getText().toString().trim();
    List<Member> groupMembers = memberListFragment.getSelectedMembers();

    cacheWipGroup();

    progressDialog.show();
    GroupService.getInstance()
        .sendNewGroupToServer(groupUid, new GroupService.GroupCreationListener() {
              @Override public void groupCreatedLocally(Group group) {
                progressDialog.dismiss();
                handleSuccessfulGroupCreation(group, false); // todo : say, "it's local"
              }

              @Override public void groupCreatedOnServer(Group group) {
                progressDialog.dismiss();
                handleSuccessfulGroupCreation(group, true);
              }

              @Override public void groupCreationError(Response<GroupResponse> response) {
                progressDialog.dismiss();
                ErrorUtils.showSnackBar(rlCgRoot, R.string.error_generic, Snackbar.LENGTH_SHORT);
              }
            });
  }

  private void handleSuccessfulGroupCreation(Group group, boolean successfullySavedToServer) {
    EventBus.getDefault().post(new GroupCreatedEvent(group));
    Intent i = new Intent(CreateGroupActivity.this, ActionCompleteActivity.class);
    String completionMessage;
    if (!group.getIsLocal()) {
      completionMessage =
          String.format(getString(R.string.ac_body_group_create_server), group.getGroupName(),
              group.getGroupMemberCount());
    } else {
      completionMessage =
          String.format(getString(R.string.ac_body_group_create_local), group.getGroupName());
    }
    i.putExtra(ActionCompleteActivity.HEADER_FIELD, R.string.ac_header_group_create);
    i.putExtra(ActionCompleteActivity.BODY_FIELD, completionMessage);
    i.putExtra(ActionCompleteActivity.TASK_BUTTONS, true);
    i.putExtra(ActionCompleteActivity.ACTION_INTENT, ActionCompleteActivity.HOME_SCREEN);
    i.putExtra(GroupConstants.OBJECT_FIELD, group);
    i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
    startActivity(i);
    finish();
  }

  @Override public void onBackPressed() {
    super.onBackPressed();
    deleteLocalCreatedGroup();
  }

  private void deleteLocalCreatedGroup(){
    if (cachedGroup != null) {
      RealmUtils.removeObjectFromDatabase(Group.class,"groupUid",groupUid);
      RealmUtils.removeObjectFromDatabase(Member.class,"groupUid",groupUid);
    }
  }

  @OnTextChanged(value = R.id.et_group_description, callback = AFTER_TEXT_CHANGED)
  public void changeLengthCounter(CharSequence s) {
    tvCounter.setText(String.format(descCharCounter, s.length()));
  }
}
