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
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnTextChanged;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.grassroot.android.R;
import org.grassroot.android.events.GroupCreatedEvent;
import org.grassroot.android.fragments.ContactSelectionFragment;
import org.grassroot.android.fragments.MemberListFragment;
import org.grassroot.android.interfaces.GroupConstants;
import org.grassroot.android.models.exceptions.ApiCallException;
import org.grassroot.android.models.Contact;
import org.grassroot.android.models.Group;
import org.grassroot.android.models.Member;
import org.grassroot.android.models.exceptions.InvalidNumberException;
import org.grassroot.android.services.GroupService;
import org.grassroot.android.utils.Constant;
import org.grassroot.android.utils.ErrorUtils;
import org.grassroot.android.utils.NetworkUtils;
import org.grassroot.android.utils.PermissionUtils;
import org.grassroot.android.utils.RealmUtils;
import org.greenrobot.eventbus.EventBus;

import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;

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

  @BindView(R.id.cg_bt_save) Button save;

  private List<Member> manuallyAddedMembers;
  private Map<Integer, Member> mapMembersContacts;
  private MemberListFragment memberListFragment;

  private ContactSelectionFragment contactSelectionFragment;
  private boolean onMainScreen;
  private boolean menuOpen;

  private String groupUid = UUID.randomUUID().toString();;
  private Group cachedGroup;
  private boolean editingOfflineGroup = false;
  private String serverGroupUid;

  private String descCharCounter;

  private ProgressDialog progressDialog;

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_create__group);
    ButterKnife.bind(this);

    descCharCounter = getString(R.string.generic_160_char_counter);
    progressDialog = new ProgressDialog(this);
    progressDialog.setMessage(getString(R.string.txt_pls_wait));
    progressDialog.setIndeterminate(true);

    init();
    checkForWipGroup();
    setUpMemberList();
  }

  private void checkForWipGroup() {
    cachedGroup = getIntent().getParcelableExtra(GroupConstants.OBJECT_FIELD);
    if (cachedGroup != null) {
      if (cachedGroup.getIsLocal()) {
        groupUid = cachedGroup.getGroupUid();
        et_groupname.setText(cachedGroup.getGroupName());
        et_group_description.setText(cachedGroup.getDescription());
        editingOfflineGroup = true;
      } else {
        cachedGroup = null;
      }
    }
  }

  private void init() {
    contactSelectionFragment = ContactSelectionFragment.newInstance(null, false);
    mapMembersContacts = new HashMap<>();
    manuallyAddedMembers = new ArrayList<>();
    onMainScreen = true;
  }

  private void setUpMemberList() {
    memberListFragment = MemberListFragment.newInstance(cachedGroup, false, null,
            new MemberListFragment.MemberClickListener() {
              @Override
              public void onMemberClicked(int position, String memberUid) {
                memberContextMenu(position, memberUid);
              }
            });

    getSupportFragmentManager().beginTransaction()
        .add(R.id.cg_new_member_list_container, memberListFragment)
        .commit();
  }

  // we do this so the user can always come back to it ... being extra careful on list fragment though
  private void cacheWipGroup() {
    final String currentName = et_groupname.getText().toString().trim();
    if (!TextUtils.isEmpty(currentName)) {
      final String currentDesc = et_group_description.getText().toString().trim();
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
      cleanUpLocalEntities();
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
        Member m = new Member(UUID.randomUUID().toString(), groupUid, c.selectedMsisdn,
            c.getDisplayName(), GroupConstants.ROLE_ORDINARY_MEMBER, c.id, true);
        m.setLocal(true);
        RealmUtils.saveDataToRealmWithSubscriber(m);
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
        Member newMember = new Member(UUID.randomUUID().toString(), groupUid, data.getStringExtra("selectedNumber"),
            data.getStringExtra("name"), GroupConstants.ROLE_ORDINARY_MEMBER, -1, true);
        newMember.setLocal(true);
        manuallyAddedMembers.add(newMember);
        memberListFragment.addMembers(Collections.singletonList(newMember));
      } else if (requestCode == Constant.activityManualMemberEdit) {
        Member revisedMember = data.getParcelableExtra(GroupConstants.MEMBER_OBJECT);
        int position = data.getIntExtra(Constant.INDEX_FIELD, -1);
        memberListFragment.updateMember(position, revisedMember);
        Log.d(TAG, "at position: " + position + ", member received back : " + revisedMember);
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
    save.setEnabled(false);
    cacheWipGroup();
    progressDialog.show();
    progressDialog.setCancelable(false);
    GroupService.getInstance().sendNewGroupToServer(groupUid, AndroidSchedulers.mainThread())
        .subscribe(new Subscriber<String>() {

          @Override
          public void onError(Throwable e) {
            progressDialog.dismiss();
            switch (e.getMessage()) {
              case NetworkUtils.SERVER_ERROR:
                save.setEnabled(true);
                handleServerError((ApiCallException) e);
                break;
              case NetworkUtils.CONNECT_ERROR:
                Group wipGroup = RealmUtils.loadGroupFromDB(groupUid);
                handleGroupCreationAndExit(wipGroup, true);
                break;
              default:
                break;
            }
          }

          @Override
          public void onNext(String s) {
            Log.e(TAG, "string received : " + s);
            progressDialog.dismiss();
            Group finalGroup;
            if (NetworkUtils.SAVED_OFFLINE_MODE.equals(s)) {
              finalGroup = RealmUtils.loadGroupFromDB(groupUid);
              handleGroupCreationAndExit(finalGroup, false);
            } else {
              final String serverUid = s.substring("OK-".length());
              finalGroup = RealmUtils.loadGroupFromDB(serverUid);
              if ("OK".equals(s.substring(0, 1))) {
                handleGroupCreationAndExit(finalGroup, false);
              } else {
                handleSavedButSomeInvalid(serverUid);
              }
            }
          }

          @Override
          public void onCompleted() { }
        });
  }

  private void handleServerError(ApiCallException e) {
    if (e instanceof InvalidNumberException) {
      save.setText(R.string.input_error_try_again);
      AlertDialog.Builder builder = new AlertDialog.Builder(this);
      builder.setMessage(R.string.input_error_member_phone_all)
          .setCancelable(true)
          .create()
          .show();
      handleMembersWithInvalidNumbers((String) e.data);
    } else {
      final String errorMsg = ErrorUtils.serverErrorText(e.errorTag, CreateGroupActivity.this);
      Snackbar.make(rlCgRoot, errorMsg, Snackbar.LENGTH_SHORT); // todo : have a "save anyway" button, and/or options to edit number
    }
  }

  private void handleGroupCreationAndExit(Group group, boolean unexpectedConnectionError) {
    EventBus.getDefault().post(new GroupCreatedEvent(group));
    Intent i = new Intent(CreateGroupActivity.this, ActionCompleteActivity.class);
    String completionMessage;
    if (!group.getIsLocal()) {
      completionMessage =
          String.format(getString(R.string.ac_body_group_create_server), group.getGroupName(),
              group.getGroupMemberCount());
    } else if (!unexpectedConnectionError) {
      completionMessage =
          String.format(getString(R.string.ac_body_group_create_local), group.getGroupName());
    } else {
      completionMessage = getString(R.string.ac_body_group_create_connect_error);
    }

    i.putExtra(ActionCompleteActivity.HEADER_FIELD, R.string.ac_header_group_create);
    i.putExtra(ActionCompleteActivity.BODY_FIELD, completionMessage);
    i.putExtra(ActionCompleteActivity.TASK_BUTTONS, !unexpectedConnectionError);
    i.putExtra(ActionCompleteActivity.OFFLINE_BUTTONS, unexpectedConnectionError);
    i.putExtra(ActionCompleteActivity.ACTION_INTENT, ActionCompleteActivity.HOME_SCREEN);
    i.putExtra(GroupConstants.OBJECT_FIELD, group);
    i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
    cleanUpLocalEntities();
    startActivity(i);
    finish();
  }

  private void handleMembersWithInvalidNumbers(final String listOfNumbers) {
    List<String> memberNos = Arrays.asList(listOfNumbers.split(","));
    List<Member> invalidMembers = ErrorUtils.findMembersFromListOfNumbers(memberNos,
        memberListFragment.getSelectedMembers());
    for (Member m : invalidMembers) {
      m.setNumberInvalid(true);
    }
    memberListFragment.transitionToMemberList(invalidMembers);
  }

  private void handleSavedButSomeInvalid(final String serverUid) {
    save.setText(R.string.input_error_try_again);
    save.setEnabled(true);
    serverGroupUid = serverUid;
    RealmUtils.loadMembersSortedInvalid(serverUid).subscribe(new Action1<List<Member>>() {
      @Override
      public void call(List<Member> members) {
        AlertDialog.Builder builder = new AlertDialog.Builder(CreateGroupActivity.this);
        builder.setMessage(R.string.input_error_member_phone_saved)
            .setCancelable(true)
            .create()
            .show();
        memberListFragment.transitionToMemberList(members);
        save.setOnClickListener(new View.OnClickListener() {
          @Override
          public void onClick(View v) {
            retryInvalidNumbers(serverUid);
          }
        });
      }
    });
  }

  private void retryInvalidNumbers(final String serverUid) {
    progressDialog.show();
    GroupService.getInstance().addMembersToGroup(serverUid, memberListFragment.getSelectedMembers(), true)
        .subscribe(new Subscriber<String>() {
          @Override
          public void onNext(String s) {
            progressDialog.dismiss();
            Group finalGroup = RealmUtils.loadGroupFromDB(serverUid);
            if (NetworkUtils.SAVED_SERVER.equals(s)) {
              handleGroupCreationAndExit(finalGroup, false);
            } else {
              giveUpOnInvalidNumbersAndExit(finalGroup);
            }
          }

          @Override
          public void onError(Throwable e) {
            // note : may in future want to make this differentiate, but to evaluate after user feedback
            Group finalGroup = RealmUtils.loadGroupFromDB(serverUid);
            giveUpOnInvalidNumbersAndExit(finalGroup);
          }

          @Override
          public void onCompleted() { }
        });
  }

  private void giveUpOnInvalidNumbersAndExit(final Group group) {
    GroupService.getInstance().cleanInvalidNumbersOnExit(group.getGroupUid(),
        AndroidSchedulers.mainThread()).subscribe(new Action1<String>() {
      @Override
      public void call(String s) {
        EventBus.getDefault().post(new GroupCreatedEvent(group));
        Intent i = new Intent(CreateGroupActivity.this, ActionCompleteActivity.class);
        String completionMessage = getString(R.string.input_error_still_invalid);
        i.putExtra(ActionCompleteActivity.HEADER_FIELD, R.string.ac_header_group_create_but);
        i.putExtra(ActionCompleteActivity.BODY_FIELD, completionMessage);
        i.putExtra(ActionCompleteActivity.TASK_BUTTONS, true);
        i.putExtra(ActionCompleteActivity.OFFLINE_BUTTONS, false);
        i.putExtra(ActionCompleteActivity.ACTION_INTENT, ActionCompleteActivity.HOME_SCREEN);
        i.putExtra(GroupConstants.OBJECT_FIELD, group);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        cleanUpLocalEntities();
        startActivity(i);
        finish();
      }
    });
  }

  @Override public void onBackPressed() {
    super.onBackPressed();
    cleanUpLocalEntities();
  }

  private void cleanUpLocalEntities(){
    if (cachedGroup != null && !editingOfflineGroup) {
      GroupService.getInstance().cleanInvalidNumbersOnExit(groupUid, null).subscribe(new Action1<String>() {
        @Override
        public void call(String s) {
          GroupService.getInstance().deleteLocallyCreatedGroup(groupUid);
        }
      });
    }
    if (!TextUtils.isEmpty(serverGroupUid)) {
      GroupService.getInstance().cleanInvalidNumbersOnExit(serverGroupUid, null).subscribe();
    }
  }

  @OnTextChanged(value = R.id.et_group_description, callback = AFTER_TEXT_CHANGED)
  public void changeLengthCounter(CharSequence s) {
    tvCounter.setText(String.format(descCharCounter, s.length()));
  }
}
