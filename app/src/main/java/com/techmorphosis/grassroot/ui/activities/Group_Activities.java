package com.techmorphosis.grassroot.ui.activities;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.android.volley.AuthFailureError;
import com.android.volley.NoConnectionError;
import com.android.volley.ServerError;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.techmorphosis.grassroot.Animator.CustomItemAnimator;
import com.techmorphosis.grassroot.Interface.FilterInterface;
import com.techmorphosis.grassroot.Network.AllLinsks;
import com.techmorphosis.grassroot.Network.NetworkCall;
import com.techmorphosis.grassroot.R;
import com.techmorphosis.grassroot.adapters.Group_ActivitiesAdapter;
import com.techmorphosis.grassroot.models.Group_ActivitiesModel;
import com.techmorphosis.grassroot.ui.fragments.FilterFragment;
import com.techmorphosis.grassroot.utils.L;
import com.techmorphosis.grassroot.utils.ProgressBarCircularIndeterminate;
import com.techmorphosis.grassroot.utils.SettingPreffrence;
import com.techmorphosis.grassroot.utils.UtilClass;
import com.techmorphosis.grassroot.utils.listener.ErrorListenerVolley;
import com.techmorphosis.grassroot.utils.listener.ResponseListenerVolley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

public class Group_Activities extends PortraitActivity  implements View.OnClickListener{


    private RelativeLayout gaToolbar;
    private ImageView ivGaBack;
    private TextView tvGaToolbarTxt;
    private RecyclerView rcGa;

    public ArrayList<Group_ActivitiesModel> activitiesList;
    private UtilClass utilclass;
    private ImageView ivGaFilter;
    private String groupid;

    private View errorLayout;
    private LinearLayout llNoResult;
    private LinearLayout llNoInternet;
    private LinearLayout llServerError;
    private LinearLayout llInvalidToken;


    private LinearLayoutManager mLayoutManager;
    private Group_ActivitiesAdapter group_activitiesAdapter;
    private static final String TAG = "Group_Activities";
    private Snackbar snackbar;
    private RelativeLayout rlActivityRoot;
    public boolean vote_click =false, meeting_click =false, todo_click =false;
    private ArrayList<Group_ActivitiesModel> voteList;
    private ArrayList<Group_ActivitiesModel> meetingList;
    private ArrayList<Group_ActivitiesModel> toDoList;
    private ProgressBar mProgressBar;
    private boolean clear_click =false;
    private FloatingActionButton fabbutton;
    private String groupName;
    private int error_flag;//0-success 1- no Internet 2- Invalid Token 3- Unknown error
    private ProgressBarCircularIndeterminate prgGa;
    private TextView txtPrgGa;
    private View v;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group__activities);

        Bundle extras = getIntent().getExtras();
        if(extras != null) {
            Log.e(TAG, "not null ");
            groupid = extras.getString("groupid");
            groupName = extras.getString("groupName");
        }



        findAllViews();
        init();
        recyclerview();
        Group_Activities_WS();
    }

    private void recyclerview()
    {

        mLayoutManager = new LinearLayoutManager(Group_Activities.this);
        rcGa.setLayoutManager(mLayoutManager);
        rcGa.setItemAnimator(new CustomItemAnimator());
        group_activitiesAdapter = new Group_ActivitiesAdapter(new ArrayList<Group_ActivitiesModel>(),Group_Activities.this);
        rcGa.setAdapter(group_activitiesAdapter);

    }

    private void Group_Activities_WS()
    {

        //preExecute
        preExecute();


        //doInBackground
        doInBackground();



    }


    private void preExecute() {

        activitiesList = new ArrayList<>();

        error_flag = 0;

        //visible
        prgGa.setVisibility(View.VISIBLE);
        txtPrgGa.setVisibility(View.VISIBLE);

        //gone
        rcGa.setVisibility(View.INVISIBLE);
        errorLayout.setVisibility(View.GONE);
        llNoInternet.setVisibility(View.GONE);
        llServerError.setVisibility(View.GONE);
        llNoResult.setVisibility(View.GONE);

    }

    private void doInBackground() {


        NetworkCall networkCall = new NetworkCall
                (
                        Group_Activities.this,

                        new ResponseListenerVolley() {
                            @Override
                            public void onSuccess(String response) {

                                error_flag = 0;
                                postExecute(response);


                            }
                        },

                        new ErrorListenerVolley() {
                            @Override
                            public void onError(VolleyError volleyError) {

                                if ((volleyError instanceof NoConnectionError)|| (volleyError instanceof TimeoutError)) {
                                    error_flag = 1;
                                    postExecute("");
                                }
                                else if (volleyError instanceof ServerError)
                                {
                                    String response = null, status, message;
                                    try {
                                        response = new String(volleyError.networkResponse.data, "utf-8");
                                        L.e(TAG, "response is  ", response);
                                        JSONObject jsonObject_error = new JSONObject(response);

                                        error_flag = 0;
                                        postExecute(response);

                                    } catch (UnsupportedEncodingException e) {// Data is not able to UnsupportedEncodingException
                                        e.printStackTrace();

                                        error_flag = 5;
                                        postExecute("");

                                    } catch (JSONException e) {// Data is not able to parse
                                        e.printStackTrace();

                                        error_flag= 5;
                                        postExecute("");
                                    }


                                }
                                else if (volleyError instanceof AuthFailureError) {
                                    error_flag = 2;
                                    postExecute("");
                                }
                                else {//Unknown error
                                    error_flag = 5;
                                    postExecute("");
                                }

                            }
                        },
                        AllLinsks.groupactivities + groupid + "/" + SettingPreffrence.getPREF_Phone_Token(Group_Activities.this),
                        getString(R.string.prg_message),
                        false

                );

        networkCall.makeStringRequest_GET();
    }

    private void postExecute(String response) {

        Log.e(TAG,"error_flag is " + error_flag);

        if (error_flag == 1) {//no Internet
            prgGa.setVisibility(View.GONE);
            txtPrgGa.setVisibility(View.GONE);

            errorLayout.setVisibility(View.VISIBLE);
            llNoInternet.setVisibility(View.VISIBLE);
        }
        else if (error_flag==2) {// Invalid Token
            prgGa.setVisibility(View.GONE);
            txtPrgGa.setVisibility(View.GONE);

            errorLayout.setVisibility(View.VISIBLE);
            llInvalidToken.setVisibility(View.VISIBLE);
        }
        else if (error_flag==3) {//Unknown error
            prgGa.setVisibility(View.GONE);
            txtPrgGa.setVisibility(View.GONE);

            errorLayout.setVisibility(View.VISIBLE);
            llServerError.setVisibility(View.VISIBLE);
        }
        else if (error_flag==0) {

            try {
                JSONObject jsonObject= new JSONObject(response);
                if (jsonObject.getString("status").equalsIgnoreCase("SUCCESS"))
                {


                    //proceed
                    JSONArray jsonArray = jsonObject.getJSONArray("data");

                    if (jsonArray.length() > 0) {

                        for (int i = 0; i < jsonArray.length(); i++) {
                            JSONObject array_jsonobject = (JSONObject) jsonArray.get(i);
                            Group_ActivitiesModel model = new Group_ActivitiesModel();
                            model.id = array_jsonobject.getString("id");
                            model.title = array_jsonobject.getString("title");
                            model.description = array_jsonobject.getString("description");
                            model.name = array_jsonobject.getString("name");
                            model.type = array_jsonobject.getString("type");
                            model.deadline = array_jsonobject.getString("deadline");
                            model.hasResponded = array_jsonobject.getBoolean("hasResponded");
                            model.canAction = array_jsonobject.getBoolean("canAction");
                            model.reply = array_jsonobject.getString("reply");
                            if (model.type.equalsIgnoreCase("VOTE")) {
                                votemeeting(model);
                            } else if (model.type.equalsIgnoreCase("MEETING")) {
                                votemeeting(model);
                            } else if (model.type.equalsIgnoreCase("TODO")) {
                                ToDo(model);
                            }
                            activitiesList.add(model);

                        }


                        group_activitiesAdapter.clearApplications();


                        group_activitiesAdapter.addApplications(activitiesList);

                        //step1-hide the loader
                        prgGa.setVisibility(View.GONE);
                        txtPrgGa.setVisibility(View.GONE);

                        //step2- show the list
                        rcGa.setVisibility(View.VISIBLE);

                        //step3- now enable the ui onclick
                        ivGaFilter.setEnabled(true);


                    } else {

                        //No result
                        prgGa.setVisibility(View.GONE);
                        txtPrgGa.setVisibility(View.GONE);

                        errorLayout.setVisibility(View.VISIBLE);
                        llNoResult.setVisibility(View.VISIBLE);

                    }




                }
                else if (jsonObject.getString("status").equalsIgnoreCase("Failure"))
                {
                    Log.e(TAG, "Failure ");

                    prgGa.setVisibility(View.GONE);
                    txtPrgGa.setVisibility(View.GONE);


                    errorLayout.setVisibility(View.VISIBLE);
                    llNoResult.setVisibility(View.VISIBLE);
                }

            } catch (JSONException e) {
                e.printStackTrace();

                Log.e(TAG, "JSONException is " + e.getMessage());

                prgGa.setVisibility(View.GONE);
                txtPrgGa.setVisibility(View.GONE);

                errorLayout.setVisibility(View.VISIBLE);
                llServerError.setVisibility(View.VISIBLE);
            }

        }
        else {
            Log.e(TAG, "case not match is "  );

            prgGa.setVisibility(View.GONE);
            txtPrgGa.setVisibility(View.GONE);

            errorLayout.setVisibility(View.VISIBLE);
            llServerError.setVisibility(View.VISIBLE);
        }

    }


    private void ToDo(Group_ActivitiesModel model)
    {
        if (model.reply.equalsIgnoreCase("COMPLETED"))
        {
            model.completedyes="disableclick";
            model.completedno="disableclick";
        }
        else
        {
            model.completedyes="enableclick";
            model.completedno="disableclick";

        }
    }

    private void votemeeting(Group_ActivitiesModel model)
    {
        canAction(model);

    }

    private void canAction(Group_ActivitiesModel model)
    {
        if (model.canAction)
        {

            if (model.hasResponded)
            {  //model.hasResponded is true

                //model.canAction is true
                canActionIsTrue(model);
            }
            else
            {
                //model.hasResponded is false


                //model.canAction2 is true
                canActionIsTrue2(model);

            }



        }
        else if (!model.canAction)
        {

            //model.canAction is false
            canActionIsFalse(model);


        }

    }

    private void canActionIsTrue2(Group_ActivitiesModel model)
    {
        model.Thumpsup="enableclick";
        model.Thumpsdown="enableclick";

    }

    private void canActionIsFalse(Group_ActivitiesModel model)
    {

        model.Thumpsup="disableclick";
        model.Thumpsdown="disableclick";

    }

    private void canActionIsTrue(Group_ActivitiesModel model)
    {
        if (model.reply.equalsIgnoreCase("Yes"))
        {

            model.Thumpsup="disableclick";
            model.Thumpsdown="enableclick";


        }
        else  if (model.reply.equalsIgnoreCase("NO_RESPONSE"))
        {

            model.Thumpsup="enableclick";
            model.Thumpsdown="disableclick";

        }
    }

    private void init()
    {
//        activitiesList = new ArrayList<>();
        utilclass = new UtilClass();

    }

    private void findAllViews()
    {
        rlActivityRoot = (RelativeLayout) findViewById(R.id.rl_activity_root);
        gaToolbar = (RelativeLayout) findViewById(R.id.ga_toolbar);
        ivGaBack = (ImageView) findViewById(R.id.iv_ga_back);
        ivGaFilter = (ImageView) findViewById(R.id.iv_ga_filter);
        tvGaToolbarTxt = (TextView) findViewById(R.id.tv_ga_toolbar_txt);
        rcGa = (RecyclerView) findViewById(R.id.rc_ga);

        errorLayout = (View) findViewById(R.id.error_layout);
        prgGa = (ProgressBarCircularIndeterminate) findViewById(R.id.prg_ga);
        txtPrgGa = (TextView) findViewById(R.id.txt_prg_ga);


        errorLayout = findViewById(R.id.error_layout);

        llNoResult = (LinearLayout) errorLayout.findViewById(R.id.ll_no_result);
        llNoInternet = (LinearLayout) errorLayout.findViewById(R.id.ll_no_internet);
        llServerError = (LinearLayout) errorLayout.findViewById(R.id.ll_server_error);
        llInvalidToken = (LinearLayout) errorLayout.findViewById(R.id.ll_invalid_token);


        // Handle ProgressBar
        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
        fabbutton = (FloatingActionButton) findViewById(R.id.fabbutton);

        ivGaFilter.setEnabled(false);
        ivGaFilter.setOnClickListener(ivGaFilter());
        ivGaBack.setOnClickListener(ivGaBack());
        tvGaToolbarTxt.setText(groupName);

        fabbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SettingPreffrence.setGroupId(Group_Activities.this,groupid);
                Log.e(TAG,"groupid is " + groupid);
                Intent open= new Intent(Group_Activities.this,NewActivities.class);
                startActivity(open);
                overridePendingTransition(R.anim.push_up_in, R.anim.push_up_out);
            }

        });

        //onClick
        llNoResult.setOnClickListener(this);
        llServerError.setOnClickListener(this);
        llNoInternet.setOnClickListener(this);
        llInvalidToken.setOnClickListener(this);

    }

    private View.OnClickListener ivGaFilter() {
        return  new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Log.e(TAG, "before ");
                Log.e(TAG, "vote is " + vote_click);
                Log.e(TAG, "meeting is " + meeting_click);
                Log.e(TAG, "todo is " + todo_click);
                Log.e(TAG, "clear is " + clear_click);

                //sort
                FilterFragment sortFragment= new FilterFragment();
                Bundle b = new Bundle();
                b.putBoolean("Vote", vote_click);
                b.putBoolean("Meeting",meeting_click);
                b.putBoolean("ToDo",todo_click);
                b.putBoolean("Clear", clear_click);

                sortFragment.setArguments(b);
                sortFragment.show(getFragmentManager(), "FilterFragment");
                sortFragment.setListener(new FilterInterface() {
                    @Override
                    public void vote(boolean vote, boolean meeting, boolean todo, boolean clear) {
                        Log.e(TAG, "vote is ");



                        voteList = new ArrayList<Group_ActivitiesModel>();

                        //validation
                        for (int i = 0; i < activitiesList.size(); i++) {
                            if (activitiesList.get(i).type.equalsIgnoreCase("Vote"))
                                voteList.add(activitiesList.get(i));
                        }
                        if (voteList.size() > 0) {

                            //Filter State
                            vote_click = true;
                            meeting_click = false;
                            todo_click = false;
                            clear_click = false;


                            //show progress
                            rcGa.setVisibility(View.GONE);
                            mProgressBar.setVisibility(View.VISIBLE);

                            //pre-execute
                            group_activitiesAdapter.clearApplications();


                            Log.e(TAG, "activitiesList is " + activitiesList.size());
                            Log.e(TAG, "voteList is " + voteList.size());

                            //postExecute
                            //handle visibility
                            rcGa.setVisibility(View.VISIBLE);
                            mProgressBar.setVisibility(View.GONE);

                            //set data for list
                            group_activitiesAdapter.addApplications(voteList);
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




                        meetingList = new ArrayList<Group_ActivitiesModel>();
                        for (int i = 0; i < activitiesList.size(); i++) {
                            if (activitiesList.get(i).type.equalsIgnoreCase("Meeting"))
                                meetingList.add(activitiesList.get(i));
                        }
                        if (meetingList.size() > 0) {
                            //Filter State
                            meeting_click = true;
                            vote_click = false;
                            todo_click = false;
                            clear_click = false;

                            //show progress
                            rcGa.setVisibility(View.GONE);
                            mProgressBar.setVisibility(View.VISIBLE);



                            Log.e(TAG, "activitiesList is " + activitiesList.size());
                            Log.e(TAG, "Meeting is " + meetingList.size());


                            //pre-execute
                            group_activitiesAdapter.clearApplications();


                            //doInBackground
                            //  groupList.clear_click();

                            //postExecute
                            //handle visibility
                            rcGa.setVisibility(View.VISIBLE);
                            mProgressBar.setVisibility(View.GONE);

                            //set data for list
                            group_activitiesAdapter.addApplications(meetingList);

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


                        toDoList = new ArrayList<Group_ActivitiesModel>();

                        for (int i = 0; i < activitiesList.size(); i++) {
                            if (activitiesList.get(i).type.equalsIgnoreCase("todo"))
                                toDoList.add(activitiesList.get(i));
                        }
                        if (toDoList.size() > 0) {
                            //Filter State
                            todo_click = true;
                            vote_click = false;
                            meeting_click = false;
                            clear_click = false;

                            //show progress
                            rcGa.setVisibility(View.GONE);
                            mProgressBar.setVisibility(View.VISIBLE);


                            //pre-execute
                            group_activitiesAdapter.clearApplications();

                            Log.e(TAG, "activitiesList is " + activitiesList.size());
                            Log.e(TAG, "toDoList is " + toDoList.size());


                            //postExecute
                            //handle visibility
                            rcGa.setVisibility(View.VISIBLE);
                            mProgressBar.setVisibility(View.GONE);

                            //set data for list
                            group_activitiesAdapter.addApplications(toDoList);

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
                        rcGa.setVisibility(View.GONE);
                        mProgressBar.setVisibility(View.VISIBLE);

                        voteList = new ArrayList<Group_ActivitiesModel>();
                        meetingList = new ArrayList<Group_ActivitiesModel>();
                        toDoList = new ArrayList<Group_ActivitiesModel>();

                        //pre-execute
                        group_activitiesAdapter.clearApplications();

                        //doInBackground


                        //postExecute
                        //handle visibility
                        rcGa.setVisibility(View.VISIBLE);
                        mProgressBar.setVisibility(View.GONE);

                        //set data for list
                        group_activitiesAdapter.addApplications(activitiesList);

                        Log.e(TAG, "after ");
                        Log.e(TAG, "vote is " + vote_click);
                        Log.e(TAG, "meeting is " + meeting_click);
                        Log.e(TAG, "todo is " + todo_click);
                        Log.e(TAG, "clear is " + clear_click);
                    }
                });


            }
        };
    }

    private View.OnClickListener ivGaBack() {
        return  new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        };
    }

    public void thumps_Up(ImageView iv2, final int position) {
        iv2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
              //  Toast.makeText(getBaseContext(), "thumps_Up position is " + position, Toast.LENGTH_LONG).show();
                CallVoteMeetingWS(position,"Yes");
            }
        });

    }

    private void CallVoteMeetingWS(final int position, String response) {
        //preExecute
            String Url = null;
        String type = null;

        if (activitiesList.get(position).type.equalsIgnoreCase("VOTE")) {


            type = "Vote";
            Url = AllLinsks.Vote + activitiesList.get(position).id + "/" + SettingPreffrence.getPREF_Phone_Token(Group_Activities.this) + "?response=" + response;
            Log.e(TAG, "VoteUrl is " + Url);

        }
        else if (activitiesList.get(position).type.equalsIgnoreCase("MEETING"))
        {


            type="Meeting";
            Url=AllLinsks.Meeting + activitiesList.get(position).id + "/" + SettingPreffrence.getPREF_Phone_Token(Group_Activities.this) + "?response=" + response;
            Log.e(TAG,"MeetingUrl is " + Url );

        }

        //doInBacground
        final String finalResponse = response;
        final String finalResponse1 = response;
        final String finalType = type;
        NetworkCall networkCall = new NetworkCall
                (
                        Group_Activities.this,

                        new ResponseListenerVolley() {
                            @Override
                            public void onSuccess(String s) {
                                //parse string to json
                                try {
                                    JSONObject jsonObject = new JSONObject(s);

                                    if (jsonObject.getString("status").equalsIgnoreCase("SUCCESS")) {


                                        Group_Activities_WS();

                                        if (finalType.equalsIgnoreCase("Vote"))
                                        {
                                            showSnackBar(getString(R.string.ga_Votesend), "", "", "", "",Snackbar.LENGTH_SHORT);
                                        }
                                        else  if (finalType.equalsIgnoreCase("Meeting"))
                                        {
                                            showSnackBar(getString(R.string.ga_Meetingsend), "", "", "", "",Snackbar.LENGTH_SHORT);

                                        }


                                    }

                                } catch (JSONException e) {
                                    e.printStackTrace();
                                    //failed to parse nly due to no internet
                                    showSnackBar(getString(R.string.No_network), getString(R.string.Retry), "VoteMeeting", finalResponse1, String.valueOf(position), Snackbar.LENGTH_INDEFINITE);

                                }


                            }
                        },

                        new ErrorListenerVolley() {
                            @Override
                            public void onError(VolleyError volleyError) {
                                if (volleyError instanceof NoConnectionError || volleyError instanceof TimeoutError)
                                {
                                    showSnackBar(getString(R.string.Unknown_error), "", "", "", "", snackbar.LENGTH_SHORT);

                                }
                                else if (volleyError instanceof ServerError)
                                {
                                    try {
                                        String responsebody = new String(volleyError.networkResponse.data, "utf-8");
                                        Log.e(TAG, "responseBody " + responsebody);
                                        JSONObject jsonObject = new JSONObject(responsebody);
                                        String status = jsonObject.getString("status");
                                        String message = jsonObject.getString("message");
                                        if (status.equalsIgnoreCase("SUCCESS")) {
                                            Log.e(TAG, "status is" + status);
                                            Log.e(TAG, "message is" + message);
                                            if (jsonObject.getString("code").equalsIgnoreCase("409")) {
                                                showSnackBar(getString(R.string.ga_VoteFailure), "", "", "", "", snackbar.LENGTH_SHORT);
                                            } else {
                                                showSnackBar(getString(R.string.Unknown_error), "", "", "", "", snackbar.LENGTH_SHORT);

                                            }

                                        }


                                    } catch (UnsupportedEncodingException e) {
                                        e.printStackTrace();
                                        showSnackBar(getString(R.string.Unknown_error), "", "", "", "", snackbar.LENGTH_SHORT);

                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                        showSnackBar(getString(R.string.Unknown_error), "", "", "", "", snackbar.LENGTH_SHORT);

                                    }

                                }
                                else if (volleyError instanceof AuthFailureError)
                                {
                                    showSnackBar(getString(R.string.INVALID_TOKEN), "", "", "", "", snackbar.LENGTH_SHORT);

                                }


                            }
                        },

                        Url,
                        getString(R.string.prg_message),
                        true

                );

        networkCall.makeStringRequest_GET();


        //postExecute
        /*update model and notify adapter*/

    }

    public void thumps_Down(View iv3, final int position) {
        iv3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
             //   Toast.makeText(getBaseContext(), "thumps_Down position is " + position, Toast.LENGTH_LONG).show();
                CallVoteMeetingWS(position, "No");

            }
        });



    }
    public void CardView(View mainView, final int position)
    {
        mainView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Group_ActivitiesModel model;
                if (vote_click)
                {
                     model = voteList.get(position);

                }
                else if (meeting_click)
                {
                     model = meetingList.get(position);

                }
                else if (todo_click)
                {
                     model = toDoList.get(position);

                }
                else
                {
                     model = activitiesList.get(position);

                }
                Log.e(TAG,"positions is " + position);
                Log.e(TAG,"title is " + model.title);
                Log.e(TAG,"type is " + model.type);
                if (model.type.equalsIgnoreCase("VOTE")) {
                    Intent vote_view = new Intent(Group_Activities.this, ViewVote.class);
                    vote_view.putExtra("voteid", model.id);
                    startActivityForResult(vote_view,1);
                }
            }
        });
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);


        if (resultCode == 1 && requestCode == 1) {

            Log.e(this.TAG, "resultCode==1 ");
            if (data != null) {
                if (data.getStringExtra("update").equals("1"))
                {
                    Log.e(TAG,"update");
                    Group_Activities_WS();

                }
                else
                {
                    Log.e(TAG," nothing to update");
                }
            }
        } else  {
            Log.e(this.TAG, "resultCode==2");

        }
    }


    public void Completed(ImageView iv2, final int position, final String response)
    {
        iv2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Toast.makeText(getBaseContext(), "Completed position is " + position, Toast.LENGTH_LONG).show();
                CallToDoWS(position, response);
            }
        });

    }

    private void CallToDoWS(final int position, final String response)
    {

        //doInBacground
        NetworkCall networkCall = new NetworkCall
                (
                        Group_Activities.this,

                        new ResponseListenerVolley() {
                            @Override
                            public void onSuccess(String s) {
                                //parse string to json
                                try {
                                    JSONObject jsonObject = new JSONObject(s);
                                    if (jsonObject.getString("status").equalsIgnoreCase("SUCCESS")) {


                                        Group_Activities_WS();


                                     /*   Group_ActivitiesModel update_model = activitiesList.get(position);

                                        //No responded previously
                                        update_model.hasResponded=true;
                                        update_model.canAction=false;
                                        update_model.reply=response;

*/
                                       /* if (update_model.reply.equalsIgnoreCase("Yes"))
                                        {
                                            update_model.reply="NO_RESPONSE";

                                        }
                                        else
                                        {
                                            //No responded previously
                                            update_model.hasResponded=true;
                                            update_model.canAction=false;
                                            update_model.reply="Yes";


                                        }*/
                                        //group_activitiesAdapter.notifyDataSetChanged();

                                        showSnackBar(getString(R.string.ga_ToDocompleted), "", "", "","", Snackbar.LENGTH_SHORT);

                                    }

                                } catch (JSONException e) {
                                    e.printStackTrace();
                                    //failed to parse nly due to no internet
                                    showSnackBar(getString(R.string.No_network), getString(R.string.Retry), "VoteMeeting",response, String.valueOf(position), Snackbar.LENGTH_INDEFINITE);

                                }


                            }
                        },

                        new ErrorListenerVolley() {
                            @Override
                            public void onError(VolleyError volleyError) {
                                if (volleyError instanceof  NoConnectionError || volleyError instanceof  TimeoutError)
                                {
                                    showSnackBar(getString(R.string.No_network), getString(R.string.Retry), "ToDoWs",response, String.valueOf(position), Snackbar.LENGTH_INDEFINITE);

                                }
                                else if (volleyError instanceof ServerError)
                                {

                                    try {
                                        String responsebody = new String(volleyError.networkResponse.data, "utf-8");
                                        Log.e(TAG, "responseBody " + responsebody);
                                        JSONObject jsonObject = new JSONObject(responsebody);
                                        String status = jsonObject.getString("status");
                                        String message = jsonObject.getString("message");
                                        if (status.equalsIgnoreCase("SUCCESS")) {
                                            Log.e(TAG, "status is" + status);
                                            Log.e(TAG, "message is" + message);
                                            if (jsonObject.getString("code").equalsIgnoreCase("409")) {
                                                showSnackBar(getString(R.string.ga_ToDoFailure), "", "", "", "", snackbar.LENGTH_SHORT);
                                            } else {
                                                showSnackBar(getString(R.string.Unknown_error), "", "", "", "", snackbar.LENGTH_SHORT);

                                            }

                                        }


                                    } catch (UnsupportedEncodingException e) {
                                        e.printStackTrace();
                                        showSnackBar(getString(R.string.Unknown_error), "", "", "", "", snackbar.LENGTH_SHORT);

                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                        showSnackBar(getString(R.string.Unknown_error), "", "", "", "", snackbar.LENGTH_SHORT);

                                    }

                                }
                                else if (volleyError instanceof  AuthFailureError)
                                {
                                    showSnackBar(getString(R.string.INVALID_TOKEN), "", "", "", "", snackbar.LENGTH_SHORT);

                                }
                                else
                                {
                                    showSnackBar(getString(R.string.Unknown_error), "", "", "", "", snackbar.LENGTH_SHORT);

                                }


                            }
                        },

                        AllLinsks.ToDo + activitiesList.get(position).id + "/" + SettingPreffrence.getPREF_Phone_Token(Group_Activities.this),
                        getString(R.string.prg_message),
                        true

                );

        networkCall.makeStringRequest_GET();

    }


    private void showSnackBar(String message, final String actionButtontext,  final String type,final  String response,final String positions,int length)
    {
        snackbar= Snackbar.make(rlActivityRoot, message, length);
        snackbar.setActionTextColor(Color.RED);

        if (!actionButtontext.isEmpty() )
        {
            snackbar.setAction(actionButtontext, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (type.equalsIgnoreCase("VoteMeeting"))
                    {
                        CallVoteMeetingWS(Integer.parseInt(positions), response);
                    }
                    else
                    {
                        CallToDoWS(Integer.parseInt(positions), response);

                    }
                }
            });
        }
        snackbar.show();

    }


    @Override
    public void onClick(View v) {

        /*if (v == llNoResult || v == llServerError || v == llNoInternet || v==llInvalidToken) {
            llNoResult.setAlpha((float) 0.3);
        }*/
        switch (v.getId())
        {
            case  R.id.ll_no_result :
                //  llNoResult.setAlpha((float) 0.2);
                Group_Activities_WS();
                break;
            case  R.id.ll_server_error :
                // llServerError.setAlpha((float) 0.2);
                Group_Activities_WS();

                break;
            case  R.id.ll_no_internet :
                // llNoInternet.setAlpha((float) 0.2);
                Group_Activities_WS();

                break;
         /*   case  R.id.ll_invalid_token :
                //llInvalidToken.setAlpha((float) 0.2);
                Group_Activities_WS();

                break;*/
        }


    }

    @Override
    protected void onResume() {
        super.onResume();
        if (SettingPreffrence.getPREF_Call_Vote(Group_Activities.this)) {

            SettingPreffrence.setPREF_Call_Vote(Group_Activities.this,false);

            showSnackBar(getString(R.string.nm_cratevote_msg), "", "", "", "", snackbar.LENGTH_SHORT);
            Group_Activities_WS();
        }
    }

}
