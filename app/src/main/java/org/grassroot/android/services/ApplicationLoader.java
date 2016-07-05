package org.grassroot.android.services;

import android.app.Application;
import android.content.Context;
import io.realm.Realm;
import io.realm.RealmConfiguration;
import org.grassroot.android.BuildConfig;

/**
 * Created by luke on 2016/06/17.
 */
public class ApplicationLoader extends Application {

  public static volatile Context applicationContext;

  @Override public void onCreate() {
    super.onCreate();
    // Create a RealmConfiguration that saves the Realm file in the app's "files" directory.
    RealmConfiguration.Builder realmConfigBuilder =
        new RealmConfiguration.Builder(getApplicationContext());
    if (BuildConfig.DEBUG) {
      realmConfigBuilder.deleteRealmIfMigrationNeeded();
    }
    Realm.setDefaultConfiguration(realmConfigBuilder.build());
    applicationContext = getApplicationContext();
  }

  // todo : turn location utils into a service started here
}
