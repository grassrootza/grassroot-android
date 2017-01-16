package org.grassroot.android.models.responses;

import com.google.gson.annotations.SerializedName;

import org.grassroot.android.models.Account;

/**
 * Created by luke on 2017/01/11.
 */

public class AccountResponse extends AbstractResponse {

    @SerializedName("data")
    private Account account;

    public Account getAccount() { return account; }

}
