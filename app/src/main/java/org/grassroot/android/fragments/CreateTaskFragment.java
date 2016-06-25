package org.grassroot.android.fragments;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.CardView;
import android.support.v7.widget.SwitchCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.grassroot.android.R;
import org.grassroot.android.events.TaskAddedEvent;
import org.grassroot.android.interfaces.GroupConstants;
import org.grassroot.android.interfaces.NetworkErrorDialogListener;
import org.grassroot.android.interfaces.TaskConstants;
import org.grassroot.android.models.Member;
import org.grassroot.android.models.TaskResponse;
import org.grassroot.android.services.GrassrootRestService;
import org.grassroot.android.slideDateTimePicker.SlideDateTimeListener;
import org.grassroot.android.slideDateTimePicker.SlideDateTimePicker;
import org.grassroot.android.utils.Constant;
import org.grassroot.android.utils.ErrorUtils;
import org.grassroot.android.utils.MenuUtils;
import org.grassroot.android.utils.PreferenceUtils;
import org.grassroot.android.utils.Utilities;
import org.greenrobot.eventbus.EventBus;

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
import butterknife.OnTextChanged;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Created by luke on 2016/06/01.
 */
public class CreateTaskFragment extends Fragment {

    private static final String TAG = CreateTaskFragment.class.getCanonicalName();

    private String groupUid;
    private String taskType;

    private Date selectedDateTime;
    private boolean includeWholeGroup;
    private Set<Member> assignedMembers;

    private ViewGroup vContainer;

    @BindView(R.id.ctsk_et_title)
    TextInputEditText etTitleInput;
    @BindView(R.id.ctsk_et_location)
    TextInputEditText etLocationInput;
    @BindView(R.id.ctsk_et_description)
    TextInputEditText etDescriptionInput;

    private SlideDateTimeListener datePickerListener;
    @BindView(R.id.ctsk_txt_deadline)
    TextView dateTimeDisplayed;

    @BindView(R.id.ctsk_sw_one_day)
    SwitchCompat swOneDayAhead;
    @BindView(R.id.ctsk_sw_half_day)
    SwitchCompat swHalfDayAhead;
    @BindView(R.id.ctsk_sw_one_hour)
    SwitchCompat swOneHourAhead;

    @BindView(R.id.sw_notifyall)
    SwitchCompat swNotifyAll;
    @BindView(R.id.ctsk_rl_notify_count)
    LinearLayout notifyCountHolder;
    @BindView(R.id.ctsk_tv_member_count)
    TextView notifyMembersCount;
    @BindView(R.id.ctsk_tv_suffix)
    TextView notifyCountSuffix;

    @BindView(R.id.ctsk_reminder_body)
    RelativeLayout rlReminderBody;

    @BindView(R.id.ctsk_btn_create_task)
    Button btTaskCreate;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle b = getArguments();
        if (b == null) {
            throw new UnsupportedOperationException("Error! Fragment needs to be created with arguments");
        }
        groupUid = b.getString(GroupConstants.UID_FIELD);
        taskType = b.getString(TaskConstants.TASK_TYPE_FIELD);
        includeWholeGroup = true;

        datePickerListener = new SlideDateTimeListener() {
            @Override
            public void onDateTimeSet(Date date) {
                selectedDateTime = date;
                dateTimeDisplayed.setText(TaskConstants.dateDisplayFormatWithHours.format(date));
            }
        };
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View viewToReturn = inflater.inflate(R.layout.fragment_create_task, container, false);
        ButterKnife.bind(this, viewToReturn);
        this.vContainer = container;
        setUpStrings();
        return viewToReturn;
    }

    @OnClick(R.id.ctsk_cv_datepicker)
    public void launchDateTimePicker() {
        new SlideDateTimePicker.Builder(getFragmentManager())
                .setInitialDate(new Date())
                .setMinDate(new Date())
                .setIndicatorColor(R.color.primaryColor)
                .setListener(datePickerListener)
                .build()
                .show();
    }

    @OnCheckedChanged(R.id.sw_notifyall)
    public void selectAssignedMembers(boolean checked) {
        includeWholeGroup = checked;
        if (checked) {
            notifyCountHolder.setVisibility(View.GONE);
        } else {
            ArrayList<Member> preSelectedMembers = (assignedMembers == null) ? new ArrayList<Member>() :
                    new ArrayList<>(assignedMembers);
            Intent pickMember = MenuUtils.memberSelectionIntent(getActivity(), groupUid, CreateTaskFragment.class.getCanonicalName(), preSelectedMembers);
            startActivityForResult(pickMember, Constant.activitySelectGroupMembers);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK && requestCode == Constant.activitySelectGroupMembers) {
            Log.d(TAG, "got a result in fragment, from member picker");
            setAssignedMembers(data);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (assignedMembers == null || assignedMembers.isEmpty()) {
            swNotifyAll.setChecked(true);
        }
    }

    // todo : see if we can structure the calls better so don't have to round trip through activity
    public void setAssignedMembers(Intent data) {
        if (data == null) {
            throw new UnsupportedOperationException("Error! Need not null data back from activity");
        }

        List<Member> members = data.getParcelableArrayListExtra(Constant.SELECTED_MEMBERS_FIELD);
        if (members == null) {
            throw new UnsupportedOperationException("Error! Member picker must not return null list");
        }

        assignedMembers = new HashSet<>(members);
        if (assignedMembers.isEmpty()) {
            includeWholeGroup = true;
            swNotifyAll.setChecked(true);
        } else {
            includeWholeGroup = false;
            notifyMembersCount.setText(String.valueOf(assignedMembers.size()));
            notifyCountSuffix.setText(assignedMembers.size() > 1 ? "members" : "member");
            notifyCountHolder.setVisibility(View.VISIBLE);
        }
    }

    @OnClick(R.id.ctsk_btn_create_task)
    public void validateFormAndCreateTask() {
        if (etTitleInput.getText().toString().trim().equals("")) {
            ErrorUtils.showSnackBar(vContainer, "Please enter a subject", Snackbar.LENGTH_LONG, "", null);
        } else if (TaskConstants.MEETING.equals(taskType) && etLocationInput.getText().toString().trim().equals("")) {
            ErrorUtils.showSnackBar(vContainer, "Please enter a location", Snackbar.LENGTH_LONG, "", null);
        } else if (selectedDateTime == null) {
            ErrorUtils.showSnackBar(vContainer, "Please enter a date and time for the meeting", Snackbar.LENGTH_LONG, "", null);
        } else {
            createTask();
        }
    }

    public void createTask() {
        setUpApiCall().enqueue(new Callback<TaskResponse>() {
            @Override
            public void onResponse(Call<TaskResponse> call, Response<TaskResponse> response) {
                if (response.isSuccessful()) {
                    Intent i = new Intent();
                    String successString = generateSuccessString();
                    i.putExtra(Constant.SUCCESS_MESSAGE, successString);
                    getActivity().setResult(Activity.RESULT_OK, i);
                    Log.e(TAG, "putting task created event on the bus ...");
                    EventBus.getDefault().post(new TaskAddedEvent(response.body().getTasks().get(0), successString));
                    getActivity().finish(); // todo : double check calling finish on activity from within fragment is okay
                } else {
                    ErrorUtils.showSnackBar(vContainer, "Error! Something went wrong", Snackbar.LENGTH_LONG, "", null);
                }
            }

            @Override
            public void onFailure(Call<TaskResponse> call, Throwable t) {
                // todo: improve and fix this
                ErrorUtils.connectivityError(getActivity(), R.string.error_no_network, new NetworkErrorDialogListener() {
                    @Override
                    public void retryClicked() {
                      createTask();
                    }

                });

            }
        });
    }

    public Call<TaskResponse> setUpApiCall() {

        final String phoneNumber = PreferenceUtils.getUserPhoneNumber(getContext());
        final String code = PreferenceUtils.getAuthToken(getContext());
        final String title = etTitleInput.getText().toString();
        final String description = etDescriptionInput.getText().toString();
        final String dateTimeISO = Constant.isoDateTimeSDF.format(selectedDateTime);
        final int minutes = obtainReminderMinutes();

        final Set<String> memberUids;

        if (includeWholeGroup || assignedMembers == null || assignedMembers.isEmpty()) {
            memberUids = Collections.emptySet();
        } else {
            memberUids = Utilities.convertMembersToUids(assignedMembers);
        }

        switch (taskType) {
            case TaskConstants.MEETING:
                final String location = etLocationInput.getText().toString();
                return GrassrootRestService.getInstance().getApi().createMeeting(phoneNumber, code, groupUid,
                        title, description, dateTimeISO, minutes, location, memberUids);
            case TaskConstants.VOTE:
                return GrassrootRestService.getInstance().getApi().createVote(phoneNumber, code, groupUid, title,
                        description, dateTimeISO, minutes, memberUids, false);
            case TaskConstants.TODO:
                return GrassrootRestService.getInstance().getApi().createTodo(phoneNumber, code, groupUid, title,
                        description, dateTimeISO, minutes, memberUids);
            default:
                throw new UnsupportedOperationException("Error! Missing task type in call");
        }
    }

    private int obtainReminderMinutes() {
        if (TaskConstants.MEETING.equals(taskType)) {
            if (swOneDayAhead.isChecked()) {
                return 60 * 24;
            } else if (swHalfDayAhead.isChecked()) {
                return 60 * 6;
            } else if (swOneHourAhead.isChecked()) {
                return 60;
            } else {
                return -1;
            }
        } else {
            return -1;
        }
    }

    // note : returning just the int might be slightly more efficient, but then break string/placeholder pattern
    private String generateSuccessString() {
        switch (taskType) {
            case TaskConstants.MEETING:
                return includeWholeGroup ? getActivity().getString(R.string.ctsk_meeting_all_success) :
                        String.format(getActivity().getString(R.string.ctsk_meeting_assigned_success), assignedMembers.size());
            case TaskConstants.VOTE:
                return includeWholeGroup ? getActivity().getString(R.string.ctsk_vote_all_success) :
                        String.format(getActivity().getString(R.string.ctsk_vote_assigned_success), assignedMembers.size());
            case TaskConstants.TODO:
                return includeWholeGroup ? getActivity().getString(R.string.ctsk_todo_all_success) :
                        String.format(getActivity().getString(R.string.ctsk_todo_assigned_success), assignedMembers.size());
            default:
                throw new UnsupportedOperationException("Error! Missing task type");
        }
    }

    /*
    Substantive stuff over, remainder of code just sets up labels, animators & toggles
     */

    @BindView(R.id.ctsk_txt_ipl)
    TextInputLayout subjectInput;
    @BindView(R.id.ctsk_til_location)
    TextInputLayout locationInput;
    @BindView(R.id.txt_deadline_title)
    TextView deadlineTitle;
    @BindView(R.id.ctsk_cv_reminder)
    CardView reminderCard;
    @BindView(R.id.ctsk_tv_assign_label)
    TextView assignmentLabel;

    private void setUpStrings() {

        final boolean meeting = TaskConstants.MEETING.equals(taskType);

        etTitleInput.setImeOptions(meeting ? EditorInfo.IME_ACTION_NEXT : EditorInfo.IME_ACTION_DONE);
        locationInput.setVisibility(TaskConstants.MEETING.equals(taskType) ? View.VISIBLE : View.GONE);
        locationCharCounter.setVisibility(TaskConstants.MEETING.equals(taskType) ? View.VISIBLE : View.GONE);

        switch (taskType) {
            case TaskConstants.MEETING:
                // since meeting is most complex, and most commonly used, fragment is pre-set for it, so nothing to do here
                break;
            case TaskConstants.VOTE:
                subjectInput.setHint(getContext().getString(R.string.cvote_subject));
                deadlineTitle.setText(R.string.cvote_datetime);
                reminderCard.setVisibility(View.GONE);

                descriptionBody.setVisibility(View.VISIBLE);
                ivDescExpandIcon.setImageResource(R.drawable.ic_arrow_up);
                descriptionInput.setHint(getContext().getString(R.string.cvote_desc_hint));
                descriptionCard.setCardBackgroundColor(ContextCompat.getColor(getContext(), R.color.white));
                assignmentLabel.setText(R.string.cvote_invite_all);

                break;
            case TaskConstants.TODO:
                subjectInput.setHint(getContext().getString(R.string.ctodo_subject));
                deadlineTitle.setText(R.string.ctodo_datetime);
                descriptionInput.setHint(getContext().getString(R.string.ctodo_desc_hint));
                assignmentLabel.setText(R.string.ctodo_invite_all);
                btTaskCreate.setText(R.string.ctodo_button);
                break;
            default:
                throw new UnsupportedOperationException("Error! Fragment must have valid task type");
        }
    }

    @BindView(R.id.ctsk_iv_expand_alert)
    ImageView ivExpandReminders;

    @OnClick(R.id.ctsk_reminder_header)
    public void expandableReminderHeader() {

        if (rlReminderBody.getVisibility() != View.VISIBLE) {
            ivExpandReminders.setImageResource(R.drawable.ic_arrow_up);
            rlReminderBody.setVisibility(View.VISIBLE);
        } else {
            ivExpandReminders.setImageResource(R.drawable.ic_arrow_down);
            rlReminderBody.setVisibility(View.GONE);
        }
    }

    @OnCheckedChanged({ R.id.ctsk_sw_one_hour, R.id.ctsk_sw_half_day, R.id.ctsk_sw_one_day } )
    public void toggleReminders(SwitchCompat swToggled, boolean checked) {
        if (checked) {
            toggleSwitches(swToggled);
        }
    }

    private void toggleSwitches(SwitchCompat swChecked) {
        swOneHourAhead.setChecked(swChecked.equals(swOneHourAhead));
        swHalfDayAhead.setChecked(swChecked.equals(swHalfDayAhead));
        swOneDayAhead.setChecked(swChecked.equals(swOneDayAhead));
    }

    @BindView(R.id.ctsk_cv_description)
    CardView descriptionCard;
    @BindView(R.id.ctsk_rl_desc_body)
    RelativeLayout descriptionBody;
    @BindView(R.id.ctsk_til_desc)
    TextInputLayout descriptionInput;
    @BindView(R.id.ctsk_desc_expand)
    ImageView ivDescExpandIcon;

    @OnClick(R.id.ctsk_cv_description)
    public void expandDescription() {

        if (descriptionBody.getVisibility() != View.VISIBLE) {
            descriptionBody.setVisibility(View.VISIBLE);
            ivDescExpandIcon.setImageResource(R.drawable.ic_arrow_up);
        } else {
            descriptionBody.setVisibility(View.GONE);
            ivDescExpandIcon.setImageResource(R.drawable.ic_arrow_down);
        }
    }

    @BindView(R.id.ctsk_subject_count)
    TextView subjectCharCounter;
    @BindView(R.id.ctsk_desc_count)
    TextView descriptionCharCounter;
    @BindView(R.id.ctsk_location_count)
    TextView locationCharCounter;

    @OnTextChanged(R.id.ctsk_et_title)
    public void changeCharCounter(CharSequence s) {
        subjectCharCounter.setText(s.length() + " / 35"); // todo : externalize
    }

    @OnTextChanged(R.id.ctsk_et_location)
    public void changeLocCharCounter(CharSequence s) {
        locationCharCounter.setText(s.length() + " / 35");
    }

    @OnTextChanged(R.id.ctsk_et_description)
    public void changeDescCounter(CharSequence s) {
        descriptionCharCounter.setText(s.length() + " / 250"); // todo : externalize
    }
}
