package com.techmorphosis.grassroot.ui.activities;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.CardView;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.techmorphosis.grassroot.R;
import com.techmorphosis.grassroot.adapters.VoteNotifyMembersAdapter;
import com.techmorphosis.grassroot.interfaces.ClickListener;
import com.techmorphosis.grassroot.services.GrassrootRestService;
import com.techmorphosis.grassroot.services.NoConnectivityException;
import com.techmorphosis.grassroot.services.model.Member;
import com.techmorphosis.grassroot.services.model.MemberList;
import com.techmorphosis.grassroot.ui.views.RecyclerTouchListener;
import com.techmorphosis.grassroot.utils.Constant;
import com.techmorphosis.grassroot.ui.views.ProgressBarCircularIndeterminate;
import com.techmorphosis.grassroot.utils.PreferenceUtils;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import retrofit2.Callback;
import retrofit2.Response;

public class VoteNotifyMembersActivity extends PortraitActivity {

    private static final String TAG = "VoteNotifyMembers";
    @BindView(R.id.tlb_nm)
    Toolbar tlbNm;
    @BindView(R.id.txt_nm_tlb)
    TextView txtNmTlb;
    @BindView(R.id.ll_nm_main_layout)
    LinearLayout llNmMainLayout;
    @BindView((R.id.card_view))
    CardView cardView;
    @BindView(R.id.rl_nm_notify_header)
    RelativeLayout rlNmNotifyHeader;
    @BindView(R.id.sw_notifyall)
    SwitchCompat swNotifyall;
    @Nullable
    @BindView(R.id.recyclerView)
    RecyclerView recyclerView;
    @BindView(R.id.btn_nm_done)
    Button btnnmdone;
    @BindView(R.id.progressBarCircularIndeterminate)
    ProgressBarCircularIndeterminate progressBarCircularIndeterminate;
    @BindView(R.id.txt_prg)
    TextView txtPrg;
    @BindView(R.id.rl_root_layout)
    RelativeLayout rlRootLayout;
    @BindView(R.id.icl_nm_error_layout)
    View iclNmErrorLayout;
    @BindView(R.id.ll_no_result)
    LinearLayout llNoResult;
    @BindView(R.id.ll_no_internet)
    LinearLayout llNoInternet;
    @BindView(R.id.ll_server_error)
    LinearLayout llServerError;
    @BindView(R.id.ll_invalid_token)
    LinearLayout llInvalidToken;
    private Snackbar snackbar;
    private String groupId;

    public ArrayList<Member> memberlist;
    public VoteNotifyMembersAdapter voteNotifyMembersAdapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vote_notify_members);
        ButterKnife.bind(this);
        groupId = getIntent().getStringExtra(Constant.GROUPUID_FIELD);
        setUpToolbar();
        memberlist = new ArrayList<>();
        Bundle b = getIntent().getExtras();
        memberlist = b.getParcelableArrayList(Constant.VotedmemberList);
        init();
        if (memberlist.size() == 0) {
            getGroupMembers();
        } else {
            setView();
        }
    }

    private void init() {
        switchOff();
        setRecyclerView();

    }

    private void setRecyclerView() {
        recyclerView.setHasFixedSize(false);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getApplicationContext());
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.addOnItemTouchListener(new RecyclerTouchListener(getApplicationContext(), recyclerView, new ClickListener() {
            @Override
            public void onClick(View view, int position) {
                Log.e(TAG, "onErrorLayoutClick ");
                Log.e(TAG, "position is  " + position);
                Member click_model = memberlist.get(position);

                if (click_model.isSelected()) {
                    Log.e(TAG, "if");
                    click_model.setSelected(false);
                } else if (!click_model.isSelected()) {
                    Log.e(TAG, "else ");
                    click_model.setSelected(true);
                }
                voteNotifyMembersAdapter.notifyDataSetChanged();
                if (getMemberCount() == memberlist.size()) {
                    switchOn();
                } else {
                    switchOff();
                }

            }

            @Override
            public void onLongClick(View view, int position) {

            }
        }));

    }

    private int getMemberCount() {
        int membercounter = 0;
        for (int i = 0; i < memberlist.size(); i++) {
            Member member = memberlist.get(i);
            if (member.isSelected()) {
                membercounter++;
            }
        }
        return membercounter;
    }

    private void switchOff() {
        swNotifyall.setChecked(false);
    }

    private void switchOn() {
        swNotifyall.setChecked(true);
    }


    private void getGroupMembers() {

        GrassrootRestService grassrootRestService = new GrassrootRestService(this);
        String phoneNumber = PreferenceUtils.getuser_mobilenumber(this);
        String code = PreferenceUtils.getuser_token(this);
        showProgress();
        grassrootRestService.getApi().getGroupMembers(groupId, phoneNumber, code, true).enqueue(
                new Callback<MemberList>() {

                    @Override
                    public void onResponse(retrofit2.Call<MemberList> call, Response<MemberList> response) {
                        if (response.isSuccessful()) {
                            memberlist.addAll(response.body().getMembers());
                            setView();
                        } else {
                            progressBarCircularIndeterminate.setVisibility(View.GONE);
                            txtPrg.setVisibility(View.GONE);
                            iclNmErrorLayout.setVisibility(View.VISIBLE);
                            llInvalidToken.setVisibility(View.VISIBLE);
                        }

                    }

                    @Override
                    public void onFailure(retrofit2.Call<MemberList> call, Throwable t) {
                        if (t instanceof NoConnectivityException) {
                            progressBarCircularIndeterminate.setVisibility(View.GONE);
                            txtPrg.setVisibility(View.GONE);
                            iclNmErrorLayout.setVisibility(View.VISIBLE);
                            llNoInternet.setVisibility(View.VISIBLE);

                        }

                    }
                }

        );
    }


    private void setView() {

        voteNotifyMembersAdapter = new VoteNotifyMembersAdapter(VoteNotifyMembersActivity.this, memberlist);
        recyclerView.setAdapter(voteNotifyMembersAdapter);
        progressBarCircularIndeterminate.setVisibility(View.GONE);
        txtPrg.setVisibility(View.GONE);

        llNmMainLayout.setVisibility(View.VISIBLE);
        switchlistner();

    }


    private void setUpToolbar() {

        tlbNm.setNavigationIcon(R.drawable.btn_back_wt);
        tlbNm.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
    }


    private void switchlistner() {

        Log.e(TAG, "switchlistner");
        swNotifyall.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                if (isChecked) {
                    Log.e(TAG, "isChecked " + true);
                    if (getMemberCount() == memberlist.size()) {
                        Log.e(TAG, "if switchlistner selectAllmember");

                    } else {
                        Log.e(TAG, "else switchlistner selectAllmember");
                        selectAllmember();
                    }

                } else {
                    Log.e(TAG, "isChecked " + false);
                    if (getMemberCount() == memberlist.size()) {
                        removeAllmember();
                        Log.e(TAG, "if  switchlistner removeAllmember");
                    } else if (getMemberCount() > 0) {
                        Log.e(TAG, "else switchlistner removeAllmember");
                    }


                }
            }
        });
    }

    private void removeAllmember() {
        Log.e(TAG, "removeAllmember");
        for (int i = 0; i < memberlist.size(); i++) {
            Member selectallmodel = memberlist.get(i);
            selectallmodel.setSelected(false);
        }

        voteNotifyMembersAdapter.notifyDataSetChanged();
    }

    private void selectAllmember() {
        Log.e(TAG, "selectAllmember");
        for (int i = 0; i < memberlist.size(); i++) {
            Member selectallmodel = memberlist.get(i);
            selectallmodel.setSelected(true);
        }

        voteNotifyMembersAdapter.notifyDataSetChanged();
    }


    @OnClick(R.id.btn_nm_done)
    public void button_done() {
        if (getMemberCount() > 0) {

            Intent i = new Intent();
            i.putParcelableArrayListExtra(Constant.VotedmemberList, memberlist);
            setResult(1, i);
            finish();
        } else {
            showSnackBar(getString(R.string.nm_vote_members_msg), Snackbar.LENGTH_SHORT, "");
        }

    }
    @OnClick({R.id.ll_no_internet, R.id.ll_server_error, R.id.ll_no_result})
    public void onErrorLayoutClick(View v) {
        switch (v.getId()) {
            case R.id.ll_no_result:
                getGroupMembers();
                break;
            case R.id.ll_server_error:
                getGroupMembers();

                break;
            case R.id.ll_no_internet:

                getGroupMembers();

                break;

        }
    }

    private void showSnackBar(String message, int length, final String actionButtontext) {
        snackbar = Snackbar.make(rlRootLayout, message, length);
        snackbar.setActionTextColor(Color.RED);

        if (!actionButtontext.isEmpty()) {
            snackbar.setAction(actionButtontext, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    getGroupMembers();
                }
            });
        }
        snackbar.show();

    }

    private void showProgress() {

        //hide the MainLayout
        llNmMainLayout.setVisibility(View.GONE);
        //hide the error Layout
        iclNmErrorLayout.setVisibility(View.GONE);
        llNoResult.setVisibility(View.GONE);
        llNoInternet.setVisibility(View.GONE);
        llServerError.setVisibility(View.GONE);
        progressBarCircularIndeterminate.setVisibility(View.VISIBLE);
        txtPrg.setVisibility(View.VISIBLE);

    }


}