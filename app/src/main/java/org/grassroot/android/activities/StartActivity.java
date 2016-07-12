package org.grassroot.android.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import org.grassroot.android.services.ApplicationLoader;
import org.grassroot.android.services.LocationServices;
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
        LocationServices.getInstance().connect();
        Intent i  = PreferenceUtils.userHasGroups(ApplicationLoader.applicationContext) ?
                new Intent(StartActivity.this, HomeScreenActivity.class) :
                new Intent(StartActivity.this, NoGroupWelcomeActivity.class);
        startActivity(i);
    }

}