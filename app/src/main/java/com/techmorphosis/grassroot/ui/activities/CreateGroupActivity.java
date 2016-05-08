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
import com.techmorphosis.grassroot.Interface.ClickListener;
import com.techmorphosis.grassroot.R;
import com.techmorphosis.grassroot.RecyclerView.RecyclerTouchListener;
import com.techmorphosis.grassroot.adapters.MemberListAdapter;
import com.techmorphosis.grassroot.models.SingleContact;
import com.techmorphosis.grassroot.services.GrassrootRestService;
import com.techmorphosis.grassroot.services.model.GenericResponse;
import com.techmorphosis.grassroot.utils.Constant;
import com.techmorphosis.grassroot.utils.PermissionUtils;
import com.techmorphosis.grassroot.utils.SettingPreference;
import com.techmorphosis.grassroot.utils.UtilClass;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import rx.Observer;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

// subclass AppCompatActivity
public class CreateGroupActivity extends PortraitActivity {

    private MemberListAdapter mAdapter;
    private String TAG = CreateGroupActivity.class.getSimpleName();
    private static final String regexForName = "[^a-zA-Z0-9 ]";

    @BindView(R.id.rc_members_list)
    RecyclerView mRecyclerView;
    @BindView(R.id.add_member_options)
    FloatingActionMenu addMemberOptions;
    @BindView(R.id.icon_add_member_manually)
    FloatingActionButton ic_edit;
    @BindView(R.id.icon_add_from_contacts)
    FloatingActionButton ic_fab_call;

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
    EditText et_groupname;
    @BindView(R.id.rl_cg_root)
    RelativeLayout rlCgRoot;
    @BindView(R.id.iv_crossimage)
    ImageView ivCrossimage;

    //all phonebooklist
    private ArrayList<SingleContact> phoneBookList;
    private ArrayList<SingleContact> phoneBookFilteredList;
    private ArrayList<SingleContact> manualInputList;
    private ArrayList<SingleContact> mergeList;

    private Snackbar snackBar;
    private GrassrootRestService grassrootRestService = new GrassrootRestService();
    private ProgressDialog progressDialog;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create__group);
        ButterKnife.bind(this);
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Please wait..");
        findAllView();
        init();
        mRecyclerView();
        et_group_description.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                tvCounter.setText("" + s.length() + "/" + "160");
            }
        });

    }

    private void init() {
        this.phoneBookList = new ArrayList();
        this.mergeList = new ArrayList();
        this.phoneBookFilteredList = new ArrayList();
        this.manualInputList = new ArrayList<>();
    }

    private void mRecyclerView() {
        this.mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        this.mAdapter = new MemberListAdapter(this.mergeList, getApplicationContext());
        this.mRecyclerView.setAdapter(this.mAdapter);
        this.mRecyclerView.addOnItemTouchListener(new RecyclerTouchListener(getApplicationContext(), this.mRecyclerView, new ClickListener() {
            @Override
            public void onClick(View view, int position) {
                Log.e(TAG, "position is  " + position);
                SingleContact click_model = (SingleContact) CreateGroupActivity.this.mergeList.get(position);
                if (click_model == null) {
                    Log.e(CreateGroupActivity.this.TAG, "click_model  is  " + null);
                } else {
                    Log.e(CreateGroupActivity.this.TAG, "click_model  is not null ");
                }
                if (click_model.isSelected) {
                    click_model.isSelected = false;
                    CreateGroupActivity.this.mAdapter.notifyDataSetChanged();
                    return;
                }
                click_model.isSelected = true;
                CreateGroupActivity.this.mAdapter.notifyDataSetChanged();
            }

            @Override
            public void onLongClick(View view, int position) {

            }
        }));
    }


    private void findAllView() {
        CollapsingToolbarLayout collapsingToolbar = (CollapsingToolbarLayout) findViewById(R.id.collapsing_toolbar);
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

    @OnClick(R.id.iv_crossimage)
    public void ivCrossimage() {
        finish();
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
            UtilClass.callPhoneBookActivity(this, phoneBookFilteredList);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == Constant.alertAskForContactPermission && grantResults.length > 0) {
            PermissionUtils.checkPermGrantedAndLaunchPhoneBook(this, grantResults[0], phoneBookFilteredList);
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
        for (int i = 0; i < mergeList.size(); i++) {
            SingleContact numbers = mergeList.get(i);
            if (numbers.isSelected) {
                tempList.add(numbers.selectedNumber);
            }
        }

        String[] phoneNumbers = tempList.toArray(new String[tempList.size()]);
        grassrootRestService.getApi()
                .createGroup(mobileNumber, code, groupName, groupDescription, phoneNumbers)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<GenericResponse>() {
                    @Override
                    public void onCompleted() {
                        hideProgress();
                    }
                    @Override
                    public void onError(Throwable e) {
                        hideProgress();
                        snackBar(CreateGroupActivity.this, "", "", Snackbar.LENGTH_SHORT);
                    }
                    @Override
                    public void onNext(GenericResponse response) {
                        SettingPreference.setPrefHasSaveClicked(CreateGroupActivity.this, true);
                        snackBar(CreateGroupActivity.this, getResources().getString(R.string.GROUP_CREATED), "", Snackbar.LENGTH_SHORT);
                        SettingPreference.setisHasgroup(CreateGroupActivity.this, true);
                        finish();
                    }
                });

    Log.e(TAG, "groupName is " + groupName);
    Log.e(TAG, "description is " + groupDescription);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        mergeList.clear();

        if (resultCode == Activity.RESULT_OK && requestCode == Constant.activityContactSelection) {
            Log.e(this.TAG, "resultCode==1 ");
            if (data != null) {

                phoneBookList = new ArrayList<>();
                phoneBookFilteredList = new ArrayList<>();

                this.phoneBookList = data.getParcelableArrayListExtra(Constant.phoneBookList);

                Log.e(this.TAG, "phoneBookList size is " + this.phoneBookList.size());

                for (int i = 0; i < this.phoneBookList.size(); i++) {
                    SingleContact sortedmodel = (SingleContact) this.phoneBookList.get(i);
                    if (sortedmodel == null) {
                        Log.e(this.TAG, "null");
                    } else if (sortedmodel.isSelected) {
                        this.phoneBookFilteredList.add(sortedmodel);
                    }
                }

                mergeList.addAll(phoneBookFilteredList);
                mergeList.addAll(manualInputList);

                Log.e(this.TAG, "mergeList size is " + this.mergeList.size());
                Log.e(this.TAG, "phoneBookFilteredList size is " + this.phoneBookFilteredList.size());

                CreateGroupActivity.this.mAdapter.notifyDataSetChanged();
            }

        } else if (resultCode == Activity.RESULT_OK && requestCode == Constant.activityManualMemberEntry) {

            Log.d(TAG, "inside createGroupActivity ... came back with result code == " + Constant.activityManualMemberEntry);
            String name = data.getStringExtra("name");
            String selectedNumber = data.getStringExtra("selectedNumber");
            SingleContact singleContact = new SingleContact();
            singleContact.isSelected = true;
            singleContact.name = name;
            singleContact.selectedNumber = selectedNumber;
            this.manualInputList.add(singleContact);
            Log.e(this.TAG, "manualInputList.size() " + this.manualInputList.size());

            mergeList.addAll(phoneBookFilteredList);
            mergeList.addAll(manualInputList);

            Log.e(this.TAG, "mergeList.size() " + this.mergeList.size());

            CreateGroupActivity.this.mAdapter.notifyDataSetChanged();
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
