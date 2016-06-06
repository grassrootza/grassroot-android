package org.grassroot.android.ui.activities;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.grassroot.android.R;

import org.grassroot.android.interfaces.NetworkErrorDialogListener;
import org.grassroot.android.services.GrassrootRestService;
import org.grassroot.android.services.model.EventModel;
import org.grassroot.android.services.model.EventResponse;
import org.grassroot.android.services.model.GenericResponse;
import org.grassroot.android.utils.Constant;
import org.grassroot.android.utils.ErrorUtils;
import org.grassroot.android.ui.views.ProgressBarCircularIndeterminate;
import org.grassroot.android.utils.PreferenceUtils;


import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ViewVoteActivity extends PortraitActivity {

    private static final String TAG = ViewVoteActivity.class.getCanonicalName();

    @BindView(R.id.vv_toolbar)
    Toolbar vvToolbar;
    @BindView(R.id.txt_toolbar)
    TextView txtToolbar;
    @BindView(R.id.txt_vv_title)
    TextView txtVvTitle;
    @BindView(R.id.rl_nested)
    RelativeLayout rlNested;
    @BindView(R.id.txt_vv_groupname)
    TextView txtVvGroupname;
    @BindView(R.id.txt_vv_deadline)
    TextView txtVvDeadline;
    @BindView(R.id.txt_vv_description)
    TextView txtVvDescription;
    @BindView(R.id.rl_vote_status)
    RelativeLayout rlVoteStatus;
    @BindView(R.id.ll_image_holder)
    LinearLayout llImageHolder;
    @BindView(R.id.txt_header)
    TextView txtHeader;
    @BindView(R.id.scrollView)
    ScrollView scrollView;
    @BindView(R.id.rl_vv_main_layout)
    LinearLayout rlVvMainLayout;
    @BindView(R.id.progressBarCircularIndeterminate)
    ProgressBarCircularIndeterminate progressBarCircularIndeterminate;
    @BindView(R.id.txt_prg)
    TextView txtPrg;
    @BindView(R.id.bt_editVote)
    Button bt_editVote;
    @BindView(R.id.expandable)
    LinearLayout mLinearLayout;
    @BindView(R.id.header)
    RelativeLayout mRelativeLayoutHeader;
    ValueAnimator mAnimator;
    @BindView(R.id.ll_yes)
    LinearLayout llYes;
    @BindView(R.id.count_yes)
    TextView countYes;
    @BindView(R.id.ll_no)
    LinearLayout llNo;
    @BindView(R.id.count_no)
    TextView countNo;
    @BindView(R.id.iv_expand)
    ImageView ivExpand;
    @BindView(R.id.txt_yes)
    TextView txtYes;
    @BindView(R.id.txt_no)
    TextView txtNo;
    @BindView(R.id.vv_root)
    RelativeLayout vvRoot;
    private Snackbar snackbar;
    @BindView(R.id.thumbs_down)
    ImageView thumbsDown;
    @BindView(R.id.thumbs_up)
    ImageView thumbsUp;
    @BindView(R.id.error_layout)
    View errorLayout;
    @BindView(R.id.ll_maybe)
    LinearLayout llMaybe;
    @BindView(R.id.txt_maybe)
    TextView txtMaybe;
    @BindView(R.id.count_maybe)
    TextView countMaybe;
    @BindView(R.id.txt_invalid)
    TextView txtInvalid;
    @BindView(R.id.count_invalid)
    TextView countInvalid;
    @BindView(R.id.ll_numberOfUsers)
    LinearLayout llNumberOfUsers;
    @BindView(R.id.txt_numberOfUsers)
    TextView txtNumberOfUsers;
    @BindView(R.id.count_numberOfUsers)
    TextView countNumberOfUsers;
    @BindView(R.id.ll_numberNoRSVP)
    LinearLayout llNumberNoRSVP;
    @BindView(R.id.txt_numberNoRSVP)
    TextView txtNumberNoRSVP;
    @BindView(R.id.count_numberNoRSVP)
    TextView countNumberNoRSVP;
    private String voteid;
    private String title;
    private String description;
    private String deadline;
    private boolean canEdit;
    private String phoneNumber;
    private String code;
    private EventModel eventModel;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_vote);
        ButterKnife.bind(this);
        if (getIntent() != null) {
            voteid = getIntent().getExtras().getString(Constant.UID);
        }
        setUpToolbar();
        init();

    }

    private void setUpToolbar() {
        vvToolbar.setNavigationIcon(R.drawable.btn_back_wt);
        vvToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
    }

    public void onBackPressed() {
        Intent i = new Intent();
        i.putExtra("update", 0);
        setResult(1, i);
        finish();
    }

    private void init() {
        phoneNumber = PreferenceUtils.getuser_mobilenumber(this);
        code = PreferenceUtils.getuser_token(this);
        updateView();
    }

    private void updateView() {
        showProgress();
        fetchVoteDetails();
    }


    private void showProgress() {
        rlVvMainLayout.setVisibility(View.GONE);
        progressBarCircularIndeterminate.setVisibility(View.VISIBLE);
        txtPrg.setVisibility(View.VISIBLE);

    }

    private void fetchVoteDetails() {
        GrassrootRestService.getInstance().getApi().viewVote(phoneNumber, code, voteid).enqueue(new Callback<EventResponse>() {
            @Override
            public void onResponse(Call<EventResponse> call, Response<EventResponse> response) {
                if (response.isSuccessful()) {
                    Log.d(TAG, response.body().toString());
                    eventModel = response.body().getEventModel();
                    setView(eventModel);
                }
            }

            @Override
            public void onFailure(Call<EventResponse> call, Throwable t) {
                progressBarCircularIndeterminate.setVisibility(View.GONE);
                ErrorUtils.connectivityError(ViewVoteActivity.this, R.string.No_network, new NetworkErrorDialogListener() {
                    @Override
                    public void retryClicked() {
                     updateView();
                    }
                });
              //  ErrorUtils.handleNetworkError(ViewVoteActivity.this, errorLayout, t);
            }
        });


    }

    private void setView(EventModel model) {
        Log.e(TAG, "setView");

        txtVvTitle.setText(model.getTitle());
        txtVvGroupname.setText("Posted by " + model.getName());
        try {
            txtVvDeadline.setText(model.getDeadline());
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "e is " + e.getMessage());
        }
        txtVvDescription.setText(model.getDescription());
        txtYes.setText(getString(R.string.vv_yes));
        countYes.setText(String.valueOf(model.getResponseTotalsModel().getYes()));
        txtNo.setText(getString(R.string.vv_no));
        countNo.setText(String.valueOf(model.getResponseTotalsModel().getNo()));
        txtMaybe.setText(getString(R.string.vv_maybe));
        countMaybe.setText(String.valueOf(model.getResponseTotalsModel().getMaybe()));
        txtInvalid.setText(getString(R.string.vv_invalid));
        countInvalid.setText(String.valueOf(model.getResponseTotalsModel().getInvalid()));
        txtNumberOfUsers.setText(getString(R.string.vv_numberOfUsers));
        countNumberOfUsers.setText(String.valueOf(model.getResponseTotalsModel().getNumberOfUsers()));
        txtNumberNoRSVP.setText(getString(R.string.vv_numberNoRSVP));
        countNumberNoRSVP.setText(String.valueOf(model.getResponseTotalsModel().getNumberOfRsvp()));
        title = model.getTitle();
        canEdit = model.isCanEdit();
        deadline = model.getDeadline();
        description = model.getDescription();

        toggleActionButtons(model);

        if (model.isCanEdit()) {
            bt_editVote.setClickable(true);
            bt_editVote.setVisibility(View.VISIBLE);
        }
        progressBarCircularIndeterminate.setVisibility(View.GONE);
        txtPrg.setVisibility(View.GONE);
        rlVvMainLayout.setVisibility(View.VISIBLE);
        mLinearLayout.getViewTreeObserver().addOnPreDrawListener(
                new ViewTreeObserver.OnPreDrawListener() {
                    @Override
                    public boolean onPreDraw() {
                        mLinearLayout.getViewTreeObserver().removeOnPreDrawListener(this);
                        mLinearLayout.setVisibility(View.GONE);
                        final int widthSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
                        final int heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
                        mLinearLayout.measure(widthSpec, heightSpec);
                        mAnimator = slideAnimator(0, mLinearLayout.getMeasuredHeight());
                        return true;
                    }
                });

    }

    private void toggleActionButtons(EventModel model) {
        if (model.getCanAction()) {
            if (model.getHasResponded()) {
                hasRespondedButCanAction(model);
            } else {
                hasNotRespondedAndCanAction(model);
            }
        } else if (!model.getCanAction()) {
            cannotAction(model);
        }
    }

    private void hasNotRespondedAndCanAction(EventModel model) {
        thumbsUp.setEnabled(true);
        thumbsDown.setEnabled(true);
        thumbsUp.setImageResource(R.drawable.ic_no_vote_inactive);
        thumbsDown.setImageResource(R.drawable.ic_vote_inactive);
    }

    private void hasRespondedButCanAction(EventModel model) {
        if (model.getReply().equalsIgnoreCase("Yes")) {
            thumbsUp.setImageResource(R.drawable.ic_vote_active);
            thumbsDown.setImageResource(R.drawable.ic_no_vote_inactive);
            thumbsUp.setEnabled(false);
            thumbsDown.setEnabled(true);

        } else if (model.getReply().equalsIgnoreCase("No")) {
            thumbsUp.setImageResource(R.drawable.ic_vote_inactive);
            thumbsDown.setImageResource(R.drawable.ic_no_vote_active);
            thumbsUp.setEnabled(true);
            thumbsDown.setEnabled(false);

        } else if (model.getReply().equalsIgnoreCase("NO_RESPONSE")) {
            thumbsUp.setImageResource(R.drawable.ic_vote_inactive);
            thumbsDown.setImageResource(R.drawable.ic_no_vote_inactive);
            thumbsUp.setEnabled(true);
            thumbsDown.setEnabled(true);
        }

    }

    private void cannotAction(EventModel model) {
        thumbsUp.setEnabled(false);
        thumbsDown.setEnabled(false);
        if (model.getReply().equalsIgnoreCase("Yes")) {
            thumbsUp.setImageResource(R.drawable.ic_vote_active);
            thumbsDown.setImageResource(R.drawable.ic_no_vote_inactive);

        } else if (model.getReply().equalsIgnoreCase("No")) {
            thumbsUp.setImageResource(R.drawable.ic_vote_inactive);
            thumbsDown.setImageResource(R.drawable.ic_no_vote_active);


        } else if (model.getReply().equalsIgnoreCase("NO_RESPONSE")) {
            thumbsUp.setImageResource(R.drawable.ic_vote_inactive);
            thumbsDown.setImageResource(R.drawable.ic_no_vote_inactive);

        }

    }

    private void castVote(final String response) {

        GrassrootRestService.getInstance().getApi().castVote(voteid, phoneNumber, code, response).
                enqueue(new Callback<GenericResponse>() {
                    @Override
                    public void onResponse(Call<GenericResponse> call, Response<GenericResponse> response) {
                        if (response.isSuccessful()) {
                            showSnackBar(getString(R.string.ga_Votesend), "", "", "", Snackbar.LENGTH_SHORT);
                            updateView();
                        }
                    }
                    @Override
                    public void onFailure(Call<GenericResponse> call, Throwable t) {
                        progressBarCircularIndeterminate.setVisibility(View.GONE);
                        txtPrg.setVisibility(View.GONE);
                        ErrorUtils.connectivityError(ViewVoteActivity.this, R.string.No_network, new NetworkErrorDialogListener() {
                            @Override
                            public void retryClicked() {
                                castVote(response);
                            }
                        });


                       // ErrorUtils.handleNetworkError(ViewVoteActivity.this, errorLayout, t);
                    }
                });
    }


    @OnClick(R.id.thumbs_down)
    public void voteNo() {
        castVote("No");
    }

    @OnClick(R.id.thumbs_up)
    public void voteYes() {
        castVote("Yes");
    }

    @OnClick(R.id.iv_expand)
    public void onExpandableHeaderClickClick(View v) {
        if (mLinearLayout.getVisibility() == View.GONE) {
            ivExpand.setImageResource(R.drawable.ic_arrow_up);
            expand();
        } else {
            ivExpand.setImageResource(R.drawable.ic_arrow_down);
            collapse();
        }

    }

    @OnClick(R.id.bt_editVote)
    public void editVote() {
        if (canEdit) {
            Intent i = new Intent(ViewVoteActivity.this, EditVoteActivity.class);
            i.putExtra("description", description);
            i.putExtra("deadline", deadline);
            i.putExtra("voteid", voteid);
            i.putExtra("title", title);
            startActivityForResult(i, 1);

        }
    }

    public void expand() {
        mLinearLayout.setVisibility(View.VISIBLE);
        mAnimator.start();
    }

    private void collapse() {
        int finalHeight = mLinearLayout.getHeight();
        ValueAnimator mAnimator = slideAnimator(finalHeight, 0);
        mAnimator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationEnd(Animator animator) {
                //Height=0, but it set visibility to GONE
                mLinearLayout.setVisibility(View.GONE);

            }
            @Override
            public void onAnimationStart(Animator animator) {
            }
            @Override
            public void onAnimationCancel(Animator animator) {
            }
            @Override
            public void onAnimationRepeat(Animator animator) {
            }
        });
        mAnimator.start();
    }


    private ValueAnimator slideAnimator(int start, int end) {

        ValueAnimator animator = ValueAnimator.ofInt(start, end);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                //Update Height
                int value = (Integer) valueAnimator.getAnimatedValue();
                ViewGroup.LayoutParams layoutParams = mLinearLayout.getLayoutParams();
                layoutParams.height = value;
                mLinearLayout.setLayoutParams(layoutParams);
            }
        });
        return animator;
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);


        if (resultCode == 1 && requestCode == 1) {
            Log.e(this.TAG, "resultCode==1 ");
            if (data != null) {
                showSnackBar(getString(R.string.vv_voteup), "", "", "", Snackbar.LENGTH_SHORT);
                txtVvDeadline.setText(data.getStringExtra("deadline"));
                deadline = data.getStringExtra("deadline");
                txtVvDescription.setText(data.getStringExtra("description"));
                description = data.getStringExtra("description");
            }
        } else {
             updateView();
            Log.e(this.TAG, "resultCode==2");
        }
    }


    private void showSnackBar(String message, final String actionButtontext, final String type, final String response, int length) {
        snackbar = Snackbar.make(vvRoot, message, length);
        snackbar.setActionTextColor(Color.RED);
        if (!actionButtontext.isEmpty()) {
            snackbar.setAction(actionButtontext, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (type.equalsIgnoreCase("VoteMeeting")) {
                        castVote(response);
                    } else {
                        updateView();
                    }
                }
            });
        }
        snackbar.show();

    }


}

