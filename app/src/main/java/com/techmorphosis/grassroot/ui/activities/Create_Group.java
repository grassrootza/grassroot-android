package com.techmorphosis.grassroot.ui.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.android.volley.NoConnectionError;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.github.clans.fab.FloatingActionButton;
import com.github.clans.fab.FloatingActionMenu;
import com.techmorphosis.grassroot.Interface.ClickListener;
import com.techmorphosis.grassroot.Network.AllLinsks;
import com.techmorphosis.grassroot.Network.NetworkCall;
import com.techmorphosis.grassroot.R;
import com.techmorphosis.grassroot.RecyclerView.RecyclerTouchListener;
import com.techmorphosis.grassroot.adapters.CreateGroupAdapter;
import com.techmorphosis.grassroot.models.ContactsModel;
import com.techmorphosis.grassroot.utils.Constant;
import com.techmorphosis.grassroot.utils.SettingPreffrence;
import com.techmorphosis.grassroot.utils.UtilClass;
import com.techmorphosis.grassroot.utils.listener.ErrorListenerVolley;
import com.techmorphosis.grassroot.utils.listener.ResponseListenerVolley;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;

public class Create_Group extends PortraitActivity {

    private RecyclerView mRecyclerView;
    private CreateGroupAdapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;
   public static FloatingActionMenu menu2;
    private FloatingActionButton ic_edit;
    private FloatingActionButton ic_fab_call;

    private LinearLayout llToolbar;
    private RelativeLayout rlOne;
    private TextInputLayout et1;
    private TextInputLayout et2;
    private TextView tvCounter;
    private TextView tvRcTitle;
    private RecyclerView rcContacts;


    private ArrayList<ContactsModel> mergeList;

    //all phonebooklist
    private ArrayList<ContactsModel> phonebookList;

    private ArrayList<ContactsModel> phonebook_filterdList;

    private ArrayList<ContactsModel> manuallList;


    private Button btnSelection;
    private EditText et_group_description;
    private TextView txtToolbar;
    private String TAG=Create_Group.class.getSimpleName();
    private EditText et_groupname;
    private RelativeLayout rlCgRoot;
    private Snackbar snackBar;
    private UtilClass utilClass;
    private ImageView ivCrossimage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create__group);

        findAllView();
        init();
        mRecyclerView();

        et_group_description.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
             //   Log.e(TAG,"before count is " + count);
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
               // Log.e(TAG,"onTextChanged count is " + count);

            }

            @Override
            public void afterTextChanged(Editable s) {
              //  Log.e(TAG,"afterTextChanged length  is " + s.length());
                tvCounter.setText("" + s.length() + "/" + "320");

            }
        });


    }

    private void init() {
        this.phonebookList = new ArrayList();
        this.mergeList = new ArrayList();
        this.phonebook_filterdList = new ArrayList();
        this.manuallList = new ArrayList<>();
        utilClass = new UtilClass();

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


    private void findAllView()
    {
        CollapsingToolbarLayout collapsingToolbar = (CollapsingToolbarLayout) findViewById(R.id.collapsing_toolbar);
        menu2 = (FloatingActionMenu) findViewById(R.id.menu2);

        rlCgRoot = (RelativeLayout) findViewById(R.id.rl_cg_root);
        ic_edit = (FloatingActionButton) findViewById(R.id.ic_edit);
        ic_fab_call = (FloatingActionButton) findViewById(R.id.ic_fab_call);

        ivCrossimage = (ImageView) findViewById(R.id.iv_crossimage);
        
        ic_edit.setOnClickListener(ic_edit_call());
        ic_fab_call.setOnClickListener(ic_fab_call());

        txtToolbar = (TextView) findViewById(R.id.txt_toolbar);
        rlOne = (RelativeLayout) findViewById(R.id.rl_one);
        et_groupname = (EditText) findViewById(R.id.et_groupname);
        et_group_description = (EditText) findViewById(R.id.et_group_description);
        tvCounter = (TextView) findViewById(R.id.tv_counter);
        mRecyclerView = (RecyclerView) findViewById(R.id.rc_contacts);
        btnSelection = (Button) findViewById(R.id.bt_save);
        btnSelection.setOnClickListener(Save());
        ivCrossimage.setOnClickListener(ivCrossimage());

        Animation animation1 = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fab);
        menu2.startAnimation(animation1);
        menu2.setVisibility(View.VISIBLE);

        menu2.setOnMenuToggleListener(new FloatingActionMenu.OnMenuToggleListener() {
            @Override
            public void onMenuToggle(boolean opened) {
                String text = "";
                if (opened) {
                    ic_fab_call.setVisibility(View.VISIBLE);
                    ic_edit.setVisibility(View.VISIBLE);
                    text = "Menu opened";
                   // menu2.addMenuButton(programFab2);

                } else {
                    ic_fab_call.setVisibility(View.GONE);
                    ic_edit.setVisibility(View.GONE);

                    text = "Menu closed";
                   // menu2.removeMenuButton(programFab2);

                }
              //  Toast.makeText(Create_Group.this, text, Toast.LENGTH_SHORT).show();
            }
        });


    }

    private View.OnClickListener ivCrossimage() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        };
    }

    private View.OnClickListener ic_fab_call() {
            return  new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    try {
                        menu2.close(true);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    Intent phonebook_intent=new Intent(Create_Group.this, PhoneBookContacts.class);
                    phonebook_intent.putParcelableArrayListExtra(Constant.filterdList, phonebook_filterdList);
                    startActivityForResult(phonebook_intent,1);
                }
            };
    }

    private View.OnClickListener ic_edit_call() {
            return  new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        menu2.close(true);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    Create_Group.this.startActivityForResult(new Intent(Create_Group.this, AddContactManually.class), 2);

                }
            };
    }

    private View.OnClickListener Save() {
        return  new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {

                try {
                    menu2.close(true);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                validate_allFields();

            }
        };
    }

    private void validate_allFields()
    {

        if ( !(TextUtils.isEmpty(et_groupname.getText().toString().trim().replaceAll("[^a-zA-Z0-9 ]", "")) ))
        {
            Group_CreationWS();
        }
        else
        {
            snackBar(getApplicationContext(),getResources().getString(R.string.et_groupname),"", Snackbar.LENGTH_SHORT);
        }
    }



    private void Group_CreationWS()
    {

        NetworkCall networkCall = new NetworkCall(Create_Group.this, new ResponseListenerVolley()
        {
            @Override
            public void onSuccess(String s)
            {

                String status;

                try {
                    JSONObject jsonsuccess = new JSONObject(s);
                    status=jsonsuccess.getString("status");
                    if (status.equalsIgnoreCase("SUCCESS"))
                    {
                        SettingPreffrence.setPrefHasSaveClicked(Create_Group.this, true);
                        snackBar(Create_Group.this,getResources().getString(R.string.GROUP_CREATED),"",Snackbar.LENGTH_SHORT);
                        SettingPreffrence.setisHasgroup(Create_Group.this,true);
                        finish();
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                    utilClass.showsnackBar(rlCgRoot, Create_Group.this, getString(R.string.Unknown_error));

                }

            }
        }
                ,
                new ErrorListenerVolley()
                {
                    @Override
                    public void onError(VolleyError volleyError) {

                        if ((volleyError instanceof NoConnectionError) || (volleyError instanceof TimeoutError))
                        {
                            snackBar(Create_Group.this,getResources().getString(R.string.No_network),"Retry",Snackbar.LENGTH_LONG);

                        }
                        else
                        {
                            try {
                                String responseBody= new String(volleyError.networkResponse.data,"utf-8");
                                Log.e(TAG,"responseBody is " + responseBody);
                                JSONObject jsonObject_error= new JSONObject(responseBody);
                                String status = jsonObject_error.getString("status");
                                String message = jsonObject_error.getString("message");
                                if (status.equalsIgnoreCase("FAILURE"))
                                {
                                    snackBar(Create_Group.this,message,"",Snackbar.LENGTH_SHORT);
                                }

                            } catch (UnsupportedEncodingException e) {
                                e.printStackTrace();
                            } catch (JSONException e) {
                                e.printStackTrace();
                                    utilClass.showsnackBar(rlCgRoot,Create_Group.this,getString(R.string.Unknown_error));

                            }


                        }
                    }

                },

                AllLinsks.groupcreation + SettingPreffrence.getuser_mobilenumber(Create_Group.this) + "/" +  SettingPreffrence.getuser_token(Create_Group.this),

                getResources().getString(R.string.prg_message),

                true
        );

        HashMap<String,String> hashMap= new HashMap<>();
        hashMap.put("groupName" ,et_groupname.getText().toString().trim().replaceAll("[^a-zA-Z0-9 ]", ""));
        hashMap.put("description" ,et_group_description.getText().toString().trim().replaceAll("[^a-zA-Z0-9 ]", ""));
        for (int i = 0; i <  mergeList.size(); i++)
        {
            ContactsModel numbers= mergeList.get(i);
            if (numbers.isSelected)
            {
                hashMap.put("phoneNumbers["+i+"]",numbers.selectedNumber);
            }

        }
        networkCall.makeStringRequest_POST(hashMap);

        Log.e(TAG, "groupName is " + et_groupname.getText().toString().trim().replaceAll("[^a-zA-Z0-9 ]", ""));
        Log.e(TAG,"description is " + et_group_description.getText().toString().trim().replaceAll("[^a-zA-Z0-9 ]", ""));

    }


    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

       // mergeList = new ArrayList<>();
        mergeList.clear();
        if (resultCode == 1 && requestCode == 1) {

            Log.e(this.TAG, "resultCode==1 ");
            if (data != null) {
                phonebookList = new ArrayList<>();
                phonebook_filterdList = new ArrayList<>();

                this.phonebookList = data.getParcelableArrayListExtra(Constant.PhoneBook_list);

                Log.e(this.TAG, "phonebookList size is " + this.phonebookList.size());

                for (int i = 0; i < this.phonebookList.size(); i++)
                {
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


    private void snackBar(Context applicationContext, String message,String Action_title,int lengthShort)
    {

        snackBar=Snackbar.make(rlCgRoot,message,lengthShort);
        if (!Action_title.isEmpty())
        {
            snackBar.setAction(Action_title, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Group_CreationWS();
                }
            });
        }
        snackBar.show();
    }
}
