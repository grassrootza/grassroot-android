package org.grassroot.android.fragments;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.grassroot.android.R;
import org.grassroot.android.events.TaskUpdatedEvent;
import org.grassroot.android.fragments.dialogs.ConfirmCancelDialogFragment;
import org.grassroot.android.interfaces.NetworkErrorDialogListener;
import org.grassroot.android.interfaces.TaskConstants;
import org.grassroot.android.models.GenericResponse;
import org.grassroot.android.models.Member;
import org.grassroot.android.models.TaskModel;
import org.grassroot.android.models.TaskResponse;
import org.grassroot.android.services.GrassrootRestService;
import org.grassroot.android.slideDateTimePicker.SlideDateTimeListener;
import org.grassroot.android.slideDateTimePicker.SlideDateTimePicker;
import org.grassroot.android.utils.Constant;
import org.grassroot.android.utils.ErrorUtils;
import org.grassroot.android.utils.PreferenceUtils;
import org.grassroot.android.utils.Utilities;
import org.greenrobot.eventbus.EventBus;

import java.util.Date;
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
 * Created by paballo on 2016/06/21.
 */
public class EditTaskFragment extends Fragment {

    private static final String TAG = EditTaskFragment.class.getCanonicalName();

    private String groupUid;
    private String taskType;
    private TaskModel task;

    private Date selectedDateTime;
    private Set<Member> assignedMembers;

    private ViewGroup vContainer;

    @BindView(R.id.etsk_et_title)
    TextInputEditText etTitleInput;
    @BindView(R.id.etsk_et_location)
    TextInputEditText etLocationInput;
    @BindView(R.id.etsk_et_description)
    TextInputEditText etDescriptionInput;

    private SlideDateTimeListener datePickerListener;
    @BindView(R.id.etsk_txt_deadline)
    TextView dateTimeDisplayed;

    @BindView(R.id.etsk_sw_one_day)
    SwitchCompat swOneDayAhead;
    @BindView(R.id.etsk_sw_half_day)
    SwitchCompat swHalfDayAhead;
    @BindView(R.id.etsk_sw_one_hour)
    SwitchCompat swOneHourAhead;

    @BindView(R.id.etsk_rl_notify_count)
    RelativeLayout notifyCountHolder;
    @BindView(R.id.etsk_tv_member_count)
    TextView notifyMembersCount;
    @BindView(R.id.etsk_tv_suffix)
    TextView notifyCountSuffix;

    @BindView(R.id.etsk_reminder_body)
    RelativeLayout rlReminderBody;

    @BindView(R.id.etsk_btn_update_task)
    Button btTaskUpdate;
    @BindView(R.id.etsk_btn_cancel_task)
    Button btCancelTask;


    public static EditTaskFragment newInstance(TaskModel task) {
        EditTaskFragment fragment = new EditTaskFragment();
        Bundle args = new Bundle();
        args.putParcelable(TaskConstants.TASK_ENTITY_FIELD, task);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle b = getArguments();
        if (b == null) {
            throw new UnsupportedOperationException("Error! Fragment needs to be created with arguments");
        }
        Bundle args = getArguments();
        task = args.getParcelable(TaskConstants.TASK_ENTITY_FIELD);
        taskType = task.getType();
        groupUid = task.getParentUid();
        selectedDateTime = task.getDeadlineDate();
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
        View viewToReturn = inflater.inflate(R.layout.fragment_edit_task, container, false);
        ButterKnife.bind(this, viewToReturn);
        this.vContainer = container;
        setUpStrings();
        populateFields();
        return viewToReturn;
    }

    @OnClick(R.id.etsk_cv_datepicker)
    public void launchDateTimePicker() {
        new SlideDateTimePicker.Builder(getFragmentManager())
                .setInitialDate(new Date())
                .setMinDate(new Date())
                .setIndicatorColor(R.color.primaryColor)
                .setListener(datePickerListener)
                .build()
                .show();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @OnClick(R.id.etsk_btn_update_task)
    public void validateAndUpdate() {
        if (etTitleInput.getText().toString().trim().equals("")) {
            ErrorUtils.showSnackBar(vContainer, "Please enter a subject", Snackbar.LENGTH_LONG, "", null);
        } else if (TaskConstants.MEETING.equals(taskType) && etLocationInput.getText().toString().trim().equals("")) {
            ErrorUtils.showSnackBar(vContainer, "Please enter a location", Snackbar.LENGTH_LONG, "", null);
        } else if (selectedDateTime == null) {
            ErrorUtils.showSnackBar(vContainer, "Please enter a date and time for the meeting", Snackbar.LENGTH_LONG, "", null);
        } else {
            updateTask();
        }
    }

    @OnClick(R.id.etsk_btn_cancel_task)
    public void confirmAndCancel(){
        String dialogMessage = generateConfirmationDialogStrings();
        ConfirmCancelDialogFragment confirmCancelDialogFragment = ConfirmCancelDialogFragment.newInstance(dialogMessage, new ConfirmCancelDialogFragment.ConfirmDialogListener() {
            @Override
            public void doConfirmClicked() {
                cancelTask();

            }
        });
        confirmCancelDialogFragment.show(getFragmentManager(), TAG);
    }

    private void populateFields() {
        switch (task.getType()) {
            case TaskConstants.MEETING:
                etTitleInput.setText(task.getTitle());
                etDescriptionInput.setText(task.getDescription());
                etLocationInput.setText(task.getLocation());
                dateTimeDisplayed.setText(TaskConstants.dateDisplayFormatWithHours.format(task.getDeadlineDate()));
                break;
            case TaskConstants.VOTE:
                etTitleInput.setText(task.getTitle());
                etTitleInput.setEnabled(false);
                etDescriptionInput.setText(task.getDescription());
                ;
                dateTimeDisplayed.setText(TaskConstants.dateDisplayFormatWithHours.format(task.getDeadlineDate()));
                break;
            case TaskConstants.TODO:
                etTitleInput.setText(task.getTitle());

        }
    }

    public void updateTask() {
        setUpUpdateApiCall().enqueue(new Callback<TaskResponse>() {
            @Override
            public void onResponse(Call<TaskResponse> call, Response<TaskResponse> response) {
                if (response.isSuccessful()) {
                    Intent i = new Intent();
                    i.putExtra(Constant.SUCCESS_MESSAGE, generateSuccessString());
                    getActivity().setResult(Activity.RESULT_OK, i);
                    EventBus.getDefault().post(new TaskUpdatedEvent(response.body().getTasks().get(0)));
                    getActivity().finish();
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
                        updateTask();
                    }

                });

            }
        });
    }

    public void cancelTask() {
        setUpCancelApiCall().enqueue(new Callback<GenericResponse>() {
            @Override
            public void onResponse(Call<GenericResponse> call, Response<GenericResponse> response) {
                if (response.isSuccessful()) {
                    Intent i = new Intent();
                    i.putExtra(Constant.SUCCESS_MESSAGE, generateSuccessString());
                    getActivity().setResult(Activity.RESULT_OK, i);
                    getActivity().finish();
                } else {
                    ErrorUtils.showSnackBar(vContainer, "Error! Something went wrong", Snackbar.LENGTH_LONG, "", null);
                }
            }

            @Override
            public void onFailure(Call<GenericResponse> call, Throwable t) {
                ErrorUtils.connectivityError(getActivity(), R.string.error_no_network, new NetworkErrorDialogListener() {
                    @Override
                    public void retryClicked() {
                        cancelTask();
                    }

                });

            }
        });

    }

    public Call<TaskResponse> setUpUpdateApiCall() {

        final String uid = task.getTaskUid();
        final String phoneNumber = PreferenceUtils.getUserPhoneNumber(getContext());
        final String code = PreferenceUtils.getAuthToken(getContext());
        final String title = etTitleInput.getText().toString();
        final String description = etDescriptionInput.getText().toString();
        final String dateTimeISO = Constant.isoDateTimeSDF.format(selectedDateTime);
        final int minutes = obtainReminderMinutes();

        switch (taskType) {
            case TaskConstants.MEETING:
                final String location = etLocationInput.getText().toString();
                return GrassrootRestService.getInstance().getApi().editMeeting(phoneNumber, code, uid,
                        title, description, location, dateTimeISO, minutes);
            case TaskConstants.VOTE:
                return GrassrootRestService.getInstance().getApi().editVote(phoneNumber, code, uid, title,
                        description, dateTimeISO);
       /*     case TaskConstants.TODO:
                return GrassrootRestService.getInstance().getApi().createTodo(phoneNumber, code, groupUid, title,
                        description, dateTimeISO, minutes, memberUids);*/
            default:
                throw new UnsupportedOperationException("Error! Missing task type in call");
        }
    }

    public Call<GenericResponse> setUpCancelApiCall() {

        final String uid = task.getTaskUid();
        final String phoneNumber = PreferenceUtils.getUserPhoneNumber(getContext());
        final String code = PreferenceUtils.getAuthToken(getContext());
        switch (taskType) {
            case TaskConstants.MEETING:
                return GrassrootRestService.getInstance().getApi().cancelMeeting(phoneNumber, code, uid);
            case TaskConstants.VOTE:
                return GrassrootRestService.getInstance().getApi().cancelVote(phoneNumber, code, uid);
       /*     case TaskConstants.TODO:
                return GrassrootRestService.getInstance().getApi().createTodo(phoneNumber, code, groupUid, title,
                        description, dateTimeISO, minutes, memberUids);*/
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

    private String generateSuccessString() {
        switch (taskType) {
            case TaskConstants.MEETING:
                return getActivity().getString(R.string.etsk_meeting_updated_success);
            case TaskConstants.VOTE:
                return getActivity().getString(R.string.etsk_vote_update_success);
            case TaskConstants.TODO:
                return getActivity().getString(R.string.etsk_todo_updated_success);
            default:
                throw new UnsupportedOperationException("Error! Missing task type");
        }
    }

    private String generateConfirmationDialogStrings(){
        switch (taskType) {
            case TaskConstants.MEETING:
                return getActivity().getString(R.string.et_cnfrm_mtg);
            case TaskConstants.VOTE:
                return getActivity().getString(R.string.et_cnfrm_vt);
            case TaskConstants.TODO:
                return getActivity().getString(R.string.et_cnfrm_td);
            default:
                throw new UnsupportedOperationException("Error! Missing task type");
        }

    }


    @BindView(R.id.etsk_txt_ipl)
    TextInputLayout subjectInput;
    @BindView(R.id.etsk_til_location)
    TextInputLayout locationInput;
    @BindView(R.id.txt_deadline_title)
    TextView deadlineTitle;
    @BindView(R.id.etsk_cv_reminder)
    CardView reminderCard;


    private void setUpStrings() {

        locationInput.setVisibility(TaskConstants.MEETING.equals(taskType) ? View.VISIBLE : View.GONE);
        locationCharCounter.setVisibility(TaskConstants.MEETING.equals(taskType) ? View.VISIBLE : View.GONE);

        switch (taskType) {
            case TaskConstants.MEETING:
                btTaskUpdate.setText(R.string.uMeeting);
                btCancelTask.setText(R.string.caMeeting);
                break;
            case TaskConstants.VOTE:
                subjectInput.setHint(getContext().getString(R.string.cvote_subject));
                deadlineTitle.setText(R.string.cvote_datetime);
                reminderCard.setVisibility(View.GONE);
                descriptionBody.setVisibility(View.VISIBLE);
                ivDescExpandIcon.setImageResource(R.drawable.ic_arrow_up);
                descriptionInput.setHint(getContext().getString(R.string.cvote_desc_hint));
                descriptionCard.setCardBackgroundColor(ContextCompat.getColor(getContext(), R.color.white));
                btTaskUpdate.setText(R.string.uVote);
                btCancelTask.setText(R.string.caVote);
                break;
            case TaskConstants.TODO:
                subjectInput.setHint(getContext().getString(R.string.ctodo_subject));
                deadlineTitle.setText(R.string.ctodo_datetime);
                descriptionInput.setHint(getContext().getString(R.string.ctodo_desc_hint));
                btTaskUpdate.setText(R.string.uTodo);
                btCancelTask.setText(R.string.caTodo);
                break;
            default:
                throw new UnsupportedOperationException("Error! Fragment must have valid task type");
        }
    }

    @BindView(R.id.etsk_iv_expand_alert)
    ImageView ivExpandReminders;
    private ValueAnimator remindersExpandAnimator;
    private ValueAnimator remindersContractAnimator;

    private void setUpReminderAnimators() {
        remindersExpandAnimator = Utilities.createSlidingAnimator(rlReminderBody, true);
        remindersContractAnimator = Utilities.createSlidingAnimator(rlReminderBody, true);
        remindersContractAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                rlReminderBody.setVisibility(View.GONE);
            }
        });
    }

    @OnClick(R.id.etsk_reminder_header)
    public void expandableReminderHeader() {

        if (remindersExpandAnimator == null) {
            setUpReminderAnimators();
        }

        if (rlReminderBody.getVisibility() != View.VISIBLE) {
            ivExpandReminders.setImageResource(R.drawable.ic_arrow_up);
            rlReminderBody.setVisibility(View.VISIBLE);
            remindersExpandAnimator.start();
        } else {
            ivExpandReminders.setImageResource(R.drawable.ic_arrow_down);
            remindersContractAnimator.start();
        }
    }

    @OnCheckedChanged({R.id.etsk_sw_one_hour, R.id.etsk_sw_half_day, R.id.etsk_sw_one_day})
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

    private ValueAnimator descriptionExpandAnimator;
    private ValueAnimator descriptionContractAnimator;

    @BindView(R.id.etsk_cv_description)
    CardView descriptionCard;
    @BindView(R.id.etsk_rl_desc_body)
    RelativeLayout descriptionBody;
    @BindView(R.id.etsk_til_desc)
    TextInputLayout descriptionInput;
    @BindView(R.id.etsk_desc_expand)
    ImageView ivDescExpandIcon;

    private void setUpDescriptionAnimators() {
        descriptionExpandAnimator = Utilities.createSlidingAnimator(descriptionBody, true);
        descriptionContractAnimator = Utilities.createSlidingAnimator(descriptionBody, false);

        descriptionExpandAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
                etDescriptionInput.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                etDescriptionInput.setVisibility(View.VISIBLE);
            }
        });

        descriptionContractAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
                etDescriptionInput.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                etDescriptionInput.setVisibility(View.GONE);
            }
        });
    }

    @OnClick(R.id.etsk_cv_description)
    public void expandDescription() {
        if (descriptionExpandAnimator == null) {
            setUpDescriptionAnimators();
        }
        if (descriptionBody.getVisibility() == View.GONE) {
            descriptionBody.setVisibility(View.VISIBLE);
            ivDescExpandIcon.setImageResource(R.drawable.ic_arrow_up);
            descriptionExpandAnimator.start();
        } else {
            ivDescExpandIcon.setImageResource(R.drawable.ic_arrow_down);
            descriptionContractAnimator.start();
        }
    }

    @BindView(R.id.etsk_subject_count)
    TextView subjectCharCounter;
    @BindView(R.id.etsk_desc_count)
    TextView descriptionCharCounter;
    @BindView(R.id.etsk_location_count)
    TextView locationCharCounter;

    @OnTextChanged(R.id.etsk_et_title)
    public void changeCharCounter(CharSequence s) {
        subjectCharCounter.setText(s.length() + " / 35"); // todo : externalize
    }

    @OnTextChanged(R.id.etsk_et_location)
    public void changeLocCharCounter(CharSequence s) {
        locationCharCounter.setText(s.length() + " / 35");
    }

    @OnTextChanged(R.id.etsk_et_description)
    public void changeDescCounter(CharSequence s) {
        descriptionCharCounter.setText(s.length() + " / 250"); // todo : externalize
    }


}
