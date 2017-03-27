package org.grassroot.android.services;

import android.content.ContentResolver;
import android.database.ContentObserver;
import android.database.Cursor;
import android.os.Handler;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.util.Log;

import org.grassroot.android.events.PhoneContactsChanged;
import org.grassroot.android.models.Contact;
import org.grassroot.android.utils.Utilities;
import org.greenrobot.eventbus.EventBus;
import org.reactivestreams.Subscriber;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Scheduler;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by luke on 2016/06/17.
 */
public class ContactService {

  private static final String TAG = ContactService.class.getSimpleName();

  public List<Contact> displayedContacts = new ArrayList<>();
  private Map<Integer, Contact> phoneBookIDMap = new HashMap<>();
  private Map<String, Contact> phoneBookMSISDNMap = new HashMap<>();

  private List<Contact> storedFilteredContacts;

  public boolean contactsLoading = false;
  public boolean contactsFinishedLoading = false;

  private boolean observerRegistered = false;

  private final String[] projectionForPhones = {
      ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
      ContactsContract.CommonDataKinds.Phone.NUMBER, ContactsContract.CommonDataKinds.Phone.TYPE,
      ContactsContract.CommonDataKinds.Phone.LABEL,
      ContactsContract.RawContacts.VERSION // initial tests show this picks up _contact_ version, not phone number version, which is what we want, but keep an eye on it
  };

  private final String[] projectionForNames = {
      ContactsContract.CommonDataKinds.StructuredName.CONTACT_ID,
      ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME,
      ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME,
      ContactsContract.Data.DISPLAY_NAME,
      ContactsContract.RawContacts.VERSION // initial tests show this picks up _contact_ version, not phone number version, which is what we want, but keep an eye on it
  };

  private final String[] projectionForWhatsApp = {
      "display_name", ContactsContract.RawContacts.SYNC1, ContactsContract.RawContacts.CONTACT_ID
  };

  private static ContactService instance = null;

  protected ContactService() {
  }

  // todo : add cleanup method
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

  public Observable<Boolean> syncContactList(final List<Contact> contactsToFilter,
                                             final boolean clearSelections, Scheduler observingThread) {
    return Observable.create(new ObservableOnSubscribe<Boolean>() {
      @Override
      public void subscribe(ObservableEmitter<Boolean> subscriber) {
        contactsLoading = true;
        contactsFinishedLoading = false;

        if (phoneBookIDMap == null || phoneBookIDMap.isEmpty()) {
          loadContactMap();
        }

        storedFilteredContacts = contactsToFilter;
        resetDisplayToContactMap();

        contactsLoading = false;
        contactsFinishedLoading = true;
        subscriber.onNext(true);

        if (!observerRegistered) {
          ApplicationLoader.applicationContext.getContentResolver()
              .registerContentObserver(ContactsContract.Contacts.CONTENT_URI, true, contactsChangedObserver);
          observerRegistered = true;
        }

        subscriber.onComplete();
      }
    }).subscribeOn(Schedulers.io()).observeOn(observingThread);
  }

  private void resetDisplayToContactMap() {
    if (storedFilteredContacts != null) {
      displayedContacts = filterContactList(storedFilteredContacts);
      sortContactList();
    } else {
      displayedContacts = new ArrayList<>(phoneBookIDMap.values());
      sortContactList();
    }
  }

  private ContentObserver contactsChangedObserver = new ContentObserver(new Handler()) {
    @Override
    public void onChange(boolean selfChange) {
      super.onChange(selfChange);
      loadContactMap();
      resetDisplayToContactMap();
      EventBus.getDefault().post(new PhoneContactsChanged());
    }
  };

  public Observable<Boolean> setContactSelected(final int displayedPosition,
                                                final int selectedNumberIndex) {
    return Observable.create(new ObservableOnSubscribe<Boolean>() {
      @Override
      public void subscribe(ObservableEmitter<Boolean> subscriber) {
        final Contact contact = displayedContacts.get(displayedPosition);
        contact.selectedNumber = contact.numbers.get(selectedNumberIndex);
        contact.selectedMsisdn = contact.msisdns.get(selectedNumberIndex);
        contact.isSelected = true;
        phoneBookIDMap.put(contact.id, contact);
        subscriber.onNext(true);
        subscriber.onComplete();
      }
    }).subscribeOn(Schedulers.computation()).observeOn(AndroidSchedulers.mainThread());
  }

  public Observable<Boolean> toggleContactSelected(final int displayedPosition) {
    return Observable.create(new ObservableOnSubscribe<Boolean>() {
      @Override
      public void subscribe(ObservableEmitter<Boolean> subscriber) {
        final Contact contact = displayedContacts.get(displayedPosition);
        contact.isSelected = !contact.isSelected;
        phoneBookIDMap.put(contact.id, contact);
        subscriber.onNext(contact.isSelected);
        subscriber.onComplete();
      }
    }).subscribeOn(Schedulers.computation()).observeOn(AndroidSchedulers.mainThread());
  }

  private void loadContactMap() {

    try {
      ContentResolver cr = ApplicationLoader.applicationContext.getContentResolver();

      List<Integer> contactKeys = new ArrayList<>();
      Cursor contactHolder =
          cr.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, projectionForPhones, null,
              null, null);

      boolean changedVersionContactCleaned = false;

      if (contactHolder != null) {
        final int keyCol =
            contactHolder.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.CONTACT_ID);
        final int numberCol =
            contactHolder.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER);
        final int versionCol =
            contactHolder.getColumnIndexOrThrow(ContactsContract.RawContacts.VERSION);

        while (contactHolder.moveToNext()) {
          String number = contactHolder.getString(numberCol);
          int version = contactHolder.getInt(versionCol);

          if (TextUtils.isEmpty(number) || !Utilities.checkIfLocalNumber(number)) {
            continue;
          }

          // there is a number_norm column in contacts contract but don't fully trust it
          String numberNorm = Utilities.formatNumberToE164(number);

          // if the number has already been added to the contact, then just return
          if (phoneBookMSISDNMap.containsKey(numberNorm)) {
            continue;
          }

          // check if a contact is already being tracked and if not, start tracking it, and reset the cleaning flag
          Integer id = contactHolder.getInt(keyCol);
          if (!contactKeys.contains(id)) {
            contactKeys.add(id);
            changedVersionContactCleaned = false;
          }

          Log.d(TAG, String.format("phone number %s, version %d", numberNorm, version));

          // check if a contact has already been created, if not, create it and add it to the overall map
          // otherwise, if nothing has changed in the contact, don't bother
          // note : do not set contact version here or it will skip over all numbers except the first one
          // note : slightly unpredictable if contact updates while selected ... to fix in future

          Contact contact = phoneBookIDMap.get(id);
          if (contact == null) {
            contact = new Contact();
            contact.id = id;
            contact.version = -1; // to make sure we loop over all of the phone numbers
            phoneBookIDMap.put(id, contact);
          } else if (contact.version == version) {
            continue;
          } else if (contact.version != -1 && !changedVersionContactCleaned) {
            Log.d(TAG, "found a contact with changed version, resetting numbers ...");
            contact.numbers.clear();
            contact.msisdns.clear();
            changedVersionContactCleaned = true;
          }

          contact.numbers.add(number);
          contact.msisdns.add(numberNorm);

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

      // note : this is not fully triggering for name changes, to check in future
      contactHolder =
          cr.query(ContactsContract.Data.CONTENT_URI, projectionForNames, queryString, null, null);

      if (contactHolder != null) {
        while (contactHolder.moveToNext()) {
          Integer lKey = contactHolder.getInt(0);
          String fName = contactHolder.getString(1);
          String lName = contactHolder.getString(2);
          String miscName = contactHolder.getString(3);
          int version = contactHolder.getInt(4);

          Contact contact = phoneBookIDMap.get(lKey);
          if (contact.version == version) {
            continue;
          } else {
            contact.version = version;
          }

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
              name = phone;
            }

            Contact contact = new Contact();
            contact.firstName = name;
            contact.lastName = "";
            contact.id = contactHolder.getInt(2);
            contact.numbers.add(phone);
            contact.msisdns.add(normNumber);

            phoneBookMSISDNMap.put(normNumber, contact);
            phoneBookIDMap.put(contact.id, contact);
          }
          contactHolder.close();
        }
      } catch (Exception e) {
        Log.e(TAG,
            "ERROR! Something went wrong in attempting to read WhatsApp contacts ... trace ...");
        e.printStackTrace();
      }
    } catch (Exception e) {
      Log.e(TAG, "ERROR! Something went wrong inside contact assembly ... ");
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
  public List<Contact> filterContactList(List<Contact> filteredContacts) {
    HashMap<Integer, Contact> localReplica = new HashMap<>(phoneBookIDMap);
    List<Integer> remove = new ArrayList<>();
    for (Contact c : filteredContacts) {
      Contact contactWithId = phoneBookMSISDNMap.get(c.selectedMsisdn);
      if (contactWithId != null) {
        remove.add(contactWithId.id);
      }
    }
    List<Integer> remainder = new ArrayList<>(phoneBookIDMap.keySet());
    remainder.removeAll(remove);
    localReplica.keySet().retainAll(remainder);
    return new ArrayList<>(localReplica.values());
  }

  public void resetSelectedState(boolean selected) {
    if (displayedContacts == null) {
      return;
    }

    for (Contact c : displayedContacts)
      c.isSelected = selected;

    if (phoneBookIDMap != null) {
      for (Contact c : phoneBookIDMap.values()) {
        c.isSelected = false;
      }
    }
  }

  public List<Contact> returnSelectedContacts() {
    List<Contact> toReturn = new ArrayList<>();
    for (Contact c : displayedContacts) { // hmm, once do filters, may need to use whole list
      if (c.isSelected) toReturn.add(c);
    }
    return toReturn;
  }
}
