package com.techmorphosis.grassroot.adapters;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.os.AsyncTask;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.Contacts;
import android.util.Log;

import com.techmorphosis.grassroot.Interface.ContactListRequester;
import com.techmorphosis.grassroot.models.SingleContact;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GetContactListAsync extends AsyncTask<Void, Void, List<SingleContact>> {

    public String TAG = GetContactListAsync.class.getSimpleName();

    private ContentResolver contentResolver;
    private Context context;

    private ContactListRequester requester;
    private List<SingleContact> contactsRetrieved;

    public GetContactListAsync(Context context, ContactListRequester requestor) {
        this.context = context;
        this.requester = requestor;
        this.contactsRetrieved = new ArrayList();
    }

    @Override
    protected List<SingleContact> doInBackground(Void... params) {

        this.contentResolver = context.getContentResolver();
        Cursor cur = contentResolver.query(Contacts.CONTENT_URI, null, null, null, "display_name ASC");
        Log.e(TAG, "inside retrieveContacts, just got cursor with : " + cur.getCount() + " elements");

        if (cur.getCount() > 0) {

            String currentId;
            int idIndex = cur.getColumnIndex(Contacts._ID);

            while (cur.moveToNext()) {

                currentId = cur.getString(idIndex);
                Cursor phoneCursor = this.contentResolver.query(Phone.CONTENT_URI, null,
                        ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?", new String[]{currentId}, null);
                int phoneIndex = phoneCursor.getColumnIndex(Phone.NUMBER);

                if (phoneCursor.moveToNext()) {

                    SingleContact contact = new SingleContact();
                    contact.name = cur.getString(cur.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
                    contact.contact_ID = currentId;

                    String phoneNo = phoneCursor.getString(phoneIndex);
                    List<String> listPhones = Collections.singletonList(phoneNo);

                    if (phoneCursor.getCount() > 1) {
                        while (phoneCursor.moveToNext()) {
                            phoneNo = phoneCursor.getString(phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                            listPhones.add(phoneNo);
                        }
                        contact.selectedNumber = "";
                    } else {
                        contact.selectedNumber = phoneNo;
                    }

                    contact.numbers = listPhones;
                    contactsRetrieved.add(contact);
                }

                phoneCursor.close();
            }
        }
        cur.close();
        return contactsRetrieved;
    }

    protected void onPostExecute(List<SingleContact> contactsModels) {
        Log.d(TAG, "in async Contact List, calling onPostExecute, with list = " + contactsModels.toString());
        requester.putContactList(contactsModels);
    }
}