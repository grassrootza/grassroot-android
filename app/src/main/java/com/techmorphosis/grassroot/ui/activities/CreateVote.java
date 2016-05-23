package com.techmorphosis.grassroot.ui.activities;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.techmorphosis.grassroot.R;
import com.techmorphosis.grassroot.models.VoteMemberModel;
import com.techmorphosis.grassroot.services.GrassrootRestService;
import com.techmorphosis.grassroot.services.model.GenericResponse;
import com.techmorphosis.grassroot.slideDateTimePicker.SlideDateTimeListener;
import com.techmorphosis.grassroot.slideDateTimePicker.SlideDateTimePicker;
import com.techmorphosis.grassroot.utils.Constant;
import com.techmorphosis.grassroot.utils.ErrorUtils;
import com.techmorphosis.grassroot.utils.SettingPreference;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnCheckedChanged;
import butterknife.OnClick;
import butterknife.OnEditorAction;
import butterknife.OnTextChanged;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CreateVote extends PortraitActivity {

    private static final String TAG = CreateVote.class.getCanonicalName();

    @BindView(R.id.rl_root_cv)
    RelativeLayout rlRootCv;
    @BindView(R.id.tlb_cv)
    Toolbar tlbCv;

    @BindView(R.id.et_title_cv)
    EditText et_title_cv;
    @BindView(R.id.txt_title_count)
    TextView txtTitleCount;
    @BindView(R.id.txt_postedname)
    TextView txtPostedname;
    @BindView(R.id.et_description_cv)
    EditText et_description_cv;
    @BindView(R.id.txt_desc_count)
    TextView txtDescCount;
    @BindView(R.id.txt_deadline)
    TextView txtDeadline;

    @BindView(R.id.rl_alerts_body)
    RelativeLayout rlAlertsBody;
    @BindView(R.id.iv_expand_alert)
    ImageView ivExpandCv;

    @BindView(R.id.sw_one_day)
    SwitchCompat swOneDay;
    @BindView(R.id.sw_half_day)
    SwitchCompat swHalfDay;
    @BindView(R.id.sw_one_hour)
    SwitchCompat swOneHour;
    @BindView(R.id.sw_immediate)
    SwitchCompat swImmediate;

    @BindView(R.id.sw_notifyall)
    SwitchCompat swNotifyall;
    @BindView(R.id.rl_notify_body)
    RelativeLayout rlNotifyBody;
    @BindView(R.id.member_count)
    TextView memberCount;

    @BindView(R.id.suffix)
    TextView suffix;

    private ValueAnimator mAnimator;

    private Date selectedDate;
    private boolean dateSelected = false;
    private static SimpleDateFormat displayFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm");
    private SlideDateTimeListener listener;

    private List<VoteMemberModel> voteMemberArrayList;
    private boolean notifyWholeGroup;
    private int notificationReminderSetting; // todo: enum! I mean, string as int, which this was ...

    private GrassrootRestService grassrootRestService;
    private String groupId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_vote);
        ButterKnife.bind(this);

        init();
        setUpViews();
        setUpToolbar();
    }

    private void init() {
        if (getIntent().getExtras()!= null) {
            groupId = getIntent().getStringExtra(Constant.GROUPUID_FIELD);
        } else {
            throw new UnsupportedOperationException("Cannot create vote without group ID");
        }
        grassrootRestService = new GrassrootRestService(this);
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

    private void setUpViews() {

        txtDescCount.setText("0/160"); // todo : convert to constant
        txtTitleCount.setText("0/35");
        txtPostedname.setText("Posted by " + SettingPreference.getuser_name(CreateVote.this));

        swImmediate.setChecked(true);
        swNotifyall.setChecked(true);
        notifyWholeGroup = true;

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

        listener = new SlideDateTimeListener() {
            @Override
            public void onDateTimeSet(Date date) {
                Log.e("TAG", "date is " + date);
                dateSelected = true;
                selectedDate = date;
                txtDeadline.setText(displayFormat.format(date));
            }
        };
    }

    @OnEditorAction(R.id.et_title_cv)
    public boolean onKeyPressedInTitleEdit(KeyEvent keyEvent) {
        if ((keyEvent != null && (keyEvent.getKeyCode() == KeyEvent.KEYCODE_ENTER))) {
            et_description_cv.requestFocus();
            return true;
        } else {
            return false;
        }
    }

    @OnTextChanged(value = R.id.et_title_cv, callback = OnTextChanged.Callback.AFTER_TEXT_CHANGED)
    public void updateTitleTextCounter(Editable s) {
        txtTitleCount.setText(s.length() + "/35");

    }

    @OnTextChanged(value = R.id.et_description_cv, callback = OnTextChanged.Callback.AFTER_TEXT_CHANGED)
    public void updateDescriptionTextCounter(Editable s) {
        txtDescCount.setText(s.length() + "/160"); // todo: make the denominator a constant ...
    }

    @OnClick(R.id.rl_notify_body)
    public void onNotifyClicked() {
        Intent notifyactivity = new Intent(CreateVote.this, VoteNotifyMembers.class);
        notifyactivity.putParcelableArrayListExtra(Constant.VotedmemberList, (ArrayList) voteMemberArrayList);
        startActivityForResult(notifyactivity, 1);
    }

    @OnCheckedChanged(R.id.sw_one_day)
    public void oneDaySelected(boolean isChecked) {
        if (isChecked) {
            notificationReminderSetting = 3;
            toggleSwitches(swOneDay);
        }
    }

    @OnCheckedChanged(R.id.sw_half_day)
    public void halfDaySelected(boolean isChecked) {
        if (isChecked) {
            notificationReminderSetting = 2;
            toggleSwitches(swHalfDay);
        }
    }

    @OnCheckedChanged(R.id.sw_one_hour)
    public void oneHourSelected(boolean isChecked) {
        if (isChecked) {
            notificationReminderSetting = 1;
            toggleSwitches(swOneHour);
        }
    }

    @OnCheckedChanged(R.id.sw_immediate)
    public void immediateSelected(boolean isChecked) {
        if (isChecked) {
            notificationReminderSetting = 0;
            toggleSwitches(swImmediate);
        }
    }

    private void toggleSwitches(SwitchCompat selectedSwitch) {
        swOneDay.setChecked(selectedSwitch.equals(swOneDay));
        swHalfDay.setChecked(selectedSwitch.equals(swHalfDay));
        swOneHour.setChecked(selectedSwitch.equals(swOneHour));
        swImmediate.setChecked(selectedSwitch.equals(swImmediate));
    }

    @OnClick(R.id.bt_call_vote)
    public void validateFormAndSubmit() {

        if (TextUtils.isEmpty(et_title_cv.getText().toString().trim().replaceAll("[^\\sa-zA-Z0-9 ]", ""))) {
            showSnackBar(getString(R.string.nm_title_error_msg),Snackbar.LENGTH_SHORT,"");
        } else {
           if (selectedDate == null) {
               showSnackBar(getString(R.string.nm_closingtime_msg),Snackbar.LENGTH_SHORT,"");
           } else {
               callVoteWS();
           }
        }
    }

    private void  callVoteWS() {

        // todo : convert member list into list of UIDs
        final String phoneNumber = SettingPreference.getuser_mobilenumber(this);
        final String code = SettingPreference.getuser_token(this);
        final String title = et_title_cv.getText().toString();
        final String description = et_description_cv.getText().toString(); // todo: make sure can handle empty descs
        final String closingTime = Constant.isoDateTimeSDF.format(selectedDate);

        grassrootRestService.getApi()
                .createVote(phoneNumber, code, groupId, title, description, closingTime, notificationReminderSetting, null, false)
                .enqueue(new Callback<GenericResponse>() {
                    @Override
                    public void onResponse(Call<GenericResponse> call, Response<GenericResponse> response) {
                        Log.d(TAG, response.body().getMessage());
                        finish();
                        exitActivity(); // todo : show a snackbar or something
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

    @OnCheckedChanged(R.id.sw_notifyall)
    public void toggleNotifyAllMembers(boolean isChecked) {
        if (isChecked) {
            notifyWholeGroup = true;
            rlNotifyBody.setVisibility(View.GONE);
        } else {
            notifyWholeGroup = false;
            voteMemberArrayList.clear(); // wtf, vs two lines later
            Intent notifyactivity = new Intent(CreateVote.this, VoteNotifyMembers.class);
            notifyactivity.putParcelableArrayListExtra(Constant.VotedmemberList, (ArrayList) voteMemberArrayList);
            startActivityForResult(notifyactivity, Constant.activitySelectGroupMembers);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && requestCode == Constant.activitySelectGroupMembers) {

            voteMemberArrayList = data.getParcelableArrayListExtra(Constant.VotedmemberList);
            int selectedMemberCount = calculateMemberNumber();

            if (selectedMemberCount > 0) {
                rlNotifyBody.setVisibility(View.VISIBLE);
                memberCount.setText(String.valueOf(selectedMemberCount));
                suffix.setText(selectedMemberCount > 1 ? getString(R.string.cv_notify_member_suffix_two) :
                        getString(R.string.cv_notify_member_suffix_one));
                swNotifyall.setChecked(selectedMemberCount == voteMemberArrayList.size());
            }
        }
    }

    public int calculateMemberNumber() {
        int count = 0;
        for (VoteMemberModel m : voteMemberArrayList) {
            if (m.isSelected) {
                count++;
            }
        }
        return count;
    }

    @OnClick(R.id.cv_datepicker)
    public void simpleDatePicker() {

        Date dateToPass;
        if (dateSelected) {
            dateToPass = selectedDate;
        } else {
            final long TEN_MINUTES=10 * 60 * 1000; //millisecs
            long t = SystemClock.currentThreadTimeMillis();
            dateToPass = new Date(t + (TEN_MINUTES));
        }

        new SlideDateTimePicker.Builder(getSupportFragmentManager())
                .setListener(listener)
                .setInitialDate(dateToPass)
                .setMinDate(new Date())
                .setIndicatorColor(Color.parseColor("#207A33"))
                .build()
                .show();
    }

    @OnClick(R.id.rl_alerts_header)
    public void expandableHeader() {
        if (rlAlertsBody.getVisibility()==View.GONE){
            ivExpandCv.setImageResource(R.drawable.ic_arrow_up);
            rlAlertsBody.setVisibility(View.VISIBLE);
            mAnimator.start();
        } else {
            ivExpandCv.setImageResource(R.drawable.ic_arrow_down);
            collapse();
        }
    }

    private void collapse() {
        int finalHeight = rlAlertsBody.getHeight();

        ValueAnimator mAnimator = slideAnimator(finalHeight, 0);

        // must be a more efficient way to do this
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

    private void showSnackBar(String message, int length, final String actionButtontext) {
        if (actionButtontext == null || actionButtontext.isEmpty()) {
            ErrorUtils.showSnackBar(rlRootCv, message, length, null, null);
        } else {
            ErrorUtils.showSnackBar(rlRootCv, message, length, actionButtontext, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    callVoteWS();
                }
            });
        }
    }
}