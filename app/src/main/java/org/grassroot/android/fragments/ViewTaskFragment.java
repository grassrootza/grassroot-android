package org.grassroot.android.fragments;


import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.widget.TextViewCompat;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.grassroot.android.R;
import org.grassroot.android.activities.EditVoteActivity;
import org.grassroot.android.activities.NotBuiltActivity;
import org.grassroot.android.adapters.MtgRsvpAdapter;
import org.grassroot.android.interfaces.NetworkErrorDialogListener;
import org.grassroot.android.interfaces.TaskConstants;
import org.grassroot.android.models.ResponseTotalsModel;
import org.grassroot.android.models.RsvpListModel;
import org.grassroot.android.models.TaskModel;
import org.grassroot.android.models.TaskResponse;
import org.grassroot.android.services.GrassrootRestService;
import org.grassroot.android.utils.ErrorUtils;
import org.grassroot.android.utils.PreferenceUtils;

import java.util.Date;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ViewTaskFragment extends Fragment {

    private static final String TAG = ViewTaskFragment.class.getCanonicalName();

    private TaskModel task;
    private String taskType;
    private String taskUid;
    private String phoneNumber;
    private String code;
    private boolean canViewResponses;
    private MtgRsvpAdapter mtgRsvpAdapter;

    private ViewGroup mContainer;

    @BindView(R.id.vt_title)
    TextView tvTitle;
    @BindView(R.id.vt_header)
    TextView tvHeader;
    @BindView(R.id.vt_location)
    TextView tvLocation;
    @BindView(R.id.vt_posted_by)
    TextView tvPostedBy;
    @BindView(R.id.vt_date_time)
    TextView tvDateTime;
    @BindView(R.id.vt_description)
    TextView tvDescription;

    @BindView(R.id.vt_cv_respond)
    CardView respondCard;
    @BindView(R.id.vt_response_header)
    TextView tvResponseHeader;
    @BindView(R.id.vt_ll_response_icons)
    LinearLayout llResponseIcons;
    @BindView(R.id.vt_left_response)
    ImageView icRespondPositive;
    @BindView(R.id.vt_right_response)
    ImageView icRespondNegative;

    @BindView(R.id.vt_responses_count)
    TextView tvResponsesCount;
    @BindView(R.id.vt_ic_responses_expand)
    ImageView icResponsesExpand;
    @BindView(R.id.vt_mtg_response_list)
    RecyclerView rcResponseList;
    @BindView(R.id.vt_vote_response_details)
    LinearLayout llVoteResponseDetails;

    @BindView(R.id.vt_bt_modify)
    Button btModifyTask;

    // todo : may be able to simplify, a lot, this seems a lot of stuff
    @BindView(R.id.vt_progress_bar)
    ProgressBar progressBar;
    @BindView(R.id.error_layout)
    RelativeLayout errorLayout;
    @BindView(R.id.ll_no_internet)
    LinearLayout imNoInternet;

    public ViewTaskFragment() { }

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

            phoneNumber = PreferenceUtils.getuser_mobilenumber(getContext());
            code = PreferenceUtils.getuser_token(getContext());
            canViewResponses = false;
        } else {
            throw new UnsupportedOperationException("Error! View task fragment initiated without arguments");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View viewToReturn = inflater.inflate(R.layout.fragment_view_task, container, false);
        ButterKnife.bind(this, viewToReturn);
        mContainer = container;
        if (task == null) {
            retrieveTaskDetails();
        } else {
            setUpViews();
        }
        return viewToReturn;
    }

    private void retrieveTaskDetails() {
        progressBar.setVisibility(View.VISIBLE);
        GrassrootRestService.getInstance().getApi().fetchTaskEntity(phoneNumber, code, taskUid, taskType)
                .enqueue(new Callback<TaskResponse>() {
            @Override
            public void onResponse(Call<TaskResponse> call, Response<TaskResponse> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful()) {
                    Log.d(TAG, response.body().toString());
                    task = response.body().getTasks().get(0);
                    setUpViews();
                }
            }

            @Override
            public void onFailure(Call<TaskResponse> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                handleNoNetwork("FETCH");
            }
        });
    }

    private void setUpViews() {
        tvDescription.setVisibility(TextUtils.isEmpty(task.getDescription()) ? View.GONE : View.VISIBLE);

        switch (taskType) {
            case TaskConstants.MEETING:
                setViewForMeeting();
                break;
            case TaskConstants.VOTE:
                setViewForVote();
                break;
            case TaskConstants.TODO:
                setViewForToDo();
                break;
        }
    }

    private void setViewForMeeting() {
        tvTitle.setText(R.string.vt_mtg_title);
        tvHeader.setText(task.getTitle());
        tvLocation.setVisibility(View.VISIBLE);
        tvLocation.setText(String.format(getString(R.string.vt_mtg_location), task.getLocation())); // todo: integrate w/Maps

        tvPostedBy.setText(String.format(getString(R.string.vt_mtg_posted), task.getName()));
        TextViewCompat.setTextAppearance(tvPostedBy, R.style.CardViewFinePrint);

        tvDateTime.setText(String.format(getString(R.string.vt_mtg_datetime),
                TaskConstants.dateDisplayWithDayName.format(task.getDeadlineDate()))); // todo: integrate w/Calendar

        if (task.canAction()) {
            tvResponseHeader.setText(!task.hasResponded() ? getString(R.string.vt_mtg_responseq) :
                    textHasRespondedCanChange());
            llResponseIcons.setVisibility(View.VISIBLE);
            setUpResponseIconsCanAction();
        } else {
            final String suffix = !task.hasResponded() ? getString(R.string.vt_mtg_no_response) :
                    task.respondedYes() ? getString(R.string.vt_mtg_attended) : getString(R.string.vt_mtg_notattend);
            tvResponseHeader.setText(String.format(getString(R.string.vt_mtg_response_past), suffix));
            llResponseIcons.setVisibility(View.GONE);
            diminishResponseCard();
        }

        if (task.isCanEdit()) {
            btModifyTask.setVisibility(View.VISIBLE);
            btModifyTask.setText(R.string.vt_mtg_modify);
        }

        setMeetingRsvpView();
    }

    private void setViewForVote() {
        tvTitle.setText(R.string.vt_vote_title);
        tvHeader.setText(task.getTitle());
        tvLocation.setVisibility(View.GONE);
        tvPostedBy.setText(String.format(getString(R.string.vt_vote_posted), task.getName()));
        tvDateTime.setText(String.format(getString(R.string.vt_vote_datetime),
                TaskConstants.dateDisplayFormatWithHours.format(task.getDeadlineDate())));

        if (task.canAction()) {
            tvResponseHeader.setText(!task.hasResponded() ? getString(R.string.vt_vote_responseq) : textHasRespondedCanChange());
            llResponseIcons.setVisibility(View.VISIBLE);
            setUpResponseIconsCanAction();
        } else {
            final String suffix = !task.hasResponded() ? getString(R.string.vt_vote_no_response) :
                    task.respondedYes() ? getString(R.string.vt_vote_voted_yes) : getString(R.string.vt_vote_voted_no);
            tvResponseHeader.setText(String.format(getString(R.string.vt_vote_response_past), suffix));
            llResponseIcons.setVisibility(View.GONE);
            diminishResponseCard();
        }

        if (task.isCanEdit()) {
            btModifyTask.setVisibility(View.VISIBLE);
            btModifyTask.setText(R.string.vt_vote_modify);
        }

        setVoteResponseView();
    }

    private void setViewForToDo() {

    }

    private void diminishResponseCard() {
        respondCard.setCardElevation(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2, getResources().getDisplayMetrics()));
    }

    private void setUpResponseIconsCanAction() {
        icRespondPositive.setImageResource(task.respondedYes() ? R.drawable.ic_vote_active : R.drawable.ic_vote_inactive); // todo : rename these thumbs up / down
        icRespondPositive.setEnabled(!task.respondedYes());
        icRespondNegative.setImageResource(task.respondedNo() ? R.drawable.ic_no_vote_active : R.drawable.ic_no_vote_inactive);
        icRespondNegative.setEnabled(!task.respondedNo());
    }

    @OnClick(R.id.vt_left_response)
    public void doRespondYes() {
        Call<TaskResponse> call = taskType.equals(TaskConstants.VOTE) ? voteCall(TaskConstants.RESPONSE_YES) :
                meetingCall(TaskConstants.RESPONSE_YES);
        call.enqueue(new Callback<TaskResponse>() {
            @Override
            public void onResponse(Call<TaskResponse> call, Response<TaskResponse> response) {
                if (response.isSuccessful()) {
                    handleSuccessfulReply(response, TaskConstants.RESPONSE_YES);
                } else {
                    handleUnknownError(response);
                }
            }

            @Override
            public void onFailure(Call<TaskResponse> call, Throwable t) {
                handleNoNetwork("RESPOND_YES");
            }
        });
    }

    @OnClick(R.id.vt_right_response)
    public void doRespondNo() {
        Call<TaskResponse> call = taskType.equals(TaskConstants.VOTE) ? voteCall(TaskConstants.RESPONSE_NO) :
                meetingCall(TaskConstants.RESPONSE_NO);
        call.enqueue(new Callback<TaskResponse>() {
            @Override
            public void onResponse(Call<TaskResponse> call, Response<TaskResponse> response) {
                if (response.isSuccessful()) {
                    handleSuccessfulReply(response, TaskConstants.RESPONSE_NO);
                } else {
                    handleUnknownError(response);
                }
            }

            @Override
            public void onFailure(Call<TaskResponse> call, Throwable t) {
                handleNoNetwork("RESPOND_NO");
            }
        });
    }

    private Call<TaskResponse> voteCall(String response) {
        return GrassrootRestService.getInstance().getApi().castVote(taskUid, phoneNumber, code, response);
    }

    private Call<TaskResponse> meetingCall(String response) {
        return GrassrootRestService.getInstance().getApi().rsvp(taskUid, phoneNumber, code, response);
    }

    private void handleSuccessfulReply(Response<TaskResponse> response, String reply) {
        task = response.body().getTasks().get(0);
        ErrorUtils.showSnackBar(mContainer, snackBarMsg(reply), Snackbar.LENGTH_SHORT); // todo: rename error utils
        setUpResponseIconsCanAction();
        tvResponseHeader.setText(snackBarMsg(reply));
    }

    // todo : consider shifting these into a map? but maybe better to rely on processor than clog memory
    private String textHasRespondedCanChange() {
        switch (taskType) {
            case TaskConstants.MEETING:
                final String suffix = task.respondedYes() ? getString(R.string.vt_mtg_attending) :
                        getString(R.string.vt_mtg_not_attending);
                return String.format(getString(R.string.vt_mtg_responded_can_action), suffix);
            case TaskConstants.VOTE:
                final String suffix2 = task.respondedYes() ? getString(R.string.vt_vote_yes) : getString(R.string.vt_vote_no);
                return String.format(getString(R.string.vt_vote_responded_can_action), suffix2);
            case TaskConstants.TODO:
            default:
                return "";
        }
    }

    /*
    SECTION : HANDLING DETAILS ON RSVP LIST, VOTE TOTALS, ETC.
     */

    private void setMeetingRsvpView() {

        // have to do this here, as must be on main thread or won't draw
        mtgRsvpAdapter = new MtgRsvpAdapter();
        rcResponseList.setLayoutManager(new LinearLayoutManager(getContext()));
        rcResponseList.setAdapter(mtgRsvpAdapter);

        GrassrootRestService.getInstance().getApi().fetchMeetingRsvps(phoneNumber, code, taskUid)
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
                            Log.e(TAG, "error! printing: " + response.errorBody());
                        }
                    }

                    @Override
                    public void onFailure(Call<RsvpListModel> call, Throwable t) {
                        handleNoNetwork("MTG_RSVP");
                    }
                });
    }

    private void setVoteResponseView() {
        tvResponsesCount.setText(task.getDeadlineDate().after(new Date()) ? R.string.vt_vote_count_open :
                R.string.vt_vote_count_closed);
        GrassrootRestService.getInstance().getApi().fetchVoteTotals(phoneNumber, code, taskUid)
                .enqueue(new Callback<ResponseTotalsModel>() {
                    @Override
                    public void onResponse(Call<ResponseTotalsModel> call, Response<ResponseTotalsModel> response) {
                        if (response.isSuccessful()) {
                            ResponseTotalsModel totals = response.body();
                            displayVoteTotals(totals);
                            canViewResponses = true;
                        } else {
                            Log.e(TAG, "error! printing: " + response.errorBody());
                        }
                    }

                    @Override
                    public void onFailure(Call<ResponseTotalsModel> call, Throwable t) {
                        handleNoNetwork("VOTE_TOTALS");
                    }
                });
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
                    break;
            }
        }
    }

    public void toggleResponseList() {
        Log.d(TAG, "toggling recycler view, should have items : " + rcResponseList.getAdapter().getItemCount());
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
    SECTION : MISCELLANEOUS HELPER METHODS AND INTENT TRIGGERS
     */

    @OnClick(R.id.vt_bt_modify)
    public void modifyTask() {
        if (task.isCanEdit()) {
            switch (taskType) {
                case TaskConstants.MEETING:
                    // todo : make this more efficient
                    Intent editMtg = new Intent(getActivity(), NotBuiltActivity.class);
                    editMtg.putExtra("title", "Modify meeting");
                    startActivity(editMtg);
                    break;
                case TaskConstants.VOTE:
                    Intent editVote = new Intent(getActivity(), EditVoteActivity.class);
                    editVote.putExtra("description", task.getDescription());
                    editVote.putExtra("deadline", task.getDeadlineDate());
                    editVote.putExtra("voteid", task.getTaskUid());
                    editVote.putExtra("title", task.getTitle());
                    startActivityForResult(editVote, 1);
                    break;
                case TaskConstants.TODO:
                    break;
            }
        }
    }

    private int snackBarMsg(String response) {
        switch (taskType) {
            case TaskConstants.MEETING:
                return response.equals(TaskConstants.RESPONSE_YES) ? R.string.vt_snackbar_response_attend :
                        R.string.vt_snackbar_response_notattend;
            case TaskConstants.VOTE:
                return response.equals(TaskConstants.RESPONSE_YES) ? R.string.vt_vote_snackbar_yes :
                        R.string.vt_vote_snackbar_no;
            case TaskConstants.TODO:
                break;
        }
        return -1;
    }

    private void handleNoNetwork(final String retryTag) {
        errorLayout.setVisibility(View.VISIBLE);
        imNoInternet.setVisibility(View.VISIBLE);
        ErrorUtils.connectivityError(getActivity(), R.string.error_no_network, new NetworkErrorDialogListener() {
            @Override
            public void retryClicked() {
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
                    default:
                        retrieveTaskDetails();
                }
            }
        });
    }

    private void handleUnknownError(Response<TaskResponse> response) {
        ErrorUtils.showSnackBar(mContainer, R.string.error_generic, Snackbar.LENGTH_LONG);
    }

}