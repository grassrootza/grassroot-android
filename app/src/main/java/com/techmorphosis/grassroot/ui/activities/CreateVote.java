package com.techmorphosis.grassroot.ui.activities;

import android.animation.Animator;
import android.animation.ValueAnimator;
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
import android.view.KeyEvent;
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

import com.techmorphosis.grassroot.R;
import com.techmorphosis.grassroot.models.VoteMemberModel;
import com.techmorphosis.grassroot.services.GrassrootRestService;
import com.techmorphosis.grassroot.services.model.GenericResponse;
import com.techmorphosis.grassroot.slideDateTimePicker.SlideDateTimeListener;
import com.techmorphosis.grassroot.slideDateTimePicker.SlideDateTimePicker;
import com.techmorphosis.grassroot.utils.Constant;
import com.techmorphosis.grassroot.utils.SettingPreference;
import com.techmorphosis.grassroot.utils.UtilClass;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CreateVote extends PortraitActivity {

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
    private ArrayList<String> membersuidList;

    private String closingTime;
    private Snackbar snackbar;
    private RelativeLayout rlRootCv;
    public  boolean receiver = false;

    private ArrayList<VoteMemberModel> voteMemberArrayList;
    private RelativeLayout rlNotifyBody;
    private TextView memberCount;
    private int membercounter=0;
    private StringBuilder stringBuilder;
    private TextView suffix;

    private SimpleDateFormat mFormatter1 = new SimpleDateFormat("MMMM dd yyyy HH:MM");
    private SimpleDateFormat mFormatter = new SimpleDateFormat("MMMM dd yyyy hh:mm aa");

    private GrassrootRestService grassrootRestService;
    private String groupId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_vote);
        if (getIntent().getExtras()!= null) {
            groupId = getIntent().getStringExtra(Constant.GROUPUID_FIELD);
            receiver=false;
        } else {
            groupId = "";
            receiver = true;
        }

        grassrootRestService = new GrassrootRestService(this);

        findAllViews();
        setUpToolbar();
        init();

    }

    private void init() {
        voteMemberArrayList = new ArrayList<>();
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

        rlNotifyBody = (RelativeLayout) findViewById(R.id.rl_notify_body);
        memberCount = (TextView) findViewById(R.id.member_count);


        suffix = (TextView) findViewById(R.id.suffix);

        txtDescCount.setText("0/320");
        txtTitleCount.setText("0/35");
        txtPostedname.setText("Posted by " + SettingPreference.getuser_name(CreateVote.this));


        setAllListner();
    }

    private void setAllListner() {


        rlAlertsHeader.setOnClickListener(expandableHeader());
        cvDatepicker.setOnClickListener(datetimepicker());
        btcallvote.setOnClickListener(button_callVote());
        rlNotifyBody.setOnClickListener(callVoteNotifyClass());

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


        et_title_cv.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent keyEvent) {
                Log.e(TAG, "keyEvent.getKeyCode()  " + keyEvent.getKeyCode());
                if ((keyEvent != null && (keyEvent.getKeyCode() == KeyEvent.KEYCODE_ENTER))) {

                    Log.e(TAG, "enter is pressed");
                    et_description_cv.requestFocus();
                  //  Toast.makeText(CreateVote.this, "enter is pressed", Toast.LENGTH_SHORT).show();
                    return true;

                } else {
                    //Toast.makeText(CreateVote.this, "els e", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "else other key");
                }
                return false;
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

    private View.OnClickListener callVoteNotifyClass() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent notifyactivity = new Intent(CreateVote.this, VoteNotifyMembers.class);
                notifyactivity.putParcelableArrayListExtra(Constant.VotedmemberList, voteMemberArrayList);
                startActivityForResult(notifyactivity, 1);

            }
        };
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

        if (TextUtils.isEmpty(et_title_cv.getText().toString().trim().replaceAll("[^\\sa-zA-Z0-9 ]", ""))) {
            showSnackBar(getString(R.string.nm_title_error_msg),snackbar.LENGTH_SHORT,"");
        } else {
           if (TextUtils.isEmpty(et_description_cv.getText().toString().trim().replaceAll("[^\\sa-zA-Z0-9 ]", ""))) {
               showSnackBar(getString(R.string.nm_description_error_msg),snackbar.LENGTH_SHORT,"");
           } else {
               if (TextUtils.isEmpty(closingTime)) {
                   showSnackBar(getString(R.string.nm_closingtime_msg),snackbar.LENGTH_SHORT,"");
               } else {
                   callVoteWS();
               }
            }

        }
    }

    private void  callVoteWS() {

        String phoneNumber = SettingPreference.getuser_mobilenumber(this);
        String code = SettingPreference.getuser_token(this);
        String title = et_title_cv.getText().toString();
        String description = et_description_cv.getText().toString();

        grassrootRestService.getApi().createVote(phoneNumber, code, groupId, title, description, closingTime, minute, null, true)
                .enqueue(new Callback<GenericResponse>() {
                    @Override
                    public void onResponse(Call<GenericResponse> call, Response<GenericResponse> response) {
                        Log.d(TAG, response.body().getMessage());
                        finish();
                        exitActivity();
                }

                    @Override
                    public void onFailure(Call<GenericResponse> call, Throwable t) {
                        Log.d(TAG, t.getMessage());
                    }
                });

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

                    notifyGroup = "true";
                    rlNotifyBody.setVisibility(View.GONE);

                } else {
                    notifyGroup = "false";
                    voteMemberArrayList.clear();
                    Intent notifyactivity = new Intent(CreateVote.this, VoteNotifyMembers.class);
                    notifyactivity.putParcelableArrayListExtra(Constant.VotedmemberList, voteMemberArrayList);
                    startActivityForResult(notifyactivity, 1);
                }


            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);


        if (resultCode == 1 && requestCode == 1) {

            this.voteMemberArrayList = data.getParcelableArrayListExtra(Constant.VotedmemberList);
            if (giveMembercount() > 0) {
                rlNotifyBody.setVisibility(View.VISIBLE);
                if (membercounter > 1) {

                    memberCount.setText(String.valueOf(membercounter));
                    suffix.setText(getString(R.string.cv_notify_member_suffix_two));
                } else {
                    memberCount.setText(String.valueOf(membercounter));
                    suffix.setText(getString(R.string.cv_notify_member_suffix_one));
                }

                if (membercounter == voteMemberArrayList.size()) {
                    swNotifyall.setChecked(true);
                } else {
                    swNotifyall.setChecked(false);
                }

            } else {

            }

        }
    }

    public int giveMembercount() {

         membercounter = 0;
         stringBuilder = new StringBuilder();
        for (int i = 0; i < voteMemberArrayList.size(); i++) {
            VoteMemberModel membercount = voteMemberArrayList.get(i);
            if (membercount.isSelected) {
                membercounter++;
                stringBuilder.append(membercount.memberUid);
                stringBuilder.append(",");
            }
        }

        if (stringBuilder.toString().equalsIgnoreCase("")) {
            members = "";
        } else {
            members = stringBuilder.toString().substring(0, stringBuilder.toString().length() - 1);
        }

        return membercounter;
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

                // MDdatepicker();
                SimpleDatePicker();
            }


        };
    }


    private void SimpleDatePicker() {

        simpleDateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm");
        Date date1 = null;
        try {
            if (dateselected) {
                date1 = (Date) simpleDateFormat.parse(selectedDate);

            } else {

                //today date but addition 10min
                final long ONE_MINUTE_IN_MILLIS=60000;//millisecs
                Calendar date = Calendar.getInstance();
                long t= date.getTimeInMillis();
                final Date afterAddingTenMins=new Date(t + (10 * ONE_MINUTE_IN_MILLIS));

                todaydateString = simpleDateFormat.format(afterAddingTenMins);

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

        new SlideDateTimePicker.Builder(getSupportFragmentManager())
                .setListener(listener)
                .setInitialDate(date1)
                        .setMinDate(new Date())
                        //.setMaxDate(maxDate)
                        //.setIs24HourTime(true)
                        //.setTheme(SlideDateTimePicker.HOLO_DARK)
                .setIndicatorColor(Color.parseColor("#207A33"))
                .build()
                .show();
    }

    private SlideDateTimeListener listener = new SlideDateTimeListener() {

        @Override
        public void onDateTimeSet(Date date)
        {
            Log.e("TAG", "date is " + date);

            dateselected = true;

            simpleDateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm");

            txtDeadline.setText(simpleDateFormat.format(date));

            selectedDate = simpleDateFormat.format(date);

            SimpleDateFormat target_date = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm");

            closingTime = target_date.format(date);

            Log.e(TAG,"simpleDateFormat.format(date) is " + simpleDateFormat.format(date));


        }

        // Optional cancel listener
        @Override
        public void onDateTimeCancel()
        {
/*
            Toast.makeText(CreateVote.this,
                    "Canceled", Toast.LENGTH_SHORT).show();
*/
        }
    };


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

    public  String timeZone()
    {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT"), Locale.getDefault());
        String   timeZone = new SimpleDateFormat("Z").format(calendar.getTime());
        return timeZone.substring(0, 3) + ":"+ timeZone.substring(3, 5);
    }

}
