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

import com.android.volley.NoConnectionError;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.github.clans.fab.FloatingActionMenu;
import com.techmorphosis.grassroot.Interface.FilterInterface;
import com.techmorphosis.grassroot.Network.AllLinsks;
import com.techmorphosis.grassroot.Network.NetworkCall;
import com.techmorphosis.grassroot.R;
import com.techmorphosis.grassroot.adapters.Group_ActivitiesAdapter;
import com.techmorphosis.grassroot.models.Group_ActivitiesModel;
import com.techmorphosis.grassroot.ui.fragments.FilterFragment;
import com.techmorphosis.grassroot.utils.SettingPreffrence;
import com.techmorphosis.grassroot.utils.UtilClass;
import com.techmorphosis.grassroot.utils.listener.ErrorListenerVolley;
import com.techmorphosis.grassroot.utils.listener.ResponseListenerVolley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

public class Group_Activities extends PortraitActivity {

    private RelativeLayout gaToolbar;
    private ImageView ivGaBack;
    private TextView tvGaToolbarTxt;
    private RecyclerView rcGa;

    public ArrayList<Group_ActivitiesModel> activitiesList;
    private UtilClass utilclass;
    private ImageView ivGaFilter;
    private String groupid;
    private View errorLayout;
    private ImageView imNoResults;
    private ImageView imServerError;
    private ImageView imNoInternet;
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
    private FloatingActionMenu fabbutton;
    private String groupName;


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
        activitiesList = new ArrayList<>();

        

        //doInBackground
        NetworkCall networkCall = new NetworkCall
                (
                        Group_Activities.this,

                        new ResponseListenerVolley() {
                            @Override
                            public void onSuccess(String s)
                            {

                                //parse string to json

                                try {
                                    JSONObject jsonObject= new JSONObject(s);
                                    if (jsonObject.getString("status").equalsIgnoreCase("SUCCESS"))
                                    {
                                        //proceed
                                        JSONArray jsonArray = jsonObject.getJSONArray("data");

                                        if (jsonArray.length()>0)
                                        {

                                            for (int i = 0; i < jsonArray.length(); i++)
                                            {
                                            JSONObject array_jsonobject= (JSONObject) jsonArray.get(i);
                                            Group_ActivitiesModel model = new Group_ActivitiesModel();
                                                model.id=array_jsonobject.getString("id");
                                                model.title = array_jsonobject.getString("title");
                                                model.description = array_jsonobject.getString("description");
                                                model.name = array_jsonobject.getString("name");
                                                model.type = array_jsonobject.getString("type");
                                                model.deadline = array_jsonobject.getString("deadline");
                                                model.hasResponded = array_jsonobject.getBoolean("hasResponded");
                                                model.canAction = array_jsonobject.getBoolean("canAction");
                                                model.reply = array_jsonobject.getString("reply");
                                                if (model.type.equalsIgnoreCase("VOTE")){
                                                    votemeeting(model);
                                                }
                                                else if (model.type.equalsIgnoreCase("MEETING")){
                                                    votemeeting(model);
                                                }
                                                else if (model.type.equalsIgnoreCase("TODO")){
                                                    ToDo(model);
                                                }
                                                activitiesList.add(model);

                                            }

                                        }
                                      /*  Log.e(TAG,"activitiesList size is  " + activitiesList.size());
                                        for (int i = 0; i < activitiesList.size(); i++) {
                                            Log.e(TAG,"position is " + i);
                                            Log.e(TAG,"title is " + activitiesList.get(i).title);
                                        }*/

                                        group_activitiesAdapter.clearApplications();

                                        rcGa.setVisibility(View.VISIBLE);

                                        group_activitiesAdapter.addApplications(activitiesList);
                                        ivGaFilter.setEnabled(true);


                                    }

                                } catch (JSONException e) {
                                    e.printStackTrace();

                                    //failed to parse nly due to no internet
                                    errorLayout.setVisibility(View.VISIBLE);
                                    imNoInternet.setVisibility(View.VISIBLE);
                                }


                            }
                        },

                        new ErrorListenerVolley() {
                            @Override
                            public void onError(VolleyError volleyError) {

                                if ((volleyError instanceof NoConnectionError)|| (volleyError instanceof TimeoutError))
                                {
                                    //failed to parse nly due to no internet
                                    errorLayout.setVisibility(View.VISIBLE);
                                    imNoInternet.setVisibility(View.VISIBLE);
                                }
                                else
                                {
                                    try
                                    {
                                        String responsebody = new String(volleyError.networkResponse.data,"utf-8");
                                        Log.e(TAG, "responseBody " + responsebody);
                                        JSONObject jsonObject = new JSONObject(responsebody);
                                        String status = jsonObject.getString("status");
                                        String message = jsonObject.getString("message");
                                        if (status.equalsIgnoreCase("SUCCESS"))
                                        {
                                            Log.e(TAG, "status is" + status);
                                            Log.e(TAG, "message is" + message);
                                            if (jsonObject.getString("code").equalsIgnoreCase("404"))
                                            {
                                                showSnackBar(getString(R.string.ga_no_activities),"","","","",snackbar.LENGTH_SHORT);
                                            }
                                            else
                                            {
                                                showSnackBar(getString(R.string.Unknown_error),"","","","",snackbar.LENGTH_SHORT);

                                            }

                                        }


                                    } catch (UnsupportedEncodingException e) {
                                        e.printStackTrace();
                                        showSnackBar(getString(R.string.Unknown_error), "", "", "","", snackbar.LENGTH_SHORT);

                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                        showSnackBar(getString(R.string.Unknown_error), "", "", "","", snackbar.LENGTH_SHORT);

                                    }

                                }

                            }
                        },
                        AllLinsks.groupactivities + groupid + "/" + SettingPreffrence.getPREF_Phone_Token(Group_Activities.this),
                        getString(R.string.prg_message),
                        true

                );

        networkCall.makeStringRequest_GET();

        //PostExecute

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
        errorLayout = findViewById(R.id.error_layout);
        imNoResults = (ImageView) errorLayout.findViewById(R.id.im_no_results);
        imServerError = (ImageView) errorLayout.findViewById(R.id.im_server_error);
        imNoInternet = (ImageView) errorLayout.findViewById(R.id.im_no_internet);
        // Handle ProgressBar
        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
        fabbutton = (FloatingActionMenu) findViewById(R.id.fabbutton);

        ivGaFilter.setEnabled(false);
        ivGaFilter.setOnClickListener(ivGaFilter());
        ivGaBack.setOnClickListener(ivGaBack());
        tvGaToolbarTxt.setText(groupName);
        fabbutton.setOnMenuToggleListener(new FloatingActionMenu.OnMenuToggleListener() {
            @Override
            public void onMenuToggle(boolean opened) {
                String text = "";
                if (opened) {
                    fabbutton.toggle(false);
                    Intent open= new Intent(Group_Activities.this,NewActivities.class);
                    startActivity(open);
                    overridePendingTransition(R.anim.push_up_in, R.anim.push_up_out);
                } else {

                }
            }
        });

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
        if (activitiesList.get(position).type.equalsIgnoreCase("VOTE"))
        {

            response=response;
          /*  if (activitiesList.get(position).reply.equalsIgnoreCase("Yes"))
            {
                response="NO_RESPONSE";
            }
            else
            {
                response="Yes";

            }*/
            Url=AllLinsks.Vote + activitiesList.get(position).id + "/" + SettingPreffrence.getPREF_Phone_Token(Group_Activities.this) + "?response=" + response;
            Log.e(TAG,"VoteUrl is " + Url );

        }
        else if (activitiesList.get(position).type.equalsIgnoreCase("MEETING"))
        {


            response=response;

         /*   if (activitiesList.get(position).reply.equalsIgnoreCase("Yes"))
            {
                response="NO_RESPONSE";
            }
            else
            {
                response="Yes";

            }*/
            Url=AllLinsks.Meeting + activitiesList.get(position).id + "/" + SettingPreffrence.getPREF_Phone_Token(Group_Activities.this) + "?response=" + response;
            Log.e(TAG,"MeetingUrl is " + Url );

        }

        //doInBacground
        final String finalResponse = response;
        final String finalResponse1 = response;
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


/*
                                        Group_ActivitiesModel update_model = activitiesList.get(position);

                                        update_model.hasResponded=true;//u responded succesfully
                                        update_model.canAction=false;//u cant do action now
                                        update_model.reply= finalResponse;//ur reply is save
*/

                                        /*
                                        if (update_model.reply.equalsIgnoreCase("Yes"))
                                        {

                                            //if Yes then change No
                                            update_model.hasResponded=true;
                                            update_model.canAction=false;
                                            update_model.reply="No";


                                        }
                                        else
                                        {
                                            //if No then change Yes
                                            update_model.hasResponded=true;
                                            update_model.canAction=false;
                                            update_model.reply="Yes";


                                        }
                                        */

                                     //   group_activitiesAdapter.notifyDataSetChanged();

                                        if (activitiesList.get(position).type.equalsIgnoreCase("VOTE"))
                                        {
                                            showSnackBar(getString(R.string.ga_Votesend), "", "", "", "",Snackbar.LENGTH_SHORT);
                                        }
                                        else
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
                                            showSnackBar(getString(R.string.ga_VoteFailure), "", "", "","", snackbar.LENGTH_SHORT);
                                        } else {
                                            showSnackBar(getString(R.string.Unknown_error), "", "", "","", snackbar.LENGTH_SHORT);

                                        }

                                    }


                                } catch (UnsupportedEncodingException e) {
                                    e.printStackTrace();
                                    showSnackBar(getString(R.string.Unknown_error), "", "", "","", snackbar.LENGTH_SHORT);

                                } catch (JSONException e) {
                                    e.printStackTrace();
                                    showSnackBar(getString(R.string.Unknown_error), "", "", "","", snackbar.LENGTH_SHORT);

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

    public void Completed(ImageView iv2, final int position, final String response)
    {
        iv2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               // Toast.makeText(getBaseContext(), "Completed position is " + position, Toast.LENGTH_LONG).show();
                    CallToDoWS(position,response);
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
                                            showSnackBar(getString(R.string.ga_ToDoFailure), "", "", "","", snackbar.LENGTH_SHORT);
                                        } else {
                                            showSnackBar(getString(R.string.Unknown_error), "", "", "","", snackbar.LENGTH_SHORT);

                                        }

                                    }


                                } catch (UnsupportedEncodingException e) {
                                    e.printStackTrace();
                                    showSnackBar(getString(R.string.Unknown_error), "", "", "", "",snackbar.LENGTH_SHORT);

                                } catch (JSONException e) {
                                    e.printStackTrace();
                                    showSnackBar(getString(R.string.Unknown_error), "", "", "","", snackbar.LENGTH_SHORT);

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
    
}
