package org.grassroot.android.services;

import android.app.Activity;
import android.util.Log;
import android.view.View;

import org.grassroot.android.R;
import org.grassroot.android.events.GroupsRefreshedEvent;
import org.grassroot.android.interfaces.GroupConstants;
import org.grassroot.android.interfaces.NetworkErrorDialogListener;
import org.grassroot.android.interfaces.TaskConstants;
import org.grassroot.android.models.Group;
import org.grassroot.android.models.GroupJoinRequest;
import org.grassroot.android.models.GroupResponse;
import org.grassroot.android.models.GroupsChangedResponse;
import org.grassroot.android.models.Member;
import org.grassroot.android.models.PreferenceObject;
import org.grassroot.android.models.RealmString;
import org.grassroot.android.utils.ErrorUtils;
import org.grassroot.android.utils.NetworkUtils;
import org.grassroot.android.utils.PermissionUtils;
import org.grassroot.android.utils.RealmUtils;
import org.grassroot.android.utils.Utilities;
import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import io.realm.Realm;
import io.realm.RealmList;
import io.realm.RealmResults;
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

          public void onResponse(Call<GroupsChangedResponse> call, Response<GroupsChangedResponse> response) {
            if (response.isSuccessful()) {
              updateGroupsFetchedTime();
              groupsLoading = false;
              groupsFinishedLoading = true;
              userGroups = new ArrayList<>(response.body().getAddedAndUpdated());
              RealmUtils.saveDataToRealm(response.body().getAddedAndUpdated());
              EventBus.getDefault().post(new GroupsRefreshedEvent());
              listener.groupListLoaded();
              for(Group g : response.body().getAddedAndUpdated()){
                for(Member m : g.getMembers()){
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
          @Override
          public void onResponse(Call<GroupsChangedResponse> call, Response<GroupsChangedResponse> response) {
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

  public void createGroup(final String groupName, final String groupDescription,
      final List<Member> groupMembers, final GroupCreationListener listener) {
    String mobileNumber = RealmUtils.loadPreferencesFromDB().getMobileNumber();
    String code = RealmUtils.loadPreferencesFromDB().getToken();

    if (!NetworkUtils.isNetworkAvailable(ApplicationLoader.applicationContext)) {
      Group group = createGroupLocally(groupName, groupDescription, groupMembers);
      listener.groupCreatedLocally(group);
    } else {
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
                RealmUtils.saveDataToRealm(response.body().getGroups().first());
                listener.groupCreatedOnServer(response.body().getGroups().first());
              } else {
                listener.groupCreationError(response);
              }
            }

            @Override public void onFailure(Call<GroupResponse> call, Throwable t) {
              Log.e(TAG, "Error! This should not occur");
              Group group = createGroupLocally(groupName, groupDescription, groupMembers);
              listener.groupCreatedLocally(group);
            }
          });
    }
  }

  private Group createGroupLocally(final String groupName, final String groupDescription,
      final List<Member> groupMembers) {
    Realm realm = Realm.getDefaultInstance();
    Group group = new Group();
    group.setGroupName(groupName);
    group.setDescription(groupDescription);
    group.setIsLocal(true);
    group.setGroupCreator(RealmUtils.loadPreferencesFromDB().getUserName());
    group.setGroupUid(UUID.randomUUID().toString());
    group.setLastChangeType(GroupConstants.GROUP_CREATED);
    group.setGroupMemberCount(1);
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
      m.setGroupUid(group.getGroupUid());
    }
    realm.commitTransaction();
    realm.close();
    return group;
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
    GrassrootRestService.getInstance().getApi().getOpenJoinRequests(mobileNumber, code)
        .enqueue(new Callback<RealmList<GroupJoinRequest>>() {
          @Override
          public void onResponse(Call<RealmList<GroupJoinRequest>> call, Response<RealmList<GroupJoinRequest>> response) {
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

          @Override
          public void onFailure(Call<RealmList<GroupJoinRequest>> call, Throwable t) {
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