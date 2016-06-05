package org.grassroot.android.services;

import android.content.Intent;

/**
 * Created by paballo on 2016/05/09.
 */
public class InstanceIDListenerService  extends com.google.android.gms.iid.InstanceIDListenerService{

    @Override
    public void onTokenRefresh() {
        // Fetch updated Instance ID token and notify our app's server of any changes (if applicable).
        Intent intent = new Intent(this, GcmRegistrationService.class);
        startService(intent);
    }

}
