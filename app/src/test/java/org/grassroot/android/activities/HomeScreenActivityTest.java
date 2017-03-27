package org.grassroot.android.activities;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.support.v4.widget.DrawerLayout;

import org.grassroot.android.BuildConfig;
import org.grassroot.android.R;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.support.v4.SupportFragmentTestUtil;

import static org.junit.Assert.assertNotNull;

/**
 * Created by paballo on 2016/05/16.
 */

@Config(constants = BuildConfig.class, sdk = Build.VERSION_CODES.LOLLIPOP)
@RunWith(RobolectricTestRunner.class)
public class HomeScreenActivityTest {

    private HomeScreenActivity homeScreenActivity;
    private SharedPreferences sharedPreferences;
    private final String PREF_NAME = "Grassroot";


    @Before
    public void setUp(){
        homeScreenActivity = Robolectric.buildActivity(HomeScreenActivity.class).attach().create().start().resume().get();
     sharedPreferences = RuntimeEnvironment.application.getSharedPreferences(
                        PREF_NAME, Context.MODE_PRIVATE);
    }

    @Test
    public void  activityShouldNotBeNull(){
        assertNotNull(homeScreenActivity);
    }

    @Test
    public void viewsShouldNotBeNull(){
        DrawerLayout drawerLayout = (DrawerLayout) homeScreenActivity.findViewById(R.id.drawer_layout);
        assertNotNull(drawerLayout);

    }
    @Test
    public void welcomeFragmentIsShownWhenNoGroups() {
        getSharedPreferences().edit().putBoolean("IsHasgroup",false).commit();
        WelcomeFragment welcomeFragment = new WelcomeFragment();
        SupportFragmentTestUtil.startVisibleFragment(welcomeFragment);
        assertNotNull(welcomeFragment);
    }



    public SharedPreferences getSharedPreferences() {
        return sharedPreferences;
    }
}

