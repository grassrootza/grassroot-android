package org.grassroot.android.models;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Date;

/**
 * Created by luke on 2017/01/11.
 */

public class Account implements Parcelable {

    private String uid;
    private String createdByUserName;
    private boolean createdByCallingUser;

    private String billingUserName;
    private boolean billedToCallingUser;

    private boolean enabled;

    private String name;
    private String type;

    private int maxNumberGroups;
    private int maxSizePerGroup;
    private int maxSubGroupDepth;
    private int todosPerGroupPerMonth;
    private int freeFormMessages;

    private long outstandingBalance;
    private int subscriptionFee;

    private long nextBillingDateMilli;
    private long lastPaymentDateMilli;

    protected Account(Parcel in) {
        uid = in.readString();
        createdByUserName = in.readString();
        createdByCallingUser = in.readByte() != 0;
        billingUserName = in.readString();
        billedToCallingUser = in.readByte() != 0;
        enabled = in.readByte() != 0;
        name = in.readString();
        type = in.readString();
        maxNumberGroups = in.readInt();
        maxSizePerGroup = in.readInt();
        maxSubGroupDepth = in.readInt();
        todosPerGroupPerMonth = in.readInt();
        freeFormMessages = in.readInt();
        outstandingBalance = in.readLong();
        subscriptionFee = in.readInt();
        nextBillingDateMilli = in.readLong();
        lastPaymentDateMilli = in.readLong();
    }

    public static final Creator<Account> CREATOR = new Creator<Account>() {
        @Override
        public Account createFromParcel(Parcel in) {
            return new Account(in);
        }

        @Override
        public Account[] newArray(int size) {
            return new Account[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(uid);
        parcel.writeString(createdByUserName);
        parcel.writeByte((byte) (createdByCallingUser ? 1 : 0));
        parcel.writeString(billingUserName);
        parcel.writeByte((byte) (billedToCallingUser ? 1 : 0));
        parcel.writeByte((byte) (enabled ? 1 : 0));
        parcel.writeString(name);
        parcel.writeString(type);
        parcel.writeInt(maxNumberGroups);
        parcel.writeInt(maxSizePerGroup);
        parcel.writeInt(maxSubGroupDepth);
        parcel.writeInt(todosPerGroupPerMonth);
        parcel.writeInt(freeFormMessages);
        parcel.writeLong(outstandingBalance);
        parcel.writeInt(subscriptionFee);
        parcel.writeLong(nextBillingDateMilli);
        parcel.writeLong(lastPaymentDateMilli);
    }

    public String getUid() {
        return uid;
    }

    public String getCreatedByUserName() {
        return createdByUserName;
    }

    public boolean isCreatedByCallingUser() {
        return createdByCallingUser;
    }

    public String getBillingUserName() {
        return billingUserName;
    }

    public boolean isBilledToCallingUser() {
        return billedToCallingUser;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getName() {
        return name;
    }

    public String getType() { return type; }

    public int getMaxNumberGroups() {
        return maxNumberGroups;
    }

    public int getMaxSizePerGroup() {
        return maxSizePerGroup;
    }

    public int getMaxSubGroupDepth() {
        return maxSubGroupDepth;
    }

    public int getTodosPerGroupPerMonth() {
        return todosPerGroupPerMonth;
    }

    public int getFreeFormMessages() {
        return freeFormMessages;
    }

    public long getOutstandingBalance() {
        return outstandingBalance;
    }

    public int getSubscriptionFee() {
        return subscriptionFee;
    }

    public long getNextBillingDateMilli() { return nextBillingDateMilli; }

    public long getLastPaymentDateMilli() { return lastPaymentDateMilli; }

    public Date getNextBillingDate() { return new Date(nextBillingDateMilli); }

    public Date getLastPaymentDate() { return new Date(lastPaymentDateMilli); }


}
