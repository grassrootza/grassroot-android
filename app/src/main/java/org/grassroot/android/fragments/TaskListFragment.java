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
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import java.util.HashMap;
import java.util.Map;
import org.grassroot.android.R;
import org.grassroot.android.adapters.TasksAdapter;
import org.grassroot.android.events.TaskAddedEvent;
import org.grassroot.android.events.TaskCancelledEvent;
import org.grassroot.android.events.TaskChangedEvent;
import org.grassroot.android.fragments.dialogs.ConfirmCancelDialogFragment;
import org.grassroot.android.interfaces.GroupPickCallbacks;
import org.grassroot.android.interfaces.NetworkErrorDialogListener;
import org.grassroot.android.interfaces.TaskConstants;
import org.grassroot.android.models.Group;
import org.grassroot.android.models.TaskModel;
import org.grassroot.android.models.TaskResponse;
import org.grassroot.android.services.GrassrootRestService;
import org.grassroot.android.services.TaskService;
import org.grassroot.android.utils.ErrorUtils;
import org.grassroot.android.utils.RealmUtils;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Created by luke on 2016/05/13.
 */
public class TaskListFragment extends Fragment implements TasksAdapter.TaskListListener {

  private static final String TAG = TaskListFragment.class.getSimpleName();

  GroupPickCallbacks mCallbacks;
  TaskListListener listener;

  private TasksAdapter groupTasksAdapter;
  private String groupUid;
  private String phoneNumber;
  private String code;
  private boolean noTaskMessageVisible;
  private boolean displayFAB; // todo : just show it always (move FAB from GT-Activity to here)

  private ViewGroup container;
  private Unbinder unbinder;

  @BindView(R.id.tl_swipe_refresh) SwipeRefreshLayout swipeRefreshLayout;
  @BindView(R.id.tl_recycler_view) RecyclerView rcTaskView;
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
    fragment.mCallbacks = groupPickCallbacks;
    fragment.listener = listener;
    fragment.displayFAB = showFAB;
    return fragment;
  }

  @Override public void onAttach(Context context) {
    super.onAttach(context);
    EventBus.getDefault().register(this);
  }

  @Override public void onCreate(Bundle savedInstanceState) {
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

    setUpSwipeRefresh();
    rcTaskView.setLayoutManager(new LinearLayoutManager(getActivity()));
    groupTasksAdapter = new TasksAdapter(this, getActivity(), groupUid);
    rcTaskView.setAdapter(groupTasksAdapter);
    floatingActionButton.setVisibility(displayFAB ? View.VISIBLE : View.GONE);

    progressDialog = new ProgressDialog(getContext());
    progressDialog.setIndeterminate(true);

    fetchTaskList();
    return viewToReturn;
  }

  @Override public void onResume() {
    if (groupTasksAdapter != null) {
      groupTasksAdapter.registerForEvents();
    }
    super.onResume();
  }

  @Override public void onPause() {
    if (groupTasksAdapter != null) {
      groupTasksAdapter.deRegisterEvents();
    }
    super.onPause();
  }

  @Override public void onDestroyView() {
    super.onDestroyView();
    progressDialog.dismiss();
    unbinder.unbind();
  }

  @Override public void onDetach() {
    super.onDetach();
    EventBus.getDefault().unregister(this);
  }

  private void fetchTaskList() {
    if (groupUid != null) {
      fetchGroupTasks();
    } else {
      fetchAllUpcomingTasks();
    }
  }

  private void fetchAllUpcomingTasks() {
    if (TaskService.getInstance().hasLoadedTasks) {
      if (TaskService.getInstance().hasUpcomingTasks()) {
        noTaskMessageVisible = false;
        groupTasksAdapter.changeToTaskList(TaskService.getInstance().upcomingTasks);
      } else {
        handleNoTasksFound();
      }
    } else {
      // todo : make this process cleaner
    }
  }

  private void fetchGroupTasks() {
    final Group group = RealmUtils.loadObjectFromDB(Group.class, "groupUid", groupUid);
    if(group.isFetchedTasks()) groupTasksAdapter.changeToTaskList(
        RealmUtils.loadListFromDB(TaskModel.class, "parentUid", groupUid));
    swipeRefreshLayout.setRefreshing(true);
    progressDialog.show();

    Call<TaskResponse> call =
        GrassrootRestService.getInstance().getApi().getGroupTasks(phoneNumber, code, groupUid);

    call.enqueue(new Callback<TaskResponse>() {
      @Override public void onResponse(Call<TaskResponse> call, Response<TaskResponse> response) {
        progressDialog.hide();
        if (response.isSuccessful()) {
          swipeRefreshLayout.setRefreshing(false);
          final TaskResponse taskResponse = response.body();
          if (taskResponse.getTasks() == null || taskResponse.getTasks().isEmpty()) {
            handleNoTasksFound();
          } else {
            groupTasksAdapter.changeToTaskList(taskResponse.getTasks());
            RealmUtils.saveDataToRealm(taskResponse.getTasks());
          }
          group.setFetchedTasks(true);
          RealmUtils.saveDataToRealm(group);
          return;
        }
        ErrorUtils.handleServerError(rcTaskView, getActivity(), response);
      }

      @Override
      public void onFailure(Call<TaskResponse> call, Throwable t) {
        if (swipeRefreshLayout != null && swipeRefreshLayout.isRefreshing()) {
          swipeRefreshLayout.setRefreshing(false);
        }
        progressDialog.dismiss();
        /* todo : do actually need to make this work
        ErrorUtils.connectivityError(getActivity(), R.string.error_no_network,
                new NetworkErrorDialogListener() {
                  @Override
                  public void retryClicked() {
                    fetchTaskList();
                  }
                });*/
      }
    });
  }

  private void handleNoTasksFound() {
    swipeRefreshLayout.setVisibility(View.GONE);
    if (groupUid == null) {
      noTaskMessageText.setText(R.string.txt_no_task_upcoming);
    }
    noTaskMessageLayout.setVisibility(View.VISIBLE);
    noTaskMessageVisible = true;
  }

  /*
  HANDLE NEW TASK
   */

  @OnClick(R.id.tl_fab) public void quickTaskModal() {
    if (mCallbacks != null) {
      QuickTaskModalFragment modal = QuickTaskModalFragment.newInstance(false, null,
          new QuickTaskModalFragment.TaskModalListener() {
            @Override public void onTaskClicked(String taskType) {
              mCallbacks.groupPickerTriggered(taskType);
            }
          });
      modal.show(getFragmentManager(), QuickTaskModalFragment.class.getSimpleName());
    }
  }

    /*
    SECTION : REFRESH TASK LIST, IN WHOLE OR IN PART (SOME REDUNDANCE TO ABOVE FOR NOW, BUT THAT WILL CHANGE)
     */

  private void setUpSwipeRefresh() {
    swipeRefreshLayout.setColorSchemeColors(
        ContextCompat.getColor(getActivity(), R.color.primaryColor));
    swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
      @Override public void onRefresh() {
        fetchTaskList();
      }
    });
  }

    /*
    SECTION : RESPOND TO MICRO INTERACTIONS / SWITCH TO VIEW AND EDITS
     */

  @Override
  public void respondToTask(String taskUid, String taskType, String response, int position) {

    Call<TaskResponse> restCall;
    final int confirmationMessage, msgSuccess, msgAlreadyResponded;

    Log.e(TAG, "responding to task with uid = " + taskUid);
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

    // todo: this seems awkward ... may want to switch to using app.Fragment, given only supporting v4.0+
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

  //For some reason which i suspect have to do with the ui thread, the handler in the adapter is not updating view
  @Subscribe public void onTaskCreated(TaskAddedEvent event) {
    Log.e(TAG, "onTaskCreated triggered ... ");
    final TaskModel task = event.getTaskCreated();
    if (noTaskMessageVisible) {
      noTaskMessageLayout.setVisibility(View.GONE);
      noTaskMessageVisible = false;
    }
    fetchTaskList(); // todo : just insert the new task
  }

  @Subscribe public void onTaskCancelledEvent(TaskCancelledEvent e) {
    Log.e(TAG, "homeScreen : task cancelled!");
    Fragment frag =
        getFragmentManager().findFragmentByTag(ViewTaskFragment.class.getCanonicalName());
    if (frag != null && frag.isVisible()) {
      Log.e(TAG, "homeScreen : found task!");
      getFragmentManager().beginTransaction()
          .setCustomAnimations(R.anim.push_down_in, R.anim.push_down_out)
          .remove(frag)
          .commit();
    }
  }

  /*
  HANDLE SEARCHING
  todo : make this much more efficient
   */

  public void searchStringChanged(String query) {
    if (TextUtils.isEmpty(query)) {
      groupTasksAdapter.stopFiltering();
    } else {
      groupTasksAdapter.filterByName(query);
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
        groupTasksAdapter.addOrRemoveTaskType(typeChanged, changedFlagState);
      }

      @Override public void clearFilters() {
        filteringActive = false;
        groupTasksAdapter.stopFiltering();
        resetFilterFlags();
      }
    });
  }

  private void startFiltering() {
    resetFilterFlags();
    groupTasksAdapter.startFiltering();
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
