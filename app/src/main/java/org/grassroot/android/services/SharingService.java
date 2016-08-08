package org.grassroot.android.services;

import android.app.IntentService;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.os.Parcelable;
import android.util.Log;

import org.grassroot.android.models.TaskModel;

import java.util.ArrayList;
import java.util.List;

public class SharingService extends IntentService {

    public static final String TASK_TAG = "TASK_TAG";

    private static final String TAG = SharingService.class.getName();

    public SharingService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent1) {
        Intent i = findClients((TaskModel) intent1.getExtras().get(TASK_TAG));
        if (i != null) {
            startActivity(i);
        }else{

        }
    }

    private Intent findClients(TaskModel task) {
        List<Intent> targetShareIntents = new ArrayList<>();
        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        List<ResolveInfo> resInfos = getPackageManager().queryIntentActivities(shareIntent, 0);
        if (!resInfos.isEmpty()) {
            for (ResolveInfo resInfo : resInfos) {
                String packageName = resInfo.activityInfo.packageName;
                if (packageName.contains("com.facebook.orca") || packageName.contains("com.whatsapp")) {
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
            } else {
                Log.d(TAG, "No apps to share");
            }
        }
        return null;
    }
}
