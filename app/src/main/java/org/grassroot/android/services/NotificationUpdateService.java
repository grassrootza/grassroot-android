package org.grassroot.android.services;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;

import org.grassroot.android.interfaces.NotificationConstants;
import org.grassroot.android.models.GenericResponse;
import org.grassroot.android.models.TaskNotification;
import org.grassroot.android.utils.RealmUtils;
import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import io.realm.Realm;
import io.realm.RealmQuery;
import io.realm.RealmResults;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Created by paballo on 2016/06/09.
 */
public class NotificationUpdateService extends IntentService {

    private static final String TAG = NotificationUpdateService.class.getCanonicalName();

    public static final String UPDATE_SINGLE = "update_single";
    public static final String UPDATE_BATCH = "update_batch";
    public static final String ACTION_FIELD = "action";

    public static final String UIDS_SET = "notification_uids";

    public NotificationUpdateService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        final String action = intent.getStringExtra(ACTION_FIELD);
        if (TextUtils.isEmpty(action) || UPDATE_SINGLE.equals(action)) {
            String uid = intent.getStringExtra(NotificationConstants.NOTIFICATION_UID);
            update(uid);
        } else if (UPDATE_BATCH.equals(action)) {
            ArrayList<String> uids = intent.getStringArrayListExtra(UIDS_SET);
            updateBatch(uids, true);
        }
    }

    private void update(final String notificationUid) {
      final String phoneNumber = RealmUtils.loadPreferencesFromDB().getMobileNumber();
      final String userToken = RealmUtils.loadPreferencesFromDB().getToken();

        GrassrootRestService.getInstance()
            .getApi()
            .updateRead(phoneNumber, userToken, notificationUid)
            .enqueue(new Callback<GenericResponse>() {
                    @Override
                    public void onResponse(Call<GenericResponse> call, Response<GenericResponse> response) {
                        if (response.isSuccessful()) {
                            Log.d(TAG, "updated notification with uid " + notificationUid + "to read status");
                        }
                    }

                    @Override
                    public void onFailure(Call<GenericResponse> call, Throwable t) {
                        Log.e(TAG, "network failure");
                    }
                });
    }

    public void updateBatch(final List<String> notificationUids, final boolean read) {
        final String phoneNumber = RealmUtils.loadPreferencesFromDB().getMobileNumber();
        final String code = RealmUtils.loadPreferencesFromDB().getToken();

        GrassrootRestService.getInstance()
            .getApi()
            .updateReadBatch(phoneNumber, code, read, notificationUids)
            .enqueue(new Callback<GenericResponse>() {
                @Override
                public void onResponse(Call<GenericResponse> call, Response<GenericResponse> response) {
                    if (response.isSuccessful()) {
                        // all worked on server, so clean up locally
                        setBatchToUpdateOnserver(notificationUids, read, false);
                    } else {
                        // didn't work on server, so don't do anything
                        Log.e(TAG, "error on server! could not change notification status");
                    }
                }

                @Override
                public void onFailure(Call<GenericResponse> call, Throwable t) {
                    setBatchToUpdateOnserver(notificationUids, read, true);
                }
            });
    }

    private void setBatchToUpdateOnserver(final List<String> notificationUids, final boolean isRead,
                                          final boolean toUpdateOnServer) {
        if (notificationUids != null && !notificationUids.isEmpty()) {
            Realm realm = Realm.getDefaultInstance();
            realm.executeTransactionAsync(new Realm.Transaction() {
                @Override
                public void execute(Realm realm) {
                    RealmQuery<TaskNotification> query = realm.where(TaskNotification.class);
                    final int number = notificationUids.size();
                    query = query.equalTo("uid", notificationUids.get(0));
                    for (int i = 1; i < number; i++) {
                        query = query.or().equalTo("uid", notificationUids.get(1));
                    }
                    RealmResults<TaskNotification> results = query.findAll();
                    for (int i = (results.size()-1); i >= 0; i--) {
                        results.get(i).setToChangeOnServer(toUpdateOnServer);
                        results.get(i).setViewedAndroid(isRead);
                    }
                }
            });
        }
    }

    public static void updateNotificationStatus(Context context, String notificationUid){
        Intent intent = new Intent(context,NotificationUpdateService.class);
        intent.putExtra(NotificationConstants.NOTIFICATION_UID, notificationUid);
        context.startService(intent);
    }

}



