package org.grassroot.android.services;

import android.text.TextUtils;
import android.util.Log;

import org.grassroot.android.events.TasksRefreshedEvent;
import org.grassroot.android.interfaces.TaskConstants;
import org.grassroot.android.models.Group;
import org.grassroot.android.models.TaskChangedResponse;
import org.grassroot.android.models.TaskModel;
import org.grassroot.android.models.TaskResponse;
import org.grassroot.android.utils.NetworkUtils;
import org.grassroot.android.utils.RealmUtils;
import org.grassroot.android.utils.Utilities;
import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import io.realm.RealmList;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Created by luke on 2016/07/06.
 */
public class TaskService {

  private static final String TAG = TaskService.class.getSimpleName();

  public static final String FETCH_OKAY = "fetch";
  public static final String FETCH_ERROR = "fetch_error";
  public static final String FETCH_OFFLINE = "offline_fetch";

  public ArrayList<TaskModel> upcomingTasks;
  public boolean hasLoadedTasks;

  private static TaskService instance;

  public interface TaskServiceListener {
    void taskFetchingComplete(String fetchType, Object data);
  }

  public interface TaskCreationListener {
    void taskCreatedLocally(TaskModel task);
    void taskCreatedOnServer(TaskModel task);
    void taskCreationError(TaskModel task);
  }

  protected TaskService() {
    upcomingTasks = new ArrayList<>();
    hasLoadedTasks = false;
  }

  public static TaskService getInstance() {
    TaskService methodInstance = instance;
    if (methodInstance == null) {
      synchronized (TaskService.class) {
        methodInstance = instance;
        if (methodInstance == null) {
          instance = methodInstance = new TaskService();
        }
      }
    }
    return methodInstance;
  }

  public void fetchTasks(final String parentUid, final TaskServiceListener listener) {
    if (TextUtils.isEmpty(parentUid)) {
      fetchUpcomingTasks(listener);
    } else {
      fetchGroupTasks(parentUid, listener);
    }
  }

  public void fetchUpcomingTasks(final TaskServiceListener listener) {
    final String mobile = RealmUtils.loadPreferencesFromDB().getMobileNumber();
    final String code = RealmUtils.loadPreferencesFromDB().getToken();
    GrassrootRestService.getInstance()
        .getApi()
        .getUserTasks(mobile, code)
        .enqueue(new Callback<TaskResponse>() {
          @Override
          public void onResponse(Call<TaskResponse> call, Response<TaskResponse> response) {
            if (response.isSuccessful()) {
              upcomingTasks = new ArrayList<>(response.body().getTasks());
              for (TaskModel task : upcomingTasks) {
                task.getDeadlineDate(); // triggers processing & store of Date object (maybe move into a JSON converter)
              }
              RealmUtils.saveDataToRealm(upcomingTasks);
              if (listener != null) {
                listener.taskFetchingComplete(FETCH_OKAY, null);
              }
              EventBus.getDefault().post(new TasksRefreshedEvent());
            } else {
              if (listener != null) {
                listener.taskFetchingComplete(FETCH_ERROR, response);
              }
            }
          }

          @Override public void onFailure(Call<TaskResponse> call, Throwable t) {
            if (listener != null) {
              listener.taskFetchingComplete(FETCH_ERROR, t);
            }
          }
        });
  }

  public void fetchGroupTasks(final String groupUid, final TaskServiceListener listener) {
    final String phoneNumber = RealmUtils.loadPreferencesFromDB().getMobileNumber();
    final String code = RealmUtils.loadPreferencesFromDB().getToken();
    Group group = RealmUtils.loadObjectFromDB(Group.class, "groupUid", groupUid);

    Call<TaskChangedResponse> call;
    if (group.getLastTimeTasksFetched() == null) {
      call = GrassrootRestService.getInstance().getApi().getGroupTasks(phoneNumber, code, groupUid);
    } else {
      call = GrassrootRestService.getInstance().getApi()
              .getGroupTasksChangedSince(phoneNumber, code, groupUid, Long.valueOf(group.getLastTimeTasksFetched()));
    }

    call.enqueue(new Callback<TaskChangedResponse>() {
      @Override public void onResponse(Call<TaskChangedResponse> call, Response<TaskChangedResponse> response) {
        if (response.isSuccessful()) {
          System.out.println(Thread.currentThread().getName());
          updateAndRemoveTasks(response.body(), groupUid);
          listener.taskFetchingComplete(FETCH_OKAY, null);
        } else {
          listener.taskFetchingComplete(FETCH_ERROR, response);
        }
      }

      @Override public void onFailure(Call<TaskChangedResponse> call, Throwable t) {
        listener.taskFetchingComplete(FETCH_ERROR, t);
      }
    });
  }

  private void updateAndRemoveTasks(final TaskChangedResponse responseBody, final String groupUid) {
    try {
      RealmUtils.saveDataToRealm(responseBody.getAddedAndUpdated());
      RealmUtils.removeObjectsByUid(TaskModel.class, "taskUid", responseBody.getRemovedUids());
      updateTasksFetchedTime(groupUid);
    } catch (Exception e) {
      Log.e(TAG, "exception in realm saveGroupIfNamed ... not updating last time fetched ...");
      e.printStackTrace();
    }
  }

  private void updateTasksFetchedTime(String parentUid) {
    Group group = RealmUtils.loadObjectFromDB(Group.class, "groupUid", parentUid);
    group.setLastTimeTasksFetched(String.valueOf(Utilities.getCurrentTimeInMillisAtUTC()));
    group.setFetchedTasks(true);
    RealmUtils.saveGroupToRealm(group);
    Log.e(TAG, "group last time fetched after update: " + group.getLastTimeTasksFetched());
  }


  public void createTask(final TaskModel task, final TaskCreationListener listener) {
    if (NetworkUtils.isOnline(ApplicationLoader.applicationContext)) {
      newTaskApiCall(task).enqueue(new Callback<TaskResponse>() {
        @Override public void onResponse(Call<TaskResponse> call, Response<TaskResponse> response) {
          if (response.isSuccessful()) {
            final TaskModel taskFromServer = response.body().getTasks().get(0);
            taskFromServer.getDeadlineDate(); // trigger forming Date entity, as above, create custom converter
            RealmUtils.saveDataToRealm(taskFromServer);
            listener.taskCreatedOnServer(taskFromServer);
          } else {
            RealmUtils.saveDataToRealm(task);
            listener.taskCreationError(task);
          }
        }

        @Override public void onFailure(Call<TaskResponse> call, Throwable t) {
          Log.e(TAG, "Error! Should not occur ... check Network Utils");
          RealmUtils.saveDataToRealm(task);
          listener.taskCreatedLocally(task);
        }
      });
    } else {
      RealmUtils.saveDataToRealm(task);
      listener.taskCreatedLocally(task);
    }
  }

  // todo : figure out DB replace etc logic here (currently won't save until next explicit fetch)
  public void sendNewTaskToServer(final TaskModel model, final TaskCreationListener listener) {
    newTaskApiCall(model).enqueue(new Callback<TaskResponse>() {
      @Override public void onResponse(Call<TaskResponse> call, Response<TaskResponse> response) {
        Log.d(TAG, response.body().getTasks().get(0).toString());
        if (listener != null) {
          listener.taskCreatedLocally(response.body().getTasks().get(0));
        }
      }

      @Override public void onFailure(Call<TaskResponse> call, Throwable t) {
        t.printStackTrace();
        if (listener != null) {
          listener.taskCreationError(model);
        }
      }
    });

  }

  private Call<TaskResponse> newTaskApiCall(TaskModel task) {
    final String phoneNumber = RealmUtils.loadPreferencesFromDB().getMobileNumber();
    final String code = RealmUtils.loadPreferencesFromDB().getToken();

    switch (task.getType()) {
      case TaskConstants.MEETING:
        final String location = task.getLocation();
        return GrassrootRestService.getInstance()
            .getApi()
            .createMeeting(phoneNumber, code, task.getParentUid(), task.getTitle(),
                task.getDescription(), task.getDeadlineISO(), task.getMinutes(), location,
                new HashSet<>(
                    RealmUtils.convertListOfRealmStringInListOfString(task.getMemberUIDS())));
      case TaskConstants.VOTE:
        return GrassrootRestService.getInstance()
            .getApi()
            .createVote(phoneNumber, code, task.getParentUid(), task.getTitle(),
                task.getDescription(), task.getDeadlineISO(), task.getMinutes(), new HashSet<>(
                    RealmUtils.convertListOfRealmStringInListOfString(task.getMemberUIDS())),
                false);
      case TaskConstants.TODO:
        return GrassrootRestService.getInstance()
            .getApi()
            .createTodo(phoneNumber, code, task.getParentUid(), task.getTitle(),
                task.getDescription(), task.getDeadlineISO(), task.getMinutes(), new HashSet<>(
                    RealmUtils.convertListOfRealmStringInListOfString(task.getMemberUIDS())));
      default:
        throw new UnsupportedOperationException("Error! Missing task type in call");
    }
  }

  public void sendTaskUpdateToServer(final TaskModel taskModel, boolean selectedMembersChanged) {
    updateTaskApiCall(taskModel, selectedMembersChanged).enqueue(new Callback<TaskModel>() {
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

  private Call<TaskModel> updateTaskApiCall(TaskModel model, boolean selectedMembersChanged) {
    List<String> memberUids = selectedMembersChanged ? model.getMemberUIDS() : Collections.EMPTY_LIST;
    final String phoneNumber = RealmUtils.loadPreferencesFromDB().getMobileNumber();
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


  /*
  FETCH A TASK AND STORE IT LOCALLY (FOR USE IN BACKGROUND WHEN GET & VIEW NOTIFICATION)
   */
  public void fetchAndStoreTask(final String taskUid, final String taskType) {
    final String phoneNumber = RealmUtils.loadPreferencesFromDB().getMobileNumber();
    final String code = RealmUtils.loadPreferencesFromDB().getToken();
    if (NetworkUtils.isOnline()) {
      GrassrootRestService.getInstance().getApi().fetchTaskEntity(phoneNumber, code, taskUid, taskType)
          .enqueue(new Callback<TaskResponse>() {
            @Override
            public void onResponse(Call<TaskResponse> call, Response<TaskResponse> response) {
              if (response.isSuccessful()) {
                final TaskModel taskModel = response.body().getTasks().first();
                if (taskModel != null) {
                  RealmUtils.saveDataToRealm(taskModel);
                }
              }
            }

            @Override
            public void onFailure(Call<TaskResponse> call, Throwable t) {

            }
          });
    }
  }

}
