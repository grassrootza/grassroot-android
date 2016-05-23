package com.techmorphosis.grassroot.ui.activities;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.techmorphosis.grassroot.ContactLib.PinnedHeaderListView;
import com.techmorphosis.grassroot.Interface.GetContactList;
import com.techmorphosis.grassroot.R;
import com.techmorphosis.grassroot.adapters.ContactsAdapter;
import com.techmorphosis.grassroot.adapters.GetContactListAsync;
import com.techmorphosis.grassroot.models.ContactsModel;
import com.techmorphosis.grassroot.utils.Constant;
import com.techmorphosis.grassroot.utils.ProgressBarCircularIndeterminate;
import com.techmorphosis.grassroot.utils.UtilClass;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


public class PhoneBookContacts extends PortraitActivity implements GetContactList
{

  public static final String TAG = PhoneBookContacts.class.getSimpleName();
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
  private ArrayList<Parcelable> filterdList;
  private ProgressBarCircularIndeterminate prgPb;
  private TextView txtPrgPb;

  private View errorLayout;
  private LinearLayout llNoResult;
  private LinearLayout llServerError;
  private Snackbar snackBar;

  @Override
  protected void onCreate(Bundle savedInstanceState)
    {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.phonebookmain);
      findAllViews();
      init();
      this.context = this;

      showLoader();

      Bundle b= getIntent().getExtras();
      filterdList =b.getParcelableArrayList(Constant.filterdList);

      new GetContactListAsync(this.context, this).execute(new Void[0]);



      mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
          multi_number_positons = position;

          ContactsModel contact_position = contacts_names.get(position);

          if (contact_position.numbers.size() > 1) {//show dialog

            Intent i = new Intent(PhoneBookContacts.this, DilogActivity.class);
            i.putStringArrayListExtra("numberList", (ArrayList<String>) contact_position.numbers);
            i.putExtra("selectedNumber", contact_position.selectedNumber);
            startActivityForResult(i, 1);

          } else {
            if (contact_position.isSelected) {//already selected
              contact_position.isSelected = false;
              contact_position.selectedNumber = "";
              mAdapter.notifyDataSetChanged();
            } else {
              //not selected
              if (NumberValidation(contact_position.numbers.get(0).trim().replaceAll("[^+0-9]", ""))) {
                Log.e(TAG,"true");

                try {
                  contact_position.selectedNumber = contact_position.numbers.get(0);
                } catch (Exception e) {
                  e.printStackTrace();
                  utilClass.showsnackBar(rlPhonebookRoot,PhoneBookContacts.this,getString(R.string.empty_contact));

                }
                contact_position.isSelected = true;

                mAdapter.notifyDataSetChanged();

              } else {
                Log.e(TAG,"false");

              }


            }
           // Log.e(TAG, "selectedNumber is " + contact_position.selectedNumber);

          }


        }
      });

    }



  private void showLoader() {

    ivSearch.setEnabled(false);
    mListView.setVisibility(View.GONE);
    errorLayout.setVisibility(View.GONE);
    llNoResult.setVisibility(View.GONE);

    prgPb.setVisibility(View.VISIBLE);
    txtPrgPb.setVisibility(View.VISIBLE);
  }

  private void init() {

     utilClass = new UtilClass();
    filterdList= new ArrayList<>();
    contacts_names=new ArrayList<>();

  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (resultCode==1)//add Button
    {
     // Log.e(TAG,"Add Button");

      String selectednumber = data.getStringExtra("selectednumber");

      if (NumberValidation(selectednumber)) {
        Log.e(TAG,"true");
        ContactsModel contact_position = contacts_names.get(multi_number_positons);
        //not selected
        contact_position.isSelected = true;
        contact_position.selectedNumber=selectednumber;
        mAdapter.notifyDataSetChanged();
      } else {
        Log.e(TAG,"false");
      }


     // Log.e(TAG, "onActivityResult selectedNumber is " + selectednumber);

  // Log.e(TAG, "onActivityResult model selectedNumber is " + contact_position.selectedNumber);


    }
    else if (resultCode==2)
    {//Cancel Button

      //Log.e(TAG,"Cancel");
      ContactsModel contact_position = contacts_names.get(multi_number_positons);

        contact_position.isSelected = false;
        contact_position.selectedNumber="";
        mAdapter.notifyDataSetChanged();

        multi_number_positons=1;


    }
  }

  private void findAllViews()
    {

      ivSearch = (ImageView) findViewById(R.id.iv_search);


      rlSimple = (RelativeLayout) findViewById(R.id.rl_simple);
      ivSearch = (ImageView) findViewById(R.id.iv_search);
      ivBack = (ImageView) findViewById(R.id.iv_back);

      rlSearch = (RelativeLayout) findViewById(R.id.rl_search);
      ivRlSearch = (ImageView) findViewById(R.id.iv_rl_search);
      ivCross = (ImageView) findViewById(R.id.iv_cross);
      et_search = (EditText) findViewById(R.id.et_search);

      mListView=(PinnedHeaderListView)findViewById(android.R.id.list);
      rlPhonebookRoot = (RelativeLayout) findViewById(R.id.rl_phonebook_root);

      prgPb = (ProgressBarCircularIndeterminate) findViewById(R.id.prg_pb);
      txtPrgPb = (TextView) findViewById(R.id.txt_prg_pb);

      errorLayout = findViewById(R.id.error_layout);

      llNoResult = (LinearLayout) errorLayout.findViewById(R.id.ll_no_result);
      llServerError = (LinearLayout) errorLayout.findViewById(R.id.ll_server_error);



      setAllListner();



    }

  private void setAllListner() {

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
    Log.e(TAG, "search_string is " + search_string);


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
        i.putParcelableArrayListExtra(Constant.PhoneBook_list,  contacts_names);
        setResult(1,i);
        finish();
      }
    };
  }

  private View.OnClickListener ivCross() {
    return new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        if (et_search.getText().toString().isEmpty())
        {
          rlSearch.setVisibility(View.GONE);
          rlSimple.setVisibility(View.VISIBLE);

          try {
            InputMethodManager imm= (InputMethodManager) getApplicationContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(),0);
          } catch (Exception e) {
            e.printStackTrace();
          }
        }
        else
        {
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
      public void onClick(View v)
      {
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


  public void getContactList(List<ContactsModel> list)
  {

    //Log.e(TAG, "listPhones size is " + list.size());

      if (filterdList.size()!=0 && filterdList!=null)
      {
        //update model


        for (int i = 0; i < filterdList.size(); i++)
        {
          //keep comparing until filterdList siz only

          ContactsModel filterdModel = (ContactsModel) filterdList.get(i);

          //Log.e(TAG,"filterdModel " + i + " is " + filterdModel.name);

          for (int j = 0; j < list.size(); j++)
          {

            ContactsModel listModel = (ContactsModel) list.get(j);

            if (filterdModel.contact_ID.equals(listModel.contact_ID))
            {
             /// Log.e(TAG,"compare " + i + " is " + filterdModel.name);
             // Log.e(TAG,"compare " + j + " is " + listModel.name);

              listModel.isSelected = true;
              listModel.selectedNumber=filterdModel.selectedNumber;
            }
            else
            {
             // Log.e(TAG,"else compare " + i + " is " + filterdModel.name);
             // Log.e(TAG,"else compare " + j + " is " + listModel.name);
            }

          }


        }

        contacts_names.addAll(list);
        ivSearch.setEnabled(true);

      }
    else if (filterdList.size()==0 || filterdList==null)
      {
        Log.e(TAG, "filterdList.size()==0 || filterdList==null");


        if (list.size() == 0) {

          Log.e(TAG, "list.size() == 0 ");
          errorLayout.setVisibility(View.VISIBLE);
          llNoResult.setVisibility(View.VISIBLE);

        } else {
          //just add All data
          Log.e(TAG, "else == 0 ");
          contacts_names.addAll(list);
          ivSearch.setEnabled(true);
        }

      }


    mAdapter=new ContactsAdapter(contacts_names,getApplicationContext());
    mListView.setAdapter(mAdapter);
    mListView.setOnScrollListener(mAdapter);
    mListView.setEnableHeaderTransparencyChanges(false);
    hideLoader();

  }

  private void hideLoader() {

    prgPb.setVisibility(View.GONE);
    txtPrgPb.setVisibility(View.GONE);
    mListView.setVisibility(View.VISIBLE);
  }

  private boolean NumberValidation(String number) {

    Log.e(TAG,"number is " + number);
    if (number.length() == 10) {

      int start=0;
      int end=1;
      String target = "0";

      if (validsubstring(number, target, start, end) && validcharAt(1, number)) {//2nd digit
        return  true;
      }
      else {
        showSnackBar(getApplicationContext(), "", getResources().getString(R.string.Cellphone_number_invalid), "", 0, Snackbar.LENGTH_SHORT);

      }


    }
    else if (number.length() == 12) {

      int start=0;
      int end=3;
      String target = "+27";


      if (validsubstring(number,target,start,end) && validcharAt(3, number) ) {//fourth digit should be 6, 7 or 8
        return  true;
      }
      else {
        showSnackBar(getApplicationContext(), "", getResources().getString(R.string.Cellphone_number_invalid), "", 0, Snackbar.LENGTH_SHORT);
      }

    }
    else if (number.length() == 13) {

      int start=0;
      int end=4;
      String target = "0027";

      if (validsubstring(number,target,start,end) && validcharAt(4, number) ) {//fifth digit should be 6, 7, or 8
        return  true;
      }
      else {
        showSnackBar(getApplicationContext(), "", getResources().getString(R.string.Cellphone_number_invalid), "", 0, Snackbar.LENGTH_SHORT);
      }

    }
    else {
      showSnackBar(getApplicationContext(), "", getResources().getString(R.string.Cellphone_number_invalid), "", 0, Snackbar.LENGTH_SHORT);
    }
    return false;
  }

  private boolean validsubstring(String source,String target, int start, int end) {


    if (source.substring(start, end).equals(target)) {

      return true;
    }

    return false;
  }

  private boolean validcharAt(int index,String value) {


    int compareint=Integer.parseInt(String.valueOf(value.charAt(index)));

    if (compareint == 6 || compareint == 7 || compareint == 8) {//6 || 7 || 8
      return true;
    }
    return false;
  }

  public void showSnackBar(Context context, final String type, String message, String textLabel, int color, int length) {

    snackBar = Snackbar.make(rlPhonebookRoot, message, length);
    View view = snackBar.getView();


    snackBar.show();
  }

}




