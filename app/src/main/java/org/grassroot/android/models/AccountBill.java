package org.grassroot.android.models;

/**
 * Created by luke on 2017/01/19.
 */

public class AccountBill {

    private String uid;
    private String accountUid;

    private long createdDateTimeMillis;
    private long statementDateTimeMillis;
    private long billedPeriodStartMillis;
    private long billedPeriodEndMillis;

    private long nextPaymentDateMillis;
    private long paidDateMillis;

    private long openingBalance;
    private long amountBilledThisPeriod;
    private long totalAmountToPay;

    private boolean paid;
    private long paidAmount;
    private String paymentId;
    private String paymentDescription;

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getAccountUid() {
        return accountUid;
    }

    public void setAccountUid(String accountUid) {
        this.accountUid = accountUid;
    }

    public long getCreatedDateTimeMillis() {
        return createdDateTimeMillis;
    }

    public void setCreatedDateTimeMillis(long createdDateTimeMillis) {
        this.createdDateTimeMillis = createdDateTimeMillis;
    }

    public long getStatementDateTimeMillis() {
        return statementDateTimeMillis;
    }

    public void setStatementDateTimeMillis(long statementDateTimeMillis) {
        this.statementDateTimeMillis = statementDateTimeMillis;
    }

    public long getBilledPeriodStartMillis() {
        return billedPeriodStartMillis;
    }

    public void setBilledPeriodStartMillis(long billedPeriodStartMillis) {
        this.billedPeriodStartMillis = billedPeriodStartMillis;
    }

    public long getBilledPeriodEndMillis() {
        return billedPeriodEndMillis;
    }

    public void setBilledPeriodEndMillis(long billedPeriodEndMillis) {
        this.billedPeriodEndMillis = billedPeriodEndMillis;
    }

    public long getNextPaymentDateMillis() {
        return nextPaymentDateMillis;
    }

    public void setNextPaymentDateMillis(long nextPaymentDateMillis) {
        this.nextPaymentDateMillis = nextPaymentDateMillis;
    }

    public long getPaidDateMillis() {
        return paidDateMillis;
    }

    public void setPaidDateMillis(long paidDateMillis) {
        this.paidDateMillis = paidDateMillis;
    }

    public long getOpeningBalance() {
        return openingBalance;
    }

    public void setOpeningBalance(long openingBalance) {
        this.openingBalance = openingBalance;
    }

    public long getAmountBilledThisPeriod() {
        return amountBilledThisPeriod;
    }

    public void setAmountBilledThisPeriod(long amountBilledThisPeriod) {
        this.amountBilledThisPeriod = amountBilledThisPeriod;
    }

    public long getTotalAmountToPay() {
        return totalAmountToPay;
    }

    public void setTotalAmountToPay(long totalAmountToPay) {
        this.totalAmountToPay = totalAmountToPay;
    }

    public boolean isPaid() {
        return paid;
    }

    public void setPaid(boolean paid) {
        this.paid = paid;
    }

    public long getPaidAmount() {
        return paidAmount;
    }

    public void setPaidAmount(long paidAmount) {
        this.paidAmount = paidAmount;
    }

    public String getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(String paymentId) {
        this.paymentId = paymentId;
    }

    public String getPaymentDescription() {
        return paymentDescription;
    }

    public void setPaymentDescription(String paymentDescription) {
        this.paymentDescription = paymentDescription;
    }
}
