package org.grassroot.android.fragments;

import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.ContactsContract;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import org.grassroot.android.R;
import org.grassroot.android.adapters.ContactsAdapter2;
import org.grassroot.android.models.Contact;
import org.grassroot.android.utils.Constant;
import org.grassroot.android.utils.UtilClass;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnItemClick;

/**
 * Created by luke on 2016/06/07.
 */
public class ContactSelectionFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<Cursor>, PickNumberDialogFragment.PickNumberListener {

    public interface ContactSelectionListener {
        void onContactSelectionComplete(List<Contact> contactsAdded, Set<Contact> contactsRemoved);
    }

    private static final String TAG = ContactSelectionFragment.class.getCanonicalName();

    private ContactsAdapter2 adapter;
    private ContactSelectionListener listener;

    private List<Contact> retrievedContacts;

    private Map<String, Contact> contactFilterMap;

    private Set<Contact> contactsToPreselect;
    private Set<Contact> contactsRemoved;

    @BindView(R.id.cs_list_view)
    ListView contactListView;

    private ProgressDialog progressDialog;

    public ContactSelectionFragment() { }

    public void setContactsToPreselect(Set<Contact> contactsToPreselect) {
        this.contactsToPreselect = contactsToPreselect;
    }

    public void setContactsToFilter(Set<Contact> contactsToFilter) {
        contactFilterMap = new HashMap<>();
        for (Contact c : contactsToFilter) {
            final List<String> nums = c.numbers;
            if (!nums.isEmpty()) {
                for (String number : nums) {
                    contactFilterMap.put(UtilClass.formatNumberToE164(number), c);
                }
            }
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            listener = (ContactSelectionListener) context;
        } catch (ClassCastException e) {
            throw new UnsupportedOperationException("Error! Activity must implement listener");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (contactsToPreselect == null)
            contactsToPreselect = new HashSet<>();
        if (contactsRemoved == null)
            contactsRemoved = new HashSet<>();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View viewToReturn = inflater.inflate(R.layout.fragment_contact_selection, container, false);
        ButterKnife.bind(this, viewToReturn);
        adapter = new ContactsAdapter2(this.getContext(), R.id.tv_person_name);
        contactListView.setAdapter(adapter);
        return viewToReturn;
    }

    // keep an eye out on this only being called when fragment is added to stack, not too early
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getLoaderManager().initLoader(Constant.loaderContacts, null, this);
        progressDialog = new ProgressDialog(getContext());
        progressDialog.setTitle(R.string.cs_loading_contacts);
        progressDialog.show();
    }

    @OnClick(R.id.cs_bt_save)
    public void saveAndFinish() {
        List<Contact> addedMembers = new ArrayList<>(adapter.getSelectedContacts());
        addedMembers.removeAll(contactsToPreselect);
        listener.onContactSelectionComplete(addedMembers, contactsRemoved);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {

        final String SORT_ORDER= Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB ?
                ContactsContract.Contacts.SORT_KEY_PRIMARY: ContactsContract.Contacts.DISPLAY_NAME;
        final String CONTACT_NAME = Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB ?
                ContactsContract.Contacts.DISPLAY_NAME_PRIMARY: ContactsContract.Contacts.DISPLAY_NAME;

        final String[] projection = {
                ContactsContract.Contacts._ID,
                ContactsContract.Contacts.LOOKUP_KEY,
                CONTACT_NAME,
                SORT_ORDER
        };

        final String select = "((" + ContactsContract.Contacts.DISPLAY_NAME + " NOTNULL) AND ("
                + ContactsContract.Contacts.HAS_PHONE_NUMBER + "=1) AND ("
                + ContactsContract.Contacts.DISPLAY_NAME + " != '' ))";

        final String search = ContactsContract.Contacts.DISPLAY_NAME + " COLLATE LOCALIZED ASC";

        CursorLoader cursorLoader = new CursorLoader(
                getContext(),
                ContactsContract.Contacts.CONTENT_URI,
                projection,
                select,
                null,
                search);

        return cursorLoader;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        assembleContactList(data);
        adapter.setContactsToDisplay(retrievedContacts);
        progressDialog.hide();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // todo : do we need to do anything?
        retrievedContacts = new ArrayList<>();
        adapter.setContactsToDisplay(retrievedContacts);
    }

    private void assembleContactList(Cursor contactHolder) {
        Long startTime = SystemClock.currentThreadTimeMillis();
        if (contactHolder == null || contactHolder.isClosed()) {
            throw new UnsupportedOperationException("Error! Null or closed cursor handed to contact list assembler");
        }

        if (retrievedContacts == null) {
            retrievedContacts = new ArrayList<>();
        } else {
            retrievedContacts.clear();
        }

        Log.d(TAG, "inside assembleContacts, just got cursor with : " + contactHolder.getCount() + " elements");

        final boolean filteringActive = contactFilterMap != null && !contactFilterMap.isEmpty();

        if (contactHolder.getCount() > 0) {

            final ContentResolver resolver = getContext().getContentResolver();
            final int idIndex = contactHolder.getColumnIndexOrThrow(ContactsContract.Contacts.LOOKUP_KEY);
            final int nameIndex = contactHolder.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY);
            final String[] projection = { ContactsContract.CommonDataKinds.Phone.NUMBER };

            while (contactHolder.moveToNext()) {

                final String[] lookupKey = { contactHolder.getString(idIndex) };
                final Cursor thisPhoneList = resolver.query(
                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        projection,
                        ContactsContract.CommonDataKinds.Phone.LOOKUP_KEY + " = ?",
                        lookupKey,
                        null);

                if (thisPhoneList != null) {
                    if (thisPhoneList.getCount() > 0) {
                        final Contact contactToAdd = constructContact(contactHolder, nameIndex, lookupKey[0], thisPhoneList,
                                filteringActive);
                        if (contactToAdd != null) {
                            retrievedContacts.add(contactToAdd);
                        }
                    }
                    thisPhoneList.close(); // cursor is not managed by a loader, so make sure to close
                }
            }

            contactHolder.moveToPosition(-1); // reset, in case cursor is reused via stack management
        }

        Log.d(TAG, String.format("Processed %d contacts, resulting in final list of %d entities, in %d msecs",
                contactHolder.getCount(), retrievedContacts.size(), SystemClock.currentThreadTimeMillis() - startTime));

    }

    private Contact constructContact(final Cursor contactHolder, final int nameIndex,
                                     final String lookupKey, final Cursor phoneList, boolean filteringActive) {

        Contact contact = null;
        final int phoneIndex = phoneList.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);

        // todo: shift the local number checking into a regex passed to the cursor
        if (phoneList.getCount() == 1) {
            phoneList.moveToFirst();
            final String phone = phoneList.getString(phoneIndex);
            if (UtilClass.checkIfLocalNumber(phone) && !isNumberInFilterMap(phone)) {
                contact = new Contact(lookupKey, contactHolder.getString(nameIndex));
                contact.selectedNumber = phone;
                contact.numbers = Collections.singletonList(phone);
            }
        } else {
            while (phoneList.moveToNext()) {
                final String phone = phoneList.getString(phoneIndex);
                if (UtilClass.checkIfLocalNumber(phone)) {
                    if (isNumberInFilterMap(phone)) {
                        return null;
                    } else if (contact == null) {
                        contact = new Contact(lookupKey, contactHolder.getString(nameIndex));
                        contact.numbers = new ArrayList<>();
                    }
                    contact.numbers.add(phone);
                }
            }
        }

        if (contact != null && contactsToPreselect.contains(contact))
            contact.isSelected = true;

        return contact;
    }

    private boolean isNumberInFilterMap(String phone) {
        if (contactFilterMap == null) return false;
        if (contactFilterMap.get(UtilClass.formatNumberToE164(phone)) == null) return false;
        return true;
    }

    /**
     * SECTION : handle clicking on member, including asking to pick one number if multiple
     * Note : we store view and position as seems more efficient than passing around to dialogs etc
     * which should only be concerned with the contact, but can revisit
     */

    private View temporaryViewHolder;

    @OnItemClick(R.id.cs_list_view)
    public void selectMember(View view, int position) {
        final Contact contact = adapter.getItem(position);
        if (contact.isSelected) {
            adapter.toggleSelected(position, view);
            handlePreselected(contact);
        } else {
            if (contact.numbers.size() <= 1) {
                adapter.toggleSelected(position, view);
                handlePreselected(contact);
            } else {
                temporaryViewHolder = view;
                pickNumberDialog(contact, position);
            }
        }
    }

    private void pickNumberDialog(Contact contact, int position) {
        PickNumberDialogFragment dialog = new PickNumberDialogFragment();
        dialog.setUp(contact, position, this);
        dialog.show(getFragmentManager(), "PickNumberDialog");
    }

    @Override
    public void onNumberPicked(final int contactPosition, final CharSequence number) {
        adapter.setSelected(contactPosition, temporaryViewHolder, number);
        handlePreselected(adapter.getItem(contactPosition));
        temporaryViewHolder = null;
    }

    // nb : only ever call this _after_ toggling select (it presumes current state of isSelected is 'final')
    private void handlePreselected(final Contact contact) {
        if (!contactsToPreselect.isEmpty() && contactsToPreselect.contains(contact)) {
            if (contact.isSelected) {
                contactsRemoved.remove(contact);
            } else {
                contactsRemoved.add(contact);
            }
        }
    }

}