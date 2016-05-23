package com.techmorphosis.grassroot.ui.activities;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputLayout;
import android.support.v7.widget.CardView;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
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
import com.techmorphosis.grassroot.slideDateTimePicker.SlideDateTimeListener;
import com.techmorphosis.grassroot.slideDateTimePicker.SlideDateTimePicker;
import com.techmorphosis.grassroot.utils.SettingPreffrence;
import com.techmorphosis.grassroot.utils.UtilClass;
import com.techmorphosis.grassroot.utils.listener.ErrorListenerVolley;
import com.techmorphosis.grassroot.utils.listener.ResponseListenerVolley;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

public class EditVote extends PortraitActivity{

    private Toolbar vvToolbar;
    private TextView txtToolbar;
    private RelativeLayout rlVvMainLayout;
    private ScrollView scrollView;
    private CardView upperCard;
    private TextInputLayout et2;
    private TextView tvCounter;
    private String description;
    private EditText et_description;
    private String deadline;
    private String closingTime;
    private TextView txtEvDeadline;
    private String voteid;

    ProgressDialog progressDialog;
    private int error_flag;
    private static final String TAG = "EditVote";
    private RelativeLayout rlEvRoot;

    private Snackbar snackbar;
    private CardView datetimepicker;

    private int year, month, day, hour, minute, second;
    private String title;

    String selectedDate;
    private SimpleDateFormat simpleDateFormat;
    private boolean dateselected = false;
    Calendar now;
    private UtilClass utilClass;
    private SimpleDateFormat mFormatter1 = new SimpleDateFormat("MMMM dd yyyy HH:MM");
    private SimpleDateFormat mFormatter = new SimpleDateFormat("MMMM dd yyyy hh:mm aa");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_vote);

        utilClass = new UtilClass();

        findAllViews();
        setUpToolbar();
        if (getIntent() != null) {
            title = getIntent().getExtras().getString("title");
            description = getIntent().getExtras().getString("description");
            deadline = getIntent().getExtras().getString("deadline");
            voteid = getIntent().getExtras().getString("voteid");
        }
        et_description.setText(description);
        closingTime = deadline;
        try {
            txtEvDeadline.setText(deadline);
            Log.e(TAG, "DateConversion " + DateConversion(deadline));
            closingTime = DateConversion(deadline);
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "e is " + e.getMessage());
        }
        Log.e(TAG, "voteid is  " + voteid);

    }

    private String DateConversion(String inputdate) {

        SimpleDateFormat original = new SimpleDateFormat("dd-MM-yyyy HH:mm");
        String target_string = null;
        try {
            Date date_original = original.parse(inputdate);
            SimpleDateFormat target_date = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm");
            target_string = target_date.format(date_original);

        } catch (ParseException e) {
            e.printStackTrace();

        }

        return target_string;

    }

    private void findAllViews() {
        rlEvRoot = (RelativeLayout) findViewById(R.id.rl_ev_root);
        vvToolbar = (Toolbar) findViewById(R.id.vv_toolbar);
        txtToolbar = (TextView) findViewById(R.id.txt_toolbar);

        rlVvMainLayout = (RelativeLayout) findViewById(R.id.rl_vv_main_layout);

        upperCard = (CardView) findViewById(R.id.upper_card);
        datetimepicker = (CardView) findViewById(R.id.datetimepicker);

        txtEvDeadline = (TextView) findViewById(R.id.txt_ev_deadline);
        et_description = (EditText) findViewById(R.id.et_description);

        tvCounter = (TextView) findViewById(R.id.tv_counter);
        findViewById(R.id.bt_editVote).setOnClickListener(button_save());
        datetimepicker.setOnClickListener(datetimepicker());

        et_description.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                //   Log.e(TAG,"before count is " + count);
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Log.e(TAG,"onTextChanged count is " + count);

            }

            @Override
            public void afterTextChanged(Editable s) {
                //  Log.e(TAG,"afterTextChanged length  is " + s.length());
                tvCounter.setText("" + s.length() + "/" + "320");

            }
        });

    }

    private View.OnClickListener datetimepicker() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {


           /*     simpleDateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm");
                Date date1 = null;
                try {
                    if (dateselected) {
                        date1 = (Date) simpleDateFormat.parse(selectedDate);

                    } else {

                        try {
                            date1 = (Date) simpleDateFormat.parse(deadline);
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
                        EditVote.this,
                        now.get(Calendar.YEAR),
                        now.get(Calendar.MONTH),
                        now.get(Calendar.DAY_OF_MONTH)
                );
                dpd.vibrate(true);
                dpd.dismissOnPause(false);
                dpd.setAccentColor(getResources().getColor(R.color.primaryColor));
                dpd.setMinDate(today);
                dpd.show(getFragmentManager(), "Datepickerdialog");*/


                showDateTimepPicker();

            }


        };
    }

    private SlideDateTimeListener listener = new SlideDateTimeListener() {

        @Override
        public void onDateTimeSet(Date date)
        {
            dateselected = true;

            Log.e("TAG", "date is " + date);

           /* Toast.makeText(EditVote.this, mFormatter.format(date), Toast.LENGTH_SHORT).show();
            Toast.makeText(EditVote.this, mFormatter1.format(date), Toast.LENGTH_SHORT).show();*/

            simpleDateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm");

            txtEvDeadline.setText(simpleDateFormat.format(date));

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
            Toast.makeText(EditVote.this,
                    "Canceled", Toast.LENGTH_SHORT).show();
*/
        }
    };


    private void showDateTimepPicker() {

        simpleDateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm");
        Date date1 = null;
        try {
            if (dateselected) {
                date1 = (Date) simpleDateFormat.parse(selectedDate);

            } else {

                try {
                    date1 = (Date) simpleDateFormat.parse(deadline);
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

/*
    public String convertW3CTODeviceTimeZone(String strDate) throws Exception {
        SimpleDateFormat simpleDateFormatW3C = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        Date dateServer = simpleDateFormatW3C.parse(strDate);

        TimeZone deviceTimeZone = TimeZone.getDefault();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
        simpleDateFormat.setTimeZone(deviceTimeZone);

        String formattedDate = simpleDateFormat.format(dateServer);
        // long timeMilliness=new Date(formattedDate).getTime();
        return formattedDate;
    }
*/

    private View.OnClickListener button_save() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                EditVoteWS();

            }

        };
    }

    private void EditVoteWS() {

        preExecute();
        doInbackground();
    }

    private void preExecute() {
        error_flag = 0;

        progressDialog = new ProgressDialog(EditVote.this);
        progressDialog.setMessage("Please wait..");
        progressDialog.setIndeterminate(true);
        progressDialog.setCancelable(false);
        progressDialog.show();

    }


    private void doInbackground() {

        NetworkCall networkCall = new NetworkCall
                (
                        EditVote.this,
                        new ResponseListenerVolley() {
                            @Override
                            public void onSuccess(String s) {
                                error_flag = 0;
                                postExecute(s);
                            }
                        },
                        new ErrorListenerVolley() {
                            @Override
                            public void onError(VolleyError volleyError) {

                                if ((progressDialog != null) && progressDialog.isShowing()) {
                                    progressDialog.dismiss();
                                }

                                if (volleyError instanceof NoConnectionError || volleyError instanceof TimeoutError) {
                                    error_flag = 1;
                                    postExecute("");
                                } else if (volleyError instanceof ServerError) {

                                    String responsebody = null;
                                    try {
                                        responsebody = new String(volleyError.networkResponse.data, "utf-8");
                                        Log.e(TAG, "responsebody is " + responsebody);


                                        try {
                                            JSONObject jsonObject = new JSONObject(responsebody);
                                            error_flag = 0;
                                            postExecute(responsebody);
                                        } catch (JSONException e) {
                                            e.printStackTrace();
                                            error_flag = 5;
                                            postExecute("");
                                        }
                                        ;

                                    } catch (UnsupportedEncodingException e) {
                                        e.printStackTrace();
                                        error_flag = 5;
                                        postExecute("");
                                    }

                                } else if (volleyError instanceof AuthFailureError) {//unKnown error
                                    error_flag = 4;
                                    postExecute("");
                                } else {
                                    error_flag = 5;
                                    postExecute("");
                                }


                            }
                        },
                        AllLinsks.EditVote + voteid + "/" + SettingPreffrence.getPREF_Phone_Token(EditVote.this),
                        "",
                        false
                );
        HashMap<String, String> hashMap = new HashMap<>();
        hashMap.put("description", et_description.getText().toString());
        hashMap.put("closingTime", closingTime + utilClass.timeZone());
        hashMap.put("title", title);
        networkCall.makeStringRequest_POST(hashMap);

        Log.e(TAG, "description is " + et_description.getText().toString());
        Log.e(TAG, "closingTime is " + closingTime + utilClass.timeZone());
        Log.e(TAG, "title is " + title);
    }


    private void postExecute(String response) {
        Log.e(TAG, "  **** error **** " + error_flag);

        if ((progressDialog != null) && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }

        if (error_flag == 1) {//no Internet
            //snackbar
            showSnackBar(getString(R.string.No_network), Snackbar.LENGTH_INDEFINITE, getString(R.string.Retry));

        } else if (error_flag == 4) {//Authentication error
            //snackbar
            showSnackBar(getString(R.string.INVALID_TOKEN), Snackbar.LENGTH_SHORT, "");

        } else if (error_flag == 5) {//catch error
            //snackbar
            showSnackBar(getString(R.string.Unknown_error), Snackbar.LENGTH_SHORT, "");

        } else if (error_flag == 0) {


            Intent close = new Intent();
            close.putExtra("description", et_description.getText().toString());
            close.putExtra("deadline", txtEvDeadline.getText().toString());
            setResult(1, close);
            finish();
        }

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

    public void onBackPressed() {

        finish();
    }

    public void showSnackBar(String message, int length, String actionButtontext) {

        snackbar = Snackbar.make(rlEvRoot, message, length);
        snackbar.setActionTextColor(Color.RED);

        if (!actionButtontext.isEmpty()) {
            snackbar.setAction(actionButtontext, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    EditVoteWS();
                }
            });

        }
        snackbar.show();
    }


    @Override
    public void onResume() {
        super.onResume();
     /*   TimePickerDialog tpd = (TimePickerDialog) getFragmentManager().findFragmentByTag("Timepickerdialog");
        DatePickerDialog dpd = (DatePickerDialog) getFragmentManager().findFragmentByTag("Datepickerdialog");

        if (tpd != null) tpd.setOnTimeSetListener(this);
        if (dpd != null) dpd.setOnDateSetListener(this);*/

    }


/*
    @Override
    public void onTimeSet(RadialPickerLayout view, int hourOfDay, int minutes, int seconds) {
        */
/*String hourString = hourOfDay < 10 ? "0"+hourOfDay : ""+hourOfDay;
        String minuteString = minute < 10 ? "0"+minute : ""+minute;
        String secondString = second < 10 ? "0"+second : ""+second;*//*

        hour = hourOfDay;
        minute = minutes;
        second = seconds;
        dateselected = true;

        selectedDate = selectedDate + " " + hourOfDay + ":" + minutes + ":" + seconds;


        txtEvDeadline.setText((day < 10 ? "0" + day : day) + "-" +
                (month < 10 ? "0" + (month) : month) + "-" +
                year + "  " +
                (hour < 10 ? "0" + hour : hour) + ":" +
                (minute < 10 ? "0" + minute : minute));

        closingTime = year + "-" +
                (month < 10 ? "0" + (month) : month) + "-" +
                (day < 10 ? "0" + day : day) + "T" +
                (hour < 10 ? "0" + hour : hour) + ":" +
                (minute < 10 ? "0" + minute : minute);


    }
*/


/*
    @Override
    public void onDateSet(DatePickerDialog view, int years, int monthOfYear, int dayOfMonth) {
        // String date = "You picked the following date: "+dayOfMonth+"/"+(++monthOfYear)+"/"+year;
        year = years;
        month = ++monthOfYear;
        day = dayOfMonth;

        selectedDate = day + "-" + month + "-" + year;
        dateselected = true;

        */
/*Calendar now = Calendar.getInstance();*//*

        TimePickerDialog tpd = TimePickerDialog.newInstance(
                EditVote.this,
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
*/

}
