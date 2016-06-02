package org.grassroot.android.ui.activities;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.CardView;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.grassroot.android.R;
import org.grassroot.android.interfaces.NetworkErrorDialogListener;
import org.grassroot.android.services.GrassrootRestService;
import org.grassroot.android.services.model.GenericResponse;
import org.grassroot.android.slideDateTimePicker.SlideDateTimeListener;
import org.grassroot.android.slideDateTimePicker.SlideDateTimePicker;
import org.grassroot.android.ui.fragments.NetworkErrorDialogFragment;
import org.grassroot.android.utils.ErrorUtils;
import org.grassroot.android.utils.PreferenceUtils;
import org.grassroot.android.utils.UtilClass;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnTextChanged;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EditVoteActivity extends PortraitActivity {

    private static final String TAG = "EditVoteActivity";

    @BindView(R.id.vv_toolbar)
    Toolbar vvToolbar;
    @BindView(R.id.txt_toolbar)
    TextView txtToolbar;
    @BindView(R.id.rl_vv_main_layout)
    RelativeLayout rlVvMainLayout;
    @BindView(R.id.upper_card)
    CardView upperCard;
    @BindView(R.id.tv_counter)
    TextView tvCounter;
    @BindView(R.id.et_description)
    EditText et_description;
    @BindView(R.id.txt_ev_deadline)
    TextView txtEvDeadline;
    @BindView(R.id.datetimepicker)
    CardView datetimepicker;
    @BindView(R.id.rl_ev_root)
    RelativeLayout rlEvRoot;

    private Snackbar snackbar;
    private ProgressDialog progressDialog;
    private String description;
    private String deadline;
    private String closingTime;
    private String title;
    private String voteId;
    private String selectedDate;
    private SimpleDateFormat simpleDateFormat;
    private boolean dateselected = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_vote);
        ButterKnife.bind(this);
        setUpToolbar();
        if (getIntent() != null) {
            title = getIntent().getExtras().getString("title");
            description = getIntent().getExtras().getString("description");
            deadline = getIntent().getExtras().getString("deadline");
            voteId = getIntent().getExtras().getString("voteid");
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
        Log.e(TAG, "voteId is  " + voteId);

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

    @OnTextChanged(value = R.id.et_description, callback = OnTextChanged.Callback.AFTER_TEXT_CHANGED)
    public void afterTextChanged(Editable e) {
        tvCounter.setText("" + e.length() + "/" + "320");
    }

    @OnClick(R.id.datetimepicker)
    public void dateTimePickerListener() {
        showDateTimepPicker();
    }

    private SlideDateTimeListener listener = new SlideDateTimeListener() {

        @Override
        public void onDateTimeSet(Date date) {
            dateselected = true;
            Log.e("TAG", "date is " + date);
            simpleDateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm");
            txtEvDeadline.setText(simpleDateFormat.format(date));
            selectedDate = simpleDateFormat.format(date);
            SimpleDateFormat target_date = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm");
            closingTime = target_date.format(date);
            Log.e(TAG, "simpleDateFormat.format(date) is " + simpleDateFormat.format(date));
        }

        // Optional cancel listener
        @Override
        public void onDateTimeCancel() {
        }
    };


    private void showDateTimepPicker() {

        simpleDateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm");
        Date date1 = null;
        try {
            if (dateselected) {
                date1 = simpleDateFormat.parse(selectedDate);

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
                .setIndicatorColor(Color.parseColor("#207A33"))
                .build()
                .show();
    }

    @OnClick(R.id.bt_editVote)
    public void save() {
        updateVoteDetails();
    }

    private void showProgress() {

        progressDialog = new ProgressDialog(EditVoteActivity.this);
        progressDialog.setMessage("Please wait..");
        progressDialog.setIndeterminate(true);
        progressDialog.setCancelable(false);
        progressDialog.show();

    }

    private void hideProgress() {

        if ((progressDialog != null) && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }

    }

    private void updateVoteDetails() {

        String phoneNumber = PreferenceUtils.getuser_mobilenumber(this);
        String code = PreferenceUtils.getuser_token(this);
        description = et_description.getText().toString();
        showProgress();
        GrassrootRestService grassrootRestService = new GrassrootRestService(this);
        grassrootRestService.getApi().editVote(phoneNumber, code, voteId, title, description,
                closingTime + UtilClass.timeZone()).enqueue(new Callback<GenericResponse>() {
            @Override
            public void onResponse(Call<GenericResponse> call, Response<GenericResponse> response) {
                if (response.isSuccessful()) {
                    hideProgress();
                    Intent close = new Intent();
                    close.putExtra("description", et_description.getText().toString());
                    close.putExtra("deadline", txtEvDeadline.getText().toString());
                    setResult(1, close);
                    finish();
                }
            }

            @Override
            public void onFailure(Call<GenericResponse> call, Throwable t) {
                hideProgress();
                handleNetworkError(R.string.No_network);
            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        updateVoteDetails();
    }

    private void handleNetworkError(int message) {
        ErrorUtils.connectivityError(this, message,new NetworkErrorDialogListener() {
            @Override
            public void retryClicked() {
                updateVoteDetails();
            }
        });

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

    @Override
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
                    updateVoteDetails();
                }
            });

        }
        snackbar.show();
    }

    @Override
    public void onResume() {
        super.onResume();
    }


}
