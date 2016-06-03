package org.grassroot.android.ui.activities;

import android.app.DialogFragment;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import com.github.clans.fab.FloatingActionMenu;
import org.grassroot.android.R;
import org.grassroot.android.adapters.TasksAdapter;
import org.grassroot.android.events.TaskAddedEvent;
import org.grassroot.android.interfaces.AlertDialogListener;
import org.grassroot.android.interfaces.ConfirmDialogListener;
import org.grassroot.android.interfaces.NetworkErrorDialogListener;
import org.grassroot.android.interfaces.TaskListListener;
import org.grassroot.android.services.GrassrootRestService;
import org.grassroot.android.services.NoConnectivityException;
import org.grassroot.android.services.model.GenericResponse;
import org.grassroot.android.services.model.TaskModel;
import org.grassroot.android.services.model.TaskResponse;
import org.grassroot.android.ui.fragments.AlertDialogFragment;
import org.grassroot.android.ui.fragments.ConfirmCancelDialogFragment;
import org.grassroot.android.ui.fragments.FilterFragment;
import org.grassroot.android.ui.views.CustomItemAnimator;
import org.grassroot.android.utils.Constant;
import org.grassroot.android.utils.ErrorUtils;
import org.grassroot.android.utils.MenuUtils;
import org.grassroot.android.utils.PreferenceUtils;
import org.grassroot.android.utils.UtilClass;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static java.util.Collections.*;

public class GroupTasksActivity extends PortraitActivity implements TaskListListener {

    private static final String TAG = GroupTasksActivity.class.getCanonicalName();

    private String groupUid;
    private String groupName;

    private String phoneNumber;
    private String code;

    @BindView(R.id.rl_activity_root)
    RelativeLayout rlActivityRoot;

    @BindView(R.id.ga_toolbar)
    Toolbar gaToolbar;

    @BindView(R.id.rc_ga)
    RecyclerView recycleViewGroupActivities;

    @BindView(R.id.fabbutton)
    FloatingActionMenu fabbutton;
    @BindView(R.id.progressBar)
    ProgressBar mProgressBar;

    private Snackbar snackbar;
    @BindView(R.id.error_layout)
    View errorLayout;
    @BindView(R.id.im_no_results)
    ImageView imNoResults;
    @BindView(R.id.im_server_error)
    ImageView imServerError;
    @BindView(R.id.im_no_internet)
    ImageView imNoInternet;

    private TasksAdapter group_activitiesAdapter;
    private AlertDialogFragment alertDialogFragment;

    public List<TaskModel> fullTasksList; // stores all the tasks we get from server
    public List<TaskModel> viewedTasksList; // on filtering, stores only those we have selected

    private Map<String, List<TaskModel>> decomposedList; // decomposed list of votes, meetings, to-dos, for fast filter
    private Map<String, Boolean> filterFlags;
    private boolean listsAlreadyDecomposed = false, filtersActive;

    private List<TaskModel> voteList; // stores the votes, only created if user hits filter
    private List<TaskModel> meetingList; // stores the meetings, only created if user hits filter
    private List<TaskModel> toDoList; // stores the todos, only created if user hits filter


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group__activities);
        ButterKnife.bind(this);
        EventBus.getDefault().register(this);

        Bundle extras = getIntent().getExtras();

        if (extras == null) {
            throw new UnsupportedOperationException("Group activities action called without group Uid!");
        }

        groupUid = extras.getString(Constant.GROUPUID_FIELD);
        groupName = extras.getString(Constant.GROUPNAME_FIELD);

        init();
        setUpViews();
        initRecyclerView();

        getTasks();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // todo: check for permissions
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_group_tasks, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // todo: check permissions
        switch (item.getItemId()) {
            case R.id.mi_icon_filter:
                filterTasks();
                return true;
            case R.id.mi_view_members:
                Intent viewMembers = MenuUtils.constructIntent(this, GroupMembersActivity.class, groupUid, groupName);
                viewMembers.putExtra(Constant.PARENT_TAG_FIELD, GroupTasksActivity.class.getCanonicalName());
                startActivity(viewMembers);
                return true;
            case R.id.mi_add_members:
                startActivity(MenuUtils.constructIntent(this, AddMembersActivity.class, groupUid, groupName));
                return true;
            case R.id.mi_remove_members:
                Intent removeMembers = MenuUtils.constructIntent(this, RemoveMembersActivity.class, groupUid, groupName);
                startActivity(removeMembers); // todo: pass a tag
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onDestroy() {
        EventBus.getDefault().unregister(this);
        super.onDestroy();
    }

    private void init() {
        phoneNumber = PreferenceUtils.getuser_mobilenumber(this);
        code = PreferenceUtils.getuser_token(this);
        filterFlags = new HashMap<>();
        viewedTasksList = new ArrayList<>();
        resetFilterFlags();
    }

    private void setUpViews() {

        setTitle(groupName);
        setSupportActionBar(gaToolbar);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // todo : check if there is a cleaner way to do this
        fabbutton.setOnMenuToggleListener(new FloatingActionMenu.OnMenuToggleListener() {
            @Override
            public void onMenuToggle(boolean opened) {
                if (opened) {
                    // todo: check & pass permissions
                    fabbutton.toggle(false);
                    startActivity(MenuUtils.constructIntent(GroupTasksActivity.this, NewTaskMenuActivity.class, groupUid, groupName));
                    overridePendingTransition(R.anim.push_up_in, R.anim.push_up_out);
                }
            }
        });
    }

    private void initRecyclerView() {
        recycleViewGroupActivities.setLayoutManager(new LinearLayoutManager(GroupTasksActivity.this));
        recycleViewGroupActivities.setItemAnimator(new CustomItemAnimator());
        group_activitiesAdapter = new TasksAdapter(new ArrayList<TaskModel>(), this, this);
        recycleViewGroupActivities.setAdapter(group_activitiesAdapter);

    }

    private void getTasks() {
        mProgressBar.setVisibility(View.VISIBLE);
        fullTasksList = new ArrayList<>();

        GrassrootRestService.getInstance().getApi()
                .getGroupTasks(groupUid, phoneNumber, code)
                .enqueue(new Callback<TaskResponse>() {
                    @Override
                    public void onResponse(Call<TaskResponse> call, Response<TaskResponse> response) {
                        mProgressBar.setVisibility(View.INVISIBLE);
                        if (response.isSuccessful()) {
                            TaskResponse taskResponse = response.body();
                            if (Constant.NO_GROUP_TASKS.equals(taskResponse.getMessage())) {
                                // todo : make this go "up" to the home page, not empty group page
                                startActivity(MenuUtils.constructIntent(GroupTasksActivity.this, NewTaskMenuActivity.class, groupUid, groupName));
                            } else {
                                fullTasksList = taskResponse.getTasks();
                                group_activitiesAdapter.clearTasks();
                                recycleViewGroupActivities.setVisibility(View.VISIBLE);
                                resetViewToAllTasks();
                            }
                        } // todo: add snack bar with error possibilities
                    }

                    @Override
                    public void onFailure(Call<TaskResponse> call, Throwable t)  {
                        Log.e(TAG, "Inside getActivities ... Here is the failure! " + t.getMessage());
                        mProgressBar.setVisibility(View.INVISIBLE);
                        if (t instanceof NoConnectivityException)
                            imNoInternet.setVisibility(View.VISIBLE);
                        else
                            ErrorUtils.connectivityError(GroupTasksActivity.this, R.string.No_network, new NetworkErrorDialogListener() {
                                @Override
                                public void retryClicked() {
                                    getTasks();
                                }
                            });
                          //  ErrorUtils.handleNetworkError(GroupTasksActivity.this, rlActivityRoot, t);
                    }
                });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        getTasks();
    }

    private void resetViewToAllTasks() {
        Log.d(TAG, "resetting view to all tasks! full task list is: " + fullTasksList.size());
        viewedTasksList.clear();
        viewedTasksList.addAll(fullTasksList);
        group_activitiesAdapter.changeToTaskList(viewedTasksList);
    }

    private void filterTasks() {
        // really destroy / reconstruct on every click? maybe persist and clean up later
        FilterFragment filterDialog = new FilterFragment();
        filterDialog.setArguments(assembleFilterBundle());
        filterDialog.show(getFragmentManager(), "FilterFragment");

        if (!listsAlreadyDecomposed) {
            // first time accessing filter dialog
            decomposeLists();
            listsAlreadyDecomposed = true;
            filtersActive = true;
        }

        filterDialog.setListener(new FilterFragment.TasksFilterListener() {
            @Override
            public void itemClicked(String typeChanged, boolean changedFlagState) {
                addOrRemoveTaskType(typeChanged, changedFlagState);
            }

            @Override
            public void clearFilters() {
                resetViewToAllTasks();
                resetFilterFlags();
            }

        });
    }

    private void decomposeLists() {
        Long startTime = SystemClock.currentThreadTimeMillis();

        decomposedList = new HashMap<>();
        viewedTasksList = new ArrayList<>();
        voteList = new ArrayList<>();
        meetingList = new ArrayList<>();
        toDoList = new ArrayList<>();

        for (TaskModel tm : fullTasksList) {
            if (Constant.MEETING.equals(tm.getType())) meetingList.add(tm);
            else if (Constant.VOTE.equals(tm.getType())) voteList.add(tm);
            else if (Constant.TODO.equals(tm.getType())) toDoList.add(tm);
        }

        decomposedList.put(Constant.MEETING, meetingList);
        decomposedList.put(Constant.VOTE, voteList);
        decomposedList.put(Constant.TODO, toDoList);

        Log.d(TAG, String.format("decomposed task list, sizes: %d total, %d votes, %d mtgs, %d todos, took %d msecs", fullTasksList.size(),
                voteList.size(), meetingList.size(), toDoList.size(), SystemClock.currentThreadTimeMillis() - startTime));
    }

    private void addOrRemoveTaskType(String taskType, boolean turnFilterOn) {
        filterFlags.put(taskType, turnFilterOn);
        List<TaskModel> tasks = decomposedList.get(taskType);

        if (turnFilterOn) {
            if (tasks.isEmpty()) {
                showSnackBar("No tasks of that type"); // todo : call different strings ("ga_noTodo")
            } else {
                if (!filtersActive) {
                    viewedTasksList.clear();
                    filtersActive = true;
                }
                viewedTasksList.addAll(tasks);
                sort(viewedTasksList, reverseOrder());
                group_activitiesAdapter.changeToTaskList(viewedTasksList);
            }
        } else {
            viewedTasksList.removeAll(tasks);
            sort(viewedTasksList, reverseOrder());
            group_activitiesAdapter.changeToTaskList(viewedTasksList);
        }
    }

    private void resetFilterFlags() {
        filtersActive = false;
        filterFlags.put(Constant.MEETING, false);
        filterFlags.put(Constant.VOTE, false);
        filterFlags.put(Constant.TODO, false);
    }

    @Override
    public void respondToTask(String taskUid, String taskType, String response) {

        Call<GenericResponse> restCall;
        final String msgSuccess, msgAlreadyResponded;
        Log.e(TAG, "responding to task with uid = " +taskUid);
        if (taskType.equals("VOTE")) {
            restCall = GrassrootRestService.getInstance().getApi().castVote(taskUid, phoneNumber, code, response);
            msgSuccess = getString(R.string.ga_Votesend);
            msgAlreadyResponded = getString(R.string.ga_VoteFailure);
            confirmAction(taskType,restCall,response,msgSuccess,msgAlreadyResponded);
        } else if (taskType.equals("MEETING")) {
            restCall = GrassrootRestService.getInstance().getApi().rsvp(taskUid, phoneNumber, code, response);
            msgSuccess = getString(R.string.ga_Meetingsend);
            msgAlreadyResponded = getString(R.string.ga_VoteFailure);
            confirmAction(taskType,restCall,response,msgSuccess,msgAlreadyResponded);

        } else if (taskType.equals("TODO")) {
            restCall = GrassrootRestService.getInstance().getApi().completeTodo(phoneNumber, code, taskUid);
            msgSuccess = getString(R.string.ga_ToDocompleted);
            msgAlreadyResponded = getString(R.string.ga_ToDoFailure);
            confirmAction(taskType,restCall,response,msgSuccess,msgAlreadyResponded);
        } else {
            throw new UnsupportedOperationException("Responding to neither vote nor meeting! Error somewhere");
        }
    }



    public void onCardClick(View view, final int position) {
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TaskModel model = viewedTasksList.get(position); // todo : check this
                Log.e(TAG, "positions is " + position);
                Log.e(TAG, "title is " + model.getTitle());
                Log.e(TAG, "type is " + model.getType());
                if (model.getType().equalsIgnoreCase("VOTE")) {
                    Intent vote_view = new Intent(GroupTasksActivity.this, ViewVoteActivity.class);
                    vote_view.putExtra("id", model.getId());
                    startActivity(vote_view);
                }
            }
        });
    }


    private void showSnackBar(String message) {
        showSnackBar(message, "", "", "", "", snackbar.LENGTH_LONG);
    }

    private void showSnackBar(String message, final String actionButtontext, final String type,
                              final String response, final String positions, int length) {

        snackbar = Snackbar.make(rlActivityRoot, message, length);
        snackbar.setActionTextColor(Color.RED);

        if (!actionButtontext.isEmpty()) { // not entirely sure why/how this is set up
            snackbar.setAction(actionButtontext, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (type.equalsIgnoreCase("VoteMeeting")) {
                        TaskModel task = fullTasksList.get(Integer.parseInt(positions));
                        respondToTask(task.getId(), task.getType(), response);
                    } else {
                        respondToTask(fullTasksList.get(Integer.parseInt(positions)).getId(), "TODO", response);
                    }
                }
            });
        }
        snackbar.show();

    }

    private Bundle assembleFilterBundle() {
        Bundle b = new Bundle();
        b.putBoolean(Constant.VOTE, filterFlags.get(Constant.VOTE));
        b.putBoolean(Constant.MEETING, filterFlags.get(Constant.MEETING));
        b.putBoolean(Constant.TODO, filterFlags.get(Constant.TODO));
        return b;
    }

    public void confirmAction(final String taskType, final Call<GenericResponse> restCall, final String response,
                              final String msgSuccess, final String msgAlreadyResponded) {

        Log.e(TAG, "confirmAction" + taskType);
        int message = generateConfirmationMessage(taskType, response);
        DialogFragment newFragment = ConfirmCancelDialogFragment.newInstance(message, new ConfirmDialogListener() {
            @Override
            public void doConfirmClicked() {
                restCall.enqueue(new Callback<GenericResponse>() {
                    @Override
                    public void onResponse(Call<GenericResponse> call, Response<GenericResponse> response) {
                        mProgressBar.setVisibility(View.INVISIBLE);
                        if (response.isSuccessful()) {
                            showSnackBar(msgSuccess);
                            getTasks(); // refresh list of tasks (todo: make efficient, just refresh one activity)
                        } else {
                            if (response.code() == 409) { // todo: check this is right, and use constant, not hard code
                                showSnackBar(msgAlreadyResponded);
                            } else {
                                showSnackBar(getString(R.string.Unknown_error));
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<GenericResponse> call, Throwable t) {
                        mProgressBar.setVisibility(View.INVISIBLE);
                        if (t instanceof NoConnectivityException) {
                            imNoInternet.setVisibility(View.VISIBLE);
                        } else {
                            ErrorUtils.handleNetworkError(GroupTasksActivity.this, rlActivityRoot, t);
                        }
                    }
                });
            }
        });
        newFragment.show(getFragmentManager(), "dialog");
    }



    @Subscribe
    public void onEvent(TaskAddedEvent event){
        getTasks();
    }


    private int generateConfirmationMessage(final String taskType, final String response) {
        int confirmationMessage;

        Log.d(TAG, "Generating confirmation message.");
        switch (taskType) {
            case "VOTE":
                confirmationMessage = (response.equalsIgnoreCase("Yes")) ? R.string.RESPOND_YES : R.string.RESPOND_NO;
                break;
            case "MEETING":
                confirmationMessage = (response.equalsIgnoreCase("Yes")) ? R.string.RSVP_YES : R.string.RSVP_NO;
                break;
            case "TODO":
                confirmationMessage = R.string.TODO_COMPLETED;
                break;
            default:
                throw new UnsupportedOperationException("cannot have unknown task type");
        }
        return confirmationMessage;


    }
}