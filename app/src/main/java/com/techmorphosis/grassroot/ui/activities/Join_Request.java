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
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.android.volley.NoConnectionError;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.techmorphosis.grassroot.Interface.ClickListener;
import com.techmorphosis.grassroot.Network.AllLinsks;
import com.techmorphosis.grassroot.Network.NetworkCall;
import com.techmorphosis.grassroot.R;
import com.techmorphosis.grassroot.RecyclerView.RecyclerTouchListener;
import com.techmorphosis.grassroot.adapters.JoinRequestAdapter;
import com.techmorphosis.grassroot.models.Join_RequestModel;
import com.techmorphosis.grassroot.ui.fragments.AlertDialogFragment;
import com.techmorphosis.grassroot.utils.SettingPreference;
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
    private View errorLayout;
    private ImageView imNOResults;
    private ImageView imServerError;
    private ImageView imNOInternet;
    private AlertDialogFragment alerdialog;
    String uid;

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
        errorLayout =  findViewById(R.id.error_layout);
        imNOResults = (ImageView) errorLayout.findViewById(R.id.im_no_results);
        imServerError = (ImageView) errorLayout.findViewById(R.id.im_server_error);
        imNOInternet = (ImageView) errorLayout.findViewById(R.id.im_no_internet);
        imNOResults.setOnClickListener(this);
        imServerError.setOnClickListener(this);
        imNOInternet.setOnClickListener(this);

        jrRecyclerView = (RecyclerView) findViewById(R.id.jr_RecyclerView);
        et_searchbox=(EditText) findViewById(R.id.et_searchbox);
        et_searchbox.addTextChangedListener(new TextWatcher()
        {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s)
            {
                if (s.length()>0)
                {
                    et_searchbox.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.btn_close, 0);
                    btn_close=true;
                }
                else
                {
                    et_searchbox.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.btn_search_gray, 0);
                    btn_close=false;
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
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event)
            {
                if (actionId== EditorInfo.IME_ACTION_SEARCH)
                {
                    try {
                        InputMethodManager imm= (InputMethodManager) getApplicationContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    if (et_searchbox.getText().toString().trim().isEmpty())
                    {
                        showSnackBar(getString(R.string.validate_search_box),"",Snackbar.LENGTH_SHORT);

                    }
                    else
                    {
                        Group_SearchWS();
                    }



                }

                return false;
            }
        });

    }

    private void Group_SearchWS()
    {

        Log.e(TAG, "Group_SearchWS");
        joinrequestList.clear();
        jrRecyclerView.setVisibility(View.INVISIBLE);
        errorLayout.setVisibility(View.INVISIBLE);
        imNOInternet.setVisibility(View.INVISIBLE);
        imNOResults.setVisibility(View.INVISIBLE);
        imServerError.setVisibility(View.INVISIBLE);


        String prgMessage = "Please Wait..";
        boolean prgboolean = true;

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
                        public void onSuccess(String s)
                        {
                            Log.e(TAG, " onSuccess " + s);

                            jrRecyclerView.setVisibility(View.VISIBLE);

                            String status,message,code,groupName,description,groupCreator,count;
                            try {
                                JSONObject jsonobject= new JSONObject(s);
                                status= jsonobject.getString("status");


                                if (status.equalsIgnoreCase("SUCCESS"))
                                {

                                    code = jsonobject.getString("code");
                                    message = jsonobject.getString("message");
                                    JSONArray jsonarray = jsonobject.getJSONArray("data");
                                    for (int i = 0; i < jsonarray.length() ; i++)
                                    {
                                        JSONObject json=jsonarray.getJSONObject(i);
                                        uid= json.getString("id");
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

                                  /*      Log.e(TAG, "i is " + i);
                                        Log.e(TAG, "id is " + uid);
                                        Log.e(TAG, "groupName is " + groupName);
                                        Log.e(TAG, "description is " + description);
                                        Log.e(TAG, "groupCreator is " + groupCreator);
                                        Log.e(TAG, "count is " + count);*/


                                    }

                                  /*  Log.e(TAG, "status is " + status);
                                    Log.e(TAG, "code is " + code);
                                    Log.e(TAG, "message is " + message);*/

                                    //pre-execute
                                    joinrequestAdapter.clearApplications();

                                    jrRecyclerView.setVisibility(View.VISIBLE);

                                    joinrequestAdapter.addApplications(joinrequestList);

                                }




                            }
                            catch (JSONException e) {
                                e.printStackTrace();
                            }



                        }
                    }
                    ,
                    new ErrorListenerVolley() {
                        @Override
                        public void onError(VolleyError volleyError) {

                            if ((volleyError instanceof NoConnectionError) || (volleyError instanceof TimeoutError))
                            {
                                jrRecyclerView.setVisibility(View.GONE);
                                errorLayout.setVisibility(View.VISIBLE);
                                imNOInternet.setVisibility(View.VISIBLE);

                            }
                            else
                            {
                                try {
                                    String responseBody = new String(volleyError.networkResponse.data,"utf-8");
                                    Log.e(TAG, "responseBody " + responseBody);
                                    String status, message, code = null;
                                    JSONObject jsonObject = new JSONObject(responseBody);
                                    status = jsonObject.getString("status");
                                    message = jsonObject.getString("message");

                                    if (status.equalsIgnoreCase("Failure")) {
                                        Log.e(TAG, "failure");
                                        Log.e(TAG, "code is " + code);
                                        Log.e(TAG, "message is " + message);

                                       jrRecyclerView.setVisibility(View.GONE);
                                        errorLayout.setVisibility(View.VISIBLE);
                                        imNOResults.setVisibility(View.VISIBLE);



                                    }



                                } catch (UnsupportedEncodingException e) {
                                    e.printStackTrace();
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }

                            }

                        }
                    }
                    ,AllLinsks.groupsearch + URLEncoder.encode(et_searchbox.getText().toString(), "UTF-8")
                    ,prgMessage
                    ,prgboolean
            );

            networkCall.makeStringRequest_GET();

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }


    }

    private void Join_RequestWS()
    {
        Log.e(TAG, "Join_RequestWS");


        Log.e(TAG,"Join_RequestWS link is "  + AllLinsks.joinrequest+ SettingPreference.getuser_mobilenumber(Join_Request.this)+"/"+ SettingPreference.getuser_token(Join_Request.this));

        NetworkCall networkCall = new NetworkCall(
                //context
                Join_Request.this,

                //response
                new ResponseListenerVolley() {
                    @Override
                    public void onSuccess(String s)
                    {
                        String  status,message;
                        try {
                            JSONObject jsonObject= new JSONObject(s);
                            status=jsonObject.getString("status");
                            message=jsonObject.getString("message");

                            if (status.equalsIgnoreCase("SUCCESS"))
                            {


                                alerdialog = utilclass.showAlerDialog(getFragmentManager(),"Your request has been sent " ,"" ,"OK" ,false,new AlertDialogListener() {
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



                        } catch (JSONException e) {
                            e.printStackTrace();
                        }


                    }
                },

                //error
                new ErrorListenerVolley()
                {
                    @Override
                    public void onError(VolleyError volleyError)
                    {
                        if ((volleyError instanceof NoConnectionError)|| (volleyError instanceof TimeoutError))
                        {
                            showSnackBar(getString(R.string.No_network),getString(R.string.Retry),Snackbar.LENGTH_INDEFINITE);
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
                                if (status.equalsIgnoreCase("FAILURE"))
                                {
                                    Log.e(TAG, "status is" + status);
                                    Log.e(TAG, "message is" + message);

                                    showSnackBar(getString(R.string.INVALID_TOKEN),"",snackbar.LENGTH_SHORT);
                                }


                            } catch (UnsupportedEncodingException e) {
                                e.printStackTrace();
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }

                        }
                    }
                },

                AllLinsks.joinrequest+ SettingPreference.getuser_mobilenumber(Join_Request.this)+"/"+ SettingPreference.getuser_token(Join_Request.this),
                prgMessage,
                prgboolean

        );

        HashMap<String,String> hashMap=new HashMap<String,String>();
        hashMap.put("uid", uid);

        networkCall.makeStringRequest_POST(hashMap);

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
        if (v==imNOInternet || v==imNOResults || v==imNOResults )
            Group_SearchWS();

    }


}
