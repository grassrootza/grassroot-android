package org.grassroot.android.ui.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import org.grassroot.android.R;
import org.grassroot.android.adapters.ContactsAdapter;
import org.grassroot.android.interfaces.ContactListRequester;
import org.grassroot.android.models.Contact;
import org.grassroot.android.services.GetContactListAsync;
import org.grassroot.android.ui.fragments.ContactSelectionFragment;
import org.grassroot.android.utils.Constant;
import org.grassroot.android.utils.PermissionUtils;
import org.grassroot.android.utils.UtilClass;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnItemClick;
import butterknife.OnTextChanged;


public class ContactSelectionActivity extends PortraitActivity implements ContactSelectionFragment.ContactSelectionListener {

    public static final String TAG = ContactSelectionActivity.class.getSimpleName();

    private ContactSelectionFragment contactSelectionFragment;

    @BindView(R.id.cs_fragment_holder)
    FrameLayout flFragmentHolder;

    @BindView(R.id.rl_simple)
    RelativeLayout rlSimple;

    @BindView(R.id.rl_search)
    RelativeLayout rlSearch;
    @BindView(R.id.et_search)
    EditText et_search;

    @BindView(R.id.prg_pb)
    ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_phone_book_2);
        ButterKnife.bind(this);
        // setUpProgressBar();

        Bundle b = getIntent().getExtras();

        if (PermissionUtils.contactReadPermissionGranted(this)) {
            setUpFragment(b);
        } else {
            Log.e(TAG, "Error! Phone book activity called without permission to read contacts");
            finish();
        }
    }

    private void setUpFragment(Bundle b) {
        contactSelectionFragment = new ContactSelectionFragment();
        List<Contact> preSelectedList = b.getParcelableArrayList(Constant.filteredList);
        List<Contact> contactsToFilterOut = b.getParcelableArrayList(Constant.doNotDisplayContacts);
        contactSelectionFragment.setArguments(b);

        getSupportFragmentManager().beginTransaction()
                .add(R.id.cs_fragment_holder, contactSelectionFragment)
                .commit();
    }


    /**
     * SECTION: Handle returning to prior page, including sending the results
     */
    @OnClick(R.id.iv_back)
    public void ivBack() {
        Intent i = new Intent();
        //i.putParcelableArrayListExtra(Constant.contactsAdded, contactsSelected);
        //i.putParcelableArrayListExtra(Constant.contactsRemoved, contactsRemoved);
        setResult(RESULT_OK,i);
        finish();
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
        // mAdapter.getFilter().filter(search_string);
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
        progressBar.setVisibility(View.VISIBLE);
        Log.d(TAG, "inside phoneBookContactsActivity ... progressBar created");
    }

    @Override
    public void onContactSelectionComplete(List<Contact> contactsSelected, Set<Contact> contactsRemoved) {
        Log.e(TAG, "contacts selected! these: " + contactsSelected.toString());
    }
}