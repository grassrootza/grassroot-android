package com.techmorphosis.grassroot.interfaces;

import com.techmorphosis.grassroot.models.Contact;

import java.util.List;

public interface ContactListRequester {
    void putContactList(List<Contact> list);
}
