package com.techmorphosis.grassroot.ui.activities;

import android.graphics.Color;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
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
import com.techmorphosis.grassroot.Interface.ClickListener;
import com.techmorphosis.grassroot.Network.AllLinsks;
import com.techmorphosis.grassroot.Network.NetworkCall;
import com.techmorphosis.grassroot.R;
import com.techmorphosis.grassroot.RecyclerView.RecyclerTouchListener;
import com.techmorphosis.grassroot.adapters.VoteNotifyMembersAdapter;
import com.techmorphosis.grassroot.models.ContactsModel;
import com.techmorphosis.grassroot.utils.ProgressBarCircularIndeterminate;
import com.techmorphosis.grassroot.utils.SettingPreffrence;
import com.techmorphosis.grassroot.utils.listener.ErrorListenerVolley;
import com.techmorphosis.grassroot.utils.listener.ResponseListenerVolley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

public class VoteNotifyMembers extends AppCompatActivity {

    private static final String TAG = "VoteNotifyMembers";
    
    private Toolbar tlbNm;
    private TextView txtNmTlb;
    private LinearLayout llNmMainLayout;
    private CardView cardView;
    private RelativeLayout rlNmNotifyHeader;
    private SwitchCompat swNotifyall;
    private RecyclerView recyclerView;
    private ProgressBar progressPaging;
    private View iclNmErrorLayout;
    private Button btnnmdone;
    private ImageView imNoResults;
    private ImageView imServerError;
    private ImageView imNoInternet;
    private ProgressBarCircularIndeterminate progressBarCircularIndeterminate;
    private TextView txtPrg;
    private int error_flag;//0-success 1- no Internet 5- Unknown error 4- Invalid Token
    private RelativeLayout rlRootLayout;
    private Snackbar snackbar;

    public ArrayList<ContactsModel> memberlist;
    public VoteNotifyMembersAdapter voteNotifyMembersAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vote_notify_members);

        findAllViews();
        setUpToolbar();
        init();
        
    }

    private void init()
    {
        setRecyclerView();
        MembersWS();
    }

    private void setRecyclerView() {
        recyclerView.setHasFixedSize(false);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getApplicationContext());
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.addOnItemTouchListener(new RecyclerTouchListener(getApplicationContext(), recyclerView, new ClickListener() {
            @Override
            public void onClick(View view, int position) {
                ContactsModel click_model = memberlist.get(position);

                if (click_model.isSelected) {
                    click_model.isSelected = false;
                }
                else
                    click_model.isSelected = true;


                voteNotifyMembersAdapter.notifyDataSetChanged();

            }

            @Override
            public void onLongClick(View view, int position) {

            }
        }));

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
        imNoResults.setVisibility(View.GONE);
        imNoInternet.setVisibility(View.GONE);
        imServerError.setVisibility(View.GONE);

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
                        AllLinsks.Votemembers + SettingPreffrence.getGroupId(VoteNotifyMembers.this) + "/" + SettingPreffrence.getPREF_Phone_Token(VoteNotifyMembers.this)+"?"+"page=1",
                        "",
                        false


                );
        networkcall.makeStringRequest_GET();
    }

    private void postExecute(String response)
    {
        Log.e(TAG,"error_flag is  " + error_flag);
        progressBarCircularIndeterminate.setVisibility(View.GONE);
        txtPrg.setVisibility(View.GONE);

        if (error_flag == 1) {//no Internet

            iclNmErrorLayout.setVisibility(View.VISIBLE);
            imNoInternet.setVisibility(View.VISIBLE);

        } else if ( error_flag == 5) {//catch error


            iclNmErrorLayout.setVisibility(View.VISIBLE);
            imServerError.setVisibility(View.VISIBLE);

        }else if (error_flag == 4) //invalid token
        {
            showSnackBar(getString(R.string.INVALID_TOKEN),snackbar.LENGTH_INDEFINITE,"");
        }
        else if (error_flag == 0) {
            llNmMainLayout.setVisibility(View.VISIBLE);

            try {
                JSONObject jsonObject = new JSONObject(response);

                if (jsonObject.getString("status").equalsIgnoreCase("SUCCESS")) {
                    Log.e(TAG, "if");
                    JSONObject dataObject = jsonObject.getJSONObject("data");
                    JSONArray membersarray = dataObject.getJSONArray("members");
                    if (membersarray.length() > 0)
                    {
                        for (int i = 0; i < membersarray.length(); i++) {
                            JSONObject memberObject = membersarray.getJSONObject(i);
                            ContactsModel contactsModel = new ContactsModel();
                            contactsModel.contact_ID = memberObject.getString("id");
                            contactsModel.name = memberObject.getString("displayName");
                            contactsModel.isSelected = false;
                            memberlist.add(contactsModel);
                        }

                        setView();

                    } else {

                        iclNmErrorLayout.setVisibility(View.VISIBLE);
                        imNoResults.setVisibility(View.VISIBLE);
                    }


                } else
                    Log.e(TAG, "else");


            } catch (JSONException e) {
                e.printStackTrace();
                llNmMainLayout.setVisibility(View.GONE);
                iclNmErrorLayout.setVisibility(View.VISIBLE);
                imServerError.setVisibility(View.VISIBLE);
            }
        }

    }

    private void setView() {

        voteNotifyMembersAdapter = new VoteNotifyMembersAdapter(VoteNotifyMembers.this,memberlist);
        recyclerView.setAdapter(voteNotifyMembersAdapter);
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
        progressPaging = (ProgressBar) findViewById(R.id.progress_paging);
        btnnmdone =(Button)findViewById(R.id.btn_nm_done);

        progressBarCircularIndeterminate = (ProgressBarCircularIndeterminate) findViewById(R.id.progressBarCircularIndeterminate);
        txtPrg = (TextView) findViewById(R.id.txt_prg);


        iclNmErrorLayout = findViewById(R.id.icl_nm_error_layout);
        imNoResults = (ImageView) iclNmErrorLayout.findViewById(R.id.im_no_results);
        imServerError = (ImageView) iclNmErrorLayout.findViewById(R.id.im_server_error);
        imNoInternet = (ImageView) iclNmErrorLayout.findViewById(R.id.im_no_internet);

        btnnmdone.setOnClickListener(button_done());
    }

    private View.OnClickListener button_done() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {

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



}
