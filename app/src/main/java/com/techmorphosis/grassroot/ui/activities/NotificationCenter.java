package com.techmorphosis.grassroot.ui.activities;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
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
import com.techmorphosis.grassroot.adapters.NotificationAdapter;
import com.techmorphosis.grassroot.models.NotificationModel;
import com.techmorphosis.grassroot.utils.ProgressBarCircularIndeterminate;
import com.techmorphosis.grassroot.utils.SettingPreffrence;
import com.techmorphosis.grassroot.utils.listener.ErrorListenerVolley;
import com.techmorphosis.grassroot.utils.listener.ResponseListenerVolley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

public class NotificationCenter extends PortraitActivity implements View.OnClickListener {

    private static final String TAG = "NotificationCenter";
    private Toolbar tlbNc;
    private RecyclerView rcNc;
    private TextView txtTlbNc;
    private LinearLayoutManager mLayoutManager;
    private NotificationAdapter notificationAdapter;
    private int error_flag; // 0- success ,1 - No Internet , 4-Invalid token , 5- Unknown error
    private RelativeLayout rlRootNc;
    private Snackbar snackbar;
    private Integer pageNumber=0;
    private Integer totalPages=0;
    public ArrayList<NotificationModel> notifyList;
    private ProgressBarCircularIndeterminate prgNc;
    private TextView txtPrgNc;
    private ProgressBar prgNcPaging;
    int firstVisibleItem, visibleItemCount, totalItemCount;
    private boolean loading = true;
    private int ItemLeftCount;
    private boolean isLoading=false; //false---now u call WS //true---WS is busy ..so plz wait untill data load
    private int nextPage;
    public  int pagecount=0;

    private View errorLayout;
    private LinearLayout llNoResult;
    private LinearLayout llNoInternet;
    private LinearLayout llServerError;
    private LinearLayout llInvalidToken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification_center);

        findAllViews();
        setUpToolbar();
        mRecylerview();
        init();
        

    }

    private void init() {
        notifyList = new ArrayList<>();
        NotificationWS();
    }


    private void findAllViews()
    {
        tlbNc = (Toolbar) findViewById(R.id.tlb_nc);
        txtTlbNc = (TextView) findViewById(R.id.txt_tlb_nc);
        rcNc = (RecyclerView) findViewById(R.id.rc_nc);
        rlRootNc = (RelativeLayout) findViewById(R.id.rl_root_nc);
        prgNc = (ProgressBarCircularIndeterminate) findViewById(R.id.prg_nc);
        txtPrgNc = (TextView) findViewById(R.id.txt_prg_nc);
        prgNcPaging = (ProgressBar) findViewById(R.id.prg_nc_paging);

        errorLayout = findViewById(R.id.error_layout);

        llNoResult = (LinearLayout) errorLayout.findViewById(R.id.ll_no_result);
        llNoInternet = (LinearLayout) errorLayout.findViewById(R.id.ll_no_internet);
        llServerError = (LinearLayout) errorLayout.findViewById(R.id.ll_server_error);
        llInvalidToken = (LinearLayout) errorLayout.findViewById(R.id.ll_invalid_token);

        //onClick
        llNoResult.setOnClickListener(this);
        llServerError.setOnClickListener(this);
        llNoInternet.setOnClickListener(this);


    }

    private void setUpToolbar() {

        tlbNc.setNavigationIcon(R.drawable.btn_back_wt);
        tlbNc.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
    }

    private void mRecylerview() {

        rcNc.setHasFixedSize(false);
        mLayoutManager = new LinearLayoutManager(getApplicationContext());
        rcNc.setLayoutManager(mLayoutManager);
        rcNc.setItemAnimator(new DefaultItemAnimator());

        rcNc.addOnItemTouchListener(new RecyclerTouchListener(NotificationCenter.this, rcNc, new ClickListener() {
            @Override
            public void onClick(View view, int position) {
                NotificationModel clickmodel = notifyList.get(position);

                Intent openactivity = null;
                switch (clickmodel.getEntityType().toLowerCase()) {
                    case "vote":
                        openactivity = new Intent(NotificationCenter.this, ViewVote.class);
                        openactivity.putExtra("voteid", clickmodel.getEntityUid());
                        break;
                    case "meeting":
                        openactivity = new Intent(NotificationCenter.this, Blank.class);
                        openactivity.putExtra("title", "Meeting");
                        break;
                    case "todo":
                        openactivity = new Intent(NotificationCenter.this, Blank.class);
                        openactivity.putExtra("title", "ToDo");
                        break;
                }
                startActivity(openactivity);

            }

            @Override
            public void onLongClick(View view, int position) {

            }
        }));

        rcNc.addOnScrollListener(new RecyclerView.OnScrollListener() {

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                visibleItemCount = rcNc.getChildCount();
                totalItemCount = mLayoutManager.getItemCount();
                firstVisibleItem = mLayoutManager.findFirstVisibleItemPosition();
                ItemLeftCount = (totalItemCount - firstVisibleItem);

                Log.e(TAG, "firstVisibleItem is " + firstVisibleItem);
                Log.e(TAG, "Remaining is " + (totalItemCount - firstVisibleItem));
                Log.e(TAG, "isLoading is " + isLoading);
                Log.e(TAG, "totalPages" + totalPages);
                Log.e(TAG, "pageNumber" + pageNumber);


                if (pageNumber < totalPages && ItemLeftCount <= 10 && !isLoading) {
                    prgNcPaging.setVisibility(View.VISIBLE);
                    doInBackground();
                    isLoading = true;

                }


            }
        });
    }


    private void NotificationWS() {

        preExecute();

        doInBackground();


    }


    private void preExecute()
    {
        error_flag = 0;

        if (pageNumber == 0) {
            //hide the List
            rcNc.setVisibility(View.GONE);
        }



        if (pageNumber == 0) {
            //show thr progress bar
            prgNc.setVisibility(View.VISIBLE);
            txtPrgNc.setVisibility(View.VISIBLE);

            //gone
            rcNc.setVisibility(View.INVISIBLE);
            errorLayout.setVisibility(View.GONE);
            llNoInternet.setVisibility(View.GONE);
            llServerError.setVisibility(View.GONE);
            llNoResult.setVisibility(View.GONE);
        }


    }

    private void doInBackground()
    {
        NetworkCall networkcall = new NetworkCall
                (
                        NotificationCenter.this,
                        new ResponseListenerVolley() {
                            @Override
                            public void onSuccess(String response) {

                                try {
                                    JSONObject jsonObject = new JSONObject(response);
                                    error_flag =0;
                                    postExecute(response);

                                } catch (JSONException e) {
                                    e.printStackTrace();
                                    Log.e(TAG, "JSONException is " + e.getMessage());
                                    error_flag = 5;
                                    postExecute("");

                                }

                            }
                        },
                        new ErrorListenerVolley() {
                            @Override
                            public void onError(VolleyError volleyError) {
                                if (volleyError instanceof NoConnectionError || volleyError instanceof TimeoutError)
                                {
                                    //No Internet
                                    error_flag=1;
                                    postExecute("");
                                }
                                else if (volleyError instanceof ServerError) {
                                    try {
                                        String response = new String(volleyError.networkResponse.data, "utf-8");
                                        //Success
                                        error_flag = 0;
                                        postExecute(response);

                                    } catch (UnsupportedEncodingException e) {
                                        e.printStackTrace();
                                        error_flag = 5;
                                        postExecute("");
                                    }

                                     }
                                else if (volleyError instanceof AuthFailureError)
                                {
                                    //Invalid Token
                                    error_flag = 4;
                                    postExecute("");
                                }
                                else
                                {
                                    //Unknown error
                                    error_flag = 5;
                                    postExecute("");
                                }

                            }
                        },
                        AllLinsks.notificationList + SettingPreffrence.getPREF_Phone_Token(NotificationCenter.this)+"?page="+pagecount,
                        "",
                        false
                );

        networkcall.makeStringRequest_GET();

        Log.e(TAG,"call wS " + AllLinsks.notificationList + SettingPreffrence.getPREF_Phone_Token(NotificationCenter.this)+"?page="+pagecount);
    }

    private void postExecute(String response) {


        Log.e(TAG, "error flag is " + error_flag);
        Log.e(TAG,"pE isLoading " + isLoading);

        if (error_flag == 1) {//No Internet

            //hide the progress bar
            prgNc.setVisibility(View.GONE);
            txtPrgNc.setVisibility(View.GONE);

            //hide the paging prg
            prgNcPaging.setVisibility(View.GONE);

            isLoading = false;
            Log.e(TAG,"noInternet isLoading " + isLoading);

            if (pagecount == 0) {

                errorLayout.setVisibility(View.VISIBLE);
                llNoInternet.setVisibility(View.VISIBLE);
               // showSnackbar(getString(R.string.No_network), snackbar.LENGTH_INDEFINITE, getString(R.string.Retry));
            }
        }
        else if (error_flag == 4) {//Invalid Token

            //hide the progress bar
            prgNc.setVisibility(View.GONE);
            txtPrgNc.setVisibility(View.GONE);

            //hide the paging prg
            prgNcPaging.setVisibility(View.GONE);

            if (pagecount==0) {

                errorLayout.setVisibility(View.VISIBLE);
                llInvalidToken.setVisibility(View.VISIBLE);

               // showSnackbar(getString(R.string.INVALID_TOKEN), snackbar.LENGTH_INDEFINITE, "");
            }

        }
        else if (error_flag == 5) {//Unkown error

            //hide the progress bar
            prgNc.setVisibility(View.GONE);
            txtPrgNc.setVisibility(View.GONE);

            //hide the paging prg
            prgNcPaging.setVisibility(View.GONE);

            isLoading = false;
            if (pagecount==0) {
                errorLayout.setVisibility(View.VISIBLE);
                llServerError.setVisibility(View.VISIBLE);

               // showSnackbar(getString(R.string.Unknown_error), snackbar.LENGTH_SHORT, "");
            }
        }
        else if (error_flag==0) {//Success




            JSONObject jsonObject = null;
            try {
                jsonObject = new JSONObject(response);

                JSONObject dataObject = jsonObject.getJSONObject("data");

                pageNumber = dataObject.getInt("pageNumber");
                totalPages = dataObject.getInt("totalPages");
                //nextPage = dataObject.getInt("nextPage");


                JSONArray notificationsarray = dataObject.getJSONArray("notifications");

                if (notificationsarray.length() > 0) {


                    for (int i = 0; i < notificationsarray.length(); i++) {
                        JSONObject singlearry_object = notificationsarray.getJSONObject(i);
                        NotificationModel notificationModel = new NotificationModel();
                        notificationModel.setUid(singlearry_object.getString("uid"));
                        notificationModel.setEntityUid(singlearry_object.getString("entityUid"));
                        notificationModel.setMessage(singlearry_object.getString("message"));
                        notificationModel.setCreatedDatetime(singlearry_object.getString("createdDatetime"));
                        notificationModel.setNotificationType(singlearry_object.getString("notificationType"));
                        notificationModel.setEntityType(singlearry_object.getString("entityType"));
                        notifyList.add(notificationModel);


                    }


                    //now set the data
                    setAdapter(notifyList);

                } else {
                    //no data to set


                    if (pagecount==0) {
                        //hide the progress bar
                        prgNc.setVisibility(View.GONE);
                        txtPrgNc.setVisibility(View.GONE);

                        errorLayout.setVisibility(View.VISIBLE);
                        llNoResult.setVisibility(View.VISIBLE);


                        //showSnackbar(getString(R.string.No_notifications),snackbar.LENGTH_INDEFINITE,"");
                    }
                    else
                    {
                        //hide the paging prg
                        prgNcPaging.setVisibility(View.GONE);
                    }
                }

            } catch (JSONException e) {
                e.printStackTrace();
                Log.e(TAG, "JSONException is " + e.getMessage());
             //   showSnackbar(getString(R.string.Unknown_error), snackbar.LENGTH_SHORT, "");

                prgNc.setVisibility(View.GONE);
                txtPrgNc.setVisibility(View.GONE);

                errorLayout.setVisibility(View.VISIBLE);
                llServerError.setVisibility(View.VISIBLE);
            }


        }

    }

    private void setAdapter(ArrayList<NotificationModel> notifyList)
    {

     //   Log.e(TAG, "setAdapter is " + notifyList.size());
        Log.e(TAG,"*** pageNumber is " + pageNumber);

        if (pageNumber == 1) {//initially set Adapter

            notificationAdapter = new NotificationAdapter(notifyList);
            rcNc.setAdapter(notificationAdapter);

            //hide the progress bar
            prgNc.setVisibility(View.GONE);
            txtPrgNc.setVisibility(View.GONE);

            rcNc.setVisibility(View.VISIBLE);


        }
        else//just update the Adapter
        {
            notificationAdapter.notifyDataSetChanged();
            Log.e(TAG, "** notify adapter size " + notifyList.size());
            Log.e(TAG,"** notify adapter");

            //hide the paging prg
            prgNcPaging.setVisibility(View.GONE);
        }

        isLoading = false;
        Log.e(TAG,"*** Adapter isLoading " + isLoading);

        pagecount++;



    }

    public  void showSnackbar(String message,int length,String actionbuttontxt)
    {
        snackbar = Snackbar.make(rlRootNc, message, length);
        snackbar.setActionTextColor(Color.RED);

        if (!TextUtils.isEmpty(actionbuttontxt))
        {
            snackbar.setAction(actionbuttontxt, new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    NotificationWS();
                }
            });

        }

        snackbar.show();
    }

    @Override
    public void onClick(View v)
    {

        if (v==llNoInternet || v==llNoResult || v==llNoResult )
            NotificationWS();

    }
}
