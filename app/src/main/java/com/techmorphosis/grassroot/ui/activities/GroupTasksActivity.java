package com.techmorphosis.grassroot.ui.activities;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
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
import com.techmorphosis.grassroot.interfaces.FilterInterface;
import com.techmorphosis.grassroot.interfaces.TaskListListener;
import com.techmorphosis.grassroot.R;
import com.techmorphosis.grassroot.adapters.TasksAdapter;
import com.techmorphosis.grassroot.services.GrassrootRestService;
import com.techmorphosis.grassroot.services.model.GenericResponse;
import com.techmorphosis.grassroot.services.model.TaskModel;
import com.techmorphosis.grassroot.services.model.TaskResponse;
import com.techmorphosis.grassroot.ui.fragments.FilterFragment;
import com.techmorphosis.grassroot.utils.Constant;
import com.techmorphosis.grassroot.utils.SettingPreference;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import retrofit.RetrofitError;
import rx.Observable;
import rx.Observer;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class GroupTasksActivity extends PortraitActivity implements TaskListListener {

    private static final String TAG = GroupTasksActivity.class.getCanonicalName();

    private String groupid;
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

    @BindView(R.id.error_layout)
    View errorLayout;
    @BindView(R.id.im_no_results)
    ImageView imNoResults;
    @BindView(R.id.im_server_error)
    ImageView imServerError;
    @BindView(R.id.im_no_internet)
    ImageView imNoInternet;

    private TasksAdapter group_activitiesAdapter;

    private Snackbar snackbar;
    public boolean vote_click = false, meeting_click = false, todo_click = false;
    private boolean clear_click = false;

    private List<TaskModel> voteList;
    private List<TaskModel> meetingList;
    private List<TaskModel> toDoList;
    public List<TaskModel> activitiesList;

    private GrassrootRestService grassrootRestService = new GrassrootRestService();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group__activities);
        ButterKnife.bind(this);

        Bundle extras = getIntent().getExtras();

        if (extras == null) {
            throw new UnsupportedOperationException("Group activities action called without group Uid!");
        }

        groupid = extras.getString("groupid");
        groupName = extras.getString("groupName");

        init();
        setUpViews();
        initRecyclerView();
        groupActivitiesWS();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // todo: check for permissions
        Log.e(TAG, "inside onCreateOptionsMenu!");
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_group_tasks, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.mi_icon_filter:
                filterTasks();
                return true;
            case R.id.mi_add_members:
                Intent addMember = new Intent(this, AddMembersActivity.class);
                addMember.putExtra(Constant.GROUPUID_FIELD, groupid);
                addMember.putExtra(Constant.GROUPNAME_FIELD, groupName);
                startActivity(addMember);
                Log.d(TAG, "user wants to add members!");
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void init() {
        phoneNumber = SettingPreference.getuser_mobilenumber(this);
        code = SettingPreference.getuser_token(this);
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
                    callNewTaskActivity();
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

    private void groupActivitiesWS() {

        mProgressBar.setVisibility(View.VISIBLE);
        activitiesList = new ArrayList<>();
        grassrootRestService.getApi().getGroupTasks(groupid, phoneNumber, code).subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<TaskResponse>() {
                    @Override
                    public void onCompleted() {
                        mProgressBar.setVisibility(View.INVISIBLE);
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.e(TAG, "Inside getActivities ... Here is the failure! " + e.getMessage());
                        mProgressBar.setVisibility(View.INVISIBLE);
                        imNoInternet.setVisibility(View.VISIBLE);
                    }

                    @Override
                    public void onNext(TaskResponse response) {
                        if (Constant.NO_GROUP_TASKS.equals(response.getMessage())) {
                            Log.e(TAG, "No group activities!");
                            callNewTaskActivity();
                        } else {
                            activitiesList = response.getTasks();
                            group_activitiesAdapter.clearTasks();
                            recycleViewGroupActivities.setVisibility(View.VISIBLE);
                            group_activitiesAdapter.addTasks(activitiesList);
                        }
                    }
                });
    }

    private void callNewTaskActivity() {
        Intent open = new Intent(this, NewActivities.class);
        open.putExtra(Constant.GROUPUID_FIELD, groupid);
        open.putExtra(Constant.GROUPNAME_FIELD, groupName);
        startActivity(open);
    }

    private void filterTasks() {
        Log.e(TAG, "before ");
        Log.e(TAG, "vote is " + vote_click);
        Log.e(TAG, "meeting is " + meeting_click);
        Log.e(TAG, "todo is " + todo_click);
        Log.e(TAG, "clear is " + clear_click);

        //sort
        FilterFragment sortFragment = new FilterFragment();
        Bundle b = new Bundle();
        b.putBoolean("Vote", vote_click);
        b.putBoolean("Meeting", meeting_click);
        b.putBoolean("ToDo", todo_click);
        b.putBoolean("Clear", clear_click);

        sortFragment.setArguments(b);
        sortFragment.show(getFragmentManager(), "FilterFragment");
        sortFragment.setListener(new FilterInterface() {

            @Override
            public void vote(boolean vote, boolean meeting, boolean todo, boolean clear) {
                Log.e(TAG, "vote is ");


                voteList = new ArrayList<TaskModel>();

                //validation
                for (int i = 0; i < activitiesList.size(); i++) {
                    if (activitiesList.get(i).getType().equalsIgnoreCase("Vote"))
                        voteList.add(activitiesList.get(i));
                }
                if (voteList.size() > 0) {

                    //Filter State
                    vote_click = true;
                    meeting_click = false;
                    todo_click = false;
                    clear_click = false;


                    //show progress
                    recycleViewGroupActivities.setVisibility(View.GONE);
                    mProgressBar.setVisibility(View.VISIBLE);

                    //pre-execute
                    group_activitiesAdapter.clearTasks();


                    Log.e(TAG, "activitiesList is " + activitiesList.size());
                    Log.e(TAG, "voteList is " + voteList.size());

                    //postExecute
                    //handle visibility
                    recycleViewGroupActivities.setVisibility(View.VISIBLE);
                    mProgressBar.setVisibility(View.GONE);

                    //set data for list
                    group_activitiesAdapter.addTasks(voteList);
                } else {
                    vote_click = false;
                    showSnackBar(getString(R.string.ga_noVote));
                }

                Log.e(TAG, "after ");
                Log.e(TAG, "vote is " + vote_click);
                Log.e(TAG, "meeting is " + meeting_click);
                Log.e(TAG, "todo is " + todo_click);
                Log.e(TAG, "clear is " + clear_click);

            }

            @Override
            public void meeting(boolean vote, boolean meeting, boolean todo, boolean clear) {

                Log.e(TAG, "meeting is ");


                meetingList = new ArrayList<TaskModel>();
                for (int i = 0; i < activitiesList.size(); i++) {
                    if (activitiesList.get(i).getType().equalsIgnoreCase("Meeting"))
                        meetingList.add(activitiesList.get(i));
                }
                if (meetingList.size() > 0) {
                    //Filter State
                    meeting_click = true;
                    vote_click = false;
                    todo_click = false;
                    clear_click = false;

                    //show progress
                    recycleViewGroupActivities.setVisibility(View.GONE);
                    mProgressBar.setVisibility(View.VISIBLE);


                    Log.e(TAG, "activitiesList is " + activitiesList.size());
                    Log.e(TAG, "Meeting is " + meetingList.size());


                    //pre-execute
                    group_activitiesAdapter.clearTasks();


                    //doInBackground
                    //  groupList.clear_click();

                    //postExecute
                    //handle visibility
                    recycleViewGroupActivities.setVisibility(View.VISIBLE);
                    mProgressBar.setVisibility(View.GONE);

                    //set data for list
                    group_activitiesAdapter.addTasks(meetingList);

                } else {
                    meeting_click = false;

                    showSnackBar(getString(R.string.ga_noMeeting), "", "", "", "", snackbar.LENGTH_SHORT);

                }
                Log.e(TAG, "after ");
                Log.e(TAG, "vote is " + vote_click);
                Log.e(TAG, "meeting is " + meeting_click);
                Log.e(TAG, "todo is " + todo_click);
                Log.e(TAG, "clear is " + clear_click);
            }

            @Override
            public void todo(boolean vote, boolean meeting, boolean todo, boolean clear) {
                Log.e(TAG, "todo is ");


                toDoList = new ArrayList<TaskModel>();

                for (int i = 0; i < activitiesList.size(); i++) {
                    if (activitiesList.get(i).getType().equalsIgnoreCase("todo"))
                        toDoList.add(activitiesList.get(i));
                }
                if (toDoList.size() > 0) {
                    //Filter State
                    todo_click = true;
                    vote_click = false;
                    meeting_click = false;
                    clear_click = false;

                    //show progress
                    recycleViewGroupActivities.setVisibility(View.GONE);
                    mProgressBar.setVisibility(View.VISIBLE);


                    //pre-execute
                    group_activitiesAdapter.clearTasks();

                    Log.e(TAG, "activitiesList is " + activitiesList.size());
                    Log.e(TAG, "toDoList is " + toDoList.size());


                    //postExecute
                    //handle visibility
                    recycleViewGroupActivities.setVisibility(View.VISIBLE);
                    mProgressBar.setVisibility(View.GONE);

                    //set data for list
                    group_activitiesAdapter.addTasks(toDoList);

                } else {
                    todo_click = false;

                    showSnackBar(getString(R.string.ga_noToDo), "", "", "", "", snackbar.LENGTH_SHORT);

                }
                Log.e(TAG, "after ");
                Log.e(TAG, "vote is " + vote_click);
                Log.e(TAG, "meeting is " + meeting_click);
                Log.e(TAG, "todo is " + todo_click);
                Log.e(TAG, "clear is " + clear_click);
            }

            @Override
            public void clear(boolean vote, boolean meeting, boolean todo, boolean clear) {

                //Filter State
                vote_click = false;
                meeting_click = false;
                todo_click = false;
                clear_click = false;

                //show progress
                recycleViewGroupActivities.setVisibility(View.GONE);
                mProgressBar.setVisibility(View.VISIBLE);

                voteList = new ArrayList<>();
                meetingList = new ArrayList<>();
                toDoList = new ArrayList<>();

                //pre-execute
                group_activitiesAdapter.clearTasks();
                recycleViewGroupActivities.setVisibility(View.VISIBLE);
                mProgressBar.setVisibility(View.GONE);

                //set data for list
                group_activitiesAdapter.addTasks(activitiesList);

                Log.e(TAG, "after ");
                Log.e(TAG, "vote is " + vote_click);
                Log.e(TAG, "meeting is " + meeting_click);
                Log.e(TAG, "todo is " + todo_click);
                Log.e(TAG, "clear is " + clear_click);
            }
        });
    }

    @Override
    public void respondToTask(String taskUid, String taskType, String response) {

        Observable<GenericResponse> restCall;
        final String msgSuccess, msgAlreadyResponded;
        if (taskType.equals("VOTE")) {
            restCall = grassrootRestService.getApi().castVote(taskUid, phoneNumber, code, response);
            msgSuccess = getString(R.string.ga_Votesend);
            msgAlreadyResponded = getString(R.string.ga_VoteFailure);
        } else if (taskType.equals("MEETING")) {
            restCall = grassrootRestService.getApi().rsvp(taskUid, phoneNumber, code, response);
            msgSuccess = getString(R.string.ga_Meetingsend);
            msgAlreadyResponded = getString(R.string.ga_VoteFailure);
        } else if (taskType.equals("TODO")) {
            restCall = grassrootRestService.getApi().completeTodo(phoneNumber, code, taskUid);
            msgSuccess = getString(R.string.ga_ToDocompleted);
            msgAlreadyResponded = getString(R.string.ga_ToDoFailure);
        } else {
            throw new UnsupportedOperationException("Responding to neither vote nor meeting! Error somewhere");
        }

        restCall.observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<GenericResponse>() {
                    @Override
                    public void onCompleted() {
                        mProgressBar.setVisibility(View.INVISIBLE);
                    }

                    @Override
                    public void onError(Throwable e) {
                        mProgressBar.setVisibility(View.INVISIBLE);
                        RetrofitError retrofitError = (RetrofitError) e;
                        if (retrofitError.getKind().equals(RetrofitError.Kind.NETWORK)) {
                            imNoInternet.setVisibility(View.VISIBLE);
                        } else if (retrofitError.getResponse().getStatus() == 409) {
                            showSnackBar(msgAlreadyResponded);
                        } else {
                            showSnackBar(getString(R.string.Unknown_error));
                        }
                    }

                    @Override
                    public void onNext(GenericResponse genericResponse) {
                        groupActivitiesWS();
                        showSnackBar(msgSuccess);
                    }
                });
    }

    private void showSnackBar(String message) {
        showSnackBar(message, "", "", "", "", snackbar.LENGTH_SHORT);
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
                        TaskModel task = activitiesList.get(Integer.parseInt(positions));
                        respondToTask(task.getId(), task.getType(), response);
                    } else {
                        respondToTask(activitiesList.get(Integer.parseInt(positions)).getId(), "TODO", response);
                    }
                }
            });
        }
        snackbar.show();

    }

}
