package org.grassroot.android.fragments;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
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
import android.widget.RelativeLayout;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnTextChanged;
import com.github.clans.fab.FloatingActionButton;
import com.github.clans.fab.FloatingActionMenu;
import io.realm.Realm;
import io.realm.RealmList;
import io.realm.RealmResults;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.grassroot.android.R;
import org.grassroot.android.activities.CreateGroupActivity;
import org.grassroot.android.activities.GroupSearchActivity;
import org.grassroot.android.activities.GroupTasksActivity;
import org.grassroot.android.adapters.GroupListAdapter;
import org.grassroot.android.events.NetworkActivityResultsEvent;
import org.grassroot.android.events.TaskAddedEvent;
import org.grassroot.android.interfaces.GroupConstants;
import org.grassroot.android.interfaces.NetworkErrorDialogListener;
import org.grassroot.android.interfaces.SortInterface;
import org.grassroot.android.interfaces.TaskConstants;
import org.grassroot.android.models.Group;
import org.grassroot.android.models.GroupResponse;
import org.grassroot.android.models.TaskModel;
import org.grassroot.android.services.GrassrootRestService;
import org.grassroot.android.services.GroupService;
import org.grassroot.android.utils.Constant;
import org.grassroot.android.utils.ErrorUtils;
import org.grassroot.android.utils.MenuUtils;
import org.grassroot.android.utils.PreferenceUtils;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeGroupListFragment extends android.support.v4.app.Fragment
    implements GroupListAdapter.GroupRowListener {

  private String TAG = HomeGroupListFragment.class.getSimpleName();

  @BindView(R.id.rl_ghp_root) RelativeLayout rlGhpRoot;
  @BindView(R.id.iv_ghp_drawer) ImageView ivGhpDrawer;
  @BindView(R.id.iv_ghp_search) ImageView ivGhpSearch;
  @BindView(R.id.iv_ghp_sort) ImageView ivGhpSort;

  @BindView(R.id.gl_swipe_refresh) SwipeRefreshLayout glSwipeRefresh;
  @BindView(R.id.recycler_view) RecyclerView rcGroupList;

  @BindView(R.id.menu1) FloatingActionMenu menu1;
  @BindView(R.id.ic_fab_new_mtg) FloatingActionButton icFabNewMtg;
  @BindView(R.id.ic_fab_join_group) FloatingActionButton icFabJoinGroup;
  @BindView(R.id.ic_fab_start_group) FloatingActionButton icFabStartGroup;

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

  @Override public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.activity_group__homepage, container, false);
    ButterKnife.bind(this, view);
    EventBus.getDefault().register(this);
    return view;
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
    creating = false;
  }

  private void init() {

    userGroups = new ArrayList<>();
    ivGhpSort.setEnabled(false);
    ivGhpSearch.setEnabled(false);

    menu1.setOnMenuToggleListener(new FloatingActionMenu.OnMenuToggleListener() {
      @Override public void onMenuToggle(boolean opened) {
        icFabNewMtg.setVisibility(opened ? View.VISIBLE : View.GONE);
        icFabJoinGroup.setVisibility(opened ? View.VISIBLE : View.GONE);
        icFabStartGroup.setVisibility(opened ? View.VISIBLE : View.GONE);
      }
    });

    // Get a Realm instance for this thread
    realm = Realm.getDefaultInstance();

    setUpRecyclerView();
    setUpSwipeRefresh();

    //first load from db
    showGroups(loadGroupsFromDB());
  }

  public void showSuccessMessage(Intent data) {
    // todo : update the group's card
    Log.e(TAG, "and ... it's done");
    String message = data.getStringExtra(Constant.SUCCESS_MESSAGE);
    ErrorUtils.showSnackBar(rlGhpRoot, message, Snackbar.LENGTH_LONG, "", null);
  }

  /**
   * Method executed to retrieve and populate list of groups. Note: this does not handle the
   * absence
   * of a connection very well, at all. Will probably need to rethink.
   */
  public void fetchGroupList() {
    showProgress();
    GroupService.getInstance()
        .fetchGroupList(getActivity(), rlGhpRoot, new GroupService.GroupServiceListener() {
          @Override public void groupListLoaded() {
            hideProgress();
            rcGroupList.setVisibility(View.VISIBLE);
            groupListRowAdapter.addData(GroupService.getInstance().userGroups);
            ivGhpSearch.setEnabled(true);
            ivGhpSort.setEnabled(true);
            rcGroupList.setVisibility(View.VISIBLE);
          }

          @Override public void groupListLoadingError() {
            hideProgress();
          }
        });
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

  private void showGroups(RealmList<Group> groups) {
    userGroups = new ArrayList<>(groups);
    rcGroupList.setVisibility(View.VISIBLE);
    groupListRowAdapter.setGroupList(userGroups);
    ivGhpSearch.setEnabled(true);
    ivGhpSort.setEnabled(true);
    rcGroupList.setVisibility(View.VISIBLE);
  }

  private void saveGroupsInDB(RealmList<Group> groups) {
    if (groups != null && realm != null && !realm.isClosed()) {
      realm.beginTransaction();
      realm.copyToRealmOrUpdate(groups);
      realm.commitTransaction();
    }
  }

  private RealmList<Group> loadGroupsFromDB() {
    RealmList<Group> groups = new RealmList<>();
    if (realm != null && !realm.isClosed()) {
      RealmResults<Group> results = realm.where(Group.class).findAll();
      groups.addAll(results.subList(0, results.size()));
    }
    return groups;
  }

  /*
  Separating this method from the above, because we will probably want it to call some kind of diff
  in time, rather than doing a full refresh, and don't need to worry about progress bar, etc
   */
  public void refreshGroupList() {

    GroupService.getInstance()
        .refreshGroupList(getActivity(), new GroupService.GroupServiceListener() {
              @Override public void groupListLoaded() {
                groupListRowAdapter.setGroupList(GroupService.getInstance().userGroups);
                glSwipeRefresh.setRefreshing(false);
              }

              @Override public void groupListLoadingError() {
                glSwipeRefresh.setRefreshing(false);
                ErrorUtils.connectivityError(getActivity(), R.string.error_no_network,
                    new NetworkErrorDialogListener() {
                      @Override public void retryClicked() {
                        refreshGroupList();
                      }
                    });
              }
            }
        );
  }

  public void updateSingleGroup(final int position, final String groupUid) {
    if (position == -1) {
      throw new UnsupportedOperationException(
          "ERROR! This should not be called without a valid position");
    }

    Group groupUpdated = userGroups.get(position);
    if (groupUpdated.getGroupUid().equals(groupUid)) {
      String mobileNumber = PreferenceUtils.getUserPhoneNumber(getContext());
      String code = PreferenceUtils.getAuthToken(getContext());
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
              groupListRowAdapter.updateGroup(position, group);
            }

            @Override public void onFailure(Call<GroupResponse> call, Throwable t) {
              ErrorUtils.connectivityError(getActivity(), R.string.error_no_network,
                  new NetworkErrorDialogListener() {
                    @Override public void retryClicked() {
                      updateSingleGroup(position, groupUid);
                    }
                  });
            }
          });
    } else {
      Log.e(TAG, "ERROR! Group position and ID do not match, not updating");
    }
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
  }

  private void setUpSwipeRefresh() {
    glSwipeRefresh.setColorSchemeColors(
        ContextCompat.getColor(getActivity(), R.color.primaryColor));
    glSwipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
      @Override public void onRefresh() {
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
          Log.e(TAG, "model.groupName.trim() " + group.getGroupName()
              .trim()
              .toLowerCase(Locale.getDefault()));
          Log.e(TAG, "searchwords is " + searchwords);
          filteredGroups.add(group);
        } else {
          //Log.e(TAG,"not found");
        }
      }

      groupListRowAdapter.setGroupList(filteredGroups);
    }
  }

  @OnClick(R.id.iv_ghp_drawer) public void ivGhpDrawer() {
    mCallbacks.menuClick();
  }

  @OnClick(R.id.iv_ghp_search) public void ivGhpSearch() {
    rlSimple.setVisibility(View.GONE);
    rlSearch.setVisibility(View.VISIBLE);
  }

  @OnClick(R.id.iv_ghp_sort) public void ivGhpSort() {
    SortFragment sortFragment = new SortFragment();
    Bundle b = new Bundle();
    b.putBoolean("Date", date_click);
    b.putBoolean("Role", role_click);
    b.putBoolean("Default", defaults_click);
    sortFragment.setArguments(b);
    sortFragment.show(getFragmentManager(), "SortFragment");
    sortFragment.setListener(new SortInterface() {

      @Override public void tvDateClick(boolean date, boolean role, boolean defaults) {
        date_click = true;
        role_click = false;
        Long start = SystemClock.currentThreadTimeMillis();
        groupListRowAdapter.sortByDate();
        Log.d(TAG, String.format("sorting group list took %d msecs",
            SystemClock.currentThreadTimeMillis() - start));
      }

      @Override public void roleClick(boolean date, boolean role, boolean defaults) {
        date_click = false;
        role_click = true;
        Long start = SystemClock.currentThreadTimeMillis();
        groupListRowAdapter.sortByRole();
        Log.d(TAG, String.format("sorting group list took %d msecs",
            SystemClock.currentThreadTimeMillis() - start));
      }

      @Override public void defaultsClick(boolean date, boolean role, boolean defaults) {
        date_click = false;
        role_click = false;
        groupListRowAdapter.sortByDate();
      }
    });
  }

  @OnClick(R.id.iv_cross) public void ivCross() {
    if (et_search.getText().toString().isEmpty()) {
      rlSearch.setVisibility(View.GONE);
      rlSimple.setVisibility(View.VISIBLE);
      try {
        InputMethodManager imm =
            (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(getActivity().getCurrentFocus().getWindowToken(), 0);
      } catch (Exception e) {
        e.printStackTrace();
      }
    } else {
      et_search.setText("");
    }
  }

  @OnClick(R.id.ic_fab_new_mtg) public void icFabCallMeeting() {
    mCallbacks.groupPickerTriggered(TaskConstants.MEETING);
    menu1.close(true);
  }

  @OnClick(R.id.ic_fab_join_group) public void icFabJoinGroup() {
    menu1.close(true);
    Intent icFabJoinGroup = new Intent(getActivity(), GroupSearchActivity.class);
    startActivity(icFabJoinGroup);
  }

  @OnClick(R.id.ic_fab_start_group) public void icFabStartGroup() {
    menu1.close(true);
    Intent icFabStartGroup = new Intent(getActivity(), CreateGroupActivity.class);
    startActivityForResult(icFabStartGroup, Constant.activityCreateGroup);
  }

  @Override public void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (resultCode == Activity.RESULT_OK && requestCode == Constant.activityCreateGroup) {
      Group createdGroup = data.getParcelableExtra(GroupConstants.OBJECT_FIELD);
      Log.d(TAG, "createdGroup returned! with UID: " + createdGroup);
      insertGroup(0, createdGroup);
    }
  }

  @Override public void onGroupRowShortClick(Group group) {
    menu1.close(true);
    startActivity(MenuUtils.constructIntent(getActivity(), GroupTasksActivity.class, group));
  }

  @Override public void onGroupRowLongClick(Group group) {
    showQuickOptionsDialog(group, false);
  }

  private void showQuickOptionsDialog(Group group, boolean addMembersOption) {
    GroupQuickTaskModalFragment dialog = new GroupQuickTaskModalFragment();
    dialog.setGroupParameters(group.getGroupUid(), group.getGroupName());
    Bundle args = new Bundle();
    args.putBoolean(TaskConstants.MEETING,
        group.getPermissions().contains(GroupConstants.PERM_CREATE_MTG));
    args.putBoolean(TaskConstants.VOTE,
        group.getPermissions().contains(GroupConstants.PERM_CALL_VOTE));
    args.putBoolean(TaskConstants.TODO,
        group.getPermissions().contains(GroupConstants.PERM_CREATE_TODO));
    args.putBoolean(GroupConstants.PERM_ADD_MEMBER, addMembersOption && group.canAddMembers());
    dialog.setArguments(args);
    dialog.show(getFragmentManager(), GroupQuickTaskModalFragment.class.getSimpleName());
  }

  @Override public void onGroupRowMemberClick(Group group, int position) {
    GroupQuickMemberModalFragment dialog = new GroupQuickMemberModalFragment();
    Bundle args = new Bundle();
    args.putParcelable(GroupConstants.OBJECT_FIELD, group);
    args.putInt(Constant.INDEX_FIELD, position);
    dialog.setArguments(args);
    dialog.show(getFragmentManager(), "GroupQuickMemberModalFragment");
  }

  @Override public void onDetach() {
    super.onDetach();
    mCallbacks = null;
    Log.e("onDetach", "Detached");
  }

  @Subscribe public void onEvent(NetworkActivityResultsEvent networkActivityResultsEvent) {
    Log.e(TAG, "onEvent");
    fetchGroupList();
  }

  @Subscribe public void onTaskCreatedEvent(TaskAddedEvent e) {
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

  @Override public void onDestroy() {
    if (realm != null && !realm.isClosed()) {
      realm.close();
    }
    super.onDestroy();
  }
}