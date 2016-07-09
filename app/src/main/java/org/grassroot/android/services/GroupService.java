package org.grassroot.android.services;

import android.app.Activity;
import android.util.Log;
import android.view.View;
import io.realm.Realm;
import io.realm.RealmList;
import io.realm.RealmResults;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.grassroot.android.R;
import org.grassroot.android.events.GroupsRefreshedEvent;
import org.grassroot.android.events.JoinRequestsReceived;
import org.grassroot.android.interfaces.NetworkErrorDialogListener;
import org.grassroot.android.models.Group;
import org.grassroot.android.models.GroupJoinRequest;
import org.grassroot.android.models.GroupResponse;
import org.grassroot.android.models.TaskModel;
import org.grassroot.android.utils.ErrorUtils;
import org.grassroot.android.utils.PreferenceUtils;
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
      return loadGroupsFromDB();
    }
  }

  public void fetchGroupList(final Activity activity, final View errorViewHolder,
      final GroupServiceListener listener) {

    if (listener == null) {
      throw new UnsupportedOperationException("Error! Call to fetch group list must have listener");
    }

    final String mobileNumber = PreferenceUtils.getPhoneNumber();
    final String userCode = PreferenceUtils.getAuthToken();

    groupsLoading = true;
    GrassrootRestService.getInstance()
        .getApi()
        .getUserGroups(mobileNumber, userCode)
        .enqueue(new Callback<GroupResponse>() {
          @Override
          public void onResponse(Call<GroupResponse> call, Response<GroupResponse> response) {
            if (response.isSuccessful()) {
              groupsLoading = false;
              groupsFinishedLoading = true;
              userGroups = new ArrayList<>(response.body().getGroups());
              saveGroupsInDB(response.body().getGroups());
              EventBus.getDefault().post(new GroupsRefreshedEvent());
              listener.groupListLoaded();
            } else {
              Log.e(TAG, response.message());
              ErrorUtils.handleServerError(errorViewHolder, activity, response);
              listener.groupListLoadingError();
            }
          }

          @Override public void onFailure(Call<GroupResponse> call, Throwable t) {
            // default back to loading from DB
            ErrorUtils.handleNetworkError(activity, errorViewHolder, t);
            loadGroupsFromDB();
            listener.groupListLoadingError();
          }
        });
  }

    public RealmList<Group> loadGroupsFromDB() {
        Realm realm = Realm.getDefaultInstance();
        RealmList<Group> groups = new RealmList<>();
        if (realm != null && !realm.isClosed()) {
            RealmResults<Group> results = realm.where(Group.class).findAll();
            groups.addAll(realm.copyFromRealm(results));
        }
        userGroups = new ArrayList<>(groups);
        realm.close();
        return groups;
    }

  /*

 Called from "swipe refresh" on group recycler, so am just formally separating from the initiating call (which is triggered on app load)
  */
  public void refreshGroupList(final Activity activity, final GroupServiceListener listener) {
    final String mobileNumber = PreferenceUtils.getPhoneNumber();
    final String userCode = PreferenceUtils.getAuthToken();
    GrassrootRestService.getInstance()
        .getApi()
        .getUserGroups(mobileNumber, userCode)
        .enqueue(new Callback<GroupResponse>() {
          @Override
          public void onResponse(Call<GroupResponse> call, Response<GroupResponse> response) {
            if (response.isSuccessful()) {
              listener.groupListLoaded();
            } else {
              listener.groupListLoadingError();
            }
          }

          @Override public void onFailure(Call<GroupResponse> call, Throwable t) {
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

  /*
  Called when, e.g., some change group event is triggered on event bus or elsewhere
   */
  public void refreshSingleGroup(final int position, final String groupUid, final Activity activity,
      final GroupServiceListener listener) {
    Group groupUpdated = userGroups.get(position);
    if (groupUpdated.getGroupUid().equals(groupUid)) {
      String mobileNumber = PreferenceUtils.getPhoneNumber();
      String code = PreferenceUtils.getAuthToken();
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

  private void saveGroupsInDB(RealmList<Group> groups) {
    Realm realm = Realm.getDefaultInstance();
    if (groups != null && realm != null && !realm.isClosed()) {
      realm.beginTransaction();
      realm.copyToRealmOrUpdate(groups);
      realm.commitTransaction();
      realm.close();
    }
  }


    /* METHODS FOR RETRIEVING AND APPROVING GROUP JOIN REQUESTS */

    public void fetchGroupJoinRequests() {
        final String mobileNumber = PreferenceUtils.getPhoneNumber();
        final String userToken = PreferenceUtils.getAuthToken();
        GrassrootRestService.getInstance().getApi().getOpenJoinRequests(mobileNumber, userToken)
                .enqueue(new Callback<RealmList<GroupJoinRequest>>() {
                    @Override
                    public void onResponse(Call<RealmList<GroupJoinRequest>> call, Response<RealmList<GroupJoinRequest>> response) {
                        if (response.isSuccessful()) {
                            saveJoinRequestsInDB(response.body());
                            Log.d(TAG, "join requests received: " + response.body());
                            if (!response.body().isEmpty()) {
                                EventBus.getDefault().post(new JoinRequestsReceived());
                            }
                        } else {
                            loadGroupsFromDB();
                            Log.e(TAG, "Error retrieving join requests!");
                        }
                    }

                    @Override
                    public void onFailure(Call<RealmList<GroupJoinRequest>> call, Throwable t) {
                        loadGroupsFromDB();
                        Log.e(TAG, "Error in network!"); // todo : anything?
                    }
                });
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