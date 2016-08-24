package org.grassroot.android.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.grassroot.android.R;
import org.grassroot.android.adapters.TasksAdapter;
import org.grassroot.android.events.TaskAddedEvent;
import org.grassroot.android.events.TaskCancelledEvent;
import org.grassroot.android.events.TaskUpdatedEvent;
import org.grassroot.android.fragments.dialogs.ConfirmCancelDialogFragment;
import org.grassroot.android.interfaces.GroupPickCallbacks;
import org.grassroot.android.interfaces.TaskConstants;
import org.grassroot.android.models.TaskModel;
import org.grassroot.android.services.TaskService;
import org.grassroot.android.utils.ErrorUtils;
import org.grassroot.android.utils.NetworkUtils;
import org.grassroot.android.utils.RealmUtils;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

  GroupPickCallbacks pickCallbacks;
  TaskListListener listener;

  private TasksAdapter tasksAdapter;

  private String groupUid;

  private boolean isInNoTaskMessageView;
  private boolean hasFetchedFromServer;

  private boolean displayFAB; // todo : just show it always (move FAB from GT-Activity to here)
  private ViewGroup container;
  private Unbinder unbinder;

  @BindView(R.id.tl_swipe_refresh) SwipeRefreshLayout swipeRefreshLayout;
  @BindView(R.id.tl_recycler_view) RecyclerView taskView;
  @BindView(R.id.tl_fab) FloatingActionButton floatingActionButton;

  @BindView(R.id.tl_no_task_message) RelativeLayout noTaskMessageLayout;
  @BindView(R.id.tl_no_task_text) TextView noTaskMessageText;

  private boolean filteringActive;
  private Map<String, Boolean> filterFlags;

  @BindView(R.id.progressBar) ProgressBar progressBar;

  public interface TaskListListener {
    void onTaskLoaded(String taskName);
  }

    /*
    SECTION : SET UP VIEWS AND POPULATE THE LIST
     */

  // pass null if this is a group-neutral task fragment
  public static TaskListFragment newInstance(String parentUid,
      GroupPickCallbacks groupPickCallbacks, TaskListListener listener, boolean showFAB) {

    TaskListFragment fragment = new TaskListFragment();
    fragment.groupUid = parentUid;
    fragment.pickCallbacks = groupPickCallbacks;
    fragment.listener = listener;
    fragment.displayFAB = showFAB;

    return fragment;
  }

  @Override public void onAttach(Context context) {
    super.onAttach(context);
    EventBus.getDefault().register(this);
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    filterFlags = new HashMap<>();
  }

  @Override public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {

    View viewToReturn = inflater.inflate(R.layout.fragment_task_list, container, false);
    unbinder = ButterKnife.bind(this, viewToReturn);
    this.container = container;
    floatingActionButton.setVisibility(displayFAB ? View.VISIBLE : View.GONE);

    tasksAdapter = new TasksAdapter(new ArrayList<TaskModel>(), TextUtils.isEmpty(groupUid), this);
    taskView.setAdapter(tasksAdapter);
    taskView.setLayoutManager(new LinearLayoutManager(getActivity()));
    taskView.setHasFixedSize(true);
    taskView.setDrawingCacheEnabled(true);

    loadTasksOnCreateView();
    return viewToReturn;
  }

  private void loadTasksOnCreateView() {
    if (hasTasksInDB()) {
      loadTasksFromDB(NetworkUtils.FETCHED_CACHE);
      refreshTasksFromServer();
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

  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    swipeRefreshLayout.setColorSchemeColors(ContextCompat.getColor(getActivity(), R.color.primaryColor));
    swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
      @Override public void onRefresh() {
        refreshTasksFromServer();
      }
    });
  }

  @Override public void onDestroyView() {
    super.onDestroyView();
    unbinder.unbind();
  }

  @Override public void onDetach() {
    super.onDetach();
    EventBus.getDefault().unregister(this);
  }

  /*
  SECTION : LOGIC FOR FETCHING AND DISPLAYING TASKS
   */

  // a null or empty groupUid passed through, tells the service to fetch all upcoming tasks, across groups
  private void refreshTasksFromServer() {
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
          tasksAdapter.refreshTaskList(taskModels);
          tasksAdapter.notifyDataSetChanged();
          taskView.setVisibility(View.VISIBLE);
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
        noTaskMessageText.setText(groupUid == null ? R.string.txt_no_task_upcoming : R.string.txt_no_task_group);
      } else {
        noTaskMessageText.setText(R.string.txt_task_could_not_fetch);
      }
    }

    if (noTaskMessageLayout != null) {
      noTaskMessageLayout.setVisibility(View.VISIBLE);
    }
    isInNoTaskMessageView = true;
  }

  /*
  HANDLE NEW TASK
   */

  @OnClick(R.id.tl_fab) public void quickTaskModal() {
    if (pickCallbacks != null) {
      QuickTaskModalFragment modal = QuickTaskModalFragment.newInstance(false, null,
          new QuickTaskModalFragment.TaskModalListener() {
            @Override public void onTaskClicked(String taskType) {
              pickCallbacks.groupPickerTriggered(taskType);
            }
          });
      modal.show(getFragmentManager(), QuickTaskModalFragment.class.getSimpleName());
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
                  Snackbar.make(container, msgSuccess, Snackbar.LENGTH_LONG).show();
                } else {
                  Snackbar.make(container, R.string.task_list_response_offline, Snackbar.LENGTH_SHORT).show();
                }
              }
            }, new Action1<Throwable>() {
              @Override
              public void call(Throwable e) {
                progressBar.setVisibility(View.GONE);
                if (NetworkUtils.CONNECT_ERROR.equals(e.getMessage())) {
                  Snackbar.make(container, R.string.task_list_response_offline, Snackbar.LENGTH_LONG).show();
                } else {
                  final String errorMsg = ErrorUtils.serverErrorText(e);
                  final String retryMsg = getString(R.string.snackbar_try_again);
                  ErrorUtils.showSnackBar(container, errorMsg, Snackbar.LENGTH_LONG, retryMsg, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                      confirmAction(taskUid, response, msgSuccess, message);
                    }
                  });
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
    ViewTaskFragment taskFragment = ViewTaskFragment.newInstance(taskType, taskUid);
    getFragmentManager().beginTransaction()
        .setCustomAnimations(R.anim.up_from_bottom, R.anim.down_from_top)
        .add(container.getId(), taskFragment, ViewTaskFragment.class.getCanonicalName()) // must preserve this so managing activities etc can retrieve
        .addToBackStack(null)
        .commit();
  }

  @Subscribe(threadMode = ThreadMode.MAIN)
  public void onEvent(TaskAddedEvent event) {
    if (isInNoTaskMessageView) {
      noTaskMessageLayout.setVisibility(View.GONE);
      isInNoTaskMessageView = false;
    }
    tasksAdapter.addTaskToList(event.getTaskCreated(), 0);
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
  todo : make this much more efficient
   */

  public void searchStringChanged(String query) {
    if (TextUtils.isEmpty(query)) {
      tasksAdapter.stopFiltering();
    } else {
      tasksAdapter.filterByName(query);
    }
  }

    /*
    HANDLE FILTERING
     */

  public void filter() {
    if (!filteringActive) {
      startFiltering();
    }
    FilterFragment filterDialog = new FilterFragment();
    filterDialog.setArguments(assembleFilterBundle());
    filterDialog.show(getActivity().getSupportFragmentManager(), "FilterFragment");

    filterDialog.setListener(new FilterFragment.TasksFilterListener() {
      @Override public void itemClicked(String typeChanged, boolean changedFlagState) {
        filterFlags.put(typeChanged, changedFlagState);
        tasksAdapter.addOrRemoveTaskType(typeChanged, changedFlagState);
      }

      @Override public void clearFilters() {
        filteringActive = false;
        tasksAdapter.stopFiltering();
        resetFilterFlags();
      }
    });
  }

  private void startFiltering() {
    resetFilterFlags();
    tasksAdapter.startFiltering();
    filteringActive = true;
  }

  private Bundle assembleFilterBundle() {
    Bundle b = new Bundle();
    b.putBoolean(TaskConstants.VOTE, filterFlags.get(TaskConstants.VOTE));
    b.putBoolean(TaskConstants.MEETING, filterFlags.get(TaskConstants.MEETING));
    b.putBoolean(TaskConstants.TODO, filterFlags.get(TaskConstants.TODO));
    return b;
  }

  private void resetFilterFlags() {
    filterFlags.put(TaskConstants.VOTE, false);
    filterFlags.put(TaskConstants.MEETING, false);
    filterFlags.put(TaskConstants.TODO, false);
  }
}
