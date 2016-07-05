package org.grassroot.android.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.design.widget.Snackbar;
import android.util.Log;
import io.realm.Realm;
import io.realm.RealmList;
import io.realm.RealmResults;
import java.util.ArrayList;
import org.grassroot.android.R;
import org.grassroot.android.events.GroupCreatedEvent;
import org.grassroot.android.interfaces.GroupConstants;
import org.grassroot.android.models.Group;
import org.grassroot.android.models.GroupResponse;
import org.grassroot.android.models.Member;
import org.grassroot.android.services.GrassrootRestService;
import org.grassroot.android.utils.ErrorUtils;
import org.grassroot.android.utils.NetworkUtils;
import org.grassroot.android.utils.PreferenceUtils;
import org.greenrobot.eventbus.EventBus;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NetworkConnectivityRecevier extends BroadcastReceiver {


  @Override public void onReceive(final Context context, Intent intent) {
    if(NetworkUtils.isNetworkAvailable(context)) {
      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
      final Realm realm = Realm.getDefaultInstance();
      RealmResults<Group> list = realm.where(Group.class).equalTo("isLocal",true).findAll();
      for(final Group g : list){
        GrassrootRestService.getInstance().getApi()
            .createGroup(PreferenceUtils.getPhoneNumber(), PreferenceUtils.getAuthToken(), g.getGroupName(), g.getDescription(), new ArrayList<Member>())
            .enqueue(new Callback<GroupResponse>() {
              @Override
              public void onResponse(
                  Call<GroupResponse> call, Response<GroupResponse> response) {
                if (response.isSuccessful()) {
                  PreferenceUtils.setUserHasGroups(context, true);
                  realm.beginTransaction();
                  realm.copyToRealmOrUpdate(response.body().getGroups().first());
                  g.deleteFromRealm();
                  realm.commitTransaction();
                  realm.close();
                  Log.d("tag","returning group created! with UID : " + response.body().getGroups().get(0).getGroupUid());
                  EventBus.getDefault().post(new GroupCreatedEvent());
                } else {

                }
              }

              @Override
              public void onFailure(Call<GroupResponse> call, Throwable t) {
              }
            });
      }
    }
  }
}
