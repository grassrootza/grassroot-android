package org.grassroot.android.fragments;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v4.widget.TextViewCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.grassroot.android.R;
import org.grassroot.android.activities.EditTaskActivity;
import org.grassroot.android.activities.ImageDisplayActivity;
import org.grassroot.android.adapters.MemberListAdapter;
import org.grassroot.android.adapters.MtgRsvpAdapter;
import org.grassroot.android.events.TaskCancelledEvent;
import org.grassroot.android.events.TaskUpdatedEvent;
import org.grassroot.android.fragments.dialogs.ConfirmCancelDialogFragment;
import org.grassroot.android.fragments.dialogs.NetworkErrorDialogFragment;
import org.grassroot.android.interfaces.TaskConstants;
import org.grassroot.android.models.ImageRecord;
import org.grassroot.android.models.Member;
import org.grassroot.android.models.ResponseTotalsModel;
import org.grassroot.android.models.RsvpListModel;
import org.grassroot.android.models.TaskModel;
import org.grassroot.android.models.exceptions.ApiCallException;
import org.grassroot.android.services.ApplicationLoader;
import org.grassroot.android.services.SharingService;
import org.grassroot.android.services.TaskService;
import org.grassroot.android.utils.ErrorUtils;
import org.grassroot.android.utils.NetworkUtils;
import org.grassroot.android.utils.RealmUtils;
import org.grassroot.android.utils.RetryWithDelay;
import org.grassroot.android.utils.image.LocalImageUtils;
import org.grassroot.android.utils.image.NetworkImageUtils;
import org.grassroot.android.utils.rxutils.SingleObserverFromConsumer;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

import static android.app.Activity.RESULT_OK;
import static android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI;

public class ViewTaskFragment extends Fragment {

    private static final String TAG = ViewTaskFragment.class.getSimpleName();

    private static final int CAMERA_RESULT_INT = 1001;
    private static final int GALLERY_RESULT_INT = 1002;

    private static final int VIEWING_PHOTOS_RESULT_INT = 1003;

    private TaskModel task;
    private String taskType;
    private String taskUid;
    private boolean canViewResponses;
    private MtgRsvpAdapter mtgRsvpAdapter;
    private MemberListAdapter memberListAdapter;

    private ViewGroup mContainer;
    private Unbinder unbinder;
    private boolean viewsBound;

    private long photoCount;
    private String currentPhotoPath;

    @BindView(R.id.vt_title) TextView tvTitle;
    @BindView(R.id.vt_header) TextView tvHeader;
    @BindView(R.id.vt_location) TextView tvLocation;
    @BindView(R.id.vt_posted_by) TextView tvPostedBy;
    @BindView(R.id.vt_date_time) TextView tvDateTime;
    @BindView(R.id.vt_description) TextView tvDescription;

    @BindView(R.id.vt_cv_respond) CardView respondCard;
    @BindView(R.id.vt_response_header) TextView tvResponseHeader;
    @BindView(R.id.vt_ll_response_icons) LinearLayout llResponseIcons;
    @BindView(R.id.vt_left_response) ImageView icRespondPositive;
    @BindView(R.id.vt_right_response) ImageView icRespondNegative;

    @BindView(R.id.vt_cv_response_list) CardView cvResponseList;
    @BindView(R.id.vt_responses_count) TextView tvResponsesCount;
    @BindView(R.id.vt_ic_responses_expand) ImageView icResponsesExpand;
    @BindView(R.id.vt_mtg_response_list) RecyclerView rcResponseList;
    @BindView(R.id.vt_vote_response_details) LinearLayout llVoteResponseDetails;
    @BindView(R.id.td_rl_response_icon) RelativeLayout rlResponse;
    @BindView(R.id.bt_td_respond) ImageView btTodoRespond;

    @BindView(R.id.vt_ll_photo) ViewGroup takePhotoButton;
    @BindView(R.id.vt_bt_view_photos) Button btViewPhotos;
    @BindView(R.id.vt_bt_modify_cancel) ViewGroup modifyCancelButtons;
    @BindView(R.id.vt_bt_modify) Button btModifyTask;
    @BindView(R.id.vt_bt_cancel) Button btCancelTask;

    @BindView(R.id.progressBar) ProgressBar progressBar;
    ProgressDialog progressDialog;

    // use this if creating or calling the fragment without whole task object (e.g., entering from notification)
    public static ViewTaskFragment newInstance(String taskType, String taskUid) {
        ViewTaskFragment fragment = new ViewTaskFragment();
        Bundle args = new Bundle();
        args.putString(TaskConstants.TASK_TYPE_FIELD, taskType);
        args.putString(TaskConstants.TASK_UID_FIELD, taskUid);
        fragment.setArguments(args);
        return fragment;
    }

    // use this if creating or calling the fragment with whole task object
    public static ViewTaskFragment newInstance(TaskModel task) {
        ViewTaskFragment fragment = new ViewTaskFragment();
        Bundle args = new Bundle();
        args.putParcelable(TaskConstants.TASK_ENTITY_FIELD, task);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        if (getArguments() != null) {
            Bundle args = getArguments();
            task = args.getParcelable(TaskConstants.TASK_ENTITY_FIELD);
            if (task == null) {
                taskType = args.getString(TaskConstants.TASK_TYPE_FIELD);
                taskUid = args.getString(TaskConstants.TASK_UID_FIELD);
            } else {
                taskType = task.getType();
                taskUid = task.getTaskUid();
            }

            if (taskType == null || taskUid == null) {
                Log.e(TAG, "Error! View task fragment with type or UID missing");
                startActivity(ErrorUtils.gracefulExitToTasks(getActivity()));
            }

            if (task == null) {
                task = RealmUtils.loadObjectFromDB(TaskModel.class, "taskUid", taskUid);
            }

            Log.e(TAG, "tags? : " + task.getTags());

            photoCount = 0; // until set otherwise
            canViewResponses = false;
        } else {
            throw new UnsupportedOperationException(
                    "Error! View task fragment initiated without arguments");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View viewToReturn = inflater.inflate(R.layout.fragment_view_task, container, false);
        unbinder = ButterKnife.bind(this, viewToReturn);
        viewsBound = true;
        EventBus.getDefault().register(this);
        mContainer = container;
        if (task == null) {// only the case if task was not in DB, e.g., entering from notification
            fetchTaskDetailsFromNetwork();
        } else {
            setUpViews(task);
        }
        return viewToReturn;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        EventBus.getDefault().unregister(this);
        viewsBound = false;
        unbinder.unbind();
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        boolean taskInFuture = (task == null || task.isInFuture());
        if (menu.findItem(R.id.mi_icon_filter) != null)
            menu.findItem(R.id.mi_icon_filter).setVisible(false);
        if (menu.findItem(R.id.mi_icon_sort) != null)
            menu.findItem(R.id.mi_icon_sort).setVisible(false);
        if (menu.findItem(R.id.mi_view_members) != null)
            menu.findItem(R.id.mi_view_members).setVisible(false);
        if (menu.findItem(R.id.mi_group_settings) != null)
            menu.findItem(R.id.mi_group_settings).setVisible(false);
        if (menu.findItem(R.id.mi_group_unsubscribe) != null)
            menu.findItem(R.id.mi_group_unsubscribe).setVisible(false);
        if (menu.findItem(R.id.mi_add_members) != null)
            menu.findItem(R.id.mi_add_members).setVisible(false);
        if (menu.findItem(R.id.mi_remove_members) != null)
            menu.findItem(R.id.mi_remove_members).setVisible(false);
        if (menu.findItem(R.id.mi_view_join_code) != null)
            menu.findItem(R.id.mi_view_join_code).setVisible(false);
        if (menu.findItem(R.id.mi_change_desc) != null)
            menu.findItem(R.id.mi_change_desc).setVisible(false);
        if (menu.findItem(R.id.action_search) != null)
            menu.findItem(R.id.action_search).setVisible(false);
        if (menu.findItem(R.id.mi_refresh_screen) != null)
            menu.findItem(R.id.mi_refresh_screen).setVisible(false);

        if (menu.findItem(R.id.mi_share_default) != null)
            menu.findItem(R.id.mi_share_default).setVisible(taskInFuture);

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if ((item.getItemId() == R.id.mi_share_default)) {
            showAndHandleShareOptions();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    private void showAndHandleShareOptions() {
        if (SharingService.jumpStraightToOtherIntent()) {
            generateShareIntent(SharingService.OTHER);
        } else {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(R.string.share_title)
                .setItems(SharingService.itemsForMultiChoice(), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    generateShareIntent(SharingService.sharePackageFromItemSelected(which));
                }
            });
            builder.create().show();
        }
    }

    private void generateShareIntent(String packageName){
        Intent i = new Intent(getActivity(), SharingService.class);
        i.putExtra(SharingService.TASK_TAG, task);
        i.putExtra(SharingService.APP_SHARE_TAG, packageName);
        i.putExtra(SharingService.ACTION_TYPE,SharingService.TYPE_SHARE);
        getActivity().startService(i);
    }

    private Intent generateMapsIntent() {
        if (taskType.equals(TaskConstants.MEETING) && task != null) {
            Uri mapsIntentUri = Uri.parse("geo:0,0?q=" + Uri.encode(task.getLocation()));
            Intent intent = new Intent(Intent.ACTION_VIEW, mapsIntentUri);
            if (intent.resolveActivity(ApplicationLoader.applicationContext.getPackageManager()) != null) {
                return intent;
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    private void fetchTaskDetailsFromNetwork() {
        task = RealmUtils.loadObjectFromDB(TaskModel.class, "taskUid", taskUid);
        if (!NetworkUtils.isOnline(getContext())) {
            if (task != null) {
                setUpViews(task);
            } else {
                // todo : show a dialogue box (network error listener thing)
            }
        } else {
            progressBar.setVisibility(View.VISIBLE);
            TaskService.getInstance().fetchAndStoreTask(taskUid, taskType, AndroidSchedulers.mainThread())
                .subscribe(new Consumer<String>() {
                    @Override
                    public void accept(@NonNull String s) {
                        if (NetworkUtils.FETCHED_SERVER.equals(s)) {
                            task = RealmUtils.loadObjectFromDB(TaskModel.class, "taskUid", taskUid);
                            setUpViews(task);
                            progressBar.setVisibility(View.GONE);
                        } else {
                            if (task != null) {
                                setUpViews(task);
                                final String errorMsg = NetworkUtils.CONNECT_ERROR;
                            } else {
                                // todo : show an error dialogue
                            }
                        }
                    }
                });
        }
    }

    private void setMeetingRsvpView() {

        mtgRsvpAdapter = new MtgRsvpAdapter();
        rcResponseList.setLayoutManager(new LinearLayoutManager(getContext()));
        rcResponseList.setAdapter(mtgRsvpAdapter);
        rcResponseList.setItemAnimator(null);

        TaskService.getInstance().fetchMeetingRsvps(taskUid)
            .subscribe(new Consumer<RsvpListModel>() {
                @Override
                public void accept(@NonNull RsvpListModel meetingResponses) throws Exception {
                    if (viewsBound) {
                        tvResponsesCount.setText(String.format(getString(R.string.vt_mtg_response_count),
                                meetingResponses.getNumberInvited(), meetingResponses.getNumberYes()));
                        if (meetingResponses.isCanViewRsvps()) {
                            icResponsesExpand.setVisibility(View.VISIBLE);
                            mtgRsvpAdapter.setMapOfResponses(meetingResponses.getRsvpResponses());
                            canViewResponses = true;
                            mtgRsvpAdapter.notifyDataSetChanged();
                        } else {
                            icResponsesExpand.setVisibility(View.GONE);
                            canViewResponses = false;
                            cvResponseList.setClickable(false);
                        }
                    }
                }
            }, new Consumer<Throwable>() {
                @Override
                public void accept(@NonNull Throwable throwable) throws Exception {
                    if (viewsBound) {
                        tvResponsesCount.setText(R.string.vt_mtg_response_error); // add a button for retry in future
                        icResponsesExpand.setVisibility(View.GONE);
                        cvResponseList.setClickable(false);
                    }

                    if (throwable instanceof ApiCallException) {
                        if (NetworkUtils.CONNECT_ERROR.equals(throwable.getMessage())) {
                            // to remain somewhat discreet use the snackbar here, rather than dialog
                            ErrorUtils.networkErrorSnackbar(mContainer, R.string.connect_error_view_task_snackbar,
                                new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        setMeetingRsvpView();
                                    }
                                });
                        } else {
                            Snackbar.make(mContainer, ErrorUtils.serverErrorText(((ApiCallException) throwable).errorTag),
                                Snackbar.LENGTH_SHORT).show();
                        }
                    }
            }
        });
    }

    private void setVoteResponseView() {
        tvResponsesCount.setText(task.getDeadlineDate().after(new Date()) ? R.string.vt_vote_count_open
            : R.string.vt_vote_count_closed);
        TaskService.getInstance().fetchVoteTotals(taskUid).subscribe(new Consumer<ResponseTotalsModel>() {
            @Override
            public void accept(@NonNull ResponseTotalsModel responseTotalsModel) throws Exception {
                if (viewsBound) {
                    TextView tvYes = (TextView) llVoteResponseDetails.findViewById(R.id.count_yes);
                    TextView tvNo = (TextView) llVoteResponseDetails.findViewById(R.id.count_no);
                    TextView tvAbstain = (TextView) llVoteResponseDetails.findViewById(R.id.count_abstain);
                    TextView tvNoResponse = (TextView) llVoteResponseDetails.findViewById(R.id.count_no_response);

                    tvYes.setText(String.valueOf(responseTotalsModel.getYes()));
                    tvNo.setText(String.valueOf(responseTotalsModel.getNo()));
                    tvAbstain.setText(String.valueOf(responseTotalsModel.getAbstained()));
                    tvNoResponse.setText(String.valueOf(responseTotalsModel.getNumberNoReply()));

                    canViewResponses = true;
                }
            }
        }, new Consumer<Throwable>() {
            @Override
            public void accept(@NonNull Throwable throwable) throws Exception {
                if (viewsBound) {
                    tvResponsesCount.setText(R.string.vt_vote_totals_error); // add a button for retry in future
                    icResponsesExpand.setVisibility(View.GONE);
                    cvResponseList.setClickable(false);
                }

                if (throwable instanceof ApiCallException) {
                    if (NetworkUtils.CONNECT_ERROR.equals(throwable.getMessage())) {
                        ErrorUtils.networkErrorSnackbar(mContainer, R.string.connect_error_view_task_snackbar,
                            new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    setVoteResponseView();
                                }
                            });
                    } else {
                        Snackbar.make(mContainer, ErrorUtils.serverErrorText(((ApiCallException) throwable).errorTag),
                            Snackbar.LENGTH_SHORT).show();
                    }
                }
            }

        });

    }

    private void setUpToDoAssignedMemberView() {
        memberListAdapter = new MemberListAdapter(getActivity(), true);
        rcResponseList.setLayoutManager(new LinearLayoutManager(getContext()));
        rcResponseList.setAdapter(memberListAdapter);
        rcResponseList.setItemAnimator(null);

        TaskService.getInstance().fetchAssignedMembers(taskUid, TaskConstants.TODO)
            .subscribe(new Consumer<List<Member>>() {
                @Override
                public void accept(@NonNull List<Member> members) throws Exception {
                    if (viewsBound) {
                        if (!members.isEmpty()) {
                            Log.e(TAG, "returned these members : " + members.toString());
                            memberListAdapter.setMembers(members);
                            canViewResponses = true;
                            tvResponsesCount.setText(R.string.vt_todo_members_assigned);
                        } else {
                            tvResponsesCount.setText(R.string.vt_todo_group_assigned);
                            icResponsesExpand.setVisibility(View.GONE);
                            cvResponseList.setClickable(false);
                        }
                    }
                }
            }, new Consumer<Throwable>() {
                @Override
                public void accept(@NonNull Throwable throwable) throws Exception {
                    if (viewsBound) {
                        tvResponsesCount.setText(R.string.vt_todo_assigned_error);
                        icResponsesExpand.setVisibility(View.GONE);
                        cvResponseList.setClickable(false);
                    }

                    if (throwable instanceof ApiCallException) {
                        if (NetworkUtils.CONNECT_ERROR.equals(throwable.getMessage())) {
                            ErrorUtils.networkErrorSnackbar(mContainer, R.string.connect_error_view_task_snackbar,
                                new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    setUpToDoAssignedMemberView();
                                }
                            });
                        } else {
                            Snackbar.make(mContainer, ErrorUtils.serverErrorText(((ApiCallException) throwable).errorTag), Snackbar.LENGTH_SHORT)
                                .show();
                        }
                    }
            }
        });
    }

    private void setUpViews(TaskModel task) {
        tvDescription.setVisibility(TextUtils.isEmpty(task.getDescription()) ? View.GONE : View.VISIBLE);
        modifyCancelButtons.setVisibility(task.isCanEdit() ? View.VISIBLE : View.GONE);

        switch (taskType) {
            case TaskConstants.MEETING:
                setViewForMeeting(task);
                break;
            case TaskConstants.VOTE:
                setViewForVote(task);
                break;
            case TaskConstants.TODO:
                setViewForToDo(task);
                break;
        }

    }

    private void setViewForMeeting(final TaskModel task) {
        tvTitle.setText(task.isInFuture() ? R.string.vt_mtg_title : R.string.vt_mtg_title_past);
        tvHeader.setText(task.getTitle());
        tvLocation.setVisibility(View.VISIBLE);

        tvLocation.setText(String.format(getString(R.string.vt_mtg_location), task.getLocation()));
        if (generateMapsIntent() != null) {
            tvLocation.setTextColor(ContextCompat.getColor(getContext(), R.color.md_teal_700));
            tvLocation.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    openLocationInMaps();
                }
            });
        }

        tvPostedBy.setText(String.format(getString(R.string.vt_mtg_posted), task.getName()));
        TextViewCompat.setTextAppearance(tvPostedBy, R.style.CardViewFinePrint);

        final boolean inFuture = task.getDeadlineDate().after(new Date());
        final int dateColor = inFuture ? R.color.dark_grey_text : R.color.red;
        tvDateTime.setText(inFuture ? String.format(getString(R.string.vt_mtg_datetime),
                TaskConstants.dateDisplayWithDayName.format(task.getDeadlineDate()))
                : String.format(getString(R.string.vt_mtg_date_past),
                TaskConstants.dateDisplayWithoutHours.format(task.getDeadlineDate())));
        tvDateTime.setTextColor(ContextCompat.getColor(getActivity(), dateColor));

        if (task.isCreatedByUser()) {
            tvResponseHeader.setText(R.string.vt_mtg_called_by_user);
            llResponseIcons.setVisibility(View.GONE);
            tvResponseHeader.setTypeface(null, Typeface.NORMAL);
            tvResponsesCount.setTypeface(null, Typeface.BOLD);
        } else if (task.canAction()) {
            tvResponseHeader.setText(!task.hasResponded() ? getString(R.string.vt_mtg_responseq)
                    : textHasRespondedCanChange());
            llResponseIcons.setVisibility(View.VISIBLE);
            setUpResponseIconsForEvent();
        } else {
            final String suffix = !task.hasResponded() ? getString(R.string.vt_mtg_no_response)
                    : task.respondedYes() ? getString(R.string.vt_mtg_attended)
                    : getString(R.string.vt_mtg_notattend);
            tvResponseHeader.setText(String.format(getString(R.string.vt_mtg_response_past), suffix));
            llResponseIcons.setVisibility(View.GONE);
            diminishResponseCard();
        }

        if (task.isCanEdit()) {
            btModifyTask.setText(R.string.vt_mtg_modify);
            btCancelTask.setText(R.string.vt_mtg_cancel);
        }

        setMeetingRsvpView();
        setViewPhotoButton();
    }

    private void setViewPhotoButton() {
        // todo : start using local DB for a bunch of these
        TaskService.getInstance().countTaskImages(taskType, taskUid).subscribe(new Consumer<Long>() {
            @Override
            public void accept(@NonNull Long aLong) {
                updatePhotoCount(aLong);
            }
        }, new Consumer<Throwable>() {
            @Override
            public void accept(@NonNull Throwable throwable) {
                disablePhotoButton();
            }
        });
    }

    private void updatePhotoCount(Long count) {
        if (count != null && count > 0) {
            Log.e(TAG, "prior photo count: " + photoCount + ", new count: " + count);
            photoCount = count;
            btViewPhotos.setEnabled(true);
            btViewPhotos.setTextColor(ContextCompat.getColor(getContext(), R.color.primaryColor));
            btViewPhotos.setText(getString(R.string.vt_mtg_view_photo_count, count));
        } else {
            photoCount = 0;
            btViewPhotos.setText(R.string.vt_mtg_view_no_photos);
            disablePhotoButton();
        }
    }

    private void disablePhotoButton() {
        btViewPhotos.setEnabled(false);
        btViewPhotos.setTextColor(ContextCompat.getColor(getContext(), R.color.md_grey_400));
    }

    private void openLocationInMaps() {
        // monitor user reaction to this (i.e., do we need dialog box before launching or can remove ...)
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setMessage(R.string.vt_mtg_maps_open)
            .setPositiveButton(R.string.vt_mtg_loc_dial_okay, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    startActivity(generateMapsIntent());
                }
            })
            .setNegativeButton(R.string.alert_cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            })
            .setCancelable(true)
            .show();
    }

    @BindView(R.id.vt_vote_options) Spinner voteOptionsSelect;
    private boolean hasRespondedToVote = false; // because, Android -- else the spinner goes into an infinte loop (!!!) -- wtf

    private void setViewForVote(final TaskModel task) {
        tvTitle.setText(task.isInFuture() ? R.string.vt_vote_title : R.string.vt_vote_title_past);
        tvHeader.setText(task.getTitle());
        tvLocation.setVisibility(View.GONE);
        tvPostedBy.setText(String.format(getString(R.string.vt_vote_posted), task.getName()));
        tvDateTime.setText(String.format(getString(R.string.vt_vote_datetime),
                TaskConstants.dateDisplayFormatWithHours.format(task.getDeadlineDate())));

        if (task.canAction()) {
            boolean multiOption = !isYesNoVote(RealmUtils.convertListOfRealmStringInListOfString(task.getTags()));
            if (multiOption) {
                tvResponseHeader.setText(R.string.vt_vote_response_multi);
                ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(),
                        android.R.layout.simple_spinner_item, task.getTagStrings());
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                llResponseIcons.setVisibility(View.GONE);
                voteOptionsSelect.setAdapter(adapter);
                voteOptionsSelect.setSelection(TextUtils.isEmpty(task.getReply()) ? 0 :
                        task.getTagStrings().indexOf(task.getReply()), false);
                voteOptionsSelect.setVisibility(View.VISIBLE);
                voteOptionsSelect.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                        if (!hasRespondedToVote) {
                            hasRespondedToVote = true;
                            respondToTask(task.getTagStrings().get(i));
                        }
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> adapterView) {

                    }
                });
            } else {
                tvResponseHeader.setText(task.hasResponded() ?
                        getString(R.string.vt_vote_responseq) : textHasRespondedCanChange());
                llResponseIcons.setVisibility(View.VISIBLE);
                setUpResponseIconsForEvent();
            }
        } else {
            final String suffix = !task.hasResponded() ? getString(R.string.vt_vote_no_response)
                    : task.respondedYes() ? getString(R.string.vt_vote_voted_yes)
                    : task.respondedNo() ? getString(R.string.vt_vote_voted_no)
                    : getString(R.string.vt_vote_voted_other, task.getReply());
            tvResponseHeader.setText(String.format(getString(R.string.vt_vote_response_past), suffix));
            llResponseIcons.setVisibility(View.GONE);
            diminishResponseCard();
        }

        takePhotoButton.setVisibility(View.GONE);
        btViewPhotos.setVisibility(View.GONE);

        if (task.isCanEdit()) {
            btModifyTask.setVisibility(View.VISIBLE);
            btModifyTask.setText(R.string.vt_vote_modify);
            btCancelTask.setVisibility(View.GONE);
        }

        setVoteResponseView();
    }

    private boolean isYesNoVote(final List<String> tags) {
        return !(tags != null && !tags.isEmpty()) || listContainsIgnoreCase(tags, "yes") && listContainsIgnoreCase(tags, "no");
    }

    private boolean listContainsIgnoreCase(final List<String> strings, String term) {
        for (String string : strings) {
            if (string.trim().equalsIgnoreCase(term.trim())) {
                return true;
            }
        }
        return false;
    }

    private void setViewForToDo(final TaskModel task) {
        tvTitle.setText(task.isInFuture() ? R.string.vt_todo_title : R.string.vt_todo_title_past);
        tvHeader.setText(task.getTitle());
        tvPostedBy.setText(String.format(getString(R.string.vt_vote_posted), task.getName()));
        tvDateTime.setText(String.format(getString(R.string.vt_todo_datetime),
                TaskConstants.dateDisplayFormatWithHours.format(task.getDeadlineDate())));
        rlResponse.setVisibility(task.hasResponded() ? View.GONE : View.VISIBLE);
        llResponseIcons.setVisibility(View.GONE);

        if (!task.isInFuture() && !task.hasResponded()) {
            tvResponseHeader.setText(R.string.vt_todo_overdue);
        } else if (task.hasResponded()) {
            tvResponseHeader.setText(R.string.vt_todo_completed);
        } else if (!task.hasResponded() && task.canAction()) {
            tvResponseHeader.setText(R.string.vt_todo_pending);
        } else {
            tvResponseHeader.setText(R.string.vt_todo_pending);
        }

        if (task.isCanAction()) {
            setUpResponseIconsForTodo();
        }

        if (task.isCanEdit()) {
            btModifyTask.setText(R.string.vt_todo_modify);
            btCancelTask.setText(R.string.vt_todo_cancel);
        }

        takePhotoButton.setVisibility(View.GONE); // for now
        btViewPhotos.setVisibility(View.GONE);

        setUpToDoAssignedMemberView();
    }

    private void diminishResponseCard() {
        respondCard.setCardElevation(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2,
                getResources().getDisplayMetrics()));
    }

    private void setUpResponseIconsForEvent() {

        icRespondPositive.setImageResource(task.respondedYes() ?
            R.drawable.respond_yes_active : R.drawable.respond_yes_inactive);

        icRespondNegative.setImageResource(task.respondedNo() ?
            R.drawable.respond_no_active : R.drawable.respond_no_inactive);

        if (!task.canAction()) {
            icRespondPositive.setEnabled(false);
            icRespondNegative.setEnabled(false);
        } else {
            icRespondPositive.setEnabled(!task.respondedYes());
            icRespondNegative.setEnabled(!task.respondedNo());
        }
    }

    private void setUpResponseIconsForTodo() {
        if (!task.hasResponded()) {
            btTodoRespond.setImageResource(R.drawable.respond_confirm_inactive);
            btTodoRespond.setEnabled(true);
        } else{
            btTodoRespond.setImageResource(R.drawable.respond_confirm_active);
            btTodoRespond.setEnabled(false);
        }
    }

    public void respondToTask(final String response) {
        progressBar.setVisibility(View.VISIBLE);
        TaskService.getInstance().respondToTask(task.getTaskUid(), response, null)
            .subscribe(new Consumer<String>() {
                @Override
                public void accept(@NonNull String s) throws Exception {
                    progressBar.setVisibility(View.GONE);
                    task = RealmUtils.loadObjectFromDB(TaskModel.class, "taskUid", task.getTaskUid());
                    if (NetworkUtils.SAVED_SERVER.equals(s)) {
                        handleSuccessfulReply(response);
                    } else if (NetworkUtils.SAVED_OFFLINE_MODE.equals(s)) {
                        handleSavedOffline(response);
                    }
                }
            }, new Consumer<Throwable>() {
                @Override
                public void accept(@NonNull Throwable e) throws Exception {
                    progressBar.setVisibility(View.GONE);
                    if (NetworkUtils.CONNECT_ERROR.equals(e.getMessage())) {
                        task = RealmUtils.loadObjectFromDB(TaskModel.class, "taskUid", task.getTaskUid());
                        handleSavedOffline(response);
                    } else {
                        Snackbar.make(mContainer, ErrorUtils.serverErrorText(e), Snackbar.LENGTH_SHORT).show();
                    }
                }
        });
    }

    @OnClick(R.id.vt_left_response)
    public void doRespondYes() {
        respondToTask(TaskConstants.RESPONSE_YES);
    }

    @OnClick(R.id.vt_right_response)
    public void doRespondNo() {
        respondToTask(TaskConstants.RESPONSE_NO);
    }

    @OnClick(R.id.bt_td_respond)
    public void completeTodo() {
        respondToTask(TaskConstants.TODO_DONE);
    }

    private void resetIconsAfterResponse() {
        switch (task.getType()) {
            case TaskConstants.TODO:
                btTodoRespond.setImageResource(R.drawable.respond_confirm_active);
                btTodoRespond.setEnabled(false);
                break;
            case TaskConstants.VOTE:
                task.setCanAction(false);
                setUpResponseIconsForEvent();
                break;
            default:
				setUpResponseIconsForEvent();
                break;
        }
    }

    private void handleSuccessfulReply(String response) {
        Toast.makeText(ApplicationLoader.applicationContext, snackBarMsg(response), Toast.LENGTH_SHORT).show();
        if (viewsBound) {
            resetIconsAfterResponse();
            tvResponseHeader.setText(snackBarMsg(response));
        }
    }

    private void handleSavedOffline(String action) {
        handleNoNetworkResponse(action, R.string.connect_error_task_responding);
    }

    private void handleNoNetworkResponse(final String retryTag, int snackbarMsg) {
        NetworkErrorDialogFragment.newInstance(snackbarMsg, progressBar, new SingleObserverFromConsumer<>(new Consumer<String>() {
            @Override
            public void accept(@NonNull String s) throws Exception {
                progressBar.setVisibility(View.GONE);
                if (s.equals(NetworkUtils.CONNECT_ERROR)) {
                    Snackbar.make(mContainer, R.string.connect_error_failed_retry, Snackbar.LENGTH_SHORT).show();
                } else {
                    switch (retryTag) {
                        case "RESPOND_YES":
                            doRespondYes();
                            break;
                        case "RESPOND_NO":
                            doRespondNo();
                            break;
                        case "COMPLETE_TODO":
                            completeTodo();
                            break;
                        default:
                            fetchTaskDetailsFromNetwork();
                    }
                }
            }
        })).show(getFragmentManager(), "dialog");
    }

    private String textHasRespondedCanChange() {
        switch (taskType) {
            case TaskConstants.MEETING:
                final String suffix = task.respondedYes() ? getString(R.string.vt_mtg_attending)
                        : getString(R.string.vt_mtg_not_attending);
                return String.format(getString(R.string.vt_mtg_responded_can_action), suffix);
            case TaskConstants.VOTE:
                final String suffix2 =
                        task.respondedYes() ? getString(R.string.vt_vote_yes) : getString(R.string.vt_vote_no);
                return String.format(getString(R.string.vt_vote_responded_can_action), suffix2);
            case TaskConstants.TODO:
            default:
                return "";
        }
    }

    /*
    SECTION : VIEWING DETAILS ON RSVP LIST, VOTE TOTALS, ETC.
     */

    @OnClick(R.id.vt_cv_response_list)
    public void slideOutDetails() {
        Log.e(TAG, "viewing responses!");
        if (canViewResponses) {
            switch (taskType) {
                case TaskConstants.MEETING:
                    toggleResponseList();
                    break;
                case TaskConstants.VOTE:
                    toggleVoteDetails();
                    break;
                case TaskConstants.TODO:
                    toggleResponseList();
                    break;
            }
        }
    }

    public void toggleResponseList() {
        if (rcResponseList.getVisibility() != View.VISIBLE) {
            rcResponseList.setVisibility(View.VISIBLE);
            icResponsesExpand.setImageResource(R.drawable.ic_arrow_up);
        } else {
            rcResponseList.setVisibility(View.GONE);
            icResponsesExpand.setImageResource(R.drawable.ic_arrow_down);
        }
    }

    public void toggleVoteDetails() {
        if (llVoteResponseDetails.getVisibility() != View.VISIBLE) {
            llVoteResponseDetails.setVisibility(View.VISIBLE);
            icResponsesExpand.setImageResource(R.drawable.ic_arrow_up);
        } else {
            llVoteResponseDetails.setVisibility(View.GONE);
            icResponsesExpand.setImageResource(R.drawable.ic_arrow_down);
        }
    }

    /*
    SECTION : METHODS FOR TRIGGERING PHOTO/MODIFY/CANCEL
     */

    @OnClick(R.id.vt_ll_photo)
    public void taskPhoto() {
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
                currentPhotoPath = photoFile.getAbsolutePath();
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

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == GALLERY_RESULT_INT) {
            if (resultCode == RESULT_OK && data != null) {
                Uri selectedImage = data.getData();
                final String localImagePath = LocalImageUtils.getLocalFileNameFromURI(selectedImage);
                checkImageSizeAndConfirmUpload(localImagePath, LocalImageUtils.getMimeType(selectedImage), false);
            }
        } else if (requestCode == CAMERA_RESULT_INT) {
            if (resultCode == RESULT_OK) {
                LocalImageUtils.addImageToGallery(currentPhotoPath);
                checkImageSizeAndConfirmUpload(currentPhotoPath, "image/jpeg", true);
            }
        } else if (requestCode == VIEWING_PHOTOS_RESULT_INT) {
            setViewPhotoButton(); // in case an image was deleted, this will reset the count
        }
    }

    private void checkImageSizeAndConfirmUpload(final String localImagePath, final String mimeType, final boolean tryUploadLongLat) {
        double imageSizeMb = (double) LocalImageUtils.getImageFileSize(localImagePath) / (1024 * 1024);
        Log.e(TAG, "image size on disk: " + LocalImageUtils.getImageFileSize(localImagePath));
        final DecimalFormat df = new DecimalFormat("#.##");
        final String message = getString(R.string.vt_photo_size, df.format(imageSizeMb));
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext())
                .setMessage(message)
                .setPositiveButton(R.string.vt_photo_upload_full, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        uploadImageFromUri(localImagePath, mimeType, tryUploadLongLat);
                    }
                })
                .setNeutralButton(R.string.vt_photo_upload_small, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        String compressedFilePath = LocalImageUtils.getCompressedFileFromImage(localImagePath, false);
                        uploadImageFromUri(compressedFilePath, mimeType, tryUploadLongLat);
                    }
                })
                .setNegativeButton(R.string.vt_photo_upload_later, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.cancel();
                    }
                })
                .setCancelable(true);
        builder.show();
    }

    private void uploadImageFromUri(String localImagePath, String mimeType, boolean tryUploadLongLat) {
        showUploadProgress();
        NetworkImageUtils.uploadTaskImage(task, localImagePath, mimeType, tryUploadLongLat)
                .subscribeOn(Schedulers.io())
                .delay(1L, TimeUnit.SECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<String>() {
                               @Override
                               public void accept(@NonNull String s) throws Exception {
                                   dismissUploadProgress();
                                   Toast.makeText(getContext(), R.string.vt_mtg_photo_succeeded, Toast.LENGTH_SHORT).show();
                                   updatePhotoCount(photoCount + 1);
                                   // Log.e(TAG, "back from upload image, about to call check analysis results");
                                   checkForAnalysisResults(s);
                               }
                           }, new Consumer<Throwable>() {
                               @Override
                               public void accept(@NonNull Throwable throwable) throws Exception{
                                   dismissUploadProgress();
                                   Toast.makeText(getContext(), R.string.vt_mtg_photo_failed, Toast.LENGTH_LONG).show();
                               }
                           });
    }

    // this retries 3 times, over 15 seconds, until it gets a result saying analysis complete (watch for nulls)
    private void checkForAnalysisResults(final String logUid) {
        NetworkImageUtils.checkForImageAnalysis(logUid, taskType)
                .subscribeOn(Schedulers.io())
                .retryWhen(new RetryWithDelay(3, 5000))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<ImageRecord>() {
                    @Override
                    public void accept(@NonNull ImageRecord imageRecord) {
                        if (imageRecord.hasFoundFaces()) {
                            showFaceCountDialog(imageRecord);
                        } else {
                            Log.e(TAG, "nothing found ...");
                        }
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(@NonNull Throwable throwable) {
                        Log.e(TAG, "call failed");
                    }
                });
    }

    private void showFaceCountDialog(final ImageRecord imageRecord) {
        Log.e(TAG, "analysis result found, record: " + imageRecord);
        new AlertDialog.Builder(getContext())
                .setMessage(getString(R.string.vt_photo_face_count, imageRecord.getNumberFaces()))
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Intent intent = viewImageIntent();
                        intent.putExtra(ImageDisplayActivity.OPEN_ON_IMAGE, imageRecord);
                        startActivityForResult(intent, VIEWING_PHOTOS_RESULT_INT);
                    }
                })
                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.cancel();
                    }
                })
                .setCancelable(true)
                .show();
    }

    @OnClick(R.id.vt_bt_view_photos)
    public void viewPhotos() {
        startActivityForResult(viewImageIntent(), VIEWING_PHOTOS_RESULT_INT);
    }

    private Intent viewImageIntent() {
        Intent viewIntent = new Intent(getActivity(), ImageDisplayActivity.class);
        viewIntent.putExtra(TaskConstants.TASK_UID_FIELD, taskUid);
        viewIntent.putExtra(TaskConstants.TASK_TYPE_FIELD, taskType);
        return viewIntent;
    }

    @OnClick(R.id.vt_bt_modify)
    public void modifyTask() {
        if (task.isCanEdit()) {
            // make sure passing latest version of task
            // note: might also just pass UID, but parcelable lighter weight and entity gets passed around a bit
            Intent editMtg = new Intent(getActivity(), EditTaskActivity.class);
            final TaskModel latestVersion = RealmUtils.loadObjectFromDB(TaskModel.class, "taskUid", task.getTaskUid());
            if (latestVersion != null) {
                latestVersion.calcDeadlineDate();
                editMtg.putExtra(TaskConstants.TASK_ENTITY_FIELD, latestVersion);
                startActivityForResult(editMtg, 1);
            } else {
                Snackbar.make(mContainer, R.string.local_error_load_task_for_edit, Snackbar.LENGTH_SHORT).show();
            }
        }
    }

    @OnClick(R.id.vt_bt_cancel)
    public void promptCancel() {
        if (task.isCanEdit()) {
            String dialogMessage = generateConfirmationDialogStrings();
            ConfirmCancelDialogFragment confirmCancelDialogFragment =
                    ConfirmCancelDialogFragment.newInstance(dialogMessage,
                            new ConfirmCancelDialogFragment.ConfirmDialogListener() {
                                @Override
                                public void doConfirmClicked() {
                                    cancelTask();
                                }
                            });
            confirmCancelDialogFragment.show(getFragmentManager(), TAG);
        }
    }

    private void cancelTask() {
        progressBar.setVisibility(View.VISIBLE);
        TaskService.getInstance().cancelTask(taskUid, taskType, AndroidSchedulers.mainThread())
            .subscribe(new Consumer<String>() {
                @Override
                public void accept(@NonNull String s) {
                    progressBar.setVisibility(View.GONE);
                    EventBus.getDefault().post(new TaskCancelledEvent(taskUid));
                }
            }, new Consumer<Throwable>() {
                @Override
                public void accept(@NonNull Throwable e) {
                    progressBar.setVisibility(View.GONE);
                    if (NetworkUtils.CONNECT_ERROR.equals(e.getMessage())) {
                        handleRetryCancel();
                    } else {
                        Snackbar.make(mContainer, ErrorUtils.serverErrorText(e), Snackbar.LENGTH_SHORT).show();
                    }
                }
            });
    }

    private void handleRetryCancel() {
        NetworkErrorDialogFragment.newInstance(R.string.connect_error_task_cancelled, progressBar,
            new SingleObserverFromConsumer<>(new Consumer<String>() {
                @Override
                public void accept(@NonNull String s) throws Exception {
                    progressBar.setVisibility(View.GONE);
                    if (s.equals(NetworkUtils.CONNECT_ERROR)) {
                        Snackbar.make(mContainer, R.string.connect_error_failed_retry, Snackbar.LENGTH_SHORT).show();
                    } else {
                        cancelTask();
                    }
                }
            })).show(getFragmentManager(), "dialog");
    }

    private String generateConfirmationDialogStrings() {
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

    private int snackBarMsg(String response) {
        switch (taskType) {
            case TaskConstants.MEETING:
                return response.equals(TaskConstants.RESPONSE_YES) ? R.string.vt_snackbar_response_attend
                        : R.string.vt_snackbar_response_notattend;
            case TaskConstants.VOTE:
                return response.equals(TaskConstants.RESPONSE_YES) ? R.string.vt_vote_snackbar_yes
                        : R.string.vt_vote_snackbar_option;
            case TaskConstants.TODO:
                return R.string.vt_todo_done;
        }
        return -1;
    }

    private void showUploadProgress() {
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(getContext());
            progressDialog.setMessage(getString(R.string.vt_photo_uploading));
            progressDialog.setIndeterminate(true);
        }
        progressDialog.show();
    }

    private void dismissUploadProgress() {
        if (progressDialog != null) {
            progressDialog.dismiss();
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onTaskUpdated(TaskUpdatedEvent event) {
        TaskModel updatedTask = event.getTask();
        setUpViews(updatedTask);
    }


}