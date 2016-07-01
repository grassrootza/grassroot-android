package org.grassroot.android.services;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.View;

import org.grassroot.android.R;
import org.grassroot.android.interfaces.NetworkErrorDialogListener;
import org.grassroot.android.models.Group;
import org.grassroot.android.models.GroupResponse;
import org.grassroot.android.utils.ErrorUtils;
import org.grassroot.android.utils.PreferenceUtils;

import java.util.ArrayList;
import java.util.HashMap;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Created by luke on 2016/07/01.
 */
public class GroupService {

    public static final String TAG = GroupService.class.getSimpleName();

    public ArrayList<Group> userGroups;
    public HashMap<String, Integer> groupUidMap;

    public boolean groupsLoading = false;
    public boolean groupsFinishedLoading = false;

    private static GroupService instance = null;

    public interface GroupServiceListener {
        void groupListLoaded();
        void groupListLoadingError();
    }

    protected GroupService() {
        userGroups = new ArrayList<>();
        groupUidMap = new HashMap<>();
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

    public void fetchGroupList(final Activity activity, final View errorViewHolder, final GroupServiceListener listener) {

        if (listener == null) {
            throw new UnsupportedOperationException("Error! Call to fetch group list must have listener");
        }

        final String mobileNumber = PreferenceUtils.getPhoneNumber();
        final String userCode = PreferenceUtils.getAuthToken();

        groupsLoading = true;
        GrassrootRestService.getInstance().getApi().getUserGroups(mobileNumber, userCode)
                .enqueue(new Callback<GroupResponse>() {
                    @Override
                    public void onResponse(Call<GroupResponse> call, Response<GroupResponse> response) {
                        if (response.isSuccessful()) {
                            groupsLoading = false;
                            groupsFinishedLoading = true;
                            userGroups = new ArrayList<>(response.body().getGroups());
                            listener.groupListLoaded();
                            createUidMap();
                        } else {
                            Log.e(TAG, response.message());
                            ErrorUtils.handleServerError(errorViewHolder, activity, response);
                            listener.groupListLoadingError();
                        }
                    }

                    @Override
                    public void onFailure(Call<GroupResponse> call, Throwable t) {
                        ErrorUtils.handleNetworkError(activity, errorViewHolder, t);
                        listener.groupListLoadingError();
                    }
                });
    }

    /*
   Called from "swipe refresh" on group recycler, so am just formally separating from the initiating call (which is triggered on app load)
    */
    public void refreshGroupList(final Activity activity, final GroupServiceListener listener) {
        final String mobileNumber = PreferenceUtils.getPhoneNumber();
        final String userCode = PreferenceUtils.getAuthToken();
        GrassrootRestService.getInstance().getApi().getUserGroups(mobileNumber, userCode).enqueue(new Callback<GroupResponse>() {
            @Override
            public void onResponse(Call<GroupResponse> call, Response<GroupResponse> response) {
                if (response.isSuccessful()) {
                    listener.groupListLoaded();
                } else {
                    listener.groupListLoadingError();
                }
            }

            @Override
            public void onFailure(Call<GroupResponse> call, Throwable t) {
                ErrorUtils.connectivityError(activity, R.string.error_no_network, new NetworkErrorDialogListener() {
                    @Override
                    public void retryClicked() {
                        refreshGroupList(activity, listener);
                    }
                });

            }
        });
    }

    private void createUidMap() {

        if (userGroups == null) {
            throw new UnsupportedOperationException("Error! Group map creation called without groups");
        }

        if (groupUidMap == null) {
            groupUidMap = new HashMap<>();
        }

        // note : watch out for / think about what to do if we create a group locally and don't have its UID yet
        final int size = userGroups.size();
        for (int i = 0; i < size; i++) {
            groupUidMap.put(userGroups.get(i).getGroupUid(), i);
        }
    }

}
