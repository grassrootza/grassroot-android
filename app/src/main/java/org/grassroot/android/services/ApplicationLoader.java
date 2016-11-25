package org.grassroot.android.services;

import android.app.ActivityManager;
import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.jakewharton.picasso.OkHttp3Downloader;
import com.squareup.picasso.Picasso;

import org.grassroot.android.BuildConfig;
import org.grassroot.android.models.RealmMigrationGroupChat;
import org.grassroot.android.receivers.TaskManagerReceiver;

import java.io.IOException;
import java.util.List;

import io.fabric.sdk.android.Fabric;
import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.exceptions.RealmMigrationNeededException;
import okhttp3.Cache;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Response;

import static org.grassroot.android.utils.RealmUtils.loadPreferencesFromDB;

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

        realmConfigBuilder.schemaVersion(2).migration(new RealmMigrationGroupChat());
        Realm.setDefaultConfiguration(realmConfigBuilder.build());

        // since there was some early confusion, try to open it, to trigger a Realm error, and wipe if needed
        try {
            Realm realm = Realm.getDefaultInstance();
            realm.close();
        } catch (RealmMigrationNeededException|IllegalArgumentException e) {
            Log.e("GRASSROOT", "Error! Realm migration failed, wiping DB");
            Realm.deleteRealm(realmConfigBuilder.build());
        }

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

    public static boolean isAppIsInBackground(Context context) {
        boolean isInBackground = true;
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT_WATCH) {
            List<ActivityManager.RunningAppProcessInfo> runningProcesses = am.getRunningAppProcesses();
            for (ActivityManager.RunningAppProcessInfo processInfo : runningProcesses) {
                if (processInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                    for (String activeProcess : processInfo.pkgList) {
                        if (activeProcess.equals(context.getPackageName())) {
                            isInBackground = false;
                        }
                    }
                }
            }
        } else {
            List<ActivityManager.RunningTaskInfo> taskInfo = am.getRunningTasks(1);
            ComponentName componentInfo = taskInfo.get(0).topActivity;
            if (componentInfo.getPackageName().equals(context.getPackageName())) {
                isInBackground = false;
            }
        }
        return isInBackground;
    }

}
