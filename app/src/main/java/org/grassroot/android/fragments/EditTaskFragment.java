package org.grassroot.android.fragments;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.CardView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.TimePicker;

import org.grassroot.android.R;
import org.grassroot.android.events.TaskUpdatedEvent;
import org.grassroot.android.interfaces.NetworkErrorDialogListener;
import org.grassroot.android.interfaces.TaskConstants;
import org.grassroot.android.models.TaskModel;
import org.grassroot.android.models.TaskResponse;
import org.grassroot.android.services.GrassrootRestService;
import org.grassroot.android.utils.Constant;
import org.grassroot.android.utils.ErrorUtils;
import org.grassroot.android.utils.MenuUtils;
import org.grassroot.android.utils.PreferenceUtils;
import org.greenrobot.eventbus.EventBus;

import java.util.Calendar;
import java.util.Date;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnTextChanged;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Created by paballo on 2016/06/21.
 */
public class EditTaskFragment extends Fragment implements DatePickerDialog.OnDateSetListener, TimePickerDialog.OnTimeSetListener {

    private static final String TAG = EditTaskFragment.class.getCanonicalName();

    private TaskModel task;
    private String taskType;

    private Date updatedDate;
    private Date updatedTime;

    private ViewGroup vContainer;

    @BindView(R.id.etsk_title_ipl)
    TextInputLayout subjectInput;
    @BindView(R.id.etsk_til_location)
    TextInputLayout locationInput;
    @BindView(R.id.etsk_et_title)
    TextInputEditText etTitleInput;
    @BindView(R.id.etsk_et_location)
    TextInputEditText etLocationInput;
    @BindView(R.id.etsk_et_description)
    TextInputEditText etDescriptionInput;

    @BindView(R.id.etsk_deadline_date)
    TextView dateDisplayed;
    @BindView(R.id.etsk_cv_time)
    CardView timeCard;
    @BindView(R.id.etsk_deadline_time)
    TextView timeDisplayed;

    @BindView(R.id.etsk_cv_description)
    CardView descriptionCard;
    @BindView(R.id.etsk_rl_desc_body)
    RelativeLayout descriptionBody;
    @BindView(R.id.etsk_til_desc)
    TextInputLayout descriptionInput;
    @BindView(R.id.etsk_desc_expand)
    ImageView ivDescExpandIcon;

    @BindView(R.id.etsk_tv_assign_label)
    TextView tvInviteeLabel;

    @BindView(R.id.etsk_btn_update_task)
    Button btTaskUpdate;

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
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View viewToReturn = inflater.inflate(R.layout.fragment_edit_task, container, false);
        ButterKnife.bind(this, viewToReturn);
        this.vContainer = container;
        populateFields();
        return viewToReturn;
    }

    private void populateFields() {

        etTitleInput.setEnabled(false);
        etLocationInput.setEnabled(false);
        etDescriptionInput.setEnabled(false);

        locationInput.setVisibility(TaskConstants.MEETING.equals(taskType) ? View.VISIBLE : View.GONE);
        locationCharCounter.setVisibility(TaskConstants.MEETING.equals(taskType) ? View.VISIBLE : View.GONE);

        switch (task.getType()) {
            case TaskConstants.MEETING:
                etTitleInput.setText(task.getTitle());
                etTitleInput.setHint(R.string.cmtg_title_hint);
                etLocationInput.setText(task.getLocation());
                etLocationInput.setHint(R.string.cmtg_location_hint);
                etDescriptionInput.setText(task.getDescription());

                dateDisplayed.setText(String.format(getString(R.string.etsk_mtg_date), TaskConstants.dateDisplayWithoutHours.format(task.getDeadlineDate())));
                timeDisplayed.setText(String.format(getString(R.string.etsk_mtg_time), TaskConstants.timeDisplayWithoutDate.format(task.getDeadlineDate())));

                tvInviteeLabel.setText(task.getWholeGroupAssigned() ? R.string.etsk_mtg_invite : R.string.etsk_mtg_invite_x);

                btTaskUpdate.setText(R.string.etsk_bt_mtg_save);
                break;
            case TaskConstants.VOTE:
                etTitleInput.setText(task.getTitle());
                etDescriptionInput.setText(task.getDescription());
                dateDisplayed.setText(String.format(getString(R.string.etsk_vote_date), TaskConstants.dateDisplayWithoutHours.format(task.getDeadlineDate())));
                timeDisplayed.setText(String.format(getString(R.string.etsk_vote_time), TaskConstants.timeDisplayWithoutDate.format(task.getDeadlineDate())));
                descriptionBody.setVisibility(View.VISIBLE);
                ivDescExpandIcon.setImageResource(R.drawable.ic_arrow_up);
                etDescriptionInput.setText(task.getDescription());
                btTaskUpdate.setText(R.string.etsk_bt_vote_save);
                break;
            case TaskConstants.TODO:
                etTitleInput.setText(task.getTitle());
                etDescriptionInput.setText(task.getDescription());
                dateDisplayed.setText(String.format(getString(R.string.etsk_todo_date), TaskConstants.dateDisplayWithoutHours.format(task.getDeadlineDate())));
                timeCard.setVisibility(View.GONE);
                btTaskUpdate.setText(R.string.etsk_bt_todo_save);
                break;
            default:
                throw new UnsupportedOperationException("Error! Fragment must have valid task type");
        }
    }

    // todo : figure out why this is unpredictable and often not catching
    @OnClick(R.id.etsk_title_ipl)
    public void setTitleEnable() {
        Log.e(TAG, "title clicked, enabling");
        etTitleInput.setEnabled(true);
        etTitleInput.requestFocus();
    }

    @OnClick(R.id.etsk_et_location)
    public void enableLocationInput() {
        Log.e(TAG, "location clicked");
        etLocationInput.setEnabled(true);
        etLocationInput.requestFocus();
    }

    @OnClick(R.id.etsk_cv_date)
    public void launchDatePicker() {
        final Calendar c = Calendar.getInstance();
        c.setTime(task.getDeadlineDate());
        DatePickerDialog dialog = new DatePickerDialog(getActivity(), R.style.AppTheme, this, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
        dialog.show();
    }

    @Override
    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
        final Calendar c = Calendar.getInstance();
        c.set(year, monthOfYear, dayOfMonth);
        updatedDate = c.getTime();
        Log.e(TAG, "date set: " + TaskConstants.dateDisplayWithoutHours.format(updatedDate));
    }

    @OnClick(R.id.etsk_cv_time)
    public void launchTimePicker() {
        final Calendar c = Calendar.getInstance();
        c.setTime(task.getDeadlineDate());
        TimePickerDialog dialog = new TimePickerDialog(getActivity(), R.style.AppTheme, this, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true);
        dialog.show();
    }

    @Override
    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
        final Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, hourOfDay);
        c.set(Calendar.MINUTE, minute);
        updatedTime = c.getTime();
        Log.e(TAG, "time set: " + TaskConstants.dateDisplayFormatWithHours.format(updatedTime));
    }

    @OnClick(R.id.etsk_btn_update_task)
    public void validateAndUpdate() {
        if (etTitleInput.getText().toString().trim().equals("")) {
            ErrorUtils.showSnackBar(vContainer, "Please enter a subject", Snackbar.LENGTH_LONG, "", null);
        } else if (TaskConstants.MEETING.equals(taskType) && etLocationInput.getText().toString().trim().equals("")) {
            ErrorUtils.showSnackBar(vContainer, "Please enter a location", Snackbar.LENGTH_LONG, "", null);
        } else {
            updateTask();
        }
    }

    @OnClick(R.id.etsk_cv_description)
    public void expandDescription() {
        if (descriptionBody.getVisibility() == View.GONE) {
            ivDescExpandIcon.setImageResource(R.drawable.ic_arrow_up);
            descriptionBody.setVisibility(View.VISIBLE);
        } else {
            ivDescExpandIcon.setImageResource(R.drawable.ic_arrow_down);
            descriptionBody.setVisibility(View.GONE);
        }
    }

    @OnClick(R.id.etsk_cv_notify)
    public void changeMemberSelection() {
        Intent pickMember = MenuUtils.memberSelectionIntent(getActivity(), task.getParentUid(), TAG, null);
        startActivityForResult(pickMember, Constant.activitySelectGroupMembers);
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
                ErrorUtils.connectivityError(getActivity(), R.string.error_no_network, new NetworkErrorDialogListener() {
                    @Override
                    public void retryClicked() {
                        updateTask();
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
        final String dateTimeISO = Constant.isoDateTimeSDF.format(dateDisplayed);

        switch (taskType) {
            case TaskConstants.MEETING:
                final String location = etLocationInput.getText().toString();
                return GrassrootRestService.getInstance().getApi().editMeeting(phoneNumber, code, uid,
                        title, description, location, dateTimeISO);
            case TaskConstants.VOTE:
                return GrassrootRestService.getInstance().getApi().editVote(phoneNumber, code, uid, title,
                        description, dateTimeISO);
            case TaskConstants.TODO:
                return GrassrootRestService.getInstance().getApi().editTodo(phoneNumber, code, title,
                        dateTimeISO, null);
            default:
                throw new UnsupportedOperationException("Error! Missing task type in call");
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

    @BindView(R.id.etsk_subject_count)
    TextView subjectCharCounter;
    @BindView(R.id.etsk_desc_count)
    TextView descriptionCharCounter;
    @BindView(R.id.etsk_location_count)
    TextView locationCharCounter;

    @OnTextChanged(R.id.etsk_et_title)
    public void changeCharCounter(CharSequence s) {
        subjectCharCounter.setText(s.length() + " / 35");
    }

    @OnTextChanged(R.id.etsk_et_location)
    public void changeLocCharCounter(CharSequence s) {
        locationCharCounter.setText(s.length() + " / 35");
    }

    @OnTextChanged(R.id.etsk_et_description)
    public void changeDescCounter(CharSequence s) {
        descriptionCharCounter.setText(s.length() + " / 250");
    }

}
