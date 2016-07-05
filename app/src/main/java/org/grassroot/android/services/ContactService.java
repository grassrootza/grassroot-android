package org.grassroot.android.services;

import android.content.ContentResolver;
import android.database.Cursor;
import android.os.AsyncTask;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.util.Log;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.grassroot.android.events.ContactsLoadedEvent;
import org.grassroot.android.models.Contact;
import org.grassroot.android.models.RealmString;
import org.grassroot.android.utils.Utilities;
import org.greenrobot.eventbus.EventBus;

/**
 * Created by luke on 2016/06/17.
 */
public class ContactService {

  private static final String TAG = ContactService.class.getSimpleName();

  public List<Contact> displayedContacts = new ArrayList<>();
  private Map<Integer, Contact> phoneBookIDMap = new HashMap<>();
  private Map<String, Contact> phoneBookMSISDNMap = new HashMap<>();

  public boolean contactsLoading = false;
  public boolean contactsFinishedLoading = false;

  private final String[] projectionForPhones = {
      ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
      ContactsContract.CommonDataKinds.Phone.NUMBER, ContactsContract.CommonDataKinds.Phone.TYPE,
      ContactsContract.CommonDataKinds.Phone.LABEL,
  };

  private final String[] projectionForNames = {
      ContactsContract.CommonDataKinds.StructuredName.CONTACT_ID,
      ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME,
      ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME,
      ContactsContract.Data.DISPLAY_NAME
  };

  private final String[] projectionForWhatsApp = {
      "display_name", ContactsContract.RawContacts.SYNC1, ContactsContract.RawContacts.CONTACT_ID
  };

  private static ContactService instance = null;

  protected ContactService() {
  }

  public static ContactService getInstance() {
    ContactService methodInstance = instance;
    if (methodInstance == null) {
      synchronized (ContactService.class) {
        methodInstance = instance;
        if (methodInstance == null) {
          instance = methodInstance = new ContactService();
        }
      }
    }
    return methodInstance;
  }

  public void syncContactList(final List<Contact> contactsToFilter, final boolean filterByID) {
    // todo : write logic to check if done already, but incorporating refresh check
    contactsLoading = true;
    contactsFinishedLoading = false;
    Log.d(TAG, "starting to sync contact list ...");
    AsyncTask.execute(new Runnable() {
      @Override public void run() {
        loadContactMap();
        if (contactsToFilter != null) {
          displayedContacts = filterContactList(contactsToFilter, filterByID);
        } else {
          displayedContacts = new ArrayList<>(phoneBookIDMap.values());
        }
        sortContactList();
        Log.d(TAG, "list sorted, setting flags, about to post event");
        contactsLoading = false;
        contactsFinishedLoading = true;
        EventBus.getDefault().post(new ContactsLoadedEvent());
      }
    });
  }

  private void loadContactMap() {
    phoneBookIDMap.clear();
    phoneBookMSISDNMap.clear();

    try {

      // todo : check for permission

      ContentResolver cr = ApplicationLoader.applicationContext.getContentResolver();

      List<Integer> contactKeys = new ArrayList<>();
      Cursor contactHolder =
          cr.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, projectionForPhones, null,
              null, null);

      if (contactHolder != null) {
        final int keyCol =
            contactHolder.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.CONTACT_ID);
        final int numberCol =
            contactHolder.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER);

        while (contactHolder.moveToNext()) {
          String number = contactHolder.getString(numberCol);

          if (TextUtils.isEmpty(number) || !Utilities.checkIfLocalNumber(number)) {
            continue;
          }

          String numberNorm = Utilities.formatNumberToE164(number);

          if (phoneBookMSISDNMap.containsKey(numberNorm)) {
            continue;
          }

          Integer id = contactHolder.getInt(keyCol);
          if (!contactKeys.contains(id)) {
            contactKeys.add(id);
          }

          Contact contact = phoneBookIDMap.get(id);
          if (contact == null) {
            contact = new Contact();
            contact.id = id;
            phoneBookIDMap.put(id, contact);
          }

          contact.numbers.add(new RealmString(number));
          contact.msisdns.add(new RealmString(numberNorm));

          phoneBookMSISDNMap.put(numberNorm, contact);
        }
        contactHolder.close();
      }

      final String ids = TextUtils.join(",", contactKeys);
      final String queryString = ContactsContract.CommonDataKinds.StructuredName.CONTACT_ID
          + " IN ("
          + ids
          + ") AND "
          + ContactsContract.Data.MIMETYPE
          + " = '"
          + ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE
          + "'";

      contactHolder =
          cr.query(ContactsContract.Data.CONTENT_URI, projectionForNames, queryString, null, null);

      if (contactHolder != null) {
        while (contactHolder.moveToNext()) {
          Integer lKey = contactHolder.getInt(0);
          String fName = contactHolder.getString(1);
          String lName = contactHolder.getString(2);
          String miscName = contactHolder.getString(3);

          Contact contact = phoneBookIDMap.get(lKey);

          if (contact != null && TextUtils.isEmpty(contact.firstName) && TextUtils.isEmpty(
              contact.lastName)) {
            contact.firstName = fName == null ? "" : fName;
            contact.lastName = lName == null ? "" : lName;
            if (TextUtils.isEmpty(contact.firstName)
                && TextUtils.isEmpty(contact.lastName)
                && !TextUtils.isEmpty(miscName)) {
              contact.firstName = miscName;
            }
          }
        }
        contactHolder.close();
      }

      try {
        final String selection =
            ContactsContract.RawContacts.ACCOUNT_TYPE + " = " + "'com.whatsapp'";
        contactHolder =
            cr.query(ContactsContract.RawContacts.CONTENT_URI, projectionForWhatsApp, selection,
                null, null);
        if (contactHolder != null) {
          while (contactHolder.moveToNext()) {
            String phone = contactHolder.getString(1);
            String normNumber = Utilities.formatNumberToE164(phone);

            if (!Utilities.checkIfLocalNumber(normNumber)) {
              continue;
            }

            if (phoneBookMSISDNMap.containsKey(normNumber)) {
              continue;
            }

            String name = contactHolder.getString(0);
            if (TextUtils.isEmpty(name)) {
              name = phone; // todo : format this
            }

            Contact contact = new Contact();
            contact.firstName = name;
            contact.lastName = "";
            contact.id = contactHolder.getInt(2);
            contact.numbers.add(new RealmString(phone));
            contact.msisdns.add(new RealmString(normNumber));

            phoneBookMSISDNMap.put(normNumber, contact);
            phoneBookIDMap.put(contact.id, contact);
          }
          contactHolder.close();
        }
      } catch (Exception e) {
        Log.e(TAG,
            "ERROR! Something went wrong in attempting to read WhatsApp contacts ... trace ...");
        e.printStackTrace(); // todo : log to file
      }
    } catch (Exception e) {
      Log.e(TAG, "ERROR! Something went wrong instide contact assembly ... ");
      e.printStackTrace();
      phoneBookIDMap.clear();
      phoneBookMSISDNMap.clear();
    }
  }

  private void sortContactList() {
    Collections.sort(displayedContacts, new Comparator<Contact>() {
      @Override public int compare(Contact contact, Contact t1) {
        return contact.getDisplayName().compareTo(t1.getDisplayName());
      }
    });
  }

  // a faster implementation would be to do this via removing on displayedContacts, but Java list remove
  // is not behaving well (even with equals/hashcode on ID), hence doing it this way
  public List<Contact> filterContactList(List<Contact> filteredContacts, boolean filterByID) {
    HashMap<Integer, Contact> localReplica = new HashMap<>(phoneBookIDMap);
    List<Integer> remove = new ArrayList<>();
    for (Contact c : filteredContacts) {
      if (filterByID) {
        remove.add(c.id);
      } else {
        Contact contactWithId = phoneBookMSISDNMap.get(c.selectedMsisdn);
        if (contactWithId != null) {
          remove.add(contactWithId.id);
        }
      }
    }
    Log.e(TAG, "removing IDs  : " + remove);
    List<Integer> remainder = new ArrayList<>(phoneBookIDMap.keySet());
    remainder.removeAll(remove);
    localReplica.keySet().retainAll(remainder);
    Log.e(TAG, "replica filtered : " + localReplica);
    return new ArrayList<>(localReplica.values());
  }

  public void resetSelectedState(boolean selected) {
    if (displayedContacts == null) {
      return;
    }

    for (Contact c : displayedContacts)
      c.isSelected = selected;
  }

  public List<Contact> returnSelectedContacts() {
    List<Contact> toReturn = new ArrayList<>();
    for (Contact c : displayedContacts) { // hmm, once do filters, may need to use whole list
      if (c.isSelected) toReturn.add(c);
    }
    return toReturn;
  }
}
