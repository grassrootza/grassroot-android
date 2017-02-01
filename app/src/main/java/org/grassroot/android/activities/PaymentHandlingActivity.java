package org.grassroot.android.activities;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;

/**
 * Created by luke on 2017/02/01.
 */

public class PaymentHandlingActivity extends Activity {

    private static final String TAG = PaymentHandlingActivity.class.getSimpleName();

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        if ("grassroot".equals(intent.getScheme())) {
            String checkoutId = intent.getData().getQueryParameter("id");
            Log.e(TAG, "callback triggered! checkout Id: " + checkoutId);

            // check if payment succeeded

            Intent i = new Intent(this, GrassrootExtraActivity.class);
            startActivity(i);
        }
    }

}
