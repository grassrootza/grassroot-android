package com.techmorphosis.grassroot.ui.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.techmorphosis.grassroot.R;
import com.techmorphosis.grassroot.adapters.UserListAdapter;
import com.techmorphosis.grassroot.services.GrassrootRestService;
import com.techmorphosis.grassroot.services.model.Member;
import com.techmorphosis.grassroot.services.model.MemberList;
import com.techmorphosis.grassroot.utils.ContactUtil.ErrorUtils;
import com.techmorphosis.grassroot.utils.SettingPreference;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Created by luke on 2016/05/08.
 */
public class MemberListFragment extends Fragment {

    private static final String TAG = MemberListFragment.class.getCanonicalName();

    private String groupUid;
    private List<Member> membersToPassAdapter;

    private GrassrootRestService grassrootRestService;
    private UserListAdapter userListAdapter;

    @BindView(R.id.mlist_frag_recycler_view)
    RecyclerView memberListRecyclerView;

    private ViewGroup vgContainer;

    // NB: we are not doing checks for null on this because we will use the fragment in create group
    public void setGroupUid(String groupUid) {
        this.groupUid = groupUid;
    }

    public void setMemberList(List<Member> members) { this.membersToPassAdapter = members; }

    public List<Member> getMemberList() { return this.userListAdapter.getMembers(); }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
    }

    private void init() {
        if (groupUid != null) {
            this.grassrootRestService = new GrassrootRestService(this.getContext());
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.e(TAG, "setting up the fragment!");
        View viewToReturn = inflater.inflate(R.layout.fragment_member_list, container, false);
        ButterKnife.bind(this, viewToReturn);
        setUpRecyclerView();
        this.vgContainer = container;
        return viewToReturn;
    }

    private void setUpRecyclerView() {
        Log.e(TAG, "setting up the recycler view!");
        this.memberListRecyclerView.setLayoutManager(new LinearLayoutManager(this.getContext()));
        this.userListAdapter = new UserListAdapter(this.getContext());

        // memberListRecyclerView.setHasFixedSize(true);
        memberListRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        memberListRecyclerView.setAdapter(userListAdapter);

        if (groupUid != null)
            retrieveGroupMembers();

        if (membersToPassAdapter != null && membersToPassAdapter.size() > 0)
            userListAdapter.addMembers(membersToPassAdapter);

        memberListRecyclerView.setVisibility(View.VISIBLE);
        Log.e(TAG, "ZOG : set up view, adaptor has : " + userListAdapter.getItemCount() + " items");
    }

    @Override
    public void onPause() {
        // todo: persist this list of members temporarily, to cut down on redundant calls
        super.onPause();
    }

    private void retrieveGroupMembers() {
        if (groupUid == null)
            throw new UnsupportedOperationException("Cannot retrieve group members from null group uid");

        String userPhoneNumber = SettingPreference.getuser_mobilenumber(this.getContext());
        String userSessionCode = SettingPreference.getuser_token(this.getContext());

        Log.d(TAG, "inside MemberListFragment, retrieving group members for uid = " + groupUid);

        grassrootRestService.getApi()
                .getGroupMembers(groupUid, userPhoneNumber, userSessionCode)
                .enqueue(new Callback<MemberList>() {
                    @Override
                    public void onResponse(Call<MemberList> call, Response<MemberList> response) {
                        if (response.isSuccessful()) {
                            List<Member> membersReturned = response.body().getMembers();
                            userListAdapter.addMembers(membersReturned);
                        } else {
                            // todo: handle error, via a dialog box
                        }
                    }

                    @Override
                    public void onFailure(Call<MemberList> call, Throwable t) {
                        ErrorUtils.handleNetworkError(getContext(), vgContainer, t); // todo : will snackbar show correctly??
                    }
                });

    }



}
