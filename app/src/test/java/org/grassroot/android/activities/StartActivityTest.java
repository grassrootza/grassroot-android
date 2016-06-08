package org.grassroot.android.activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import com.google.android.gms.common.ConnectionResult;
import com.grassroot.android.BuildConfig;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowActivity;
import org.robolectric.shadows.ShadowGooglePlayServicesUtil;
import org.robolectric.shadows.ShadowIntent;
import org.robolectric.shadows.ShadowLooper;
import org.robolectric.util.ActivityController;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.robolectric.Shadows.shadowOf;


/**
 * Created by paballo on 2016/05/12.
 */
@Config(constants = BuildConfig.class, sdk = Build.VERSION_CODES.LOLLIPOP)
@RunWith(RobolectricGradleTestRunner.class)
public class StartActivityTest {

    private ActivityController<StartActivity> controller;
    private StartActivity startActivity = new StartActivity();
    private static final String PREF_NAME = "Grassroot";
    private SharedPreferences sharedPreferences;



    @Before
    public void setUp(){
        controller = Robolectric.buildActivity(StartActivity.class);
        sharedPreferences = RuntimeEnvironment.application.getSharedPreferences(
                PREF_NAME, Context.MODE_PRIVATE);
    }

    @Test
    public void activityShouldNotBeNull() {
        startActivity = controller.create().start().resume().get();
        assertNotNull(startActivity);
    }

    @Test
    public void homeScreenActivityShouldBeStarted(){
        sharedPreferences = RuntimeEnvironment.application.getSharedPreferences(
                PREF_NAME, Context.MODE_PRIVATE);

        sharedPreferences.edit().putBoolean("IsLoggedIn", true).commit();
        startActivity = controller.create().start().resume().get();
        ShadowGooglePlayServicesUtil.setIsGooglePlayServicesAvailable(ConnectionResult.SUCCESS);
        ShadowActivity shadowActivity = shadowOf(startActivity);
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();
        Intent startedIntent = shadowActivity.getNextStartedActivity();
        ShadowIntent shadowIntent = shadowOf(startedIntent);
        assertEquals(HomeScreenActivity.class.getName(), shadowIntent.getComponent().getClassName());
    }


    @After
    public void tearDown()
    {
        controller.stop().destroy();
    }


}