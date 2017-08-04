package org.grassroot.android.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import org.grassroot.android.services.NotificationService;
import org.grassroot.android.utils.LoginRegUtils;
import org.grassroot.android.utils.NetworkUtils;
import org.grassroot.android.utils.RealmUtils;

import static org.grassroot.android.services.NotificationService.isNotificationServiceRunning;

/**
 * Created by luke on 15-June-2016.
 */
public class StartActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        Log.d("START", "startActivity on create ... timer ... " + SystemClock.currentThreadTimeMillis());

        if (RealmUtils.loadPreferencesFromDB().isLoggedIn()) {
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
        Log.e("START", "user name :" + RealmUtils.loadPreferencesFromDB().getUserName());
        if(!isNotificationServiceRunning())startNotificationService();
        NetworkUtils.syncAndStartTasks(this, false, false).subscribe();
        Intent i  = LoginRegUtils.selectHomeScreen(this, false);
        startActivity(i);
    }

    private void startNotificationService(){
        Intent notificationServiceIntent = new Intent(this, NotificationService.class);
        startService(notificationServiceIntent);
    }




}