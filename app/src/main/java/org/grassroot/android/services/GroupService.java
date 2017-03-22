package org.grassroot.android.services;

import android.os.Looper;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

import org.grassroot.android.events.GroupDeletedEvent;
import org.grassroot.android.events.GroupEditedEvent;
import org.grassroot.android.events.GroupPictureChangedEvent;
import org.grassroot.android.events.GroupsRefreshedEvent;
import org.grassroot.android.events.LocalGroupToServerEvent;
import org.grassroot.android.events.TasksRefreshedEvent;
import org.grassroot.android.interfaces.GroupConstants;
import org.grassroot.android.interfaces.TaskConstants;
import org.grassroot.android.models.Group;
import org.grassroot.android.models.GroupJoinRequest;
import org.grassroot.android.models.LocalGroupEdits;
import org.grassroot.android.models.Member;
import org.grassroot.android.models.Permission;
import org.grassroot.android.models.PreferenceObject;
import org.grassroot.android.models.TaskModel;
import org.grassroot.android.models.exceptions.ApiCallException;
import org.grassroot.android.models.exceptions.InvalidNumberException;
import org.grassroot.android.models.helpers.RealmString;
import org.grassroot.android.models.responses.GenericResponse;
import org.grassroot.android.models.responses.GroupChatSettingResponse;
import org.grassroot.android.models.responses.GroupResponse;
import org.grassroot.android.models.responses.GroupsChangedResponse;
import org.grassroot.android.models.responses.PermissionResponse;
import org.grassroot.android.models.responses.RestResponse;
import org.grassroot.android.models.responses.ServerErrorModel;
import org.grassroot.android.utils.ErrorUtils;
import org.grassroot.android.utils.NetworkUtils;
import org.grassroot.android.utils.PermissionUtils;
import org.grassroot.android.utils.RealmUtils;
import org.grassroot.android.utils.Utilities;
import org.grassroot.android.utils.image.LocalImageUtils;
import org.greenrobot.eventbus.EventBus;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.realm.Realm;
import io.realm.RealmList;
import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.Response;
import rx.Observable;
import rx.Scheduler;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

/**
 * Created by luke on 2016/07/01.
 */
public class GroupService {

  public static final String TAG = GroupService.class.getSimpleName();

  private static GroupService instance = null;
  public static boolean isFetchingGroups = false;

  protected GroupService() {
    // empty for instances
  }

  public static GroupService getInstance() {
    GroupService methodInstance = instance;
    if (methodInstance == null) {
      synchronized (GroupService.class) {
        methodInstance = instance;
        if (methodInstance == null) {
          instance = methodInstance = new GroupService();
        }
      }
    }
    return methodInstance;
  }

  public Observable<String> fetchGroupList(Scheduler observingThread) {
    observingThread = (observingThread == null) ? AndroidSchedulers.mainThread() : observingThread;
    return Observable.create(new Observable.OnSubscribe<String>() {
      @Override
      public void call(Subscriber<? super String> subscriber) {
        final String mobileNumber = RealmUtils.loadPreferencesFromDB().getMobileNumber();
        final String userCode = RealmUtils.loadPreferencesFromDB().getToken();
        if (!NetworkUtils.isOfflineOrLoggedOut(subscriber, mobileNumber, userCode)) {
          isFetchingGroups = true;
          long lastTimeUpdated = RealmUtils.loadPreferencesFromDB().getLastTimeGroupsFetched();
          Call<GroupsChangedResponse> apiCall = (lastTimeUpdated == 0) ?
              GrassrootRestService.getInstance().getApi().getUserGroups(mobileNumber, userCode) :
              GrassrootRestService.getInstance().getApi().getUserGroupsChangedSince(mobileNumber, userCode, lastTimeUpdated);
          try {
            Response<GroupsChangedResponse> response = apiCall.execute();
            updateGroupsFetchedTime();
            isFetchingGroups = false;
            if (response.isSuccessful()) {
              persistGroupsAddedUpdated(response.body());
              subscriber.onNext(NetworkUtils.FETCHED_SERVER);
            } else {
              Log.e(TAG, response.message());
              subscriber.onNext(NetworkUtils.SERVER_ERROR); // use these so calling class can decide whether to handle errors or just subscribe
            }
            EventBus.getDefault().post(new GroupsRefreshedEvent());
            subscriber.onCompleted();
          } catch (IOException e) {
            isFetchingGroups = false;
            NetworkUtils.setConnectionFailed();
            subscriber.onNext(NetworkUtils.CONNECT_ERROR);
            subscriber.onCompleted();
          }
        } else {
          subscriber.onNext(NetworkUtils.OFFLINE_SELECTED);
          subscriber.onCompleted();
        }

      }
    }).subscribeOn(Schedulers.io()).observeOn(observingThread);
  }

  public Observable<String> unsubscribeFromGroup(final String groupUid, @NonNull Scheduler observingThread) {
    return Observable.create(new Observable.OnSubscribe<String>() {
      @Override
      public void call(Subscriber<? super String> subscriber) {
        if (!NetworkUtils.isOnline()) {
          throw new ApiCallException(NetworkUtils.CONNECT_ERROR);
        } else {
          final String phoneNumber = RealmUtils.loadPreferencesFromDB().getMobileNumber();
          final String code = RealmUtils.loadPreferencesFromDB().getToken();
          try {
            Response<GenericResponse> response = GrassrootRestService.getInstance().getApi()
                .unsubscribeFromGroup(phoneNumber, code, groupUid).execute();
            if (response.isSuccessful()) {
              cleanGroupFromDB(groupUid);
              subscriber.onNext(NetworkUtils.SAVED_SERVER);
              fetchGroupList(Schedulers.immediate()).subscribe(); // to sync up removed UIDs and sync time
              subscriber.onCompleted();
            } else {
              throw new ApiCallException(NetworkUtils.SERVER_ERROR,
                  ErrorUtils.getRestMessage(response.errorBody()));
            }
          } catch (IOException e) {
            throw new ApiCallException(NetworkUtils.CONNECT_ERROR);
          }
        }
      }
    }).subscribeOn(Schedulers.io()).observeOn(observingThread);
  }

  public Observable<String> updateMemberChatSetting(final String groupUid, final String userUid, final boolean userInitiated, final boolean active, @NonNull Scheduler observingThread){
     return Observable.create(new Observable.OnSubscribe<String>() {
       @Override
       public void call(Subscriber<? super String> subscriber) {
         if (!NetworkUtils.isOnline()) {
           throw new ApiCallException(NetworkUtils.CONNECT_ERROR);
         } else {
           final String phoneNumber = RealmUtils.loadPreferencesFromDB().getMobileNumber();
           final String code = RealmUtils.loadPreferencesFromDB().getToken();
           try {
             Response<GenericResponse> response = GrassrootRestService.getInstance().getApi()
                     .updateUserGroupChatSettings(phoneNumber, code,groupUid,userUid,active,userInitiated).execute();
             if(response.isSuccessful()){
               subscriber.onNext(NetworkUtils.SAVED_SERVER);
               subscriber.onCompleted();
             }else{
               throw  new ApiCallException(NetworkUtils.SERVER_ERROR, ErrorUtils.getRestMessage(response.errorBody()));
             }
           } catch (IOException e) {
             throw new ApiCallException(NetworkUtils.CONNECT_ERROR);
           }

         }
         }
       }).subscribeOn(Schedulers.io()).observeOn(observingThread);
     }

  public Observable<GroupChatSettingResponse> fetchGroupChatSetting(final String groupUid, Scheduler observingThread, final String userUid){
    return Observable.create(new Observable.OnSubscribe<GroupChatSettingResponse>(){
      @Override
      public void call(Subscriber<? super GroupChatSettingResponse> subscriber) {
        if(!NetworkUtils.isOnline()){
          throw  new ApiCallException(NetworkUtils.CONNECT_ERROR);
        }else{
          final String phoneNumber = RealmUtils.loadPreferencesFromDB().getMobileNumber();
          final String code = RealmUtils.loadPreferencesFromDB().getToken();
          try{
            Response<GroupChatSettingResponse> response = GrassrootRestService.getInstance().getApi().fetchGroupMessengerSettings(phoneNumber,code,groupUid, userUid).execute();
            if(response.isSuccessful()){
              subscriber.onNext(response.body());
              subscriber.onCompleted();
            }else {
              throw new ApiCallException(NetworkUtils.SERVER_ERROR);
            }
          } catch (IOException e) {
            subscriber.onError(e);
            throw  new ApiCallException(NetworkUtils.CONNECT_ERROR);
          }
        }

      }
    }).subscribeOn(Schedulers.io()).observeOn(observingThread);
  }

  public Observable<String> requestPing(final String groupUid, Scheduler observingThread){
    return Observable.create(new Observable.OnSubscribe<String>(){
      @Override
      public void call(Subscriber<? super String> subscriber) {
        if(!NetworkUtils.isOnline()){
          subscriber.onNext(NetworkUtils.CONNECT_ERROR);
        }else{
          final String phoneNumber = RealmUtils.loadPreferencesFromDB().getMobileNumber();
          final String code = RealmUtils.loadPreferencesFromDB().getToken();
          try {
            Response<GenericResponse> response = GrassrootRestService.getInstance().getApi().requestPing(phoneNumber,code,groupUid).execute();
            if(response.isSuccessful()){
              subscriber.onNext(NetworkUtils.ONLINE_DEFAULT);
            } else {
              subscriber.onNext(NetworkUtils.SERVER_ERROR);
            }
          } catch (IOException e) {
            subscriber.onNext(NetworkUtils.CONNECT_ERROR);
          } finally {
            subscriber.onCompleted();
          }
        }
      }
    }).subscribeOn(Schedulers.io()).observeOn(observingThread);
  }

  public Observable<String>  markMessagesAsRead(final String groupUid, Scheduler observingThread){
    return Observable.create(new Observable.OnSubscribe<String>(){
      @Override
      public void call(Subscriber<? super String> subscriber) {
        if(!NetworkUtils.isOnline()){
          subscriber.onNext(NetworkUtils.CONNECT_ERROR);
        }else{
          final String phoneNumber = RealmUtils.loadPreferencesFromDB().getMobileNumber();
          final String code = RealmUtils.loadPreferencesFromDB().getToken();
          final Set<String> messageUids = RealmUtils.loadUnreadMessages(groupUid);
          if(!messageUids.isEmpty()) {
            try {
              Response<GenericResponse> response = GrassrootRestService.getInstance().getApi()
                      .markAsRead(phoneNumber, code, groupUid, messageUids).execute();
              if (response.isSuccessful()) {
                subscriber.onNext(NetworkUtils.ONLINE_DEFAULT);
              } else {
                subscriber.onNext(NetworkUtils.SERVER_ERROR);
              }
            } catch (IOException e) {
              subscriber.onNext(NetworkUtils.CONNECT_ERROR);
            } finally {
              subscriber.onCompleted();
            }
          }
      }
    }}).subscribeOn(Schedulers.io()).observeOn(observingThread);
  }



  private void persistGroupsAddedUpdated(GroupsChangedResponse responseBody) {
    if (Looper.myLooper() == Looper.getMainLooper()) {
      throw new IllegalStateException("Must not persist group list on main thread");
    }

    /*
    note: there are corner cases where user has both left and joined a group in the period since
    the last refresh, so the same group appears in both lists (e.g., unsubscribed, then was
    added back) ... when this happens, the balance of risk is to leave the group in place,
    since a group not present can't be retrieved without exit / login, whereas spurious group
    in place can be cleaned locally with an unsubscribe (as long as that activity exits gracefully)
    a fully robust reconcile on the diffs would not have this problem, but that would require a lot
    more complexity on the server changedSince logic than is present at possible (e.g., would have
    to differentiate between unsubs from Android and from USSD, and so forth)
    for the moment, then, we remove before we add, and trigger a group refresh after a succesful
    unsubscribe, which should avoid 90% of cases
    */

    if (!responseBody.getRemovedUids().isEmpty()) {
      RealmUtils.removeObjectsByUid(Group.class, "groupUid",
          RealmUtils.convertListOfRealmStringInListOfString(
              responseBody.getRemovedUids()));
      for(String uid: RealmUtils.convertListOfRealmStringInListOfString(
              responseBody.getRemovedUids())){
        MqttConnectionManager.getInstance().unsubscribeFromTopic(uid);
      }

    }

    RealmUtils.saveDataToRealmSync(responseBody.getAddedAndUpdated());
    MqttConnectionManager.getInstance().subscribeToGroups(responseBody.getAddedAndUpdated());

    List<Member> composedMembers = new ArrayList<>();
    for (Group g : responseBody.getAddedAndUpdated()) {
      for (Member m : g.getMembers()) {
        m.composeMemberGroupUid();;
        composedMembers.add(m);
      }
    }

    RealmUtils.saveDataToRealm(composedMembers, Schedulers.immediate()).subscribe();
  }

  private void cleanGroupFromDB(final String groupUid) {
    Map<String, Object> memberMap = new HashMap<>();
    memberMap.put("groupUid", groupUid);
    RealmUtils.removeObjectsFromDatabase(Member.class, memberMap);
    Map<String, Object> taskMap = new HashMap<>();
    taskMap.put("parentUid", groupUid);
    RealmUtils.removeObjectsFromDatabase(TaskModel.class, taskMap);
    RealmUtils.removeObjectFromDatabase(Group.class, "groupUid", groupUid);
    if (RealmUtils.countGroupsInDB() == 0) {
      setHasGroups(false);
    }
    EventBus.getDefault().post(new GroupsRefreshedEvent());
    EventBus.getDefault().post(new TasksRefreshedEvent());
  }

  private void updateGroupsFetchedTime() {
    PreferenceObject preferenceObject = RealmUtils.loadPreferencesFromDB();
    preferenceObject.setLastTimeGroupsFetched(Utilities.getCurrentTimeInMillisAtUTC());
    RealmUtils.saveDataToRealm(preferenceObject).subscribe();
  }

  public void setHasGroups(boolean hasGroups) {
    PreferenceObject preferenceObject = RealmUtils.loadPreferencesFromDB();
    preferenceObject.setHasGroups(hasGroups);
    RealmUtils.saveDataToRealmSync(preferenceObject);
  }

  public Observable<String> refreshGroupMembers(final String groupUid) {
    return Observable.create(new Observable.OnSubscribe<String>() {
      @Override
      public void call(Subscriber<? super String> subscriber) {
        final String phoneNumber = RealmUtils.loadPreferencesFromDB().getMobileNumber();
        final String code = RealmUtils.loadPreferencesFromDB().getToken();
        try {
          Response<RestResponse<List<Member>>> response = GrassrootRestService.getInstance().getApi()
              .fetchCurrentGroupMembers(phoneNumber, code, groupUid).execute();
          if (response.isSuccessful()) {
            List<Member> members = response.body().getData();

            Map<String, Object> existingMap = new HashMap<>();
            existingMap.put("groupUid", groupUid);
            @SuppressWarnings("unchecked")
            List<Member> removedMembers = RealmUtils.loadListFromDBInline(Member.class, existingMap);

            for (Member m : members) {
              m.composeMemberGroupUid();
            }
            RealmUtils.saveDataToRealmSync(members);

            removedMembers.removeAll(members);
            if (!removedMembers.isEmpty()) {
              List<String> uidsToRemove = new ArrayList<>();
              for (Member m : removedMembers) {
                uidsToRemove.add(m.getMemberGroupUid()); // these have been stored, by definition, so must have composite key
              }
              RealmUtils.removeObjectsByUid(Member.class, "memberGroupUid", uidsToRemove);
            }

            subscriber.onNext(NetworkUtils.FETCHED_SERVER);
            subscriber.onCompleted();
          } else{
            throw new ApiCallException(NetworkUtils.SERVER_ERROR, ErrorUtils.getRestMessage(response.errorBody()));
          }
        } catch (IOException e) {
          throw new ApiCallException(NetworkUtils.CONNECT_ERROR);
        }
      }
    }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread());
  }

    /*
    METHODS FOR CREATING AND MODIFYING / EDITING GROUPS
     */

  public Group createGroupLocally(final String groupUid, final String groupName,
      final String groupDescription, final List<Member> groupMembers) {
    Realm realm = Realm.getDefaultInstance();
    Group group = new Group(groupUid);
    group.setGroupName(groupName);
    group.setDescription(groupDescription);
    group.setIsLocal(true);
    group.setGroupCreator(RealmUtils.loadPreferencesFromDB().getUserName());
    group.setLastChangeType(GroupConstants.GROUP_CREATED);
    group.setGroupMemberCount(groupMembers.size()); // view will add +1 if group is stored locally
    group.setDate(new Date());
    group.setDateTimeStringISO(group.getDateTimeStringISO());
    group.setLastMajorChangeMillis(Utilities.getCurrentTimeInMillisAtUTC());
    RealmList<RealmString> permissions = new RealmList<>();

    permissions.add(new RealmString(PermissionUtils.permissionForTaskType(TaskConstants.MEETING)));
    permissions.add(new RealmString(PermissionUtils.permissionForTaskType(TaskConstants.VOTE)));
    permissions.add(new RealmString(PermissionUtils.permissionForTaskType(TaskConstants.TODO)));
    permissions.add(new RealmString(GroupConstants.PERM_ADD_MEMBER));
    permissions.add(new RealmString(GroupConstants.PERM_VIEW_MEMBERS));
    permissions.add(new RealmString(GroupConstants.PERM_DEL_MEMBER));
    permissions.add(new RealmString(GroupConstants.PERM_GROUP_SETTNGS));
    group.setPermissions(permissions);

    realm.beginTransaction();
    realm.copyToRealmOrUpdate(group);
    realm.commitTransaction();
    realm.beginTransaction();
    for (Member m : groupMembers) {
      if (TextUtils.isEmpty(m.getGroupUid())) {
        m.setGroupUid(groupUid); // don't set the primary key or may not be able to erase later
      }
      realm.copyToRealmOrUpdate(m);
    }
    realm.commitTransaction();
    realm.close();
    return group;
  }

  public Group updateLocalGroup(Group group, final String updatedName,
      final String groupDescription, final List<Member> groupMembers) {
    Realm realm = Realm.getDefaultInstance();
    group.setGroupName(updatedName);
    group.setDescription(groupDescription);
    group.setGroupMemberCount(groupMembers.size()); // view will add +1 if group is only local
    group.setLastMajorChangeMillis(Utilities.getCurrentTimeInMillisAtUTC());
    realm.beginTransaction();
    realm.copyToRealmOrUpdate(group);
    realm.commitTransaction();
    realm.beginTransaction();
    for (Member m : groupMembers) {
      if (TextUtils.isEmpty(m.getGroupUid())) {
        m.setGroupUid(group.getGroupUid());
      }
      realm.copyToRealmOrUpdate(m);
    }
    realm.commitTransaction();
    realm.close();
    return group;
  }

  public Observable<String> sendNewGroupToServer(final String localGroupUid, Scheduler observingThread) {
    observingThread = (observingThread == null) ? AndroidSchedulers.mainThread() : observingThread;
    return Observable.create(new Observable.OnSubscribe<String>() {
      @Override
      public void call(Subscriber<? super String> subscriber) {
        if (!NetworkUtils.isOnline()) {
          subscriber.onNext(NetworkUtils.SAVED_OFFLINE_MODE);
          subscriber.onCompleted();
        } else {
          final String phoneNumber = RealmUtils.loadPreferencesFromDB().getMobileNumber();
          final String code = RealmUtils.loadPreferencesFromDB().getToken();
          final Group localGroup = RealmUtils.loadGroupFromDB(localGroupUid);
          Map<String, Object> map = new HashMap<>();
          map.put("groupUid", localGroupUid);
          List<Member> members = RealmUtils.loadListFromDBInline(Member.class, map);
          Log.e(TAG, "adding members count "+members.size() );
          try {
            Response<GroupResponse> response = GrassrootRestService.getInstance().getApi().createGroup(phoneNumber, code,
                localGroup.getGroupName(), localGroup.getDescription(), members).execute();
            if (response.isSuccessful()) {
              final Group groupFromServer = response.body().getGroups().first();
              final String serverUid = groupFromServer.getGroupUid();

              saveCreatedGroupToRealm(groupFromServer);
              cleanUpLocalGroup(localGroupUid, groupFromServer);
              EventBus.getDefault().post(new LocalGroupToServerEvent(localGroupUid, serverUid));

              if (groupFromServer.getInvalidNumbers() == null || groupFromServer.getInvalidNumbers().isEmpty()) {
                subscriber.onNext("OK-" + serverUid);
              } else {
                // so that activity can retrieve them & sort them
                saveInvalidMembersForNewlyCreatedGroup(members, serverUid, groupFromServer.getInvalidNumbers());
                final String returnMessage = "ER-" + serverUid;
                subscriber.onNext(returnMessage);
              }
              subscriber.onCompleted();
            } else {
              ServerErrorModel errorModel = ErrorUtils.convertErrorBody(response.errorBody());
              if (errorModel == null) {
                throw new ApiCallException(NetworkUtils.SERVER_ERROR);
              }

              if (ErrorUtils.GROUP_MEMBER_INVALID_PHONE.equals(errorModel.getMessage())) {
                final String invalidString = (String) errorModel.getData();
                List<String> invalidNumbers = TextUtils.isEmpty(invalidString) ? null :
                    Arrays.asList(invalidString.split(","));
                Log.e(TAG, "here's the split list: " + invalidNumbers);
                setMemberNumbersInvalidIfInDB(localGroupUid, invalidNumbers);
                throw new InvalidNumberException(invalidString);
              } else {
                throw new ApiCallException(NetworkUtils.SERVER_ERROR, errorModel.getMessage());
              }
            }
          } catch (IOException e) {
            NetworkUtils.setConnectionFailed();
            throw new ApiCallException(NetworkUtils.CONNECT_ERROR);
          }
        }
      }
    }).subscribeOn(Schedulers.io()).observeOn(observingThread);
  }

  public void deleteLocallyCreatedGroup(final String groupUid) {
    RealmUtils.removeObjectFromDatabase(Group.class, "groupUid", groupUid);
    RealmUtils.removeObjectFromDatabase(Member.class, "groupUid", groupUid);
    EventBus.getDefault().post(new GroupDeletedEvent(groupUid));
  }

  private void saveCreatedGroupToRealm(Group group) {
    PreferenceObject preferenceObject = RealmUtils.loadPreferencesFromDB();
    preferenceObject.setHasGroups(true);
    RealmUtils.saveDataToRealmSync(preferenceObject);
    RealmUtils.saveGroupToRealm(group);
  }

  private void cleanUpLocalGroup(final String localGroupUid, final Group groupFromServer) {
    Map<String, Object> findTasks = new HashMap<>();
    findTasks.put("parentUid", localGroupUid);
    RealmUtils.removeObjectFromDatabase(Member.class,"groupUid", localGroupUid);
    for(Member m : groupFromServer.getMembers()){
      m.composeMemberGroupUid();
      RealmUtils.saveDataToRealmSync(m);
    }

    RealmList<TaskModel> models = RealmUtils.loadListFromDBInline(TaskModel.class, findTasks);
    for (int i = 0; i < models.size(); i++) {
      (models.get(i)).setParentUid(groupFromServer.getGroupUid());
      TaskService.getInstance().sendTaskToServer(models.get(i), Schedulers.immediate()).subscribe(new Subscriber<TaskModel>() {
        @Override
        public void onCompleted() { }

        @Override
        public void onError(Throwable e) {
          Log.e(TAG, "task didn't send ... error in server or connection : ");
          e.printStackTrace();
        }

        @Override
        public void onNext(TaskModel taskModel) { }
      });
    }
    RealmUtils.removeObjectFromDatabase(Group.class, "groupUid", localGroupUid);
  }

  /* METHODS FOR ADDING AND REMOVING MEMBERS */

  public Observable addMembersToGroup(final String groupUid, final List<Member> members, final boolean priorSaved) {
    return Observable.create(new Observable.OnSubscribe<String>() {
      @Override
      public void call(Subscriber<? super String> subscriber) {
        if (!NetworkUtils.isOnline()) {
          if (!priorSaved) {
            saveAddedMembersLocal(groupUid, members);
          }
          subscriber.onNext(NetworkUtils.SAVED_OFFLINE_MODE);
          subscriber.onCompleted();
        } else {
          try {
            final String msisdn = RealmUtils.loadPreferencesFromDB().getMobileNumber();
            final String code = RealmUtils.loadPreferencesFromDB().getToken();
            Response<GroupResponse> serverCall = GrassrootRestService.getInstance().getApi()
                .addGroupMembers(groupUid, msisdn, code, members).execute();

            if (serverCall.isSuccessful()) {
              // remove members which were added fine
              Map<String, Object> map2 = new HashMap<>();
              map2.put("isLocal", true);
              map2.put("groupUid", groupUid);
              map2.put("isNumberInvalid", false);
              RealmUtils.removeObjectsFromDatabase(Member.class, map2);

              // update the group locally (included members added fine)
              final Group updatedGroup = serverCall.body().getGroups().first();
              for (Member m : updatedGroup.getMembers()) {
                m.composeMemberGroupUid();
                if (!m.isNumberInvalid()) {
                  RealmUtils.saveDataToRealm(m).subscribe();
                }
              }
              RealmUtils.saveGroupToRealm(serverCall.body().getGroups().first());

              // if there were no error numbers, report back all okay; if there were, report that
              if (updatedGroup.getInvalidNumbers() == null || updatedGroup.getInvalidNumbers().isEmpty()) {
                subscriber.onNext(NetworkUtils.SAVED_SERVER);
              } else {
                final String invalidMsisdns = TextUtils.join(" ", updatedGroup.getInvalidNumbers());
                subscriber.onNext(invalidMsisdns);
              }
              EventBus.getDefault().post(new GroupEditedEvent(GroupEditedEvent.MEMBERS_ADDED,
                  NetworkUtils.SAVED_SERVER, groupUid, null));
              subscriber.onCompleted();
            } else {
              ServerErrorModel errorModel = ErrorUtils.convertErrorBody(serverCall.errorBody());
              if (errorModel == null) {
                throw new ApiCallException(NetworkUtils.SERVER_ERROR);
              }

              final String restMessage = errorModel.getMessage();

              if (ErrorUtils.GROUP_MEMBER_INVALID_PHONE.equals(restMessage)) {
                Log.d(TAG, "here's the invalid msisdn : " + errorModel.getData());
                final String concatInvalidMsisdns = (String) errorModel.getData(); // todo : use list of strings ?
                List<String> invalidNumbers = TextUtils.isEmpty(concatInvalidMsisdns) ? null :
                    Arrays.asList(concatInvalidMsisdns.split("\\s+"));
                setMemberNumbersInvalidIfInDB(groupUid, invalidNumbers);
                throw new InvalidNumberException(concatInvalidMsisdns);
              } else {
                if (!priorSaved && !ErrorUtils.PERMISSION_DENIED.equals(restMessage)) {
                  saveAddedMembersLocal(groupUid, members);
                }
                throw new ApiCallException(NetworkUtils.SERVER_ERROR, restMessage);
              }

            }
          } catch (IOException e) {
            if (!priorSaved) {
              saveAddedMembersLocal(groupUid, members);
            }
			NetworkUtils.setConnectionFailed();
            throw new ApiCallException(NetworkUtils.CONNECT_ERROR);
          }
		}
      }
    }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread());
  }

  public void saveAddedMembersLocal(final String groupUid, List<Member> members) {
    RealmUtils.saveDataToRealm(members, null).subscribe(new Action1<Boolean>() {
      @Override
      public void call(Boolean aBoolean) {
        Group group = RealmUtils.loadGroupFromDB(groupUid);
        group.setEditedLocal(true);
        group.setGroupMemberCount((int) RealmUtils.countGroupMembers(groupUid));
        RealmUtils.saveGroupToRealm(group);
        EventBus.getDefault().post(new GroupEditedEvent(GroupEditedEvent.MEMBERS_ADDED,
            NetworkUtils.SAVED_OFFLINE_MODE, groupUid, null));
      }
    });
  }

  public void saveInvalidMembersForNewlyCreatedGroup(List<Member> originalMembers, String serverGroupUid,
                                                     List<String> errorNumbers) {
    List<Member> errorMembers = ErrorUtils.findMembersFromListOfNumbers(errorNumbers, originalMembers);
    for (Member m : errorMembers) {
      m.setNumberInvalid(true);
      m.setGroupUid(serverGroupUid);
      m.composeMemberGroupUid();
      RealmUtils.saveDataToRealmSync(m);
    }
  }

  public void setMemberNumbersInvalidIfInDB(final String groupUid, final List<String> invalidNumbers) {
    // note : only sets them invalid _if_ stored in DB, but should not be stored if this is first call
    Map<String, Object> map2 = new HashMap<>();
    map2.put("isLocal", true);
    map2.put("groupUid", groupUid);

    @SuppressWarnings("unchecked")
    List<Member> errorMembers = ErrorUtils
        .findMembersFromListOfNumbers(invalidNumbers,
        RealmUtils.loadListFromDBInline(Member.class, map2));

    for (Member m : errorMembers) {
      m.setNumberInvalid(true);
      RealmUtils.saveDataToRealmSync(m);
    }
  }

  public Observable<String> cleanInvalidNumbersOnExit(final String groupUid, Scheduler observingThread) {
    observingThread = (observingThread == null) ? AndroidSchedulers.mainThread() : observingThread;
    return Observable.create(new Observable.OnSubscribe<String>() {
      @Override
      public void call(Subscriber<? super String> subscriber) {
        Map<String, Object> removalMap = new HashMap<>();
        removalMap.put("groupUid", groupUid);
        removalMap.put("isNumberInvalid", true);
        Log.e(TAG, "about to try remove members ... count is : " + RealmUtils.countListInDB(Member.class, removalMap));
        RealmUtils.removeObjectsFromDatabase(Member.class, removalMap);

        // now, reset group count, if group still exists
        Group group = RealmUtils.loadGroupFromDB(groupUid);
        if (group != null) {
          group.setGroupMemberCount((int) RealmUtils.countGroupMembers(groupUid));
          RealmUtils.saveGroupToRealm(group);
        }

        // and we're done
        subscriber.onNext("DONE");
        subscriber.onCompleted();
      }
    }).subscribeOn(Schedulers.io()).observeOn(observingThread);
  }

  public Observable<String> removeGroupMembers(final String groupUid, final Set<String> membersToRemoveUIDs) {
    return Observable.create(new Observable.OnSubscribe<String>() {
      @Override
      public void call(Subscriber<? super String> subscriber) {
        if (!NetworkUtils.isOnline()) {
          removeMembersInDB(membersToRemoveUIDs, groupUid, true);
          subscriber.onNext(NetworkUtils.SAVED_OFFLINE_MODE);
          EventBus.getDefault().post(new GroupEditedEvent(GroupEditedEvent.MEMBERS_REMOVED,
              NetworkUtils.SAVED_OFFLINE_MODE, groupUid, null));
          subscriber.onCompleted();
        } else {
          try {
            final String phoneNumber = RealmUtils.loadPreferencesFromDB().getMobileNumber();
            final String code = RealmUtils.loadPreferencesFromDB().getToken();
            Response response = GrassrootRestService.getInstance().getApi()
                .removeGroupMembers(phoneNumber, code, groupUid, membersToRemoveUIDs).execute();
            if (response.isSuccessful()) {
              removeMembersInDB(membersToRemoveUIDs, groupUid, false);
              subscriber.onNext(NetworkUtils.SAVED_SERVER);
              EventBus.getDefault().post(new GroupEditedEvent(GroupEditedEvent.MEMBERS_REMOVED,
                  NetworkUtils.SAVED_SERVER, groupUid, null));
              subscriber.onCompleted();
            } else {
              // note : this may be because of permission denied, so don't remove locally
              throw new ApiCallException(NetworkUtils.SERVER_ERROR, ErrorUtils.getRestMessage(response.errorBody()));
            }
          } catch (IOException e) {
            removeMembersInDB(membersToRemoveUIDs, groupUid, true);
            NetworkUtils.setConnectionFailed();
            EventBus.getDefault().post(new GroupEditedEvent(GroupEditedEvent.MEMBERS_REMOVED,
                NetworkUtils.SAVED_OFFLINE_MODE, groupUid, null));
            throw new ApiCallException(NetworkUtils.CONNECT_ERROR);
          }
        }
      }
    }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread());
  }

  private void removeMembersInDB(final Set<String> memberUids, final String groupUid, boolean generateLocalEditStore) {

    if (generateLocalEditStore) {
      LocalGroupEdits edits = generateLocalGroupEditObject(groupUid);
      RealmList<RealmString> removeUids = RealmUtils.convertListOfStringInRealmListOfString(new ArrayList<>(memberUids));
      edits.setMembersToRemove(removeUids);
      RealmUtils.saveDataToRealm(edits).subscribe();
    }

    for (String memberUid : memberUids) {
      final String memberGroupUid = memberUid + groupUid;
      RealmUtils.removeObjectFromDatabase(Member.class, "memberGroupUid", memberGroupUid);
    }

    Group group = RealmUtils.loadGroupFromDB(groupUid);
    group.setGroupMemberCount((int) RealmUtils.countGroupMembers(groupUid));
    RealmUtils.saveGroupToRealm(group);
  }

  /* METHODS FOR EDITING GROUP */

  private LocalGroupEdits generateLocalGroupEditObject(final String groupUid) {
    LocalGroupEdits existingEdits = RealmUtils.loadObjectFromDB(LocalGroupEdits.class, "groupUid", groupUid);
    if (existingEdits == null) {
      existingEdits = new LocalGroupEdits(groupUid);
    }
    return existingEdits;
  }

  private void removeLocalEditsIfFound(final String groupUid) {
    LocalGroupEdits existingEdits = RealmUtils.loadObjectFromDB(LocalGroupEdits.class, "groupUid", groupUid);
    if (existingEdits != null) {
      RealmUtils.removeObjectFromDatabase(LocalGroupEdits.class, "groupUid", groupUid);
    }
  }

  public Observable<String> sendLocalEditsToServer(final LocalGroupEdits existingEdits, Scheduler observingThread) {
    Observable<String> observable = Observable.create(new Observable.OnSubscribe<String>() {
      @Override
      public void call(Subscriber<? super String> subscriber) {
        if (existingEdits == null || !NetworkUtils.isOnline()) {
          subscriber.onCompleted();
        } else {
          try {
            Response<GroupResponse> response = generateGroupEditSyncCall(existingEdits).execute();
            if (response.isSuccessful()) {
              final Group updatedGroup = response.body().getGroups().first();
              final String groupUid = existingEdits.getGroupUid();
              RealmUtils.saveGroupToRealm(updatedGroup);
              removeLocalEditsIfFound(groupUid);
              subscriber.onNext(NetworkUtils.SAVED_SERVER);
              EventBus.getDefault().post(new GroupEditedEvent(GroupEditedEvent.MULTIPLE_TO_SERVER,
                  NetworkUtils.SAVED_SERVER, groupUid, ""));
              subscriber.onCompleted();
            } else {
              throw new ApiCallException(NetworkUtils.SERVER_ERROR, ErrorUtils.getRestMessage(response.errorBody()));
            }
          } catch (IOException e) {
            NetworkUtils.setConnectionFailed();
            throw new ApiCallException(NetworkUtils.CONNECT_ERROR);
          }
        }
      }
    }).subscribeOn(Schedulers.io()).observeOn(observingThread);
    return observable;
  }

  private Call<GroupResponse> generateGroupEditSyncCall(LocalGroupEdits edits) {
    final String groupUid = edits.getGroupUid();
    final String phoneNumber = RealmUtils.loadPreferencesFromDB().getMobileNumber();
    final String code = RealmUtils.loadPreferencesFromDB().getToken();
    return GrassrootRestService.getInstance().getApi()
        .combinedGroupEdits(phoneNumber, code, groupUid,
            edits.getRevisedGroupName(), edits.getRevisedGroupDescription(),
            edits.isChangedImage(), edits.getChangedImageName(),
            edits.isChangedPublicPrivate(), edits.isChangedToPublic(), edits.isClosedJoinCode(),
            RealmUtils.convertListOfRealmStringInListOfString(edits.getMembersToRemove()),
            RealmUtils.convertListOfRealmStringInListOfString(edits.getOrganizersToAdd()));
  }

  /* FIRST, METHODS FOR ADJUSTING GROUP IMAGE */

	/**
     * Method to reset a group to one of the custom images
     * @param group The group being changed
     * @param defaultImage The standardized name of the image (from GroupConstants)
     * @param defaultImageRes The R id of the image
     * @param observingThread The thread observing the operation (passing null defaults to main thread)
     * @return
	 */
  public Observable<String> changeGroupDefaultImage(final Group group, final String defaultImage, final int defaultImageRes, Scheduler observingThread) {
    observingThread = (observingThread == null) ? AndroidSchedulers.mainThread() : observingThread;
    return Observable.create(new Observable.OnSubscribe<String>() {
      @Override
      public void call(Subscriber<? super String> subscriber) {
        // may be able to remove both of thse ...
        group.setDefaultImage(defaultImage);
        group.setDefaultImageRes(defaultImageRes);
        RealmUtils.saveGroupToRealm(group);

        final String groupUid = group.getGroupUid();
        if (!NetworkUtils.isOnline()) {
          storeImageChangeLocally(groupUid, defaultImage);
          EventBus.getDefault().post(new GroupPictureChangedEvent(groupUid));
          subscriber.onNext(NetworkUtils.SAVED_OFFLINE_MODE);
          subscriber.onCompleted();
        } else {
          try {
            final String mobile = RealmUtils.loadPreferencesFromDB().getMobileNumber();
            final String token = RealmUtils.loadPreferencesFromDB().getToken();
            Response<GroupResponse> response = GrassrootRestService.getInstance().getApi()
                .changeDefaultImage(mobile, token, groupUid, defaultImage)
                .execute();
            if (response.isSuccessful()) {
              RealmUtils.saveGroupToRealm(response.body().getGroups().first());
              removeLocalEditsIfFound(groupUid);
              EventBus.getDefault().post(new GroupPictureChangedEvent(groupUid));
              subscriber.onNext(NetworkUtils.SAVED_SERVER);
              subscriber.onCompleted();
            } else {
              throw new ApiCallException(NetworkUtils.SERVER_ERROR,
                  ErrorUtils.getRestMessage(response.errorBody()));
            }
          } catch (IOException e) {
            NetworkUtils.setConnectionFailed();
            storeImageChangeLocally(groupUid, defaultImage);
            EventBus.getDefault().post(new GroupPictureChangedEvent(groupUid));
            throw new ApiCallException(NetworkUtils.CONNECT_ERROR);
          }
        }
      }
    }).subscribeOn(Schedulers.io()).observeOn(observingThread);
  }

  private void storeImageChangeLocally(final String groupUid, final String defaultImage) {
    LocalGroupEdits editStore = generateLocalGroupEditObject(groupUid);
    editStore.setChangedImage(true);
    editStore.setChangedImageName(defaultImage);
    RealmUtils.saveDataToRealm(editStore).subscribe();
  }

  public Observable<String> uploadCustomImage(final String groupUid, final String compressedFilePath,
                                              final String mimeType, Scheduler observingThread) {
    observingThread = (observingThread == null) ? AndroidSchedulers.mainThread() : observingThread;
    return Observable.create(new Observable.OnSubscribe<String>() {
      @Override
      public void call(Subscriber<? super String> subscriber) {
        if (!NetworkUtils.isOnline()) {
          throw new ApiCallException(NetworkUtils.OFFLINE_SELECTED); // require online for this (maybe change later ...)
        } else {

          MultipartBody.Part image = LocalImageUtils.getImageFromPath(compressedFilePath, mimeType);
          try {
            final String phoneNumber = RealmUtils.loadPreferencesFromDB().getMobileNumber();
            final String code = RealmUtils.loadPreferencesFromDB().getToken();

            Response<GroupResponse> response = GrassrootRestService.getInstance().getApi()
                .uploadGroupImage(phoneNumber, code, groupUid, image).execute();

            if (response.isSuccessful()) {
              RealmUtils.saveGroupToRealm(response.body().getGroups().first());
              EventBus.getDefault().post(new GroupPictureChangedEvent(groupUid));
              subscriber.onNext(NetworkUtils.SAVED_SERVER);
              subscriber.onCompleted();
            } else {
              throw new ApiCallException(NetworkUtils.SERVER_ERROR, ErrorUtils.getRestMessage(response.errorBody()));
            }

          } catch (IOException e) {
            NetworkUtils.setConnectionFailed();
            throw new ApiCallException(NetworkUtils.CONNECT_ERROR);
          }
        }
      }
    }).subscribeOn(Schedulers.io()).observeOn(observingThread);
  }

  // NB : this means must only ever be atomicity in here, i.e., don't call this method in sequence with others (else local edits deleted, and then ...)
  public Observable<String> renameGroup(final String groupUid, final String newName, Scheduler observingThread) {
    observingThread = (observingThread == null) ? AndroidSchedulers.mainThread() : observingThread;
    return Observable.create(new Observable.OnSubscribe<String>() {
      @Override
      public void call(Subscriber<? super String> subscriber) {
        Group group = RealmUtils.loadGroupFromDB(groupUid);
        GroupEditedEvent event = new GroupEditedEvent(GroupEditedEvent.RENAMED, null, groupUid, newName); // type of save set later
        String typeOfSave;
        if (!NetworkUtils.isOnline()) {
          saveRenamedGroupToDB(group, newName, true);
          typeOfSave = NetworkUtils.SAVED_OFFLINE_MODE;
        } else {
          try {
            final String mobileNumber = RealmUtils.loadPreferencesFromDB().getMobileNumber();
            final String code = RealmUtils.loadPreferencesFromDB().getToken();
            Response<GenericResponse> response = GrassrootRestService.getInstance().getApi()
                .renameGroup(mobileNumber, code, groupUid, newName).execute();
            if (response.isSuccessful()) {
              saveRenamedGroupToDB(group, newName, false);
              removeLocalEditsIfFound(groupUid);
              typeOfSave = NetworkUtils.SAVED_SERVER;
            } else {
              // don't save group, as likely permission error / will decouple from server
              throw new ApiCallException(NetworkUtils.SERVER_ERROR, ErrorUtils.getRestMessage(response.errorBody()));
            }
          } catch (IOException e) {
            saveRenamedGroupToDB(group, newName, true);
            NetworkUtils.setConnectionFailed();
            typeOfSave = NetworkUtils.SAVED_OFFLINE_MODE;
          }
        }

        subscriber.onNext(typeOfSave);
        event.setTypeOfSave(typeOfSave);
        EventBus.getDefault().post(event);
        subscriber.onCompleted();

      }
    }).subscribeOn(Schedulers.io()).observeOn(observingThread);
  }

  private void saveRenamedGroupToDB(Group group, final String newName, boolean storeEditsForSync) {
    group.setGroupName(newName);
    group.setLastMajorChangeMillis(Utilities.getCurrentTimeInMillisAtUTC());
    group.setLastChangeType(GroupConstants.GROUP_MOD_OTHER);
    group.setDate(new Date());
    RealmUtils.saveGroupToRealm(group);

    if (storeEditsForSync) {
      LocalGroupEdits edits = generateLocalGroupEditObject(group.getGroupUid());
      edits.setRevisedGroupName(newName);
      RealmUtils.saveDataToRealm(edits).subscribe();
    }
  }

  public Observable<String> changeGroupDescription(final String groupUid, final String newDescription,
                                                   Scheduler observingThread) {
    observingThread = (observingThread == null) ? AndroidSchedulers.mainThread() : observingThread;
    return Observable.create(new Observable.OnSubscribe<String>() {
      @Override
      public void call(Subscriber<? super String> subscriber) {
        String saveType;
        if (!NetworkUtils.isOnline()) {
          saveGroupDescToDB(groupUid, newDescription, true);
          saveType = NetworkUtils.SAVED_OFFLINE_MODE;
        } else {
          final String phoneNumber = RealmUtils.loadPreferencesFromDB().getMobileNumber();
          final String token = RealmUtils.loadPreferencesFromDB().getToken();
          try {
            Response<GenericResponse> response = GrassrootRestService.getInstance().getApi()
                .changeGroupDesc(phoneNumber, token, groupUid, newDescription).execute();
            if (response.isSuccessful()) {
              saveGroupDescToDB(groupUid, newDescription, false);
              saveType = NetworkUtils.SAVED_SERVER;
            } else {
              throw new ApiCallException(NetworkUtils.SERVER_ERROR, ErrorUtils.getRestMessage(response.errorBody()));
            }
          } catch (IOException e) {
            NetworkUtils.setConnectionFailed();
            saveGroupDescToDB(groupUid, newDescription, true);
            saveType = NetworkUtils.CONNECT_ERROR;
          }
        }
        subscriber.onNext(saveType);
        EventBus.getDefault().post(new GroupEditedEvent(GroupEditedEvent.DESCRIPTION, saveType,
            groupUid, newDescription));
        subscriber.onCompleted();
      }
    }).subscribeOn(Schedulers.io()).observeOn(observingThread);
  }

  private void saveGroupDescToDB(final String groupUid, final String newDesc, boolean storeEditsForSync) {
    Group group = RealmUtils.loadGroupFromDB(groupUid);
    group.setDescription(newDesc);
    group.setLastMajorChangeMillis(Utilities.getCurrentTimeInMillisAtUTC());
    group.setLastChangeType(GroupConstants.GROUP_MOD_OTHER);
    group.setDate(new Date());
    RealmUtils.saveGroupToRealm(group);

    if (storeEditsForSync) {
      LocalGroupEdits edits = generateLocalGroupEditObject(group.getGroupUid());
      edits.setRevisedGroupDescription(newDesc);
      RealmUtils.saveDataToRealm(edits).subscribe();
    }
  }

  public Observable<String> switchGroupPublicPrivate(final String groupUid, final boolean isPublic, Scheduler observingThread) {
    observingThread = (observingThread == null) ? AndroidSchedulers.mainThread() : observingThread;
    return Observable.create(new Observable.OnSubscribe<String>() {
      @Override
      public void call(Subscriber<? super String> subscriber) {
        String saveType;
        if (!NetworkUtils.isOnline()) {
          storeSwitchPublicStatus(groupUid, isPublic, true);
          saveType = NetworkUtils.SAVED_OFFLINE_MODE;
        } else {
          final String phoneNumber = RealmUtils.loadPreferencesFromDB().getMobileNumber();
          final String token = RealmUtils.loadPreferencesFromDB().getToken();
          try {
            Response<GenericResponse> response = GrassrootRestService.getInstance().getApi()
                .switchGroupPublicPrivate(phoneNumber, token, groupUid, isPublic).execute();
            if (response.isSuccessful()) {
              storeSwitchPublicStatus(groupUid, isPublic, false);
              saveType = NetworkUtils.SAVED_SERVER;
            } else {
              throw new ApiCallException(NetworkUtils.SERVER_ERROR,
                  ErrorUtils.getRestMessage(response.errorBody()));
            }
          } catch (IOException e) {
            storeSwitchPublicStatus(groupUid, isPublic, true);
            NetworkUtils.setConnectionFailed();
            saveType = NetworkUtils.SAVED_OFFLINE_MODE;
          }
        }
        subscriber.onNext(saveType);
        subscriber.onCompleted();
      }
    }).subscribeOn(Schedulers.io()).observeOn(observingThread);
  }

  private void storeSwitchPublicStatus(final String groupUid, final boolean isPublic, boolean storeForSync) {
    Group group = RealmUtils.loadGroupFromDB(groupUid);
    group.setDiscoverable(isPublic);
    RealmUtils.saveGroupToRealm(group);

    if (storeForSync) {
      LocalGroupEdits edits = generateLocalGroupEditObject(groupUid);
      edits.setChangedPublicPrivate(true);
      edits.setChangedToPublic(isPublic);
      RealmUtils.saveDataToRealm(edits).subscribe();
    }
  }

  public Observable<String> closeJoinCode(final String groupUid, Scheduler observingThread) {
    observingThread = (observingThread == null) ? AndroidSchedulers.mainThread() : observingThread;
    return Observable.create(new Observable.OnSubscribe<String>() {
      @Override
      public void call(Subscriber<? super String> subscriber) {
        if (!NetworkUtils.isOnline()) {
			// shouldn't be allowed to happen ... too sensitive a task to allow via risky queueing
          throw new ApiCallException(NetworkUtils.CONNECT_ERROR);
        } else {
          final String phoneNumber = RealmUtils.loadPreferencesFromDB().getMobileNumber();
          final String token = RealmUtils.loadPreferencesFromDB().getToken();
          try {
            Response<GenericResponse> response = GrassrootRestService.getInstance().getApi()
                .closeJoinCode(phoneNumber, token, groupUid).execute();
            if (response.isSuccessful()) {
              subscriber.onNext(NetworkUtils.SAVED_SERVER);
              storeJoinCodeClosed(groupUid);
			  subscriber.onCompleted();
            } else {
              throw new ApiCallException(NetworkUtils.SERVER_ERROR, ErrorUtils.getRestMessage(response.errorBody()));
            }
          } catch (IOException e) {
            throw new ApiCallException(NetworkUtils.CONNECT_ERROR);
          }
        }
      }
    }).subscribeOn(Schedulers.io()).observeOn(observingThread);
  }

  private void storeJoinCodeClosed(String groupUid) {
	  Group group = RealmUtils.loadGroupFromDB(groupUid);
	  group.setJoinCode(GroupConstants.NO_JOIN_CODE);
	  RealmUtils.saveGroupToRealm(group);
	  EventBus.getDefault().post(new GroupEditedEvent(GroupEditedEvent.JOIN_CODE_CLOSED, groupUid));
  }

  public Observable<String> openJoinCode(final String groupUid, Scheduler observingThread) {
    observingThread = (observingThread == null) ? AndroidSchedulers.mainThread() : observingThread;
    return Observable.create(new Observable.OnSubscribe<String>() {
      @Override
      public void call(Subscriber<? super String> subscriber) {
        if (!NetworkUtils.isOnline()) {
          throw new ApiCallException(NetworkUtils.CONNECT_ERROR);
        } else {
          try {
            final String phoneNumber = RealmUtils.loadPreferencesFromDB().getMobileNumber();
            final String token = RealmUtils.loadPreferencesFromDB().getToken();
            Response<GenericResponse> response = GrassrootRestService.getInstance().getApi()
                  .openJoinCode(phoneNumber, token, groupUid).execute();
            if (response.isSuccessful()) {
              final String newJoinCode = (String) response.body().getData();
              Group group = RealmUtils.loadGroupFromDB(groupUid);
              group.setJoinCode(newJoinCode);
              RealmUtils.saveGroupToRealm(group);
              EventBus.getDefault().post(new GroupEditedEvent(GroupEditedEvent.JOIN_CODE_OPENED,
                  NetworkUtils.SAVED_SERVER, groupUid, newJoinCode));
              subscriber.onNext(newJoinCode);
              subscriber.onCompleted();
            } else {
              throw new ApiCallException(NetworkUtils.SERVER_ERROR);
            }
          } catch (IOException e) {
            NetworkUtils.setConnectionFailed();
            throw new ApiCallException(NetworkUtils.CONNECT_ERROR);
          }
        }
      }
    }).subscribeOn(Schedulers.io()).observeOn(observingThread);
  }

  public Observable<String> addOrganizer(final String groupUid, final String memberUid, Scheduler observingThread) {
    observingThread = (observingThread == null) ? AndroidSchedulers.mainThread() : observingThread;
    return Observable.create(new Observable.OnSubscribe<String>() {
      @Override
      public void call(Subscriber<? super String> subscriber) {
		  String saveType;
		  if (!NetworkUtils.isOnline()) {
			  storeAddedOrganizer(groupUid, memberUid, true);
			  saveType = NetworkUtils.SAVED_OFFLINE_MODE;
		  } else {
			  try {
				  final String phoneNumber = RealmUtils.loadPreferencesFromDB().getMobileNumber();
				  final String token = RealmUtils.loadPreferencesFromDB().getToken();
				  Response<GenericResponse> response = GrassrootRestService.getInstance().getApi()
					  .changeMemberRole(phoneNumber, token, groupUid, memberUid, GroupConstants.ROLE_GROUP_ORGANIZER).execute();
				  if (response.isSuccessful()) {
						storeAddedOrganizer(groupUid, memberUid, false);
					  saveType = NetworkUtils.SAVED_SERVER;
				  } else {
					  throw new ApiCallException(NetworkUtils.SERVER_ERROR, ErrorUtils.getRestMessage(response.errorBody()));
				  }
			  } catch (IOException e) {
				  storeAddedOrganizer(groupUid, memberUid, true);
				  saveType = NetworkUtils.SAVED_OFFLINE_MODE;
			  }
		  }
		  subscriber.onNext(saveType);
			EventBus.getDefault().post(new GroupEditedEvent(GroupEditedEvent.ORGANIZER_ADDED, saveType, groupUid, memberUid));
			subscriber.onCompleted();
      }
    }).subscribeOn(Schedulers.io()).observeOn(observingThread);
  }

  private void storeAddedOrganizer(final String groupUid, final String memberUid, boolean storeForSyncLater) {
    updateMemberRoleInDB(groupUid, memberUid, GroupConstants.ROLE_GROUP_ORGANIZER);
    if (storeForSyncLater) {
      LocalGroupEdits edits = generateLocalGroupEditObject(groupUid);
      edits.addOrganizer(memberUid);
      RealmUtils.saveDataToRealm(edits).subscribe();
    }
  }

  /*
  METHODS FOR HANDLING PERMISSIONS, ROLE CHANGES ETC
   */

  public Observable<List<Permission>> fetchGroupPermissions(final String groupUid, final String roleName) {
    return Observable.create(new Observable.OnSubscribe<List<Permission>>() {
      @Override
      public void call(Subscriber<? super List<Permission>> subscriber) {
        if (!NetworkUtils.isOnline()) {
          throw new ApiCallException(NetworkUtils.CONNECT_ERROR);
        } else {
          final String mobileNumber = RealmUtils.loadPreferencesFromDB().getMobileNumber();
          final String code = RealmUtils.loadPreferencesFromDB().getToken();
          try {
            Response<PermissionResponse> response = GrassrootRestService.getInstance().getApi()
                .fetchPermissions(mobileNumber, code, groupUid, roleName).execute();
            if (response.isSuccessful()) {
              subscriber.onNext(response.body().getPermissions());
            } else {
              throw new ApiCallException(NetworkUtils.SERVER_ERROR, ErrorUtils.getRestMessage(response.errorBody()));
            }
          } catch (IOException e) {
            throw new ApiCallException(NetworkUtils.CONNECT_ERROR);
          }
        }
      }
    }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread());
  }

  public Observable<String> updateGroupPermissions(final String groupUid, final String roleName, final List<Permission> permissions) {
    return Observable.create(new Observable.OnSubscribe<String>() {
      @Override
      public void call(Subscriber<? super String> subscriber) {
        if (!NetworkUtils.isOnline()) {
          throw new ApiCallException(NetworkUtils.CONNECT_ERROR);
        } else {
          try {
            final String mobileNumber = RealmUtils.loadPreferencesFromDB().getMobileNumber();
            final String code = RealmUtils.loadPreferencesFromDB().getToken();
            Response<GenericResponse> response = GrassrootRestService.getInstance().getApi()
                .updatePermissions(mobileNumber, code, groupUid, roleName, permissions).execute();
            if (response.isSuccessful()) {
              subscriber.onNext(NetworkUtils.SAVED_SERVER);
              subscriber.onCompleted();
            } else {
              throw new ApiCallException(NetworkUtils.SERVER_ERROR, ErrorUtils.getRestMessage(response.errorBody()));
            }
          } catch (IOException e) {
            throw new ApiCallException(NetworkUtils.CONNECT_ERROR);
          }
        }
      }
    }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread());
  }

	public Observable<String> changeMemberRole(final String groupUid, final String memberUid,
																						 final String newRole) {
		return Observable.create(new Observable.OnSubscribe<String>() {
			@Override
			public void call(Subscriber<? super String> subscriber) {
				String typeOfSave;
				if (!NetworkUtils.isOnline()) {
					// storing these locally would require a complex hashset, in Realm, etc., so for now disable
					throw new ApiCallException(NetworkUtils.CONNECT_ERROR);
				} else {
					final String mobileNumber = RealmUtils.loadPreferencesFromDB().getMobileNumber();
					final String code = RealmUtils.loadPreferencesFromDB().getToken();
					try {
						Response<GenericResponse> response = GrassrootRestService.getInstance().getApi()
								.changeMemberRole(mobileNumber, code, groupUid, memberUid, newRole).execute();
						if (response.isSuccessful()) {
							updateMemberRoleInDB(groupUid, memberUid, newRole);
							typeOfSave = NetworkUtils.SAVED_SERVER;
						} else {
							throw new ApiCallException(NetworkUtils.SERVER_ERROR,
									ErrorUtils.getRestMessage(response.errorBody()));
						}
					} catch (IOException e) {
						throw new ApiCallException(NetworkUtils.CONNECT_ERROR);
					}
				}
				subscriber.onNext(typeOfSave);
				EventBus.getDefault().post(new GroupEditedEvent(GroupEditedEvent.ROLE_CHANGED, typeOfSave,
						groupUid, memberUid));
				subscriber.onCompleted();
			}
		}).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread());
	}

  private void updateMemberRoleInDB(final String groupUid, final String memberUid,
      final String roleName) {
    final String memberGroupUid = memberUid + groupUid;
    Member member = RealmUtils.loadObjectFromDB(Member.class, "memberGroupUid", memberGroupUid);
    if (member != null) {
			member.setRoleName(roleName);
			RealmUtils.saveDataToRealmSync(member);
		}
  }

    /* METHODS FOR RETRIEVING AND APPROVING GROUP JOIN REQUESTS */

  public Observable<String> fetchGroupJoinRequests(Scheduler observingThread) {
    observingThread = (observingThread == null) ? AndroidSchedulers.mainThread() : observingThread;
    return Observable.create(new Observable.OnSubscribe<String>() {
      @Override
      public void call(Subscriber<? super String> subscriber) {
        final String mobileNumber = RealmUtils.loadPreferencesFromDB().getMobileNumber();
        final String code = RealmUtils.loadPreferencesFromDB().getToken();
        if (!NetworkUtils.isOfflineOrLoggedOut(subscriber, mobileNumber, code)) {
          try {
            Response<RealmList<GroupJoinRequest>> response =  GrassrootRestService.getInstance().getApi()
                .getOpenJoinRequests(mobileNumber, code).execute();
            if (response.isSuccessful()) {
              RealmUtils.persistFullListJoinRequests(response.body());
              subscriber.onNext(NetworkUtils.FETCHED_SERVER);
              subscriber.onCompleted();
            }
          } catch (IOException e) {
            // note : not throwing an error here, as this isn't critical / don't want to enforce onError handling
            subscriber.onNext(NetworkUtils.CONNECT_ERROR);
            subscriber.onCompleted();
          }
        }
      }
    }).subscribeOn(Schedulers.io()).observeOn(observingThread);
  }

  public Observable<String> respondToJoinRequest(@NonNull final String approvedOrDenied, @NonNull final String requestUid,
                                                 @NonNull Scheduler observingThread) {
    return Observable.create(new Observable.OnSubscribe<String>() {
      @Override
      public void call(Subscriber<? super String> subscriber) {
        if (!NetworkUtils.isOnline()) {
          throw new ApiCallException(NetworkUtils.CONNECT_ERROR);
        } else {
          final String phoneNumber = RealmUtils.loadPreferencesFromDB().getMobileNumber();
          final String code = RealmUtils.loadPreferencesFromDB().getToken();
          try {
            Response<GenericResponse> response = GrassrootRestService.getInstance().getApi()
                .respondToJoinRequest(phoneNumber, code, requestUid, approvedOrDenied).execute();
            if (response.isSuccessful()) {
              RealmUtils.removeObjectFromDatabase(GroupJoinRequest.class, "requestUid", requestUid);
              subscriber.onNext(NetworkUtils.SAVED_SERVER);
              subscriber.onCompleted();
            } else {
              throw new ApiCallException(NetworkUtils.SERVER_ERROR, ErrorUtils.getRestMessage(response.errorBody()));
            }
          } catch (IOException e) {
            throw new ApiCallException(NetworkUtils.CONNECT_ERROR);
          }
        }
      }
    }).subscribeOn(Schedulers.io()).observeOn(observingThread);
  }

}