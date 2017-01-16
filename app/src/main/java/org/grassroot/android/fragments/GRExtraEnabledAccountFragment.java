package org.grassroot.android.fragments;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.grassroot.android.R;
import org.grassroot.android.models.Account;
import org.grassroot.android.utils.ErrorUtils;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;

/**
 * Created by luke on 2017/01/11.
 */

public class GRExtraEnabledAccountFragment extends Fragment {

    private static final String TAG = GRExtraEnabledAccountFragment.class.getSimpleName();

    private static final String ACCOUNT = "account";

    private Account account;
    private Unbinder unbinder;

    @BindView(R.id.account_settings_header) TextView accounSettingsHeader;
    @BindView(R.id.account_type_field) TextView accountTypeDescription;
    @BindView(R.id.account_settings_billing_dates) TextView accountBillingDates;

    @BindView(R.id.account_limits_free_form) TextView freeFormPerMonth;
    @BindView(R.id.account_limits_group_nums) TextView groupsOnAccount;

    @BindView(R.id.account_limits_group_size) TextView groupSizeLimit;
    @BindView(R.id.account_limits_todos_month) TextView todosPerMonthLimit;


    public static GRExtraEnabledAccountFragment newInstance(@NonNull Account account) {
        GRExtraEnabledAccountFragment fragment = new GRExtraEnabledAccountFragment();
        Bundle bundle = new Bundle();
        bundle.putParcelable(ACCOUNT, account);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_account_settings, container, false);
        unbinder = ButterKnife.bind(this, view);

        account = getArguments().getParcelable(ACCOUNT);
        if (account == null) {
            Log.e(TAG, "error! null account passed to fragment");
            startActivity(ErrorUtils.gracefulExitToHome(getActivity()));
        } else {
            setTextFields();
        }

        return view;
    }

    private void setTextFields() {
        accounSettingsHeader.setText(account.getName());
        accountTypeDescription.setText(getString(R.string.account_type, account.getType().toLowerCase()));

        final DecimalFormat subs = new DecimalFormat("0.##");
        final String subscriptionFee = getString(R.string.account_subs_fee, subs.format(account.getSubscriptionFee() / 100));

        final String nextBillingDate = account.getNextBillingDateMilli() == 0 ? "" :
                getString(R.string.account_billing_next, formatDate(account.getNextBillingDate()));
        final String lastPaymentDate = account.getLastPaymentDateMilli() == 0 ? "" :
                getString(R.string.account_payment_last, formatDate(account.getLastPaymentDate()));

        accountBillingDates.setText(subscriptionFee + nextBillingDate + lastPaymentDate);

        freeFormPerMonth.setText(getString(R.string.account_limits_msgs, account.getFreeFormMessages()));
        groupsOnAccount.setText(getString(R.string.account_limits_group_num, account.getMaxNumberGroups()));
        groupSizeLimit.setText(getString(R.string.account_limits_group_size, account.getMaxSizePerGroup()));
        todosPerMonthLimit.setText(getString(R.string.account_limits_todos, account.getTodosPerGroupPerMonth()));

    }

    private String formatDate(final Date date) {
        final DateFormat sdf = SimpleDateFormat.getDateInstance(DateFormat.SHORT);
        ((SimpleDateFormat) sdf).applyLocalizedPattern("EEE, MMM d");
        return sdf.format(date);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }

    @OnClick(R.id.gextra_free_form_btn)
    public void sendFreeFormMsg() {
        Log.e(TAG, "sending free form message!");
    }

    @OnClick(R.id.gextra_add_group_btn)
    public void addGroupToAccount() {
        Log.e(TAG, "adding group to account!");
    }

    @OnClick(R.id.account_type_change)
    public void changeAccountType() {
        Log.e(TAG, "change account clicked!");
    }

}
