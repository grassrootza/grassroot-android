package org.grassroot.android.services;

import android.text.TextUtils;
import android.util.Log;
import io.realm.Realm;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import org.grassroot.android.events.TaskUpdatedEvent;
import org.grassroot.android.events.TasksRefreshedEvent;
import org.grassroot.android.interfaces.TaskConstants;
import org.grassroot.android.models.Group;
import org.grassroot.android.models.PreferenceObject;
import org.grassroot.android.models.TaskChangedResponse;
import org.grassroot.android.models.TaskModel;
import org.grassroot.android.models.TaskResponse;
import org.grassroot.android.utils.NetworkUtils;
import org.grassroot.android.utils.RealmUtils;
import org.grassroot.android.utils.Utilities;
import org.greenrobot.eventbus.EventBus;

import io.realm.RealmList;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import rx.functions.Action1;

/**
 * Created by luke on 2016/07/06.
 */
public class TaskService {

  private static final String TAG = TaskService.class.getSimpleName();

  public static final String FETCH_OKAY = "FETCH_OKAY";
  public static final String FETCH_ERROR = "FETCH_ERROR";
  public static final String QUICK_DB_LOAD = "QUICK_DB_LOAD";

  private static TaskService instance;

  public interface TaskServiceListener {
    void taskFetchingComplete(String fetchType, Object data);
  }

  public interface TaskActionListener {
    void taskActionComplete(TaskModel task, String reply);
    void taskActionError(Response<TaskResponse> response);
    void taskActionCompleteOffline(TaskModel task, String reply);
  }

  public interface TaskCreationListener {
    void taskCreatedLocally(TaskModel task);

    void taskCreatedOnServer(TaskModel task);

    void taskCreationError(TaskModel task);
  }

  protected TaskService() { }

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
    final String phoneNumber = RealmUtils.loadPreferencesFromDB().getMobileNumber();
    final String code = RealmUtils.loadPreferencesFromDB().getToken();
    long lastTimeUpdated = RealmUtils.loadPreferencesFromDB().getLastTimeUpcomingTasksFetched();
    Log.e(TAG, "fetching upcoming tasks, last time checked: " + lastTimeUpdated);
    if (lastTimeUpdated == 0) {
      fetchUpcomingTasksForFirstTime(phoneNumber, code, listener);
    } else {
      fetchUpcomingTasksAndCheckForCancelled(lastTimeUpdated, phoneNumber, code, listener);
    }
  }

  private void fetchUpcomingTasksForFirstTime(final String mobile, final String code,
                                              final TaskServiceListener listener) {
    GrassrootRestService.getInstance()
        .getApi()
        .getUserTasks(mobile, code)
        .enqueue(new Callback<TaskResponse>() {
          @Override
          public void onResponse(Call<TaskResponse> call, Response<TaskResponse> response) {
            if (response.isSuccessful()) {
              updateTasksFetchedTime(null);
              persistUpcomingTasks(response.body().getTasks(), listener);
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

  private void fetchUpcomingTasksAndCheckForCancelled(long changedSince, final String mobile, final String code,
                                                      final TaskServiceListener listener) {

    GrassrootRestService.getInstance().getApi().getUpcomingTasksAndCancellations(mobile, code, changedSince)
        .enqueue(new Callback<TaskChangedResponse>() {
          @Override
          public void onResponse(Call<TaskChangedResponse> call, Response<TaskChangedResponse> response) {
            if (response.isSuccessful()) {
              updateTasksFetchedTime(null);
              RealmUtils.removeObjectsByUid(TaskModel.class, "taskUid", response.body().getRemovedUids());
              persistUpcomingTasks(response.body().getAddedAndUpdated(), listener);
            } else {
              if (listener != null) {
                listener.taskFetchingComplete(FETCH_ERROR, response);
              }
            }
          }

          @Override
          public void onFailure(Call<TaskChangedResponse> call, Throwable t) {
            if (listener != null) {
              listener.taskFetchingComplete(FETCH_ERROR, t);
            }
          }
        });

  }

  private void persistUpcomingTasks(RealmList<TaskModel> tasks, final TaskServiceListener listener) {
    for (TaskModel task : tasks) {
      task.getDeadlineDate(); // triggers processing & store of Date object (maybe move into a JSON converter)
    }
    RealmUtils.saveDataToRealm(tasks).subscribe(new Action1() {
      @Override
      public void call(Object o) {
        if (listener != null) {
          listener.taskFetchingComplete(FETCH_OKAY, null);
        }
        EventBus.getDefault().post(new TasksRefreshedEvent());
      }
    });
  }

  public void fetchGroupTasks(final String groupUid, final TaskServiceListener listener) {
    final String phoneNumber = RealmUtils.loadPreferencesFromDB().getMobileNumber();
    final String code = RealmUtils.loadPreferencesFromDB().getToken();
    Group group = RealmUtils.loadObjectFromDB(Group.class, "groupUid", groupUid);

    Call<TaskChangedResponse> call;
    if (group == null || group.getLastTimeTasksFetched() == null) {
      call = GrassrootRestService.getInstance().getApi().getGroupTasks(phoneNumber, code, groupUid);
    } else {
      call = GrassrootRestService.getInstance()
          .getApi()
          .getGroupTasksChangedSince(phoneNumber, code, groupUid,
              Long.valueOf(group.getLastTimeTasksFetched()));
    }

    call.enqueue(new Callback<TaskChangedResponse>() {
      @Override public void onResponse(Call<TaskChangedResponse> call,
          Response<TaskChangedResponse> response) {
        if (response.isSuccessful()) {
          updateAndRemoveTasks(response.body(), groupUid, listener);
        } else {
          listener.taskFetchingComplete(FETCH_ERROR, response);
        }
      }

      @Override public void onFailure(Call<TaskChangedResponse> call, Throwable t) {
        RealmUtils.loadListFromDB(TaskModel.class, "parentUid", groupUid)
            .subscribe(new Action1<List<TaskModel>>() {
              @Override public void call(List<TaskModel> realmResults) {
                listener.taskFetchingComplete(FETCH_ERROR,realmResults);
              }
            });
      }
    });
  }

  private void updateAndRemoveTasks(final TaskChangedResponse responseBody, final String groupUid,
      final TaskServiceListener listener) {

    try {
      RealmUtils.saveDataToRealm(responseBody.getAddedAndUpdated()).subscribe(new Action1() {
        @Override public void call(Object o) {
          System.out.println(responseBody.getAddedAndUpdated().size());
          RealmUtils.removeObjectsByUid(TaskModel.class, "taskUid", responseBody.getRemovedUids());
          RealmUtils.loadListFromDB(TaskModel.class, "parentUid", groupUid)
              .subscribe(new Action1<List<TaskModel>>() {
                @Override public void call(List<TaskModel> realmResults) {
                  System.out.println(realmResults.size());
                  listener.taskFetchingComplete(FETCH_OKAY,realmResults);
                  updateTasksFetchedTime(groupUid);
                }
              });
        }
      });
    } catch (Exception e) {
      Log.e(TAG, "exception in realm saveGroupIfNamed ... not updating last time fetched ...");
      e.printStackTrace();
    }
  }

  private void updateTasksFetchedTime(String parentUid) {
    if (!TextUtils.isEmpty(parentUid)) {
      Group group = RealmUtils.loadObjectFromDB(Group.class, "groupUid", parentUid);
      if (group != null) {
        group.setLastTimeTasksFetched(String.valueOf(Utilities.getCurrentTimeInMillisAtUTC()));
        group.setFetchedTasks(true);
        RealmUtils.saveGroupToRealm(group);
      }
    } else {
      PreferenceObject prefs = RealmUtils.loadPreferencesFromDB();
      prefs.setLastTimeUpcomingTasksFetched(Utilities.getCurrentTimeInMillisAtUTC());
      RealmUtils.saveDataToRealm(prefs).subscribe();
    }
  }


  public void createTask(final TaskModel task, final TaskCreationListener listener) {
    if (NetworkUtils.isOnline(ApplicationLoader.applicationContext)) {
      newTaskApiCall(task).enqueue(new Callback<TaskResponse>() {
        @Override public void onResponse(Call<TaskResponse> call, Response<TaskResponse> response) {
          if (response.isSuccessful()) {
            final TaskModel taskFromServer = response.body().getTasks().get(0);
            taskFromServer.getDeadlineDate(); // trigger forming Date entity, as above, create custom converter
            RealmUtils.saveDataToRealmSync(taskFromServer);
            listener.taskCreatedOnServer(taskFromServer);
          } else {
            RealmUtils.saveDataToRealmSync(task);
            listener.taskCreationError(task);
          }
        }

        @Override public void onFailure(Call<TaskResponse> call, Throwable t) {
          Log.e(TAG, "Error! Should not occur ... check Network Utils");
          RealmUtils.saveDataToRealm(task).subscribe(new Action1() {
            @Override public void call(Object o) {
              System.out.println("task saved");
              listener.taskCreatedLocally(task);
            }
          });
        }
      });
    } else {
      RealmUtils.saveDataToRealm(task).subscribe(new Action1() {
        @Override public void call(Object o) {
          System.out.println("task saved");
          listener.taskCreatedLocally(task);
        }
      });
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
    updateTaskApiCall(taskModel, selectedMembersChanged).enqueue(new Callback<TaskResponse>() {
      @Override public void onResponse(Call<TaskResponse> call, final Response<TaskResponse> response) {
        RealmUtils.saveDataToRealm(response.body()).subscribe(new Action1() {
          @Override public void call(Object o) {
            System.out.println("TASK edited" + response.body().toString());
          }
        });
        //RealmUtils.removeObjectFromDatabase(TaskModel.class,"taskUid",model.getTaskUid());
      }

      @Override public void onFailure(Call<TaskResponse> call, Throwable t) {
        t.printStackTrace();
      }
    });
  }

  private Call<TaskResponse> updateTaskApiCall(TaskModel model, boolean selectedMembersChanged) {
    List<String> memberUids =
        selectedMembersChanged ? model.getMemberUIDS() : Collections.EMPTY_LIST;
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
                  RealmUtils.saveDataToRealm(taskModel).subscribe();
                }
              }
            }

            @Override
            public void onFailure(Call<TaskResponse> call, Throwable t) {

            }
          });
    }
  }
  public void respondToTask(final TaskModel task, final String type, final TaskActionListener listener){
    final String phoneNumber = RealmUtils.loadPreferencesFromDB().getMobileNumber();
    final String code = RealmUtils.loadPreferencesFromDB().getToken();
    Call<TaskResponse> call =
            task.getType().equals(TaskConstants.VOTE) ? voteCall(task.getTaskUid(),phoneNumber,code,type)
                    : meetingCall(task.getTaskUid(),phoneNumber,code,type);
    call.enqueue(new Callback<TaskResponse>() {
      @Override public void onResponse(Call<TaskResponse> call, Response<TaskResponse> response) {
        if (response.isSuccessful()) {
          listener.taskActionComplete(response.body().getTasks().first(),type);
          RealmUtils.saveDataToRealmSync(response.body().getTasks().first());
          EventBus.getDefault().post(new TaskUpdatedEvent(response.body().getTasks().first()));
        } else {
         listener.taskActionError(response);
        }
      }

      @Override public void onFailure(Call<TaskResponse> call, Throwable t) {
      listener.taskActionCompleteOffline(task,type);
        EventBus.getDefault().post(new TaskUpdatedEvent(task));
      }
    });
  }

  private Call<TaskResponse> voteCall(String taskUid, String phoneNumber, String code,String response) {
    return GrassrootRestService.getInstance()
            .getApi()
            .castVote(taskUid, phoneNumber, code, response);
  }

  private Call<TaskResponse> meetingCall(String taskUid, String phoneNumber, String code,String response) {
    return GrassrootRestService.getInstance().getApi().rsvp(taskUid, phoneNumber, code, response);
  }
}
