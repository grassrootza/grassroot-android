package com.techmorphosis.grassroot.ui.activities;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputLayout;
import android.support.v7.widget.CardView;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
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
import com.techmorphosis.grassroot.utils.SettingPreffrence;
import com.techmorphosis.grassroot.utils.listener.ErrorListenerVolley;
import com.techmorphosis.grassroot.utils.listener.ResponseListenerVolley;
import com.wdullaer.materialdatetimepicker.date.DatePickerDialog;
import com.wdullaer.materialdatetimepicker.time.RadialPickerLayout;
import com.wdullaer.materialdatetimepicker.time.TimePickerDialog;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.TimeZone;

public class CreateVote extends PortraitActivity implements TimePickerDialog.OnTimeSetListener, DatePickerDialog.OnDateSetListener {

    private static final String TAG = "CreateVote";
    private Toolbar tlbCv;
    private TextView txtTlbCv;
    private ScrollView cvScrollview;
    private CardView cardView;
    private RelativeLayout rlTxtIpl;
    private TextInputLayout txtIpl;
    private TextView txtTitleCount;
    private TextView txtPostedname;
    private RelativeLayout rlTxtIplDesc;
    private TextInputLayout txtIplDesc;
    private TextView txtDescCount;
    private CardView cvDatepicker;
    private TextView txtDeadlineTitle;
    private TextView txtDeadline;
    private RelativeLayout rlAlertsHeader;
    private RelativeLayout rlAlertsBody;
    private RelativeLayout rlRowOneDay;
    private SwitchCompat swOneDay;
    private RelativeLayout rlRowHalfDay;
    private SwitchCompat swHalfDay;
    private RelativeLayout rlRowOneHour;
    private SwitchCompat swOneHour;
    private RelativeLayout rlRowImmediate;
    private SwitchCompat swImmediate;
    private EditText et_title_cv;
    private EditText et_description_cv;
    private ImageView ivExpandCv;

    ValueAnimator mAnimator;
    private int year, month, day, hour, minute,second;
    private String title;

    String selectedDate;
    private SimpleDateFormat simpleDateFormat;
    private boolean dateselected=false;
    Calendar now;
    private String todaydateString;

    public String[] switchnamearr={"swOneDay","swHalfDay","swOneHour","swImmediate"};
    public SwitchCompat[] switchCompatarr={swOneDay,swHalfDay,swOneHour,swImmediate};
    private CardView rlNotifyHeader;
    private SwitchCompat swNotifyall;
    private Button btcallvote;
    private String notifyGroup;
    private String reminderMins;
    private String members="";
    private String closingTime;
    private Snackbar snackbar;
    private RelativeLayout rlRootCv;
    public  boolean receiver = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_vote);
        if (getIntent().getExtras()!= null)
        {//No Broadcast Reciever
           // Toast.makeText(CreateVote.this, "not null", Toast.LENGTH_SHORT).show();
                receiver=false;
        }
        else
        {//Broadcast Reciever
            //Toast.makeText(CreateVote.this, " null", Toast.LENGTH_SHORT).show();
            receiver = true;
        }

        findAllViews();
        setUpToolbar();


    }

    private void setUpToolbar() {
        tlbCv.setNavigationIcon(R.drawable.btn_back_wt);
        tlbCv.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
    }

    private void findAllViews() {

        rlRootCv = (RelativeLayout) findViewById(R.id.rl_root_cv);
        tlbCv = (Toolbar) findViewById(R.id.tlb_cv);
        txtTlbCv = (TextView) findViewById(R.id.txt_tlb_cv);

        cvScrollview = (ScrollView) findViewById(R.id.cv_scrollview);
        cardView = (CardView) findViewById(R.id.card_view);

        rlTxtIpl = (RelativeLayout) findViewById(R.id.rl_txt_ipl);
        et_title_cv = (EditText) findViewById(R.id.et_title_cv);
        txtTitleCount = (TextView) findViewById(R.id.txt_title_count);

        txtPostedname = (TextView) findViewById(R.id.txt_postedname);

        rlTxtIplDesc = (RelativeLayout) findViewById(R.id.rl_txt_ipl_desc);
        et_description_cv = (EditText) findViewById(R.id.et_description_cv);
        txtDescCount = (TextView) findViewById(R.id.txt_desc_count);

        cvDatepicker = (CardView) findViewById(R.id.cv_datepicker);

        txtDeadlineTitle = (TextView) findViewById(R.id.txt_deadline_title);
        txtDeadline = (TextView) findViewById(R.id.txt_deadline);

        rlAlertsHeader = (RelativeLayout) findViewById(R.id.rl_alerts_header);
        rlAlertsBody = (RelativeLayout) findViewById(R.id.rl_alerts_body);
        ivExpandCv = (ImageView) findViewById(R.id.iv_expand_alert);

        rlRowOneDay = (RelativeLayout) findViewById(R.id.rl_row_one_day);
        swOneDay = (SwitchCompat) findViewById(R.id.sw_one_day);

        rlRowHalfDay = (RelativeLayout) findViewById(R.id.rl_row_half_day);
        swHalfDay = (SwitchCompat) findViewById(R.id.sw_half_day);

        rlRowOneHour = (RelativeLayout) findViewById(R.id.rl_row_one_hour);
        swOneHour = (SwitchCompat) findViewById(R.id.sw_one_hour);

        rlRowImmediate = (RelativeLayout) findViewById(R.id.rl_row_immediate);
        swImmediate = (SwitchCompat) findViewById(R.id.sw_immediate);

        rlNotifyHeader =(CardView) findViewById(R.id.rl_notify_header);
        swNotifyall = (SwitchCompat) findViewById(R.id.sw_notifyall);

        btcallvote = (Button)findViewById(R.id.bt_call_vote);

        
        txtDescCount.setText("0/320");
        txtTitleCount.setText("0/35");
        txtPostedname.setText("Posted by " + SettingPreffrence.getuser_name(CreateVote.this));


        setAllListner();
    }

    private void setAllListner() {


        rlAlertsHeader.setOnClickListener(expandableHeader());
        cvDatepicker.setOnClickListener(datetimepicker());
        btcallvote.setOnClickListener(button_callVote());
        //Add onPreDrawListener
        rlAlertsBody.getViewTreeObserver().addOnPreDrawListener(
                new ViewTreeObserver.OnPreDrawListener() {

                    @Override
                    public boolean onPreDraw() {
                        rlAlertsBody.getViewTreeObserver().removeOnPreDrawListener(this);
                        rlAlertsBody.setVisibility(View.GONE);

                        final int widthSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
                        final int heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
                        rlAlertsBody.measure(widthSpec, heightSpec);

                        mAnimator = slideAnimator(0, rlAlertsBody.getMeasuredHeight());
                        return true;
                    }
                });


        et_title_cv.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                txtTitleCount.setText(s.length() + "/35");
            }
        });


        et_description_cv.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                txtDescCount.setText(s.length() + "/320");
            }
        });
        
        switchListner();




    }

    private View.OnClickListener button_callVote() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FormValidation();

            }
        };
    }

    private void FormValidation() {

        if (TextUtils.isEmpty(et_title_cv.getText().toString()))
        {
  //          Toast.makeText(CreateVote.this, "if et_title_cv", Toast.LENGTH_SHORT).show();
            showSnackBar(getString(R.string.nm_title_error_msg),snackbar.LENGTH_SHORT,"");
        }
        else
        {
           // Toast.makeText(CreateVote.this, "else  et_title_cv", Toast.LENGTH_SHORT).show();

            if (TextUtils.isEmpty(et_description_cv.getText().toString()))
            {

    //            Toast.makeText(CreateVote.this, "if et_description_cv", Toast.LENGTH_SHORT).show();

                showSnackBar(getString(R.string.nm_description_error_msg),snackbar.LENGTH_SHORT,"");


            }
            else
            {
      //          Toast.makeText(CreateVote.this, "else  et_description_cv", Toast.LENGTH_SHORT).show();

                if (TextUtils.isEmpty(closingTime))
                {
//                    Toast.makeText(CreateVote.this, "if   closingTime", Toast.LENGTH_SHORT).show();

                    showSnackBar(getString(R.string.nm_closingtime_msg),snackbar.LENGTH_SHORT,"");
                }
                else
                {
                   // Toast.makeText(CreateVote.this, "else    closingTime", Toast.LENGTH_SHORT).show();


                    callVoteWS();

                }
            }

        }
    }

    private void  callVoteWS()
    {

        NetworkCall networkCall = new NetworkCall
                (
                        CreateVote.this,
                        new ResponseListenerVolley() {
                            @Override
                            public void onSuccess(String s) {
                                try {
                                    JSONObject jsonObject = new JSONObject(s);
                                    if (jsonObject.getString("status").equalsIgnoreCase("SUCCESS"))
                                    {
                                        SettingPreffrence.setPREF_Call_Vote(CreateVote.this, true);
                                        finish();
                                        if (receiver) {
                                            exitActivity();
                                        }

                                        SettingPreffrence.setGroupId(CreateVote.this,"");
                                    }

                                } catch (JSONException e) {
                                    e.printStackTrace();
                                    showSnackBar(getString(R.string.Unknown_error),snackbar.LENGTH_SHORT,"");
                                }

                            }
                        },
                        new ErrorListenerVolley() {
                            @Override
                            public void onError(VolleyError volleyError) {

                                if (volleyError instanceof NoConnectionError || volleyError instanceof TimeoutError) {
                                    showSnackBar(getString(R.string.No_network),snackbar.LENGTH_LONG,getString(R.string.Retry));
                                }
                                else if (volleyError instanceof ServerError)
                                {
                                    showSnackBar(getString(R.string.Unknown_error),snackbar.LENGTH_SHORT,"");
                                }
                                else if (volleyError instanceof AuthFailureError)
                                {
                                    showSnackBar(getString(R.string.INVALID_TOKEN),snackbar.LENGTH_LONG,"");
                                }
                                else
                                {
                                    showSnackBar(getString(R.string.Unknown_error),snackbar.LENGTH_SHORT,"");

                                }
                            }
                        },
                        AllLinsks.CreateVote + SettingPreffrence.getGroupId(CreateVote.this) + "/" + SettingPreffrence.getPREF_Phone_Token(CreateVote.this),
                        getString(R.string.prg_message),
                        true
                );

        HashMap<String, String> hashMap = new HashMap<>();
        hashMap.put("title", et_title_cv.getText().toString());
        hashMap.put("description", et_description_cv.getText().toString());
        hashMap.put("closingTime",closingTime);
        hashMap.put("notifyGroup", notifyGroup);
        hashMap.put("reminderMins", reminderMins);
        hashMap.put("members", members);


        networkCall.makeStringRequest_POST(hashMap);

        Log.e(TAG,"getGroupId is " + SettingPreffrence.getGroupId(CreateVote.this));

        /*Log.e(TAG, "title is  " + et_title_cv.getText().toString());
        Log.e(TAG,"et_description_cv is  " + et_description_cv.getText().toString());
        Log.e(TAG,"closingTime is  " + closingTime);
        Log.e(TAG,"notifyGroup is  " + notifyGroup);
        Log.e(TAG,"reminderMins is  " + reminderMins);
        Log.e(TAG,"members is  " + members);*/

    }

    private void exitActivity() {
        sendBroadcast(new Intent().setAction(getString(R.string.bs_BR_name)));

    }

    private void switchListner() {


        swOneDay.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Log.d(TAG, "swOneDay onCheckedChanged: " + isChecked);

                if (isChecked) {
                    reminderMins = "3";

                    offAll("swOneDay");

                }


            }
        });

        swHalfDay.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Log.d(TAG, "swHalfDay onCheckedChanged: " + isChecked);

                if (isChecked) {
                    reminderMins = "2";

                    offAll("swHalfDay");

                }


            }
        });


        swOneHour.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Log.d(TAG, "swOneHour onCheckedChanged: " + isChecked);

                if (isChecked) {
                    reminderMins = "1";

                    offAll("swOneHour");

                }


            }
        });


        swImmediate.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Log.d(TAG, "swImmediate onCheckedChanged: " + isChecked);

                if (isChecked) {
                    reminderMins = "0";
                    offAll("swImmediate");

                }


            }
        });
        swImmediate.setChecked(true);
        swNotifyall.setChecked(true);
        notifyGroup = "true";
        swNotifyall.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                if (isChecked) {
                  /*  Intent notifyactivity = new Intent(CreateVote.this, VoteNotifyMembers.class);
                    startActivity(notifyactivity);*/
                    notifyGroup = "true";
                }
                else
                    notifyGroup = "false";
            }
        });

    }

    private void offAll(String switchname) {


        if (switchname.equalsIgnoreCase("swOneDay")) {
            swHalfDay.setChecked(false);
            swOneHour.setChecked(false);
            swImmediate.setChecked(false);
        }
        else if (switchname.equalsIgnoreCase("swHalfDay")) {

            swOneDay.setChecked(false);
            swOneHour.setChecked(false);
            swImmediate.setChecked(false);
        }
        else if (switchname.equalsIgnoreCase("swOneHour")) {
            swOneDay.setChecked(false);
            swHalfDay.setChecked(false);
            swImmediate.setChecked(false);
        }
        else if (switchname.equalsIgnoreCase("swImmediate")) {

            swOneDay.setChecked(false);
            swHalfDay.setChecked(false);
            swOneHour.setChecked(false);
        }

    }


    private View.OnClickListener datetimepicker() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                simpleDateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
                Date date1 = null;
                try {
                    if (dateselected)
                    {
                        date1 = (Date) simpleDateFormat.parse(selectedDate);

                    }
                    else
                    {

                        Calendar todaydate = Calendar.getInstance();
                        todaydateString =todaydate.get(Calendar.YEAR)+"-"+todaydate.get(Calendar.MONTH)+"-"+todaydate.get(Calendar.DAY_OF_MONTH)+" "+todaydate.get(Calendar.HOUR_OF_DAY)+":"+todaydate.get(Calendar.MINUTE)+":"+todaydate.get(Calendar.SECOND);

                        try {
                            date1 = (Date) simpleDateFormat.parse(todaydateString);
                        } catch (Exception e) {
                            e.printStackTrace();
                            Log.e(TAG, " e is " + e.getMessage());
                        }
                    }
                } catch (ParseException e) {
                    e.printStackTrace();
                }

                now = Calendar.getInstance();
                now.setTime(date1);

                Calendar today = Calendar.getInstance();

                DatePickerDialog dpd = DatePickerDialog.newInstance(
                        CreateVote.this,
                        now.get(Calendar.YEAR),
                        now.get(Calendar.MONTH),
                        now.get(Calendar.DAY_OF_MONTH)
                );
                dpd.vibrate(true);
                dpd.dismissOnPause(false);
                dpd.setAccentColor(getResources().getColor(R.color.primaryColor));
                dpd.setMinDate(today);
                dpd.show(getFragmentManager(), "Datepickerdialog");

            }


        };
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

    @Override
    public void onTimeSet(RadialPickerLayout view, int hourOfDay, int minutes, int seconds) {

        hour= hourOfDay;
        minute = minutes;
        second = seconds;
        dateselected =true;

        selectedDate=selectedDate+" "+hourOfDay+":"+minutes+":"+seconds;


        txtDeadline.setText( (day < 10 ? "0"+day : day)+"-"+
                (month < 10 ? "0"+(month) : month)+"-"+
                year +"  "+
                (hour < 10 ? "0"+hour : hour)+":"+
                (minute < 10 ? "0"+minute : minute));

        closingTime =year+"-"+
                (month < 10 ? "0"+(month) : month)+"-"+
                (day < 10 ? "0"+day : day)+"T"+
                (hour < 10 ? "0"+hour : hour)+":"+
                (minute < 10 ? "0"+minute : minute);
    }


    @Override
    public void onDateSet(DatePickerDialog view, int years, int monthOfYear, int dayOfMonth) {
        // String date = "You picked the following date: "+dayOfMonth+"/"+(++monthOfYear)+"/"+year;
        year = years;
        month = ++monthOfYear;
        day = dayOfMonth;

        selectedDate= day+"-"+month+"-"+year;
        dateselected =true;

        /*Calendar now = Calendar.getInstance();*/
        TimePickerDialog tpd = TimePickerDialog.newInstance(
                CreateVote.this,
                now.get(Calendar.HOUR_OF_DAY),
                now.get(Calendar.MINUTE),
                false
        );
        tpd.vibrate(true);//yes
        tpd.dismissOnPause(false);//yes
        tpd.enableSeconds(true);
        tpd.setAccentColor(getResources().getColor(R.color.primaryColor));


        tpd.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                Log.d("TimePicker", "Dialog was cancelled");
            }
        });
        tpd.show(getFragmentManager(), "Timepickerdialog");
    }


    private View.OnClickListener expandableHeader() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (rlAlertsBody.getVisibility()==View.GONE){
                    ivExpandCv.setImageResource(R.drawable.ic_arrow_up);
                    expand();
                }else{
                    ivExpandCv.setImageResource(R.drawable.ic_arrow_down);
                    collapse();
                }
            }
        };
    }

    private void expand() {
        //set Visible
        rlAlertsBody.setVisibility(View.VISIBLE);


        mAnimator.start();
    }

    private void collapse() {
        int finalHeight = rlAlertsBody.getHeight();

        ValueAnimator mAnimator = slideAnimator(finalHeight, 0);

        mAnimator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationEnd(Animator animator) {
                //Height=0, but it set visibility to GONE
                rlAlertsBody.setVisibility(View.GONE);
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

                ViewGroup.LayoutParams layoutParams = rlAlertsBody.getLayoutParams();
                layoutParams.height = value;
                rlAlertsBody.setLayoutParams(layoutParams);
            }
        });
        return animator;
    }

    private void showSnackBar(String message,int length, final String actionButtontext)
    {
        snackbar = Snackbar.make(rlRootCv, message, length);
        snackbar.setActionTextColor(Color.RED);

        if (!actionButtontext.isEmpty() )
        {
            snackbar.setAction(actionButtontext, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    callVoteWS();

                }
            });
        }
        snackbar.show();

    }


}
