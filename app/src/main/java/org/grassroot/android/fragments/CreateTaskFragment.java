package org.grassroot.android.fragments;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.app.TimePickerDialog;
import android.content.Context;
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
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.TimePicker;

import org.grassroot.android.R;
import org.grassroot.android.activities.ActionCompleteActivity;
import org.grassroot.android.activities.GrassrootExtraActivity;
import org.grassroot.android.events.TaskAddedEvent;
import org.grassroot.android.fragments.dialogs.AccountLimitDialogFragment;
import org.grassroot.android.fragments.dialogs.DatePickerFragment;
import org.grassroot.android.fragments.dialogs.TimePickerFragment;
import org.grassroot.android.interfaces.GroupConstants;
import org.grassroot.android.interfaces.NavigationConstants;
import org.grassroot.android.interfaces.TaskConstants;
import org.grassroot.android.models.Group;
import org.grassroot.android.models.Member;
import org.grassroot.android.models.RealmString;
import org.grassroot.android.models.TaskModel;
import org.grassroot.android.models.exceptions.ApiCallException;
import org.grassroot.android.services.ApplicationLoader;
import org.grassroot.android.services.TaskService;
import org.grassroot.android.utils.Constant;
import org.grassroot.android.utils.ErrorUtils;
import org.grassroot.android.utils.IntentUtils;
import org.grassroot.android.utils.NetworkUtils;
import org.grassroot.android.utils.RealmUtils;
import org.grassroot.android.utils.Utilities;
import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnCheckedChanged;
import butterknife.OnClick;
import butterknife.OnTextChanged;
import butterknife.Unbinder;
import io.realm.RealmList;
import rx.Subscriber;
import rx.functions.Action1;

/**
 * Created by luke on 2016/06/01.
 */
public class CreateTaskFragment extends Fragment {

    private static final String TAG = CreateTaskFragment.class.getCanonicalName();

    private String groupUid;
    private String taskType;
    private boolean groupLocal;

    private boolean datePicked, timePicked;
    private Date selectedDateTime;
    private Calendar selectedDateTimeCal; // Java 7 nastiness
    private boolean includeWholeGroup;
    private Set<Member> assignedMembers;

    private ViewGroup vContainer;

    private ProgressDialog progressDialog;
    private String shortCharCounter;
    private String longCharCounter;

    private Unbinder unbinder;

    @BindView(R.id.ctsk_et_title) TextInputEditText etTitleInput;
    @BindView(R.id.ctsk_et_location) TextInputEditText etLocationInput;
    @BindView(R.id.ctsk_et_description) TextInputEditText etDescriptionInput;

    private DatePickerFragment datePicker;
    private TimePickerFragment timePicker;
    @BindView(R.id.ctsk_txt_deadline) TextView dateDisplayed;
    @BindView(R.id.ctsk_txt_time) TextView timeDisplayed;

    @BindView(R.id.ctsk_reminder_body) RelativeLayout rlReminderBody;
    @BindView(R.id.ctsk_remind_option0) TextView textRemindOption0;
    @BindView(R.id.ctsk_sw_one_day) SwitchCompat switchReminderOption0;
    @BindView(R.id.ctsk_remind_option1) TextView textRemindOption1;
    @BindView(R.id.ctsk_sw_half_day) SwitchCompat switchReminderOption1;
    @BindView(R.id.ctsk_remind_option2) TextView textRemindOption2;
    @BindView(R.id.ctsk_sw_one_hour) SwitchCompat switchRemindOption2;

    @BindView(R.id.ctsk_tv_member_count)
    TextView notifyMembersCount;

    @BindView(R.id.ctsk_btn_create_task) Button btTaskCreate;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle b = getArguments();
        if (b == null) {
            throw new UnsupportedOperationException("Error! Fragment needs to be created with arguments");
        }
        groupUid = b.getString(GroupConstants.UID_FIELD);
        taskType = b.getString(TaskConstants.TASK_TYPE_FIELD);
        groupLocal = b.getBoolean(GroupConstants.LOCAL_FIELD);

        selectedDateTimeCal = Calendar.getInstance();
        includeWholeGroup = true;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View viewToReturn = inflater.inflate(R.layout.fragment_create_task, container, false);
        unbinder = ButterKnife.bind(this, viewToReturn);

        this.vContainer = container;
        progressDialog = new ProgressDialog(getContext());
        progressDialog.setIndeterminate(true);
        progressDialog.setMessage(getString(R.string.wait_message));
        shortCharCounter = getString(R.string.generic_35_char_counter);
        longCharCounter = getString(R.string.generic_250_char_conter);

        setUpStrings();
        return viewToReturn;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }

    @OnClick(R.id.ctsk_cv_datepicker)
    public void launchDateTimePicker() {
        if (datePicker == null) {
            setUpDatePicker();
        }
        datePicker.show(getFragmentManager(), DatePickerFragment.class.getCanonicalName());
    }

    private void setUpDatePicker() {
        datePicker = DatePickerFragment.newInstance(new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                selectedDateTimeCal.set(Calendar.YEAR, year);
                selectedDateTimeCal.set(Calendar.MONTH, monthOfYear);
                selectedDateTimeCal.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                updateDate();
            }
        });
    }

    private void updateDate() {
        datePicked = true;
        selectedDateTime = selectedDateTimeCal.getTime();
        dateDisplayed.setText(TaskConstants.dateDisplayWithoutHours.format(selectedDateTime));
    }

    @OnClick(R.id.ctsk_cv_timepicker)
    public void launchTimePicker() {
        if (timePicker == null) {
            setUpTimePicker();
        }
        timePicker.show(getFragmentManager(), TimePickerFragment.class.getCanonicalName());
    }

    private void setUpTimePicker() {
        timePicker = TimePickerFragment.newInstance(new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                selectedDateTimeCal.set(Calendar.HOUR_OF_DAY, hourOfDay);
                selectedDateTimeCal.set(Calendar.MINUTE, minute);
                updateTime();
            }
        });
    }

    private void updateTime() {
        timePicked = true;
        selectedDateTime = selectedDateTimeCal.getTime();
        timeDisplayed.setText(TaskConstants.timeDisplayWithoutDate.format(selectedDateTime));
    }

    @OnClick(R.id.ctsk_notify_switch)
    public void selectAssignedMembers() {
        final ArrayList<Member> preSelectedMembers = new ArrayList<>();
        if (assignedMembers == null) {
            RealmUtils.loadGroupMembers(groupUid, true).subscribe(new Action1<List<Member>>() {
                @Override
                public void call(List<Member> members) {
                    preSelectedMembers.addAll(members);
                    startPickMemberActivity(preSelectedMembers);
                }
            });
        } else {
            preSelectedMembers.addAll(assignedMembers);
            startPickMemberActivity(preSelectedMembers);
        }
    }

    private void startPickMemberActivity(ArrayList<Member> preSelectedMembers) {
        Intent pickMember = IntentUtils.memberSelectionIntent(getActivity(), groupUid, CreateTaskFragment.class.getCanonicalName(), preSelectedMembers);
        startActivityForResult(pickMember, NavigationConstants.SELECT_MEMBERS);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK && requestCode == NavigationConstants.SELECT_MEMBERS) {
            setAssignedMembers(data);
        }
    }

    public void setAssignedMembers(Intent data) {
        if (data == null) {
            Log.e(TAG, "Error! Need not null data back from activity");
            return;
        }

        List<Member> members = data.getParcelableArrayListExtra(Constant.SELECTED_MEMBERS_FIELD);
        if (members == null) {
            Log.e(TAG, "Error! Member picker must not return null list");
            return;
        }

        assignedMembers = new HashSet<>(members);
        if (assignedMembers.isEmpty()) {
            includeWholeGroup = true;
        } else {
            includeWholeGroup = false;
            final String pluralMember = getResources().getQuantityString(R.plurals.numberMembersSelected,
                assignedMembers.size(), assignedMembers.size());
            Log.e(TAG, "pluralMember string = " + pluralMember);
            notifyMembersCount.setText(pluralMember);
            notifyMembersCount.setVisibility(View.VISIBLE);
        }
    }

    @OnClick(R.id.ctsk_btn_create_task)
    public void validateFormAndCreateTask() {
        if (etTitleInput.getText().toString().trim().isEmpty()) {
            etTitleInput.setError(getString(R.string.input_error_task_no_subject));
            Snackbar.make(vContainer, R.string.input_error_task_no_subject, Snackbar.LENGTH_SHORT).show();
        } else if (TaskConstants.MEETING.equals(taskType) && etLocationInput.getText().toString().trim().isEmpty()) {
            etLocationInput.setError(getString(R.string.input_error_task_no_location));
            Snackbar.make(vContainer, R.string.input_error_task_no_location, Snackbar.LENGTH_SHORT).show();
        } else if (!datePicked || (!taskType.equals(TaskConstants.TODO) && !timePicked)) {
            Snackbar.make(vContainer, R.string.input_error_task_no_date, Snackbar.LENGTH_SHORT).show();
        } else {
            createTask();
        }
    }

    public void createTask() {
        progressDialog.show();
        final TaskModel model = generateTaskObject();
        TaskService.getInstance().sendTaskToServer(model, null).subscribe(new Subscriber<TaskModel>() {
            @Override
            public void onNext(TaskModel taskModel) {
                progressDialog.dismiss();
                finishAndLaunchDoneFragment(taskModel);
            }

            @Override
            public void onError(Throwable e) {
                if (e instanceof ApiCallException) {
                    handleError((ApiCallException) e, model);
                }
            }

            @Override
            public void onCompleted() { }
        });
    }

    private void finishAndLaunchDoneFragment(TaskModel model) {
        Intent i = new Intent(getActivity(), ActionCompleteActivity.class);
        if (model.isLocal()) {
            i.putExtra(ActionCompleteActivity.BODY_FIELD, getString(R.string.ac_body_task_create_local, taskType.toLowerCase()));
            i.putExtra(ActionCompleteActivity.HEADER_FIELD, R.string.ac_header_task_create_local);
        } else {
            i.putExtra(ActionCompleteActivity.HEADER_FIELD, R.string.ac_header_task_create);
            i.putExtra(ActionCompleteActivity.BODY_FIELD, generateSuccessString());
        }
        i.putExtra(ActionCompleteActivity.SHARE_BUTTON, true);
        i.putExtra(ActionCompleteActivity.TASK_BUTTONS, false);
        i.putExtra(ActionCompleteActivity.ACTION_INTENT, ActionCompleteActivity.GROUP_SCREEN);

        i.putExtra(TaskConstants.TASK_ENTITY_FIELD, model);
        Group taskGroup = RealmUtils.loadObjectFromDB(Group.class, "groupUid", model.getParentUid());
        i.putExtra(GroupConstants.OBJECT_FIELD, taskGroup); // note : this seems heavy ... likely better to send UID and load in activity .. to optimize in future

        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        EventBus.getDefault().post(new TaskAddedEvent(model, generateSuccessString()));
        startActivity(i);
        getActivity().finish();
    }

    private void handleError(ApiCallException e, TaskModel model) {
        progressDialog.dismiss();
        final String type = e.getMessage();
        if (NetworkUtils.CONNECT_ERROR.equals(type)) {
            finishAndLaunchDoneFragment(model);
        } else {
            if (ErrorUtils.TODO_LIMIT_REACHED.equals(e.errorTag)) {
                AccountLimitDialogFragment.showAccountLimitDialog(getFragmentManager(), R.string.account_todo_limit_reached)
                        .subscribe(new Action1<String>() {
                            @Override
                            public void call(String s) {
                                if (AccountLimitDialogFragment.GO_TO_GR.equals(s)) {
                                    Intent i = new Intent(getContext(), GrassrootExtraActivity.class);
                                    startActivity(i);
                                } else if (AccountLimitDialogFragment.ABORT.equals(s)) {
                                    getActivity().finish();
                                }
                            }
                        });
            } else {
                final String msg = ErrorUtils.serverErrorText(e);
                Snackbar.make(vContainer, msg, Snackbar.LENGTH_SHORT); // todo : add a "save and try again option"
            }
        }
    }

    private TaskModel generateTaskObject() {
        Log.e(TAG, "creating task model ... isGroupLocal set to ... " + groupLocal);

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

        TaskModel model = new TaskModel();

        model.setDescription(description);
        model.setTitle(title);
        model.setCreatedByUserName(RealmUtils.loadPreferencesFromDB().getUserName());
        model.setDeadlineISO(dateTimeISO);
        model.setDeadlineDate(selectedDateTime);
        model.setLocation(etLocationInput.getText().toString());
        model.setParentUid(groupUid);
        model.setTaskUid(UUID.randomUUID().toString());
        model.setType(taskType);
        model.setParentLocal(groupLocal);
        model.setLocal(true); // true until replaced when received from server ...
        model.setMinutes(minutes);
        model.setCanEdit(true);
        model.setCanAction(true);
        model.setReply(TaskConstants.TODO_PENDING);
        model.setHasResponded(false);

        final RealmList<RealmString> realmMemberUids = RealmUtils.convertListOfStringInRealmListOfString(new ArrayList<>(memberUids));
        model.setMemberUIDS(realmMemberUids);

        return model;
    }

    private int obtainReminderMinutes() {
        final int reminderChecked = getReminderSwitchChecked();
        if (reminderChecked != -1 && reminderChecked < 3) {
            switch (taskType) {
                case TaskConstants.MEETING:
                    return TaskConstants.meetingReminderMinutes[reminderChecked];
                case TaskConstants.TODO:
                    return TaskConstants.todoReminderMinutes[reminderChecked];
                default:
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
                return includeWholeGroup ? getActivity().getString(R.string.ctsk_meeting_all_success)
                        : String.format(getActivity().getString(R.string.ctsk_meeting_assigned_success),
                        assignedMembers.size());
            case TaskConstants.VOTE:
                return includeWholeGroup ? getActivity().getString(R.string.ctsk_vote_all_success)
                        : String.format(getActivity().getString(R.string.ctsk_vote_assigned_success),
                        assignedMembers.size());
            case TaskConstants.TODO:
                return includeWholeGroup ? getActivity().getString(R.string.ctsk_todo_all_success)
                        : String.format(getActivity().getString(R.string.ctsk_todo_assigned_success),
                        assignedMembers.size());
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
    @BindView(R.id.txt_date_title)
    TextView dateTitle;
    @BindView(R.id.txt_time_title)
    TextView timeTitle;
    @BindView(R.id.ctsk_cv_reminder)
    CardView reminderCard;
    @BindView(R.id.ctsk_tv_assign_label)
    TextView assignmentLabel;

    private void setUpStrings() {

        final boolean meeting = TaskConstants.MEETING.equals(taskType);

        etTitleInput.setImeOptions(meeting ? EditorInfo.IME_ACTION_NEXT : EditorInfo.IME_ACTION_DONE);
        locationInput.setVisibility(TaskConstants.MEETING.equals(taskType) ? View.VISIBLE : View.GONE);
        locationCharCounter.setVisibility(
                TaskConstants.MEETING.equals(taskType) ? View.VISIBLE : View.GONE);

        switch (taskType) {
            case TaskConstants.MEETING:
                // since meeting is most complex, and most commonly used, fragment is pre-set for it
                // hence just making explicit remind logic here

                textRemindOption0.setText(TaskConstants.meetingReminderDesc[0]);
                textRemindOption1.setText(TaskConstants.meetingReminderDesc[1]);
                textRemindOption2.setText(TaskConstants.meetingReminderDesc[2]);
                // defaults to one day ahead
                switchReminderOption0.setChecked(true);
                toggleSwitches(switchReminderOption0);

                break;
            case TaskConstants.VOTE:
                subjectInput.setHint(getContext().getString(R.string.cvote_subject));
                dateTitle.setText(R.string.cvote_date);
                timeTitle.setText(R.string.cvote_time);
                reminderCard.setVisibility(View.GONE);

                descriptionBody.setVisibility(View.VISIBLE);
                ivDescExpandIcon.setImageResource(R.drawable.ic_arrow_up);
                descriptionInput.setHint(getContext().getString(R.string.cvote_desc_hint));
                descriptionCard.setCardBackgroundColor(ContextCompat.getColor(getContext(), R.color.white));
                assignmentLabel.setText(R.string.cvote_invite_all);

                break;
            case TaskConstants.TODO:
                subjectInput.setHint(getContext().getString(R.string.ctodo_subject));
                dateTitle.setText(R.string.ctodo_date);
                timeTitle.setText(R.string.ctodo_time);
                descriptionInput.setHint(getContext().getString(R.string.ctodo_desc_hint));
                assignmentLabel.setText(R.string.ctodo_invite_all);
                btTaskCreate.setText(R.string.ctodo_button);

                textRemindOption0.setText(TaskConstants.todoReminderDesc[0]);
                textRemindOption1.setText(TaskConstants.todoReminderDesc[1]);
                textRemindOption2.setText(TaskConstants.todoReminderDesc[2]);
                // default to one day ahead
                switchReminderOption1.setChecked(true);
                toggleSwitches(switchReminderOption1);

                break;
            default:
                throw new UnsupportedOperationException("Error! Fragment must have valid task type");
        }
    }

    @BindView(R.id.ctsk_iv_expand_alert)
    ImageView ivExpandReminders;

    @OnClick(R.id.ctsk_reminder_header)
    public void expandAndContractReminders() {
        if (rlReminderBody.getVisibility() != View.VISIBLE) {
            ivExpandReminders.setImageResource(R.drawable.ic_arrow_up);
            rlReminderBody.setVisibility(View.VISIBLE);
            hideKeyboard(rlReminderBody); // to make sure the views underneath stay visible
        } else {
            ivExpandReminders.setImageResource(R.drawable.ic_arrow_down);
            rlReminderBody.setVisibility(View.GONE);
        }
    }

    @OnCheckedChanged({R.id.ctsk_sw_one_hour, R.id.ctsk_sw_half_day, R.id.ctsk_sw_one_day})
    public void toggleReminders(SwitchCompat swToggled, boolean checked) {
        if (checked) {
            toggleSwitches(swToggled);
        }
    }

    private void toggleSwitches(SwitchCompat swChecked) {
        switchReminderOption0.setChecked(swChecked.equals(switchReminderOption0));
        switchReminderOption1.setChecked(swChecked.equals(switchReminderOption1));
        switchRemindOption2.setChecked(swChecked.equals(switchRemindOption2));
    }

    private int getReminderSwitchChecked() {
        if (switchReminderOption0.isChecked()) {
            return 0;
        } else if (switchReminderOption1.isChecked()) {
            return 1;
        } else if (switchRemindOption2.isChecked()) {
            return 2;
        } else {
            return -1;
        }
    }

    private void hideKeyboard(View currentView) {
        InputMethodManager imm = (InputMethodManager) ApplicationLoader.applicationContext
            .getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(currentView.getWindowToken(), 0);
    }

    @BindView(R.id.ctsk_cv_description)
    CardView descriptionCard;
    @BindView(R.id.ctsk_rl_desc_body)
    LinearLayout descriptionBody;
    @BindView(R.id.ctsk_til_desc)
    TextInputLayout descriptionInput;
    @BindView(R.id.ctsk_desc_expand)
    ImageView ivDescExpandIcon;

    @OnClick(R.id.ctsk_cv_description)
    public void expandDescription() {
        if (descriptionBody.getVisibility() != View.VISIBLE) {
            descriptionBody.setVisibility(View.VISIBLE);
            ivDescExpandIcon.setImageResource(R.drawable.ic_arrow_up);
            // hide these to make sure space on small phone screens
            ivExpandReminders.setImageResource(R.drawable.ic_arrow_down);
            rlReminderBody.setVisibility(View.GONE);;
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
        subjectCharCounter.setText(String.format(shortCharCounter, s.length()));
    }

    @OnTextChanged(R.id.ctsk_et_location)
    public void changeLocCharCounter(CharSequence s) {
        locationCharCounter.setText(String.format(shortCharCounter, s.length()));
    }

    @OnTextChanged(R.id.ctsk_et_description)
    public void changeDescCounter(CharSequence s) {
        descriptionCharCounter.setText(String.format(longCharCounter, s.length()));
    }
}
