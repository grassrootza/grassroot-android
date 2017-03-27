package org.grassroot.android.services;

import android.app.IntentService;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import org.grassroot.android.R;
import org.grassroot.android.interfaces.TaskConstants;
import org.grassroot.android.models.PreferenceObject;
import org.grassroot.android.models.ShareModel;
import org.grassroot.android.models.TaskModel;
import org.grassroot.android.utils.RealmUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import io.realm.RealmList;

public class SharingService extends IntentService {

    public static final String APP_SHARE_TAG = "APP_SHARE_TAG";
    public static final String FB_PACKAGE_NAME = "com.facebook.orca";
    public static final String WAPP_PACKAGE_NAME = "com.whatsapp";
    public static final String OTHER = "OTHER";

    public static final String TASK_TAG = "TASK_TAG"; // if passed, the service assembles default message
    public static final String MESSAGE = "MESSAGE"; // if passed, the share is just the message

    public static final String ACTION_TYPE = "ACTION_TYPE";
    public static final String TYPE_SEARCH = "SEARCH";
    public static final String TYPE_SHARE = "SHARE";

    private static final String TAG = SharingService.class.getName();

    public SharingService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent1) {

        if (TYPE_SHARE.equals(intent1.getExtras().getString(ACTION_TYPE))) {
            final String appToShare = intent1.getExtras().getString(APP_SHARE_TAG);
            final String explicitMessage = intent1.getExtras().getString(MESSAGE);
            final TaskModel task = (TaskModel) intent1.getExtras().get(TASK_TAG);
            if (!TextUtils.isEmpty(explicitMessage)) {
                assembleAndLaunchIntent(appToShare, explicitMessage);
            } else if (task != null) {
                assembleAndLaunchIntent(appToShare, assembleShareMessage(task));
            }  // else do nothing
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

    public static Intent simpleTextShare(final String message) {
        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, message);
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return shareIntent;
    }

    private void assembleAndLaunchIntent(final String appToShare, final String message) {
        if (!OTHER.equals(appToShare)) {
            shareByStandardApp(appToShare, message);
        } else {
            Intent i = findOtherClients(getApplicationContext(), message);
            if (i != null) {
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                safeShareTrigger(i);
            }
        }
    }

    public static boolean jumpStraightToOtherIntent() {
        return !RealmUtils.loadPreferencesFromDB().isHasWappInstalled()
            && !RealmUtils.loadPreferencesFromDB().isHasFbInstalled();
    }

    public static CharSequence[] itemsForMultiChoice() {
        final boolean hasWApp = RealmUtils.loadPreferencesFromDB().isHasWappInstalled();
        final boolean hasFB = RealmUtils.loadPreferencesFromDB().isHasWappInstalled();
        final int labelSize = 1 + (hasWApp ? 1 : 0) + (hasFB ? 1 : 0);
        CharSequence[] itemLabels = new CharSequence[ labelSize ];
        if (hasWApp)
            itemLabels[0] = ApplicationLoader.applicationContext.getString(R.string.wapp_short);
        if (hasFB)
            itemLabels[hasWApp ? 1 : 0] = ApplicationLoader.applicationContext.getString(R.string.fbm_short);
        itemLabels[labelSize - 1] = ApplicationLoader.applicationContext.getString(R.string.other_short);
        return itemLabels;
    }

    public static String sharePackageFromItemSelected(int optionSelected) {

        final boolean hasWApp = RealmUtils.loadPreferencesFromDB().isHasWappInstalled();
        final boolean hasFBM = RealmUtils.loadPreferencesFromDB().isHasWappInstalled();

        if (optionSelected == 0) {
            if (hasWApp) {
                return WAPP_PACKAGE_NAME;
            } else if (hasFBM) {
                return FB_PACKAGE_NAME;
            } else {
                return OTHER;
            }
        } else if (optionSelected == 1){
            if (hasFBM) {
                return FB_PACKAGE_NAME;
            } else {
                return OTHER;
            }
        } else {
            return OTHER;
        }
    }

    private void shareByStandardApp(final String appToShare, final String message) {
        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, message);
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        shareIntent.setPackage(appToShare);
        safeShareTrigger(shareIntent);
    }

    private void safeShareTrigger(Intent shareIntent) {
        try {
            startActivity(shareIntent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(ApplicationLoader.applicationContext, R.string.share_error_no_app,
                Toast.LENGTH_SHORT).show();
        }
    }

    private String assembleShareMessage(final TaskModel task) {
        String message;
        switch (task.getType()) {
            case TaskConstants.MEETING:
                final String mtgFormat = getString(R.string.share_meeting_format);
                final String mtgDate = TaskConstants.dateDisplayWithDayName.format(task.getDeadlineDate());
                message = String.format(mtgFormat, task.getTitle(), mtgDate, task.getLocation());
                break;
            case TaskConstants.VOTE:
                final String voteFormat = getString(R.string.share_vote_format);
                final String voteDate = TaskConstants.dateDisplayFormatWithHours.format(task.getDeadlineDate());
                message = String.format(voteFormat, task.getTitle(), voteDate);
                break;
            case TaskConstants.TODO:
                final String todoFormat = getString(R.string.share_todo_format);
                final String todoDate = TaskConstants.dateDisplayWithoutHours.format(task.getDeadlineDate());
                message = String.format(todoFormat, task.getTitle(), todoDate);
                break;
            default:
                Log.e(TAG, "error, invalid task type in share!");
                message = null;
        }
        return message;
    }

    private void findClients(final Context context) {
        Observable.create(new ObservableOnSubscribe<Boolean>() {
            @Override
            public void subscribe(ObservableEmitter<Boolean> subscriber) {
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
                        if (defaultPackage.equals(FB_PACKAGE_NAME))
                            hasDeletedDefaultPackage = true;

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

    private Intent findOtherClients(Context context, final String message) {
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
                    intent.putExtra(Intent.EXTRA_TEXT, message);
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
