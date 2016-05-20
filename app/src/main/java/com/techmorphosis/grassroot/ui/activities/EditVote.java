package com.techmorphosis.grassroot.ui.activities;

import android.app.ProgressDialog;
import android.content.DialogInterface;
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

import com.techmorphosis.grassroot.R;
import com.techmorphosis.grassroot.services.GrassrootRestService;
import com.techmorphosis.grassroot.services.model.GenericResponse;
import com.techmorphosis.grassroot.utils.SettingPreference;
import com.wdullaer.materialdatetimepicker.date.DatePickerDialog;
import com.wdullaer.materialdatetimepicker.time.RadialPickerLayout;
import com.wdullaer.materialdatetimepicker.time.TimePickerDialog;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EditVote extends PortraitActivity  implements  TimePickerDialog.OnTimeSetListener,
        DatePickerDialog.OnDateSetListener {

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
    private TextView txtEvDeadline;
    private String voteid;


    ProgressDialog progressDialog;
    private int error_flag;
    private static final String TAG = "EditVote";
    private RelativeLayout rlEvRoot;

    private Snackbar snackbar;
    private CardView datetimepicker;

    private int year, month, day, hour, minute,second;
    private String title;

    String selectedDate;
    private SimpleDateFormat simpleDateFormat;
    private boolean dateselected=false;
    private GrassrootRestService grassrootRestService;
    Calendar now;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_vote);
        grassrootRestService =  new GrassrootRestService(this);
        findAllViews();
        setUpToolbar();
        if (getIntent()!=null)
        {
            title = getIntent().getExtras().getString("title");
            description = getIntent().getExtras().getString("description");
            deadline = getIntent().getExtras().getString("deadline");
            voteid = getIntent().getExtras().getString("voteid");
        }
        et_description.setText(description);
        try {
            txtEvDeadline.setText(deadline);
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG,"e is " + e.getMessage()) ;
        }
        Log.e(TAG, "voteid is  " + voteid);

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


                    simpleDateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm");
                    Date date1 = null;
                    try {
                        if (dateselected)
                        {
                            date1 = (Date) simpleDateFormat.parse(selectedDate);

                        }
                        else
                        {

                            try {
                                date1 = (Date) simpleDateFormat.parse(deadline);
                            } catch (Exception e) {
                                e.printStackTrace();
                                Log.e(TAG," e is " + e.getMessage());
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

    private View.OnClickListener button_save() {
            return new View.OnClickListener() {
                @Override
                public void onClick(View v)
                {

                    EditVoteWS();

                }

            };
    }

    private void EditVoteWS()
    {

        preExecute();
        doInbackground();
    }

    private void preExecute()
    {
        error_flag = 0;

        progressDialog = new ProgressDialog(EditVote.this);
        progressDialog.setMessage("Please wait..");
        progressDialog.setIndeterminate(true);
        progressDialog.setCancelable(false);
        progressDialog.show();

    }


    private void doInbackground()
    {

       String phoneNumber = SettingPreference.getuser_mobilenumber(this);
        String code = SettingPreference.getuser_token(this);

       grassrootRestService.getApi().editVote(phoneNumber,code,voteid,title,description,deadline).
               enqueue(new Callback<GenericResponse>() {
                   @Override
                   public void onResponse(Call<GenericResponse> call, Response<GenericResponse> response) {
                       if (response.isSuccessful()) {
                         postExecute();
                       }
                   }
                   @Override
                   public void onFailure(Call<GenericResponse> call, Throwable t) {
                       Log.d(TAG, t.getMessage());

                    //   rlVvMainLayout.setVisibility(View.GONE);
                       if ((progressDialog != null) && progressDialog.isShowing()) {
                           progressDialog.dismiss();
                       }

                       //  ErrorUtils.handleNetworkError(ViewVote.this, errorLayout, t);
                   }
               });


    /*    NetworkCall networkCall = new NetworkCall
                (
                        EditVote.this,
                        new ResponseListenerVolley() {
                            @Override
                            public void onSuccess(String s)
                            {
                                error_flag = 0 ;
                                postExecute(s);
                            }
                        },
                        new ErrorListenerVolley() {
                            @Override
                            public void onError(VolleyError volleyError) {

                                if ((progressDialog != null) && progressDialog.isShowing()) {
                                    progressDialog.dismiss();
                                }

                                if (volleyError instanceof NoConnectionError || volleyError instanceof TimeoutError)
                                {
                                    error_flag = 1;
                                    postExecute("");
                                }
                                else if (volleyError instanceof ServerError) {

                                    String responsebody = null;
                                    try {
                                        responsebody = new String(volleyError.networkResponse.data,"utf-8");
                                        Log.e(TAG, "responsebody is " + responsebody);


                                        try {
                                            JSONObject jsonObject = new JSONObject(responsebody);
                                            error_flag=0;
                                            postExecute(responsebody);
                                        } catch (JSONException e) {
                                            e.printStackTrace();
                                            error_flag=5;
                                            postExecute("");
                                        }
                                        ;

                                    } catch (UnsupportedEncodingException e)
                                    {
                                        e.printStackTrace();
                                        error_flag=5;
                                        postExecute("");
                                    }

                                     }
                               else
                                {//unKnown error
                                    error_flag=5;
                                    postExecute("");
                                }


                            }
                        },
                        AllLinsks.EditVote + voteid + "/" + SettingPreffrence.getPREF_Phone_Token(EditVote.this),
                        "",
                        false
                );
        HashMap<String,String> hashMap= new HashMap<>();
        hashMap.put("description", et_description.getText().toString());
        hashMap.put("closingTime", deadline);
        hashMap.put("title", title);
        networkCall.makeStringRequest_POST(hashMap);

        Log.e(TAG, "description is " + et_description.getText().toString());
        Log.e(TAG, "closingTime is " + deadline);
        Log.e(TAG,"title is "+ title);*/
    }


    private void postExecute()
    {
        Log.e(TAG, "  **** error **** " + error_flag);

        if ((progressDialog != null) && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
            Intent close= new Intent();
            close.putExtra("description",et_description.getText().toString());
            close.putExtra("deadline",txtEvDeadline.getText().toString());
            setResult(1, close);
            finish();

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

        finish();
    }

    public  void showSnackBar(String message, int length, String actionButtontext)
    {

        snackbar=Snackbar.make(rlEvRoot, message, length);
        snackbar.setActionTextColor(Color.RED);

        if (!actionButtontext.isEmpty())
        {
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
        TimePickerDialog tpd = (TimePickerDialog) getFragmentManager().findFragmentByTag("Timepickerdialog");
        DatePickerDialog dpd = (DatePickerDialog) getFragmentManager().findFragmentByTag("Datepickerdialog");

        if(tpd != null) tpd.setOnTimeSetListener(this);
        if(dpd != null) dpd.setOnDateSetListener(this);
    }


    @Override
    public void onTimeSet(RadialPickerLayout view, int hourOfDay, int minutes, int seconds) {
        /*String hourString = hourOfDay < 10 ? "0"+hourOfDay : ""+hourOfDay;
        String minuteString = minute < 10 ? "0"+minute : ""+minute;
        String secondString = second < 10 ? "0"+second : ""+second;*/
        hour= hourOfDay;
        minute = minutes;
        second = seconds;
        dateselected =true;

        selectedDate=selectedDate+" "+hourOfDay+":"+minutes+":"+seconds;


        txtEvDeadline.setText( (day < 10 ? "0"+day : day)+"-"+
                (month < 10 ? "0"+(month) : month)+"-"+
                year +"  "+
                (hour < 10 ? "0"+hour : hour)+":"+
                (minute < 10 ? "0"+minute : minute));

       deadline=year+"-"+
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

}
