package com.techmorphosis.grassroot.ui.activities;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.CardView;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.android.volley.AuthFailureError;
import com.android.volley.NoConnectionError;
import com.android.volley.ServerError;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.techmorphosis.grassroot.Interface.ClickListener;
import com.techmorphosis.grassroot.Network.AllLinsks;
import com.techmorphosis.grassroot.Network.NetworkCall;
import com.techmorphosis.grassroot.R;
import com.techmorphosis.grassroot.RecyclerView.RecyclerTouchListener;
import com.techmorphosis.grassroot.adapters.VoteNotifyMembersAdapter;
import com.techmorphosis.grassroot.models.VoteMemberModel;
import com.techmorphosis.grassroot.utils.Constant;
import com.techmorphosis.grassroot.utils.ProgressBarCircularIndeterminate;
import com.techmorphosis.grassroot.utils.SettingPreffrence;
import com.techmorphosis.grassroot.utils.listener.ErrorListenerVolley;
import com.techmorphosis.grassroot.utils.listener.ResponseListenerVolley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

public class VoteNotifyMembers extends PortraitActivity implements  View.OnClickListener{

    private static final String TAG = "VoteNotifyMembers";
    
    private Toolbar tlbNm;
    private TextView txtNmTlb;
    private LinearLayout llNmMainLayout;
    private CardView cardView;
    private RelativeLayout rlNmNotifyHeader;
    private SwitchCompat swNotifyall;
    private RecyclerView recyclerView;
//    private ProgressBar progressPaging;
    private Button btnnmdone;

    private View iclNmErrorLayout;
    private LinearLayout llNoResult;
    private LinearLayout llNoInternet;
    private LinearLayout llServerError;
    private LinearLayout llInvalidToken;


    private ProgressBarCircularIndeterminate progressBarCircularIndeterminate;
    private TextView txtPrg;
    private int error_flag;//0-success 1- no Internet 5- Unknown error 4- Invalid Token
    private RelativeLayout rlRootLayout;
    private Snackbar snackbar;

    public ArrayList<VoteMemberModel> memberlist;
    public VoteNotifyMembersAdapter voteNotifyMembersAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vote_notify_members);

        findAllViews();
        setUpToolbar();
        memberlist = new ArrayList<>();
        Bundle b= getIntent().getExtras();
        memberlist =b.getParcelableArrayList(Constant.VotedmemberList);
        init();
        if (memberlist.size() == 0) {
            MembersWS();
        } else {
            setView();
        }
    }

    private void init() {
        switchOff();
        setRecyclerView();

    }

    private void setRecyclerView() {
        recyclerView.setHasFixedSize(false);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getApplicationContext());
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.addOnItemTouchListener(new RecyclerTouchListener(getApplicationContext(), recyclerView, new ClickListener() {
            @Override
            public void onClick(View view, int position) {
                Log.e(TAG,"onClick ");
                Log.e(TAG,"position is  " + position);
                VoteMemberModel click_model = memberlist.get(position);

                if (click_model.isSelected) {
                    Log.e(TAG,"if");
                    click_model.isSelected = false;
                } else if (!click_model.isSelected) {
                    Log.e(TAG,"else ");
                    click_model.isSelected = true;
                }

                voteNotifyMembersAdapter.notifyDataSetChanged();


                if (giveMembercount()==memberlist.size()) {
                    switchOn();
                } else {
                    switchOff();
                }


            }

            @Override
            public void onLongClick(View view, int position) {

            }
        }));

    }

    private int giveMembercount() {

        int membercounter = 0;
        for (int i = 0; i < memberlist.size(); i++) {
            VoteMemberModel membercount = memberlist.get(i);
            if (membercount.isSelected) {
                membercounter++;
            }
        }

        return membercounter;
    }

    private void switchOff() {
            swNotifyall.setChecked(false);
    }

    private void switchOn() {
        swNotifyall.setChecked(true);
    }

    private void MembersWS() {

        preExecute();
        doInBackground();
    }


    private void preExecute() {

        error_flag = 0;

        //hide the MainLayout
        llNmMainLayout.setVisibility(View.GONE);


        //hide the error Layout
        iclNmErrorLayout.setVisibility(View.GONE);
        llNoResult.setVisibility(View.GONE);
        llNoInternet.setVisibility(View.GONE);
        llServerError.setVisibility(View.GONE);

        //show thr progress bar
        progressBarCircularIndeterminate.setVisibility(View.VISIBLE);
        txtPrg.setVisibility(View.VISIBLE);

    }

    private void doInBackground()
    {

        NetworkCall networkcall = new NetworkCall
                (
                        VoteNotifyMembers.this,
                        new ResponseListenerVolley() {
                            @Override
                            public void onSuccess(String s)
                            {

                                error_flag =0;
                                postExecute(s);
                            }
                        },
                        new ErrorListenerVolley() {
                            @Override
                            public void onError(VolleyError volleyError) {

                                if (volleyError instanceof NoConnectionError || volleyError instanceof TimeoutError)
                                {
                                    error_flag =1;
                                    postExecute("");

                                }
                                else if (volleyError instanceof ServerError)
                                {
                                    try {
                                        String responsebody = new String(volleyError.networkResponse.data,"utf-8");
                                        Log.e(TAG, "responsebody is " + responsebody);
                                        try {
                                            JSONObject jsonObject = new JSONObject(responsebody);
                                            error_flag =0;
                                            postExecute(responsebody);

                                        } catch (JSONException e) {
                                            e.printStackTrace();
                                            error_flag =5;
                                            postExecute("");

                                        }

                                    } catch (UnsupportedEncodingException e) {
                                        e.printStackTrace();

                                        error_flag =5;
                                        postExecute("");


                                    }

                                }
                                else if (volleyError instanceof AuthFailureError)
                                {
                                    error_flag = 4 ;
                                    postExecute("");
                                }
                                else { //Unknown volley error
                                    error_flag = 5;
                                    postExecute("");
                                }
                            }
                        },
                        AllLinsks.Votemembers +SettingPreffrence.getPREF_Phone_Token(VoteNotifyMembers.this)+"/"+SettingPreffrence.getGroupId(VoteNotifyMembers.this)+"/"+"false",
                        "",
                        false


                );
        networkcall.makeStringRequest_GET();
    }

    private void postExecute(String response)
    {
        Log.e(TAG, "error_flag is  " + error_flag);


        if (error_flag == 1) {//no Internet
            progressBarCircularIndeterminate.setVisibility(View.GONE);
            txtPrg.setVisibility(View.GONE);

            iclNmErrorLayout.setVisibility(View.VISIBLE);
            llNoInternet.setVisibility(View.VISIBLE);

        } else if ( error_flag == 5) {//catch error

            progressBarCircularIndeterminate.setVisibility(View.GONE);
            txtPrg.setVisibility(View.GONE);

            iclNmErrorLayout.setVisibility(View.VISIBLE);
            llServerError.setVisibility(View.VISIBLE);

        }else if (error_flag == 4) //invalid token
        {
            progressBarCircularIndeterminate.setVisibility(View.GONE);
            txtPrg.setVisibility(View.GONE);

            iclNmErrorLayout.setVisibility(View.VISIBLE);
            llInvalidToken.setVisibility(View.VISIBLE);

        }
        else if (error_flag == 0) {



            try {
                JSONObject jsonObject = new JSONObject(response);

                if (jsonObject.getString("status").equalsIgnoreCase("SUCCESS")) {
                    Log.e(TAG, "if");
                    //JSONObject dataObject = jsonObject.getJSONObject("data");
                    JSONArray data_arry = jsonObject.getJSONArray("data");
                    if (data_arry.length() > 0)
                    {
                        for (int i = 0; i < data_arry.length(); i++) {

                            JSONObject memberObject = data_arry.getJSONObject(i);
                            VoteMemberModel votemembermodel = new VoteMemberModel();
                            votemembermodel.memberUid = memberObject.getString("memberUid");
                            votemembermodel.displayName = memberObject.getString("displayName");
                            votemembermodel.groupUid = memberObject.getString("groupUid");
                            votemembermodel.phoneNumber = memberObject.getString("phoneNumber");
                            votemembermodel.roleName = memberObject.getString("roleName");
                            votemembermodel.isSelected = false;
                            memberlist.add(votemembermodel);
                        }

                        setView();

                    } else {

                        iclNmErrorLayout.setVisibility(View.VISIBLE);
                        llNoResult.setVisibility(View.VISIBLE);
                    }


                } else if (jsonObject.getString("status").equalsIgnoreCase("FAILURE")) {
                    Log.e(TAG, "Failure");
                    iclNmErrorLayout.setVisibility(View.VISIBLE);
                    llServerError.setVisibility(View.VISIBLE);
                } else {//Unknown status
                    Log.e(TAG,"status not match");
                    iclNmErrorLayout.setVisibility(View.VISIBLE);
                    llServerError.setVisibility(View.VISIBLE);
                }



            } catch (JSONException e) {
                e.printStackTrace();
                llNmMainLayout.setVisibility(View.GONE);
                iclNmErrorLayout.setVisibility(View.VISIBLE);
                llServerError.setVisibility(View.VISIBLE);
            }
        }

    }

    private void setView() {

        voteNotifyMembersAdapter = new VoteNotifyMembersAdapter(VoteNotifyMembers.this,memberlist);
        recyclerView.setAdapter(voteNotifyMembersAdapter);
        progressBarCircularIndeterminate.setVisibility(View.GONE);
        txtPrg.setVisibility(View.GONE);

        llNmMainLayout.setVisibility(View.VISIBLE);
        switchlistner();

    }


    private void setUpToolbar() {

        tlbNm.setNavigationIcon(R.drawable.btn_back_wt);
        tlbNm.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
    }

    private void findAllViews() {

        rlRootLayout = (RelativeLayout) findViewById(R.id.rl_root_layout);
        tlbNm = (Toolbar) findViewById(R.id.tlb_nm);
        txtNmTlb = (TextView) findViewById(R.id.txt_nm_tlb);
        llNmMainLayout = (LinearLayout) findViewById(R.id.ll_nm_main_layout);
        cardView = (CardView) findViewById(R.id.card_view);
        rlNmNotifyHeader = (RelativeLayout) findViewById(R.id.rl_nm_notify_header);
        swNotifyall = (SwitchCompat) findViewById(R.id.sw_notifyall);
        recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
       // progressPaging = (ProgressBar) findViewById(R.id.progress_paging);
        btnnmdone =(Button)findViewById(R.id.btn_nm_done);

        progressBarCircularIndeterminate = (ProgressBarCircularIndeterminate) findViewById(R.id.progressBarCircularIndeterminate);
        txtPrg = (TextView) findViewById(R.id.txt_prg);


        iclNmErrorLayout = findViewById(R.id.icl_nm_error_layout);
        llNoResult = (LinearLayout) iclNmErrorLayout.findViewById(R.id.ll_no_result);
        llNoInternet = (LinearLayout) iclNmErrorLayout.findViewById(R.id.ll_no_internet);
        llServerError = (LinearLayout) iclNmErrorLayout.findViewById(R.id.ll_server_error);
        llInvalidToken = (LinearLayout) iclNmErrorLayout.findViewById(R.id.ll_invalid_token);
        btnnmdone.setOnClickListener(button_done());



        //onClick
        llNoResult.setOnClickListener(this);
        llServerError.setOnClickListener(this);
        llNoInternet.setOnClickListener(this);
        llInvalidToken.setOnClickListener(this);

    }

    private void switchlistner() {

        Log.e(TAG,"switchlistner");
        swNotifyall.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                if (isChecked) {
                    Log.e(TAG, "isChecked " + true);
                    if (giveMembercount() == memberlist.size()) {
                        Log.e(TAG,"if switchlistner selectAllmember");

                    } else {
                        Log.e(TAG,"else switchlistner selectAllmember");
                        selectAllmember();
                    }

                } else {
                    Log.e(TAG, "isChecked " + false);
                     if (giveMembercount()==memberlist.size()){
                        removeAllmember();
                        Log.e(TAG, "if  switchlistner removeAllmember");
                    }else if (giveMembercount() > 0) {
                         Log.e(TAG,"else switchlistner removeAllmember");
                     }


                }
            }
        });
    }

    private void removeAllmember() {
        Log.e(TAG,"removeAllmember");
        for (int i = 0; i < memberlist.size() ; i++) {
            VoteMemberModel selectallmodel = memberlist.get(i);
            selectallmodel.isSelected = false;
        }

        voteNotifyMembersAdapter.notifyDataSetChanged();
    }

    private void selectAllmember() {
        Log.e(TAG,"selectAllmember");
        for (int i = 0; i < memberlist.size() ; i++) {
            VoteMemberModel selectallmodel = memberlist.get(i);
            selectallmodel.isSelected = true;
        }

        voteNotifyMembersAdapter.notifyDataSetChanged();
    }

    private View.OnClickListener button_done() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (giveMembercount() > 0) {

                    Intent i = new Intent();
                    i.putParcelableArrayListExtra(Constant.VotedmemberList, memberlist);
                    setResult(1, i);
                    finish();
                } else {
                    showSnackBar(getString(R.string.nm_vote_members_msg),Snackbar.LENGTH_SHORT,"");
                }

            }
        };
    }

    private void showSnackBar(String message,int length, final String actionButtontext)
    {
        snackbar = Snackbar.make(rlRootLayout, message, length);
        snackbar.setActionTextColor(Color.RED);

        if (!actionButtontext.isEmpty() )
        {
            snackbar.setAction(actionButtontext, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                        MembersWS();
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
                MembersWS();
                break;
            case  R.id.ll_server_error :
                // llServerError.setAlpha((float) 0.2);
                MembersWS();

                break;
            case  R.id.ll_no_internet :
                // llNoInternet.setAlpha((float) 0.2);
                MembersWS();

                break;
         /*   case  R.id.ll_invalid_token :
                //llInvalidToken.setAlpha((float) 0.2);
                Group_Activities_WS();

                break;*/
        }


    }

}
