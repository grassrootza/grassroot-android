package org.grassroot.android.fragments;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.TextViewCompat;
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
import android.widget.RelativeLayout;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import java.util.Date;
import java.util.List;
import org.grassroot.android.R;
import org.grassroot.android.activities.EditTaskActivity;
import org.grassroot.android.adapters.MemberListAdapter;
import org.grassroot.android.adapters.MtgRsvpAdapter;
import org.grassroot.android.events.TaskCancelledEvent;
import org.grassroot.android.events.TaskUpdatedEvent;
import org.grassroot.android.fragments.dialogs.ConfirmCancelDialogFragment;
import org.grassroot.android.interfaces.NetworkErrorDialogListener;
import org.grassroot.android.interfaces.TaskConstants;
import org.grassroot.android.models.exceptions.ApiCallException;
import org.grassroot.android.models.GenericResponse;
import org.grassroot.android.models.Member;
import org.grassroot.android.models.MemberList;
import org.grassroot.android.models.ResponseTotalsModel;
import org.grassroot.android.models.RsvpListModel;
import org.grassroot.android.models.TaskModel;
import org.grassroot.android.models.TaskResponse;
import org.grassroot.android.services.GrassrootRestService;
import org.grassroot.android.services.SharingService;
import org.grassroot.android.services.TaskService;
import org.grassroot.android.utils.ErrorUtils;
import org.grassroot.android.utils.NetworkUtils;
import org.grassroot.android.utils.RealmUtils;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import rx.Subscriber;

public class ViewTaskFragment extends Fragment {

  private static final String TAG = ViewTaskFragment.class.getCanonicalName();

  private TaskModel task;
  private String taskType;
  private String taskUid;
  private String phoneNumber;
  private String code;
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

  ProgressDialog progressDialog;

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

  @Override public void onCreate(Bundle savedInstanceState) {
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
        throw new UnsupportedOperationException(
            "Error! View task fragment with type or UID missing");
      }

      phoneNumber = RealmUtils.loadPreferencesFromDB().getMobileNumber();
      code = RealmUtils.loadPreferencesFromDB().getToken();
      canViewResponses = false;
    } else {
      throw new UnsupportedOperationException(
          "Error! View task fragment initiated without arguments");
    }
  }

  @Override public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
    View viewToReturn = inflater.inflate(R.layout.fragment_view_task, container, false);
    unbinder = ButterKnife.bind(this, viewToReturn);
    mContainer = container;

    progressDialog = new ProgressDialog(getActivity());
    progressDialog.setIndeterminate(true);

    if (task == null) {
      retrieveTaskDetails();
    } else {
      setUpViews(task);
    }
    return viewToReturn;
  }

  @Override
  public void onPrepareOptionsMenu(Menu menu) {
    super.onPrepareOptionsMenu(menu);
    if(menu.findItem(R.id.mi_share_task) !=null) menu.findItem(R.id.mi_share_task).setVisible(true);
  }


  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    if(item.getItemId() == R.id.mi_share_task){
      Intent i = new Intent(getActivity(), SharingService.class);
      i.putExtra(SharingService.TASK_TAG,task);
      getActivity().startService(i);
     return true;
    }else {
      return super.onOptionsItemSelected(item);
    }
  }

  @Override public void onDestroyView() {
    super.onDestroyView();
    unbinder.unbind();
  }

  private void retrieveTaskDetails() {
    if (!NetworkUtils.isOnline(getContext())) {
      task = RealmUtils.loadObjectFromDB(TaskModel.class, "taskUid", taskUid);
      setUpViews(task);
    } else {
      progressDialog.show();
      // todo : switch this to using task service, with an observer ...
      GrassrootRestService.getInstance()
          .getApi()
          .fetchTaskEntity(phoneNumber, code, taskUid, taskType)
          .enqueue(new Callback<TaskResponse>() {
            @Override
            public void onResponse(Call<TaskResponse> call, Response<TaskResponse> response) {
              progressDialog.dismiss();
              if (response.isSuccessful()) {
                Log.d(TAG, response.body().toString());
                task = response.body().getTasks().get(0);
                Log.e(TAG, task.toString());
                setUpViews(task);
              } else {
                // todo : swap to action complete activity if the error is because cancelled
                final String errorMsg = ErrorUtils.serverErrorText(response.errorBody(), getContext());
                Snackbar.make(mContainer, errorMsg, Snackbar.LENGTH_SHORT).show();
              }
            }

            @Override public void onFailure(Call<TaskResponse> call, Throwable t) {
              progressDialog.dismiss();
              handleNoNetwork("FETCH");
            }
          });
    }
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
      setUpResponseIconsCanAction();
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
      setUpResponseIconsCanAction();
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

    setUpTodoResponseIconsCanAction();
    if (task.isCanEdit()) {
      btModifyTask.setVisibility(View.VISIBLE);
      btModifyTask.setText(R.string.vt_todo_modify);
      btCancelTask.setVisibility(View.GONE);
    }
    setUpAssignedMembersView();
  }

  private void diminishResponseCard() {
    respondCard.setCardElevation(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2,
        getResources().getDisplayMetrics()));
  }

  private void setUpResponseIconsCanAction() {
    icRespondPositive.setImageResource(task.respondedYes() ? R.drawable.ic_vote_active
        : R.drawable.ic_vote_inactive); // todo : rename these thumbs up / down
    icRespondPositive.setEnabled(!task.respondedYes());
    icRespondNegative.setImageResource(
        task.respondedNo() ? R.drawable.ic_no_vote_active : R.drawable.ic_no_vote_inactive);
    icRespondNegative.setEnabled(!task.respondedNo());
    if(task.hasResponded()){
      icRespondPositive.setEnabled(false);
      icRespondNegative.setEnabled(false);
    }
  }

  private void setUpTodoResponseIconsCanAction() {
    if (task.canAction()) {
      btTodoRespond.setImageResource(R.drawable.ic_vote_tick_inactive);
    }
  }

  public void respondRx(final String reply) {
    TaskService.getInstance().respondToTaskRx(task, reply, null).subscribe(new Subscriber<String>() {
      @Override
      public void onNext(String s) {
        task = RealmUtils.loadObjectFromDB(TaskModel.class, "taskUid", task.getTaskUid());
        if (NetworkUtils.SAVED_SERVER.equals(s)) {
          handleSuccessfulReply(task, reply);
        } else if (NetworkUtils.SAVED_OFFLINE_MODE.equals(s)) {
          handleSavedOffline(reply);
        }
      }

      @Override
      public void onError(Throwable e) {
        if (e instanceof ApiCallException) {
          ApiCallException error = (ApiCallException) e;
          if (NetworkUtils.SERVER_ERROR.equals(error.getMessage())) {
            handleServerError(error.errorTag);
          } else if (NetworkUtils.CONNECT_ERROR.equals(error.getMessage())) {
            handleSavedOffline(reply);
          }
        }
      }

      @Override
      public void onCompleted() { }
    });
  }

  private void handleServerError(final String restMessage) {
    Snackbar.make(mContainer, ErrorUtils.serverErrorText(restMessage, getContext()), Snackbar.LENGTH_SHORT).show();
  }

  @OnClick(R.id.vt_left_response) public void doRespondYes() {
    respondRx(TaskConstants.RESPONSE_YES);
  }

  @OnClick(R.id.vt_right_response) public void doRespondNo() {
    respondRx(TaskConstants.RESPONSE_NO);
  }

  @OnClick(R.id.bt_td_respond) public void completeTodo() {
    respondRx(TaskConstants.TODO_DONE);
  }


  private void setUpAssignedMembersView() {

    memberListAdapter = new MemberListAdapter(getActivity());
    rcResponseList.setLayoutManager(new LinearLayoutManager(getContext()));
    rcResponseList.setAdapter(memberListAdapter);

    if(NetworkUtils.isOnline(getContext())){
    GrassrootRestService.getInstance()
        .getApi()
        .getTodoAssigned(phoneNumber, code, taskUid)
        .enqueue(new Callback<MemberList>() {
          @Override public void onResponse(Call<MemberList> call, Response<MemberList> response) {
            if (response.isSuccessful()) {
              List<Member> members = response.body().getMembers();
              if (!members.isEmpty()) {
                memberListAdapter.addMembers(response.body().getMembers());
                canViewResponses = true;
                tvResponsesCount.setText(R.string.vt_todo_members_assigned);
              } else {
                tvResponsesCount.setText(R.string.vt_todo_group_assigned);
                icResponsesExpand.setVisibility(View.GONE);
                cvResponseList.setClickable(false);
              }
            } else {
              //todo handle this error
            }
          }

          @Override public void onFailure(Call<MemberList> call, Throwable t) {
            handleNoNetwork("ASSIGNED_MEMBERS");
          }
        });
  }}

  private void handleSuccessfulReply(TaskModel task, String reply) {
    ErrorUtils.showSnackBar(mContainer, snackBarMsg(reply),
        Snackbar.LENGTH_SHORT); // todo: rename error utils)
    switch (reply) {
      case TaskConstants.TODO:
        setUpResponseIconsCanAction();
        break;
      case TaskConstants.TODO_DONE:
        btTodoRespond.setImageResource(R.drawable.ic_vote_tick_active);
        btTodoRespond.setEnabled(false);
        break;
      default:
        setUpResponseIconsCanAction();
        break;
    }
    tvResponseHeader.setText(snackBarMsg(reply));
  }

  private void handleSavedOffline(String action){
    // todo : change message displayed here
    handleNoNetwork(action);
  }

  private void handleNoNetwork(final String retryTag) {
    ErrorUtils.connectivityError(getActivity(), R.string.error_no_network,
        new NetworkErrorDialogListener() {
          @Override public void retryClicked() {
            switch (retryTag) {
              case "FETCH":
                retrieveTaskDetails();
                break;
              case "RESPOND_YES":
                doRespondYes();
                break;
              case "RESPOND_NO":
                doRespondNo();
                break;
              case "MTG_RSVP":
                setMeetingRsvpView();
                break;
              case "VOTE_TOTALS":
                setVoteResponseView();
                break;
              case "COMPLETE_TODO":
                completeTodo();
                break;
              case "ASSIGNED_MEMBERS":
                setUpAssignedMembersView();
              default:
                retrieveTaskDetails();
            }
          }
        });
  }

  private void handleUnknownError(Response<TaskResponse> response) {
    ErrorUtils.showSnackBar(mContainer, R.string.error_generic, Snackbar.LENGTH_LONG);
  }

  @Override public void onDestroy() {
    super.onDestroy();
    EventBus.getDefault().unregister(this);
  }

  @Subscribe public void onTaskUpdated(TaskUpdatedEvent event) {
    TaskModel updatedTask = event.getTask();
    setUpViews(updatedTask);
  }

  // todo : consider shifting these into a map? but maybe better to rely on processor than clog memory
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
    SECTION : HANDLING DETAILS ON RSVP LIST, VOTE TOTALS, ETC.
    todo : switch these to Rx
     */

  private void setMeetingRsvpView() {

    // have to do this here, as must be on main thread or won't draw
    mtgRsvpAdapter = new MtgRsvpAdapter();
    rcResponseList.setLayoutManager(new LinearLayoutManager(getContext()));
    rcResponseList.setAdapter(mtgRsvpAdapter);
    if (NetworkUtils.isOnline(getContext())) {
      GrassrootRestService.getInstance()
          .getApi()
          .fetchMeetingRsvps(phoneNumber, code, taskUid)
          .enqueue(new Callback<RsvpListModel>() {
            @Override
            public void onResponse(Call<RsvpListModel> call, Response<RsvpListModel> response) {
              if (response.isSuccessful()) {
                RsvpListModel rsvps = response.body();
                tvResponsesCount.setText(String.format(getString(R.string.vt_mtg_response_count),
                    rsvps.getNumberInvited(), rsvps.getNumberYes()));
                if (rsvps.isCanViewRsvps()) {
                  icResponsesExpand.setVisibility(View.VISIBLE);
                  mtgRsvpAdapter.setMapOfResponses(rsvps.getRsvpResponses());
                  canViewResponses = true;
                } else {
                  icResponsesExpand.setVisibility(View.GONE);
                }
              } else {
                final String errorMsg = ErrorUtils.serverErrorText(response.errorBody(), getContext());
                Snackbar.make(mContainer, errorMsg, Snackbar.LENGTH_SHORT).show();
              }
            }

            @Override public void onFailure(Call<RsvpListModel> call, Throwable t) {
              handleNoNetwork("MTG_RSVP");
            }
          });
    }
  }

  private void setVoteResponseView() {
    tvResponsesCount.setText(task.getDeadlineDate().after(new Date()) ? R.string.vt_vote_count_open
        : R.string.vt_vote_count_closed);
    if (NetworkUtils.isOnline(getContext())) {
      GrassrootRestService.getInstance()
          .getApi()
          .fetchVoteTotals(phoneNumber, code, taskUid)
          .enqueue(new Callback<ResponseTotalsModel>() {
            @Override public void onResponse(Call<ResponseTotalsModel> call,
                Response<ResponseTotalsModel> response) {
              if (response.isSuccessful()) {
                ResponseTotalsModel totals = response.body();
                displayVoteTotals(totals);
                canViewResponses = true;
              } else {
                final String errorMsg = ErrorUtils.serverErrorText(response.errorBody(), getContext());
                Snackbar.make(mContainer, errorMsg, Snackbar.LENGTH_SHORT).show();
              }
            }

            @Override public void onFailure(Call<ResponseTotalsModel> call, Throwable t) {
              handleNoNetwork("VOTE_TOTALS");
            }
          });
    }
  }

  private void displayVoteTotals(ResponseTotalsModel model) {
    TextView tvYes = (TextView) llVoteResponseDetails.findViewById(R.id.count_yes);
    TextView tvNo = (TextView) llVoteResponseDetails.findViewById(R.id.count_no);
    TextView tvAbstain = (TextView) llVoteResponseDetails.findViewById(R.id.count_abstain);
    TextView tvNoResponse = (TextView) llVoteResponseDetails.findViewById(R.id.count_no_response);

    tvYes.setText(String.valueOf(model.getYes()));
    tvNo.setText(String.valueOf(model.getNo()));
    tvAbstain.setText(String.valueOf(model.getAbstained()));
    tvNoResponse.setText(String.valueOf(model.getNumberNoReply())); // todo : change this
  }

  // do we want animation? seems expensive, for little gain, here. maybe, when polish this in general
  @OnClick(R.id.vt_cv_response_list) public void slideOutDetails() {
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
    Log.d(TAG, "toggling recycler view, should have items : " + rcResponseList.getAdapter()
        .getItemCount());
    if (rcResponseList.getVisibility() != View.VISIBLE) {
      rcResponseList.setVisibility(View.VISIBLE);
      icResponsesExpand.setImageResource(R.drawable.ic_arrow_up);
    } else {
      rcResponseList.setVisibility(View.GONE);
      icResponsesExpand.setImageResource(R.drawable.ic_arrow_down);
    }
  }

  public void toggleVoteDetails() {
    Log.e(TAG, "toggling vote details!");
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

  @OnClick(R.id.vt_bt_modify) public void modifyTask() {
    if (task.isCanEdit()) {
      Intent editMtg = new Intent(getActivity(), EditTaskActivity.class);
      editMtg.putExtra(TaskConstants.TASK_ENTITY_FIELD, task);
      startActivityForResult(editMtg, 1);
    }
  }

  @OnClick(R.id.vt_bt_cancel) public void promptCancel() {
    if (task.isCanEdit()) {
      String dialogMessage = generateConfirmationDialogStrings();
      ConfirmCancelDialogFragment confirmCancelDialogFragment =
          ConfirmCancelDialogFragment.newInstance(dialogMessage,
              new ConfirmCancelDialogFragment.ConfirmDialogListener() {
                @Override public void doConfirmClicked() {
                  cancelTask();
                }
              });
      confirmCancelDialogFragment.show(getFragmentManager(), TAG);
    }
  }

  private void cancelTask() {
    progressDialog.show();
    setUpCancelApiCall().enqueue(new Callback<GenericResponse>() {
      @Override
      public void onResponse(Call<GenericResponse> call, Response<GenericResponse> response) {
        progressDialog.dismiss();
        if (response.isSuccessful()) {
          EventBus.getDefault().post(new TaskCancelledEvent(task));
        } else {
          ErrorUtils.showSnackBar(getView(), "Error! Something went wrong", Snackbar.LENGTH_LONG,
              "", null);
        }
      }

      @Override public void onFailure(Call<GenericResponse> call, Throwable t) {
        ErrorUtils.connectivityError(getActivity(), R.string.error_no_network,
            new NetworkErrorDialogListener() {
              @Override public void retryClicked() {
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


}