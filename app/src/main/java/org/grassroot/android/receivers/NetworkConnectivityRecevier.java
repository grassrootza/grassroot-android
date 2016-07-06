package org.grassroot.android.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import io.realm.Realm;
import io.realm.RealmResults;
import java.util.HashSet;
import org.grassroot.android.events.GroupCreatedEvent;
import org.grassroot.android.interfaces.TaskConstants;
import org.grassroot.android.models.Group;
import org.grassroot.android.models.GroupResponse;
import org.grassroot.android.models.Member;
import org.grassroot.android.models.TaskModel;
import org.grassroot.android.models.TaskResponse;
import org.grassroot.android.services.GrassrootRestService;
import org.grassroot.android.utils.NetworkUtils;
import org.grassroot.android.utils.PreferenceUtils;
import org.greenrobot.eventbus.EventBus;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NetworkConnectivityRecevier extends BroadcastReceiver {

  @Override public void onReceive(final Context context, Intent intent) {
    if (NetworkUtils.isNetworkAvailable(context)) {
      try {
        Thread.sleep(3000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
      final Realm realm = Realm.getDefaultInstance();
      RealmResults<Group> list = realm.where(Group.class).equalTo("isLocal", true).findAll();
      for (final Group g : list) {
        final RealmResults<Member> members =
            realm.where(Member.class).equalTo("groupUid", g.getGroupUid()).findAll();
        GrassrootRestService.getInstance()
            .getApi()
            .createGroup(PreferenceUtils.getPhoneNumber(), PreferenceUtils.getAuthToken(),
                g.getGroupName(), g.getDescription(), members)
            .enqueue(new Callback<GroupResponse>() {
              @Override
              public void onResponse(Call<GroupResponse> call, Response<GroupResponse> response) {
                if (response.isSuccessful()) {
                  PreferenceUtils.setUserHasGroups(context, true);
                  realm.beginTransaction();
                  realm.copyToRealmOrUpdate(response.body().getGroups().first());
                  g.deleteFromRealm();
                  members.deleteAllFromRealm();
                  realm.commitTransaction();
                  Log.d("tag", "returning group created! with UID : " + response.body()
                      .getGroups()
                      .get(0)
                      .getGroupUid());
                  EventBus.getDefault().post(new GroupCreatedEvent());
                } else {

                }
              }

              @Override public void onFailure(Call<GroupResponse> call, Throwable t) {
              }
            });
      }
      final RealmResults<TaskModel> tasks = realm.where(TaskModel.class).equalTo("isLocal", true).findAll();
      for(final TaskModel model : tasks){
      setUpApiCall(model,context).enqueue(new Callback<TaskResponse>() {
        @Override public void onResponse(Call<TaskResponse> call, Response<TaskResponse> response) {
          realm.beginTransaction();
          realm.copyToRealmOrUpdate(response.body().getTasks().get(0));
          model.deleteFromRealm();
          realm.commitTransaction();
          System.out.println("TASK CREATED" + response.body().getTasks().get(0).toString());
        }

        @Override public void onFailure(Call<TaskResponse> call, Throwable t) {

        }
      });
      }

    }
  }

  public Call<TaskResponse> setUpApiCall(TaskModel model,Context context) {
    final String phoneNumber = PreferenceUtils.getUserPhoneNumber(context);
    final String code = PreferenceUtils.getAuthToken(context);

    switch (model.getType()) {
      case TaskConstants.MEETING:
        return GrassrootRestService.getInstance()
            .getApi()
            .createMeeting(phoneNumber, code, model.getParentUid(), model.getTitle(), model.getDescription(),
                model.getDeadlineISO(), model.getMinutes(), model.getLocation(), new HashSet<String>());
      case TaskConstants.VOTE:
        return GrassrootRestService.getInstance()
            .getApi()
            .createVote(phoneNumber, code, model.getParentUid(), model.getTitle(), model.getDescription(),
                model.getDeadlineISO(), model.getMinutes(), new HashSet<String>(), false);
      case TaskConstants.TODO:
        return GrassrootRestService.getInstance()
            .getApi()
            .createTodo(phoneNumber, code, model.getParentUid(), model.getTitle(), model.getDescription(),
                model.getDeadlineISO(), model.getMinutes(), new HashSet<String>());
      default:
        throw new UnsupportedOperationException("Error! Missing task type in call");
    }
  }
}
