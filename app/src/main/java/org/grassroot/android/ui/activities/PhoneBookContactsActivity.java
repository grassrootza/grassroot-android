package org.grassroot.android.ui.activities;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import org.grassroot.android.ContactLib.PinnedHeaderListView;
import org.grassroot.android.interfaces.ContactListRequester;
import org.grassroot.android.R;
import org.grassroot.android.adapters.ContactsAdapter;
import org.grassroot.android.services.GetContactListAsync;
import org.grassroot.android.models.Contact;
import org.grassroot.android.utils.Constant;
import org.grassroot.android.utils.PermissionUtils;
import org.grassroot.android.utils.UtilClass;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnItemClick;
import butterknife.OnTextChanged;


public class PhoneBookContactsActivity extends PortraitActivity implements ContactListRequester {

    public static final String TAG = PhoneBookContactsActivity.class.getSimpleName();

    private ContactsAdapter mAdapter;

    ArrayList<Contact> contactsDisplayed;
    ArrayList<Contact> contactsSelected;
    ArrayList<Contact> contactsRemoved;

    private ArrayList<Contact> preSelectedList; // e.g., from prior launch & return of this
    private ArrayList<Contact> contactsToFilterOut; // e.g., existing group members

    private int multiNumberPosition =-1;
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
        setContentView(R.layout.activity_phone_book);
        ButterKnife.bind(this);
        setUpProgressBar();
        init();

        Bundle b = getIntent().getExtras();
        preSelectedList = b.getParcelableArrayList(Constant.filteredList);
        contactsToFilterOut = b.getParcelableArrayList(Constant.doNotDisplayContacts);

        GetContactListAsync contactListGetter = new GetContactListAsync(this, this);

        if (PermissionUtils.contactReadPermissionGranted(this)) {
            contactListGetter.execute();
        } else {
            Log.e(TAG, "Error! Phone book activity called without permission to read contacts");
            finish();
        }
    }

    private void init() {
        utilClass = new UtilClass();
        preSelectedList = new ArrayList<>();
        contactsDisplayed = new ArrayList<>();
        contactsSelected = new ArrayList<>();
        contactsRemoved = new ArrayList<>();
    }

    public void putContactList(List<Contact> returnedContactList) {

        final boolean filterContacts = contactsToFilterOut != null && !contactsToFilterOut.isEmpty();
        final boolean preSelectContacts = preSelectedList != null && !preSelectedList.isEmpty();

        if (filterContacts) {
            // note: can't rely on hash/equals as filter list won't have contactId (local) and contacts won't have memberUid
            // assembling this map is then quicker than doing a double iteration over the lists, hence ...
            // todo: optimize this quite a bit, most obviously deciding which list is transformed to map and which is iterated here

            final Map<String, Contact> phoneNumberMap = assemblePhoneMap(returnedContactList);
            for (Contact c : contactsToFilterOut) {
                Contact contact = phoneNumberMap.get(UtilClass.formatNumberToE164(c.selectedNumber));
                if (contact != null) {
                    returnedContactList.remove(contact);
                }
            }
        }

        // as above, could do with some iteration, maybe reuse the map
        if (preSelectContacts) {
            for (Contact c : preSelectedList) {
                int index = returnedContactList.indexOf(c);
                if (index != -1) {
                    returnedContactList.get(index).isSelected = true;
                }
            }
        }

        contactsDisplayed.addAll(returnedContactList);
        mAdapter = new ContactsAdapter(contactsDisplayed, getApplicationContext());
        mListView.setAdapter(mAdapter);
        mListView.setOnScrollListener(mAdapter);
        mListView.setEnableHeaderTransparencyChanges(false);
        progressBar.cancel();
    }

    private Map<String, Contact> assemblePhoneMap(List<Contact> contactList) {
        Map<String, Contact> phoneMap = new HashMap<>();
        for (Contact c : contactList) {
            final List<String> nums = c.numbers;
            if (!nums.isEmpty()) {
                for (String number : nums) {
                    phoneMap.put(UtilClass.formatNumberToE164(number), c);
                }
            }
        }
        return phoneMap;
    }

    /**
     * SECTION: Handle returning to prior page, including sending the results
     */
    @OnClick(R.id.iv_back)
    public void ivBack() {
        Intent i = new Intent();
        i.putParcelableArrayListExtra(Constant.contactsAdded, contactsSelected);
        i.putParcelableArrayListExtra(Constant.contactsRemoved, contactsRemoved);
        setResult(RESULT_OK,i);
        finish();
    }

    /**
     * SECTION : handle clicking on member, including asking to pick one number if multiple
     * note : probably want to switch this to using recycler view on item click ... likely more robust
     */

    @OnItemClick(android.R.id.list)
    public void selectMember(int position) {

        multiNumberPosition = position;
        Contact contactClicked = contactsDisplayed.get(position);

        if (contactClicked.numbers.size() > 1) {//show dialog
            Intent i = new Intent(PhoneBookContactsActivity.this, SelectPhoneNumberActivity.class);
            i.putStringArrayListExtra("numberList", (ArrayList<String>) contactClicked.numbers);
            i.putExtra("selectedNumber", contactClicked.selectedNumber);
            startActivityForResult(i, Constant.activitySelectNumberFromContact);
        } else {
            if (contactClicked.isSelected) {
                switchContactToUnselected(contactClicked);
            } else {
                try {
                    switchContactToSelected(contactClicked);
                } catch (Exception e) {
                    utilClass.showsnackBar(rlPhonebookRoot,PhoneBookContactsActivity.this,getString(R.string.empty_contact));
                }
            }
            mAdapter.notifyDataSetChanged();
        }
    }

    private void switchContactToSelected(Contact contactClicked) {
        contactClicked.selectedNumber = contactClicked.numbers.get(0);
        if (contactsRemoved.contains(contactClicked)) {
            contactsRemoved.remove(contactClicked);
        }
        if (!preSelectedList.contains(contactClicked)) {
            contactsSelected.add(contactClicked);
        }
        contactClicked.isSelected = true;
    }

    private void switchContactToUnselected(Contact contactClicked) {
        if (preSelectedList.contains(contactClicked)) {
            contactsRemoved.add(contactClicked);
        }
        if (contactsSelected.contains(contactClicked)) {
            contactsSelected.remove(contactClicked);
        }
        // todo: this means if reclick, can't switch number, so make sure to wire up on long click
        contactClicked.isSelected = false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == 1) { // todo: replace these numbers with meaningful named constants
            String selectednumber = data.getStringExtra("selectednumber");
            Contact contactAtPosition = contactsDisplayed.get(multiNumberPosition);
            contactAtPosition.isSelected = true;
            contactAtPosition.selectedNumber = selectednumber;
            mAdapter.notifyDataSetChanged();
            Log.e(TAG, "onActivityResult selectedNumber is " + selectednumber);
        } else if (resultCode == 2) {
            Contact contactAtPosition = contactsDisplayed.get(multiNumberPosition);
            contactAtPosition.isSelected = false;
            contactAtPosition.selectedNumber="";
            mAdapter.notifyDataSetChanged();
            multiNumberPosition = -1;
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




