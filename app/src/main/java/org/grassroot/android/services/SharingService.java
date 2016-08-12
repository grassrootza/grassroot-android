package org.grassroot.android.services;

import android.app.IntentService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Parcelable;
import android.util.Log;

import org.grassroot.android.models.PreferenceObject;
import org.grassroot.android.models.ShareModel;
import org.grassroot.android.models.TaskModel;
import org.grassroot.android.utils.RealmUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import io.realm.RealmList;
import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class SharingService extends IntentService {

    public static final String TASK_TAG = "TASK_TAG";
    public static final String APP_SHARE_TAG = "APP_SHARE_TAG";
    public static final String FB_PACKAGE_NAME = "com.facebook.orca";
    public static final String WAPP_PACKAGE_NAME = "com.whatsapp";
    public static final String OTHER = "OTHER";
    public static final String SEARCH_TYPE = "SEARCH";
    public static final String SHARE_TYPE = "SHARE";
    public static final String ACTION_TYPE = "ACTION_TYPE";


    private static final String TAG = SharingService.class.getName();

    public SharingService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent1) {
        if (intent1.getExtras().getString(ACTION_TYPE).equals(SHARE_TYPE)) {
            String appToShare = intent1.getExtras().getString(APP_SHARE_TAG);
            TaskModel task = (TaskModel) intent1.getExtras().get(TASK_TAG);
            Intent i = appToShare.equals(OTHER) ? findOtherClients(getApplicationContext(), task) : share(task, appToShare);
            if (i != null) {
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(i);
            }
        } else {
            PreferenceObject preference = RealmUtils.loadPreferencesFromDB();
            if (preference.getAppsToShare().size() == 3) {
                saveApps();
                Log.d(TAG, "Has installed packages");
            } else {
                Log.d(TAG, "Has not installed packages");
                findClients(this);
            }
        }
    }

    private Intent share(TaskModel task, String appToShare) {
        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, task.getTitle());
        shareIntent.setPackage(appToShare);
        return shareIntent;
    }

    private void findClients(final Context context) {
        Observable.create(new Observable.OnSubscribe<Boolean>() {
            @Override
            public void call(Subscriber<? super Boolean> subscriber) {
                boolean fbInstalled = false;
                boolean wappInstalled = false;
                Intent shareIntent = new Intent();
                shareIntent.setAction(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                RealmList<ShareModel> apps = new RealmList<>();
                PreferenceObject preferenceObject = RealmUtils.loadPreferencesFromDB();
                PackageManager pm = context.getPackageManager();
                List<ResolveInfo> resInfos = context.getPackageManager().queryIntentActivities(shareIntent, 0);
                if (!resInfos.isEmpty()) {
                    for (int i = 0; i < resInfos.size(); i++) {
                        ResolveInfo resInfo = resInfos.get(i);
                        String packageName = resInfo.activityInfo.packageName;
                        if (packageName.contains(SharingService.FB_PACKAGE_NAME) || packageName.contains(SharingService.WAPP_PACKAGE_NAME)) {
                            if (packageName.contains(SharingService.FB_PACKAGE_NAME))
                                fbInstalled = true;
                            if (packageName.contains(SharingService.WAPP_PACKAGE_NAME))
                                wappInstalled = true;
                            ApplicationInfo app = null;
                            try {
                                app = pm.getApplicationInfo(packageName, 0);
                                ShareModel model = new ShareModel(pm.getApplicationLabel(app).toString(), packageName);
                                apps.add(model);
                            } catch (PackageManager.NameNotFoundException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    preferenceObject.setHasFbInstalled(fbInstalled);
                    preferenceObject.setHasWappInstalled(wappInstalled);
                    apps.add(new ShareModel(SharingService.OTHER, SharingService.OTHER));
                    preferenceObject.setAppsToShare(apps);
                    if (!preferenceObject.isHasSelectedDefaultPackage()) {
                        setDefaultPackage(preferenceObject);
                    } else {
                        String defaultPackage = preferenceObject.getDefaultSharePackage();
                        boolean hasDeletedDefaultPackage = true;
                        for (ShareModel model : apps) {
                            if (model.getPackageName().equals(defaultPackage)) {
                                hasDeletedDefaultPackage = false;
                            }
                        }
                        if (hasDeletedDefaultPackage) setDefaultPackage(preferenceObject);
                    }
                    RealmUtils.saveDataToRealm(preferenceObject).subscribe();
                }
            }
        }).subscribeOn(Schedulers.computation()).observeOn(AndroidSchedulers.mainThread()).subscribe();
    }

    private boolean checkIfPackageExists(String packageName) {
        PackageManager pm = getPackageManager();
        try {
            PackageInfo info = pm.getPackageInfo(packageName, PackageManager.GET_META_DATA);
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
        return true;
    }

    private void saveApps() {
        boolean hasDeletedDefaultPackage = false;
        PreferenceObject preferenceObject = RealmUtils.loadPreferencesFromDB();
        RealmList<ShareModel> apps = preferenceObject.getAppsToShare();
        String defaultPackage = preferenceObject.getDefaultSharePackage();
        Iterator<ShareModel> i = apps.iterator();
        while (i.hasNext()) {
            ShareModel s = i.next();
            if (!checkIfPackageExists(s.getPackageName())) {
                switch (s.getPackageName()) {
                    case FB_PACKAGE_NAME:
                        preferenceObject.setHasFbInstalled(false);
                        if (defaultPackage.equals(FB_PACKAGE_NAME)) hasDeletedDefaultPackage = true;
                        i.remove();
                        break;
                    case WAPP_PACKAGE_NAME:
                        preferenceObject.setHasWappInstalled(false);
                        if (defaultPackage.equals(WAPP_PACKAGE_NAME))
                            hasDeletedDefaultPackage = true;
                        i.remove();
                        break;
                }
            }
        }
        if (hasDeletedDefaultPackage) setDefaultPackage(preferenceObject);
        RealmUtils.saveDataToRealm(preferenceObject).subscribe();
    }

    private Intent findOtherClients(Context context, TaskModel task) {
        List<Intent> targetShareIntents = new ArrayList<>();
        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        List<ResolveInfo> resInfos = context.getPackageManager().queryIntentActivities(shareIntent, 0);
        if (!resInfos.isEmpty()) {
            for (ResolveInfo resInfo : resInfos) {
                String packageName = resInfo.activityInfo.packageName;
                if (!packageName.contains("com.facebook.orca") && !packageName.contains("com.whatsapp")) {
                    Intent intent = new Intent();
                    intent.setComponent(new ComponentName(packageName, resInfo.activityInfo.name));
                    intent.setAction(Intent.ACTION_SEND);
                    intent.setType("text/plain");
                    intent.putExtra(Intent.EXTRA_TEXT, task.getTitle());
                    intent.setPackage(packageName);
                    targetShareIntents.add(intent);
                }
            }
            if (!targetShareIntents.isEmpty()) {
                Intent chooserIntent = Intent.createChooser(targetShareIntents.remove(0), "Choose app to share");
                chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, targetShareIntents.toArray(new Parcelable[]{}));
                chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                return chooserIntent;
            }
        }
        return null;
    }

    private void setDefaultPackage(PreferenceObject preferenceObject) {
        if (preferenceObject.isHasWappInstalled()) {
            preferenceObject.setDefaultSharePackage(SharingService.WAPP_PACKAGE_NAME);
        } else if (preferenceObject.isHasFbInstalled()) {
            preferenceObject.setDefaultSharePackage(SharingService.FB_PACKAGE_NAME);
        } else {
            preferenceObject.setDefaultSharePackage(SharingService.OTHER);
        }
    }
}
