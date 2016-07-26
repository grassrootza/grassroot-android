package org.grassroot.android.services;

import android.app.Activity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import org.grassroot.android.R;
import org.grassroot.android.events.GroupDeletedEvent;
import org.grassroot.android.events.GroupEditErrorEvent;
import org.grassroot.android.events.GroupEditedEvent;
import org.grassroot.android.events.GroupsRefreshedEvent;
import org.grassroot.android.events.JoinRequestReceived;
import org.grassroot.android.interfaces.GroupConstants;
import org.grassroot.android.interfaces.NetworkErrorDialogListener;
import org.grassroot.android.interfaces.TaskConstants;
import org.grassroot.android.models.GenericResponse;
import org.grassroot.android.models.Group;
import org.grassroot.android.models.GroupJoinRequest;
import org.grassroot.android.models.GroupResponse;
import org.grassroot.android.models.GroupsChangedResponse;
import org.grassroot.android.models.Member;
import org.grassroot.android.models.Permission;
import org.grassroot.android.models.PermissionResponse;
import org.grassroot.android.models.PreferenceObject;
import org.grassroot.android.models.RealmString;
import org.grassroot.android.models.TaskModel;
import org.grassroot.android.utils.Constant;
import org.grassroot.android.utils.ErrorUtils;
import org.grassroot.android.utils.NetworkUtils;
import org.grassroot.android.utils.PermissionUtils;
import org.grassroot.android.utils.RealmUtils;
import org.grassroot.android.utils.Utilities;
import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.realm.Realm;
import io.realm.RealmList;
import io.realm.RealmResults;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Created by luke on 2016/07/01.
 */
public class GroupService {

  public static final String TAG = GroupService.class.getSimpleName();

  // todo : remove these?
  public ArrayList<Group> userGroups;
  public ArrayList<GroupJoinRequest> openJoinRequests;

  private static GroupService instance = null;

  public interface GroupServiceListener {
    void groupListLoaded();
    void groupListLoadingError();
  }

  public interface GroupCreationListener {
    void groupCreatedLocally(Group group);
    void groupCreatedOnServer(Group group);
    void groupCreationError(Response<GroupResponse> response);
  }

  protected GroupService() {
    userGroups = new ArrayList<>();
    openJoinRequests = new ArrayList<>();
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

  public void fetchGroupListWithErrorDisplay(final Activity activity, final View errorViewHolder,
                                             final GroupServiceListener listener) {

    if (listener == null) {
      throw new UnsupportedOperationException("Error! Call to fetch group list must have listener");
    }

    final String mobileNumber = RealmUtils.loadPreferencesFromDB().getMobileNumber();
    final String userCode = RealmUtils.loadPreferencesFromDB().getToken();
    long lastTimeUpdated = RealmUtils.loadPreferencesFromDB().getLastTimeGroupsFetched();

    Log.d(TAG, "checking for groups updated since : " + lastTimeUpdated);
    Call<GroupsChangedResponse> apiCall = (lastTimeUpdated == 0) ?
            GrassrootRestService.getInstance().getApi().getUserGroups(mobileNumber, userCode) :
            GrassrootRestService.getInstance().getApi().getUserGroupsChangedSince(mobileNumber, userCode, lastTimeUpdated);

    apiCall.enqueue(new Callback<GroupsChangedResponse>() {
          @Override
          public void onResponse(Call<GroupsChangedResponse> call, Response<GroupsChangedResponse> response) {
            if (response.isSuccessful()) {
              userGroups = new ArrayList<>(response.body().getAddedAndUpdated());
              persistGroupsAddedUpdated(response.body());
              EventBus.getDefault().post(new GroupsRefreshedEvent());
              listener.groupListLoaded();
            } else {
              Log.e(TAG, response.message());
              if (errorViewHolder != null) {
                ErrorUtils.handleServerError(errorViewHolder, activity, response);
              }
              listener.groupListLoadingError();
            }
          }

          @Override public void onFailure(Call<GroupsChangedResponse> call, Throwable t) {
            // default back to loading from DB
            if (errorViewHolder != null) {
              ErrorUtils.handleNetworkError(activity, errorViewHolder, t);
            }
            userGroups = new ArrayList<>(RealmUtils.loadGroupsSorted());
            listener.groupListLoadingError();
          }
        });
  }

  public void fetchGroupListWithoutError() {
    final String mobileNumber = RealmUtils.loadPreferencesFromDB().getMobileNumber();
    final String userCode = RealmUtils.loadPreferencesFromDB().getToken();
    long lastTimeUpdated = RealmUtils.loadPreferencesFromDB().getLastTimeGroupsFetched(); // todo : make sure this is thread safe vs above (e.g., if both start up and HGL fragment call it ...)

    Call<GroupsChangedResponse> apiCall = (lastTimeUpdated == 0) ?
            GrassrootRestService.getInstance().getApi().getUserGroups(mobileNumber, userCode) :
            GrassrootRestService.getInstance().getApi().getUserGroupsChangedSince(mobileNumber, userCode, lastTimeUpdated);

    apiCall.enqueue(new Callback<GroupsChangedResponse>() {
      @Override
      public void onResponse(Call<GroupsChangedResponse> call, Response<GroupsChangedResponse> response) {
        if (response.isSuccessful()) {
          userGroups = new ArrayList<>(response.body().getAddedAndUpdated()); // todo : might not need this
          persistGroupsAddedUpdated(response.body());
          EventBus.getDefault().post(new GroupsRefreshedEvent());
        }
      }

      @Override
      public void onFailure(Call<GroupsChangedResponse> call, Throwable t) { }
    });
  }



  private void persistGroupsAddedUpdated(GroupsChangedResponse responseBody) {
    updateGroupsFetchedTime(); // in case another call comes in (see above re threads)
    RealmUtils.saveDataToRealm(responseBody.getAddedAndUpdated());
    if (!responseBody.getRemovedUids().isEmpty()) {
      RealmUtils.removeObjectsByUid(Group.class, "groupUid",
              RealmUtils.convertListOfRealmStringInListOfString(responseBody.getRemovedUids())); // todo : just switch this to List<String> in object
    }
    // note: put this on a background thread, and do it in refresh too (if we keep refresh method)
    for (Group g : responseBody.getAddedAndUpdated()) {
      for (Member m : g.getMembers()) {
        m.setMemberGroupUid();
        RealmUtils.saveDataToRealm(m);
      }
    }
  }

  private void updateGroupsFetchedTime() {
    PreferenceObject preferenceObject = RealmUtils.loadPreferencesFromDB();
    preferenceObject.setLastTimeGroupsFetched(Utilities.getCurrentTimeInMillisAtUTC());
    RealmUtils.saveDataToRealm(preferenceObject);
  }

  /*
  Called when, e.g., some change group event is triggered on event bus or elsewhere
   */
  public void refreshSingleGroup(final int position, final String groupUid, final Activity activity,
      final GroupServiceListener listener) {
    Group groupUpdated = userGroups.get(position);
    if (groupUpdated.getGroupUid().equals(groupUid)) {
      String mobileNumber = RealmUtils.loadPreferencesFromDB().getMobileNumber();
      String code = RealmUtils.loadPreferencesFromDB().getToken();
      GrassrootRestService.getInstance()
          .getApi()
          .getSingleGroup(mobileNumber, code, groupUid)
          .enqueue(new Callback<GroupResponse>() {
            @Override
            public void onResponse(Call<GroupResponse> call, Response<GroupResponse> response) {
              // todo : check corner cases of filtered list (current list setup likely fragile)
              // todo : consider shuffling this group to the top of the list
              Group group = response.body().getGroups().get(0);
              Log.e(TAG, "Group updated, has " + group.getGroupMemberCount() + " members");
              userGroups.set(position, group);
              listener.groupListLoaded();
            }

            @Override public void onFailure(Call<GroupResponse> call, Throwable t) {
              ErrorUtils.connectivityError(activity, R.string.error_no_network,
                  new NetworkErrorDialogListener() {
                    @Override public void retryClicked() {
                      refreshSingleGroup(position, groupUid, activity, listener);
                    }

                    @Override public void offlineClicked() {
                      listener.groupListLoadingError(); // todo : instead propogate "gone offline"
                    }
                  });
            }
          });
    } else {
      listener.groupListLoadingError();
    }
  }

    /*
    METHODS FOR CREATING AND MODIFYING / EDITING GROUPS
     */

  // todo : don't need to do set members?
  public Group createGroupLocally(final String groupUid, final String groupName,
      final String groupDescription, final List<Member> groupMembers) {
    Realm realm = Realm.getDefaultInstance();
    Group group = new Group(groupUid);
    group.setGroupName(groupName);
    group.setDescription(groupDescription);
    group.setIsLocal(true);
    group.setGroupCreator(RealmUtils.loadPreferencesFromDB().getUserName());
    group.setLastChangeType(GroupConstants.GROUP_CREATED);
    group.setGroupMemberCount(groupMembers.size() + 1);
    group.setDate(new Date());
    group.setDateTimeStringISO(group.getDateTimeStringISO());
    group.setLastMajorChangeMillis(Utilities.getCurrentTimeInMillisAtUTC());
    RealmList<RealmString> permissions = new RealmList<>();
    //TODO investigate permission per user
    permissions.add(new RealmString(PermissionUtils.permissionForTaskType(TaskConstants.MEETING)));
    permissions.add(new RealmString(PermissionUtils.permissionForTaskType(TaskConstants.VOTE)));
    permissions.add(new RealmString(PermissionUtils.permissionForTaskType(TaskConstants.TODO)));
    permissions.add(new RealmString(GroupConstants.PERM_ADD_MEMBER));
    permissions.add(new RealmString(GroupConstants.PERM_GROUP_SETTNGS));
    group.setPermissions(permissions);
    realm.beginTransaction();
    realm.copyToRealmOrUpdate(group);
    realm.commitTransaction();
    realm.beginTransaction();
    for (Member m : groupMembers) {
      if (TextUtils.isEmpty(m.getGroupUid())) {
        m.setGroupUid(groupUid);
      }
      realm.copyToRealmOrUpdate(m);
    }
    realm.commitTransaction();
    realm.close();
    return group;
  }

  public Group updateLocalGroup(Group group, final String updatedName, final String groupDescription,
                                final List<Member> groupMembers) {
    Realm realm = Realm.getDefaultInstance();
    group.setGroupName(updatedName);
    group.setDescription(groupDescription);
    group.setGroupMemberCount(groupMembers.size() + 1);
    group.setLastMajorChangeMillis(Utilities.getCurrentTimeInMillisAtUTC());
    realm.beginTransaction();
    realm.copyToRealmOrUpdate(group);
    realm.commitTransaction();
    realm.beginTransaction();
    for (Member m : groupMembers) {
      realm.copyToRealmOrUpdate(m);
    }
    realm.commitTransaction();
    realm.close();
    return group;
  }

  public void sendNewGroupToServer(final String localGroupUid, final GroupCreationListener listener) {

    Log.e(TAG, "looking for group with local UID ... " + localGroupUid);
    final Group localGroup = RealmUtils.loadGroupFromDB(localGroupUid);

    if (NetworkUtils.isOnline()) {

      final String phoneNumber = RealmUtils.loadPreferencesFromDB().getMobileNumber();
      final String code = RealmUtils.loadPreferencesFromDB().getToken();
      final RealmList<Member> members = RealmUtils.loadListFromDB(Member.class, "groupUid", localGroupUid);

      GrassrootRestService.getInstance()
              .getApi()
              .createGroup(phoneNumber, code, localGroup.getGroupName(), localGroup.getDescription(), members)
              .enqueue(new Callback<GroupResponse>() {
                @Override
                public void onResponse(Call<GroupResponse> call, Response<GroupResponse> response) {
                  if (response.isSuccessful()) {
                    final Group groupFromServer = response.body().getGroups().first();
                    saveCreatedGroupToRealm(groupFromServer);
                    cleanUpLocalGroup(localGroupUid, members, groupFromServer);
                    Log.d("tag", "returning group created! with UID : " + groupFromServer.getGroupUid());
                    if (listener != null) {
                      listener.groupCreatedOnServer(groupFromServer);
                    }
                  } else {
                    saveCreatedGroupToRealm(localGroup);
                    if (listener != null) {
                      // listener.groupCreationError(response); // todo : decide if we want to call this
                      listener.groupCreatedLocally(localGroup); // we probably also want to send an error ..
                    }
                  }
                }

                @Override
                public void onFailure(Call<GroupResponse> call, Throwable t) {
                  saveCreatedGroupToRealm(localGroup);
                  if (listener != null) {
                    listener.groupCreatedLocally(localGroup);
                  }
                }
              });
    } else {
      listener.groupCreatedLocally(localGroup);
    }
  }

  public void deleteLocallyCreatedGroup(final String groupUid) {
    RealmUtils.removeObjectFromDatabase(Group.class,"groupUid",groupUid);
    RealmUtils.removeObjectFromDatabase(Member.class,"groupUid",groupUid);
    EventBus.getDefault().post(new GroupDeletedEvent());
  }

  private void saveCreatedGroupToRealm(Group group) {
    PreferenceObject preferenceObject = RealmUtils.loadPreferencesFromDB();
    preferenceObject.setHasGroups(true);
    RealmUtils.saveDataToRealm(preferenceObject);
    RealmUtils.saveGroupToRealm(group);
  }

  private void cleanUpLocalGroup(final String localGroupUid, RealmList<Member> localMembers, Group groupFromServer) {
    RealmList<TaskModel> models = RealmUtils.loadListFromDB(TaskModel.class, "parentUid", localGroupUid);
    for (int i = 0; i < models.size(); i++) {
      models.get(i).setParentUid(groupFromServer.getGroupUid());
      TaskService.getInstance().sendNewTaskToServer(models.get(i), null);
    }
    RealmUtils.removeObjectFromDatabase(Group.class, "groupUid", localGroupUid);
    RealmUtils.removeObjectFromDatabase(Member.class,"groupUid", localGroupUid);
    for(Member m : groupFromServer.getMembers()){
      m.setMemberGroupUid();
      RealmUtils.saveDataToRealm(m);
    }
  }

  /* METHODS FOR ADDING AND REMOVING MEMBERS */

  public interface MembersRemovedListener {
    void membersRemoved(String saveType);
    void memberRemovalError(String errorType, Object data);
  }

  public Group addGroupMembersLocally(final String groupUid, final List<Member> membersToAdd) {
    Group group = RealmUtils.loadGroupFromDB(groupUid);
    group.getMembers().addAll(membersToAdd);
    RealmUtils.saveGroupToRealm(group);
    RealmUtils.saveDataToRealm(membersToAdd);
    return group;
  }

  public void postNewGroupMembers(final List<Member> membersToAdd, final String groupUid) {
    final String mobileNumber = RealmUtils.loadPreferencesFromDB().getMobileNumber();
    final String sessionCode = RealmUtils.loadPreferencesFromDB().getToken();
    GrassrootRestService.getInstance()
            .getApi()
            .addGroupMembers(groupUid, mobileNumber, sessionCode, membersToAdd)
            .enqueue(new Callback<GroupResponse>() {
              @Override
              public void onResponse(Call<GroupResponse> call, Response<GroupResponse> response) {
                if (response.isSuccessful()) {
                  Map<String, Object> map2 = new HashMap<>();
                  map2.put("isLocal", true);
                  map2.put("groupUid", membersToAdd.get(0).getGroupUid());
                  RealmUtils.removeObjectsFromDatabase(Member.class,map2);
                  RealmUtils.saveDataToRealm(response.body().getGroups());
                  for(Member m : response.body().getGroups().first().getMembers()){
                    m.setMemberGroupUid();
                    RealmUtils.saveDataToRealm(m);
                  }
                } else {
                }
              }

              @Override public void onFailure(Call<GroupResponse> call, Throwable t) {
              }
            });
  }

  public void removeGroupMembers(Group group, final Set<String> membersToRemoveUIDs, final MembersRemovedListener listener) {
    final String phoneNumber = RealmUtils.loadPreferencesFromDB().getMobileNumber();
    final String code = RealmUtils.loadPreferencesFromDB().getToken();
    final String groupUid = group.getGroupUid();
    if (NetworkUtils.isOnline()) {
      GrassrootRestService.getInstance().getApi().removeGroupMembers(phoneNumber, code,
              groupUid, membersToRemoveUIDs).enqueue(new Callback<GenericResponse>() {
        @Override
        public void onResponse(Call<GenericResponse> call, Response<GenericResponse> response) {
          if (response.isSuccessful()) {
            removeMembersInDB(membersToRemoveUIDs, groupUid);
            listener.membersRemoved(GroupEditedEvent.CHANGED_ONLINE);
          } else {
            listener.memberRemovalError("", response.errorBody());
          }
        }

        @Override
        public void onFailure(Call<GenericResponse> call, Throwable t) {
          listener.memberRemovalError("", t);
        }
      });
    } else {
      removeMembersInDB(membersToRemoveUIDs, groupUid);
      listener.membersRemoved(GroupEditedEvent.CHANGED_OFFLINE); // todo : make sure this syncs later
    }
  }

  private void removeMembersInDB(final Set<String> memberUids, final String groupUid) {
    for (String memberUid : memberUids) {
      final String memberGroupUid = memberUid + groupUid;
      RealmUtils.removeObjectFromDatabase(Member.class, "memberGroupUid", memberGroupUid);
    }
  }

  /* METHODS FOR EDITING GROUP */

  public interface GroupEditingListener {
    void joinCodeOpened(final String joinCode);
    void apiCallComplete();
    void apiCallFailed(String tag, String offOrOnline);
  }

  /* FIRST, METHODS FOR ADJUSTING GROUP IMAGE */

  public void changeGroupDefaultImage(final Group group, final String defaultImage, final int defaultImageRes,
                                      final GroupEditingListener listener) {
    group.setDefaultImage(defaultImage);
    group.setDefaultImageRes(defaultImageRes);
    RealmUtils.saveGroupToRealm(group);

    final String mobile = RealmUtils.loadPreferencesFromDB().getMobileNumber();
    final String token = RealmUtils.loadPreferencesFromDB().getToken();

    GrassrootRestService.getInstance().getApi().changeDefaultImage(mobile, token, group.getGroupUid(),
            defaultImage).enqueue(new Callback<GroupResponse>() {
      @Override
      public void onResponse(Call<GroupResponse> call, Response<GroupResponse> response) {
        if (response.isSuccessful()) {
          RealmUtils.saveGroupToRealm(response.body().getGroups().first()); // todo : reexamine whether we want call above
          listener.apiCallComplete();
        } else {
          listener.apiCallFailed(GroupEditedEvent.IMAGE_TO_DEFAULT, Constant.ONLINE);
        }
      }

      @Override
      public void onFailure(Call<GroupResponse> call, Throwable t) {
        listener.apiCallFailed(GroupEditedEvent.IMAGE_TO_DEFAULT, Constant.OFFLINE);
      }
    });
  }

  public void uploadCustomImage(final String groupUid, final String compressedFilePath, final String mimeType, final GroupEditingListener listener) {
    final File file = new File(compressedFilePath);
    Log.e(TAG, "file size : " + (file.length() / 1024));
    RequestBody requestFile = RequestBody.create(MediaType.parse(mimeType), file);
    MultipartBody.Part image = MultipartBody.Part.createFormData("image", file.getName(), requestFile);

    final String phoneNumber = RealmUtils.loadPreferencesFromDB().getMobileNumber();
    final String code = RealmUtils.loadPreferencesFromDB().getToken();

    GrassrootRestService.getInstance().getApi().uploadImage(phoneNumber, code, groupUid, image).enqueue(new Callback<GroupResponse>() {
      @Override
      public void onResponse(Call<GroupResponse> call, Response<GroupResponse> response) {
        file.delete();
        if (response.isSuccessful()) {
          Group updatedGroup = response.body().getGroups().first();
          RealmUtils.saveGroupToRealm(updatedGroup);
          listener.apiCallComplete();
        } else {
          listener.apiCallFailed(GroupEditedEvent.IMAGE_UPLOADED, Constant.ONLINE);
        }
      }

      @Override
      public void onFailure(Call<GroupResponse> call, Throwable t) {
        file.delete();
        listener.apiCallFailed(GroupEditedEvent.IMAGE_UPLOADED, Constant.OFFLINE);
      }
    });
  }

  public void renameGroup(final Group group, final String newName) {
    final String groupUid = group.getGroupUid();
    if (NetworkUtils.isOnline(ApplicationLoader.applicationContext)) {
      final String mobileNumber = RealmUtils.loadPreferencesFromDB().getMobileNumber();
      final String code = RealmUtils.loadPreferencesFromDB().getToken();
      GrassrootRestService.getInstance().getApi().renameGroup(mobileNumber, code, groupUid, newName)
              .enqueue(new Callback<GenericResponse>() {
                @Override
                public void onResponse(Call<GenericResponse> call, Response<GenericResponse> response) {
                  if (response.isSuccessful()) {
                    saveRenamedGroupToDB(group, newName);
                    EventBus.getDefault().post(new GroupEditedEvent(GroupEditedEvent.RENAMED,
                            GroupEditedEvent.CHANGED_ONLINE, groupUid, newName));
                  } else {
                    EventBus.getDefault().post(new GroupEditErrorEvent(response.errorBody()));
                  }
                }

                @Override
                public void onFailure(Call<GenericResponse> call, Throwable t) {
                  EventBus.getDefault().post(new GroupEditErrorEvent(t));
                }
              });
    } else {
      // todo : put in a queue for later ...
      saveRenamedGroupToDB(group, newName);
      EventBus.getDefault().post(new GroupEditedEvent(GroupEditedEvent.RENAMED,
              GroupEditedEvent.CHANGED_OFFLINE, groupUid, newName));
    }
  }

  private void saveRenamedGroupToDB(Group group, final String newName) {
    group.setGroupName(newName);
    RealmUtils.saveGroupToRealm(group);
  }

  public void switchGroupPublicStatus(final Group group, final boolean isPublic, final GroupEditingListener listener) {
    final String groupUid = group.getGroupUid();
    final String phoneNumber = RealmUtils.loadPreferencesFromDB().getMobileNumber();
    final String token = RealmUtils.loadPreferencesFromDB().getToken();
    if (NetworkUtils.isOnline(ApplicationLoader.applicationContext)) {
      GrassrootRestService.getInstance().getApi().switchGroupPublicPrivate(phoneNumber, token,
              groupUid, isPublic).enqueue(new Callback<GenericResponse>() {
        @Override
        public void onResponse(Call<GenericResponse> call, Response<GenericResponse> response) {
          if (response.isSuccessful()) {
            listener.apiCallComplete();
            group.setDiscoverable(isPublic);
            RealmUtils.saveGroupToRealm(group);
            EventBus.getDefault().post(new GroupEditedEvent(GroupEditedEvent.PUBLIC_STATUS_CHANGED, GroupEditedEvent.CHANGED_ONLINE,
                    groupUid, String.valueOf(isPublic)));
          } else {
            EventBus.getDefault().post(new GroupEditErrorEvent(response.errorBody()));
            listener.apiCallFailed(GroupEditedEvent.PUBLIC_STATUS_CHANGED, Constant.ONLINE);
          }
        }

        @Override
        public void onFailure(Call<GenericResponse> call, Throwable t) {
          EventBus.getDefault().post(new GroupEditErrorEvent(t));
          listener.apiCallFailed(GroupEditedEvent.PUBLIC_STATUS_CHANGED, Constant.OFFLINE);
        }
      });
    } else {
      // todo : think about whether even want this to be possible offline
      listener.apiCallFailed(GroupEditedEvent.PUBLIC_STATUS_CHANGED, Constant.OFFLINE);
    }
  }

  public void closeJoinCode(final Group group, final GroupEditingListener listener) {
    final String phoneNumber = RealmUtils.loadPreferencesFromDB().getMobileNumber();
    final String token = RealmUtils.loadPreferencesFromDB().getToken();
    if (NetworkUtils.isOnline()) {
      GrassrootRestService.getInstance().getApi().closeJoinCode(phoneNumber, token, group.getGroupUid())
              .enqueue(new Callback<GenericResponse>() {
                @Override
                public void onResponse(Call<GenericResponse> call, Response<GenericResponse> response) {
                  if (response.isSuccessful()) {
                    listener.apiCallComplete();
                    EventBus.getDefault().post(new GroupEditedEvent(GroupEditedEvent.JOIN_CODE_CLOSED,
                            GroupEditedEvent.CHANGED_ONLINE, group.getGroupUid(), group.getGroupName()));
                  } else {
                    EventBus.getDefault().post(new GroupEditErrorEvent(response.errorBody()));
                    listener.apiCallFailed(GroupEditedEvent.JOIN_CODE_CLOSED, Constant.ONLINE);
                  }
                }

                @Override
                public void onFailure(Call<GenericResponse> call, Throwable t) {
                  EventBus.getDefault().post(new GroupEditErrorEvent(t));
                  listener.apiCallFailed(GroupEditedEvent.JOIN_CODE_CLOSED, Constant.OFFLINE);
                }
              });
    } else {
      listener.apiCallFailed(GroupEditedEvent.JOIN_CODE_CLOSED, Constant.OFFLINE);
    }
  }

  public void openJoinCode(final Group group, final GroupEditingListener listener) {
    final String phoneNumber = RealmUtils.loadPreferencesFromDB().getMobileNumber();
    final String token = RealmUtils.loadPreferencesFromDB().getToken();
    if (NetworkUtils.isOnline()) {
      GrassrootRestService.getInstance().getApi().openJoinCode(phoneNumber, token, group.getGroupUid())
              .enqueue(new Callback<GenericResponse>() {
                @Override
                public void onResponse(Call<GenericResponse> call, Response<GenericResponse> response) {
                  if (response.isSuccessful()) {
                    listener.apiCallComplete();
                    final String newJoinCode = (String) response.body().getData();
                    group.setJoinCode(newJoinCode);
                    RealmUtils.saveGroupToRealm(group);
                    EventBus.getDefault().post(new GroupEditedEvent(GroupEditedEvent.JOIN_CODE_OPENED,
                            GroupEditedEvent.CHANGED_ONLINE, group.getGroupUid(), newJoinCode));
                    listener.joinCodeOpened(newJoinCode);
                  } else {
                    listener.apiCallFailed(GroupEditedEvent.JOIN_CODE_OPENED, Constant.ONLINE);
                    EventBus.getDefault().post(new GroupEditErrorEvent(response.errorBody()));
                  }
                }

                @Override
                public void onFailure(Call<GenericResponse> call, Throwable t) {
                  EventBus.getDefault().post(new GroupEditErrorEvent(t));
                  listener.apiCallFailed(GroupEditedEvent.JOIN_CODE_OPENED, Constant.OFFLINE);
                }
              });
    } else {
      // have to just queue it and report back ... can't open locally (uniqueness of token ...)
      listener.apiCallFailed(GroupEditedEvent.JOIN_CODE_OPENED, Constant.OFFLINE);
    }
  }

  public void addOrganizer(final Group group, final String memberUid, final GroupEditingListener listener) {
    final String phoneNumber = RealmUtils.loadPreferencesFromDB().getMobileNumber();
    final String token = RealmUtils.loadPreferencesFromDB().getToken();
    final String groupUid = group.getGroupUid();
    if (NetworkUtils.isOnline()) {
      GrassrootRestService.getInstance().getApi().addOrganizer(phoneNumber, token,
              groupUid, memberUid).enqueue(new Callback<GenericResponse>() {
        @Override
        public void onResponse(Call<GenericResponse> call, Response<GenericResponse> response) {
          if (response.isSuccessful()) {
            listener.apiCallComplete();
            updateMemberRoleInDB(groupUid, memberUid, GroupConstants.ROLE_GROUP_ORGANIZER);
            EventBus.getDefault().post(new GroupEditedEvent(GroupEditedEvent.ORGANIZER_ADDED,
                    GroupEditedEvent.CHANGED_ONLINE, group.getGroupUid(), memberUid));
          } else {
            EventBus.getDefault().post(new GroupEditErrorEvent(response.errorBody()));
            listener.apiCallFailed(GroupEditedEvent.ORGANIZER_ADDED, Constant.ONLINE);
          }
        }

        @Override
        public void onFailure(Call<GenericResponse> call, Throwable t) {
          listener.apiCallFailed(GroupEditedEvent.ORGANIZER_ADDED, Constant.OFFLINE);
          EventBus.getDefault().post(new GroupEditErrorEvent(t));
        }
      });
    } else {
      updateMemberRoleInDB(groupUid, memberUid, GroupConstants.ROLE_GROUP_ORGANIZER);
      EventBus.getDefault().post(new GroupEditedEvent(GroupEditedEvent.ORGANIZER_ADDED,
              GroupEditedEvent.CHANGED_ONLINE, group.getGroupUid(), memberUid));
      listener.apiCallComplete();
    }
  }

  /*
  METHODS FOR HANDLING PERMISSIONS, ROLE CHANGES ETC
   */

  public interface GroupPermissionsListener {
    String OFFLINE = "offline";
    String DENIED = "access_denied";

    void permissionsLoaded(List<Permission> permissions);
    void permissionsUpdated(List<Permission> permissions);
    void errorLoadingPermissions(String errorDescription);
    void errorUpdatingPermissions(String errorDescription);
  }

  public void fetchGroupPermissions(Group group, String roleName, final GroupPermissionsListener listener) {
    final String mobileNumber = RealmUtils.loadPreferencesFromDB().getMobileNumber();
    final String token = RealmUtils.loadPreferencesFromDB().getToken();

    if (NetworkUtils.isOnline()) {
      GrassrootRestService.getInstance().getApi().fetchPermissions(mobileNumber, token,
              group.getGroupUid(), roleName).enqueue(new Callback<PermissionResponse>() {
        @Override
        public void onResponse(Call<PermissionResponse> call, Response<PermissionResponse> response) {
          if (response.isSuccessful()) {
            listener.permissionsLoaded(response.body().getPermissions());
          } else {
            listener.errorLoadingPermissions(GroupPermissionsListener.DENIED);
          }
        }

        @Override
        public void onFailure(Call<PermissionResponse> call, Throwable t) {
          listener.errorLoadingPermissions(GroupPermissionsListener.OFFLINE);
        }
      });
    } else {
      // todo : maybe we should store locally so can at least read (and, in general, have read only mode) ... tbd
      listener.errorLoadingPermissions(GroupPermissionsListener.OFFLINE);
    }
  }

  public void updateGroupPermissions(Group group, String roleName, final List<Permission> updatedPermissions, final GroupPermissionsListener listener) {
    final String mobileNumber = RealmUtils.loadPreferencesFromDB().getMobileNumber();
    final String token = RealmUtils.loadPreferencesFromDB().getToken();

    if (NetworkUtils.isOnline()) {
      GrassrootRestService.getInstance().getApi().updatePermissions(mobileNumber, token,
              group.getGroupUid(), roleName, updatedPermissions).enqueue(new Callback<GenericResponse>() {
        @Override
        public void onResponse(Call<GenericResponse> call, Response<GenericResponse> response) {
          if (response.isSuccessful()) {
            listener.permissionsUpdated(updatedPermissions);
          } else {
            listener.errorUpdatingPermissions(GroupPermissionsListener.DENIED);
          }
        }

        @Override
        public void onFailure(Call<GenericResponse> call, Throwable t) {
          listener.errorUpdatingPermissions(GroupPermissionsListener.OFFLINE);
        }
      });
    } else {
      listener.errorUpdatingPermissions(GroupPermissionsListener.OFFLINE);
    }
  }

  public void changeMemberRole(final String groupUid, final String memberUid, final String newRole) {
    final String mobileNumber = RealmUtils.loadPreferencesFromDB().getMobileNumber();
    final String token = RealmUtils.loadPreferencesFromDB().getToken();

    if (NetworkUtils.isOnline()) {
      GrassrootRestService.getInstance().getApi().changeMemberRole(mobileNumber, token, groupUid,
              memberUid, newRole).enqueue(new Callback<GenericResponse>() {
        @Override
        public void onResponse(Call<GenericResponse> call, Response<GenericResponse> response) {
          if (response.isSuccessful()) {
            updateMemberRoleInDB(groupUid, memberUid, newRole);
            EventBus.getDefault().post(new GroupEditedEvent(GroupEditedEvent.ROLE_CHANGED, GroupEditedEvent.CHANGED_ONLINE,
                    groupUid, memberUid));
          } else {
            EventBus.getDefault().post(new GroupEditErrorEvent(response.errorBody()));
          }
        }

        @Override
        public void onFailure(Call<GenericResponse> call, Throwable t) {
          EventBus.getDefault().post(new GroupEditErrorEvent(t));
        }
      });
    } else {
      // queue ? probably shouldn't allow
      updateMemberRoleInDB(groupUid, memberUid, newRole);
      EventBus.getDefault().post(new GroupEditedEvent(GroupEditedEvent.ROLE_CHANGED, GroupEditedEvent.CHANGED_OFFLINE,
              groupUid, memberUid));
    }
  }

  private void updateMemberRoleInDB(final String groupUid, final String memberUid, final String roleName) {
    final String memberGroupUid = memberUid + groupUid;
    Member member = RealmUtils.loadObjectFromDB(Member.class, "memberGroupUid", memberGroupUid);
    member.setRoleName(roleName);
    RealmUtils.saveDataToRealm(member);
  }

    /* METHODS FOR RETRIEVING AND APPROVING GROUP JOIN REQUESTS */

  public interface GroupJoinRequestListener {
    void groupJoinRequestsEmpty();
    void groupJoinRequestsOpen(RealmList<GroupJoinRequest> joinRequests);
    void groupJoinRequestsOffline(RealmList<GroupJoinRequest> openJoinRequests);
  }

  public void loadGroupJoinRequests(final GroupJoinRequestListener listener) {
    if (NetworkUtils.isOnline(ApplicationLoader.applicationContext)) {
      // todo : refine logic
      fetchGroupJoinRequests(listener);
    } else {
      listener.groupJoinRequestsOffline(loadRequestsFromDB());
    }
  }

  public void fetchGroupJoinRequests(final GroupJoinRequestListener listener) {
    String mobileNumber = RealmUtils.loadPreferencesFromDB().getMobileNumber();
    String code = RealmUtils.loadPreferencesFromDB().getToken();
    GrassrootRestService.getInstance()
        .getApi()
        .getOpenJoinRequests(mobileNumber, code)
        .enqueue(new Callback<RealmList<GroupJoinRequest>>() {
          @Override public void onResponse(Call<RealmList<GroupJoinRequest>> call,
              Response<RealmList<GroupJoinRequest>> response) {
            if (response.isSuccessful()) {
              saveJoinRequestsInDB(response.body());
              if (!response.body().isEmpty()) {
                if (listener != null) {
                  listener.groupJoinRequestsOpen(loadRequestsFromDB());
                }
                EventBus.getDefault().post(new JoinRequestReceived(response.body().get(0)));
              } else {
                if (listener != null) {
                  listener.groupJoinRequestsEmpty();
                }
              }
            } else {
              if (listener != null) {
                listener.groupJoinRequestsOffline(loadRequestsFromDB());
              }
            }
          }

          @Override public void onFailure(Call<RealmList<GroupJoinRequest>> call, Throwable t) {
            if (listener != null) {
              listener.groupJoinRequestsOffline(loadRequestsFromDB());
            }
          }
        });
  }

  public void refreshGroupJoinRequests(final GroupJoinRequestListener listener) {
    if (NetworkUtils.isOnline(ApplicationLoader.applicationContext)) {
      fetchGroupJoinRequests(listener);
    } else {
      listener.groupJoinRequestsOffline(loadRequestsFromDB());
    }
  }

  private void saveJoinRequestsInDB(RealmList<GroupJoinRequest> requests) {
    Realm realm = Realm.getDefaultInstance();
    if (requests != null && realm != null && !realm.isClosed()) {
      realm.beginTransaction();
      realm.copyToRealmOrUpdate(requests);
      realm.commitTransaction();
      realm.close();
    }
  }

  public RealmList<GroupJoinRequest> loadRequestsFromDB() {
    Realm realm = Realm.getDefaultInstance();
    RealmList<GroupJoinRequest> requests = new RealmList<>();
    if (realm != null && !realm.isClosed()) {
      // todo : probably want to filter by open, etc etc
      RealmResults<GroupJoinRequest> results = realm.where(GroupJoinRequest.class).findAll();
      requests.addAll(realm.copyFromRealm(results));
    }
    openJoinRequests = new ArrayList<>(requests);
    realm.close();
    return requests;
  }
}