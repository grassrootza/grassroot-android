package org.grassroot.android.services;

import android.app.Application;
import android.content.Context;

import com.jakewharton.picasso.OkHttp3Downloader;
import com.squareup.picasso.Picasso;

import io.realm.Realm;
import io.realm.RealmConfiguration;
import okhttp3.Cache;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Response;

import org.grassroot.android.BuildConfig;

import java.io.IOException;

/**
 * Created by luke on 2016/06/17.
 */
public class ApplicationLoader extends Application {

  public static volatile Context applicationContext;

  @Override public void onCreate() {
    super.onCreate();

    applicationContext = getApplicationContext();

    // Create a RealmConfiguration that saves the Realm file in the app's "files" directory.
    RealmConfiguration.Builder realmConfigBuilder =
        new RealmConfiguration.Builder(applicationContext);
    if (BuildConfig.DEBUG) {
      realmConfigBuilder.deleteRealmIfMigrationNeeded();
    }
    Realm.setDefaultConfiguration(realmConfigBuilder.build());


    //create a custom okhttp client for picasso and instantiate singleton
    OkHttpClient okHttpClient = new OkHttpClient.Builder().addNetworkInterceptor(new Interceptor() {
      @Override
      public Response intercept(Chain chain) throws IOException {
        Response originalResponse = chain.proceed(chain.request());
        return originalResponse.newBuilder().header("Cache-Control", "max-age=" + (60 * 60 * 24 * 365)).build();
      }
    }).cache(new Cache(applicationContext.getCacheDir(), Integer.MAX_VALUE)).build();

    Picasso.Builder builder = new Picasso.Builder(getApplicationContext());
    builder.downloader(new OkHttp3Downloader(okHttpClient));
    Picasso built = builder.build();
    built.setIndicatorsEnabled(true);
    built.setLoggingEnabled(true);
    Picasso.setSingletonInstance(built);

  }

  // todo : turn location utils into a service started here
}
