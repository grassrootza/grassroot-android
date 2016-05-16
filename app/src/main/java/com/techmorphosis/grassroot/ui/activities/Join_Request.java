package com.techmorphosis.grassroot.ui.activities;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.android.volley.AuthFailureError;
import com.android.volley.NoConnectionError;
import com.android.volley.ServerError;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.techmorphosis.grassroot.Animator.CustomItemAnimator;
import com.techmorphosis.grassroot.Interface.ClickListener;
import com.techmorphosis.grassroot.Network.AllLinsks;
import com.techmorphosis.grassroot.Network.NetworkCall;
import com.techmorphosis.grassroot.R;
import com.techmorphosis.grassroot.RecyclerView.RecyclerTouchListener;
import com.techmorphosis.grassroot.adapters.JoinRequestAdapter;
import com.techmorphosis.grassroot.models.Join_RequestModel;
import com.techmorphosis.grassroot.ui.DialogFragment.AlertDialogFragment;
import com.techmorphosis.grassroot.utils.L;
import com.techmorphosis.grassroot.utils.ProgressBarCircularIndeterminate;
import com.techmorphosis.grassroot.utils.SettingPreffrence;
import com.techmorphosis.grassroot.utils.UtilClass;
import com.techmorphosis.grassroot.utils.listener.AlertDialogListener;
import com.techmorphosis.grassroot.utils.listener.ErrorListenerVolley;
import com.techmorphosis.grassroot.utils.listener.ResponseListenerVolley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;

public class Join_Request extends PortraitActivity implements OnClickListener{

    private Toolbar toolbar;
    private TextView txtToolbar;
    private EditText et_searchbox;
    private UtilClass utilclass;
    private RecyclerView jrRecyclerView;
    private LinearLayoutManager mLayoutManager;
    private JoinRequestAdapter joinrequestAdapter;
            ArrayList<Join_RequestModel>  joinrequestList;
    private boolean btn_close;
    private String prgMessage;
    private boolean prgboolean;
    private RelativeLayout rlRoot;
    private String TAG=Join_Request.class.getSimpleName();
    private Snackbar snackbar;

    private AlertDialogFragment alerdialog;
    String uid;

    private int error_flag1;//0-success 1- no Internet 2- Invalid Token 3- Unknown error
    private int error_flag2;//0-success 1- no Internet 2- Invalid Token 3- Unknown error
    private View errorLayout;
    private LinearLayout llNoResult;
    private LinearLayout llNoInternet;
    private LinearLayout llServerError;
    private LinearLayout llInvalidToken;
    private ProgressBarCircularIndeterminate prgJa;
    private TextView txtPrgJa;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_join__request);

        findViews();
        setUpToolbar();
        init();
        jrRecyclerView();
    }

    private void jrRecyclerView()
    {
        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        jrRecyclerView.setHasFixedSize(true);

        // use a linear layout manager
        mLayoutManager = new LinearLayoutManager(this);
        jrRecyclerView.setLayoutManager(mLayoutManager);
        jrRecyclerView.setItemAnimator(new CustomItemAnimator());

        // specify an adapter
        joinrequestAdapter = new JoinRequestAdapter(getApplicationContext(),new ArrayList<Join_RequestModel>());
        jrRecyclerView.setAdapter(joinrequestAdapter);


        // jrRecyclerView.setItemAnimator(new DefaultItemAnimator());
        jrRecyclerView.addOnItemTouchListener(new RecyclerTouchListener(getApplicationContext(), jrRecyclerView, new ClickListener() {
            @Override
            public void onClick(View view, int position) {
                Join_RequestModel model = joinrequestList.get(position);
//               / Toast.makeText(getApplicationContext(),""+model.getGroupname(),Toast.LENGTH_LONG).show();

                alerdialog = utilclass.showAlerDialog(getFragmentManager(), getString(R.string.alertbox), "NO", "YES", false, new AlertDialogListener() {
                    @Override
                    public void setRightButton() {
                        Join_RequestWS();
                        alerdialog.dismiss();

                    }

                    @Override
                    public void setLeftButton() {
                        alerdialog.dismiss();

                    }
                });
            }

            @Override
            public void onLongClick(View view, int position) {

            }

        }));


    }



    private void init()
    {
        utilclass= new UtilClass();
        joinrequestList=new ArrayList<>();
    }

    private void findViews()
    {
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        txtToolbar = (TextView) findViewById(R.id.txt_toolbar);
        rlRoot = (RelativeLayout) findViewById(R.id.rl_root);

        prgJa = (ProgressBarCircularIndeterminate) findViewById(R.id.prg_ja);
        txtPrgJa = (TextView) findViewById(R.id.txt_prg_ja);

        errorLayout =  findViewById(R.id.error_layout);
        llNoResult = (LinearLayout) errorLayout.findViewById(R.id.ll_no_result);
        llNoInternet = (LinearLayout) errorLayout.findViewById(R.id.ll_no_internet);
        llServerError = (LinearLayout) errorLayout.findViewById(R.id.ll_server_error);
        llInvalidToken = (LinearLayout) errorLayout.findViewById(R.id.ll_invalid_token);


        jrRecyclerView = (RecyclerView) findViewById(R.id.jr_RecyclerView);
        et_searchbox=(EditText) findViewById(R.id.et_searchbox);

        setAllListner();

    }

    private void setAllListner() {
        //onClick
        llNoResult.setOnClickListener(this);
        llServerError.setOnClickListener(this);
        llNoInternet.setOnClickListener(this);
        llInvalidToken.setOnClickListener(this);

        et_searchbox.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() > 0) {
                    et_searchbox.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.btn_close, 0);
                    btn_close = true;
                } else {
                    et_searchbox.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.btn_search_gray, 0);
                    btn_close = false;
                }

            }
        });



        et_searchbox.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                final int DRAWABLE_LEFT = 0;
                final int DRAWABLE_TOP = 1;
                final int DRAWABLE_RIGHT = 2;
                final int DRAWABLE_BOTTOM = 3;

                if (event.getAction() == MotionEvent.ACTION_UP) {
                    if (event.getRawX() >= (et_searchbox.getRight() - et_searchbox.getTotalPaddingRight())) {
                        // your action here
                        if (btn_close) {
                            //   Group_SearchWS();
                            et_searchbox.setText("");
                        }

                        return true;
                    }
                }
                return false;
            }
        });

        et_searchbox.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    try {
                        InputMethodManager imm = (InputMethodManager) getApplicationContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    if (et_searchbox.getText().toString().trim().isEmpty()) {
                        showSnackBar(getString(R.string.validate_search_box), "", Snackbar.LENGTH_SHORT);

                    } else {
                        Group_SearchWS();
                    }


                }

                return false;
            }
        });


    }

    private void Group_SearchWS()
    {

        preExecute1();


        doInBackground1();


    }

    private void preExecute1() {


        Log.e(TAG, "Group_SearchWS");

        joinrequestList.clear();

        //visible
        prgJa.setVisibility(View.VISIBLE);
        txtPrgJa.setVisibility(View.VISIBLE);


        jrRecyclerView.setVisibility(View.INVISIBLE);
        errorLayout.setVisibility(View.INVISIBLE);
        llNoInternet.setVisibility(View.INVISIBLE);
        llNoResult.setVisibility(View.INVISIBLE);
        llServerError.setVisibility(View.INVISIBLE);
    }

    private void doInBackground1() {

        try {
            Log.e(TAG, "link is " + AllLinsks.groupsearch + URLEncoder.encode(et_searchbox.getText().toString(), "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }


        try {
            NetworkCall networkCall = new NetworkCall(
                    Join_Request.this,

                    new ResponseListenerVolley() {
                        @Override
                        public void onSuccess(String response)
                        {
                            Log.e(TAG, " onSuccess " + response);

                            error_flag1 = 0;
                            postExecute1(response);

                        }
                    }
                    ,
                    new ErrorListenerVolley() {
                        @Override
                        public void onError(VolleyError volleyError) {

                            if ((volleyError instanceof NoConnectionError)|| (volleyError instanceof TimeoutError)) {
                                error_flag1 = 1;
                                postExecute1("");
                            }
                            else if (volleyError instanceof ServerError)
                            {
                                String response = null, status, message;
                                try {
                                    response = new String(volleyError.networkResponse.data, "utf-8");
                                    L.e(TAG, "response is  ", response);
                                    JSONObject jsonObject_error = new JSONObject(response);

                                    error_flag1 = 0;
                                    postExecute1(response);

                                } catch (UnsupportedEncodingException e) {// Data is not able to UnsupportedEncodingException
                                    e.printStackTrace();

                                    error_flag1 = 5;
                                    postExecute1("");

                                } catch (JSONException e) {// Data is not able to parse
                                    e.printStackTrace();

                                    error_flag1 = 5;
                                    postExecute1("");
                                }


                            }
                            else if (volleyError instanceof AuthFailureError) {
                                error_flag1 = 2;
                                postExecute1("");
                            }
                            else {//Unknown error
                                error_flag1 = 5;
                                postExecute1("");
                            }


                        }
                    }
                    ,AllLinsks.groupsearch + URLEncoder.encode(et_searchbox.getText().toString(), "UTF-8")
                    ,""
                    ,false
            );

            networkCall.makeStringRequest_GET();

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();


        }
    }

    private void postExecute1(String response) {

        Log.e(TAG, "error_flag1 is " + error_flag1);

        if (error_flag1 == 1) {//no Internet
            prgJa.setVisibility(View.GONE);
            txtPrgJa.setVisibility(View.GONE);

            errorLayout.setVisibility(View.VISIBLE);
            llNoInternet.setVisibility(View.VISIBLE);
        }
        else if (error_flag1 ==2) {// Invalid Token
            prgJa.setVisibility(View.GONE);
            txtPrgJa.setVisibility(View.GONE);

            errorLayout.setVisibility(View.VISIBLE);
            llInvalidToken.setVisibility(View.VISIBLE);
        }
        else if (error_flag1 ==3) {//Unknown error
            prgJa.setVisibility(View.GONE);
            txtPrgJa.setVisibility(View.GONE);

            errorLayout.setVisibility(View.VISIBLE);
            llServerError.setVisibility(View.VISIBLE);
        }
        else if (error_flag1 ==0) {


            String status,message,code,groupName,description,groupCreator,count;
            try {
                JSONObject jsonobject= new JSONObject(response);
                status= jsonobject.getString("status");


                if (status.equalsIgnoreCase("SUCCESS"))
                {

                    code = jsonobject.getString("code");
                    message = jsonobject.getString("message");
                    JSONArray data_array = jsonobject.getJSONArray("data");

                    if (data_array.length() > 0) {
                        for (int i = 0; i < data_array.length(); i++) {
                            JSONObject json = data_array.getJSONObject(i);
                            uid = json.getString("id");
                            groupName = json.getString("groupName");
                            description = json.getString("description");
                            groupCreator = json.getString("groupCreator");
                            count = json.getString("count");


                            Join_RequestModel model = new Join_RequestModel();
                            model.setId(uid);
                            model.setGroupname(groupName);
                            model.setGroup_describe(description);
                            model.setGroupCreator(groupCreator);
                            model.setCount(count);
                            joinrequestList.add(model);


                        }


                        joinrequestAdapter.clearApplications();


                        joinrequestAdapter.addApplications(joinrequestList);

                        //step1-hide the loader
                        prgJa.setVisibility(View.GONE);
                        txtPrgJa.setVisibility(View.GONE);

                        //step2- show the list
                        jrRecyclerView.setVisibility(View.VISIBLE);


                    }
                    else {// no data in array mean no result

                        //No result
                        prgJa.setVisibility(View.GONE);
                        txtPrgJa.setVisibility(View.GONE);

                        errorLayout.setVisibility(View.VISIBLE);
                        llNoResult.setVisibility(View.VISIBLE);


                    }


                }
                else if (status.equalsIgnoreCase("Failure")) {
                    Log.e(TAG, "Failure ");

                    prgJa.setVisibility(View.GONE);
                    txtPrgJa.setVisibility(View.GONE);


                    errorLayout.setVisibility(View.VISIBLE);
                    llNoResult.setVisibility(View.VISIBLE);
                }
                else {
                    Log.e(TAG, "case not match is "  );

                    prgJa.setVisibility(View.GONE);
                    txtPrgJa.setVisibility(View.GONE);

                    errorLayout.setVisibility(View.VISIBLE);
                    llServerError.setVisibility(View.VISIBLE);
                }




            }
            catch (JSONException e) {
                e.printStackTrace();

                Log.e(TAG, "JSONException is " + e.getMessage());

                prgJa.setVisibility(View.GONE);
                txtPrgJa.setVisibility(View.GONE);

                errorLayout.setVisibility(View.VISIBLE);
                llServerError.setVisibility(View.VISIBLE);
            }


        }

    }

    private void Join_RequestWS() {

        doInbackground2();

    }

    private void doInbackground2()
    {
        Log.e(TAG, "Join_RequestWS link is " + AllLinsks.joinrequest + SettingPreffrence.getuser_mobilenumber(Join_Request.this) + "/" + SettingPreffrence.getuser_token(Join_Request.this));

        NetworkCall networkCall = new NetworkCall(
                //context
                Join_Request.this,

                //response
                new ResponseListenerVolley() {
                    @Override
                    public void onSuccess(String response) {
                        String status, message;
                        error_flag2 = 0;
                        postExecute2(response);

                    }
                },

                //error
                new ErrorListenerVolley() {
                    @Override
                    public void onError(VolleyError volleyError) {

                        if ((volleyError instanceof NoConnectionError)|| (volleyError instanceof TimeoutError)) {
                            error_flag2 = 1;
                            postExecute2("");
                        }
                        else if (volleyError instanceof ServerError)
                        {
                            String response = null, status, message;
                            try {
                                response = new String(volleyError.networkResponse.data, "utf-8");
                                L.e(TAG, "response is  ", response);
                                JSONObject jsonObject_error = new JSONObject(response);

                                error_flag2 = 0;
                                postExecute2(response);

                            } catch (UnsupportedEncodingException e) {// Data is not able to UnsupportedEncodingException
                                e.printStackTrace();

                                error_flag2 = 5;
                                postExecute2("");

                            } catch (JSONException e) {// Data is not able to parse
                                e.printStackTrace();

                                error_flag2= 5;
                                postExecute2("");
                            }


                        }
                        else if (volleyError instanceof AuthFailureError) {
                            error_flag2 = 2;
                            postExecute2("");
                        }
                        else {//Unknown error
                            error_flag2 = 5;
                            postExecute2("");
                        }
                    }
                },

                AllLinsks.joinrequest + SettingPreffrence.getuser_mobilenumber(Join_Request.this) + "/" + SettingPreffrence.getuser_token(Join_Request.this),
                getString(R.string.prg_message),
                true

        );

        HashMap<String, String> hashMap = new HashMap<String, String>();
        hashMap.put("uid", uid);

        networkCall.makeStringRequest_POST(hashMap);
    }


    public  void postExecute2(String response) {

        Log.e(TAG,"error_flag2 is " + error_flag2);

        if (error_flag2 == 1) {//no Internet

            showSnackBar(getString(R.string.No_network), getString(R.string.Retry), Snackbar.LENGTH_INDEFINITE);

             }
        else if (error_flag2==2) {// Invalid Token

            showSnackBar(getString(R.string.INVALID_TOKEN), "", snackbar.LENGTH_SHORT);

        }
        else if (error_flag2==3) {//Unknown error

            showSnackBar(getString(R.string.Unknown_error),"",Snackbar.LENGTH_SHORT);
              }
        else if (error_flag2 == 0) {

            try {
                JSONObject jsonObject = new JSONObject(response);
                String status = jsonObject.getString("status");
                String message = jsonObject.getString("message");

                if (status.equalsIgnoreCase("SUCCESS")) {


                    alerdialog = utilclass.showAlerDialog(getFragmentManager(), "Your request has been sent ", "", "OK", false, new AlertDialogListener() {
                        @Override
                        public void setRightButton() {

                            finish();
                            alerdialog.dismiss();

                        }

                        @Override
                        public void setLeftButton() {

                        }
                    });

                }
                else if (status.equalsIgnoreCase("Failure")) {
                    showSnackBar(getString(R.string.USER_ALREADY_PART_OF_GROUP), "", Snackbar.LENGTH_SHORT);
                }
                else {
                    showSnackBar(getString(R.string.Unknown_error), "", Snackbar.LENGTH_SHORT);

                }


            } catch (JSONException e) {
                e.printStackTrace();
            }

        }
        else {//if case not match

            showSnackBar(getString(R.string.Unknown_error),"",Snackbar.LENGTH_SHORT);
        }



    }

    private void setUpToolbar() {
        toolbar.setNavigationIcon(R.drawable.btn_back_wt);
        toolbar.setNavigationOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v)
            {
                finish();
            }
        });
    }


    private void showSnackBar(String message, String buttontext,int length)
    {
        snackbar= Snackbar.make(rlRoot,message,length);
        snackbar.setActionTextColor(Color.RED);

        if (!buttontext.isEmpty())
        {
            snackbar.setAction(buttontext, new OnClickListener() {
                @Override
                public void onClick(View v) {
                    Join_RequestWS();
                }
            });
        }
        snackbar.show();

    }

    @Override
    public void onClick(View v)
    {
        if (v==llNoInternet || v==llNoResult || v==llNoResult )
            Group_SearchWS();

    }


}
