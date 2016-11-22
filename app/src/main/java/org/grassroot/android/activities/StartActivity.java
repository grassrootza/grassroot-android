package org.grassroot.android.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import org.grassroot.android.services.NotificationService;
import org.grassroot.android.utils.MqttConnectionManager;
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
        if(!isNotificationServiceRunning())startNotificationService();
        NetworkUtils.syncAndStartTasks(this, false, false).subscribe();
        MqttConnectionManager.getInstance().connect();
        String phone = RealmUtils.loadPreferencesFromDB().getMobileNumber();
        Intent i  = RealmUtils.loadPreferencesFromDB().isHasGroups() ?
                new Intent(StartActivity.this, HomeScreenActivity.class) :
                new Intent(StartActivity.this, NoGroupWelcomeActivity.class);
        startActivity(i);

    }

    private void startNotificationService(){
        Intent notificationServiceIntent = new Intent(this, NotificationService.class);
        startService(notificationServiceIntent);
    }




}