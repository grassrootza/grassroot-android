package org.grassroot.android.services;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

import com.oppwa.mobile.connect.checkout.dialog.CheckoutActivity;
import com.oppwa.mobile.connect.checkout.meta.CheckoutPaymentMethod;
import com.oppwa.mobile.connect.checkout.meta.CheckoutSettings;
import com.oppwa.mobile.connect.exception.PaymentException;
import com.oppwa.mobile.connect.provider.Connect;
import com.oppwa.mobile.connect.service.IProviderBinder;

/**
 * Created by luke on 2017/01/26.
 */

public class AccountService {

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

}
