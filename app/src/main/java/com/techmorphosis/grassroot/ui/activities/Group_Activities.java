package com.techmorphosis.grassroot.ui.activities;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.github.clans.fab.FloatingActionMenu;
import com.techmorphosis.grassroot.Interface.FilterInterface;
import com.techmorphosis.grassroot.R;
import com.techmorphosis.grassroot.adapters.Group_ActivitiesAdapter;
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
import butterknife.OnClick;
import retrofit.RetrofitError;
import rx.Observer;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class Group_Activities extends PortraitActivity {

    private static final String TAG = Group_Activities.class.getCanonicalName();

    private String groupid;
    private String groupName;
    private String phoneNumber;
    private String code;

    @BindView(R.id.rl_activity_root)
    RelativeLayout rlActivityRoot;

    @BindView(R.id.iv_ga_back)
    ImageView ivGaBack;
    @BindView(R.id.iv_ga_filter)
    ImageView ivGaFilter;

    @BindView(R.id.tv_ga_toolbar_txt)
    TextView tvGaToolbarTxt;
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

    private Group_ActivitiesAdapter group_activitiesAdapter;

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

    private void init() {
        phoneNumber = SettingPreference.getuser_mobilenumber(this);
        code = SettingPreference.getuser_token(this);
    }

    private void setUpViews() {

        ivGaFilter.setEnabled(false);
        tvGaToolbarTxt.setText(groupName);
        fabbutton.setOnMenuToggleListener(new FloatingActionMenu.OnMenuToggleListener() {
            @Override
            public void onMenuToggle(boolean opened) {
                if (opened) {
                    // todo: check & pass permissions
                    fabbutton.toggle(false);
                    Intent open = new Intent(Group_Activities.this, NewActivities.class);
                    open.putExtra(Constant.GROUPUID_FIELD, groupid);
                    open.putExtra(Constant.GROUPNAME_FIELD, groupName);
                    startActivity(open);
                    overridePendingTransition(R.anim.push_up_in, R.anim.push_up_out);
                } else {

                }
            }
        });
    }

    private void initRecyclerView() {
        recycleViewGroupActivities.setLayoutManager(new LinearLayoutManager(Group_Activities.this));
        recycleViewGroupActivities.setItemAnimator(new CustomItemAnimator());
        group_activitiesAdapter = new Group_ActivitiesAdapter(new ArrayList<TaskModel>(), Group_Activities.this);
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
                        mProgressBar.setVisibility(View.INVISIBLE);
                        imNoInternet.setVisibility(View.VISIBLE);
                    }
                    @Override
                    public void onNext(TaskResponse response) {
                        activitiesList = response.getTasks();
                        populateCompletionStatus(activitiesList);
                        group_activitiesAdapter.clearTasks();
                        recycleViewGroupActivities.setVisibility(View.VISIBLE);
                        group_activitiesAdapter.addTasks(activitiesList);
                        ivGaFilter.setEnabled(true);
                    }
                });
    }

    private void ToDo(TaskModel model) {
        if (model.getType().equalsIgnoreCase("COMPLETED")) {
            model.setCompletedYes("disableclick");
            model.setCompletedNo("disableclick");
        } else {
            model.setCompletedYes("enableclick");
            model.setCompletedNo("disableclick");
        }
    }

    private void votemeeting(TaskModel model) {
        canAction(model);
    }

    private void canAction(TaskModel model) {
        if (model.getCanAction()) {
            if (model.getHasResponded()) {
                canActionIsTrue(model);
            } else {
                canActionIsTrue2(model);
            }
        } else if (!model.getCanAction()) {
            canActionIsFalse(model);
        }
    }

    private void canActionIsTrue2(TaskModel model) {
        model.setThumbsUp("enableclick");
        model.setThumbsDown("enableclick");
    }

    private void canActionIsFalse(TaskModel model) {
        model.setThumbsUp("disableclick");
        model.setThumbsDown("disableclick");
    }

    private void canActionIsTrue(TaskModel model) {
        if (model.getReply().equalsIgnoreCase("Yes")) {
            model.setThumbsUp("disableclick");
            model.setThumbsDown("enableclick");
        } else if (model.getReply().equalsIgnoreCase("NO_RESPONSE")) {
            model.setThumbsUp("enableclick");
            model.setThumbsDown("disableclick");
        }
    }


    @OnClick(R.id.iv_ga_filter)
    public void ivGaFilter() {
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
                    showSnackBar(getString(R.string.ga_noVote), "", "", "", "", snackbar.LENGTH_SHORT);
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

    @OnClick(R.id.iv_ga_back)
    public void ivGaBack() {
        finish();

    }

    public void thumbsUp(ImageView iv2, final int position) {
        iv2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                callVoteMeetingWS(position, "Yes");
            }
        });

    }

    private void callVoteMeetingWS(final int position, String response) {

        String id = activitiesList.get(position).getId();
        if (activitiesList.get(position).getType().equalsIgnoreCase("VOTE")) {
            grassrootRestService.getApi().castVote(id, phoneNumber, code, response)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Observer<GenericResponse>() {
                        @Override
                        public void onCompleted() {
                            mProgressBar.setVisibility(View.INVISIBLE);
                        }

                        @Override
                        public void onError(Throwable e) {
                            mProgressBar.setVisibility(View.INVISIBLE);
                            errorLayout.setVisibility(View.VISIBLE);
                            RetrofitError retrofitError = (RetrofitError) e;
                            if (retrofitError.getKind().equals(RetrofitError.Kind.NETWORK)) {
                                imNoInternet.setVisibility(View.VISIBLE);
                            } else if (retrofitError.getResponse().getStatus() == 409) {
                                showSnackBar(getString(R.string.ga_VoteFailure), "", "", "", "", snackbar.LENGTH_SHORT);
                            } else {
                                showSnackBar(getString(R.string.Unknown_error), "", "", "", "", snackbar.LENGTH_SHORT);
                            }

                        }

                        @Override
                        public void onNext(GenericResponse response) {
                            groupActivitiesWS();
                            showSnackBar(getString(R.string.ga_Votesend), "", "", "", "", Snackbar.LENGTH_SHORT);


                        }
                    });

        } else if (activitiesList.get(position).getType().equalsIgnoreCase("MEETING")) {

            grassrootRestService.getApi().castVote(id, phoneNumber, code, response)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Observer<GenericResponse>() {
                        @Override
                        public void onCompleted() {
                            mProgressBar.setVisibility(View.INVISIBLE);
                        }

                        @Override
                        public void onError(Throwable e) {
                            mProgressBar.setVisibility(View.INVISIBLE);
                            errorLayout.setVisibility(View.VISIBLE);
                            RetrofitError retrofitError = (RetrofitError) e;
                            if (retrofitError.getKind().equals(RetrofitError.Kind.NETWORK)) {
                                imNoInternet.setVisibility(View.VISIBLE);

                            } else if (retrofitError.getResponse().getStatus() == 409) {
                                showSnackBar(getString(R.string.ga_VoteFailure), "", "", "", "", snackbar.LENGTH_SHORT);
                            } else {
                                showSnackBar(getString(R.string.Unknown_error), "", "", "", "", snackbar.LENGTH_SHORT);
                            }
                        }

                        @Override
                        public void onNext(GenericResponse response) {
                            groupActivitiesWS();
                            showSnackBar(getString(R.string.ga_Meetingsend), "", "", "", "", Snackbar.LENGTH_SHORT);

                        }
                    });


        }

    }

    public void thumbsDown(View iv3, final int position) {
        iv3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "Voting No");
                callVoteMeetingWS(position, "No");

            }
        });

    }

    public void completed(ImageView iv2, final int position, final String response) {
        iv2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                callToDoWS(position, response);
            }
        });

    }
    private void callToDoWS(final int position, final String response) {

        String id = activitiesList.get(position).getId();
        grassrootRestService.getApi().completeTodo(id,phoneNumber,code)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<GenericResponse>() {
                    @Override
                    public void onCompleted() {
                        mProgressBar.setVisibility(View.INVISIBLE);
                    }

                    @Override
                    public void onError(Throwable e) {
                        mProgressBar.setVisibility(View.INVISIBLE);
                        errorLayout.setVisibility(View.VISIBLE);
                        RetrofitError retrofitError = (RetrofitError) e;
                        if (retrofitError.getKind().equals(RetrofitError.Kind.NETWORK)) {
                            imNoInternet.setVisibility(View.VISIBLE);

                        } else if (retrofitError.getResponse().getStatus() == 409) {
                            showSnackBar(getString(R.string.ga_ToDoFailure), "", "", "", "", snackbar.LENGTH_SHORT);
                        } else {
                            showSnackBar(getString(R.string.Unknown_error), "", "", "", "", snackbar.LENGTH_SHORT);
                        }
                    }

                    @Override
                    public void onNext(GenericResponse response) {
                        groupActivitiesWS();
                        showSnackBar(getString(R.string.ga_ToDocompleted), "", "", "", "", Snackbar.LENGTH_SHORT);

                    }
                });


    }


    private void showSnackBar(String message, final String actionButtontext, final String type, final String response, final String positions, int length) {
        snackbar = Snackbar.make(rlActivityRoot, message, length);
        snackbar.setActionTextColor(Color.RED);

        if (!actionButtontext.isEmpty()) {
            snackbar.setAction(actionButtontext, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (type.equalsIgnoreCase("VoteMeeting")) {
                        callVoteMeetingWS(Integer.parseInt(positions), response);
                    } else {
                        callToDoWS(Integer.parseInt(positions), response);

                    }
                }
            });
        }
        snackbar.show();

    }

    private void populateCompletionStatus(List<TaskModel> taskModels) {
        for (TaskModel taskModel : taskModels) {
            if (taskModel.getType().equalsIgnoreCase("VOTE") || taskModel.getType().equalsIgnoreCase("MEETING")) {
                votemeeting(taskModel);
            } else {
                ToDo(taskModel);
            }

        }
    }

}
