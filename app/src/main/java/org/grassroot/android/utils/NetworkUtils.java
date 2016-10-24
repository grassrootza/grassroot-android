package org.grassroot.android.utils;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.text.TextUtils;
import android.util.Log;

import org.grassroot.android.events.NetworkFailureEvent;
import org.grassroot.android.events.OfflineActionsSent;
import org.grassroot.android.events.OnlineOfflineToggledEvent;
import org.grassroot.android.interfaces.NotificationConstants;
import org.grassroot.android.models.responses.GenericResponse;
import org.grassroot.android.models.Group;
import org.grassroot.android.models.LocalGroupEdits;
import org.grassroot.android.models.Member;
import org.grassroot.android.models.PreferenceObject;
import org.grassroot.android.models.PublicGroupModel;
import org.grassroot.android.models.TaskModel;
import org.grassroot.android.models.exceptions.ApiCallException;
import org.grassroot.android.services.ApplicationLoader;
import org.grassroot.android.services.GcmRegistrationService;
import org.grassroot.android.services.GrassrootRestService;
import org.grassroot.android.services.GroupSearchService;
import org.grassroot.android.services.GroupService;
import org.grassroot.android.services.LocationServices;
import org.grassroot.android.services.TaskService;
import org.greenrobot.eventbus.EventBus;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import retrofit2.Response;
import rx.Observable;
import rx.Scheduler;
import rx.Subscriber;

import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

public class NetworkUtils {

  private static final String TAG = NetworkUtils.class.getSimpleName();

  public static final String ONLINE_DEFAULT = "default";
  public static final String OFFLINE_SELECTED = "offline_selected"; // i.e., user chose to go offline
  public static final String OFFLINE_ON_FAIL = "offline_on_fail"; // i.e., network calls failed, but user said to keep trying
  public static final String DB_EMPTY = "db_empty";

  public static final String SAVED_SERVER = "saved_server";
  public static final String SAVED_OFFLINE_MODE = "saved_offline_mode";
  public static final String SERVER_ERROR = "server_error";
  public static final String CONNECT_ERROR = "connection_error";
  public static final String LOCAL_ERROR = "local_error"; // e.g., some parameter is missing
  public static final String NO_NETWORK = "no_network";
  public static final String FETCHED_SERVER = "fetched_from_server";
  public static final String FETCHED_CACHE = "fetched_local";
  public static final String SENT_UPSTREAM = "sent_upstream"; // for chat messages

  public static final long minIntervalBetweenSyncs = 15 * 60 * 1000; // 15 minutes, in millis

  static boolean sendingLocalQueue = false;
  static boolean fetchingServerEntities = false;

  public static boolean batteryLow = false;

  public static boolean isOnline() {
    return isOnline(ApplicationLoader.applicationContext);
  }

  public static void setBatteryLow(final boolean isBatteryLow) {
    batteryLow = isBatteryLow;
  }

  public static boolean isBatteryLow() { return batteryLow; }

  public static Observable<String> toggleOnlineOfflineRx(final Context context, final boolean sendQueue, Scheduler observingThread) {
    observingThread = (observingThread == null) ? AndroidSchedulers.mainThread() : observingThread;
    final String currentStatus = RealmUtils.loadPreferencesFromDB().getOnlineStatus();
    if (ONLINE_DEFAULT.equals(currentStatus)) {
      return switchToOfflineMode(observingThread);
    } else {
      return trySwitchToOnline(context, sendQueue, observingThread);
    }
  }

  public static Observable<String> switchToOfflineMode(Scheduler observingThread) {
    observingThread = (observingThread == null) ? AndroidSchedulers.mainThread() : observingThread;
    return Observable.create(new Observable.OnSubscribe<String>() {
      @Override
      public void call(Subscriber<? super String> subscriber) {
        PreferenceObject prefs = RealmUtils.loadPreferencesFromDB();
        prefs.setOnlineStatus(OFFLINE_SELECTED);
        prefs.setLastTimeSyncPerformed(0L);
        RealmUtils.saveDataToRealmSync(prefs);
        EventBus.getDefault().post(new OnlineOfflineToggledEvent(false));
        subscriber.onNext(OFFLINE_SELECTED);
        subscriber.onCompleted();
      }
    }).subscribeOn(Schedulers.io()).observeOn(observingThread);
  }

  public static void setConnectionFailed() {
    PreferenceObject prefs = RealmUtils.loadPreferencesFromDB();
    prefs.setOnlineStatus(OFFLINE_ON_FAIL);
    prefs.setLastTimeSyncPerformed(0L);
    RealmUtils.saveDataToRealm(prefs).subscribe(new Action1() {
      @Override public void call(Object o) {
        EventBus.getDefault().post(new NetworkFailureEvent());
      }
    });
  }

  public static void setOfflineSelected() {
    PreferenceObject prefs = RealmUtils.loadPreferencesFromDB();
    prefs.setOnlineStatus(OFFLINE_SELECTED);
    prefs.setLastTimeSyncPerformed(0L);
    RealmUtils.saveDataToRealm(prefs).subscribe(new Action1() {
      @Override public void call(Object o) {
        EventBus.getDefault().post(new OnlineOfflineToggledEvent(false));
      }
    });
  }

  public static void trySwitchToOnlineQuiet(final Context context, final boolean sendQueue,
                                            Scheduler observingThread) {
    trySwitchToOnline(context, sendQueue, observingThread).subscribe(new Subscriber<String>() {
      @Override
      public void onError(Throwable e) {
        NetworkUtils.setConnectionFailed();
      }

      @Override
      public void onNext(String s) {
        Log.e(TAG, "switching to online finished, all looks fine");
        EventBus.getDefault().post(new OnlineOfflineToggledEvent(true));
      }

      @Override
      public void onCompleted() { }
    });
  }

  public static Observable<String> trySwitchToOnline(final Context context, final boolean sendQueue,
                                                     Scheduler observingThread) {
    observingThread = (observingThread == null) ? AndroidSchedulers.mainThread() : observingThread;
    return Observable.create(new Observable.OnSubscribe<String>() {
      @Override
      public void call(Subscriber<? super String> subscriber) {
        if (!isNetworkAvailable(context)) {
          setConnectionFailed();
          throw new ApiCallException(NO_NETWORK);
        } else {
          final String phoneNumber = RealmUtils.loadPreferencesFromDB().getMobileNumber();
          final String token = RealmUtils.loadPreferencesFromDB().getToken();
          try {
            Response<GenericResponse> ping = GrassrootRestService.getInstance().getApi()
                .testConnection(phoneNumber, token).execute();
            if (ping.isSuccessful()) {
              PreferenceObject prefs = RealmUtils.loadPreferencesFromDB();
              prefs.setOnlineStatus(ONLINE_DEFAULT);
              RealmUtils.saveDataToRealmSync(prefs);
              EventBus.getDefault().post(new OnlineOfflineToggledEvent(true));
              subscriber.onNext(ONLINE_DEFAULT);
              Log.d(TAG, "okay now sending the queue");
              if (sendQueue) {
                Log.e(TAG, "really sending the queue ...");
                syncLocalAndServer(context);
              }
              subscriber.onCompleted();
            } else {
              throw new ApiCallException(SERVER_ERROR);
            }
          } catch (IOException e) {
            setConnectionFailed();
            throw new ApiCallException(CONNECT_ERROR);
          }
        }
      }
    }).subscribeOn(Schedulers.io()).observeOn(observingThread);
  }

  public static void sendQueueAfterDelay() {
    Observable
        .timer(Constant.serverSyncDelay, TimeUnit.MILLISECONDS)
        .observeOn(Schedulers.io())
        .subscribe(new Action1<Long>() {
          @Override
          public void call(Long aLong) {
            Log.e(TAG, "timer done! calling sync to server");
            syncLocalAndServer(ApplicationLoader.applicationContext);
          }
        });
  }

  public static boolean isOnline(Context context) {
    final String status = RealmUtils.loadPreferencesFromDB().getOnlineStatus();
    return (!OFFLINE_SELECTED.equals(status)); // this means we try to connect every time, unless told not to (i.e., we trigger an error even if data turned off
  }

  public static boolean isOfflineOrLoggedOut(Subscriber<? super String> sub, final String phoneNumber, final String code) {
    if (!isOnline()) {
      sub.onNext(OFFLINE_SELECTED);
      sub.onCompleted();
      return true;
    } else if (TextUtils.isEmpty(phoneNumber) || TextUtils.isEmpty(code)) {
      sub.onNext(DB_EMPTY);
      sub.onCompleted();
      return true;
    } else
      return false;
  }

  public static boolean isNetworkAvailable(Context context) {
    ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
    NetworkInfo ni = cm.getActiveNetworkInfo();
//    Log.e(TAG, "is connected "+ni.isConnected());
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
        if (isOnline() && isNetworkAvailable(context)) {
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
    Log.d(TAG, "inside network utils ... about to call sending queued entities ...");
    if (!sendingLocalQueue && isOnline(context)) {
      sendingLocalQueue = true;
      Log.d(TAG, "now actually sending them ...");
      sendLocalGroups();
      sendLocallyAddedMembers();
      sendLocallyEditedGroups();
      sendNewLocalTasks();
      sendEditedTasks();
      sendTaskActions();
      sendStoredJoinRequests();
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
        GroupService.getInstance().fetchGroupList(Schedulers.immediate()).subscribe();
        GroupService.getInstance().fetchGroupJoinRequests(Schedulers.immediate()).subscribe();
        TaskService.getInstance().fetchUpcomingTasks(Schedulers.immediate()).subscribe();
      }
    }
    fetchingServerEntities = false;
  }

  public static boolean shouldAttemptSync(final Context context) {
    Log.e(TAG, "checking if we should try sync ...");
    return isOnline(context) && hasIntervalElapsedSinceSync() && !batteryLow;
  }

  private static void saveSyncTime() {
    PreferenceObject prefs = RealmUtils.loadPreferencesFromDB();
    prefs.setLastTimeSyncPerformed(Utilities.getCurrentTimeInMillisAtUTC());
    RealmUtils.saveDataToRealm(prefs).subscribe();
  }

  private static boolean hasIntervalElapsedSinceSync() {
    final long lastTimeSynced = RealmUtils.loadPreferencesFromDB().getLastTimeSyncPerformed();
    final long timeNow = Utilities.getCurrentTimeInMillisAtUTC();
    return timeNow > (lastTimeSynced + minIntervalBetweenSyncs);
  }

  private static void sendLocalGroups() {
    RealmUtils.loadListFromDB(Group.class, "isLocal", true, Schedulers.immediate())
        .subscribe(new Action1<List<Group>>() {
      @Override public void call(List<Group> realmResults) {
        Log.e(TAG, "sendLocalGroups ... found this many groups ... " + realmResults.size());
        for (final Group g : realmResults) {
          GroupService.getInstance().sendNewGroupToServer(g.getGroupUid(), Schedulers.immediate())
              .subscribe(new Subscriber<String>() {
                @Override
                public void onError(Throwable e) {
                  setConnectionFailed();
                }

                @Override
                public void onCompleted() { }

                @Override
                public void onNext(String s) {
                }
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
            setConnectionFailed(); // todo : interrupt sequence of calls
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
                setConnectionFailed();
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
           TaskService.getInstance().sendTaskToServer(model, Schedulers.immediate()).subscribe(new Subscriber<TaskModel>() {
             @Override
             public void onError(Throwable e) {
               if (e instanceof ApiCallException && NetworkUtils.CONNECT_ERROR.equals(e.getMessage())) {
                 setConnectionFailed();
               }
             }

             @Override
             public void onNext(TaskModel taskModel) {
               RealmUtils.removeObjectFromDatabase(TaskModel.class, "taskUid", localUid);
             }

             @Override
             public void onCompleted() { }
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
          TaskService.getInstance().sendTaskUpdateToServer(model, true, Schedulers.immediate())
              .subscribe(); // todo : work out selected member change logic & harmonize w/ updates below
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
        for(TaskModel taskModel : tasks) {
          TaskService.getInstance().respondToTask(taskModel.getTaskUid(), taskModel.getReply(), Schedulers.immediate())
              .subscribe(new Subscriber<String>() {
                @Override
                public void onCompleted() { }

                @Override
                public void onError(Throwable e) {
                  if (NetworkUtils.CONNECT_ERROR.equals(e.getMessage())) {
                    setConnectionFailed();
                  }
                }

                @Override
                public void onNext(String s) { }
              });
        }
      }
    });
  }

  private static void sendStoredJoinRequests() {
    Map<String, Object> map1 = new HashMap<>();
    map1.put("isJoinReqLocal", true);
    Log.d(TAG, "checking for join requests ...");
    if (RealmUtils.countListInDB(PublicGroupModel.class, map1) > 0) {
      Log.d(TAG, "found join requests stored locally ... sending ...");
      GroupSearchService.getInstance().sendStoredJoinRequests(Schedulers.immediate()).subscribe();
    }
  }

}
