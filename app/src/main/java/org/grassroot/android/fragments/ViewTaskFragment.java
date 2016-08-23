package org.grassroot.android.fragments;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
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
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.grassroot.android.R;
import org.grassroot.android.activities.EditTaskActivity;
import org.grassroot.android.adapters.MemberListAdapter;
import org.grassroot.android.adapters.MtgRsvpAdapter;
import org.grassroot.android.events.TaskCancelledEvent;
import org.grassroot.android.events.TaskUpdatedEvent;
import org.grassroot.android.fragments.dialogs.ConfirmCancelDialogFragment;
import org.grassroot.android.interfaces.NetworkErrorDialogListener;
import org.grassroot.android.interfaces.TaskConstants;
import org.grassroot.android.models.GenericResponse;
import org.grassroot.android.models.Member;
import org.grassroot.android.models.ResponseTotalsModel;
import org.grassroot.android.models.RsvpListModel;
import org.grassroot.android.models.TaskModel;
import org.grassroot.android.models.exceptions.ApiCallException;
import org.grassroot.android.services.GrassrootRestService;
import org.grassroot.android.services.SharingService;
import org.grassroot.android.services.TaskService;
import org.grassroot.android.utils.ErrorUtils;
import org.grassroot.android.utils.NetworkUtils;
import org.grassroot.android.utils.RealmUtils;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.Date;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;

public class ViewTaskFragment extends Fragment {

    private static final String TAG = ViewTaskFragment.class.getCanonicalName();

    private TaskModel task;
    private String taskType;
    private String taskUid;
    private boolean canViewResponses;
    private MtgRsvpAdapter mtgRsvpAdapter;
    private MemberListAdapter memberListAdapter;

    private ViewGroup mContainer;
    private Unbinder unbinder;

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

    @BindView(R.id.vt_bt_modify) Button btModifyTask;
    @BindView(R.id.vt_bt_cancel) Button btCancelTask;

    @BindView(R.id.progressBar) ProgressBar progressBar;

    public ViewTaskFragment() {
    }

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
        EventBus.getDefault().register(this);
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
                throw new UnsupportedOperationException("Error! View task fragment with type or UID missing");
            }

            if (task == null) {
                task = RealmUtils.loadObjectFromDB(TaskModel.class, "taskUid", taskUid);
            }

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
        mContainer = container;

        if (task == null) {
            // only the case if task was not in DB, e.g., entering from notification
            fetchTaskDetailsFromNetwork();
        } else {
            setUpViews(task);
        }
        return viewToReturn;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        boolean taskInFuture = (task == null || task.isInFuture());
        Log.e(TAG, "setting up options menu ... taskInFuture = " + taskInFuture);
        if (menu.findItem(R.id.mi_icon_filter) != null)
            menu.findItem(R.id.mi_icon_filter).setVisible(false);
        if (menu.findItem(R.id.mi_icon_sort) != null)
            menu.findItem(R.id.mi_icon_sort).setVisible(false);
        if (menu.findItem(R.id.mi_view_members) != null)
            menu.findItem(R.id.mi_view_members).setVisible(false);
        if (menu.findItem(R.id.mi_group_settings) != null)
            menu.findItem(R.id.mi_group_settings).setVisible(false);
        if (menu.findItem(R.id.mi_add_members) != null)
            menu.findItem(R.id.mi_add_members).setVisible(false);
        if (menu.findItem(R.id.mi_remove_members) != null)
            menu.findItem(R.id.mi_remove_members).setVisible(false);
        if (menu.findItem(R.id.mi_view_join_code) != null)
            menu.findItem(R.id.mi_view_join_code).setVisible(false);
        if (menu.findItem(R.id.mi_new_task) != null)
            menu.findItem(R.id.mi_new_task).setVisible(false);
        if (menu.findItem(R.id.action_search) != null)
            menu.findItem(R.id.action_search).setVisible(false);

        if (menu.findItem(R.id.mi_share_default) != null)
            menu.findItem(R.id.mi_share_default).setVisible(taskInFuture);

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if ((item.getItemId() == R.id.mi_share_default)) {
            showAndHandleShareOptions();
            return true;
        }else {
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

    private void fetchTaskDetailsFromNetwork() {
        task = RealmUtils.loadObjectFromDB(TaskModel.class, "taskUid", taskUid);
        if (!NetworkUtils.isOnline(getContext())) {
            if (task != null) {
                setUpViews(task);
            } else {
                // show a dialogue box
            }
        } else {
            progressBar.setVisibility(View.VISIBLE);
            TaskService.getInstance().fetchAndStoreTask(taskUid, taskType, AndroidSchedulers.mainThread())
                .subscribe(new Action1<String>() {
                    @Override
                    public void call(String s) {
                        if (NetworkUtils.FETCHED_SERVER.equals(s)) {
                            task = RealmUtils.loadObjectFromDB(TaskModel.class, "taskUid", taskUid);
                            setUpViews(task);
                            progressBar.setVisibility(View.GONE);
                        } else {
                            if (task != null) {
                                setUpViews(task);
                                final String errorMsg = NetworkUtils.CONNECT_ERROR;
                            } else {
                                // show an error dialogue
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

        TaskService.getInstance().fetchMeetingRsvps(taskUid).subscribe(new Subscriber<RsvpListModel>() {
            @Override
            public void onNext(RsvpListModel meetingResponses) {
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

            @Override
            public void onError(Throwable e) {
                tvResponsesCount.setText(R.string.vt_mtg_response_error); // add a button for retry in future
                icResponsesExpand.setVisibility(View.GONE);
                cvResponseList.setClickable(false);

                if (e instanceof ApiCallException) {
                    if (NetworkUtils.CONNECT_ERROR.equals(e.getMessage())) {
                        ErrorUtils.networkErrorSnackbar(mContainer, R.string.connect_error_view_task_snackbar,
                            new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    setMeetingRsvpView();
                                }
                            });
                    } else {
                        Snackbar.make(mContainer, ErrorUtils.serverErrorText(((ApiCallException) e).errorTag),
                            Snackbar.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onCompleted() { }
        });
    }

    private void setVoteResponseView() {
        tvResponsesCount.setText(task.getDeadlineDate().after(new Date()) ? R.string.vt_vote_count_open
            : R.string.vt_vote_count_closed);
        TaskService.getInstance().fetchVoteTotals(taskUid).subscribe(new Subscriber<ResponseTotalsModel>() {
            @Override
            public void onNext(ResponseTotalsModel responseTotalsModel) {
                TextView tvYes = (TextView) llVoteResponseDetails.findViewById(R.id.count_yes);
                TextView tvNo = (TextView) llVoteResponseDetails.findViewById(R.id.count_no);
                TextView tvAbstain = (TextView) llVoteResponseDetails.findViewById(R.id.count_abstain);
                TextView tvNoResponse = (TextView) llVoteResponseDetails.findViewById(R.id.count_no_response);

                tvYes.setText(String.valueOf(responseTotalsModel.getYes()));
                tvNo.setText(String.valueOf(responseTotalsModel.getNo()));
                tvAbstain.setText(String.valueOf(responseTotalsModel.getAbstained()));
                tvNoResponse.setText(String.valueOf(responseTotalsModel.getNumberNoReply())); // todo : change this

                canViewResponses = true;
            }

            @Override
            public void onError(Throwable e) {
                tvResponsesCount.setText(R.string.vt_vote_totals_error); // add a button for retry in future
                icResponsesExpand.setVisibility(View.GONE);
                cvResponseList.setClickable(false);

                if (e instanceof ApiCallException) {
                    if (NetworkUtils.CONNECT_ERROR.equals(e.getMessage())) {
                        ErrorUtils.networkErrorSnackbar(mContainer, R.string.connect_error_view_task_snackbar,
                            new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    setVoteResponseView();
                                }
                            });
                    } else {
                        Snackbar.make(mContainer, ErrorUtils.serverErrorText(((ApiCallException) e).errorTag),
                            Snackbar.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onCompleted() { }
        });

    }

    private void setUpToDoAssignedMemberView() {
        memberListAdapter = new MemberListAdapter(getActivity(), true);
        rcResponseList.setLayoutManager(new LinearLayoutManager(getContext()));
        rcResponseList.setAdapter(memberListAdapter);
        rcResponseList.setItemAnimator(null);

        TaskService.getInstance().fetchAssignedMembers(taskUid, TaskConstants.TODO)
            .subscribe(new Subscriber<List<Member>>() {
                @Override
                public void onNext(List<Member> members) {
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

                @Override
                public void onError(Throwable e) {
                    tvResponsesCount.setText(R.string.vt_todo_assigned_error);
                    icResponsesExpand.setVisibility(View.GONE);
                    cvResponseList.setClickable(false);

                    if (e instanceof ApiCallException) {
                        if (NetworkUtils.CONNECT_ERROR.equals(e.getMessage())) {
                            ErrorUtils.networkErrorSnackbar(mContainer, R.string.connect_error_view_task_snackbar,
                                new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    setUpToDoAssignedMemberView();
                                }
                            });
                        } else {
                            Snackbar.make(mContainer, ErrorUtils.serverErrorText(((ApiCallException) e).errorTag), Snackbar.LENGTH_SHORT)
                                .show();
                        }
                    }
            }

            @Override
            public void onCompleted() { }
        });
    }

    private void setUpViews(TaskModel task) {
        tvDescription.setVisibility(
                TextUtils.isEmpty(task.getDescription()) ? View.GONE : View.VISIBLE);

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
        tvLocation.setText(String.format(getString(R.string.vt_mtg_location), task.getLocation())); // todo: integrate w/Maps

        tvPostedBy.setText(String.format(getString(R.string.vt_mtg_posted), task.getName()));
        TextViewCompat.setTextAppearance(tvPostedBy, R.style.CardViewFinePrint);

        final boolean inFuture = task.getDeadlineDate().after(new Date());
        final int dateColor = inFuture ? ContextCompat.getColor(getActivity(), R.color.dark_grey_text)
                : ContextCompat.getColor(getActivity(), R.color.red);
        tvDateTime.setText(inFuture ? String.format(getString(R.string.vt_mtg_datetime),
                TaskConstants.dateDisplayWithDayName.format(task.getDeadlineDate()))
                : String.format(getString(R.string.vt_mtg_date_past),
                TaskConstants.dateDisplayWithoutHours.format(task.getDeadlineDate())));
        tvDateTime.setTextColor(dateColor);

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
            btModifyTask.setVisibility(View.VISIBLE);
            btModifyTask.setText(R.string.vt_mtg_modify);
            btCancelTask.setVisibility(View.VISIBLE);
            btCancelTask.setText(R.string.vt_mtg_cancel);
        }

        setMeetingRsvpView();
    }

    private void setViewForVote(final TaskModel task) {
        tvTitle.setText(task.isInFuture() ? R.string.vt_vote_title : R.string.vt_vote_title_past);
        tvHeader.setText(task.getTitle());
        tvLocation.setVisibility(View.GONE);
        tvPostedBy.setText(String.format(getString(R.string.vt_vote_posted), task.getName()));
        tvDateTime.setText(String.format(getString(R.string.vt_vote_datetime),
                TaskConstants.dateDisplayFormatWithHours.format(task.getDeadlineDate())));

        if (task.canAction()) {
            tvResponseHeader.setText(!task.hasResponded() ? getString(R.string.vt_vote_responseq)
                    : textHasRespondedCanChange());
            llResponseIcons.setVisibility(View.VISIBLE);
            setUpResponseIconsForEvent();
        } else {
            final String suffix = !task.hasResponded() ? getString(R.string.vt_vote_no_response)
                    : task.respondedYes() ? getString(R.string.vt_vote_voted_yes)
                    : getString(R.string.vt_vote_voted_no);
            tvResponseHeader.setText(String.format(getString(R.string.vt_vote_response_past), suffix));
            llResponseIcons.setVisibility(View.GONE);
            diminishResponseCard();
        }

        if (task.isCanEdit()) {
            btModifyTask.setVisibility(View.VISIBLE);
            btModifyTask.setText(R.string.vt_vote_modify);
            btCancelTask.setVisibility(View.GONE);
        }


        setVoteResponseView();
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
            btModifyTask.setVisibility(View.VISIBLE);
            btModifyTask.setText(R.string.vt_todo_modify);
            btCancelTask.setVisibility(View.GONE);
        }
        setUpToDoAssignedMemberView();
    }

    private void diminishResponseCard() {
        respondCard.setCardElevation(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2,
                getResources().getDisplayMetrics()));
    }

    private void setUpResponseIconsForEvent() {
		Log.e(TAG, "setting up response icons ... task = " + task.toString());
        icRespondPositive.setImageResource(task.respondedYes() ?
            R.drawable.respond_yes_active : R.drawable.respond_yes_inactive);
        icRespondPositive.setEnabled(!task.respondedYes());

        icRespondNegative.setImageResource(task.respondedNo() ?
            R.drawable.respond_no_active : R.drawable.respond_no_inactive);
        icRespondNegative.setEnabled(!task.respondedNo());

        if (task.hasResponded()) {
            icRespondPositive.setEnabled(false);
            icRespondNegative.setEnabled(false);
        }
    }

    private void setUpResponseIconsForTodo() {
        btTodoRespond.setImageResource(R.drawable.respond_confirm_inactive);
    }

    public void respondToTask(final String response) {
        TaskService.getInstance().respondToTaskRx(task, response, null)
            .subscribe(new Subscriber<String>() {
                @Override
                public void onNext(String s) {
                    task = RealmUtils.loadObjectFromDB(TaskModel.class, "taskUid", task.getTaskUid());
                    if (NetworkUtils.SAVED_SERVER.equals(s)) {
                        Log.e(TAG, "response received! handing over to handler");
                        handleSuccessfulReply(response);
                    } else if (NetworkUtils.SAVED_OFFLINE_MODE.equals(s)) {
                        handleSavedOffline(response);
                    }
                }

                @Override
                public void onError(Throwable e) {
                    if (e instanceof ApiCallException) {
                        ApiCallException error = (ApiCallException) e;
                        if (NetworkUtils.SERVER_ERROR.equals(error.getMessage())) {
                            handleServerError(error.errorTag);
                        } else if (NetworkUtils.CONNECT_ERROR.equals(error.getMessage())) {
                            // since we are resetting icons to what's stored in DB (for later sending)
                            task = RealmUtils.loadObjectFromDB(TaskModel.class, "taskUid", task.getTaskUid());
                            handleSavedOffline(response);

                        }
                    }
                }

                @Override
                public void onCompleted() { }
        });
    }

    private void handleServerError(final String restMessage) {
        Snackbar.make(mContainer, ErrorUtils.serverErrorText(restMessage), Snackbar.LENGTH_SHORT).show();
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
        Log.e(TAG, "resetting icons");
        switch (task.getType()) {
            case TaskConstants.TODO:
                Log.e(TAG, "doing so on todo");
                btTodoRespond.setImageResource(R.drawable.respond_confirm_active);
                btTodoRespond.setEnabled(false);
                break;
            default:
				Log.e(TAG, "setting response icons for event");
                setUpResponseIconsForEvent();
                break;
        }
    }

    private void handleSuccessfulReply(String response) {
        Snackbar.make(mContainer, snackBarMsg(response), Snackbar.LENGTH_SHORT).show();
        resetIconsAfterResponse();
        tvResponseHeader.setText(snackBarMsg(response));
    }

    private void handleSavedOffline(String action) {
        handleNoNetworkResponse(action, R.string.connect_error_task_responding);
    }

    private void handleNoNetworkResponse(final String retryTag, int snackbarMsg) {
        ErrorUtils.connectivityError(getActivity(), snackbarMsg,
                new NetworkErrorDialogListener() {
                    @Override
                    public void retryClicked() {
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
                });
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
    SECTION : METHODS FOR TRIGGERING MODIFY/CANCEL
     */

    @OnClick(R.id.vt_bt_modify)
    public void modifyTask() {
        if (task.isCanEdit()) {
            Intent editMtg = new Intent(getActivity(), EditTaskActivity.class);
            editMtg.putExtra(TaskConstants.TASK_ENTITY_FIELD, task);
            startActivityForResult(editMtg, 1);
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
        setUpCancelApiCall().enqueue(new Callback<GenericResponse>() {
            @Override
            public void onResponse(Call<GenericResponse> call, Response<GenericResponse> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful()) {
                    EventBus.getDefault().post(new TaskCancelledEvent(task));
                } else {
                    ErrorUtils.showSnackBar(getView(), "Error! Something went wrong", Snackbar.LENGTH_LONG,
                            "", null);
                }
            }

            @Override
            public void onFailure(Call<GenericResponse> call, Throwable t) {
                ErrorUtils.connectivityError(getActivity(), R.string.error_no_network,
                        new NetworkErrorDialogListener() {
                            @Override
                            public void retryClicked() {
                                cancelTask();
                            }
                        });
            }
        });
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

    private Call<GenericResponse> setUpCancelApiCall() {
        final String uid = task.getTaskUid();
        final String phoneNumber = RealmUtils.loadPreferencesFromDB().getMobileNumber();
        final String code = RealmUtils.loadPreferencesFromDB().getToken();
        switch (taskType) {
            case TaskConstants.MEETING:
                return GrassrootRestService.getInstance().getApi().cancelMeeting(phoneNumber, code, uid);
            case TaskConstants.VOTE:
                return GrassrootRestService.getInstance().getApi().cancelVote(phoneNumber, code, uid);
            case TaskConstants.TODO: // todo : set this up
            default:
                throw new UnsupportedOperationException("Error! Missing task type in call");
        }
    }

    private int snackBarMsg(String response) {
        switch (taskType) {
            case TaskConstants.MEETING:
                return response.equals(TaskConstants.RESPONSE_YES) ? R.string.vt_snackbar_response_attend
                        : R.string.vt_snackbar_response_notattend;
            case TaskConstants.VOTE:
                return response.equals(TaskConstants.RESPONSE_YES) ? R.string.vt_vote_snackbar_yes
                        : R.string.vt_vote_snackbar_no;
            case TaskConstants.TODO:
                return R.string.vt_todo_done;
        }
        return -1;
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onTaskUpdated(TaskUpdatedEvent event) {
        TaskModel updatedTask = event.getTask();
        setUpViews(updatedTask);
    }


}