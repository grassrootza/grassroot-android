package org.grassroot.android.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;

import org.grassroot.android.utils.Constant;
import org.grassroot.android.utils.LocationUtils;
import org.grassroot.android.utils.PreferenceUtils;

/**
 * Created by luke on 15-June-2016.
 */
public class StartActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        if (PreferenceUtils.getLoggedInStatus(this)) {
            userIsLoggedIn();
        } else {
            userIsNotLoggedIn();
        }
    }

    private void userIsNotLoggedIn() {
        Intent i = new Intent(this, IntroActivity.class);
        startActivity(i);
    }

    private void userIsLoggedIn() {
        LocationUtils.getInstance(getApplicationContext()).connect();
        Intent i  = new Intent(StartActivity.this, HomeScreenActivity.class);
        startActivity(i);
    }

}