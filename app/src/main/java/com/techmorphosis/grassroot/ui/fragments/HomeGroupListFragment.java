package com.techmorphosis.grassroot.ui.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.design.widget.Snackbar;
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
import com.techmorphosis.grassroot.R;
import com.techmorphosis.grassroot.adapters.GroupListAdapter;
import com.techmorphosis.grassroot.interfaces.SortInterface;
import com.techmorphosis.grassroot.services.GrassrootRestService;
import com.techmorphosis.grassroot.services.NoConnectivityException;
import com.techmorphosis.grassroot.services.model.Group;
import com.techmorphosis.grassroot.services.model.GroupResponse;
import com.techmorphosis.grassroot.ui.activities.CreateGroupActivity;
import com.techmorphosis.grassroot.ui.activities.GroupJoinActivity;
import com.techmorphosis.grassroot.ui.activities.GroupTasksActivity;
import com.techmorphosis.grassroot.ui.views.CustomItemAnimator;
import com.techmorphosis.grassroot.ui.views.SwipeableRecyclerViewTouchListener;
import com.techmorphosis.grassroot.utils.Constant;
import com.techmorphosis.grassroot.utils.ErrorUtils;
import com.techmorphosis.grassroot.utils.MenuUtils;
import com.techmorphosis.grassroot.utils.SettingPreference;
import com.techmorphosis.grassroot.utils.UtilClass;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
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
    private ArrayList<Group> groupList;

    public boolean date_click = false, role_click = false, defaults_click = false;

    private FragmentCallbacks mCallbacks;
    private GrassrootRestService grassrootRestService;

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
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
        setUpRecyclerView();
        updateAllUserGroups();
    }

    private void init() {
        grassrootRestService = new GrassrootRestService(this.getContext());
        groupList = new ArrayList<>();
        ivGhpSort.setEnabled(false);
        ivGhpSearch.setEnabled(false);

        menu1.setOnMenuToggleListener(new FloatingActionMenu.OnMenuToggleListener() {
            @Override
            public void onMenuToggle(boolean opened) {
                icFabJoinGroup.setVisibility(opened ? View.VISIBLE : View.GONE);
                icFabStartGroup.setVisibility(opened ? View.VISIBLE : View.GONE);
            }
        });
    }

    /**
     * Method executed to retrieve and populate list of groups. Note: this does not handle the absence
     * of a connection very well, at all. Will probably need to rethink.
     */
    private void updateAllUserGroups() {

        progressShow();

        String mobileNumber = SettingPreference.getuser_mobilenumber(getActivity());
        String code = SettingPreference.getuser_token(getActivity());

        Call<GroupResponse> call = grassrootRestService.getApi().getUserGroups(mobileNumber, code);
        call.enqueue(new Callback<GroupResponse>() {
            @Override
            public void onResponse(Call<GroupResponse> call, Response<GroupResponse> response) {
                if (response.isSuccessful()) {
                    GroupResponse groups = response.body();
                    groupList.addAll(groups.getGroups());
                    rcGroupList.setVisibility(View.VISIBLE);
                    groupListRowAdapter.addData(groupList);
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
                    ErrorUtils.handleNetworkError(getContext(), rlGhpRoot, t);
                }
            }
        });
    }

    public void updateSingleGroup(final int position, final String groupUid) {
        if (position == -1)
            throw new UnsupportedOperationException("ERROR! This should not be called without a valid position");

        Group groupUpdated = groupList.get(position);
        if (groupUpdated.getId().equals(groupUid)) {
            String mobileNumber = SettingPreference.getuser_mobilenumber(getContext());
            String code = SettingPreference.getuser_token(getContext());
            grassrootRestService.getApi().getSingleGroup(mobileNumber, code, groupUid)
                    .enqueue(new Callback<GroupResponse>() {
                        @Override
                        public void onResponse(Call<GroupResponse> call, Response<GroupResponse> response) {
                            // todo : check corner cases of filtered list (current list setup likely fragile)
                            // todo : consider shuffling this group to the top of the list
                            Group group = response.body().getGroups().get(0);
                            Log.e(TAG, "Group updated, has " + group.getGroupMemberCount() + " members");
                            groupList.set(position, group);
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

    private void progressShow() {
        mProgressBar.setVisibility(View.VISIBLE);
        rcGroupList.setVisibility(View.INVISIBLE);
        errorLayout.setVisibility(View.GONE);
        imNoInternet.setVisibility(View.GONE);
        imServerError.setVisibility(View.GONE);
        imNoResults.setVisibility(View.GONE);
    }

    private void setUpRecyclerView() {
        rcGroupList.setLayoutManager(new LinearLayoutManager(getActivity()));
        rcGroupList.setItemAnimator(new CustomItemAnimator());
        groupListRowAdapter = new GroupListAdapter(new ArrayList<Group>(), HomeGroupListFragment.this);
        rcGroupList.setAdapter(groupListRowAdapter);

        SwipeableRecyclerViewTouchListener swipeDeleteTouchListener = new SwipeableRecyclerViewTouchListener(
                getContext(),
                rcGroupList,
                R.id.task_card_view_root,
                R.id.main_background_view,

                new SwipeableRecyclerViewTouchListener.SwipeListener() {
                    @Override
                    public boolean canSwipe(int position) {
                        // todo: think this should go in onDismissed, more likely ...
                        try {
                            menu1.close(true);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        Intent blank = new Intent(getActivity(), GroupTasksActivity.class);
                        blank.putExtra("groupid", groupList.get(position).getId());
                        blank.putExtra("groupName", groupList.get(position).getGroupName());
                        startActivity(blank);
                        return false;
                    }

                    @Override
                    public void onDismissedBySwipe(RecyclerView recyclerView, int[] reverseSortedPositions) {
                        //Toast.makeText(getActivity(),"onDismissedBySwipe",Toast.LENGTH_LONG).show();
                    }
                });
        rcGroupList.addOnItemTouchListener(swipeDeleteTouchListener);
    }

    @OnTextChanged(value = R.id.et_search, callback = OnTextChanged.Callback.AFTER_TEXT_CHANGED)
    public void searchStringChanged(CharSequence s) {
        String str = s.length() > 0 ? et_search.getText().toString() : "";
        String searchwords = str.toLowerCase(Locale.getDefault());
        groupListRowAdapter.filter(searchwords);
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
        startActivity(icFabStartGroup);
    }

    @OnClick({R.id.im_no_results, R.id.im_no_internet,R.id.im_server_error})
    public void onClick() {
        updateAllUserGroups();
    }

    public void addGroupRowShortClickListener(View element, final int position) {
        element.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                menu1.close(true);
                Intent openGroupTasks = MenuUtils.constructIntent(getActivity(), GroupTasksActivity.class,
                        groupList.get(position).getId(), groupList.get(position).getGroupName());
                startActivity(openGroupTasks);
            }
        });
    }

    public void addGroupRowLongClickListener(View element, final int position) {
        element.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Group dialog_model = groupList.get(position);
                GroupQuickTaskModalFragment dialog = new GroupQuickTaskModalFragment();
                dialog.setGroupParameters(dialog_model.getId(), dialog_model.getGroupName());

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
                Group grpMembership = groupList.get(position);
                GroupQuickMemberModalFragment dialog = new GroupQuickMemberModalFragment();

                Bundle args = new Bundle();
                args.putString(Constant.GROUPUID_FIELD, grpMembership.getId());
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
}
