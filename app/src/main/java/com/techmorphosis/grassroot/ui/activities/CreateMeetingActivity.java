package com.techmorphosis.grassroot.ui.activities;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputEditText;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
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
import com.techmorphosis.grassroot.utils.MenuUtils;
import com.techmorphosis.grassroot.utils.SettingPreference;
import com.techmorphosis.grassroot.utils.UtilClass;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnCheckedChanged;
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
    @BindView(R.id.cmtg_tlb)
    Toolbar toolbar;

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

    @BindView(R.id.cmtg_reminder_header)
    RelativeLayout rlReminderHeader;
    @BindView(R.id.cmtg_reminder_body)
    RelativeLayout rlReminderBody;
    @BindView(R.id.cmtg_iv_expand_alert)
    ImageView ivReminderExpandIcon;
    @BindView(R.id.cmtg_sw_one_day)
    SwitchCompat swOneDayAhead;
    @BindView(R.id.cmtg_sw_half_day)
    SwitchCompat swHalfDayAhead;
    @BindView(R.id.cmtg_sw_one_hour)
    SwitchCompat swOneHourAhead;

    private ValueAnimator reminderSlideOutAnimator;

    @BindView(R.id.sw_notifyall)
    SwitchCompat swNotifyAll;
    @BindView(R.id.cmtg_rl_notify_count)
    RelativeLayout notifyCountHolder;
    @BindView(R.id.cmtg_tv_member_count)
    TextView notifyMembersCount;
    @BindView(R.id.cmtg_tv_suffix)
    TextView notifyCountSuffix;


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

        setSupportActionBar(toolbar);
        toolbar.setNavigationIcon(R.drawable.btn_back_wt);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish(); // todo : uh, fix
            }
        });

        rlReminderBody.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                rlReminderBody.getViewTreeObserver().removeOnPreDrawListener(this);
                rlReminderBody.setVisibility(View.GONE);

                // todo : double check these for inefficiencies
                final int widthSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
                final int heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
                rlReminderBody.measure(widthSpec, heightSpec);

                reminderSlideOutAnimator = createSlideAnimator(0, rlReminderBody.getMeasuredHeight());
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

    // todo : figure out why this is quite abrupt on collapsing
    private ValueAnimator createSlideAnimator(int startHeight, int endHeight) {
        ValueAnimator animator = ValueAnimator.ofInt(startHeight, endHeight);
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

    private void expandReminders() {
        ivReminderExpandIcon.setImageResource(R.drawable.ic_arrow_up);
        rlReminderBody.setVisibility(View.VISIBLE);
        reminderSlideOutAnimator.start();
    }

    private void collapseReminders() {
        ivReminderExpandIcon.setImageResource(R.drawable.ic_arrow_down);
        ValueAnimator slideInAnimator = createSlideAnimator(rlReminderBody.getHeight(), 0);
        // todo: figure out why this is occuring pretty fast
        slideInAnimator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {
                Log.e(TAG, "animation done, setting visibility!");
                rlReminderBody.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationEnd(Animator animator) {
            }

            @Override
            public void onAnimationCancel(Animator animator) {
            }

            @Override
            public void onAnimationRepeat(Animator animator) {
            }
        });
        slideInAnimator.start();
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
        new SlideDateTimePicker.Builder(getSupportFragmentManager())
                .setListener(dateTimeListener)
                .setInitialDate(new Date())
                .setMinDate(new Date())
                .setIndicatorColor(R.color.primaryColor)
                .build().show();
    }

    /*
    SETTING THE REMINDER DATE AND TIME
     */

    @OnClick(R.id.cmtg_reminder_header)
    public void expandableHeader() {
        if (rlReminderBody.getVisibility() == View.VISIBLE) {
            Log.e(TAG, "retracting reminder settings!");
            collapseReminders();
        } else {
            Log.e(TAG, "expanding reminder settings!");
            expandReminders();
        }
    }

    @OnCheckedChanged(R.id.cmtg_sw_one_day)
    public void toggleOneDayChecked(boolean checked) {
        if (checked) {
            toggleSwitches(swOneDayAhead);
        }
    }

    @OnCheckedChanged(R.id.cmtg_sw_half_day)
    public void toggleHalfDayChecked(boolean checked) {
        if (checked) {
            toggleSwitches(swHalfDayAhead);
        }
    }

    @OnCheckedChanged(R.id.cmtg_sw_one_hour)
    public void toggleOneHourChecked(boolean checked) {
        if (checked) {
            toggleSwitches(swOneHourAhead);
        }
    }

    private void toggleSwitches(SwitchCompat swChecked) {
        swOneHourAhead.setChecked(swChecked.equals(swOneHourAhead));
        swHalfDayAhead.setChecked(swChecked.equals(swHalfDayAhead));
        swOneDayAhead.setChecked(swChecked.equals(swOneDayAhead));
    }

    private int obtainReminderMinutes() {
        if (swOneDayAhead.isChecked()) {
            return 60 * 24;
        } else if (swHalfDayAhead.isChecked()) {
            return 60 * 6;
        } else if (swOneHourAhead.isChecked()) {
            return 60;
        } else {
            return 0;
        }
    }

    /*
    PICK MEMBERS
     */
    @OnCheckedChanged(R.id.sw_notifyall)
    public void toggleNotifyAllMembers(boolean checked) {
        notifyWholeGroup = checked;
        if (checked) {
            notifyCountHolder.setVisibility(View.GONE);
        } else {
            Log.e(TAG, "need to pick a member!");
            ArrayList<Member> preSelectedMembers = (assignedMembers == null) ? new ArrayList<Member>() :
                    new ArrayList<>(assignedMembers);
            Intent pickMember = MenuUtils.constructIntent(this, GroupMembersActivity.class, groupUid, "");
            pickMember.putExtra(Constant.PARENT_TAG_FIELD, TAG);
            pickMember.putExtra(Constant.SELECT_FIELD, true);
            pickMember.putExtra(Constant.SHOW_ACTION_BUTTON_FLAG, false);
            pickMember.putExtra(Constant.SHOW_HEADER_FLAG, false);
            pickMember.putParcelableArrayListExtra(Constant.SELECTED_MEMBERS_FIELD, preSelectedMembers);
            startActivityForResult(pickMember, Constant.activitySelectGroupMembers);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && requestCode == Constant.activitySelectGroupMembers) {
            if (data == null) {
                throw new UnsupportedOperationException("Error! Need not null data back from activity");
            }

            List<Member> members = data.getParcelableArrayListExtra(Constant.SELECTED_MEMBERS_FIELD);
            if (members == null) {
                throw new UnsupportedOperationException("Error! Member picker must not return null list");
            }

            assignedMembers = new HashSet<>(members);
            if (assignedMembers.isEmpty()) {
                swNotifyAll.setChecked(true);
            } else {
                notifyMembersCount.setText(String.valueOf(assignedMembers.size()));
                notifyCountSuffix.setText(assignedMembers.size() > 1 ? "members" : "member");
                notifyCountHolder.setVisibility(View.VISIBLE);
            }
        }
    }

    /*
    VALIDATE ENTRY AND CALL THE MEETING
     */
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
        final int minutes = obtainReminderMinutes();

        Set<String> memberUids;

        if (notifyWholeGroup || assignedMembers == null || assignedMembers.isEmpty()) {
            memberUids = Collections.emptySet();
        } else {
            memberUids = UtilClass.convertMembersToUids(assignedMembers);
        }

        grassrootRestService.getApi().createMeeting(phoneNumber, code, groupUid, title, description,
                dateTimeISO, minutes, location, memberUids).enqueue(new Callback<GenericResponse>() {
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