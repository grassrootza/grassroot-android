package org.grassroot.android.activities;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputEditText;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.grassroot.android.R;
import org.grassroot.android.events.GroupCreatedEvent;
import org.grassroot.android.fragments.ContactSelectionFragment;
import org.grassroot.android.fragments.MemberListFragment;
import org.grassroot.android.interfaces.GroupConstants;
import org.grassroot.android.interfaces.NavigationConstants;
import org.grassroot.android.models.Contact;
import org.grassroot.android.models.Group;
import org.grassroot.android.models.Member;
import org.grassroot.android.models.exceptions.ApiCallException;
import org.grassroot.android.models.exceptions.InvalidNumberException;
import org.grassroot.android.services.ContactService;
import org.grassroot.android.services.GroupService;
import org.grassroot.android.utils.Constant;
import org.grassroot.android.utils.ErrorUtils;
import org.grassroot.android.utils.NetworkUtils;
import org.grassroot.android.utils.PermissionUtils;
import org.grassroot.android.utils.RealmUtils;
import org.grassroot.android.utils.Utilities;
import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnFocusChange;
import butterknife.OnTextChanged;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;

import static butterknife.OnTextChanged.Callback.AFTER_TEXT_CHANGED;

public class CreateGroupActivity extends PortraitActivity implements ContactSelectionFragment.ContactSelectionListener {

  private static final String TAG = CreateGroupActivity.class.getSimpleName();

  private int currentScreen;
  private static final int MAIN_SCREEN = 0;
  private static final int CONTACT_LIST = 1;

  @BindView(R.id.rl_cg_root) RelativeLayout rlCgRoot;

  @BindView(R.id.cg_add_member_options) FloatingActionButton addMemberOptions;
  @BindView(R.id.ll_add_member_manually) LinearLayout addMemberManually;
  @BindView(R.id.ll_add_member_contacts) LinearLayout addMemberFromContacts;

  @BindView(R.id.tv_counter) TextView tvCounter;
  @BindView(R.id.et_groupname) TextInputEditText et_groupname;
  @BindView(R.id.et_group_description) TextInputEditText et_group_description;

  @BindView(R.id.cg_bt_save) Button save;

  @BindView(R.id.progressBar) ProgressBar progressBar;

  private List<Member> manuallyAddedMembers;
  private SparseArray<Member> mapMembersContacts;
  private MemberListFragment memberListFragment;

  private ContactSelectionFragment contactSelectionFragment;
  private boolean menuOpen;

  private String groupUid = UUID.randomUUID().toString();;

  private Group cachedGroup;
  private boolean editingOfflineGroup = false;
  private String serverGroupUid;
  private boolean groupCreatedEventIssued = false;

  private String descCharCounter;

  private ProgressDialog progressDialog;

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_create__group);
    ButterKnife.bind(this);

    descCharCounter = getString(R.string.generic_160_char_counter);
    progressDialog = new ProgressDialog(this);
    progressDialog.setMessage(getString(R.string.wait_message));
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
    contactSelectionFragment = new ContactSelectionFragment();
    mapMembersContacts = new SparseArray<>();
    manuallyAddedMembers = new ArrayList<>();
    currentScreen = MAIN_SCREEN;
  }

  private void setUpMemberList() {
    memberListFragment = MemberListFragment.newInstance(cachedGroup, false, false, null,
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

  @OnClick(R.id.cg_iv_crossimage)
  public void ivCrossimage() {
    switch (currentScreen) {
      case MAIN_SCREEN:
        progressDialog.dismiss();
        cleanUpLocalEntities(!editingOfflineGroup); // i.e., delete anything cached unless editing a prior cached group
        finish();
        break;
      case CONTACT_LIST:
        closeContactSelectionFragment(); // note : this means we do not saveGroupIfNamed / return the contacts on cross clicked
        currentScreen = MAIN_SCREEN;
        break;
      default:
        break;
    }
  }

  @OnClick(R.id.cg_add_member_options)
  public void toggleAddMenu() {
    addMemberOptions.setImageResource(menuOpen ? R.drawable.ic_add : R.drawable.ic_add_45d);
    addMemberFromContacts.setVisibility(menuOpen ? View.GONE : View.VISIBLE);
    addMemberManually.setVisibility(menuOpen ? View.GONE : View.VISIBLE);
    menuOpen = !menuOpen;
  }

  @OnFocusChange(R.id.et_group_description)
  public void onFocusChangeTextEdit(View v, boolean hasFocus) {
    Log.e(TAG, "et_group_description: focus change event!");
    if (!hasFocus) {
      Utilities.hideKeyboard(this);
    }
  }

  @OnFocusChange(R.id.et2)
  public void onFocusChangeTextEditContainer(View v, boolean hasFocus) {
    Log.e(TAG, "et2: focus change event!");
    if (!hasFocus) {
      Utilities.hideKeyboard(this);
    }
  }

  @OnClick(R.id.ll_add_member_contacts)
  public void icon_add_from_contacts() {
    toggleAddMenu();
    if (!PermissionUtils.contactReadPermissionGranted(this)) {
      PermissionUtils.requestReadContactsPermission(this);
    } else {
      launchContactSelectionFragment();
    }
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull  String[] permissions,
      @NonNull  int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    if (PermissionUtils.checkContactsPermissionGranted(requestCode, grantResults)) {
      launchContactSelectionFragment();
    }
  }

  private void launchContactSelectionFragment() {
    progressBar.setVisibility(View.VISIBLE);
    ContactService.getInstance().syncContactList(null, false, AndroidSchedulers.mainThread())
        .subscribe(new Action1<Boolean>() {
          @Override
          public void call(Boolean aBoolean) {
            currentScreen = CONTACT_LIST;
            progressBar.setVisibility(View.GONE);
            if (contactSelectionFragment != null) {
              save.setVisibility(View.GONE);
              getSupportFragmentManager().beginTransaction()
                  .add(R.id.cg_body_root, contactSelectionFragment)
                  .addToBackStack(null)
                  .commit();
            }
          }
        }, new Action1<Throwable>() {
          @Override
          public void call(Throwable throwable) {
            Snackbar.make(rlCgRoot, R.string.process_error_loading_contacts, Snackbar.LENGTH_LONG);
          }
        });
  }

  private void closeContactSelectionFragment() {
    currentScreen = MAIN_SCREEN;
    save.setVisibility(View.VISIBLE);
    getSupportFragmentManager()
            .beginTransaction()
            .remove(contactSelectionFragment)
            .commit();
  }

  @Override public void onContactSelectionComplete(List<Contact> contactsSelected) {
    progressDialog.show();
    List<Member> selectedMembers = new ArrayList<>(manuallyAddedMembers);
    for (Contact c : contactsSelected) {
      if (mapMembersContacts.indexOfKey(c.id) >= 0) {
        selectedMembers.add(mapMembersContacts.get(c.id));
      } else {
        Member m = new Member(UUID.randomUUID().toString(), groupUid, c.selectedMsisdn,
            c.getDisplayName(), GroupConstants.ROLE_ORDINARY_MEMBER, c.id, -1, true);
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
        NavigationConstants.MANUAL_MEMBER_ENTRY);
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
    startActivityForResult(i, NavigationConstants.MANUAL_MEMBER_EDIT);
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (resultCode == Activity.RESULT_OK && data != null) {
      if (requestCode == NavigationConstants.MANUAL_MEMBER_ENTRY) {
        Member newMember = new Member(UUID.randomUUID().toString(), groupUid, data.getStringExtra("selectedNumber"),
            data.getStringExtra("name"), GroupConstants.ROLE_ORDINARY_MEMBER, -1, -1, true);
        newMember.setLocal(true);
        manuallyAddedMembers.add(newMember);
        memberListFragment.addMembers(Collections.singletonList(newMember));
      } else if (requestCode == NavigationConstants.MANUAL_MEMBER_EDIT) {
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
      Snackbar.make(rlCgRoot, R.string.error_group_name_blank, Snackbar.LENGTH_SHORT);
      et_groupname.setError(getString(R.string.error_group_name_blank));
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
            Log.d(TAG, "string received : " + s);
            progressDialog.dismiss();
            Group finalGroup;
            if (NetworkUtils.SAVED_OFFLINE_MODE.equals(s)) {
              finalGroup = RealmUtils.loadGroupFromDB(groupUid);
              handleGroupCreationAndExit(finalGroup, false);
            } else {
              final String serverUid = s.substring("OK-".length());
              finalGroup = RealmUtils.loadGroupFromDB(serverUid);
              Log.d(TAG, "here is the saved group = " + finalGroup.toString());
              if ("OK".equals(s.substring(0, 2))) {
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
      final String errorMsg = ErrorUtils.serverErrorText(e.errorTag);
      Snackbar.make(rlCgRoot, errorMsg, Snackbar.LENGTH_SHORT); // todo : have a "save anyway" button, and/or options to edit number
    }
  }

  private void handleGroupCreationAndExit(Group group, boolean unexpectedConnectionError) {
    if (!groupCreatedEventIssued) {
      // issue this in almost all cases, except if previously issued when return with invalid numbers
      EventBus.getDefault().post(new GroupCreatedEvent(group));
    }
    Intent i = new Intent(CreateGroupActivity.this, ActionCompleteActivity.class);
    String completionMessage;
    if (!group.getIsLocal()) {
      completionMessage =
          String.format(getString(R.string.ac_body_group_create_server), group.getGroupName(),
              group.getGroupMemberCount());
    } else if (!unexpectedConnectionError) {
      completionMessage = String.format(getString(R.string.ac_body_group_create_local), group.getGroupName());
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
    cleanUpLocalEntities(!group.getIsLocal()); // i.e., delete locally cached group if this method has been passed one from server
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
    EventBus.getDefault().post(new GroupCreatedEvent(serverGroupUid));
    groupCreatedEventIssued = true;
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
        cleanUpLocalEntities(!TextUtils.isEmpty(serverGroupUid)); // i.e., only delete if we managed to get one saved before
        startActivity(i);
        finish();
      }
    });
  }

  @Override public void onBackPressed() {
    super.onBackPressed();
    cleanUpLocalEntities(!editingOfflineGroup); // i.e., delete everything cached unless editing a locally created group
  }

  private void cleanUpLocalEntities(final boolean deleteLocalGroup){
    if (cachedGroup != null && !editingOfflineGroup) {
      GroupService.getInstance().cleanInvalidNumbersOnExit(groupUid, null).subscribe(new Action1<String>() {
        @Override
        public void call(String s) {
          if (deleteLocalGroup) {
            GroupService.getInstance().deleteLocallyCreatedGroup(groupUid);
          }
        }
      });
    }

    if (!TextUtils.isEmpty(serverGroupUid)) {
      // depending on user's path, invalid number members may have been saved under either UID, so clean both out
      GroupService.getInstance().cleanInvalidNumbersOnExit(groupUid,null).subscribe();
      GroupService.getInstance().cleanInvalidNumbersOnExit(serverGroupUid, null).subscribe();
    }
  }

  @OnTextChanged(value = R.id.et_group_description, callback = AFTER_TEXT_CHANGED)
  public void changeLengthCounter(CharSequence s) {
    tvCounter.setText(String.format(descCharCounter, s.length()));
  }
}
