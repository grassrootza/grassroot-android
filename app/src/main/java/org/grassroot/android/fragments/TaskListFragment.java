package org.grassroot.android.fragments;

import android.app.ProgressDialog;
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
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.grassroot.android.R;
import org.grassroot.android.adapters.TasksAdapter;
import org.grassroot.android.events.TaskAddedEvent;
import org.grassroot.android.events.TaskCancelledEvent;
import org.grassroot.android.events.TaskChangedEvent;
import org.grassroot.android.events.TaskUpdatedEvent;
import org.grassroot.android.fragments.dialogs.ConfirmCancelDialogFragment;
import org.grassroot.android.interfaces.GroupPickCallbacks;
import org.grassroot.android.interfaces.TaskConstants;
import org.grassroot.android.models.TaskResponse;
import org.grassroot.android.services.GrassrootRestService;
import org.grassroot.android.services.TaskService;
import org.grassroot.android.utils.ErrorUtils;
import org.grassroot.android.utils.RealmUtils;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.HashMap;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Created by luke on 2016/05/13.
 */
public class TaskListFragment extends Fragment implements TasksAdapter.TaskListListener {

  private static final String TAG = TaskListFragment.class.getSimpleName();

  GroupPickCallbacks pickCallbacks;
  TaskListListener listener;

  private TasksAdapter tasksAdapter;

  private String groupUid;
  private String phoneNumber;
  private String code;

  private boolean isInNoTaskMessageView;
  private boolean hasFetchedTasks;

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

  ProgressDialog progressDialog;

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
    phoneNumber = RealmUtils.loadPreferencesFromDB().getMobileNumber();
    code = RealmUtils.loadPreferencesFromDB().getToken();
    filterFlags = new HashMap<>();
  }

  @Override public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
    View viewToReturn = inflater.inflate(R.layout.fragment_task_list, container, false);
    unbinder = ButterKnife.bind(this, viewToReturn);
    this.container = container;

    floatingActionButton.setVisibility(displayFAB ? View.VISIBLE : View.GONE);
    return viewToReturn;
  }

  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    setUpRecyclerView();
  }

  private void setUpRecyclerView() {
    Log.e(TAG, "task list ... setting up recycler view");

    tasksAdapter = new TasksAdapter(this, getActivity(), groupUid);
    taskView.setLayoutManager(new LinearLayoutManager(getActivity()));
    taskView.setAdapter(tasksAdapter);
    taskView.setHasFixedSize(true);
    taskView.setDrawingCacheEnabled(true);
    tasksAdapter.refreshTaskListToDB(TaskService.QUICK_DB_LOAD);

    swipeRefreshLayout.setColorSchemeColors(ContextCompat.getColor(getActivity(), R.color.primaryColor));
    swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
      @Override public void onRefresh() {
        Log.e(TAG, "calling fetch task list from swip refresh");
        fetchTaskList();
      }
    });
  }

  @Override
  public void onResume() {
    super.onResume();
    showProgressIfAdapterLoading();
    if (!hasFetchedTasks) {
      Log.e(TAG, "calling fetch task list from opening");
      fetchTaskList();
    } else if (isInNoTaskMessageView) {
      noTaskMessageLayout.setVisibility(View.VISIBLE);
    }
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
  private void fetchTaskList() {
    Log.e(TAG, "inside fetch task list");
    showProgressIfAdapterLoading();
    TaskService.getInstance().fetchTasks(groupUid, new TaskService.TaskServiceListener() {
      @Override
      public void taskFetchingComplete(final String fetchType, final Object data) {
        Log.e(TAG, "returned from fetch tasks, fetch type = " + fetchType);
        tasksAdapter.refreshTaskListToDB(fetchType);
        if (data != null && TaskService.FETCH_ERROR.equals(fetchType) && data instanceof Response) {
            ErrorUtils.handleServerError(taskView, getActivity(), (Response) data);
        }
      }
    });
  }

  @Override
  public void tasksLoaded(String fetchType) {
    Log.e(TAG, "inside tasks loaded ... fetch type = " + fetchType);
    if (tasksAdapter.getItemCount() != 0) {
      hideProgress();
      switchOffNoTasks();
      if (!TaskService.QUICK_DB_LOAD.equals(fetchType)) {
        hasFetchedTasks = true;
      }
    } else if (!TaskService.QUICK_DB_LOAD.equals(fetchType)) {
      hideProgress();
      hasFetchedTasks = true;
      handleNoTasksFound(fetchType);
    }else{
      //// TODO: 01/08/16 add logic here 
      hideProgress();
    }
  }

  private void switchOffNoTasks() {
    if (noTaskMessageLayout != null) {
      noTaskMessageLayout.setVisibility(View.GONE);
    }
    isInNoTaskMessageView = false;
  }

  private void handleNoTasksFound(final String fetchType) {
    if (noTaskMessageText != null) { // since the call may time out / return when the user is on a different fragment
      if (fetchType.equals(TaskService.FETCH_OKAY)) {
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

    Call<TaskResponse> restCall;
    final int confirmationMessage, msgSuccess, msgAlreadyResponded;
    switch (taskType) {
      case TaskConstants.VOTE:
        restCall = GrassrootRestService.getInstance()
            .getApi()
            .castVote(taskUid, phoneNumber, code, response);
        confirmationMessage =
            (response.equalsIgnoreCase("Yes")) ? R.string.RESPOND_YES : R.string.RESPOND_NO;
        msgSuccess = R.string.ga_Votesend;
        msgAlreadyResponded = R.string.ga_VoteFailure;
        break;
      case TaskConstants.MEETING:
        restCall =
            GrassrootRestService.getInstance().getApi().rsvp(taskUid, phoneNumber, code, response);
        confirmationMessage =
            (response.equalsIgnoreCase("Yes")) ? R.string.RSVP_YES : R.string.RSVP_NO;
        msgSuccess = R.string.ga_Meetingsend;
        msgAlreadyResponded = R.string.ga_VoteFailure;
        break;
      case TaskConstants.TODO:
        restCall =
            GrassrootRestService.getInstance().getApi().completeTodo(phoneNumber, code, taskUid);
        confirmationMessage = R.string.TODO_COMPLETED;
        msgSuccess = R.string.ga_ToDocompleted;
        msgAlreadyResponded = R.string.ga_ToDoFailure;
        break;
      default:
        throw new UnsupportedOperationException(
            "Responding to neither vote nor meeting! Error somewhere");
    }

    confirmAction(restCall, msgSuccess, msgAlreadyResponded, confirmationMessage, position);
  }

  public void confirmAction(final Call<TaskResponse> restCall, final int msgSuccess,
      final int msgAlreadyResponded, final int message, final int position) {

    DialogFragment newFragment = ConfirmCancelDialogFragment.newInstance(message,
        new ConfirmCancelDialogFragment.ConfirmDialogListener() {
          @Override public void doConfirmClicked() {
            restCall.enqueue(new Callback<TaskResponse>() {
              @Override
              public void onResponse(Call<TaskResponse> call, Response<TaskResponse> response) {
                if (response.isSuccessful()) {
                  ErrorUtils.showSnackBar(container, msgSuccess, Snackbar.LENGTH_LONG);
                  EventBus.getDefault()
                      .post(new TaskChangedEvent(position, response.body().getTasks().get(0)));
                } else {
                  ErrorUtils.handleServerError(container, getActivity(), response);
                }
              }

              @Override public void onFailure(Call<TaskResponse> call, Throwable t) {
                ErrorUtils.handleNetworkError(getActivity(), container, t);
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
        .add(container.getId(), taskFragment, ViewTaskFragment.class.getCanonicalName())
        .addToBackStack(null)
        .commit();
  }

  @Subscribe public void onEvent(TaskAddedEvent event) {
    if (isInNoTaskMessageView) {
      noTaskMessageLayout.setVisibility(View.GONE);
      isInNoTaskMessageView = false;
    }
    tasksAdapter.refreshTaskListToDB(null); // todo : just add it to top, maybe, once free of bugs
  }

  @Subscribe public void onTaskUpdated(TaskUpdatedEvent event){
    tasksAdapter.refreshTaskListToDB(null);
  }

  @Subscribe public void onTaskCancelledEvent(TaskCancelledEvent e) {
    Fragment frag = getFragmentManager().findFragmentByTag(ViewTaskFragment.class.getCanonicalName());
    if (frag != null && frag.isVisible()) {
      getFragmentManager().beginTransaction()
          .setCustomAnimations(R.anim.push_down_in, R.anim.push_down_out)
          .remove(frag)
          .commit();
    }
    tasksAdapter.refreshTaskListToDB(null); // todo : just find it and remove it in list
  }

  private void showProgressIfAdapterLoading() {
    if (!isInNoTaskMessageView) {
      if (tasksAdapter == null) {
        Log.d(TAG, "showing progress dialog, as adapter still null ...");
        showProgress();
      } else if (tasksAdapter.getItemCount() == 0) {
        Log.d(TAG, "showing progress dialog, as item count is zero ...");
        showProgress();
      } else {
        hideProgress();
      }
    }
  }

  private void showProgress() {
    if (progressDialog == null) {
      progressDialog = new ProgressDialog(getContext());
      progressDialog.setMessage(getString(R.string.txt_pls_wait));
      progressDialog.setIndeterminate(true);
    }
    progressDialog.show();
    if (swipeRefreshLayout != null) {
      swipeRefreshLayout.setRefreshing(true);
    }
  }

  private void hideProgress() {
    if (progressDialog != null) {
      progressDialog.dismiss();
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
