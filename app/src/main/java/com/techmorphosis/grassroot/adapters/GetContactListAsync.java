package com.techmorphosis.grassroot.adapters;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.Contacts;

import com.techmorphosis.grassroot.Interface.GetContactList;
import com.techmorphosis.grassroot.models.ContactsModel;

import java.util.ArrayList;
import java.util.List;

public class GetContactListAsync extends AsyncTask<Void, Void, List<ContactsModel>> {
    private ContentResolver contactResolver;
    private Context context;
    private GetContactList getList;
    private String id;
    private List<ContactsModel> listContacts;
    private ArrayList<String> listPhones;
    private String phone;
    private Cursor phoneCursor;
    public String TAG = GetContactListAsync.class.getSimpleName();
    private ContactsModel contactsModel;


    public GetContactListAsync(Context context, GetContactList getList)
    {
        this.context = context;
        this.getList = getList;
        this.listContacts = new ArrayList();
        this.listPhones = new ArrayList();
    }


    @Override
    protected List<ContactsModel> doInBackground(Void... params) {
        this.contactResolver = this.context.getContentResolver();
        Cursor cur = this.context.getContentResolver().query(Contacts.CONTENT_URI, null, null, null, "display_name ASC");
        if (cur.getCount() > 0) {
            while (cur.moveToNext()) {
                this.contactsModel = new ContactsModel();
                id = cur.getString(cur.getColumnIndex(ContactsContract.Contacts._ID));
                this.contactsModel.contact_ID = id;
                this.contactsModel.name =cur.getString(cur.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
                phoneCursor = this.contactResolver.query(Phone.CONTENT_URI, null, ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?", new String[]{this.id}, null);
                while (this.phoneCursor.moveToNext()) {
                    int phoneType = this.phoneCursor.getInt(this.phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE));
                    String phoneNo = this.phoneCursor.getString(this.phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                    switch (phoneType) {
                        case ContactsContract.CommonDataKinds.Phone.TYPE_HOME:
                            //phoneType = "Home";
                            this.phone = phoneNo;
                            break;
                        case ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE:
                            //phoneType = "Mobile";
                            this.phone = phoneNo;
                            break;
                        case ContactsContract.CommonDataKinds.Phone.TYPE_WORK:
                          //  phoneType = "Work";
                            this.phone = phoneNo;
                            break;
                        default:
                            this.phone = phoneNo;
                            break;
                    }
                    this.listPhones.add(phoneNo);
                }
                if (this.listPhones.size() > 0)
                {
                    this.contactsModel.numbers = this.listPhones;
                    this.contactsModel.isSelected=false;
                    this.contactsModel.selectedNumber="";
                    this.listContacts.add(contactsModel);

                }

                this.listPhones = new ArrayList();
                this.phoneCursor.close();
            }
        }
        cur.close();
        return this.listContacts;

    }




    protected void onPostExecute(List<ContactsModel> contactsModels)
    {
        this.getList.getContactList(contactsModels);
    }
}
