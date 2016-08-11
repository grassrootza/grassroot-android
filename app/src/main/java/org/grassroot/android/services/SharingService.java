package org.grassroot.android.services;

import android.app.IntentService;
import android.content.Intent;

import org.grassroot.android.models.TaskModel;
import org.grassroot.android.utils.ShareUtils;

public class SharingService extends IntentService {

    public static final String TASK_TAG = "TASK_TAG";
    public static final String APP_SHARE_TAG = "APP_SHARE_TAG";
    public static final String FB_PACKAGE_NAME = "com.facebook.orca";
    public static final String WAPP_PACKAGE_NAME = "com.whatsapp";
    public static final String OTHER = "OTHER";


    private static final String TAG = SharingService.class.getName();

    public SharingService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent1) {
        String appToShare = intent1.getExtras().getString(APP_SHARE_TAG);
        TaskModel task = (TaskModel) intent1.getExtras().get(TASK_TAG);
        Intent i = appToShare.equals(OTHER) ? ShareUtils.findOtherClients(getApplicationContext(), task) : share(task, appToShare);
        if(i!=null) {
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
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
}
