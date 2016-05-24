package com.techmorphosis.grassroot.ui.activities;

import android.animation.ValueAnimator;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputEditText;
import android.support.v7.widget.SwitchCompat;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.techmorphosis.grassroot.R;
import com.techmorphosis.grassroot.services.GrassrootRestService;
import com.techmorphosis.grassroot.services.model.GenericResponse;
import com.techmorphosis.grassroot.services.model.Member;
import com.techmorphosis.grassroot.slideDateTimePicker.SlideDateTimeListener;
import com.techmorphosis.grassroot.slideDateTimePicker.SlideDateTimePicker;
import com.techmorphosis.grassroot.utils.Constant;
import com.techmorphosis.grassroot.utils.ErrorUtils;
import com.techmorphosis.grassroot.utils.SettingPreference;
import com.techmorphosis.grassroot.utils.UtilClass;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Set;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnEditorAction;
import butterknife.OnTextChanged;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Created by luke on 2016/05/24.
 */
public class CreateMeetingActivity extends PortraitActivity {

    private static final String TAG = CreateMeetingActivity.class.getCanonicalName();

    private GrassrootRestService grassrootRestService;

    private String groupUid;
    private boolean notifyWholeGroup;
    private Set<Member> assignedMembers;

    private Date meetingStartDateTime;
    private SlideDateTimeListener dateTimeListener;
    private static final SimpleDateFormat displayFormat = new SimpleDateFormat("HH:mm dd-MM");

    @BindView(R.id.cmtg_rl_root)
    RelativeLayout rlRoot;
    @BindView(R.id.cmtg_reminder_body)
    RelativeLayout rlReminderBody;
    private ValueAnimator reminderSlideOutAnimator;

    @BindView(R.id.cmtg_et_title)
    TextInputEditText etTitleInput;
    @BindView(R.id.cmtg_et_location)
    TextInputEditText etLocationInput;
    @BindView(R.id.cmtg_et_description)
    TextInputEditText etDescriptionInput;

    @BindView(R.id.cmtg_subject_count)
    TextView subjectCharCounter;
    @BindView(R.id.cmtg_desc_count)
    TextView descriptionCharCounter;
    @BindView(R.id.cmtg_location_count)
    TextView locationCharCounter;
    @BindView(R.id.cmtg_txt_deadline)
    TextView displayedDateTime;

    @BindView(R.id.sw_notifyall)
    SwitchCompat notifyAll;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_meeting);
        ButterKnife.bind(this);

        init();
        setUpViews();;
    }

    private void init() {
        Bundle b = getIntent().getExtras();
        if (b == null) {
            throw new UnsupportedOperationException("Error! Activity must be called with bundle");
        }

        groupUid = b.getString(Constant.GROUPUID_FIELD);
        grassrootRestService = new GrassrootRestService(this);
        notifyWholeGroup = true;
    }

    private void setUpViews() {

        rlReminderBody.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                rlReminderBody.getViewTreeObserver().removeOnPreDrawListener(this);
                rlReminderBody.setVisibility(View.GONE);

                // todo : double check these for inefficiencies
                final int widthSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
                final int heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
                rlReminderBody.measure(widthSpec, heightSpec);

                reminderSlideOutAnimator = setUpAnimator(0, rlReminderBody.getMeasuredHeight());
                return true;
            }
        });

        dateTimeListener = new SlideDateTimeListener() {
            @Override
            public void onDateTimeSet(Date date) {
                meetingStartDateTime = date;
                displayedDateTime.setText(displayFormat.format(date));
            }
        };
    }

    private ValueAnimator setUpAnimator(int start, int end) {
        ValueAnimator animator = ValueAnimator.ofInt(start, end);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                ViewGroup.LayoutParams layoutParams = rlReminderBody.getLayoutParams();
                layoutParams.height = (Integer) valueAnimator.getAnimatedValue();;
                rlReminderBody.setLayoutParams(layoutParams);
            }
        });
        return animator;
    }

    @OnTextChanged(R.id.cmtg_et_title)
    public void changeCharCounter(CharSequence s) {
        subjectCharCounter.setText(s.length() + " / 35"); // todo : externalize
    }

    @OnEditorAction(R.id.cmtg_et_title)
    public boolean titleEnterPressed(KeyEvent keyEvent) {
        if ((keyEvent != null && (keyEvent.getKeyCode() == KeyEvent.KEYCODE_ENTER))) {
            etLocationInput.requestFocus();
            return true;
        } else {
            return false;
        }
    }

    @OnEditorAction(R.id.cmtg_et_location)
    public boolean locationEnterPressed(KeyEvent keyEvent) {
        if (keyEvent != null && keyEvent.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
            etDescriptionInput.requestFocus();
            return true;
        } else {
            return false;
        }
    }

    @OnTextChanged(R.id.cmtg_et_location)
    public void changeLocCharCounter(CharSequence s) {
        locationCharCounter.setText(s.length() + " / 35");
    }

    @OnTextChanged(R.id.cmtg_et_description)
    public void changeDescCounter(CharSequence s) {
        descriptionCharCounter.setText(s.length() + " / 250"); // todo : externalize
    }

    @OnClick(R.id.cmtg_cv_datepicker)
    public void simpleDatePicker() {
        Log.e(TAG, "date picker clicked!");
        new SlideDateTimePicker.Builder(getSupportFragmentManager())
                .setListener(dateTimeListener)
                .setInitialDate(new Date())
                .setMinDate(new Date())
                .setIndicatorColor(R.color.primaryColor)
                .build().show();
    }

    @OnClick(R.id.cmtg_btn_call_meeting)
    public void validateFormAndCreateMeeting() {
        if (etTitleInput.getText().toString().trim().equals("")) {
            ErrorUtils.showSnackBar(rlRoot, "Please enter a subject", Snackbar.LENGTH_LONG, "", null);
        } else if (etLocationInput.getText().toString().trim().equals("")) {
            ErrorUtils.showSnackBar(rlRoot, "Please enter a location", Snackbar.LENGTH_LONG, "", null);
        } else if (meetingStartDateTime == null) {
            ErrorUtils.showSnackBar(rlRoot, "Please enter a date and time for the meeting", Snackbar.LENGTH_LONG, "", null);
        } else {
            createMeetingAndExit();
        }
    }

    private void createMeetingAndExit() {

        final String phoneNumber = SettingPreference.getuser_mobilenumber(this);
        final String code = SettingPreference.getuser_token(this);
        final String title = etTitleInput.getText().toString();
        final String location = etLocationInput.getText().toString();
        final String description = etDescriptionInput.getText().toString();
        final String dateTimeISO = Constant.isoDateTimeSDF.format(meetingStartDateTime);

        Set<String> memberUids;

        if (notifyWholeGroup) {
            memberUids = Collections.emptySet();
        } else {
            memberUids = UtilClass.convertMembersToUids(assignedMembers);
        }

        grassrootRestService.getApi().createMeeting(phoneNumber, code, groupUid, title, description,
                dateTimeISO, "", 0, location, memberUids).enqueue(new Callback<GenericResponse>() {
            @Override
            public void onResponse(Call<GenericResponse> call, Response<GenericResponse> response) {
                if (response.isSuccessful()) {
                    // show some sort of message
                    finish();
                } else {
                    ErrorUtils.showSnackBar(rlRoot, "Error! Could not call meeting", Snackbar.LENGTH_LONG, "", null);
                }
            }

            @Override
            public void onFailure(Call<GenericResponse> call, Throwable t) {
                ErrorUtils.handleNetworkError(CreateMeetingActivity.this, rlRoot, t);
            }
        });
    }

}
