package org.grassroot.android.ui.activities;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
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
import org.grassroot.android.services.model.GenericResponse;
import org.grassroot.android.services.model.GroupResponse;
import org.grassroot.android.services.model.Member;
import org.grassroot.android.ui.fragments.MemberListFragment;
import org.grassroot.android.utils.Constant;
import org.grassroot.android.utils.ErrorUtils;
import org.grassroot.android.utils.PermissionUtils;
import org.grassroot.android.utils.PreferenceUtils;
import org.grassroot.android.utils.UtilClass;
import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnTextChanged;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static butterknife.OnTextChanged.Callback.AFTER_TEXT_CHANGED;

public class CreateGroupActivity extends PortraitActivity implements MemberListFragment.MemberListListener, MemberListFragment.MemberClickListener {

    private String TAG = CreateGroupActivity.class.getSimpleName();
    private static final String regexForName = "[^a-zA-Z0-9 ]";


    @BindView(R.id.rl_cg_root)
    RelativeLayout rlCgRoot;

    @BindView(R.id.cg_new_member_list_container)
    RelativeLayout memberListContainer;

    @BindView(R.id.cg_add_member_options)
    FloatingActionMenu addMemberOptions;
    @BindView(R.id.icon_add_member_manually)
    FloatingActionButton addMemberManually;
    @BindView(R.id.icon_add_from_contacts)
    FloatingActionButton addMemberFromContacts;

    @BindView(R.id.tv_counter)
    TextView tvCounter;
    @BindView(R.id.cg_bt_save)
    Button btnSelection;
    @BindView(R.id.et_group_description)
    EditText et_group_description;

    @BindView(R.id.cg_txt_toolbar)
    TextView txtToolbar;
    @BindView(R.id.et_groupname)
    EditText et_groupname; // complaints in latest library asking for TextInputEditText but that throws no class errors, todo: fix
    @BindView(R.id.cg_iv_crossimage)
    ImageView ivCrossimage;

    private Map<String, Member> mapMembersContacts;
    private MemberListFragment memberListFragment;

    private Snackbar snackBar;
    private GrassrootRestService grassrootRestService;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create__group);
        ButterKnife.bind(this);

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Please wait..");

        init();
        setUpViews();
        setUpMemberList();
    }

    private void init() {
        memberListFragment = new MemberListFragment();
        mapMembersContacts = new HashMap<>();
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
        finish();
    }

    @OnTextChanged(value = R.id.et_group_description, callback = AFTER_TEXT_CHANGED)
    public void changeLengthCounter(CharSequence s) {
        tvCounter.setText("" + s.length() + "/" + "160");
    }

    @OnClick(R.id.icon_add_from_contacts)
    public void icon_add_from_contacts() {
        try {
            addMemberOptions.close(true);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (!PermissionUtils.contactReadPermissionGranted(this)) {
            PermissionUtils.requestReadContactsPermission(this);
        } else {
            ArrayList<Contact> preSelectedList = new ArrayList<>(Contact.convertFromMembers(memberListFragment.getSelectedMembers()));
            Log.e(TAG, "calling phone book, with these pre-selected: " + preSelectedList.toString());
            UtilClass.callPhoneBookActivity(this, preSelectedList, null);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == Constant.alertAskForContactPermission && grantResults.length > 0) {
            ArrayList<Contact> preSelectedList = new ArrayList<>(Contact.convertFromMembers(memberListFragment.getSelectedMembers()));
            PermissionUtils.checkPermGrantedAndLaunchPhoneBook(this, grantResults[0], preSelectedList);
        }
    }

    @OnClick(R.id.icon_add_member_manually)
    public void ic_edit_call() {
        try {
            addMemberOptions.close(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
        startActivityForResult(new Intent(CreateGroupActivity.this, AddContactManually.class), Constant.activityManualMemberEntry);
    }

    @OnClick(R.id.cg_bt_save)
    public void save() {
        try {
            addMemberOptions.close(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
        validate_allFields();

    }

    private void validate_allFields() {
        if (!(TextUtils.isEmpty(et_groupname.getText().toString().trim().replaceAll(regexForName, "")))) {
            createGroup();
        } else {
            snackBar(getApplicationContext(), getResources().getString(R.string.et_groupname), "", Snackbar.LENGTH_SHORT);
        }
    }

    private void createGroup(){

        showProgress();
        String mobileNumber = PreferenceUtils.getuser_mobilenumber(CreateGroupActivity.this);
        String code = PreferenceUtils.getuser_token(CreateGroupActivity.this);
        String groupName = et_groupname.getText().toString().trim().replaceAll(regexForName, "");
        String groupDescription = et_group_description.getText().toString().trim();

        List<Member> groupMembers = memberListFragment.getSelectedMembers();

        grassrootRestService.getApi()
                .createGroupNew(mobileNumber, code, groupName, groupDescription, groupMembers)
                .enqueue(new Callback<GroupResponse>() {
                    @Override
                    public void onResponse(Call<GroupResponse> call, Response<GroupResponse> response) {
                        if (response.isSuccessful()) {
                            hideProgress();
                            PreferenceUtils.setisHasgroup(getApplicationContext(), true);
                            Intent resultIntent = new Intent();
                            Log.e(TAG, "here's the response body: " + response.body().toString());
                            resultIntent.putExtra(GroupConstants.OBJECT_FIELD, response.body().getGroups().get(0));
                            setResult(RESULT_OK, resultIntent);
                            Log.e(TAG, "returning group created! with UID : " + response.body().getGroups().get(0).getGroupUid());
                            EventBus.getDefault().post(new GroupCreatedEvent());
                            finish();
                        } else {
                            // todo: process and handle error, if any (shouldn't be)
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

        List<Member> membersToAdd = new ArrayList<>();
        List<Member> membersToRemove = new ArrayList<>();

        if (resultCode == Activity.RESULT_OK && data != null) {
            if (requestCode == Constant.activityContactSelection) {
                final ArrayList<Contact> contactsAdded = data.getParcelableArrayListExtra(Constant.contactsAdded);
                for (Contact c : contactsAdded) {
                    Member m = new Member(c.selectedNumber, c.name, Constant.ROLE_ORDINARY_MEMBER, c.contact_ID);
                    mapMembersContacts.put(c.contact_ID, m);
                    membersToAdd.add(m);
                }
                final ArrayList<Contact> contactsRemoved = data.getParcelableArrayListExtra(Constant.contactsRemoved);
                for (Contact c : contactsRemoved) {
                    // todo : null pointer checks etc
                    membersToRemove.add(mapMembersContacts.get(c.contact_ID));
                }
            } else if (requestCode == Constant.activityManualMemberEntry) {
                Member newMember = new Member(data.getStringExtra("selectedNumber"), data.getStringExtra("name"),
                        Constant.ROLE_ORDINARY_MEMBER, null);
                membersToAdd.add(newMember);
            }

            // todo : optimizing & cleaning these (e.g., going to call notify data changed twice ...)

            if (!membersToAdd.isEmpty())
                memberListFragment.addMembers(membersToAdd);
            if (!membersToRemove.isEmpty())
                memberListFragment.removeMembers(membersToRemove);
        }
    }

    private void showProgress(){
        progressDialog.show();
    }

    private void hideProgress(){
        progressDialog.dismiss();
    }

    private void snackBar(Context applicationContext, String message, String Action_title, int lengthShort) {
        snackBar = Snackbar.make(rlCgRoot, message, lengthShort);
        if (!Action_title.isEmpty()) {
            snackBar.setAction(Action_title, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    createGroup();
                }
            });
        }
        snackBar.show();
    }

    @Override
    public void onMemberListInitiated(MemberListFragment fragment) {
        // todo: use this to handle fragment setting up & observation, instead of create at start...
        memberListFragment.setShowSelected(true);
        memberListFragment.setCanDismissItems(true);
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
