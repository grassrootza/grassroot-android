package com.techmorphosis.grassroot.ui.activities;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.android.volley.AuthFailureError;
import com.android.volley.NoConnectionError;
import com.android.volley.ServerError;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.techmorphosis.grassroot.Network.AllLinsks;
import com.techmorphosis.grassroot.Network.NetworkCall;
import com.techmorphosis.grassroot.R;
import com.techmorphosis.grassroot.models.Group_ActivitiesModel;
import com.techmorphosis.grassroot.utils.ProgressBarCircularIndeterminate;
import com.techmorphosis.grassroot.utils.SettingPreffrence;
import com.techmorphosis.grassroot.utils.UtilClass;
import com.techmorphosis.grassroot.utils.listener.ErrorListenerVolley;
import com.techmorphosis.grassroot.utils.listener.ResponseListenerVolley;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class ViewVote extends PortraitActivity implements View.OnClickListener{

            /*View*/
    private Toolbar vvToolbar;
    private TextView txtToolbar;
    private TextView txtVvTitle;
    private RelativeLayout rlNested;
    private TextView txtVvGroupname;
    private TextView txtVvDeadline;
    private TextView txtVvDescription;
    private RelativeLayout rlVoteStatus;
    private LinearLayout llImageHolder;
    private TextView txtHeader;
    private UtilClass utilClass;
    private ScrollView scrollView;

    private LinearLayout rlVvMainLayout;
    private ProgressBarCircularIndeterminate progressBarCircularIndeterminate;
    private TextView txtPrg;
    private Button bt_editVote;

    LinearLayout mLinearLayout;
    RelativeLayout mRelativeLayoutHeader;
    ValueAnimator mAnimator;
    private LinearLayout llYes;
    private TextView countYes;
    private LinearLayout llNo;
    private TextView countNo;
    private ImageView ivExpand;


            /*Variables*/
    public  String voteid;
    private String id;
    private String title;
    private String description;
    private String name;
    private String type;
    private String deadline;
    private Boolean hasResponded;
    private Boolean canAction;
    private String reply;
    private Boolean canEdit;
    private String no;
    private String yes;
    private String abstained;
    private String no_reply;
    private String possible;
    private String cancelled;

        /*others*/
    private static final String TAG = "ViewVote";
    private TextView txtYes;
    private TextView txtNo;
    private RelativeLayout vvRoot;
    private Snackbar snackbar;
    private ImageView Thumpsdown;
    private ImageView Thumpsup;

    private View errorLayout;
    private LinearLayout llNoResult;
    private LinearLayout llNoInternet;
    private LinearLayout llServerError;
    private LinearLayout llInvalidToken;


    private int error_flag1; // 0- success ,1 - No Internet , 4-Invalid token , 5- Unknown error
    private int error_flag2;

    ProgressDialog progressDialog;
    private String  update="0";
    private String deadlineISO;

    private LinearLayout llMaybe;
    private TextView txtMaybe;
    private TextView countMaybe;
    //private LinearLayout llInvalid;
    private TextView txtInvalid;
    private TextView countInvalid;
    private LinearLayout llNumberOfUsers;
    private TextView txtNumberOfUsers;
    private TextView countNumberOfUsers;
    private LinearLayout llNumberNoRSVP;
    private TextView txtNumberNoRSVP;
    private TextView countNumberNoRSVP;
    private String maybe;
    private String invalid;
    private String numberOfUsers;
    private String numberOfRsvp;
    private int height;
    private int width;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_vote);

        findAllViews();
        if (getIntent()!=null)
        {
            voteid = getIntent().getExtras().getString("voteid");
        }

        setUpToolbar();
        init();


    }

    private void setUpToolbar() {
        vvToolbar.setNavigationIcon(R.drawable.btn_back_wt);
        vvToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
    }

    public void onBackPressed()
    {
        Intent i = new Intent();
        i.putExtra("update",update);
        setResult(1, i);
        finish();
    }

    private void init() {
         utilClass = new UtilClass();
         CallViewVoteWS();
    }

    private void CallViewVoteWS()
    {

    preExecute1();
    doInBackground1();
    }


    private void preExecute1()
    {

        error_flag1 = 0;

        //hide the MainLayout
        rlVvMainLayout.setVisibility(View.GONE);


         //hide the error Layout
        errorLayout.setVisibility(View.GONE);
        llNoResult.setVisibility(View.GONE);
        llNoInternet.setVisibility(View.GONE);
        llServerError.setVisibility(View.GONE);
        llInvalidToken.setVisibility(View.GONE);

        //show thr progress bar
        progressBarCircularIndeterminate.setVisibility(View.VISIBLE);
        txtPrg.setVisibility(View.VISIBLE);


    }

    private void doInBackground1()
    {

        NetworkCall networkcall = new NetworkCall
                (
                        ViewVote.this,
                        new ResponseListenerVolley() {
                            @Override
                            public void onSuccess(String s)
                            {

                                error_flag1 =0;
                                postExecute1(s);
                            }
                        },
                        new ErrorListenerVolley() {
                            @Override
                            public void onError(VolleyError volleyError) {

                                if (volleyError instanceof NoConnectionError || volleyError instanceof TimeoutError)
                                {
                                  error_flag1 =1;
                                    postExecute1("");

                                }
                                else if (volleyError instanceof ServerError)
                                {
                                    try {
                                        String responsebody = new String(volleyError.networkResponse.data,"utf-8");
                                        Log.e(TAG, "responsebody is " + responsebody);
                                        try {
                                            JSONObject jsonObject = new JSONObject(responsebody);
                                            error_flag1 =0;
                                            postExecute1(responsebody);

                                        } catch (JSONException e) {
                                            e.printStackTrace();
                                            error_flag1 =5;
                                            postExecute1("");

                                        }

                                    } catch (UnsupportedEncodingException e) {
                                        e.printStackTrace();

                                        error_flag1 =5;
                                        postExecute1("");


                                    }

                                }
                                else if (volleyError instanceof AuthFailureError)
                                {
                                    error_flag1 =4;
                                    postExecute1("");


                                }
                                else
                                {
                                    error_flag1 = 5;
                                    postExecute1("");

                                }
                            }
                        },
                        AllLinsks.VoteView + voteid + "/" + SettingPreffrence.getPREF_Phone_Token(ViewVote.this),
                        "",
                        false


                );
        networkcall.makeStringRequest_GET();
    }


    private void postExecute1(String response)
    {
        Log.e(TAG, "error_flag1 is  " + error_flag1);


        if (error_flag1 == 1) {//no Internet
            progressBarCircularIndeterminate.setVisibility(View.GONE);
            txtPrg.setVisibility(View.GONE);

            errorLayout.setVisibility(View.VISIBLE);
            llNoInternet.setVisibility(View.VISIBLE);

        }
        else if (error_flag1 == 4)//Logout
        {
            progressBarCircularIndeterminate.setVisibility(View.GONE);
            txtPrg.setVisibility(View.GONE);

            errorLayout.setVisibility(View.VISIBLE);
            llInvalidToken.setVisibility(View.VISIBLE);
           // showSnackBar(getString(R.string.INVALID_TOKEN),"","","",snackbar.LENGTH_INDEFINITE);

        }
        else if (error_flag1 == 5) {//catch error
            progressBarCircularIndeterminate.setVisibility(View.GONE);
            txtPrg.setVisibility(View.GONE);

            errorLayout.setVisibility(View.VISIBLE);
            llServerError.setVisibility(View.VISIBLE);

        } else if (error_flag1 == 0) {


            try {
                JSONObject jsonObject = new JSONObject(response);

                if (jsonObject.getString("status").equalsIgnoreCase("SUCCESS")) {
                    Log.e(TAG, "if");
                    JSONObject dataObject = jsonObject.getJSONObject("data");
                    id = dataObject.getString("id");
                    title = dataObject.getString("title");
                    description = dataObject.getString("description");
                    name = dataObject.getString("name");
                    type = dataObject.getString("type");
                    deadline = dataObject.getString("deadline");
                    deadlineISO = dataObject.getString("deadlineISO");
                    hasResponded = dataObject.getBoolean("hasResponded");
                    canAction = dataObject.getBoolean("canAction");
                    reply = dataObject.getString("reply");
                    canEdit = dataObject.getBoolean("canEdit");
                    cancelled = dataObject.getString("cancelled");

                    Group_ActivitiesModel model = new Group_ActivitiesModel();
                    model.hasResponded = hasResponded;
                    model.canAction = canAction;
                    model.reply = reply;

                    JSONObject totalObject = dataObject.getJSONObject("totals");
                    no = totalObject.getString("no");
                    yes = totalObject.getString("yes");
                    maybe = totalObject.getString("maybe");
                    invalid = totalObject.getString("invalid");
                    numberOfUsers = totalObject.getString("numberOfUsers");
                    numberOfRsvp = totalObject.getString("numberNoRSVP");

              /*  abstained = totalObject.getString("abstained");
                no_reply = totalObject.getString("no_reply");
                possible = totalObject.getString("possible");
            */
                    setView(model);

                } else
                    Log.e(TAG, "else");


            } catch (JSONException e) {
                e.printStackTrace();

                progressBarCircularIndeterminate.setVisibility(View.GONE);
                txtPrg.setVisibility(View.GONE);


                rlVvMainLayout.setVisibility(View.GONE);
                errorLayout.setVisibility(View.VISIBLE);
                llServerError.setVisibility(View.VISIBLE);
            }
        }

    }

    private void setView(Group_ActivitiesModel model)
    {
        Log.e(TAG, "setView");

        txtVvTitle.setText(title);
        txtVvGroupname.setText("Posted by " + name);
        try {
            txtVvDeadline.setText(deadline);
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG,"e is " + e.getMessage());
        }
        txtVvDescription.setText(description);
        txtYes.setText(getString(R.string.vv_yes));
        countYes.setText(yes);

        txtNo.setText(getString(R.string.vv_no));
        countNo.setText(no);

        txtMaybe.setText(getString(R.string.vv_maybe));
        countMaybe.setText(maybe);

        txtInvalid.setText(getString(R.string.vv_invalid));
        countInvalid.setText(invalid);

        txtNumberOfUsers.setText(getString(R.string.vv_numberOfUsers));
        countNumberOfUsers.setText(numberOfRsvp);

        txtNumberNoRSVP.setText(getString(R.string.vv_numberNoRSVP));
        countNumberNoRSVP.setText(numberOfRsvp);


        votemeeting(model);

        if (canEdit) {
            bt_editVote.setEnabled(true);
            bt_editVote.setOnClickListener(editVote_button());
        }
        progressBarCircularIndeterminate.setVisibility(View.GONE);
        txtPrg.setVisibility(View.GONE);

        rlVvMainLayout.setVisibility(View.VISIBLE);



    }

    public String convertW3CTODeviceTimeZone(String strDate) throws Exception
    {
        SimpleDateFormat simpleDateFormatW3C = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        Date dateServer = simpleDateFormatW3C.parse(strDate);

        TimeZone deviceTimeZone = TimeZone.getDefault();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
        simpleDateFormat.setTimeZone(deviceTimeZone);

        String formattedDate = simpleDateFormat.format(dateServer);
        // long timeMilliness=new Date(formattedDate).getTime();
        return formattedDate;
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

        Thumpsup.setEnabled(true);
        Thumpsdown.setEnabled(true);

        //Thumbs down
        Thumpsup.setImageResource(R.drawable.ic_no_vote_inactive);
        //Thumbs up
        Thumpsdown.setImageResource(R.drawable.ic_vote_inactive);

    }


    private void canActionIsTrue(Group_ActivitiesModel model)
    {
        if (model.reply.equalsIgnoreCase("Yes"))
        {

            //Thumbs up
            Thumpsup.setImageResource(R.drawable.ic_vote_active);

            //Thumbs down
            Thumpsdown.setImageResource(R.drawable.ic_no_vote_inactive);

            Thumpsup.setEnabled(false);
            Thumpsdown.setEnabled(true);


        }
        else  if (model.reply.equalsIgnoreCase("No"))
        {

            //Thumbs up
            Thumpsup.setImageResource(R.drawable.ic_vote_inactive);
            //Thumbs down
            Thumpsdown.setImageResource(R.drawable.ic_no_vote_active);


            Thumpsup.setEnabled(true);
            Thumpsdown.setEnabled(false);




        }
        else if (model.reply.equalsIgnoreCase("NO_RESPONSE"))
        {
            //Thumbs up
            Thumpsup.setImageResource(R.drawable.ic_vote_inactive);
            //Thumbs down
            Thumpsdown.setImageResource(R.drawable.ic_no_vote_inactive);

            Thumpsup.setEnabled(true);
            Thumpsdown.setEnabled(true);
        }


    }

    private void canActionIsFalse(Group_ActivitiesModel model)
    {

        Thumpsup.setEnabled(false);
        Thumpsdown.setEnabled(false);

        if (model.reply.equalsIgnoreCase("Yes"))
        {

            //Thumbs up
            Thumpsup.setImageResource(R.drawable.ic_vote_active);

            //Thumbs down
            Thumpsdown.setImageResource(R.drawable.ic_no_vote_inactive);


        }
        else  if (model.reply.equalsIgnoreCase("No"))
        {

            //Thumbs up
            Thumpsup.setImageResource(R.drawable.ic_vote_inactive);
            //Thumbs down
            Thumpsdown.setImageResource(R.drawable.ic_no_vote_active);




        }
        else if (model.reply.equalsIgnoreCase("NO_RESPONSE"))
        {
            //Thumbs up
            Thumpsup.setImageResource(R.drawable.ic_vote_inactive);
            //Thumbs down
            Thumpsdown.setImageResource(R.drawable.ic_no_vote_inactive);

        }


    }


    private void CallVoteWS(String response) {

        //preExecute2
        preExecute2(response);



        //doInBacground
        final String finalResponse = response;
        final String finalResponse1 = response;
        NetworkCall networkCall = new NetworkCall
                (
                        ViewVote.this,

                        new ResponseListenerVolley() {
                            @Override
                            public void onSuccess(String s) {
                                //parse string to json
                                error_flag2=0;
                                postExecute2(s);

                            }
                        },

                        new ErrorListenerVolley() {
                            @Override
                            public void onError(VolleyError volleyError) {

                                if (volleyError instanceof NoConnectionError || volleyError instanceof TimeoutError)
                                {
                                    error_flag2 = 1;
                                    postExecute2("");
                                }
                                else  if (volleyError instanceof ServerError)
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
                                                showSnackBar(getString(R.string.ga_VoteFailure), "", "", "", snackbar.LENGTH_SHORT);
                                            } else {
                                                showSnackBar(getString(R.string.Unknown_error), "", "", "", snackbar.LENGTH_SHORT);

                                            }

                                        }


                                    } catch (UnsupportedEncodingException e) {
                                        e.printStackTrace();
                                        showSnackBar(getString(R.string.Unknown_error), "", "", "", snackbar.LENGTH_SHORT);

                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                        showSnackBar(getString(R.string.Unknown_error), "", "", "", snackbar.LENGTH_SHORT);

                                    }


                                }
                                else if (volleyError instanceof  AuthFailureError)
                                {
                                    showSnackBar(getString(R.string.INVALID_TOKEN),"","","",snackbar.LENGTH_INDEFINITE);
                                }
                                else
                                {
                                    showSnackBar(getString(R.string.Unknown_error),"","","",snackbar.LENGTH_SHORT);
                                }


                            }
                        },

                        AllLinsks.Vote + voteid + "/" + SettingPreffrence.getPREF_Phone_Token(ViewVote.this) + "?response=" + response,
                        getString(R.string.prg_message),
                        true

                );

        networkCall.makeStringRequest_GET();


        //postExecute2
        /*update model and notify adapter*/

    }

    private void postExecute2(String respnse2)
    {
        JSONObject jsonObject = null;
        try {
            jsonObject = new JSONObject(respnse2);
            if (jsonObject.getString("status").equalsIgnoreCase("SUCCESS")) {
                showSnackBar(getString(R.string.ga_Votesend), "", "", "", Snackbar.LENGTH_SHORT);
                update="1";
                CallViewVoteWS();
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    private void preExecute2(String response)
    {
        error_flag2 = 0;


    }

    private void findAllViews() {
        vvRoot = (RelativeLayout) findViewById(R.id.vv_root);
        vvToolbar = (Toolbar) findViewById(R.id.vv_toolbar);
        scrollView = (ScrollView) findViewById(R.id.scrollView);
        rlVvMainLayout = (LinearLayout) findViewById(R.id.rl_vv_main_layout);
        txtToolbar = (TextView) findViewById(R.id.txt_toolbar);
        txtVvTitle = (TextView) findViewById(R.id.txt_vv_title);
        rlNested = (RelativeLayout) findViewById(R.id.rl_nested);
        txtVvGroupname = (TextView) findViewById(R.id.txt_vv_groupname);
        txtVvDeadline = (TextView) findViewById(R.id.txt_vv_deadline);
        txtVvDescription = (TextView) findViewById(R.id.txt_vv_description);
        rlVoteStatus = (RelativeLayout) findViewById(R.id.rl_vote_status);
        llImageHolder = (LinearLayout) findViewById(R.id.ll_image_holder);
        txtHeader = (TextView) findViewById(R.id.txt_header);

        errorLayout = findViewById(R.id.error_layout);

        llNoResult = (LinearLayout) errorLayout.findViewById(R.id.ll_no_result);
        llNoInternet = (LinearLayout) errorLayout.findViewById(R.id.ll_no_internet);
        llServerError = (LinearLayout) errorLayout.findViewById(R.id.ll_server_error);
        llInvalidToken = (LinearLayout) errorLayout.findViewById(R.id.ll_invalid_token);

        progressBarCircularIndeterminate = (ProgressBarCircularIndeterminate) findViewById(R.id.progressBarCircularIndeterminate);
        txtPrg = (TextView) findViewById(R.id.txt_prg);

        mRelativeLayoutHeader = (RelativeLayout) findViewById(R.id.header);
        mLinearLayout = (LinearLayout) findViewById(R.id.expandable);

        llYes = (LinearLayout) findViewById(R.id.ll_yes);
        txtYes = (TextView) findViewById(R.id.txt_yes);
        countYes = (TextView) findViewById(R.id.count_yes);

        llNo = (LinearLayout) findViewById(R.id.ll_no);
        txtNo = (TextView) findViewById(R.id.txt_no);;
        countNo = (TextView) findViewById(R.id.count_no);

        llMaybe = (LinearLayout) findViewById(R.id.ll_maybe);
        txtMaybe = (TextView) findViewById(R.id.txt_maybe);
        countMaybe = (TextView) findViewById(R.id.count_maybe);

        txtInvalid = (TextView) findViewById(R.id.txt_invalid);
        countInvalid = (TextView) findViewById(R.id.count_invalid);

        llNumberOfUsers = (LinearLayout) findViewById(R.id.ll_numberOfUsers);
        txtNumberOfUsers = (TextView) findViewById(R.id.txt_numberOfUsers);
        countNumberOfUsers = (TextView) findViewById(R.id.count_numberOfUsers);

        llNumberNoRSVP = (LinearLayout) findViewById(R.id.ll_numberNoRSVP);
        txtNumberNoRSVP = (TextView) findViewById(R.id.txt_numberNoRSVP);
        countNumberNoRSVP = (TextView) findViewById(R.id.count_numberNoRSVP);


        ivExpand = (ImageView) findViewById(R.id.iv_expand);
        Thumpsup = (ImageView) findViewById(R.id.Thumpsup);
        Thumpsdown = (ImageView) findViewById(R.id.Thumpsdown);

        //OnClick

        bt_editVote= (Button) findViewById(R.id.bt_editVote);
    /*    ViewTreeObserver vto = bt_editVote.getViewTreeObserver();
        vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                bt_editVote.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                width = bt_editVote.getMeasuredWidth();
                height = bt_editVote.getMeasuredHeight();

            }
        });
*/
   /*     bt_editVote.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                height= bt_editVote.getHeight();
                Toast.makeText(ViewVote.this, "height " + bt_editVote.getHeight(), Toast.LENGTH_SHORT).show();

            }
        });
        Toast.makeText(ViewVote.this, "bt_editVote.getHeight() " + height, Toast.LENGTH_SHORT).show();

        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) scrollView
                .getLayoutParams();

        layoutParams.bottomMargin = bt_editVote.getHeight()+20;
        scrollView.setLayoutParams(layoutParams);*/
        bt_editVote.setEnabled(false);

        mRelativeLayoutHeader.setOnClickListener(expandableHeader());
        Thumpsup.setOnClickListener(Thumpsup());
        Thumpsdown.setOnClickListener(Thumpsdown());
        llServerError.setOnClickListener(this);
        llNoInternet.setOnClickListener(this);

        //Add onPreDrawListener
        mLinearLayout.getViewTreeObserver().addOnPreDrawListener(
                new ViewTreeObserver.OnPreDrawListener() {

                    @Override
                    public boolean onPreDraw() {
                        mLinearLayout.getViewTreeObserver().removeOnPreDrawListener(this);
                        mLinearLayout.setVisibility(View.GONE);

                        final int widthSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
                        final int heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
                        mLinearLayout.measure(widthSpec, heightSpec);

                        mAnimator = slideAnimator(0, mLinearLayout.getMeasuredHeight());
                        return true;
                    }
                });

    }

    private View.OnClickListener Thumpsdown() {
            return new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    CallVoteWS("No");
                }
            };
    }

    private View.OnClickListener Thumpsup() {
            return new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    CallVoteWS("Yes");
                }
            };
    }

    private View.OnClickListener expandableHeader() {
                return new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (mLinearLayout.getVisibility()==View.GONE){
                            ivExpand.setImageResource(R.drawable.ic_arrow_up);
                            expand();
                        }else{
                            ivExpand.setImageResource(R.drawable.ic_arrow_down);
                            collapse();
                        }
                    }
                };
    }

    private View.OnClickListener editVote_button()
    {
            return new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    if (canEdit){
                        Intent i= new Intent(ViewVote.this,EditVote.class);
                        i.putExtra("description",description);
                        i.putExtra("deadline",deadline);
                        i.putExtra("voteid",voteid);
                        i.putExtra("title",title);
                        startActivityForResult(i,1);

                    }

                }
            };
    }


    @Override
    public void onClick(View v)
    {
        if (v==llNoResult || v==llServerError || v==llNoInternet )
            CallViewVoteWS();

    }

    private void expand() {
        //set Visible
        mLinearLayout.setVisibility(View.VISIBLE);


        mAnimator.start();
    }

    private void collapse() {
        int finalHeight = mLinearLayout.getHeight();

        ValueAnimator mAnimator = slideAnimator(finalHeight, 0);

        mAnimator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationEnd(Animator animator) {
                //Height=0, but it set visibility to GONE
                mLinearLayout.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationStart(Animator animator) {
            }

            @Override
            public void onAnimationCancel(Animator animator) {
            }

            @Override
            public void onAnimationRepeat(Animator animator) {
            }
        });
        mAnimator.start();
    }


    private ValueAnimator slideAnimator(int start, int end) {

        ValueAnimator animator = ValueAnimator.ofInt(start, end);


        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                //Update Height
                int value = (Integer) valueAnimator.getAnimatedValue();

                ViewGroup.LayoutParams layoutParams = mLinearLayout.getLayoutParams();
                layoutParams.height = value;
                mLinearLayout.setLayoutParams(layoutParams);
            }
        });
        return animator;
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);


        if (resultCode == 1 && requestCode == 1) {

            Log.e(this.TAG, "resultCode==1 ");
            if (data != null)
            {
                showSnackBar(getString(R.string.vv_voteup), "", "", "", Snackbar.LENGTH_SHORT);
                txtVvDeadline.setText(data.getStringExtra("deadline"));
                deadline= data.getStringExtra("deadline");
                txtVvDescription.setText(data.getStringExtra("description"));
                description = data.getStringExtra("description");
                update="1";
            }
        } else  {
            Log.e(this.TAG, "resultCode==2");

        }
    }



    private void showSnackBar(String message, final String actionButtontext,  final String type,final  String response,int length)
    {
        snackbar = Snackbar.make(vvRoot, message, length);
        snackbar.setActionTextColor(Color.RED);

        if (!actionButtontext.isEmpty() )
        {
            snackbar.setAction(actionButtontext, new View.OnClickListener() {
                @Override
                public void onClick(View v) {


                    if (type.equalsIgnoreCase("VoteMeeting")) {
                        CallVoteWS(response);
                    } else {
                        CallViewVoteWS();

                    }
                }
            });
        }

        snackbar.show();

    }


}

