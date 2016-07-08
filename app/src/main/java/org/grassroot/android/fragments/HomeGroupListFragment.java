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
import android.text.TextUtils;
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
import org.grassroot.android.interfaces.GroupPickCallbacks;
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

public class HomeGroupListFragment extends android.support.v4.app.Fragment
    implements GroupListAdapter.GroupRowListener {

  private String TAG = HomeGroupListFragment.class.getSimpleName();

  private Unbinder unbinder;

  @BindView(R.id.rl_ghp_root) RelativeLayout rlGhpRoot;

  @BindView(R.id.gl_swipe_refresh) SwipeRefreshLayout glSwipeRefresh;
  @BindView(R.id.recycler_view) RecyclerView rcGroupList;

  private boolean floatingMenuOpen = false;
  @BindView(R.id.fab_menu_open) FloatingActionButton fabOpenMenu;
  @BindView(R.id.ll_fab_new_task) LinearLayout fabNewTask;
  @BindView(R.id.ll_fab_join_group) LinearLayout fabFindGroup;
  @BindView(R.id.ll_fab_start_group) LinearLayout fabStartGroup;

  ProgressDialog progressDialog;

    private GroupListAdapter groupListRowAdapter;
    private List<Group> userGroups;

    private boolean creating;
    public boolean date_click = false, role_click = false, defaults_click = false;

    private GroupPickCallbacks mCallbacks;

    @Override public void onAttach(Context context) {
        super.onAttach(context);
        Activity activity = (Activity) context;
        try {
            mCallbacks = (GroupPickCallbacks) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException("Activity must implement Fragment One.");
        }
    }

  @Override public void onActivityCreated(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    creating = true;
    Log.e(TAG, "onActivityCreated Called ... initiating");
    init();
    fetchGroupList();
  }

  @Override public void onResume() {
      super.onResume();
      fabOpenMenu.setVisibility(View.VISIBLE);
      creating = false;
  }

  private void init() {
    userGroups = new ArrayList<>();
    setUpRecyclerView();

    //first load from db
    showGroups(GroupService.getInstance().loadGroupsFromDB());
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
    rcGroupList.setVisibility(View.VISIBLE);
  }

  @Override public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.activity_group__homepage, container, false);
    unbinder = ButterKnife.bind(this, view);
    EventBus.getDefault().register(this);
    return view;
  }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        EventBus.getDefault().unregister(this);
        unbinder.unbind();
    }

    public void showSuccessMessage(Intent data) {
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
            groupListRowAdapter.setGroupList(GroupService.getInstance().userGroups);
          }

          @Override public void groupListLoadingError() {
            hideProgress();
          }
        });
  }

  /*
  Separating this method from the above, because we will probably want it to call some kind of diff
  in time, rather than doing a full refresh, and don't need to worry about progress bar, etc
   */
  public void refreshGroupList() {
    GroupService.getInstance()
        .refreshGroupList(getActivity(), new GroupService.GroupServiceListener() {
          @Override public void groupListLoaded() {
            groupListRowAdapter.setGroupList(GroupService.getInstance().getGroups());
            glSwipeRefresh.setRefreshing(false);
          }

          @Override public void groupListLoadingError() {
            glSwipeRefresh.setRefreshing(false);
          }
        });
  }

  public void updateSingleGroup(final int position, final String groupUid) {
    if (position == -1) {
      throw new UnsupportedOperationException(
          "ERROR! This should not be called without a valid position");
    }

    GroupService.getInstance()
        .refreshSingleGroup(position, groupUid, getActivity(),
            new GroupService.GroupServiceListener() {
              @Override public void groupListLoaded() {
                groupListRowAdapter.updateGroup(position,
                    GroupService.getInstance().userGroups.get(position));
              }

              @Override public void groupListLoadingError() {
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
    glSwipeRefresh.setColorSchemeColors(
        ContextCompat.getColor(getActivity(), R.color.primaryColor));
    glSwipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
      @Override public void onRefresh() {
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
        fabOpenMenu.setImageResource(R.drawable.ic_add_45d);
        fabNewTask.setVisibility(View.VISIBLE);
        fabFindGroup.setVisibility(View.VISIBLE);
        fabStartGroup.setVisibility(View.VISIBLE);
    }

    private void closeFloatingMenu() {
        floatingMenuOpen = false;
        fabOpenMenu.setImageResource(R.drawable.ic_add);
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
                fabOpenMenu.setVisibility(View.GONE);
                mCallbacks.groupPickerTriggered(taskType);
            }
        });
        modal.show(getFragmentManager(), QuickTaskModalFragment.class.getSimpleName());
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
    public void onGroupRowShortClick(Group group) {
        if (floatingMenuOpen) closeFloatingMenu();
        startActivity(MenuUtils.constructIntent(getActivity(), GroupTasksActivity.class, group));
    }

  @Override public void onGroupRowLongClick(Group group) {
    showQuickOptionsDialog(group);
  }

  private void showQuickOptionsDialog(final Group group) {
    QuickTaskModalFragment dialog = QuickTaskModalFragment.newInstance(true, group,
        new QuickTaskModalFragment.TaskModalListener() {
          @Override public void onTaskClicked(String taskType) {
            Intent i;
            switch (taskType) {
              case TaskConstants.MEETING:
                i = MenuUtils.constructIntent(getActivity(), CreateMeetingActivity.class,
                    group.getGroupUid(), group.getGroupName());
                break;
              case TaskConstants.VOTE:
                i = MenuUtils.constructIntent(getActivity(), CreateVoteActivity.class,
                    group.getGroupUid(), group.getGroupName());
                break;
              case TaskConstants.TODO:
                i = MenuUtils.constructIntent(getActivity(), CreateTodoActivity.class,
                    group.getGroupUid(), group.getGroupName());
                break;
              default:
                throw new UnsupportedOperationException("Error! Unknown task type");
            }
            startActivityForResult(i, Constant.activityCreateTask);
          }
        });
    dialog.show(getFragmentManager(), QuickTaskModalFragment.class.getSimpleName());
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
  }

  @Subscribe public void onEvent(NetworkActivityResultsEvent networkActivityResultsEvent) {
    fetchGroupList();
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

  @Subscribe public void onTaskCreatedEvent(TaskAddedEvent e) {
      Log.e(TAG, "group list fragment triggered by task addition ...");
      final TaskModel t = e.getTaskCreated();
      final String groupUid = t.getParentUid();
      // todo : may want to keep a hashmap of groups ... likely will be finding & updating groups quite a bit
      for (Group g : userGroups) {
          if (groupUid.equals(g.getGroupUid())) {
              Realm realm = Realm.getDefaultInstance();
              realm.beginTransaction();
              g.setHasTasks(true);
              realm.commitTransaction();
              realm.close();
              startActivity(MenuUtils.constructIntent(getActivity(), GroupTasksActivity.class, g));
          }
      }
  }

  public void sortGroups() {
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
        // todo : restore whatever was here
      }
    });
  }

    public void searchStringChanged(String query) {
        if (TextUtils.isEmpty(query)) {
            groupListRowAdapter.setGroupList(userGroups);
        } else {
            final List<Group> filteredGroups = new ArrayList<>();
            for (Group group : userGroups) {
                if (group.getGroupName().trim().toLowerCase(Locale.getDefault()).contains(query)) {
                    filteredGroups.add(group);
                }
            }
            groupListRowAdapter.setGroupList(filteredGroups);
        }
    }
}