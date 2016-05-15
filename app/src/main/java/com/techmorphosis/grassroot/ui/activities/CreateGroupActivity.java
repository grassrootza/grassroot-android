package com.techmorphosis.grassroot.ui.activities;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
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
import com.techmorphosis.grassroot.adapters.MemberListAdapter;
import com.techmorphosis.grassroot.adapters.UserListAdapter;
import com.techmorphosis.grassroot.interfaces.ClickListener;
import com.techmorphosis.grassroot.models.Contact;
import com.techmorphosis.grassroot.services.GrassrootRestService;
import com.techmorphosis.grassroot.services.model.GenericResponse;
import com.techmorphosis.grassroot.services.model.Member;
import com.techmorphosis.grassroot.ui.fragments.MemberListFragment;
import com.techmorphosis.grassroot.ui.views.RecyclerTouchListener;
import com.techmorphosis.grassroot.utils.Constant;
import com.techmorphosis.grassroot.utils.ContactUtil.ErrorUtils;
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
public class CreateGroupActivity extends PortraitActivity {

    private MemberListAdapter mAdapter;
    private UserListAdapter memberAdapter;

    private String TAG = CreateGroupActivity.class.getSimpleName();
    private static final String regexForName = "[^a-zA-Z0-9 ]";

    @BindView(R.id.cg_new_member_list_container)
    RelativeLayout memberListContainer;
    @BindView(R.id.add_member_options)
    FloatingActionMenu addMemberOptions;
    @BindView(R.id.icon_add_member_manually)
    FloatingActionButton ic_edit;
    @BindView(R.id.icon_add_from_contacts)
    FloatingActionButton ic_fab_call;

    @BindView(R.id.cg_collapsing_toolbar)
    CollapsingToolbarLayout collapsingToolbarLayout;

    @Nullable
    @BindView(R.id.rl_one)
    RelativeLayout rlOne;
    @BindView(R.id.tv_counter)
    TextView tvCounter;
    @BindView(R.id.bt_save)
    Button btnSelection;
    @BindView(R.id.et_group_description)
    EditText et_group_description;
    @BindView(R.id.txt_toolbar)
    TextView txtToolbar;
    @BindView(R.id.et_groupname)
    EditText et_groupname; // complaints in latest library asking for TextInputEditText but that throws no class errors..
    @BindView(R.id.rl_cg_root)
    RelativeLayout rlCgRoot;
    @BindView(R.id.iv_crossimage)
    ImageView ivCrossimage;

    //all phonebooklist
    private List<Member> membersForGroup;
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

    }

    private void init() {
        grassrootRestService = new GrassrootRestService(this);
        membersForGroup = new ArrayList<>();
    }

    private void setUpViews() {

        addMemberOptions.setOnMenuToggleListener(new FloatingActionMenu.OnMenuToggleListener() {
            @Override
            public void onMenuToggle(boolean opened) {
                if (opened) {
                    ic_fab_call.setVisibility(View.VISIBLE);
                    ic_edit.setVisibility(View.VISIBLE);
                    Log.d(TAG, "Menu opened");

                } else {
                    ic_fab_call.setVisibility(View.GONE);
                    ic_edit.setVisibility(View.GONE);
                    Log.d(TAG, "Menu closed");
                }
            }
        });
    }

    private void setUpMemberList() {
        memberListFragment = new MemberListFragment();
        memberListFragment.setMemberList(membersForGroup);
        getSupportFragmentManager().beginTransaction()
                .add(R.id.cg_new_member_list_container, memberListFragment)
                .commit();

        // todo: move this into the fragment, enabled/disabled by a boolean
        /*this.mRecyclerView.addOnItemTouchListener(new RecyclerTouchListener(this, this.mRecyclerView, new ClickListener() {
            @Override
            public void onClick(View view, int position) {
                // todo: integrate selected / not selected back into adapter
                Log.e(TAG, "position is  " + position);
                Contact clickedContact = CreateGroupActivity.this.mergeList.get(position);
                if (clickedContact == null) {
                    Log.e(CreateGroupActivity.this.TAG, "click_model  is  " + null);
                } else {
                    Log.e(CreateGroupActivity.this.TAG, "click_model  is not null ");
                }
                if (clickedContact.isSelected) {
                    clickedContact.isSelected = false;
                    memberAdapter.notifyDataSetChanged();
                    return;
                }
                clickedContact.isSelected = true;
                CreateGroupActivity.this.mAdapter.notifyDataSetChanged();
            }

            @Override
            public void onLongClick(View view, int position) {

            }
        }));*/
    }

    @OnClick(R.id.iv_crossimage)
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
            // todo: reconsider this quite a bit
            ArrayList<Contact> preSelectedList = new ArrayList<>(Contact.convertFromMembers(membersForGroup, true));
            UtilClass.callPhoneBookActivity(this, preSelectedList, null);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == Constant.alertAskForContactPermission && grantResults.length > 0) {
            ArrayList<Contact> preSelectedList = new ArrayList<>(Contact.convertFromMembers(membersForGroup, true));
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

    @OnClick(R.id.bt_save)
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

        ArrayList<String> tempList = new ArrayList<>();
        for (Member member : membersForGroup) {
            tempList.add(member.getPhoneNumber());
        }

        String[] phoneNumbers = tempList.toArray(new String[tempList.size()]);
        grassrootRestService.getApi()
                .createGroup(mobileNumber, code, groupName, groupDescription, phoneNumbers) // todo: pass member entities
                .enqueue(new Callback<GenericResponse>() {
                    @Override
                    public void onResponse(Call<GenericResponse> call, Response<GenericResponse> response) {
                        hideProgress();
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

        Log.e(TAG, "groupName is " + groupName);
        Log.e(TAG, "description is " + groupDescription);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        membersForGroup.clear(); // todo : probably don't want to do this, doing for now, until properly straighten out

        if (resultCode == Activity.RESULT_OK && requestCode == Constant.activityContactSelection) {
            Log.e(this.TAG, "resultCode==1 ");

            if (data != null) {
                ArrayList<Contact> contactsSelected = data.getParcelableArrayListExtra(Constant.phoneBookList);
                for (Contact contact : contactsSelected) {
                    membersForGroup.add(new Member(contact.selectedNumber, contact.name, Constant.ROLE_ORDINARY_MEMBER));
                }
                memberListContainer.setVisibility(View.VISIBLE);
                setUpMemberList();
            }

        } else if (resultCode == Activity.RESULT_OK && requestCode == Constant.activityManualMemberEntry) {

            Log.d(TAG, "inside createGroupActivity ... came back with result code == " + Constant.activityManualMemberEntry);
            String name = data.getStringExtra("name");
            String selectedNumber = data.getStringExtra("selectedNumber");
            Contact contact = new Contact();
            contact.isSelected = true;
            contact.name = name;
            contact.selectedNumber = selectedNumber;
            // this.manualInputList.add(contact);
            // Log.e(this.TAG, "manualInputList.size() " + this.manualInputList.size());

            // mergeList.addAll(phoneBookFilteredList);
            // mergeList.addAll(manualInputList);

            // Log.e(this.TAG, "mergeList.size() " + this.mergeList.size());

            memberAdapter.notifyDataSetChanged();
        }
    }

    private void showProgress(){
        progressDialog.show();
    }

    private void hideProgress(){
        progressDialog.show();
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
}
