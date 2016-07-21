package org.grassroot.android.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.grassroot.android.models.Group;
import org.grassroot.android.models.GroupResponse;
import org.grassroot.android.models.Member;
import org.grassroot.android.models.TaskModel;
import org.grassroot.android.services.GrassrootRestService;
import org.grassroot.android.services.GroupService;
import org.grassroot.android.services.TaskService;
import org.grassroot.android.utils.NetworkUtils;
import org.grassroot.android.utils.RealmUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.realm.RealmList;
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

      sendLocalGroups(context);
      sendLocallyAddedMembers();
      sendNewLocalTasks();
      sendEditedTasks();
    }
  }

  private void sendLocalGroups(final Context context) {
    RealmList<Group> list = RealmUtils.loadListFromDB(Group.class, "isLocal", true);
    for (final Group g : list) {
      GroupService.getInstance().sendNewGroupToServer(g.getGroupUid(), context);
    }
  }

  // todo : maybe use a boolean, locallyEdited, to optimize this
  private void sendLocallyAddedMembers() {
    Map<String, Object> queryMap = new HashMap<>();
    queryMap.put("isLocal", true);
    RealmList<Group> groups = RealmUtils.loadListFromDB(Group.class);
    for (final Group g : groups) {
      queryMap.put("groupUid", g.getGroupUid());
      final RealmList<Member> addedMembers = RealmUtils.loadListFromDB(Member.class, queryMap);
      if (addedMembers.size() > 0) {
        GroupService.getInstance().postNewGroupMembers(addedMembers, g.getGroupUid());
      }
    }
  }

  private void sendNewLocalTasks() {
    Map<String, Object> map = new HashMap<>();
    map.put("isLocal", true);
    map.put("isEdited", false);
    map.put("isParentLocal", false);
    final RealmList<TaskModel> tasks = RealmUtils.loadListFromDB(TaskModel.class, map);
    for (final TaskModel model : tasks) {
      final String localUid = model.getTaskUid();
      TaskService.getInstance().sendNewTaskToServer(model, new TaskService.TaskCreationListener() {
        @Override
        public void taskCreatedLocally(TaskModel task) {
          RealmUtils.saveDataToRealm(task);
          RealmUtils.removeObjectFromDatabase(TaskModel.class, "taskUid", localUid);
          System.out.println("TASK CREATED" + task.toString());
        }

        @Override
        public void taskCreatedOnServer(TaskModel task) {

        }

        @Override
        public void taskCreationError(TaskModel task) {

        }
      });
    }
  }

  private void sendEditedTasks() {
    Map<String, Object> map1 = new HashMap<>();
    map1.put("isLocal", true);
    map1.put("isEdited", true);
    map1.put("isParentLocal", false);
    final RealmList<TaskModel> tasks1 = RealmUtils.loadListFromDB(TaskModel.class, map1);
    for (final TaskModel model : tasks1) {
      TaskService.getInstance().sendTaskUpdateToServer(model, true); // todo : work out selected member change logic
    }
  }

}