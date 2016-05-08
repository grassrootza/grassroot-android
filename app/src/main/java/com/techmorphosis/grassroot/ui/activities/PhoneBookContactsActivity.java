package com.techmorphosis.grassroot.ui.activities;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
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
import com.techmorphosis.grassroot.Interface.ContactListRequester;
import com.techmorphosis.grassroot.R;
import com.techmorphosis.grassroot.adapters.ContactsAdapter;
import com.techmorphosis.grassroot.adapters.GetContactListAsync;
import com.techmorphosis.grassroot.models.SingleContact;
import com.techmorphosis.grassroot.services.model.Member;
import com.techmorphosis.grassroot.utils.Constant;
import com.techmorphosis.grassroot.utils.PermissionUtils;
import com.techmorphosis.grassroot.utils.UtilClass;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnItemClick;
import butterknife.OnTextChanged;


public class PhoneBookContactsActivity extends PortraitActivity implements ContactListRequester {

    public static final String TAG = PhoneBookContactsActivity.class.getSimpleName();

    private ContactsAdapter mAdapter;
    ArrayList<SingleContact> contacts_names;
    private ArrayList<Parcelable> preSelectedList;
    private int multi_number_positons=-1;
    private UtilClass utilClass;

    @BindView(android.R.id.list) // android.R?
    PinnedHeaderListView mListView;

    @BindView(R.id.rl_phonebook_root)
    RelativeLayout rlPhonebookRoot;

    @BindView(R.id.rl_simple)
    RelativeLayout rlSimple;

    @BindView(R.id.rl_search)
    RelativeLayout rlSearch;
    @BindView(R.id.iv_search)
    ImageView ivSearch;
    @BindView(R.id.et_search)
    EditText et_search;

    @BindView(R.id.iv_cross)
    ImageView ivCross;
    @BindView(R.id.iv_back)
    ImageView ivBack;

    ProgressDialog progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.phonebookmain);
        ButterKnife.bind(this);
        setUpProgressBar();
        init();

        Bundle b= getIntent().getExtras();
        preSelectedList = b.getParcelableArrayList(Constant.filteredList);

        GetContactListAsync contactListGetter = new GetContactListAsync(this, this);

        if (PermissionUtils.contactReadPermissionGranted(this)) {
            contactListGetter.execute(new Void[0]);
        } else {
            Log.e(TAG, "Error! Phone book activity called without permission to read contacts");
            finish();
        }
    }

    private void init() {
        utilClass = new UtilClass();
        preSelectedList = new ArrayList<>();
        contacts_names=new ArrayList<>();
    }

    public void putContactList(List<SingleContact> returnedContactList) {

        Log.d(TAG, "listPhones size is " + returnedContactList.size());

        if (preSelectedList != null && preSelectedList.size() > 0) {

            for (int i = 0; i < preSelectedList.size(); i++) {

                SingleContact filteredModel = (SingleContact) preSelectedList.get(i);

                for (int j = 0; j < returnedContactList.size(); j++) {
                    SingleContact listModel = returnedContactList.get(j);
                    if (filteredModel.contact_ID.equals(listModel.contact_ID)) {
                        listModel.isSelected = true;
                        listModel.selectedNumber = filteredModel.selectedNumber;
                    }
                }
            }
        }

        contacts_names.addAll(returnedContactList);
        mAdapter=new ContactsAdapter(contacts_names,getApplicationContext());
        mListView.setAdapter(mAdapter);
        mListView.setOnScrollListener(mAdapter);
        mListView.setEnableHeaderTransparencyChanges(false);
        progressBar.cancel();

    }

    /**
     * SECTION: Handle returning to prior page, including sending the results
     */
    @OnClick(R.id.iv_back)
    public void ivBack() {
        Intent i = new Intent();
        i.putParcelableArrayListExtra(Constant.selectedContacts, membersToReturn());
        setResult(RESULT_OK,i);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private ArrayList<SingleContact> membersToReturn() {
        ArrayList<SingleContact> selectedMembers = new ArrayList<>();
        // oh for Java 8 ... todo: consider holding a sep list so don't have to do this iteration on close
        for (SingleContact contact : contacts_names) {
            if (contact.isSelected) {
                selectedMembers.add(contact);
            }
        }
        return selectedMembers;
    }

    /**
     * SECTION : handle clicking on member, including asking to pick one number if multiple
     */

    @OnItemClick(android.R.id.list)
    public void selectMember(int position) {

        multi_number_positons = position;
        SingleContact contactClicked = contacts_names.get(position);

        if (contactClicked.numbers.size() > 1) {//show dialog
            Intent i = new Intent(PhoneBookContactsActivity.this, DialogActivity.class);
            i.putStringArrayListExtra("numberList", (ArrayList<String>) contactClicked.numbers);
            i.putExtra("selectedNumber", contactClicked.selectedNumber);
            startActivityForResult(i, Constant.activitySelectNumberFromContact);
        } else {
            if (contactClicked.isSelected) {
                contactClicked.isSelected = false;
                contactClicked.selectedNumber = "";
            } else {
                contactClicked.isSelected = true;
                try {
                    contactClicked.selectedNumber = contactClicked.numbers.get(0);
                } catch (Exception e) {
                    e.printStackTrace();
                    utilClass.showsnackBar(rlPhonebookRoot,PhoneBookContactsActivity.this,getString(R.string.empty_contact));
                }
            }
            mAdapter.notifyDataSetChanged();
            Log.e(TAG, "selectedNumber is " + contactClicked.selectedNumber);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == 1) { // todo: replace these numbers with meaningful named constants
            String selectednumber = data.getStringExtra("selectednumber");
            SingleContact contactAtPosition = contacts_names.get(multi_number_positons);
            contactAtPosition.isSelected = true;
            contactAtPosition.selectedNumber = selectednumber;
            mAdapter.notifyDataSetChanged();
            Log.e(TAG, "onActivityResult selectedNumber is " + selectednumber);
        } else if (resultCode == 2) {
            SingleContact contactAtPosition = contacts_names.get(multi_number_positons);
            contactAtPosition.isSelected = false;
            contactAtPosition.selectedNumber="";
            mAdapter.notifyDataSetChanged();
            multi_number_positons=1;
        }
    }

    /**
     * SECTION : search bar / handler
     */
    // todo: filter as someone types, if device can handle it
    @OnTextChanged(value = R.id.et_search, callback = OnTextChanged.Callback.AFTER_TEXT_CHANGED)
    public void onSearchTextChanged(CharSequence s) {
        Filter (s.length() > 0 ? et_search.getText().toString() : "");
    }

    private void Filter(String s) {
        String search_string= s.toLowerCase(Locale.getDefault());
        Log.d(TAG, "search_string is " + search_string);
        mAdapter.filter(search_string);
    }

    @OnClick(R.id.iv_cross)
    public void onClickCross() {
        if (et_search.getText().toString().isEmpty()) {
            rlSearch.setVisibility(View.GONE);
            rlSimple.setVisibility(View.VISIBLE);
            try {
                InputMethodManager imm = (InputMethodManager) getApplicationContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            et_search.setText("");
        }
    }

    @OnClick(R.id.iv_rl_search)
    public void ivRlSearch() {
        //perform search
    }

    @OnClick(R.id.iv_search)
    public void onClickSearch() {
        rlSimple.setVisibility(View.GONE);
        rlSearch.setVisibility(View.VISIBLE);
    }

    private void setUpProgressBar() {
        progressBar = new ProgressDialog(PhoneBookContactsActivity.this);
        progressBar.setMessage("Searching...");
        progressBar.setCancelable(false);
        progressBar.show();
        Log.d(TAG, "inside phoneBookContactsActivity ... progressBar created");
    }

}




