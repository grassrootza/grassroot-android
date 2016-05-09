package com.techmorphosis.grassroot.adapters;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.Contacts;
import android.telephony.PhoneNumberUtils;
import android.util.Log;

import com.techmorphosis.grassroot.Interface.ContactListRequester;
import com.techmorphosis.grassroot.models.Contact;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GetContactListAsync extends AsyncTask<Void, Void, List<Contact>> {

    public String TAG = GetContactListAsync.class.getSimpleName();

    private ContentResolver contentResolver;
    private Context context;

    private ContactListRequester requester;
    private List<Contact> contactsRetrieved;

    public GetContactListAsync(Context context, ContactListRequester requestor) {
        this.context = context;
        this.requester = requestor;
        this.contactsRetrieved = new ArrayList();
    }

    @Override
    protected List<Contact> doInBackground(Void... params) {

        this.contentResolver = context.getContentResolver();
        Cursor cur = contentResolver.query(Contacts.CONTENT_URI, null, null, null, "display_name ASC");
        Log.e(TAG, "inside retrieveContacts, just got cursor with : " + cur.getCount() + " elements");

        if (cur.getCount() > 0) {

            String currentId;
            int idIndex = cur.getColumnIndex(Contacts._ID);

            while (cur.moveToNext()) {

                boolean localNumberFound = false;

                currentId = cur.getString(idIndex);
                Cursor phoneCursor = this.contentResolver.query(Phone.CONTENT_URI, null,
                        ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?", new String[]{currentId}, null);
                int phoneIndex = phoneCursor.getColumnIndex(Phone.NUMBER);

                if (phoneCursor.moveToNext()) {

                    Contact contact = new Contact();
                    contact.name = cur.getString(cur.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
                    contact.contact_ID = currentId;

                    List<String> listPhones;
                    String phoneNo = phoneCursor.getString(phoneIndex);

                    if (checkIfLocalNumber(phoneNo)) {
                        listPhones = Collections.singletonList(phoneNo);
                        localNumberFound = true;
                    } else {
                        listPhones = new ArrayList<>();
                    }

                    while (phoneCursor.moveToNext()) {
                        phoneNo = phoneCursor.getString(phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                        if (checkIfLocalNumber(phoneNo)) {
                            listPhones.add(phoneNo);
                            localNumberFound = true;
                        }
                    }

                    if (localNumberFound) {
                        contact.numbers = listPhones;
                        contact.selectedNumber = listPhones.size() == 1 ? listPhones.get(0) : "";
                        contactsRetrieved.add(contact);
                    }
                }

                phoneCursor.close();
            }
        }
        cur.close();
        return contactsRetrieved;
    }

    protected void onPostExecute(List<Contact> contactsModels) {
        Log.d(TAG, "in async Contact List, calling onPostExecute, with list = " + contactsModels.toString());
        requester.putContactList(contactsModels);
    }

    private boolean checkIfLocalNumber(String phoneNumber) {
        // todo: might be able to do this much quicker if use Google overall libPhoneNumber, but whole lib for this is heavy
        final String normalized = PhoneNumberUtils.stripSeparators(phoneNumber);
        Log.d(TAG, "inside contact list, normalized number : " + normalized);
        if (normalized.charAt(0) == '0' && normalized.length() == 10)
            return true;
        if (normalized.substring(0,3).equals("+27") || normalized.substring(0,2).equals("27"))
            return true;
        return false;
    }
}