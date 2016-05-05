package com.techmorphosis.grassroot.ui.activities;

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
import com.techmorphosis.grassroot.adapters.CreateGroupAdapter;
import com.techmorphosis.grassroot.models.ContactsModel;
import com.techmorphosis.grassroot.services.GrassrootRestService;
import com.techmorphosis.grassroot.services.model.GenericResponse;
import com.techmorphosis.grassroot.utils.Constant;
import com.techmorphosis.grassroot.utils.SettingPreference;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import rx.Observer;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class Create_Group extends PortraitActivity {

    @BindView(R.id.rc_contacts)
    RecyclerView mRecyclerView;
    private CreateGroupAdapter mAdapter;
    @BindView(R.id.menu2)
    FloatingActionMenu menu2;
    @BindView(R.id.ic_edit)
    FloatingActionButton ic_edit;
    @BindView(R.id.ic_fab_call)
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
    private String TAG = Create_Group.class.getSimpleName();
    @BindView(R.id.et_groupname)
    EditText et_groupname;
    @BindView(R.id.rl_cg_root)
    RelativeLayout rlCgRoot;
    private ArrayList<ContactsModel> mergeList;
    @BindView(R.id.iv_crossimage)
    ImageView ivCrossimage;

    //all phonebooklist
    private ArrayList<ContactsModel> phonebookList;
    private ArrayList<ContactsModel> phonebook_filterdList;
    private ArrayList<ContactsModel> manuallList;
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
        this.phonebookList = new ArrayList();
        this.mergeList = new ArrayList();
        this.phonebook_filterdList = new ArrayList();
        this.manuallList = new ArrayList<>();


    }

    private void mRecyclerView() {
        this.mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        this.mAdapter = new CreateGroupAdapter(this.mergeList, getApplicationContext());
        this.mRecyclerView.setAdapter(this.mAdapter);
        this.mRecyclerView.addOnItemTouchListener(new RecyclerTouchListener(getApplicationContext(), this.mRecyclerView, new ClickListener() {
            @Override
            public void onClick(View view, int position) {
                Log.e(TAG, "position is  " + position);
                ContactsModel click_model = (ContactsModel) Create_Group.this.mergeList.get(position);
                if (click_model == null) {
                    Log.e(Create_Group.this.TAG, "click_model  is  " + null);
                } else {
                    Log.e(Create_Group.this.TAG, "click_model  is not null ");
                }
                if (click_model.isSelected) {
                    click_model.isSelected = false;
                    Create_Group.this.mAdapter.notifyDataSetChanged();
                    return;
                }
                click_model.isSelected = true;
                Create_Group.this.mAdapter.notifyDataSetChanged();
            }

            @Override
            public void onLongClick(View view, int position) {

            }
        }));
    }


    private void findAllView() {
        CollapsingToolbarLayout collapsingToolbar = (CollapsingToolbarLayout) findViewById(R.id.collapsing_toolbar);
        menu2.setOnMenuToggleListener(new FloatingActionMenu.OnMenuToggleListener() {
            @Override
            public void onMenuToggle(boolean opened) {
                String text = "";
                if (opened) {
                    ic_fab_call.setVisibility(View.VISIBLE);
                    ic_edit.setVisibility(View.VISIBLE);
                    text = "Menu opened";

                } else {
                    ic_fab_call.setVisibility(View.GONE);
                    ic_edit.setVisibility(View.GONE);
                    text = "Menu closed";


                }
                //  Toast.makeText(Create_Group.this, text, Toast.LENGTH_SHORT).show();
            }
        });

    }

    @OnClick(R.id.iv_crossimage)
    public void ivCrossimage() {
        finish();
    }

    @OnClick(R.id.ic_fab_call)
    public void ic_fab_call() {
        try {
            menu2.close(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
        Intent phonebook_intent = new Intent(Create_Group.this, PhoneBookContacts.class);
        phonebook_intent.putParcelableArrayListExtra(Constant.filterdList, phonebook_filterdList);
        startActivityForResult(phonebook_intent, 1);

    }

    @OnClick(R.id.ic_edit)
    public void ic_edit_call() {

        try {
            menu2.close(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
        Create_Group.this.startActivityForResult(new Intent(Create_Group.this, AddContactManually.class), 2);

    }

    @OnClick(R.id.bt_save)
    public void save() {
        try {
            menu2.close(true);
        } catch (Exception e) {
            e.printStackTrace();
        }

        validate_allFields();

    }

    private void validate_allFields() {

        if (!(TextUtils.isEmpty(et_groupname.getText().toString().trim().replaceAll("[^a-zA-Z0-9 ]", "")))) {
            groupCreationWS();
        } else {
            snackBar(getApplicationContext(), getResources().getString(R.string.et_groupname), "", Snackbar.LENGTH_SHORT);
        }
    }

    private void groupCreationWS(){

        showProgress();
        String mobileNumber = SettingPreference.getuser_mobilenumber(Create_Group.this);
        String code = SettingPreference.getuser_token(Create_Group.this);
        String groupName = et_groupname.getText().toString().trim().replaceAll("[^a-zA-Z0-9 ]", "");
        String desciption = et_group_description.getText().toString().trim().replaceAll("[^a-zA-Z0-9 ]", "");
        ArrayList<String> tempList = new ArrayList<>();
        for (int i = 0; i < mergeList.size(); i++) {
            ContactsModel numbers = mergeList.get(i);
            if (numbers.isSelected) {
                tempList.add(numbers.selectedNumber);
            }
        }
        String[] phoneNumbers = tempList.toArray(new String[tempList.size()]);
        grassrootRestService.getApi()
                .createGroup(mobileNumber, code, groupName, desciption, phoneNumbers)
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
                        snackBar(Create_Group.this, "", "", Snackbar.LENGTH_SHORT);
                    }
                    @Override
                    public void onNext(GenericResponse response) {
                        SettingPreference.setPrefHasSaveClicked(Create_Group.this, true);
                        snackBar(Create_Group.this, getResources().getString(R.string.GROUP_CREATED), "", Snackbar.LENGTH_SHORT);
                        SettingPreference.setisHasgroup(Create_Group.this, true);
                        finish();
                    }
                });

    Log.e(TAG, "groupName is " + et_groupname.getText().toString().trim().replaceAll("[^a-zA-Z0-9 ]", ""));
    Log.e(TAG, "description is " + et_group_description.getText().toString().trim().replaceAll("[^a-zA-Z0-9 ]", ""));
    }


    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        mergeList.clear();
        if (resultCode == 1 && requestCode == 1) {

            Log.e(this.TAG, "resultCode==1 ");
            if (data != null) {
                phonebookList = new ArrayList<>();
                phonebook_filterdList = new ArrayList<>();

                this.phonebookList = data.getParcelableArrayListExtra(Constant.PhoneBook_list);

                Log.e(this.TAG, "phonebookList size is " + this.phonebookList.size());

                for (int i = 0; i < this.phonebookList.size(); i++) {
                    ContactsModel sortedmodel = (ContactsModel) this.phonebookList.get(i);
                    if (sortedmodel == null) {
                        Log.e(this.TAG, "null");
                    } else if (sortedmodel.isSelected) {
                        this.phonebook_filterdList.add(sortedmodel);
                    }
                }

                mergeList.addAll(phonebook_filterdList);
                mergeList.addAll(manuallList);

                Log.e(this.TAG, "mergeList size is " + this.mergeList.size());
                Log.e(this.TAG, "phonebook_filterdList size is " + this.phonebook_filterdList.size());

                Create_Group.this.mAdapter.notifyDataSetChanged();
            }
        } else if (resultCode == 2 && requestCode == 2) {
            Log.e(this.TAG, "resultCode==2");
            String name = data.getStringExtra("name");
            String selectedNumber = data.getStringExtra("selectedNumber");
            ContactsModel contactsModel = new ContactsModel();
            contactsModel.isSelected = true;
            contactsModel.name = name;
            contactsModel.selectedNumber = selectedNumber;
            this.manuallList.add(contactsModel);
            Log.e(this.TAG, "manuallList.size() " + this.manuallList.size());

            mergeList.addAll(phonebook_filterdList);
            mergeList.addAll(manuallList);

            Log.e(this.TAG, "mergeList.size() " + this.mergeList.size());

            Create_Group.this.mAdapter.notifyDataSetChanged();
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
