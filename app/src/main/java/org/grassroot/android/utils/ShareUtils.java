package org.grassroot.android.utils;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Parcelable;

import org.grassroot.android.models.PreferenceObject;
import org.grassroot.android.models.ShareModel;
import org.grassroot.android.models.TaskModel;
import org.grassroot.android.services.SharingService;

import java.util.ArrayList;
import java.util.List;

import io.realm.RealmList;
import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class ShareUtils {

    private ShareUtils() {
    }

    public static void findClients(final Context context) {
        Observable.create(new Observable.OnSubscribe<Boolean>() {
            @Override
            public void call(Subscriber<? super Boolean> subscriber) {
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
                                preferenceObject.setHasFbInstalled(true);
                            if (packageName.contains(SharingService.WAPP_PACKAGE_NAME))
                                preferenceObject.setHasWappInstalled(true);
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
                        apps.add(new ShareModel(SharingService.OTHER, SharingService.OTHER));
                        preferenceObject.setAppsToShare(apps);
                        if(!preferenceObject.isHasSelectedDefaultPackage()) {
                            if (preferenceObject.isHasWappInstalled()) {
                                preferenceObject.setDefaultSharePackage(SharingService.WAPP_PACKAGE_NAME);
                            } else if (preferenceObject.isHasFbInstalled()) {
                                preferenceObject.setDefaultSharePackage(SharingService.FB_PACKAGE_NAME);
                            } else {
                                preferenceObject.setDefaultSharePackage(SharingService.OTHER);
                            }
                        }
                        RealmUtils.saveDataToRealm(preferenceObject).subscribe();
                }
            }
        }).subscribeOn(Schedulers.computation()).observeOn(AndroidSchedulers.mainThread()).subscribe();
    }

    public static Intent findOtherClients(Context context, TaskModel task) {
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
}
