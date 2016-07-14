package org.grassroot.android.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import io.realm.RealmList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.grassroot.android.interfaces.TaskConstants;
import org.grassroot.android.models.Group;
import org.grassroot.android.models.GroupResponse;
import org.grassroot.android.models.Member;
import org.grassroot.android.models.PreferenceObject;
import org.grassroot.android.models.TaskModel;
import org.grassroot.android.models.TaskResponse;
import org.grassroot.android.services.GrassrootRestService;
import org.grassroot.android.utils.NetworkUtils;
import org.grassroot.android.utils.RealmUtils;
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
      RealmList<Group> list = RealmUtils.loadListFromDB(Group.class, "isLocal", true);
      final String phoneNumber =
          RealmUtils.loadPreferencesFromDB().getMobileNumber();
      final String code = RealmUtils.loadPreferencesFromDB().getToken();
      for (final Group g : list) {
        final RealmList<Member> members =
            RealmUtils.loadListFromDB(Member.class, "groupUid", g.getGroupUid());
        GrassrootRestService.getInstance()
            .getApi()
            .createGroup(phoneNumber, code,
                g.getGroupName(), g.getDescription(), members)
            .enqueue(new Callback<GroupResponse>() {
              @Override
              public void onResponse(Call<GroupResponse> call, Response<GroupResponse> response) {
                if (response.isSuccessful()) {
                  PreferenceObject preferenceObject = RealmUtils.loadPreferencesFromDB();
                  preferenceObject.setHasGroups(true);
                  RealmUtils.saveDataToRealm(preferenceObject);
                  RealmUtils.saveDataToRealm(response.body().getGroups().first());
                  //sure local, edited or not, same result --> POST to create
                  RealmList<TaskModel> models =
                      RealmUtils.loadListFromDB(TaskModel.class, "parentUid", g.getGroupUid());
                  for (int i = 0; i < models.size(); i++) {
                    models.get(i).setParentUid(response.body().getGroups().first().getGroupUid());
                    setUpApiCall(models.get(i), context).enqueue(new Callback<TaskResponse>() {
                      @Override public void onResponse(Call<TaskResponse> call,
                          Response<TaskResponse> response) {
                        System.out.println(response.body().getTasks().get(0).toString());
                      }

                      @Override public void onFailure(Call<TaskResponse> call, Throwable t) {
                        t.printStackTrace();
                      }
                    });
                  }
                  RealmUtils.removeObjectFromDatabase(Group.class, "groupUid", g.getGroupUid());

                  for (Member member : members) {
                    RealmUtils.removeObjectFromDatabase(Member.class, "groupUid",
                        member.getGroupUid());
                  }
                  Log.d("tag", "returning group created! with UID : " + response.body()
                      .getGroups()
                      .get(0)
                      .getGroupUid());
                } else {

                }
              }

              @Override public void onFailure(Call<GroupResponse> call, Throwable t) {
              }
            });
      }
      Map<String, Object> map = new HashMap<>();
      map.put("isLocal", true);
      map.put("isEdited", false);
      map.put("isParentLocal", false);
      final RealmList<TaskModel> tasks = RealmUtils.loadListFromDB(TaskModel.class, map);
      for (final TaskModel model : tasks) {
        setUpApiCall(model, context).enqueue(new Callback<TaskResponse>() {
          @Override
          public void onResponse(Call<TaskResponse> call, Response<TaskResponse> response) {
            RealmUtils.saveDataToRealm(response.body().getTasks().get(0));
            RealmUtils.removeObjectFromDatabase(TaskModel.class, "taskUid", model.getTaskUid());
            System.out.println("TASK CREATED" + response.body().getTasks().get(0).toString());
          }

          @Override public void onFailure(Call<TaskResponse> call, Throwable t) {

          }
        });
      }
      Map<String, Object> map1 = new HashMap<>();
      map1.put("isLocal", true);
      map1.put("isEdited", true);
      map1.put("isParentLocal", false);
      final RealmList<TaskModel> tasks1 = RealmUtils.loadListFromDB(TaskModel.class, map1);
      for (final TaskModel model : tasks1) {
        setUpUpdateApiCall(model).enqueue(new Callback<TaskModel>() {
          @Override public void onResponse(Call<TaskModel> call, Response<TaskModel> response) {
            RealmUtils.saveDataToRealm(response.body());
            //RealmUtils.removeObjectFromDatabase(TaskModel.class,"taskUid",model.getTaskUid());
            System.out.println("TASK edited" + response.body().toString());
          }

          @Override public void onFailure(Call<TaskModel> call, Throwable t) {
            t.printStackTrace();
          }
        });
      }

      Map<String, Object> map2 = new HashMap<>();
      RealmList<Group> groups = RealmUtils.loadListFromDB(Group.class);
      for (final Group g : groups) {
        map2.put("isLocal", true);
        map2.put("groupUid", g.getGroupUid());
        final RealmList<Member> tasks2 = RealmUtils.loadListFromDB(Member.class, map2);
        if (tasks2.size() > 0) postNewMembersToGroup(tasks2, g.getGroupUid());
      }
    }
  }

  public Call<TaskResponse> setUpApiCall(TaskModel model, Context context) {
    final String phoneNumber =
        RealmUtils.loadPreferencesFromDB().getMobileNumber();
    final String code = RealmUtils.loadPreferencesFromDB().getToken();

    switch (model.getType()) {
      case TaskConstants.MEETING:
        return GrassrootRestService.getInstance()
            .getApi()
            .createMeeting(phoneNumber, code, model.getParentUid(), model.getTitle(),
                model.getDescription(), model.getDeadlineISO(), model.getMinutes(),
                model.getLocation(), new HashSet<>(
                    RealmUtils.convertListOfRealmStringInListOfString(model.getMemberUIDS())));
      case TaskConstants.VOTE:
        return GrassrootRestService.getInstance()
            .getApi()
            .createVote(phoneNumber, code, model.getParentUid(), model.getTitle(),
                model.getDescription(), model.getDeadlineISO(), model.getMinutes(), new HashSet<>(
                    RealmUtils.convertListOfRealmStringInListOfString(model.getMemberUIDS())),
                false);
      case TaskConstants.TODO:
        return GrassrootRestService.getInstance()
            .getApi()
            .createTodo(phoneNumber, code, model.getParentUid(), model.getTitle(),
                model.getDescription(), model.getDeadlineISO(), model.getMinutes(), new HashSet<>(
                    RealmUtils.convertListOfRealmStringInListOfString(model.getMemberUIDS())));
      default:
        throw new UnsupportedOperationException("Error! Missing task type in call");
    }
  }

  public Call<TaskModel> setUpUpdateApiCall(TaskModel model) {
    Set<String> memberUids = Collections.EMPTY_SET;
    final String phoneNumber =
        RealmUtils.loadPreferencesFromDB().getMobileNumber();
    final String code = RealmUtils.loadPreferencesFromDB().getToken();
    switch (model.getType()) {
      case TaskConstants.MEETING:
        return GrassrootRestService.getInstance()
            .getApi()
            .editMeeting(phoneNumber, code, model.getTaskUid(), model.getTitle(),
                model.getDescription(), model.getLocation(), model.getDeadlineISO(), memberUids);
      case TaskConstants.VOTE:
        return GrassrootRestService.getInstance()
            .getApi()
            .editVote(phoneNumber, code, model.getTaskUid(), model.getTitle(),
                model.getDescription(), model.getDeadlineISO());
      case TaskConstants.TODO:
        return GrassrootRestService.getInstance()
            .getApi()
            .editTodo(phoneNumber, code, model.getTitle(), model.getDeadlineISO(), null);
      default:
        throw new UnsupportedOperationException("Error! Missing task type in call");
    }
  }

  private void postNewMembersToGroup(final List<Member> membersToAdd, String groupUid) {
    final String mobileNumber =
        RealmUtils.loadPreferencesFromDB().getMobileNumber();
    final String sessionCode = RealmUtils.loadPreferencesFromDB().getToken();
    GrassrootRestService.getInstance()
        .getApi()
        .addGroupMembers(groupUid, mobileNumber, sessionCode, membersToAdd)
        .enqueue(new Callback<GroupResponse>() {
          @Override
          public void onResponse(Call<GroupResponse> call, Response<GroupResponse> response) {
            if (response.isSuccessful()) {
              // todo : maybe, maybe a progress dialog
              //todo return members here from API
              Map<String, Object> map2 = new HashMap<>();
              map2.put("isLocal", true);
              map2.put("groupUid", membersToAdd.get(0).getGroupUid());
              RealmUtils.removeObjectsFromDatabase(Member.class,map2);
              RealmUtils.saveDataToRealm(response.body().getGroups());
            } else {
            }
          }

          @Override public void onFailure(Call<GroupResponse> call, Throwable t) {
          }
        });
  }
}
