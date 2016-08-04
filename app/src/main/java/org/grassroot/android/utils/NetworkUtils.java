package org.grassroot.android.utils;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.grassroot.android.events.ConnectionFailedEvent;
import org.grassroot.android.events.NetworkFailureEvent;
import org.grassroot.android.events.OfflineActionsSent;
import org.grassroot.android.events.OnlineOfflineToggledEvent;
import org.grassroot.android.interfaces.NotificationConstants;
import org.grassroot.android.models.GenericResponse;
import org.grassroot.android.models.Group;
import org.grassroot.android.models.LocalGroupEdits;
import org.grassroot.android.models.Member;
import org.grassroot.android.models.PreferenceObject;
import org.grassroot.android.models.TaskModel;
import org.grassroot.android.models.TaskResponse;
import org.grassroot.android.services.ApplicationLoader;
import org.grassroot.android.services.GcmRegistrationService;
import org.grassroot.android.services.GrassrootRestService;
import org.grassroot.android.services.GroupService;
import org.grassroot.android.services.LocationServices;
import org.grassroot.android.services.TaskService;
import org.greenrobot.eventbus.EventBus;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

public class NetworkUtils {

  private static final String TAG = NetworkUtils.class.getSimpleName();

  public static final String ONLINE_DEFAULT = "default";
  public static final String OFFLINE_SELECTED = "offline_selected"; // i.e., user chose to go offline
  public static final String OFFLINE_ON_FAIL = "offline_on_fail"; // i.e., network calls failed, but user said to keep trying

  public static final String SAVED_SERVER = "saved_server";
  public static final String SAVED_OFFLINE_MODE = "saved_offline_mode";
  public static final String SERVER_ERROR = "server_error";
  public static final String CONNECT_ERROR = "connection_error";

  public static final long minIntervalBetweenSyncs = 15 * 60 * 1000; // 15 minutes, in millis

  static boolean sendingLocalQueue = false;
  static boolean fetchingServerEntities = false;

  public interface NetworkListener {
    void connectionEstablished();
    void networkAvailableButConnectFailed(String failureType);
    void networkNotAvailable();
    void setOffline();
  }

  public static boolean isOnline() {
    return isOnline(ApplicationLoader.applicationContext);
  }

  public static void toggleOnlineOffline(final Context context, final boolean sendQueue,
      final NetworkListener listener) {
    final String currentStatus = RealmUtils.loadPreferencesFromDB().getOnlineStatus();
    Log.d(TAG, "toggling offline and online, from current status : " + currentStatus);
    if (ONLINE_DEFAULT.equals(currentStatus)) {
      switchToOfflineMode(listener);
    } else {
      trySwitchToOnline(context, sendQueue, listener);
    }
  }

  public static void setOnline() {
    PreferenceObject prefs = RealmUtils.loadPreferencesFromDB();
    prefs.setOnlineStatus(ONLINE_DEFAULT);
    RealmUtils.saveDataToRealm(prefs).subscribe(new Action1() {
      @Override
      public void call(Object o) {
        EventBus.getDefault().post(new OnlineOfflineToggledEvent(true));
      }
    });
  }

  public static void switchToOfflineMode(final NetworkListener listener) {
    PreferenceObject prefs = RealmUtils.loadPreferencesFromDB();
    prefs.setOnlineStatus(OFFLINE_SELECTED);
    prefs.setLastTimeSyncPerformed(0L);
    RealmUtils.saveDataToRealm(prefs).subscribe(new Action1() {
      @Override
      public void call(Object o) {
        EventBus.getDefault().post(new OnlineOfflineToggledEvent(false));
        if (listener != null) {
          listener.setOffline();
        }
      }
    });
  }

  public static void setOnlineFailed() {
    PreferenceObject prefs = RealmUtils.loadPreferencesFromDB();
    prefs.setOnlineStatus(OFFLINE_ON_FAIL);
    prefs.setLastTimeSyncPerformed(0L);
    RealmUtils.saveDataToRealm(prefs).subscribe(new Action1() {
      @Override public void call(Object o) {
        EventBus.getDefault().post(new NetworkFailureEvent());
      }
    });
  }

  public static void trySwitchToOnline(final Context context, final boolean sendQueue,
      final NetworkListener listener) {
    if (!isNetworkAvailable(context)) {
      listener.networkNotAvailable();
    } else {
      final String phoneNumber = RealmUtils.loadPreferencesFromDB().getMobileNumber();
      final String token = RealmUtils.loadPreferencesFromDB().getToken();
      GrassrootRestService.getInstance()
          .getApi()
          .testConnection(phoneNumber, token)
          .enqueue(new Callback<GenericResponse>() {
            @Override
            public void onResponse(Call<GenericResponse> call, Response<GenericResponse> response) {
              if (response.isSuccessful()) {
                PreferenceObject prefs = RealmUtils.loadPreferencesFromDB();
                prefs.setOnlineStatus(ONLINE_DEFAULT);
                RealmUtils.saveDataToRealm(prefs).subscribe(new Action1() {
                  @Override
                  public void call(Object o) {
                    if (listener != null) {
                      listener.connectionEstablished();
                    }
                    EventBus.getDefault().post(new OnlineOfflineToggledEvent(true));
                    if (sendQueue) {
                      syncLocalAndServer(context);
                    }
                  }
                });
              } else {
                setOnlineFailed();
                EventBus.getDefault().post(new ConnectionFailedEvent(SERVER_ERROR));
                if (listener != null) {
                  listener.networkAvailableButConnectFailed(SERVER_ERROR);
                }
              }
            }

            @Override public void onFailure(Call<GenericResponse> call, Throwable t) {
              setOnlineFailed();
              EventBus.getDefault().post(new ConnectionFailedEvent(CONNECT_ERROR));
              if (listener != null) {
                listener.networkAvailableButConnectFailed(CONNECT_ERROR);
              }
            }
          });
    }
  }

  public static boolean isOnline(Context context) {
    final String status = RealmUtils.loadPreferencesFromDB().getOnlineStatus();
    return (!OFFLINE_SELECTED.equals(status) && isNetworkAvailable(context)); // this means we try to connect every time, unless told not to
  }

  public static boolean isNetworkAvailable(Context context) {
    ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
    NetworkInfo ni = cm.getActiveNetworkInfo();
    return (ni != null && ni.isAvailable() && ni.isConnected());
  }

  public static Observable syncAndStartTasks(final Context context, final boolean checkSyncTime,
                                             final boolean fetchOnly) {
    return Observable.create(new Observable.OnSubscribe() {
      @Override
      public void call(Object o) {
        if (!checkSyncTime || shouldAttemptSync(context)) {
          LocationServices.getInstance().connect();
          if (!fetchOnly) {
            syncLocalAndServer(context);
          } else {
            fetchEntitiesFromServer(context);
          }
        }
      }
    }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread());
  }

  public static Observable registerForGCM(final Context context) {
    return Observable.create(new Observable.OnSubscribe() {
      @Override
      public void call(Object o) {
        if (isOnline()) {
          Intent gcmRegistrationIntent = new Intent(context, GcmRegistrationService.class);
          gcmRegistrationIntent.putExtra(NotificationConstants.ACTION, NotificationConstants.GCM_REGISTER);
          final String phoneNumber = RealmUtils.loadPreferencesFromDB().getMobileNumber();
          gcmRegistrationIntent.putExtra(NotificationConstants.PHONE_NUMBER, phoneNumber);
          Log.d(TAG, "sending intent to GCM registration ...");
          context.startService(gcmRegistrationIntent);
        }
      }
    }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread());
  }

  public static void syncLocalAndServer(Context context) {
    Log.e(TAG, "inside network utils ... about to call sending queued entities ...");
    if (!sendingLocalQueue && isOnline(context)) {
      sendingLocalQueue = true;
      sendLocalGroups();
      sendLocallyAddedMembers();
      sendLocallyEditedGroups();
      sendNewLocalTasks();
      sendEditedTasks();
      sendTaskActions();
      EventBus.getDefault().post(new OfflineActionsSent());
    }
    sendingLocalQueue = false;
    fetchEntitiesFromServer(context);
    saveSyncTime();
  }

  public static void fetchEntitiesFromServer(final Context context) {
    if (!fetchingServerEntities) {
      fetchingServerEntities = true;
      if (isOnline(context)) {
        GroupService.getInstance().fetchGroupListWithoutError();
        GroupService.getInstance().fetchGroupJoinRequests(null);
        TaskService.getInstance().fetchUpcomingTasks(null);
      }
    }
    fetchingServerEntities = false;
  }

  public static boolean shouldAttemptSync(final Context context) {
    Log.e(TAG, "checking if we should try sync ...");
    return isOnline(context) && hasIntervalElapsedSinceSync();
  }

  private static void saveSyncTime() {
    PreferenceObject prefs = RealmUtils.loadPreferencesFromDB();
    prefs.setLastTimeSyncPerformed(Utilities.getCurrentTimeInMillisAtUTC());
    RealmUtils.saveDataToRealm(prefs).subscribe();
  }

  private static boolean hasIntervalElapsedSinceSync() {
    final long lastTimeSynced = RealmUtils.loadPreferencesFromDB().getLastTimeSyncPerformed();
    Log.d(TAG, "and ... last time synced = " + lastTimeSynced);
    final long timeNow = Utilities.getCurrentTimeInMillisAtUTC();
    return timeNow > (lastTimeSynced + minIntervalBetweenSyncs);
  }

  private static void sendLocalGroups() {
    RealmUtils.loadListFromDB(Group.class, "isLocal", true, Schedulers.immediate())
        .subscribe(new Action1<List<Group>>() {
      @Override public void call(List<Group> realmResults) {
        for (final Group g : realmResults) {
          GroupService.getInstance().sendNewGroupToServer(g.getGroupUid(), Schedulers.immediate())
              .subscribe(new Subscriber<String>() {
                @Override
                public void onError(Throwable e) {
                  setOnlineFailed();
                }

                @Override
                public void onCompleted() { }

                @Override
                public void onNext(String s) { }
              });
        }
      }
    });
  }

  private static void sendLocallyAddedMembers() {
    RealmUtils.loadListFromDB(Group.class, "isEditedLocal", true, Schedulers.immediate())
        .subscribe(new Action1<List<Group>>() {
      @Override
      public void call(List<Group> realmResults) {
        if (!realmResults.isEmpty()) {
          Log.e(TAG, "found a locally edited group! this many : " + realmResults.size());
          final Map<String, Object> queryMap = new HashMap<>();
          queryMap.put("isLocal", true);
          for (final Group g : realmResults) {
            queryMap.put("groupUid", g.getGroupUid());
            long countLocalMembers = RealmUtils.countListInDB(Member.class, queryMap);
            if (countLocalMembers != 0L) {
              List<Member> members = RealmUtils.loadListFromDBInline(Member.class, queryMap);
              processStoredNewMembers(g.getGroupUid(), members);
            }
          }
        }
      }
    });
  }

  private static void processStoredNewMembers(final String groupUid, final List<Member> members) {
    GroupService.getInstance().addMembersToGroup(groupUid, members, true)
        .subscribe(new Subscriber() {
          @Override
          public void onCompleted() {

          }

          @Override
          public void onError(Throwable e) {
            setOnlineFailed(); // todo : interrupt sequence of calls
          }

          @Override
          public void onNext(Object o) {
            Log.e(TAG, "group edited locally returned okay ... sent to server"); // todo : flag as locally edit
          }
        });
  }

  private static void sendLocallyEditedGroups() {
    RealmUtils.loadListFromDB(LocalGroupEdits.class, Schedulers.immediate())
        .subscribe(new Action1<List<LocalGroupEdits>>() {
          @Override
          public void call(List<LocalGroupEdits> results) {
            if (results != null && !results.isEmpty()) {
              processLocalGroupEdits(results);
            }
          }
        });
  }

  private static void processLocalGroupEdits(List<LocalGroupEdits> edits) {
    for (LocalGroupEdits edit : edits) {
      GroupService.getInstance().sendLocalEditsToServer(edit, Schedulers.immediate())
          .subscribe(new Subscriber<String>() {
            @Override
            public void onError(Throwable e) {
              if (CONNECT_ERROR.equals(e.getMessage())) {
                setOnlineFailed();
              }
            }

            @Override
            public void onNext(String s) {
              Log.d(TAG, "successfully sent edits to server ...");
            }

            @Override
            public void onCompleted() { }
          });
    }
  }

  private static void sendNewLocalTasks() {
    Map<String, Object> map = new HashMap<>();
    map.put("isLocal", true);
    map.put("isEdited", false);
    map.put("isParentLocal", false);
     RealmUtils.loadListFromDB(TaskModel.class, map).subscribe(new Action1<List<TaskModel>>() {
       @Override
       public void call(List<TaskModel> tasks) {
         for (final TaskModel model : tasks) {
           final String localUid = model.getTaskUid();
           TaskService.getInstance().sendNewTaskToServer(model, new TaskService.TaskCreationListener() {
             @Override public void taskCreatedLocally(final TaskModel task) {
               RealmUtils.saveDataToRealm(task).subscribe(new Action1() {
                 @Override public void call(Object o) {
                   RealmUtils.removeObjectFromDatabase(TaskModel.class, "taskUid", localUid);
                 }
               });
               System.out.println("TASK CREATED" + task.toString());
             }

             @Override public void taskCreatedOnServer(TaskModel task) {

             }

             @Override public void taskCreationError(TaskModel task) {

             }
           });
         }
       }
     });
  }

  private static void sendEditedTasks() {
    Map<String, Object> map1 = new HashMap<>();
    map1.put("isLocal", true);
    map1.put("isEdited", true);
    map1.put("isParentLocal", false);
    RealmUtils.loadListFromDB(TaskModel.class, map1).subscribe(new Action1<List<TaskModel>>() {
      @Override
      public void call(List<TaskModel> tasks1) {
        for (final TaskModel model : tasks1) {
          TaskService.getInstance()
                  .sendTaskUpdateToServer(model, true); // todo : work out selected member change logic
        }
      }
    });
  }

  private static void sendTaskActions(){
    Map<String, Object> map1 = new HashMap<>();
    map1.put("isActionLocal", true);
    map1.put("isLocal",false);
    RealmUtils.loadListFromDB(TaskModel.class,map1).subscribe(new Action1<List<TaskModel>>() {
      @Override
      public void call(List<TaskModel> tasks) {
        for(TaskModel taskModel : tasks){
          TaskService.getInstance().respondToTask(taskModel, taskModel.getReply(), new TaskService.TaskActionListener() {
            @Override
            public void taskActionComplete(TaskModel task, String reply) {
            }

            @Override
            public void taskActionError(Response<TaskResponse> response) {

            }

            @Override
            public void taskActionCompleteOffline(TaskModel task, String reply) {

            }
          });
        }
      }
    });
  }
}
