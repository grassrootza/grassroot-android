package org.grassroot.android.fragments;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.grassroot.android.R;
import org.grassroot.android.activities.CreateGroupActivity;
import org.grassroot.android.activities.CreateMeetingActivity;
import org.grassroot.android.activities.CreateTodoActivity;
import org.grassroot.android.activities.CreateVoteActivity;
import org.grassroot.android.activities.GroupSearchActivity;
import org.grassroot.android.activities.GroupTasksActivity;
import org.grassroot.android.adapters.GroupListAdapter;
import org.grassroot.android.events.NetworkActivityResultsEvent;
import org.grassroot.android.events.TaskAddedEvent;
import org.grassroot.android.interfaces.GroupConstants;
import org.grassroot.android.interfaces.SortInterface;
import org.grassroot.android.interfaces.TaskConstants;
import org.grassroot.android.models.Group;
import org.grassroot.android.models.TaskModel;
import org.grassroot.android.services.GroupService;
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
import butterknife.Unbinder;
import io.realm.Realm;
import io.realm.RealmList;
import io.realm.RealmResults;

public class HomeGroupListFragment extends android.support.v4.app.Fragment implements GroupListAdapter.GroupRowListener {

    private String TAG = HomeGroupListFragment.class.getSimpleName();

    private Unbinder unbinder;

    @BindView(R.id.rl_ghp_root) RelativeLayout rlGhpRoot;
    @BindView(R.id.iv_ghp_drawer) ImageView ivGhpDrawer;
    @BindView(R.id.ghp_title) TextView tvTitle;

    @BindView(R.id.iv_ghp_search) ImageView ivGhpSearch;
    @BindView(R.id.iv_ghp_sort) ImageView ivGhpSort;

    @BindView(R.id.gl_swipe_refresh) SwipeRefreshLayout glSwipeRefresh;
    @BindView(R.id.recycler_view) RecyclerView rcGroupList;

    private boolean floatingMenuOpen = false;
    @BindView(R.id.fab_menu_open) FloatingActionButton fabOpenMenu;
    @BindView(R.id.ll_fab_new_task) LinearLayout fabNewTask;
    @BindView(R.id.ll_fab_join_group) LinearLayout fabFindGroup;
    @BindView(R.id.ll_fab_start_group) LinearLayout fabStartGroup;

    @BindView(R.id.iv_cross) ImageView ivCross;
    @BindView(R.id.et_search) EditText et_search;
    @BindView(R.id.rl_search) RelativeLayout rlSearch;
    @BindView(R.id.rl_simple) RelativeLayout rlSimple;

    ProgressDialog progressDialog;

    Realm realm;

    private GroupListAdapter groupListRowAdapter;
    private List<Group> userGroups;

    private boolean creating;
    public boolean date_click = false, role_click = false, defaults_click = false;

    private GroupListFragmentListener mCallbacks;

    public interface GroupListFragmentListener {
        void menuClick();
        void groupPickerTriggered(String taskType);
    }

    @Override public void onAttach(Context context) {
        super.onAttach(context);
        Activity activity = (Activity) context;
        try {
            mCallbacks = (GroupListFragmentListener) activity;
            Log.e("onAttach", "Attached");
        } catch (ClassCastException e) {
            throw new ClassCastException("Activity must implement Fragment One.");
        }
    }

  @Override public void onActivityCreated(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    creating = true;
    init();
    fetchGroupList();
  }

  @Override public void onResume() {
    super.onResume();
    if (!creating && PreferenceUtils.getGroupListMustRefresh(getContext())) {
      refreshGroupList();
      PreferenceUtils.setGroupListMustBeRefreshed(getContext(), false);
    }
      setActionBarToDefault();
    creating = false;
  }

  private void init() {

    userGroups = new ArrayList<>();
    ivGhpSort.setEnabled(false);
    ivGhpSearch.setEnabled(false);

    // Get a Realm instance for this thread
    realm = Realm.getDefaultInstance();

    setUpRecyclerView();

    //first load from db
    showGroups(loadGroupsFromDB());
  }

  /**
   * Method executed to retrieve and populate list of groups. Note: this does not handle the
   * absence
   * of a connection very well, at all. Will probably need to rethink.
   */

  private void showGroups(RealmList<Group> groups) {
    userGroups = new ArrayList<>(groups);
    rcGroupList.setVisibility(View.VISIBLE);
    groupListRowAdapter.setGroupList(userGroups);
    ivGhpSearch.setEnabled(true);
    ivGhpSort.setEnabled(true);
    rcGroupList.setVisibility(View.VISIBLE);
  }

  private RealmList<Group> loadGroupsFromDB() {
    RealmList<Group> groups = new RealmList<>();
    if (realm != null && !realm.isClosed()) {
      RealmResults<Group> results = realm.where(Group.class).findAll();
      groups.addAll(results.subList(0, results.size()));
    }
    return groups;
  }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_group__homepage, container, false);
        unbinder = ButterKnife.bind(this, view);
        EventBus.getDefault().register(this);
        toggleClickableTitle(true);
        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }

    public void showSuccessMessage(Intent data) {
        String message = data.getStringExtra(Constant.SUCCESS_MESSAGE);
        ErrorUtils.showSnackBar(rlGhpRoot, message, Snackbar.LENGTH_LONG, "", null);
    }

    /**
     * Method executed to retrieve and populate list of groups. Note: this does not handle the absence
     * of a connection very well, at all. Will probably need to rethink.
     */
    public void fetchGroupList() {
        showProgress();
        GroupService.getInstance().fetchGroupList(getActivity(), rlGhpRoot, new GroupService.GroupServiceListener() {
            @Override
            public void groupListLoaded() {
                hideProgress();
                rcGroupList.setVisibility(View.VISIBLE);
                groupListRowAdapter.addData(GroupService.getInstance().userGroups); // todo : instead draw straight from Realm
                ivGhpSearch.setEnabled(true);
                ivGhpSort.setEnabled(true);
                rcGroupList.setVisibility(View.VISIBLE);
            }

            @Override
            public void groupListLoadingError() {
                hideProgress();
            }
        });
    }

    /*
    Separating this method from the above, because we will probably want it to call some kind of diff
    in time, rather than doing a full refresh, and don't need to worry about progress bar, etc
     */
    public void refreshGroupList() {
        GroupService.getInstance().refreshGroupList(getActivity(), new GroupService.GroupServiceListener() {
            @Override
            public void groupListLoaded() {
                groupListRowAdapter.setGroupList(GroupService.getInstance().userGroups);
                glSwipeRefresh.setRefreshing(false);
              }

              @Override public void groupListLoadingError() {
                glSwipeRefresh.setRefreshing(false);
            }
        });
    }

    public void updateSingleGroup(final int position, final String groupUid) {
        if (position == -1)
            throw new UnsupportedOperationException("ERROR! This should not be called without a valid position");

        GroupService.getInstance().refreshSingleGroup(position, groupUid, getActivity(), new GroupService.GroupServiceListener() {
            @Override
            public void groupListLoaded() {
                groupListRowAdapter.updateGroup(position, GroupService.getInstance().userGroups.get(position));
            }

            @Override
            public void groupListLoadingError() {
                // todo : handle this better
                Log.e(TAG, "ERROR! Group position and ID do not match, not updating");
            }
        });
    }

    public void insertGroup(final int position, final Group group) {
        // todo : actually add it, for now, just do a refresh
        Log.e(TAG, "adding a group! at position " + position + ", the group looks like : " + group);
        groupListRowAdapter.addGroup(0, group);
    }

    private void setUpRecyclerView() {
        rcGroupList.setLayoutManager(new LinearLayoutManager(getActivity()));
        groupListRowAdapter = new GroupListAdapter(new ArrayList<Group>(), HomeGroupListFragment.this);
        rcGroupList.setAdapter(groupListRowAdapter);
        glSwipeRefresh.setColorSchemeColors(ContextCompat.getColor(getActivity(), R.color.primaryColor));
        glSwipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refreshGroupList();
            }
        });
    }

    @OnClick(R.id.fab_menu_open)
    public void toggleFloatingMenu() {
        if (!floatingMenuOpen) {
            openFloatingMenu();
        } else {
            closeFloatingMenu();
        }
    }

    private void openFloatingMenu() {
        floatingMenuOpen = true;
        fabNewTask.setVisibility(View.VISIBLE);
        fabFindGroup.setVisibility(View.VISIBLE);
        fabStartGroup.setVisibility(View.VISIBLE);
    }

    private void closeFloatingMenu() {
        floatingMenuOpen = false;
        fabNewTask.setVisibility(View.GONE);
        fabFindGroup.setVisibility(View.GONE);
        fabStartGroup.setVisibility(View.GONE);
    }

    @OnClick(R.id.ic_fab_new_task)
    public void icFabNewTask() {
        closeFloatingMenu();
        QuickTaskModalFragment modal = QuickTaskModalFragment.newInstance(false, null, new QuickTaskModalFragment.TaskModalListener() {
            @Override
            public void onTaskClicked(String taskType) {
                switchActionBarToPicker();
                mCallbacks.groupPickerTriggered(taskType);
            }
        });
        modal.show(getFragmentManager(), QuickTaskModalFragment.class.getSimpleName());
    }

    public void switchActionBarToPicker() {
        fabOpenMenu.setVisibility(View.GONE);
        tvTitle.setText(R.string.home_group_pick);
        ivGhpDrawer.setImageResource(R.drawable.btn_close_white);
        ivGhpDrawer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setActionBarToDefault();
                getFragmentManager().popBackStack();
            }
        });
        toggleClickableTitle(false);
    }

    public void setActionBarToDefault() {
        fabOpenMenu.setVisibility(View.VISIBLE);
        tvTitle.setText(R.string.ghp_toolbar_title);
        ivGhpDrawer.setImageResource(R.drawable.btn_navigation);
        ivGhpDrawer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCallbacks.menuClick();
            }
        });
        toggleClickableTitle(true);
    }

    public void toggleClickableTitle(boolean clickable) {
        if (clickable) {
            tvTitle.setClickable(true);
            tvTitle.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    icFabNewTask();
                }
            });
        } else {
            tvTitle.setClickable(false);
        }
    }

    @OnClick(R.id.ic_fab_join_group)
    public void icFabJoinGroup() {
        closeFloatingMenu();
        Intent icFabJoinGroup = new Intent(getActivity(), GroupSearchActivity.class);
        startActivity(icFabJoinGroup);
    }

    @OnClick(R.id.ic_fab_start_group)
    public void icFabStartGroup() {
        closeFloatingMenu();
        Intent icFabStartGroup=new Intent(getActivity(), CreateGroupActivity.class);
        startActivityForResult(icFabStartGroup, Constant.activityCreateGroup);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK && requestCode == Constant.activityCreateGroup) {
            Group createdGroup = data.getParcelableExtra(GroupConstants.OBJECT_FIELD);
            Log.d(TAG, "createdGroup returned! with UID: " + createdGroup);
            insertGroup(0, createdGroup);
        }
    }

    @Override
    public void onGroupRowShortClick(Group group) {
        if (floatingMenuOpen) closeFloatingMenu();
        startActivity(MenuUtils.constructIntent(getActivity(), GroupTasksActivity.class, group));
    }

    @Override
    public void onGroupRowLongClick(Group group) {
        showQuickOptionsDialog(group, false);
    }

    private void showQuickOptionsDialog(final Group group, boolean addMembersOption) {
        QuickTaskModalFragment dialog = QuickTaskModalFragment.newInstance(true, group, new QuickTaskModalFragment.TaskModalListener() {
            @Override
            public void onTaskClicked(String taskType) {
                Intent i;
                switch (taskType) {
                    case TaskConstants.MEETING:
                        i = MenuUtils.constructIntent(getActivity(), CreateMeetingActivity.class, group.getGroupUid(), group.getGroupName());
                        break;
                    case TaskConstants.VOTE:
                        i = MenuUtils.constructIntent(getActivity(), CreateVoteActivity.class, group.getGroupUid(), group.getGroupName());
                        break;
                    case TaskConstants.TODO:
                        i = MenuUtils.constructIntent(getActivity(), CreateTodoActivity.class, group.getGroupUid(), group.getGroupName());
                        break;
                    default:
                        throw new UnsupportedOperationException("Error! Unknown task type");
                }
                startActivityForResult(i, Constant.activityCreateTask);

            }
        });
        dialog.show(getFragmentManager(), QuickTaskModalFragment.class.getSimpleName());
    }

    @Override
    public void onGroupRowMemberClick(Group group, int position) {
        GroupQuickMemberModalFragment dialog = new GroupQuickMemberModalFragment();
        Bundle args = new Bundle();
        args.putParcelable(GroupConstants.OBJECT_FIELD, group);
        args.putInt(Constant.INDEX_FIELD, position);
        dialog.setArguments(args);
        dialog.show(getFragmentManager(), "GroupQuickMemberModalFragment");
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallbacks = null;
        unbinder.unbind();
    }

    @Subscribe
    public void onEvent(NetworkActivityResultsEvent networkActivityResultsEvent){
        fetchGroupList();
    }

    @Subscribe
    public void onTaskCreatedEvent(TaskAddedEvent e) {
        Log.e(TAG, "group list fragment triggered by task addition ...");
        final TaskModel t = e.getTaskCreated();
        final String groupUid = t.getParentUid();
        // todo : may want to keep a hashmap of groups ... likely will be finding & updating groups quite a bit
        for (Group g : userGroups) {
            if (groupUid.equals(g.getGroupUid())) {
                g.setHasTasks(true);
                startActivity(MenuUtils.constructIntent(getActivity(), GroupTasksActivity.class, g));
            }
        }
    }

    private void showProgress() {
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(getContext());
            progressDialog.setIndeterminate(true);
            progressDialog.setMessage(getString(R.string.txt_pls_wait));
        }
        progressDialog.show();
    }

    private void hideProgress() {
        progressDialog.dismiss();
    }

    /*
    SECTION : search methods
     */
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
                    Log.e(TAG, "model.groupName.trim() " + group.getGroupName().trim().toLowerCase(Locale.getDefault()));
                    Log.e(TAG, "searchwords is " + searchwords);
                    filteredGroups.add(group);
                } else {
                    //Log.e(TAG,"not found");
                }
            }

            groupListRowAdapter.setGroupList(filteredGroups);
        }
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
                et_search.setText("");
            }

            @Override
            public void defaultsClick(boolean date, boolean role, boolean defaults) {
                // todo : restore whatever was here
            }
        });
    }

}