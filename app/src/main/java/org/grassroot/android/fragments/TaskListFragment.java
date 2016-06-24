package org.grassroot.android.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import org.grassroot.android.R;
import org.grassroot.android.adapters.TasksAdapter;
import org.grassroot.android.events.TaskChangedEvent;
import org.grassroot.android.fragments.dialogs.ConfirmCancelDialogFragment;
import org.grassroot.android.interfaces.GroupConstants;
import org.grassroot.android.interfaces.NetworkErrorDialogListener;
import org.grassroot.android.interfaces.TaskConstants;
import org.grassroot.android.models.TaskResponse;
import org.grassroot.android.services.GrassrootRestService;
import org.grassroot.android.utils.ErrorUtils;
import org.grassroot.android.utils.PreferenceUtils;
import org.greenrobot.eventbus.EventBus;

import java.util.HashMap;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Created by luke on 2016/05/13.
 */
public class TaskListFragment extends Fragment implements TasksAdapter.TaskListListener {

    private static final String TAG = TaskListFragment.class.getCanonicalName();

    private TasksAdapter groupTasksAdapter;
    private String groupUid;
    private String phoneNumber;
    private String code;

    private ViewTaskFragment.ViewTaskListener taskViewListener;
    private ViewGroup container;

    @BindView(R.id.tl_swipe_refresh)
    SwipeRefreshLayout swipeRefreshLayout;
    @BindView(R.id.tl_recycler_view)
    RecyclerView rcTaskView;

    @BindView(R.id.tl_error_layout)
    RelativeLayout errorLayout;
    @Nullable
    @BindView(R.id.im_no_internet)
    ImageView imNoInternet;

    @Nullable
    @BindView(R.id.progressBar)
    ProgressBar progressBar;

    private boolean filteringActive;
    private Map<String, Boolean> filterFlags;

    /*
    SECTION : SET UP VIEWS AND POPULATE THE LIST
     */

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        // optional for activity to implement listener
        if (context instanceof ViewTaskFragment.ViewTaskListener)
            this.taskViewListener = (ViewTaskFragment.ViewTaskListener) context;
        else
            this.taskViewListener = null;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        if (args != null) {
            groupUid = args.getString(GroupConstants.UID_FIELD);
        }

        phoneNumber = PreferenceUtils.getUserPhoneNumber(getActivity());
        code = PreferenceUtils.getAuthToken(getActivity());
        filterFlags = new HashMap<>();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View viewToReturn = inflater.inflate(R.layout.fragment_task_list, container, false);
        ButterKnife.bind(this, viewToReturn);
        this.container = container;
        
        setUpSwipeRefresh();
        rcTaskView.setLayoutManager(new LinearLayoutManager(getActivity()));
        groupTasksAdapter = new TasksAdapter(this, getActivity(), groupUid);
        rcTaskView.setAdapter(groupTasksAdapter);

        fetchTaskList();

        return viewToReturn;
    }

    @Override
    public void onResume() {
        if (groupTasksAdapter != null) {
            groupTasksAdapter.registerForEvents();
        }
        super.onResume();
    }

    @Override
    public void onPause() {
        if (groupTasksAdapter != null) {
            groupTasksAdapter.deRegisterEvents();
        }
        super.onPause();
    }

    private void fetchTaskList() {

        swipeRefreshLayout.setRefreshing(true);
        hideErrorLayout();
        showProgress();

        Call<TaskResponse> call = (groupUid != null) ?
                GrassrootRestService.getInstance().getApi().getGroupTasks(phoneNumber, code, groupUid) :
                GrassrootRestService.getInstance().getApi().getUserTasks(phoneNumber, code);

        call.enqueue(new Callback<TaskResponse>() {
            @Override
            public void onResponse(Call<TaskResponse> call, Response<TaskResponse> response) {
                hideProgress();
                if (response.isSuccessful()) {
                    swipeRefreshLayout.setRefreshing(false);
                    TaskResponse taskResponse = response.body();
                    if (TaskConstants.NO_TASKS_FOUND.equals(taskResponse.getMessage())) {
                        // todo : show a message
                    } else {
                        groupTasksAdapter.changeToTaskList(response.body().getTasks());
                    }
                    return;

                }
                ErrorUtils.handleServerError(rcTaskView, getActivity(),response);
            }

            @Override
            public void onFailure(Call<TaskResponse> call, Throwable t) {
                swipeRefreshLayout.setRefreshing(false);
                showErrorLayout();
                hideProgress();
                ErrorUtils.connectivityError(getActivity(), R.string.error_no_network, new NetworkErrorDialogListener() {
                    @Override
                    public void retryClicked() {
                        fetchTaskList();
                    }
                });
            }
        });
    }

    private void showProgress(){
        progressBar.setVisibility(View.VISIBLE);
    }

    private void hideProgress(){
        progressBar.setVisibility(View.INVISIBLE);
    }

    private void showErrorLayout(){
        errorLayout.setVisibility(View.VISIBLE);
        imNoInternet.setVisibility(View.VISIBLE);
    }

    private void hideErrorLayout(){

        errorLayout.setVisibility(View.GONE);
        imNoInternet.setVisibility(View.VISIBLE);

    }

    /*
    SECTION : REFRESH TASK LIST, IN WHOLE OR IN PART (SOME REDUNDANCE TO ABOVE FOR NOW, BUT THAT WILL CHANGE)
     */

    private void setUpSwipeRefresh() {
        swipeRefreshLayout.setColorSchemeColors(ContextCompat.getColor(getActivity(), R.color.primaryColor));
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
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

        Log.e(TAG, "responding to task with uid = " +taskUid);
        switch(taskType) {
            case TaskConstants.VOTE:
                restCall = GrassrootRestService.getInstance().getApi().castVote(taskUid, phoneNumber, code, response);
                confirmationMessage = (response.equalsIgnoreCase("Yes")) ? R.string.RESPOND_YES : R.string.RESPOND_NO;
                msgSuccess = R.string.ga_Votesend;
                msgAlreadyResponded = R.string.ga_VoteFailure;
                break;
            case TaskConstants.MEETING:
                restCall = GrassrootRestService.getInstance().getApi().rsvp(taskUid, phoneNumber, code, response);
                confirmationMessage = (response.equalsIgnoreCase("Yes")) ? R.string.RSVP_YES : R.string.RSVP_NO;
                msgSuccess = R.string.ga_Meetingsend;
                msgAlreadyResponded = R.string.ga_VoteFailure;
                break;
            case TaskConstants.TODO:
                restCall = GrassrootRestService.getInstance().getApi().completeTodo(phoneNumber, code, taskUid);
                confirmationMessage = R.string.TODO_COMPLETED;
                msgSuccess = R.string.ga_ToDocompleted;
                msgAlreadyResponded = R.string.ga_ToDoFailure;
                break;
            default:
                throw new UnsupportedOperationException("Responding to neither vote nor meeting! Error somewhere");
        }

        confirmAction(restCall, msgSuccess, msgAlreadyResponded, confirmationMessage, position);
    }

    public void confirmAction(final Call<TaskResponse> restCall, final int msgSuccess, final int msgAlreadyResponded,
                              final int message, final int position) {

        DialogFragment newFragment = ConfirmCancelDialogFragment.newInstance(message, new ConfirmCancelDialogFragment.ConfirmDialogListener() {
            @Override
            public void doConfirmClicked() {
                restCall.enqueue(new Callback<TaskResponse>() {
                    @Override
                    public void onResponse(Call<TaskResponse> call, Response<TaskResponse> response) {
                        if (response.isSuccessful()) {
                            ErrorUtils.showSnackBar(container, msgSuccess, Snackbar.LENGTH_LONG);
                            EventBus.getDefault().post(new TaskChangedEvent(position, response.body().getTasks().get(0)));
                        } else {
                            ErrorUtils.handleServerError(container, getActivity(),response);
                    }}

                    @Override
                    public void onFailure(Call<TaskResponse> call, Throwable t) {
                        ErrorUtils.handleNetworkError(getActivity(), container, t);
                    }
                });
            }
        });

        // todo: this seems awkward ... may want to switch to using app.Fragment, given only supporting v4.0+
        newFragment.show(getFragmentManager(), "dialog");
    }

    @Override
    public void onCardClick(int position, String taskUid, String taskType) {
        ViewTaskFragment taskFragment = ViewTaskFragment.newInstance(taskType, taskUid, (ViewTaskFragment.ViewTaskListener) getActivity());
        getFragmentManager().beginTransaction()
                .setCustomAnimations(R.anim.up_from_bottom, R.anim.down_from_top)
                .add(container.getId(), taskFragment, ViewTaskFragment.class.getCanonicalName())
                .addToBackStack(null)
                .commit();
    }

    @OnClick(R.id.im_no_internet)
    public void onNoInternetClick(){
        fetchTaskList();
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
        filterDialog.show(getActivity().getFragmentManager(), "FilterFragment");

        filterDialog.setListener(new FilterFragment.TasksFilterListener() {
            @Override
            public void itemClicked(String typeChanged, boolean changedFlagState) {
                filterFlags.put(typeChanged, changedFlagState);
                groupTasksAdapter.addOrRemoveTaskType(typeChanged, changedFlagState);
            }

            @Override
            public void clearFilters() {
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
