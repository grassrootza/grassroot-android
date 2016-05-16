package com.techmorphosis.grassroot.ui.activities;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
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
import com.techmorphosis.grassroot.R;
import com.techmorphosis.grassroot.models.Contact;
import com.techmorphosis.grassroot.services.GrassrootRestService;
import com.techmorphosis.grassroot.services.model.GenericResponse;
import com.techmorphosis.grassroot.services.model.Member;
import com.techmorphosis.grassroot.ui.fragments.MemberListFragment;
import com.techmorphosis.grassroot.utils.Constant;
import com.techmorphosis.grassroot.utils.ErrorUtils;
import com.techmorphosis.grassroot.utils.PermissionUtils;
import com.techmorphosis.grassroot.utils.SettingPreference;
import com.techmorphosis.grassroot.utils.UtilClass;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnTextChanged;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static butterknife.OnTextChanged.Callback.AFTER_TEXT_CHANGED;

// subclass AppCompatActivity
public class CreateGroupActivity extends PortraitActivity implements MemberListFragment.MemberListListener {

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

    @Nullable
    @BindView(R.id.rl_one)
    RelativeLayout rlOne;
    @BindView(R.id.tv_counter)
    TextView tvCounter;
    @BindView(R.id.cg_bt_save)
    Button btnSelection;
    @BindView(R.id.et_group_description)
    EditText et_group_description;

    @BindView(R.id.cg_txt_toolbar)
    TextView txtToolbar;
    @BindView(R.id.et_groupname)
    EditText et_groupname; // complaints in latest library asking for TextInputEditText but that throws no class errors..
    @BindView(R.id.cg_iv_crossimage)
    ImageView ivCrossimage;

    //all phonebooklist
    private List<Member> membersFromContacts;
    private List<Member> membersFromManual;

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
        grassrootRestService = new GrassrootRestService(this);
        membersFromContacts = new ArrayList<>();
        membersFromManual = new ArrayList<>();
        memberListFragment = new MemberListFragment();
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
            ArrayList<Contact> preSelectedList = new ArrayList<>(Contact.convertFromMembers(membersFromContacts, true));
            Log.e(TAG, "calling phone book, with these pre-selected: " + preSelectedList.toString());
            UtilClass.callPhoneBookActivity(this, preSelectedList, null);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == Constant.alertAskForContactPermission && grantResults.length > 0) {
            ArrayList<Contact> preSelectedList = new ArrayList<>(Contact.convertFromMembers(membersFromContacts, true));
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
            groupCreationWS();
        } else {
            snackBar(getApplicationContext(), getResources().getString(R.string.et_groupname), "", Snackbar.LENGTH_SHORT);
        }
    }

    private void groupCreationWS(){

        showProgress();
        String mobileNumber = SettingPreference.getuser_mobilenumber(CreateGroupActivity.this);
        String code = SettingPreference.getuser_token(CreateGroupActivity.this);
        String groupName = et_groupname.getText().toString().trim().replaceAll(regexForName, "");
        String groupDescription = et_group_description.getText().toString().trim();

        List<Member> groupMembers = new ArrayList<>(membersFromContacts);
        groupMembers.addAll(membersFromManual);

        grassrootRestService.getApi()
                .createGroupNew(mobileNumber, code, groupName, groupDescription, groupMembers)
                .enqueue(new Callback<GenericResponse>() {
                    @Override
                    public void onResponse(Call<GenericResponse> call, Response<GenericResponse> response) {
                        hideProgress(); // note : this is leaking...
                        if (response.isSuccessful()) {
                            SettingPreference.setPrefHasSaveClicked(CreateGroupActivity.this, true);
                            snackBar(CreateGroupActivity.this, getResources().getString(R.string.GROUP_CREATED), "", Snackbar.LENGTH_SHORT);
                            SettingPreference.setisHasgroup(CreateGroupActivity.this, true);
                            finish();
                        } else {
                            // todo: process and handle error, if any (shouldn't be)
                        }
                    }

                    @Override
                    public void onFailure(Call<GenericResponse> call, Throwable t) {
                        hideProgress();
                        ErrorUtils.handleNetworkError(CreateGroupActivity.this, rlCgRoot, t);
                    }
                });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK && data != null) {
            if (requestCode == Constant.activityContactSelection) {
                ArrayList<Contact> contactsSelected = data.getParcelableArrayListExtra(Constant.selectedContacts);
                membersFromContacts = Contact.convertToMembers(contactsSelected, Constant.ROLE_ORDINARY_MEMBER);
            } else if (requestCode == Constant.activityManualMemberEntry) {
                Member newMember = new Member(data.getStringExtra("selectedNumber"), data.getStringExtra("name"),
                        Constant.ROLE_ORDINARY_MEMBER, null);
                membersFromManual.add(newMember);
            }

            memberListFragment.setMemberList(membersFromContacts);
            memberListFragment.addMembers(membersFromManual);
        }
    }

    private void showProgress(){
        progressDialog.show();
    }

    private void hideProgress(){
        progressDialog.hide();
    }

    private void snackBar(Context applicationContext, String message, String Action_title, int lengthShort) {
        snackBar = Snackbar.make(rlCgRoot, message, lengthShort);
        if (!Action_title.isEmpty()) {
            snackBar.setAction(Action_title, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    groupCreationWS();
                }
            });
        }
        snackBar.show();
    }

    @Override
    public void onMemberListInitiated(MemberListFragment fragment) {
        // todo: use this to handle fragment setting up & observation, instead of create at start...
    }
}
