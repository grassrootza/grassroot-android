package com.techmorphosis.grassroot.ui.activities;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.techmorphosis.grassroot.ContactLib.PinnedHeaderListView;
import com.techmorphosis.grassroot.Interface.GetContactList;
import com.techmorphosis.grassroot.R;
import com.techmorphosis.grassroot.adapters.ContactsAdapter;
import com.techmorphosis.grassroot.adapters.GetContactListAsync;
import com.techmorphosis.grassroot.models.ContactsModel;
import com.techmorphosis.grassroot.utils.Constant;
import com.techmorphosis.grassroot.utils.PermissionUtils;
import com.techmorphosis.grassroot.utils.UtilClass;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


public class PhoneBookContactsActivity extends PortraitActivity implements GetContactList
{

    public static final String TAG = PhoneBookContactsActivity.class.getSimpleName();
    private PinnedHeaderListView mListView;
    private ContactsAdapter mAdapter;
    private Toolbar tbPhonebook;
    private ImageView ivSearch;
    private RelativeLayout rlSimple;
    private RelativeLayout rlSearch;
    private ImageView ivRlSearch;
    private ImageView ivCross;
    private EditText et_search;
    private ImageView ivBack;
    ArrayList<ContactsModel> contacts_names;
    ProgressDialog progressBar;
    private Context context;
    private ArrayList<String> numberList;
    private int multi_number_positons=-1;
    private UtilClass utilClass;
    private RelativeLayout rlPhonebookRoot;
    private ArrayList<Parcelable> filteredList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.phonebookmain);
        setUpToolbar();
        init();

        this.context = this;
        mListView= (PinnedHeaderListView) findViewById(android.R.id.list);
        rlPhonebookRoot = (RelativeLayout) findViewById(R.id.rl_phonebook_root);

        Log.d(TAG, "inside phoneBookContactsActivity ... about to create progressBar");
        progressBar = new ProgressDialog(PhoneBookContactsActivity.this);
        progressBar.setMessage("Searching...");
        progressBar.setCancelable(false);
        progressBar.show();
        Log.d(TAG, "inside phoneBookContactsActivity ... progressBar created");

        Bundle b= getIntent().getExtras();
        filteredList = b.getParcelableArrayList(Constant.filteredList);

        GetContactListAsync contactListGetter = new GetContactListAsync(this.context, this);

        if (PermissionUtils.contactReadPermissionGranted(this.context)) {
            contactListGetter.execute(new Void[0]);
        } else {
            throw new UnsupportedOperationException("Error! Phone book activity called without permission to read contacts");
        }

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                multi_number_positons = position;
                ContactsModel contact_position = contacts_names.get(position);

                if (contact_position.numbers.size() > 1) {//show dialog
                    Intent i = new Intent(PhoneBookContactsActivity.this, DialogActivity.class);
                    i.putStringArrayListExtra("numberList", (ArrayList<String>) contact_position.numbers);
                    i.putExtra("selectedNumber", contact_position.selectedNumber);
                    startActivityForResult(i, 1);
                } else {
                    if (contact_position.isSelected) {//already selected
                        contact_position.isSelected = false;
                        contact_position.selectedNumber = "";
                        mAdapter.notifyDataSetChanged();
                    } else {
                        contact_position.isSelected = true;
                        try {
                            contact_position.selectedNumber = contact_position.numbers.get(0);
                        } catch (Exception e) {
                            e.printStackTrace();
                            utilClass.showsnackBar(rlPhonebookRoot,PhoneBookContactsActivity.this,getString(R.string.empty_contact));
                        }
                        mAdapter.notifyDataSetChanged();
                    }
                    Log.e(TAG, "selectedNumber is " + contact_position.selectedNumber);
                }
            }
        });
    }

    private void init() {
        utilClass = new UtilClass();
        filteredList = new ArrayList<>();
        contacts_names=new ArrayList<>();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode==1) {
            // Log.e(TAG,"Add Button");
            String selectednumber = data.getStringExtra("selectednumber");
            ContactsModel contact_position = contacts_names.get(multi_number_positons);
            //not selected
            contact_position.isSelected = true;
            contact_position.selectedNumber=selectednumber;
            mAdapter.notifyDataSetChanged();
            // Log.e(TAG, "onActivityResult selectedNumber is " + selectednumber);
            // Log.e(TAG, "onActivityResult model selectedNumber is " + contact_position.selectedNumber);
        } else if (resultCode==2) {
            //Cancel Button
            //Log.e(TAG,"Cancel");
            ContactsModel contact_position = contacts_names.get(multi_number_positons);
            contact_position.isSelected = false;
            contact_position.selectedNumber="";
            mAdapter.notifyDataSetChanged();
            multi_number_positons=1;
        }
    }

    private void setUpToolbar() {

        ivSearch = (ImageView) findViewById(R.id.iv_search);

        rlSimple = (RelativeLayout) findViewById(R.id.rl_simple);
        ivSearch = (ImageView) findViewById(R.id.iv_search);
        ivBack = (ImageView) findViewById(R.id.iv_back);

        rlSearch = (RelativeLayout) findViewById(R.id.rl_search);
        ivRlSearch = (ImageView) findViewById(R.id.iv_rl_search);
        ivCross = (ImageView) findViewById(R.id.iv_cross);
        et_search = (EditText) findViewById(R.id.et_search);

        ivSearch.setOnClickListener(ivSearch());
        ivRlSearch.setOnClickListener(ivRlSearch());
        ivCross.setOnClickListener(ivCross());
        ivBack.setOnClickListener(ivBack());

        et_search.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
              if (s.length() > 0) {
                Filter(et_search.getText().toString());
              } else {
                Filter("");
              }
            }
        });
    }

    private void Filter(String s) {

        String search_string= s.toLowerCase(Locale.getDefault());
        Log.d(TAG, "search_string is " + search_string);

        if (mAdapter.getCount() > 0) {
            Log.e(TAG, "globalSearchAdapter NOT NULL");
        } else if (mAdapter.getCount() < 0) {
            Log.e(TAG, "globalSearchAdapter NULL");
        }

        mAdapter.filter(search_string);
    }

    private View.OnClickListener ivBack() {
        return new View.OnClickListener() {
      @Override
      public void onClick(View v) {
            Intent i=new Intent();
            i.putParcelableArrayListExtra(Constant.phoneBookList,  contacts_names);
            setResult(1,i);
            finish();
      }
    };
  }

    private View.OnClickListener ivCross() {
        return new View.OnClickListener() {
      @Override
      public void onClick(View v) {
            if (et_search.getText().toString().isEmpty()) {
                rlSearch.setVisibility(View.GONE);
                rlSimple.setVisibility(View.VISIBLE);
                try {
                    InputMethodManager imm= (InputMethodManager) getApplicationContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(),0);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                et_search.setText("");
            }
      }
    };
  }

    private View.OnClickListener ivRlSearch() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            //perform search
            }
        };
    }

    private View.OnClickListener ivSearch() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                rlSimple.setVisibility(View.GONE);
                rlSearch.setVisibility(View.VISIBLE);
            }
        };
    }

    @Override
    protected void onDestroy()
    {
    super.onDestroy();
    }


    public void getContactList(List<ContactsModel> list) {

        Log.d(TAG, "listPhones size is " + list.size());

        if (filteredList.size()!=0 && filteredList !=null) {
            for (int i = 0; i < filteredList.size(); i++) {
                //keep comparing until filteredList siz only
                ContactsModel filterdModel = (ContactsModel) filteredList.get(i);
                Log.e(TAG,"filterdModel " + i + " is " + filterdModel.name);

                for (int j = 0; j < list.size(); j++) {
                    ContactsModel listModel = (ContactsModel) list.get(j);
                    if (filterdModel.contact_ID.equals(listModel.contact_ID)) {
                        Log.d(TAG,"compare " + i + " is " + filterdModel.name);
                        Log.d(TAG,"compare " + j + " is " + listModel.name);
                        listModel.isSelected = true;
                        listModel.selectedNumber=filterdModel.selectedNumber;
                    } else {
                        Log.e(TAG,"else compare " + i + " is " + filterdModel.name);
                        Log.e(TAG,"else compare " + j + " is " + listModel.name);
                    }
                }
            }
            contacts_names.addAll(list);

        } else {
            //dont do nothing
            contacts_names.addAll(list);
        }

        mAdapter=new ContactsAdapter(contacts_names,getApplicationContext());
        mListView.setAdapter(mAdapter);
        mListView.setOnScrollListener(mAdapter);
        mListView.setEnableHeaderTransparencyChanges(false);
        progressBar.cancel();

    }

}




