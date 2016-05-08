package com.techmorphosis.grassroot.Interface;

import com.techmorphosis.grassroot.models.SingleContact;

import java.util.List;

public interface ContactListRequester {
    void putContactList(List<SingleContact> list);
}
