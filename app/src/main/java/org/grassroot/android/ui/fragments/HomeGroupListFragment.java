package org.grassroot.android.ui.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import com.github.clans.fab.FloatingActionButton;
import com.github.clans.fab.FloatingActionMenu;
import org.grassroot.android.R;
import org.grassroot.android.adapters.GroupListAdapter;
import org.grassroot.android.events.NetworkActivityResultsEvent;
import org.grassroot.android.interfaces.NetworkErrorDialogListener;
import org.grassroot.android.interfaces.SortInterface;
import org.grassroot.android.services.GrassrootRestService;
import org.grassroot.android.services.NoConnectivityException;
import org.grassroot.android.services.model.Group;
import org.grassroot.android.services.model.GroupResponse;
import org.grassroot.android.ui.activities.CreateGroupActivity;
import org.grassroot.android.ui.activities.GroupJoinActivity;
import org.grassroot.android.ui.activities.GroupTasksActivity;
import org.grassroot.android.ui.views.CustomItemAnimator;
import org.grassroot.android.utils.Constant;
import org.grassroot.android.utils.ErrorUtils;
import org.grassroot.android.utils.MenuUtils;
import org.grassroot.android.utils.PreferenceUtils;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnTextChanged;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeGroupListFragment extends android.support.v4.app.Fragment {

    private String TAG = HomeGroupListFragment.class.getSimpleName();

    @BindView(R.id.rl_ghp_root)
    RelativeLayout rlGhpRoot;
    @BindView(R.id.iv_ghp_drawer)
    ImageView ivGhpDrawer;
    @BindView(R.id.iv_ghp_search)
    ImageView ivGhpSearch;
    @BindView(R.id.iv_ghp_sort)
    ImageView ivGhpSort;

    @BindView(R.id.gl_swipe_refresh)
    SwipeRefreshLayout glSwipeRefresh;
    @BindView(R.id.recycler_view)
    RecyclerView rcGroupList;

    @BindView(R.id.error_layout)
    View errorLayout;
    @BindView(R.id.im_no_results)
    ImageView imNoResults;
    @BindView(R.id.im_server_error)
    ImageView imServerError;
    @BindView(R.id.im_no_internet)
    ImageView imNoInternet;

    @BindView(R.id.menu1)
    FloatingActionMenu menu1;
    @BindView(R.id.ic_fab_join_group)
    FloatingActionButton icFabJoinGroup;
    @BindView(R.id.ic_fab_start_group)
    FloatingActionButton icFabStartGroup;

    @BindView(R.id.iv_cross)
    ImageView ivCross;
    @BindView(R.id.et_search)
    EditText et_search;
    @BindView(R.id.rl_search)
    RelativeLayout rlSearch;
    @BindView(R.id.rl_simple)
    RelativeLayout rlSimple;

    @BindView(R.id.progressBar)
    ProgressBar mProgressBar;

    private GroupListAdapter groupListRowAdapter;
    private List<Group> userGroups;

    private String mobileNumber;
    private String userCode;

    public boolean date_click = false, role_click = false, defaults_click = false;

    private FragmentCallbacks mCallbacks;


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        Activity activity = (Activity)context;
        try {
            mCallbacks = (FragmentCallbacks) activity;
            Log.e("onAttach", "Attached");
        } catch (ClassCastException e) {
            throw new ClassCastException("Activity must implement Fragment One.");
        }
    }

    public interface FragmentCallbacks {
        void menuClick();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_group__homepage, container, false);
        ButterKnife.bind(this, view);
        EventBus.getDefault().register(this);
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
        setUpRecyclerView();
        setUpSwipeRefresh();
        fetchGroupList();
    }

    private void init() {
       // grassrootRestService = new GrassrootRestService(this.getContext());
        userGroups = new ArrayList<>();
        ivGhpSort.setEnabled(false);
        ivGhpSearch.setEnabled(false);

        mobileNumber = PreferenceUtils.getuser_mobilenumber(getActivity());
        userCode = PreferenceUtils.getuser_token(getActivity());

        menu1.setOnMenuToggleListener(new FloatingActionMenu.OnMenuToggleListener() {
            @Override
            public void onMenuToggle(boolean opened) {
                icFabJoinGroup.setVisibility(opened ? View.VISIBLE : View.GONE);
                icFabStartGroup.setVisibility(opened ? View.VISIBLE : View.GONE);
            }
        });
    }

    public void showSuccessMessage(Intent data) {
        // todo : update the group's card
        Log.e(TAG, "and ... it's done");
        String message = data.getStringExtra(Constant.SUCCESS_MESSAGE);
        ErrorUtils.showSnackBar(rlGhpRoot, message, Snackbar.LENGTH_LONG, "", null);
    }

    /**
     * Method executed to retrieve and populate list of groups. Note: this does not handle the absence
     * of a connection very well, at all. Will probably need to rethink.
     */
    private void fetchGroupList() {

        mProgressBar.setVisibility(View.VISIBLE);


        Call<GroupResponse> call = GrassrootRestService.getInstance().getApi().getUserGroups(mobileNumber, userCode);
        call.enqueue(new Callback<GroupResponse>() {
            @Override
            public void onResponse(Call<GroupResponse> call, Response<GroupResponse> response) {
                if (response.isSuccessful()) {
                    GroupResponse groups = response.body();
                    userGroups.addAll(groups.getGroups());
                    rcGroupList.setVisibility(View.VISIBLE);
                    groupListRowAdapter.addData(userGroups);
                    ivGhpSearch.setEnabled(true);
                    ivGhpSort.setEnabled(true);
                    mProgressBar.setVisibility(View.INVISIBLE);
                    rcGroupList.setVisibility(View.VISIBLE);
                } else {
                    Snackbar.make(rlGhpRoot, getString(R.string.Unknown_error), Snackbar.LENGTH_INDEFINITE).show();
                }
            }

            @Override
            public void onFailure(Call<GroupResponse> call, Throwable t) {
                mProgressBar.setVisibility(View.INVISIBLE);
                if (t instanceof NoConnectivityException) {
                    errorLayout.setVisibility(View.VISIBLE);
                    imNoInternet.setVisibility(View.VISIBLE);
                } else {
                    Log.e(TAG, t.getMessage());
                    ErrorUtils.connectivityError(HomeGroupListFragment.this, R.string.No_network, new NetworkErrorDialogListener() {
                        @Override
                        public void retryClicked() {
                            fetchGroupList();
                        }
                    });
                   // ErrorUtils.handleNetworkError(getContext(), rlGhpRoot, t);
                }
            }
        });
    }

    /*
    Separating this method from the above, because we will probably want it to call some kind of diff
    in time, rather than doing a full refresh, and don't need to worry about progress bar, etc
     */
    public void refreshGroupList() {
        GrassrootRestService.getInstance().getApi().getUserGroups(mobileNumber, userCode).enqueue(new Callback<GroupResponse>() {
            @Override
            public void onResponse(Call<GroupResponse> call, Response<GroupResponse> response) {
                if (response.isSuccessful()) {
                    Log.i(TAG, "Refreshing Group List ... call successful");
                    groupListRowAdapter.setGroupList(response.body().getGroups());
                } else {
                    Log.e(TAG, "Refreshing group list ... error! Here is the code: " + response.errorBody());
                }
                glSwipeRefresh.setRefreshing(false);
            }

            @Override
            public void onFailure(Call<GroupResponse> call, Throwable t) {
                ErrorUtils.handleNetworkError(getContext(), rlGhpRoot, t);
            }
        });
    }

    public void updateSingleGroup(final int position, final String groupUid) {
        if (position == -1)
            throw new UnsupportedOperationException("ERROR! This should not be called without a valid position");

        Group groupUpdated = userGroups.get(position);
        if (groupUpdated.getGroupUid().equals(groupUid)) {
            String mobileNumber = PreferenceUtils.getuser_mobilenumber(getContext());
            String code = PreferenceUtils.getuser_token(getContext());
            GrassrootRestService.getInstance().getApi().getSingleGroup(mobileNumber, code, groupUid)
                    .enqueue(new Callback<GroupResponse>() {
                        @Override
                        public void onResponse(Call<GroupResponse> call, Response<GroupResponse> response) {
                            // todo : check corner cases of filtered list (current list setup likely fragile)
                            // todo : consider shuffling this group to the top of the list
                            Group group = response.body().getGroups().get(0);
                            Log.e(TAG, "Group updated, has " + group.getGroupMemberCount() + " members");
                            userGroups.set(position, group);
                            groupListRowAdapter.updateGroup(position, group);
                        }

                        @Override
                        public void onFailure(Call<GroupResponse> call, Throwable t) {
                            ErrorUtils.handleNetworkError(getContext(), rlGhpRoot, t);
                        }
                    });
        } else {
            Log.e(TAG, "ERROR! Group position and ID do not match, not updating");
        }
    }

    // called after creating a group
    public void insertGroup(final int position) {
        // todo : actually add it, for now, just do a refresh
        Log.e(TAG, "refreshing groups!");
        refreshGroupList();
    }

    private void setUpRecyclerView() {
        rcGroupList.setLayoutManager(new LinearLayoutManager(getActivity()));
        rcGroupList.setItemAnimator(new CustomItemAnimator());
        groupListRowAdapter = new GroupListAdapter(new ArrayList<Group>(), HomeGroupListFragment.this);
        rcGroupList.setAdapter(groupListRowAdapter);
    }

    private void setUpSwipeRefresh() {
        glSwipeRefresh.setColorSchemeColors(getResources().getColor(R.color.primaryColor));
        glSwipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refreshGroupList();
            }
        });
    }

    @OnTextChanged(value = R.id.et_search, callback = OnTextChanged.Callback.AFTER_TEXT_CHANGED)
    public void searchStringChanged(CharSequence s) {
        String str = s.length() > 0 ? et_search.getText().toString() : "";
        String searchwords = str.toLowerCase(Locale.getDefault());
        filter(searchwords);
    }

    private void filter(String searchwords) {
        //first clear the current data
        Log.e(TAG, "filter search_string is " + searchwords);

        if (searchwords.equals("")) {
            groupListRowAdapter.setGroupList(userGroups);
        } else {
            final List<Group> filteredGroups = new ArrayList<>();

            for (Group group : userGroups) {
                if (group.getGroupName().trim().toLowerCase(Locale.getDefault()).contains(searchwords)) {
                    Log.e(TAG,"model.groupName.trim() " + group.getGroupName().trim().toLowerCase(Locale.getDefault()));
                    Log.e(TAG,"searchwords is " + searchwords);
                    filteredGroups.add(group);
                } else {
                    //Log.e(TAG,"not found");
                }
            }

            groupListRowAdapter.setGroupList(filteredGroups);
        }
    }

    @OnClick(R.id.iv_ghp_drawer)
    public void ivGhpDrawer() {
        mCallbacks.menuClick();
    }

    @OnClick(R.id.iv_ghp_search)
    public void ivGhpSearch() {
        rlSimple.setVisibility(View.GONE);
        rlSearch.setVisibility(View.VISIBLE);
    }

    @OnClick(R.id.iv_ghp_sort)
    public void ivGhpSort() {
        SortFragment sortFragment = new SortFragment();
        Bundle b = new Bundle();
        b.putBoolean("Date", date_click);
        b.putBoolean("Role", role_click);
        b.putBoolean("Default", defaults_click);
        sortFragment.setArguments(b);
        sortFragment.show(getFragmentManager(), "SortFragment");
        sortFragment.setListener(new SortInterface() {

            @Override
            public void tvDateClick(boolean date, boolean role, boolean defaults) {
                date_click = true;
                role_click = false;
                Long start = SystemClock.currentThreadTimeMillis();
                groupListRowAdapter.sortByDate();
                Log.d(TAG, String.format("sorting group list took %d msecs", SystemClock.currentThreadTimeMillis() - start));
            }

            @Override
            public void roleClick(boolean date, boolean role, boolean defaults) {
                date_click = false;
                role_click = true;
                Long start = SystemClock.currentThreadTimeMillis();
                groupListRowAdapter.sortByRole();
                Log.d(TAG, String.format("sorting group list took %d msecs", SystemClock.currentThreadTimeMillis() - start));
            }

            @Override
            public void defaultsClick(boolean date, boolean role, boolean defaults) {
                date_click = false;
                role_click = false;
                groupListRowAdapter.sortByDate();
            }
        });
    }


    @OnClick(R.id.iv_cross)
    public void ivCross() {
        if (et_search.getText().toString().isEmpty()) {
            rlSearch.setVisibility(View.GONE);
            rlSimple.setVisibility(View.VISIBLE);
            try {
                InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(getActivity().getCurrentFocus().getWindowToken(), 0);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            et_search.setText("");
        }
    }

    @OnClick(R.id.ic_fab_join_group)
    public void icFabJoinGroup() {
        menu1.close(true);
        Intent icFabJoinGroup = new Intent(getActivity(), GroupJoinActivity.class);
        startActivity(icFabJoinGroup);
    }

    @OnClick(R.id.ic_fab_start_group)
    public void icFabStartGroup() {
        menu1.close(true);
        Intent icFabStartGroup=new Intent(getActivity(), CreateGroupActivity.class);
        startActivityForResult(icFabStartGroup, Constant.activityCreateGroup);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.e(TAG,"results_for_Activity");
        if (resultCode == Activity.RESULT_OK && requestCode == Constant.activityCreateGroup) {
            Log.e(TAG, "got the result in the fragment, code : " + requestCode);
            insertGroup(0);
        }
            fetchGroupList();
            //todo

    }

    @OnClick({R.id.im_no_results, R.id.im_no_internet,R.id.im_server_error})
    public void onClick() {
        fetchGroupList();
    }

    public void addGroupRowShortClickListener(View element, final int position) {
        element.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                menu1.close(true);
                Intent openGroupTasks = MenuUtils.constructIntent(getActivity(), GroupTasksActivity.class,
                        userGroups.get(position).getGroupUid(), userGroups.get(position).getGroupName());
                startActivity(openGroupTasks);
            }
        });
    }

    public void addGroupRowLongClickListener(View element, final int position) {
        element.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Group dialog_model = userGroups.get(position);
                GroupQuickTaskModalFragment dialog = new GroupQuickTaskModalFragment();
                dialog.setGroupParameters(dialog_model.getGroupUid(), dialog_model.getGroupName());

                Bundle args = new Bundle();
                args.putBoolean("Meeting", dialog_model.getPermissions().contains("GROUP_PERMISSION_CREATE_GROUP_MEETING"));
                args.putBoolean("Vote", dialog_model.getPermissions().contains("GROUP_PERMISSION_CREATE_GROUP_VOTE"));
                args.putBoolean("ToDo", dialog_model.getPermissions().contains("GROUP_PERMISSION_CREATE_LOGBOOK_ENTRY"));
                dialog.setArguments(args);
                dialog.show(getFragmentManager(), "GroupQuickTaskModalFragment");
                return true;
            }
        });
    }

    public void addGroupRowMemberNumberClickListener(View element, final int position) {
        element.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Group grpMembership = userGroups.get(position);
                GroupQuickMemberModalFragment dialog = new GroupQuickMemberModalFragment();

                Bundle args = new Bundle();
                args.putString(Constant.GROUPUID_FIELD, grpMembership.getGroupUid());
                args.putString(Constant.GROUPNAME_FIELD, grpMembership.getGroupName());
                args.putInt(Constant.INDEX_FIELD, position);

                // todo: make these boolean getters in the grpMembership thing
                args.putBoolean("addMember", grpMembership.getPermissions().contains("GROUP_PERMISSION_ADD_GROUP_MEMBER"));
                args.putBoolean("viewMembers", grpMembership.getPermissions().contains("GROUP_PERMISSION_SEE_MEMBER_DETAILS"));
                args.putBoolean("editSettings", grpMembership.getPermissions().contains("GROUP_PERMISSION_UPDATE_GROUP_DETAILS"));
                args.putBoolean("removeMembers", grpMembership.getPermissions().contains("GROUP_PERMISSION_DELETE_GROUP_MEMBER"));

                dialog.setArguments(args);
                dialog.show(getFragmentManager(), "GroupQuickMemberModalFragment");
            }
        });
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallbacks = null;
        Log.e("onDetach", "Detached");
    }

    @Subscribe
    public void onEvent(NetworkActivityResultsEvent networkActivityResultsEvent){
        Log.e(TAG, "onEvent");
        fetchGroupList();


    }
}