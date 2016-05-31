package org.grassroot.android.interfaces;

import org.grassroot.android.models.Contact;

import java.util.List;

public interface ContactListRequester {
    void putContactList(List<Contact> list);
}
