package org.grassroot.android.services;

import android.app.Application;
import android.content.Context;
import android.content.Intent;

import com.crashlytics.android.Crashlytics;
import com.jakewharton.picasso.OkHttp3Downloader;
import com.squareup.picasso.Picasso;

import io.fabric.sdk.android.Fabric;

import org.grassroot.android.BuildConfig;
import org.grassroot.android.receivers.TaskManagerReceiver;
import org.grassroot.android.utils.RealmUtils;

import java.io.IOException;

import io.realm.Realm;
import io.realm.RealmConfiguration;
import okhttp3.Cache;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Response;

import static org.grassroot.android.utils.RealmUtils.*;

/**
 * Created by luke on 2016/06/17.
 */
public class ApplicationLoader extends Application {

    public static volatile Context applicationContext;

    @Override
    public void onCreate() {
        super.onCreate();
        Fabric.with(this, new Crashlytics());

        applicationContext = getApplicationContext();

        // Create a RealmConfiguration that saves the Realm file in the app's "files" directory.
        RealmConfiguration.Builder realmConfigBuilder =
                new RealmConfiguration.Builder(applicationContext);

        realmConfigBuilder.deleteRealmIfMigrationNeeded();


        Realm.setDefaultConfiguration(realmConfigBuilder.build());

        //create a custom okhttp client for picasso and instantiate singleton
        OkHttpClient okHttpClient = new OkHttpClient.Builder().addNetworkInterceptor(new Interceptor() {
            @Override
            public Response intercept(Chain chain) throws IOException {
                Response originalResponse = chain.proceed(chain.request());
                return originalResponse.newBuilder()
                        .header("Cache-Control", "max-age=" + (60 * 60 * 24 * 365))
                        .build();
            }
        }).cache(new Cache(applicationContext.getFilesDir(), Integer.MAX_VALUE)).build();

        Picasso.Builder builder = new Picasso.Builder(getApplicationContext());
        builder.downloader(new OkHttp3Downloader(okHttpClient));
        Picasso built = builder.build();
        built.setIndicatorsEnabled(BuildConfig.BUILD_TYPE.equals("debug"));
        built.setLoggingEnabled(false);
        Picasso.setSingletonInstance(built);

        Intent i = new Intent(this, TaskManagerReceiver.class);
        i.setAction(TaskManagerReceiver.ACTION_START);
        sendBroadcast(i);
    }

    public static void initPlayServices() {
        if(!loadPreferencesFromDB().isHasGcmRegistered()) {
            Intent intent = new Intent(applicationContext, GcmRegistrationService.class);
            applicationContext.startService(intent);
        }
    }

}
