package com.org.grassroot.ui.activities;

import android.os.Build;

import com.org.grassroot.BuildConfig;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;

import org.grassroot.android.ui.activities.CreateGroupActivity;

import static org.junit.Assert.*;

/**
 * Created by paballo on 2016/05/16.
 */
@Config(constants = BuildConfig.class, sdk = Build.VERSION_CODES.LOLLIPOP)
@RunWith(RobolectricGradleTestRunner.class)
public class CreateGroupActivityTest {

    private CreateGroupActivity createGroupActivity;



    @Before
    public void setUp(){
        createGroupActivity = Robolectric.buildActivity(CreateGroupActivity.class).create().attach().get();
    }

    @Test
    public void activityShouldNotBeNull(){
        assertNotNull(createGroupActivity);
    }

}