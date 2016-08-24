package org.grassroot.android.activities;

import android.os.Build;

import org.grassroot.android.BuildConfig;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertNotNull;

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