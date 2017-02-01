package org.grassroot.android.services;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;

import com.oppwa.mobile.connect.checkout.dialog.CheckoutActivity;
import com.oppwa.mobile.connect.checkout.meta.CheckoutPaymentMethod;
import com.oppwa.mobile.connect.checkout.meta.CheckoutSettings;

import org.grassroot.android.R;

/**
 * Created by luke on 2017/01/26.
 */

public class AccountService {

    public static final String UID_FIELD = "accountUid";
    public static final String OBJECT_FIELD = "accountEntity";

    public static Intent initiateCheckout(Context callingContext, final String checkoutId, final String paymentTitle) {
        CheckoutSettings settings = new CheckoutSettings(
                checkoutId,
                paymentTitle,
                new CheckoutPaymentMethod[] {
                        CheckoutPaymentMethod.VISA,
                        CheckoutPaymentMethod.MASTERCARD
                });

        Intent intent = new Intent(callingContext, CheckoutActivity.class);
        intent.putExtra(CheckoutActivity.CHECKOUT_SETTINGS, settings);
        return intent;
    }

    public static Intent openWebApp() {
        final String url = "https://app.grassroot.org.za"; // todo : include direct to account page
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setData(Uri.parse(url));
        return  i;
    }

    public static AlertDialog.Builder showConnectionErrorDialog(Context context, DialogInterface.OnClickListener retryListener) {
        return new AlertDialog.Builder(context)
                .setMessage(R.string.account_connect_error_short)
                .setPositiveButton(R.string.snackbar_try_again, retryListener)
                .setNeutralButton(R.string.account_try_webapp, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        openWebApp();
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.cancel();
                    }
                })
                .setCancelable(true);
    }

    public static AlertDialog.Builder showServerErrorDialog(Context context, final String message, boolean showWebLink) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context)
                .setMessage(message)
                .setNegativeButton(R.string.alert_close, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.cancel();
                    }
                })
                .setCancelable(true);

        if (showWebLink) {
            builder.setPositiveButton(R.string.account_try_webapp, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    openWebApp();
                }
            });
        }

        return builder;
    }


}
