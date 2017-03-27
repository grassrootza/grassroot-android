package org.grassroot.android.fragments;

import android.app.Activity;
import android.app.DatePickerDialog;
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
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import org.grassroot.android.R;
import org.grassroot.android.fragments.dialogs.ConfirmCancelDialogFragment;
import org.grassroot.android.fragments.dialogs.NetworkErrorDialogFragment;
import org.grassroot.android.interfaces.NavigationConstants;
import org.grassroot.android.interfaces.TaskConstants;
import org.grassroot.android.models.Member;
import org.grassroot.android.models.TaskModel;
import org.grassroot.android.services.ApplicationLoader;
import org.grassroot.android.services.TaskService;
import org.grassroot.android.utils.Constant;
import org.grassroot.android.utils.ErrorUtils;
import org.grassroot.android.utils.IntentUtils;
import org.grassroot.android.utils.NetworkUtils;
import org.grassroot.android.utils.rxutils.SingleObserverFromConsumer;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnEditorAction;
import butterknife.OnTextChanged;
import butterknife.Unbinder;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Consumer;

/**
 * Created by paballo on 2016/06/21.
 */
public class EditTaskFragment extends Fragment implements DatePickerDialog.OnDateSetListener, TimePickerDialog.OnTimeSetListener {

    private static final String TAG = EditTaskFragment.class.getSimpleName();
    private static final int changeColor = ContextCompat.getColor(ApplicationLoader.applicationContext, R.color.red);

    private TaskModel task;
    private String taskType;

    // Java 7 loveliness on testing for difference means hold two of these
    private final Calendar changedCalendar = Calendar.getInstance();
    private final Calendar priorCalendar = Calendar.getInstance();

    private ViewGroup vContainer; // note : come back and check this for memory leaks
    private List<Member> selectedMembers;

    private Unbinder unbinder;

    @BindView(R.id.etsk_til_location) TextInputLayout locationInput;
    @BindView(R.id.etsk_et_title) TextInputEditText etTitleInput;
    @BindView(R.id.etsk_et_location) TextInputEditText etLocationInput;
    @BindView(R.id.etsk_et_description) TextInputEditText etDescriptionInput;

    @BindView(R.id.etsk_deadline_date) TextView dateDisplayed;
    @BindView(R.id.etsk_cv_time) CardView timeCard;
    @BindView(R.id.etsk_deadline_time) TextView timeDisplayed;

    @BindView(R.id.etsk_desc_header) TextView descriptionHeader;
    @BindView(R.id.etsk_rl_desc_body) RelativeLayout descriptionBody;
    @BindView(R.id.etsk_desc_expand) ImageView ivDescExpandIcon;

    @BindView(R.id.etsk_tv_assign_label) TextView tvInviteeLabel;

    @BindView(R.id.etsk_btn_update_task) Button btTaskUpdate;

    @BindView(R.id.progressBar) ProgressBar progressBar;

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

        task = b.getParcelable(TaskConstants.TASK_ENTITY_FIELD);
        if (task == null) {
            throw new UnsupportedOperationException("Error! Fragment called without valid task");
        }

        taskType = task.getType();
        changedCalendar.setTime(task.getDeadlineDate());
        priorCalendar.setTime(task.getDeadlineDate());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View viewToReturn = inflater.inflate(R.layout.fragment_edit_task, container, false);
        unbinder = ButterKnife.bind(this, viewToReturn);
        vContainer = container;
        populateFields();
        fetchAssignedMembers();
        return viewToReturn;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }

    private void populateFields() {

        locationInput.setVisibility(TaskConstants.MEETING.equals(taskType) ? View.VISIBLE : View.GONE);
        locationCharCounter.setVisibility(TaskConstants.MEETING.equals(taskType) ? View.VISIBLE : View.GONE);

        etTitleInput.setText(task.getTitle());
        etDescriptionInput.setText(task.getDescription());
        dateDisplayed.setText(String.format(getString(R.string.etsk_vote_date), TaskConstants.dateDisplayWithoutHours.format(task.getDeadlineDate())));
        timeDisplayed.setText(String.format(getString(R.string.etsk_mtg_time), TaskConstants.timeDisplayWithoutDate.format(task.getDeadlineDate())));

        // we may want to use plurals here in future, but a 1-person meeting/vote/etc seems somewhat implausible
        switch (task.getType()) {
            case TaskConstants.MEETING:
                etTitleInput.setHint(R.string.cmtg_title_hint);
                etLocationInput.setText(task.getLocation());
                etLocationInput.setHint(R.string.cmtg_location_hint);
                tvInviteeLabel.setText(task.getWholeGroupAssigned() ? getString(R.string.etsk_mtg_invite) :
                        String.format(getString(R.string.etsk_mtg_invite_x), task.getAssignedMemberCount()));
                btTaskUpdate.setText(R.string.etsk_bt_mtg_save);
                break;
            case TaskConstants.VOTE:
                etTitleInput.setHint(R.string.cvote_subject);
                descriptionBody.setVisibility(View.VISIBLE);
                ivDescExpandIcon.setImageResource(R.drawable.ic_arrow_up);
                tvInviteeLabel.setText(task.getWholeGroupAssigned() ? getString(R.string.etsk_vote_invite) :
                        String.format(getString(R.string.etsk_vote_invite_x), task.getAssignedMemberCount()));
                btTaskUpdate.setText(R.string.etsk_bt_vote_save);
                break;
            case TaskConstants.TODO:
                etTitleInput.setHint(R.string.ctodo_subject);
                timeCard.setVisibility(View.GONE);
                descriptionBody.setVisibility(View.VISIBLE);
                ivDescExpandIcon.setImageResource(R.drawable.ic_arrow_up);
                tvInviteeLabel.setText(task.getWholeGroupAssigned() ? getString(R.string.etsk_todo_invite) :
                        String.format(getString(R.string.etsk_todo_invite_x), task.getAssignedMemberCount()));
                btTaskUpdate.setText(R.string.etsk_bt_todo_save);
                break;
            default:
                throw new UnsupportedOperationException("Error! Fragment must have valid task type");
        }
    }

    private void fetchAssignedMembers() {
        selectedMembers = new ArrayList<>();
        if (!task.getWholeGroupAssigned()) {
            TaskService.getInstance().fetchAssignedMembers(task.getTaskUid(), taskType)
                .subscribe(new Consumer<List<Member>>() {
                    @Override
                    public void accept(@NonNull List<Member> members) {
                        selectedMembers = new ArrayList<>(members);
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(@NonNull Throwable throwable) {
                        // just display a snackbar (this is not a vital function, so that will be enough)
                        Snackbar.make(tvInviteeLabel, R.string.connect_error_members_assigned, Snackbar.LENGTH_SHORT);
                    }
                });
        }
    }

    @OnEditorAction(R.id.etsk_et_title)
    public boolean onTitleNextOrDone(TextInputEditText view, int actionId, KeyEvent event) {
        if (!etTitleInput.getText().toString().trim().equals(task.getTitle())) {
            etTitleInput.setTextColor(changeColor);
        }

        if (!taskType.equals(TaskConstants.MEETING)) {
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_NEXT) {
                hideKeyboard(view);
                etTitleInput.clearFocus();
                return true;
            }
        }

        return false;
    }

    @OnEditorAction(R.id.etsk_et_location)
    public boolean onLocationNextOrDone(TextInputEditText view, int actionId, KeyEvent event) {
        if (taskType.equals(TaskConstants.MEETING)) {
            if (!etLocationInput.getText().toString().trim().equals(task.getLocation())) {
                etLocationInput.setTextColor(changeColor);
            }

            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_NEXT) {
                hideKeyboard(view);
                etLocationInput.clearFocus();
                return true;
            }
        }
        return false;
    }

    // note: this assumes the view being passed has focus ...
    private void hideKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    @OnClick(R.id.etsk_cv_date)
    public void launchDatePicker() {
        DatePickerDialog dialog = new DatePickerDialog(getActivity(), this,
            changedCalendar.get(Calendar.YEAR),
            changedCalendar.get(Calendar.MONTH),
            changedCalendar.get(Calendar.DAY_OF_MONTH));
        dialog.show();
    }

    @Override
    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
        changedCalendar.set(year, monthOfYear, dayOfMonth);
        if (year != priorCalendar.get(Calendar.YEAR) || monthOfYear != priorCalendar.get(Calendar.MONTH) ||
            dayOfMonth != priorCalendar.get(Calendar.DAY_OF_MONTH)) {
            dateDisplayed.setText(String.format(getString(R.string.etsk_mtg_date_changed), TaskConstants.dateDisplayWithoutHours.format(changedCalendar.getTime())));
            dateDisplayed.setTextColor(changeColor);
        }
    }

    @OnClick(R.id.etsk_cv_time)
    public void launchTimePicker() {
        TimePickerDialog dialog = new TimePickerDialog(getActivity(), this, changedCalendar.get(Calendar.HOUR_OF_DAY), changedCalendar.get(Calendar.MINUTE), true);
        dialog.show();
    }

    @Override
    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
        changedCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
        changedCalendar.set(Calendar.MINUTE, minute);
        if (hourOfDay != priorCalendar.get(Calendar.HOUR_OF_DAY) || minute != priorCalendar.get(Calendar.MINUTE)) {
            timeDisplayed.setText(String.format(getString(R.string.etsk_mtg_time_changed), TaskConstants.timeDisplayWithoutDate.format(changedCalendar.getTime())));
            timeDisplayed.setTextColor(changeColor);
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

    @OnEditorAction(R.id.etsk_et_description)
    public boolean onDescriptionDone(TextInputEditText view, int actionId, KeyEvent event) {
        if (event != null) {
            if (!event.isShiftPressed() || !event.isAltPressed()) {
                if ((actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_NEXT || actionId == EditorInfo.IME_NULL)
                        && event.getAction() == KeyEvent.ACTION_DOWN) {
                    hideKeyboard(view);
                    view.clearFocus();
                    if (!etDescriptionInput.getText().toString().trim().equals(task.getDescription())) {
                        descriptionHeader.setTextColor(changeColor);
                    }
                    return true;
                }
            }
        }
        return false;
    }

    @OnClick(R.id.etsk_cv_notify)
    public void changeMemberSelection() {
        Intent i = IntentUtils.memberSelectionIntent(getActivity(), task.getParentUid(), EditTaskFragment.class.getCanonicalName(),
                new ArrayList<>(selectedMembers));
        startActivityForResult(i, NavigationConstants.SELECT_MEMBERS);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK && requestCode == NavigationConstants.SELECT_MEMBERS) {
            if (data == null) {
                throw new UnsupportedOperationException("Error! Null data from select members activity");
            }

            List<Member> newlySelectedMembers = data.getParcelableArrayListExtra(Constant.SELECTED_MEMBERS_FIELD);
            if (!selectedMembers.equals(newlySelectedMembers)) {
                selectedMembers = newlySelectedMembers;
                final String pluralMember = getResources().getQuantityString(R.plurals.numberMembersSelected,
                    selectedMembers.size(), selectedMembers.size());
                tvInviteeLabel.setText(pluralMember);
                tvInviteeLabel.setTextColor(changeColor);
            }
        }
    }

    @OnClick(R.id.etsk_btn_update_task)
    public void confirmAndUpdate() {
        int memberCount = (selectedMembers != null && !selectedMembers.isEmpty()) ? selectedMembers.size() : task.getAssignedMemberCount();
        final String message = String.format(getString(R.string.etsk_change_confirm), memberCount);
        ConfirmCancelDialogFragment dialogFragment = ConfirmCancelDialogFragment
                .newInstance(message, new ConfirmCancelDialogFragment.ConfirmDialogListener() {
                    @Override
                    public void doConfirmClicked() {
                        updateTask();
                    }
                });
        dialogFragment.show(getFragmentManager(), "confirm");
    }

    public void updateTask() {
        progressBar.setVisibility(View.VISIBLE);
        TaskModel model = generateTaskObject();
        TaskService.getInstance().editTask(model, selectedMembers, AndroidSchedulers.mainThread())
            .subscribe(new Consumer<String>() {
                @Override
                public void accept(@NonNull String s) {
                    progressBar.setVisibility(View.GONE);
                    generateSuccessIntent();
                }
            }, new Consumer<Throwable>() {
                @Override
                public void accept(@NonNull Throwable e) {
                    progressBar.setVisibility(View.GONE);
                    switch (e.getMessage()) {
                        case NetworkUtils.CONNECT_ERROR:
                        case NetworkUtils.OFFLINE_SELECTED:
                            handleNetworkFail();
                            break;
                        default:
                            Snackbar.make(vContainer, ErrorUtils.serverErrorText(e), Snackbar.LENGTH_LONG).show();
                    }
                }
            });
    }

    private void generateSuccessIntent(){
        Intent i = new Intent();
        final String message = generateSuccessString();
        Toast.makeText(ApplicationLoader.applicationContext, message, Toast.LENGTH_LONG).show();
        i.putExtra(Constant.SUCCESS_MESSAGE, generateSuccessString());
        getActivity().setResult(Activity.RESULT_OK, i);
        getActivity().finish();
    }

    private void handleNetworkFail() {
        NetworkErrorDialogFragment.newInstance(R.string.connect_error_edit_task, progressBar,
            new SingleObserverFromConsumer<>(new Consumer<String>() {
                @Override
                public void accept(@NonNull String s) {
                    progressBar.setVisibility(View.GONE);
                    if (s.equals(NetworkUtils.CONNECT_ERROR)) {
                        Snackbar.make(vContainer, R.string.connect_error_failed_retry, Snackbar.LENGTH_SHORT).show();
                    } else {
                        updateTask();
                    }
                }
            })).show(getFragmentManager(), "error");
    }

    private TaskModel generateTaskObject() {
        final String title = etTitleInput.getText().toString();
        final String description = etDescriptionInput.getText().toString().trim();

        TaskModel model = new TaskModel();
        model.setDescription(description);
        model.setTitle(title);
        model.setTaskUid(task.getTaskUid());
        model.setParentUid(task.getParentUid());
        model.setUpdateTime(changedCalendar.getTime().getTime());
        model.setLocation(etLocationInput.getText().toString());
        model.setType(taskType);
        model.setEdited(true);
        model.setLocal(!NetworkUtils.isOnline(getContext()));
        model.setParentLocal(task.isParentLocal());
        model.setReply(task.getReply());
        model.setCanAction(task.isCanAction());
        model.setCanEdit(task.isCanEdit());
        model.setCanMarkCompleted(task.isCanMarkCompleted());
        model.setCreatedByUserName(task.getCreatedByUserName());
        model.setDeadlineISO(Constant.isoDateTimeSDF.format(new Date(model.getUpdateTime())));
        return model;
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
