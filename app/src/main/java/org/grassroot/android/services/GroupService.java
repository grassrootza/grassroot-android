package org.grassroot.android.services;

import android.app.Activity;
import android.util.Log;
import android.view.View;
import io.realm.Realm;
import io.realm.RealmList;
import io.realm.RealmResults;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.grassroot.android.R;
import org.grassroot.android.events.GroupEditErrorEvent;
import org.grassroot.android.events.GroupEditedEvent;
import org.grassroot.android.events.GroupsRefreshedEvent;
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
import org.grassroot.android.utils.Constant;
import org.grassroot.android.utils.ErrorUtils;
import org.grassroot.android.utils.NetworkUtils;
import org.grassroot.android.utils.PermissionUtils;
import org.grassroot.android.utils.RealmUtils;
import org.grassroot.android.utils.Utilities;
import org.greenrobot.eventbus.EventBus;
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

  public boolean groupsLoading = false;
  public boolean groupsFinishedLoading = false;

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

  public List<Group> getGroups() {
    if (userGroups == null || userGroups.isEmpty()) {
      return userGroups;
    } else {
      return RealmUtils.loadListFromDB(Group.class);
    }
  }

  public void fetchGroupList(final Activity activity, final View errorViewHolder,
      final GroupServiceListener listener) {

    if (listener == null) {
      throw new UnsupportedOperationException("Error! Call to fetch group list must have listener");
    }

    final String mobileNumber = RealmUtils.loadPreferencesFromDB().getMobileNumber();
    final String userCode = RealmUtils.loadPreferencesFromDB().getToken();
    long lastTimeUpdated = RealmUtils.loadPreferencesFromDB().getLastTimeGroupsFetched();

    Log.e(TAG, "last time groups updated = " + lastTimeUpdated);

    groupsLoading = true;
    GrassrootRestService.getInstance()
        .getApi()
        .getUserGroups(mobileNumber, userCode)
        .enqueue(new Callback<GroupsChangedResponse>() {
          @Override

          public void onResponse(Call<GroupsChangedResponse> call,
              Response<GroupsChangedResponse> response) {
            if (response.isSuccessful()) {
              updateGroupsFetchedTime();
              groupsLoading = false;
              groupsFinishedLoading = true;
              userGroups = new ArrayList<>(response.body().getAddedAndUpdated());
              RealmUtils.saveDataToRealm(response.body().getAddedAndUpdated());
              EventBus.getDefault().post(new GroupsRefreshedEvent());
              listener.groupListLoaded();
              for (Group g : response.body().getAddedAndUpdated()) {
                for (Member m : g.getMembers()) {
                  m.setMemberGroupUid();
                  RealmUtils.saveDataToRealm(m);
                }
              }
            } else {
              Log.e(TAG, response.message());
              ErrorUtils.handleServerError(errorViewHolder, activity, response);
              listener.groupListLoadingError();
            }
          }

          @Override public void onFailure(Call<GroupsChangedResponse> call, Throwable t) {
            // default back to loading from DB
            ErrorUtils.handleNetworkError(activity, errorViewHolder, t);
            userGroups = new ArrayList<>(RealmUtils.loadListFromDB(Group.class));
            listener.groupListLoadingError();
          }
        });
  }

  /*

 Called from "swipe refresh" on group recycler, so am just formally separating from the initiating call (which is triggered on app load)
  */
  public void refreshGroupList(final Activity activity, final GroupServiceListener listener) {
    final String mobileNumber = RealmUtils.loadPreferencesFromDB().getMobileNumber();
    final String userCode = RealmUtils.loadPreferencesFromDB().getToken();
    final long lastTimeGroupsUpdated = RealmUtils.loadPreferencesFromDB().getLastTimeGroupsFetched();

    Log.e(TAG, "refresh group list, checking for changes since: " + lastTimeGroupsUpdated);

    GrassrootRestService.getInstance()
        .getApi()
        .getUserGroups(mobileNumber, userCode)
        .enqueue(new Callback<GroupsChangedResponse>() {
          @Override public void onResponse(Call<GroupsChangedResponse> call,
              Response<GroupsChangedResponse> response) {
            if (response.isSuccessful()) {
              updateGroupsFetchedTime();
              listener.groupListLoaded();
            } else {
              listener.groupListLoadingError();
            }
          }

          @Override public void onFailure(Call<GroupsChangedResponse> call, Throwable t) {
            ErrorUtils.connectivityError(activity, R.string.error_no_network,
                new NetworkErrorDialogListener() {
                  @Override public void retryClicked() {
                    refreshGroupList(activity, listener);
                  }

                  @Override public void offlineClicked() {
                    listener.groupListLoadingError();
                  }
                });
          }
        });
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

  public void createGroup(final String groupUid, final String groupName,
      final String groupDescription, final List<Member> groupMembers,
      final GroupCreationListener listener) {
    String mobileNumber = RealmUtils.loadPreferencesFromDB().getMobileNumber();
    String code = RealmUtils.loadPreferencesFromDB().getToken();
    final Group group = createGroupLocally(groupUid, groupName, groupDescription, groupMembers);
    GrassrootRestService.getInstance()
        .getApi()
        .createGroup(mobileNumber, code, groupName, groupDescription, groupMembers)
        .enqueue(new Callback<GroupResponse>() {
          @Override
          public void onResponse(Call<GroupResponse> call, Response<GroupResponse> response) {
            if (response.isSuccessful()) {
              Log.d(TAG, "returning group created! with UID : " + response.body()
                  .getGroups()
                  .get(0)
                  .getGroupUid());
              RealmUtils.removeObjectFromDatabase(Group.class, "groupUid", groupUid);
              RealmUtils.removeObjectFromDatabase(Member.class,"groupUid", groupUid);
              for(Member m :response.body().getGroups().first().getMembers()){
                m.setMemberGroupUid();
                RealmUtils.saveDataToRealm(m);
              }
              RealmUtils.saveDataToRealm(response.body().getGroups().first());
              listener.groupCreatedOnServer(response.body().getGroups().first());
            } else {
              listener.groupCreationError(response);
            }
          }

          @Override public void onFailure(Call<GroupResponse> call, Throwable t) {
            Log.e(TAG, "Error! This should not occur");
            listener.groupCreatedLocally(group);
          }
        });
  }

  public Group createGroupLocally(final String groupUid, final String groupName,
      final String groupDescription, final List<Member> groupMembers) {
    Realm realm = Realm.getDefaultInstance();
    Group group = new Group(groupUid);
    group.setGroupName(groupName);
    group.setDescription(groupDescription);
    group.setIsLocal(true);
    group.setGroupCreator(RealmUtils.loadPreferencesFromDB().getUserName());
    group.setLastChangeType(GroupConstants.GROUP_CREATED);
    group.setGroupMemberCount(groupMembers.size());
    group.setDate(new Date());
    group.setDateTimeStringISO(group.getDateTimeStringISO());
    RealmList<RealmString> permissions = new RealmList<>();
    //TODO investigate permission per user
    permissions.add(new RealmString(PermissionUtils.permissionForTaskType(TaskConstants.MEETING)));
    permissions.add(new RealmString(PermissionUtils.permissionForTaskType(TaskConstants.VOTE)));
    permissions.add(new RealmString(PermissionUtils.permissionForTaskType(TaskConstants.TODO)));
    group.setPermissions(permissions);
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

  /* METHODS FOR ADDING AND REMOVING MEMBERS */

  public interface MembersRemovedListener {
    void membersRemoved(String saveType);
    void memberRemovalError(String errorType, Object data);
  }

  public void removeGroupMembers(Group group, final Set<String> membersToRemoveUIDs, final MembersRemovedListener listener) {
    final String phoneNumber = RealmUtils.loadPreferencesFromDB().getMobileNumber();
    final String code = RealmUtils.loadPreferencesFromDB().getToken();
    final String groupUid = group.getGroupUid();
    if (NetworkUtils.isNetworkAvailable()) {
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

  public void renameGroup(final Group group, final String newName) {
    final String groupUid = group.getGroupUid();
    if (NetworkUtils.isNetworkAvailable(ApplicationLoader.applicationContext)) {
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
    if (NetworkUtils.isNetworkAvailable(ApplicationLoader.applicationContext)) {
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
    if (NetworkUtils.isNetworkAvailable()) {
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
    if (NetworkUtils.isNetworkAvailable()) {
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
    if (NetworkUtils.isNetworkAvailable()) {
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

    if (NetworkUtils.isNetworkAvailable()) {
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

    if (NetworkUtils.isNetworkAvailable()) {
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

    if (NetworkUtils.isNetworkAvailable()) {
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
    if (NetworkUtils.isNetworkAvailable(ApplicationLoader.applicationContext)) {
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
              Log.d(TAG, "join requests received: " + response.body());
              if (!response.body().isEmpty()) {
                listener.groupJoinRequestsOpen(loadRequestsFromDB());
                // EventBus.getDefault().post(new JoinRequestReceived());
              } else {
                listener.groupJoinRequestsEmpty();
              }
            } else {
              Log.e(TAG, "Error retrieving join requests!");
              listener.groupJoinRequestsOffline(loadRequestsFromDB());
            }
          }

          @Override public void onFailure(Call<RealmList<GroupJoinRequest>> call, Throwable t) {
            Log.e(TAG, "Error in network!"); // todo : anything?
            listener.groupJoinRequestsOffline(loadRequestsFromDB());
          }
        });
  }

  public void refreshGroupJoinRequests(final GroupJoinRequestListener listener) {
    if (NetworkUtils.isNetworkAvailable(ApplicationLoader.applicationContext)) {
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