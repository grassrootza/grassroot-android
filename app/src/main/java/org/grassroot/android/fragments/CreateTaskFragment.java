package org.grassroot.android.fragments;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.CardView;
import android.support.v7.widget.SwitchCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import org.grassroot.android.R;
import org.grassroot.android.activities.ActionCompleteActivity;
import org.grassroot.android.activities.GrassrootExtraActivity;
import org.grassroot.android.events.TaskAddedEvent;
import org.grassroot.android.fragments.dialogs.AccountLimitDialogFragment;
import org.grassroot.android.fragments.dialogs.DatePickerFragment;
import org.grassroot.android.fragments.dialogs.TimePickerFragment;
import org.grassroot.android.fragments.dialogs.TokenExpiredDialogFragment;
import org.grassroot.android.interfaces.GroupConstants;
import org.grassroot.android.interfaces.NavigationConstants;
import org.grassroot.android.interfaces.TaskConstants;
import org.grassroot.android.models.Member;
import org.grassroot.android.models.TaskModel;
import org.grassroot.android.models.exceptions.ApiCallException;
import org.grassroot.android.models.helpers.RealmString;
import org.grassroot.android.services.ApplicationLoader;
import org.grassroot.android.services.GroupService;
import org.grassroot.android.services.TaskService;
import org.grassroot.android.utils.Constant;
import org.grassroot.android.utils.ErrorUtils;
import org.grassroot.android.utils.IntentUtils;
import org.grassroot.android.utils.NetworkUtils;
import org.grassroot.android.utils.PermissionUtils;
import org.grassroot.android.utils.RealmUtils;
import org.grassroot.android.utils.Utilities;
import org.grassroot.android.utils.image.LocalImageUtils;
import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
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
import butterknife.OnEditorAction;
import butterknife.OnTextChanged;
import butterknife.Unbinder;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Consumer;
import io.realm.RealmList;

import static android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI;

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

    @BindView(R.id.ctsk_ll_photo) ViewGroup addImageButton;

    @BindView(R.id.ctsk_cv_voteoptions) CardView voteOptionsInputCard;
    @BindView(R.id.cvote_options_input) EditText voteOptionsInputField;
    @BindView(R.id.cvote_options_list) TextView voteOptionsList;
    @BindView(R.id.cvote_yes_no) RadioButton voteYesNoButton;

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
            RealmUtils.loadGroupMembers(groupUid, true).subscribe(new Consumer<List<Member>>() {
                @Override
                public void accept(List<Member> members) {
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
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == NavigationConstants.SELECT_MEMBERS) {
                setAssignedMembers(data);
            } else if (requestCode == GALLERY_RESULT_INT) {
                if (data != null) {
                    Uri selectedImage = data.getData();
                    final String localImagePath = LocalImageUtils.getLocalFileNameFromURI(selectedImage);
                    checkImageSizeAndConfirmInclusion(localImagePath, LocalImageUtils.getMimeType(selectedImage));
                }
            } else if (requestCode == CAMERA_RESULT_INT) {
                LocalImageUtils.addImageToGallery(pathOfImageToInclude);
                checkImageSizeAndConfirmInclusion(pathOfImageToInclude, "image/jpeg");
            }
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

    // todo : validate vote options non-empty
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
        final Observable<TaskModel> sendToServer = TaskService.getInstance()
                .sendTaskToServer(model, AndroidSchedulers.mainThread());
        final Consumer<TaskModel> onNext = new Consumer<TaskModel>() {
            @Override
            public void accept(@NonNull TaskModel taskModel) throws Exception {
                progressDialog.dismiss();
                finishAndLaunchDoneFragment(taskModel);
            }
        };
        final Consumer<Throwable> onError = new Consumer<Throwable>() {
            @Override
            public void accept(@NonNull Throwable throwable) throws Exception {
                progressDialog.dismiss();
                if (ErrorUtils.isTokenError(throwable)) {
                    TokenExpiredDialogFragment.showTokenExpiredDialogs(getFragmentManager(),
                            null,
                            sendToServer,
                            onNext,
                            this).subscribe();
                } else if (throwable instanceof ApiCallException) {
                    handleError((ApiCallException) throwable, model);
                } else {
                    throwable.printStackTrace();
                }
            }
        };

        sendToServer.subscribe(onNext, onError);
    }

    private void finishAndLaunchDoneFragment(TaskModel model) {
        final String successString = generateSuccessString();
        Intent i = TaskService.getInstance().generateTaskDoneIntent(getContext(), model, successString);
        if (TaskConstants.MEETING.equals(model.getType()) && NetworkUtils.isOnline()) {
            i.putExtra(ActionCompleteActivity.MEETING_PUBLIC_BUTTONS, true);
        }
        EventBus.getDefault().post(new TaskAddedEvent(model, successString));
        startActivity(i);
        getActivity().finish();
    }

    private void handleError(ApiCallException e, TaskModel model) {
        final String type = e.getMessage();
        if (NetworkUtils.CONNECT_ERROR.equals(type)) {
            finishAndLaunchDoneFragment(model);
        } else {
            if (ErrorUtils.TODO_LIMIT_REACHED.equals(e.errorTag)) {
                AccountLimitDialogFragment.showAccountLimitDialog(getFragmentManager(), R.string.account_todo_limit_reached)
                        .subscribe(new Consumer<String>() {
                            @Override
                            public void accept(String s) {
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
        model.setImageLocalUrl(pathOfImageToInclude);
        model.setImageMimeType(mimeTypeOfImage);

        final RealmList<RealmString> realmMemberUids = RealmUtils.convertListOfStringInRealmListOfString(new ArrayList<>(memberUids));
        model.setMemberUIDS(realmMemberUids);

        if (TaskConstants.VOTE.equals(taskType) && !voteYesNoButton.isChecked()) {
            model.setTags(RealmUtils.convertListOfStringInRealmListOfString(voteOptions));
        }

        Log.e(TAG, "generated task model, returning it");

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
                return NetworkUtils.isOnline() ? getActivity().getString(R.string.ctsk_meeting_success_ask_public)
                        : (includeWholeGroup ? getActivity().getString(R.string.ctsk_meeting_all_success)
                        : String.format(getActivity().getString(R.string.ctsk_meeting_assigned_success), assignedMembers.size()));
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
    /*
    SECTION : METHODS FOR TRIGGERING PHOTO/MODIFY/CANCEL
     */
    private String pathOfImageToInclude;
    private String mimeTypeOfImage;
    private static final int GALLERY_RESULT_INT = 1001;
    private static final int CAMERA_RESULT_INT = 1002;

    @OnClick(R.id.ctsk_ll_photo)
    public void taskPhoto() {
        if (!PermissionUtils.checkFilePermissions()) {
            PermissionUtils.requestFilePermissions(getActivity());
        } else {
            launchImageOptions();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (PermissionUtils.checkContactsPermissionGranted(requestCode, grantResults)) {
            launchImageOptions();
        }
    }

    private void launchImageOptions() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setItems(R.array.vt_add_photo_options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if (i == 0) {
                    try {
                        startActivityForResult(generateCameraIntent(), CAMERA_RESULT_INT);
                    } catch (IOException e) {
                        // show a toast
                        Toast.makeText(getContext(), "Sorry, error loading camera", Toast.LENGTH_SHORT).show();
                    } catch (IllegalArgumentException e) {
                        Log.e(TAG, e.getMessage());
                        Toast.makeText(getContext(), "Illegal argument exception!", Toast.LENGTH_SHORT).show();
                    }
                } else if (i == 1) {
                    startActivityForResult(generateGalleryIntent(), GALLERY_RESULT_INT);
                }
                dialogInterface.dismiss();
            }
        }).setCancelable(true);
        builder.show();
    }

    private Intent generateGalleryIntent() {
        return new Intent(Intent.ACTION_PICK, EXTERNAL_CONTENT_URI);
    }

    private Intent generateCameraIntent() throws IllegalArgumentException, IOException {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getActivity().getPackageManager()) != null) {
            File photoFile = LocalImageUtils.createImageFileForCamera();
            // Android Studio says this null check is unnecessary, but can't fully trust, so keeping it
            if (photoFile != null) {
                Uri currentPhotoUri = FileProvider.getUriForFile(getContext(),
                        "org.grassroot.android.fileprovider",
                        photoFile);
                pathOfImageToInclude = photoFile.getAbsolutePath();
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, currentPhotoUri);

                List<ResolveInfo> resInfoList = getContext().getPackageManager().queryIntentActivities(takePictureIntent,
                        PackageManager.MATCH_DEFAULT_ONLY);
                for (ResolveInfo resolveInfo : resInfoList) {
                    String packageName = resolveInfo.activityInfo.packageName;
                    getContext().grantUriPermission(packageName, currentPhotoUri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                }
            } else {
                throw new IOException("Error! File came back null");
            }
        }
        return takePictureIntent;
    }

    private void checkImageSizeAndConfirmInclusion(final String localImagePath, final String mimeType) {
        double imageSizeMb = (double) LocalImageUtils.getImageFileSize(localImagePath) / (1024 * 1024);
        Log.e(TAG, "image size on disk: " + LocalImageUtils.getImageFileSize(localImagePath));
        final DecimalFormat df = new DecimalFormat("#.##");
        final String message = getString(R.string.vt_photo_size, df.format(imageSizeMb));
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext())
                .setMessage(message)
                .setPositiveButton(R.string.vt_photo_upload_full, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        pathOfImageToInclude = localImagePath;
                        mimeTypeOfImage = mimeType;
                        Log.e(TAG, "okay, we are going to upload it as is");
                    }
                })
                .setNeutralButton(R.string.vt_photo_upload_small, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        pathOfImageToInclude = LocalImageUtils.getCompressedFileFromImage(localImagePath, false);
                        mimeTypeOfImage = mimeType;
                    }
                })
                .setNegativeButton(R.string.vt_photo_upload_later, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        pathOfImageToInclude = null;
                        mimeTypeOfImage = null;
                        dialogInterface.cancel();
                    }
                })
                .setCancelable(true);
        builder.show();
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
    @BindView(R.id.ctsk_alias_notice)
    TextView aliasNotice;

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
                addImageButton.setVisibility(View.VISIBLE);
                voteOptionsInputCard.setVisibility(View.GONE);
                textRemindOption0.setText(TaskConstants.meetingReminderDesc[0]);
                textRemindOption1.setText(TaskConstants.meetingReminderDesc[1]);
                textRemindOption2.setText(TaskConstants.meetingReminderDesc[2]);
                // defaults to one day ahead
                switchReminderOption0.setChecked(true);
                toggleSwitches(switchReminderOption0);

                break;
            case TaskConstants.VOTE:
                addImageButton.setVisibility(View.GONE);
                voteOptionsInputCard.setVisibility(View.VISIBLE);
                voteYesNoButton.setChecked(true);
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
                addImageButton.setVisibility(View.GONE);
                voteOptionsInputCard.setVisibility(View.GONE);
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

        checkForAlias();
    }

    private void checkForAlias() {
        // don't fully trust Android so just including try catch
        try {
            if (!TextUtils.isEmpty(groupUid) && GroupService.getInstance().checkUserhasAliasInGroup(groupUid)) {
                aliasNotice.setText(getString(R.string.group_alias_present,
                        GroupService.getInstance().getUserAliasInGroup(groupUid)));
                aliasNotice.setVisibility(View.VISIBLE);
            }
        } catch (Exception e) {
            Log.e(TAG, "error checking alias");
            e.printStackTrace();
        }
    }

    List<String> voteOptions = new ArrayList<>();

    @OnCheckedChanged(R.id.cvote_multi_option)
    public void onMultiOptionSelected(CompoundButton button, boolean checked) {
        voteOptionsInputField.setVisibility(checked ? View.VISIBLE : View.GONE);
        voteOptionsList.setVisibility(checked ? View.VISIBLE : View.GONE);
    }

    @OnEditorAction(R.id.cvote_options_input)
    public boolean onVoteInputDone(int actionId, KeyEvent event) {
        if (actionId == EditorInfo.IME_ACTION_DONE || (event.getAction() == KeyEvent.ACTION_DOWN &&
                event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
            handleEnterOfVoteOption();
            return true;
        }
        return false;
    }

    private void handleEnterOfVoteOption() {
        voteOptions.add(voteOptionsInputField.getText().toString());
        voteOptionsList.setText(TextUtils.join("\n", voteOptions));
        voteOptionsInputField.setText("");
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
