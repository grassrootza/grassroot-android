package org.grassroot.android.fragments;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.grassroot.android.R;
import org.grassroot.android.adapters.TasksAdapter;
import org.grassroot.android.events.TaskAddedEvent;
import org.grassroot.android.events.TaskCancelledEvent;
import org.grassroot.android.events.TaskUpdatedEvent;
import org.grassroot.android.fragments.dialogs.ConfirmCancelDialogFragment;
import org.grassroot.android.interfaces.TaskConstants;
import org.grassroot.android.models.Group;
import org.grassroot.android.models.TaskModel;
import org.grassroot.android.services.ApplicationLoader;
import org.grassroot.android.services.TaskService;
import org.grassroot.android.utils.ErrorUtils;
import org.grassroot.android.utils.NetworkUtils;
import org.grassroot.android.utils.RealmUtils;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;

/**
 * Created by luke on 2016/05/13.
 */
public class TaskListFragment extends Fragment implements TasksAdapter.TaskListListener {

  private static final String TAG = TaskListFragment.class.getSimpleName();

  TaskListListener listener;
  private TasksAdapter tasksAdapter;

  private String groupUid;
  private Group group;

  private boolean isInNoTaskMessageView;
  private boolean hasFetchedFromServer;

  private Unbinder unbinder;

  @BindView(R.id.rl_task_list_root) ViewGroup rootView;
  @BindView(R.id.tl_swipe_refresh) SwipeRefreshLayout swipeRefreshLayout;
  @BindView(R.id.tl_recycler_view) RecyclerView taskView;
  @BindView(R.id.tl_fab) FloatingActionButton floatingActionButton;

  @BindView(R.id.tl_no_task_message) RelativeLayout noTaskMessageLayout;
  @BindView(R.id.tl_no_task_text) TextView noTaskMessageText;

  // sequence meeting -> vote -> to-do must match string array
  final CharSequence[] filterOptions = ApplicationLoader.applicationContext
      .getResources().getStringArray(R.array.tasks_filter_options);
  private boolean[] filtersChecked = { false, false, false };

  @BindView(R.id.progressBar) ProgressBar progressBar;

  public interface TaskListListener {
    void onTaskLoaded(String taskName);
    void onTaskLoaded(int position, String taskUid, String taskType, String taskTitle) ;
    void onFabClicked();
  }

    /*
    SECTION : SET UP VIEWS AND POPULATE THE LIST
     */

  // pass null if this is a group-neutral task fragment
  public static TaskListFragment newInstance(String parentUid, TaskListListener listener) {

    TaskListFragment fragment = new TaskListFragment();
    fragment.groupUid = parentUid;
    fragment.listener = listener;

    return fragment;
  }

  @Override
  public void onAttach(Context context) {
    super.onAttach(context);

    if (groupUid != null) {
      group = RealmUtils.loadGroupFromDB(groupUid);
    }

    EventBus.getDefault().register(this);
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setHasOptionsMenu(true);
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {

    View viewToReturn = inflater.inflate(R.layout.fragment_task_list, container, false);
    unbinder = ButterKnife.bind(this, viewToReturn);

    tasksAdapter = new TasksAdapter(new ArrayList<TaskModel>(), TextUtils.isEmpty(groupUid), this);
    taskView.setAdapter(tasksAdapter);
    taskView.setLayoutManager(new LinearLayoutManager(getActivity()));
    taskView.setHasFixedSize(true);
    taskView.setDrawingCacheEnabled(true);


    loadTasksOnCreateView();
    return viewToReturn;
  }

  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    swipeRefreshLayout.setColorSchemeColors(ContextCompat.getColor(getActivity(), R.color.primaryColor));
    swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
      @Override public void onRefresh() {
        refreshTasksFromServer(false);
      }
    });
  }

  @Override
  public void onResume() {
    super.onResume();
    floatingActionButton.setVisibility(group == null || group.hasCreatePermissions() ? View.VISIBLE : View.GONE);
  }

  @Override
  public void onPrepareOptionsMenu(Menu menu) {
    super.onPrepareOptionsMenu(menu);
    if (menu.findItem(R.id.action_search) != null)
      menu.findItem(R.id.action_search).setVisible(!isInNoTaskMessageView);
    if (menu.findItem(R.id.mi_icon_filter) != null)
      menu.findItem(R.id.mi_icon_filter).setVisible(!isInNoTaskMessageView);
    if (menu.findItem(R.id.mi_icon_sort) != null)
      menu.findItem(R.id.mi_icon_sort).setVisible(false);
    if (menu.findItem(R.id.mi_share_default) != null)
      menu.findItem(R.id.mi_share_default).setVisible(false);
    if (menu.findItem(R.id.mi_only_unread) != null)
      menu.findItem(R.id.mi_only_unread).setVisible(false);
    if (menu.findItem(R.id.mi_refresh_screen) != null)
      menu.findItem(R.id.mi_refresh_screen).setVisible(true);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.mi_refresh_screen:
        refreshTasksFromServer(true);
        return true;
      default:
        return super.onOptionsItemSelected(item);
    }
  }

  @Override public void onDestroyView() {
    super.onDestroyView();
    unbinder.unbind();
    // want to keep adapter updated in background (not always clear when destroy view called vs detach),
    // so don't unregister from eventbus here, though avoid unchecked calls to views
  }

  @Override public void onDetach() {
    super.onDetach();
    EventBus.getDefault().unregister(this);
    // note : setting listener to null creates risks of null pointer errors if frequent fragment swapping
    // and Android stack management is an issue ... creates slight risk of memory leak, but that's a trade off
  }

  @OnClick(R.id.tl_fab)
  public void activateNewTask() {
    listener.onFabClicked();
  }

  private void loadTasksOnCreateView() {
    if (hasTasksInDB()) {
      loadTasksFromDB(NetworkUtils.FETCHED_CACHE);
      refreshTasksFromServer(false);
    } else {
      showProgress();
      TaskService.getInstance().fetchTasks(groupUid, AndroidSchedulers.mainThread()).subscribe(new Action1<String>() {
        @Override
        public void call(String s) {
          hasFetchedFromServer = NetworkUtils.FETCHED_SERVER.equals(s);
          loadTasksFromDB(s);
        }
      });
    }
  }

  private boolean hasTasksInDB() {
    if (TextUtils.isEmpty(groupUid)) {
      return RealmUtils.countUpcomingTasksInDB() > 0;
    } else {
      return RealmUtils.countGroupTasksInDB(groupUid) > 0;
    }
  }

  /*
  SECTION : LOGIC FOR FETCHING AND DISPLAYING TASKS
   */

  // a null or empty groupUid passed through, tells the service to fetch all upcoming tasks, across groups
  private void refreshTasksFromServer(boolean forceShowRefreshing) {
    if (forceShowRefreshing && swipeRefreshLayout != null) {
      swipeRefreshLayout.setRefreshing(true);
    }
    TaskService.getInstance().fetchTasks(groupUid, null).subscribe(new Action1<String>() {
      @Override
      public void call(String s) {
        loadTasksFromDB(s);
      }
    });
  }

  private void loadTasksFromDB(final String latestFetchType) {
    boolean fetchedFromServer = NetworkUtils.FETCHED_SERVER.equals(latestFetchType);
    hasFetchedFromServer = fetchedFromServer || hasFetchedFromServer;
    Observable<List<TaskModel>> loadTasks = TextUtils.isEmpty(groupUid) ?
        RealmUtils.loadUpcomingTasks() : RealmUtils.loadTasksSorted(groupUid);
    loadTasks.subscribe(new Action1<List<TaskModel>>() {
      @Override
      public void call(List<TaskModel> taskModels) {
        if (taskModels.isEmpty()) {
          handleNoTasksFound(latestFetchType);
        } else if (taskView != null) { // to catch delayed call backs when user has left fragment
          if (isInNoTaskMessageView) {
            switchOffNoTasks();
          }
          tasksAdapter.refreshTaskList(taskModels).subscribe(new Action1<Boolean>() {
            @Override
            public void call(Boolean aBoolean) {
              tasksAdapter.notifyDataSetChanged();
              taskView.setVisibility(View.VISIBLE);
            }
          });
        }
        hideProgress();
      }
    });
  }

  private void switchOffNoTasks() {
    if (noTaskMessageLayout != null && noTaskMessageLayout.getVisibility() == View.VISIBLE) {
      noTaskMessageLayout.setVisibility(View.GONE);
    }
    isInNoTaskMessageView = false;
  }

  private void handleNoTasksFound(final String fetchType) {
    // since the call may time out / return when the user is on a different fragment, have to check for null on all these

    if (taskView != null) {
      taskView.setVisibility(View.GONE);
    }

    if (noTaskMessageText != null) {
      if (fetchType.equals(NetworkUtils.FETCHED_SERVER)) {
        noTaskMessageText.setText(group == null ? R.string.txt_no_task_upcoming :
            group.hasCreatePermissions() ? R.string.txt_no_task_group : R.string.txt_no_task_no_create);
      } else {
        if (RealmUtils.loadPreferencesFromDB().getOnlineStatus().equals(NetworkUtils.OFFLINE_SELECTED)) {
          noTaskMessageText.setText(R.string.txt_no_task_offline);
        } else {
          noTaskMessageText.setText(R.string.txt_task_could_not_fetch);
        }
      }
    }

    if (noTaskMessageLayout != null) {
      noTaskMessageLayout.setVisibility(View.VISIBLE);
    }
    isInNoTaskMessageView = true;

    if (getActivity() != null) { // as usual, this call back may happen after activity is destroyed
      getActivity().supportInvalidateOptionsMenu();
    }
  }

    /*
    SECTION : RESPOND TO MICRO INTERACTIONS / SWITCH TO VIEW AND EDITS
     */

  @Override
  public void respondToTask(String taskUid, String taskType, String response, int position) {

    final int confirmationMessage, msgSuccess;
    switch (taskType) {
      case TaskConstants.VOTE:
        confirmationMessage =
            (response.equals(TaskConstants.RESPONSE_YES))
                ? R.string.vote_respond_yes_confirm
                : R.string.vote_respond_no_confirm;
        msgSuccess = R.string.tlist_vote_sent;
        break;
      case TaskConstants.MEETING:
        confirmationMessage =
            (response.equals(TaskConstants.RESPONSE_YES))
                ? R.string.mtg_respond_yes_confirm
                : R.string.mtg_respond_no_confirm;
        msgSuccess = R.string.tlist_meeting_reply_sent;
        break;
      case TaskConstants.TODO:
        confirmationMessage = R.string.todo_respond_done_confirm;
        msgSuccess = R.string.tlist_todo_reply_sent;
        break;
      default:
        throw new UnsupportedOperationException(
            "Responding to neither vote nor meeting! Error somewhere");
    }

    confirmAction(taskUid, response, msgSuccess, confirmationMessage);
  }

  public void confirmAction(final String taskUid, final String response, final int msgSuccess, final int message) {
    DialogFragment newFragment = ConfirmCancelDialogFragment.newInstance(message, new ConfirmCancelDialogFragment.ConfirmDialogListener() {
      @Override
      public void doConfirmClicked() {
        progressBar.setVisibility(View.VISIBLE);
        TaskService.getInstance().respondToTask(taskUid, response, AndroidSchedulers.mainThread())
            .subscribe(new Action1<String>() {
              @Override
              public void call(String s) {
                // event bus & subscriber will take care of adapter updating
                progressBar.setVisibility(View.GONE);
                if (NetworkUtils.SAVED_SERVER.equals(s)) {
                  Toast.makeText(ApplicationLoader.applicationContext, msgSuccess, Toast.LENGTH_LONG).show();
                } else {
                  Snackbar.make(rootView, R.string.task_list_response_offline, Snackbar.LENGTH_SHORT).show();
                }
              }
            }, new Action1<Throwable>() {
              @Override
              public void call(Throwable e) {
                progressBar.setVisibility(View.GONE);
                // since we store offline, and it's a microinteraction, with a confirm dialog already, keeping to snackbar rather than heavier dialog
                if (NetworkUtils.CONNECT_ERROR.equals(e.getMessage())) {
                  ErrorUtils.networkErrorSnackbar(rootView, R.string.task_list_response_offline,
                      new View.OnClickListener() {
                        @Override
                        public void onClick(View v) { confirmAction(taskUid, response, msgSuccess, message);
                        }
                      });
                } else {
                  Snackbar.make(rootView, ErrorUtils.serverErrorText(e), Snackbar.LENGTH_LONG).show();
                }
              }
            });
      }
    });
    newFragment.show(getFragmentManager(), "dialog");
  }

  @Override
  public void onCardClick(int position, String taskUid, String taskType, String taskTitle) {
    listener.onTaskLoaded(taskTitle);
    listener.onTaskLoaded(position, taskUid,taskType,taskTitle);
   /* ViewTaskFragment taskFragment = ViewTaskFragment.newInstance(taskType, taskUid);
    final int containerId = getView() != null ? ((ViewGroup) getView().getParent()).getId() : rootView.getId();
    getFragmentManager().beginTransaction()
        .setCustomAnimations(R.anim.up_from_bottom, R.anim.down_from_top)
        .add(containerId, taskFragment, ViewTaskFragment.class.getCanonicalName()) // must use tag so managing activities etc can retrieve
        .addToBackStack(null)
        .commit();*/

  }

  @Subscribe(threadMode = ThreadMode.MAIN)
  public void onEvent(TaskAddedEvent event) {
    switchOffNoTasks();
    if (taskView != null) {
      taskView.setVisibility(View.VISIBLE);
    }
    tasksAdapter.addTaskToList(event.getTaskCreated()); // adds to top
  }

  @Subscribe(threadMode = ThreadMode.MAIN)
  public void onTaskUpdated(TaskUpdatedEvent event){
    tasksAdapter.refreshTask(event.getTask().getTaskUid(), event.getTask());
  }

  @Subscribe(threadMode = ThreadMode.MAIN)
  public void onTaskCancelledEvent(TaskCancelledEvent e) {
    tasksAdapter.removeTaskFromList(e.getTaskUid());
  }

  private void showProgress() {
    if (progressBar != null) {
      progressBar.setVisibility(View.VISIBLE);
    }
    if (swipeRefreshLayout != null) {
      swipeRefreshLayout.setRefreshing(true);
    }
  }

  private void hideProgress() {
    if (progressBar != null) {
      progressBar.setVisibility(View.GONE);
    }
    if (swipeRefreshLayout != null) {
      swipeRefreshLayout.setRefreshing(false);
    }
  }

  /*
  HANDLE SEARCHING
   */

  public void searchStringChanged(String query) {
    if (tasksAdapter != null && taskView != null && taskView.getVisibility() == View.VISIBLE) {
      if (TextUtils.isEmpty(query)) {
        tasksAdapter.stopFiltering();
      } else {
        tasksAdapter.searchByName(query);
      }
    }
  }

    /*
    HANDLE FILTERING
     */

  public void filter() {

    // store in case error
    final boolean[] flagsChanged = Arrays.copyOf(filtersChecked, 3);

    AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
    builder.setMultiChoiceItems(filterOptions, flagsChanged, new DialogInterface.OnMultiChoiceClickListener() {
      @Override
      public void onClick(DialogInterface dialog, int which, boolean isChecked) {
        flagsChanged[which] = isChecked;
      }
    });

    builder.setPositiveButton(R.string.tasks_filter_do, new DialogInterface.OnClickListener() {
      @Override
      public void onClick(DialogInterface dialog, int which) {
        tasksAdapter.setToFilters(flagsChanged, AndroidSchedulers.mainThread())
            .subscribe(new Action1<Boolean>() {
              @Override
              public void call(Boolean aBoolean) {
                tasksAdapter.notifyDataSetChanged();
                filtersChecked = flagsChanged;
              }
            }, new Action1<Throwable>() {
              @Override
              public void call(Throwable throwable) {
                throwable.printStackTrace();
              }
            });
      }
    });

    builder.setNegativeButton(R.string.tasks_filter_clear, new DialogInterface.OnClickListener() {
      @Override
      public void onClick(DialogInterface dialog, int which) {
        Arrays.fill(filtersChecked, false); // UI initializes to false, with buttons unchecked (adapter, in logic, initializes to true)
        tasksAdapter.stopFiltering();
      }
    });

    builder
        .setCancelable(true)
        .create()
        .show();
  }
}
