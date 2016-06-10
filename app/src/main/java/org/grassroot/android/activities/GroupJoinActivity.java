package org.grassroot.android.activities;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.grassroot.android.R;
import org.grassroot.android.adapters.JoinRequestAdapter;
import org.grassroot.android.fragments.dialogs.ConfirmCancelDialogFragment;
import org.grassroot.android.interfaces.ClickListener;
import org.grassroot.android.interfaces.NetworkErrorDialogListener;
import org.grassroot.android.models.GenericResponse;
import org.grassroot.android.models.GroupSearchModel;
import org.grassroot.android.models.GroupSearchResponse;
import org.grassroot.android.services.GrassrootRestService;
import org.grassroot.android.ui.views.CustomItemAnimator;
import org.grassroot.android.ui.views.RecyclerTouchListener;
import org.grassroot.android.utils.ErrorUtils;
import org.grassroot.android.utils.PreferenceUtils;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnEditorAction;
import butterknife.OnTextChanged;
import butterknife.OnTouch;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class GroupJoinActivity extends PortraitActivity implements OnClickListener {

    private static final String TAG = GroupJoinActivity.class.getSimpleName();

    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.am_txt_toolbar)
    TextView txtToolbar;
    @BindView(R.id.et_searchbox)
    EditText et_searchbox;
    @BindView(R.id.jr_RecyclerView)
    RecyclerView jrRecyclerView;
    @BindView(R.id.im_no_results)
    ImageView imNOResults;
    @BindView(R.id.im_server_error)
    ImageView imServerError;
    @BindView(R.id.im_no_internet)
    ImageView imNOInternet;
    @BindView(R.id.rl_root)
    RelativeLayout rlRoot;
    @BindView(R.id.error_layout)
    View errorLayout;

    private JoinRequestAdapter joinrequestAdapter;
    private List<GroupSearchModel> joinrequestList;

    private boolean btn_close;
    private Snackbar snackbar;
    private String uid;

    private ProgressDialog progressDialog;
    private String prgMessage = "Please Wait..";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.e(TAG, "activity created!");
        setContentView(R.layout.activity_join__request);
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage(prgMessage);
        ButterKnife.bind(this);
        setUpToolbar();
        init();
        setUpRecyclerView();
    }

    private void setUpRecyclerView() {
        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        jrRecyclerView.setHasFixedSize(true);

        jrRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        jrRecyclerView.setItemAnimator(new CustomItemAnimator());

        // specify an adapter
        joinrequestAdapter = new JoinRequestAdapter(getApplicationContext(), new ArrayList<GroupSearchModel>());
        jrRecyclerView.setAdapter(joinrequestAdapter);

        // setUpRecyclerView.setItemAnimator(new DefaultItemAnimator());
        jrRecyclerView.addOnItemTouchListener(new RecyclerTouchListener(getApplicationContext(), jrRecyclerView, new ClickListener() {
            @Override
            public void onClick(View view, int position) {
                uid = joinrequestList.get(position).getId();
                ConfirmCancelDialogFragment.newInstance(R.string.alertbox, new ConfirmCancelDialogFragment.ConfirmDialogListener() {
                    @Override
                    public void doConfirmClicked() {
                        sendJoinRequest();
                    }
                }).show(getSupportFragmentManager(), "confirm");
            }

            @Override
            public void onLongClick(View view, int position) {
                // todo: add a modal with group description
            }
        }));
    }


    private void init() {
        joinrequestList = new ArrayList<>();
    }

    @OnTextChanged(R.id.et_searchbox)
    public void setSearchCross(CharSequence s) {
        if (s.length() > 0) {
            et_searchbox.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.btn_close, 0);
            btn_close = true;
        } else {
            et_searchbox.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.btn_search_gray, 0);
            btn_close = false;
        }
    }

    @OnTouch(R.id.et_searchbox)
    public boolean setTextBlank(View v, MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP) {
            if (event.getRawX() >= (et_searchbox.getRight() - et_searchbox.getTotalPaddingRight())) {
                // your action here
                if (btn_close) {
                    //   search();
                    et_searchbox.setText("");
                }
                return true;
            }
        }
        return false;
    }

    @OnEditorAction(R.id.et_searchbox)
    public boolean takeAction(TextView v, int actionId, KeyEvent event) {
        if (actionId == EditorInfo.IME_ACTION_SEARCH || event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
            Log.e(TAG, "action pressed!");
            InputMethodManager imm = (InputMethodManager) getApplicationContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);

            if (et_searchbox.getText().toString().trim().isEmpty()) {
                showSnackBar(getString(R.string.validate_search_box), "", Snackbar.LENGTH_SHORT);
            } else {
                search();
            }
            return true;
        } else {
            return false;
        }
    }

    private void search() {

        String searchTerm = et_searchbox.getText().toString().trim();
        joinrequestList.clear();
        hideErrorLayout();

        GrassrootRestService.getInstance().getApi().search(searchTerm)
                .enqueue(new Callback<GroupSearchResponse>() {
                    @Override
                    public void onResponse(Call<GroupSearchResponse> call, Response<GroupSearchResponse> response) {
                        progressDialog.dismiss();
                        if (response.isSuccessful()) {
                            jrRecyclerView.setVisibility(View.VISIBLE);
                            joinrequestAdapter.clearApplications();
                            joinrequestList = response.body().getGroups();
                            joinrequestAdapter.addResults(joinrequestList);
                        } else {
                            showNoResultsErrorLayout();
                            ErrorUtils.handleServerError(rlRoot, GroupJoinActivity.this, response);
                        }
                    }

                    @Override
                    public void onFailure(Call<GroupSearchResponse> call, Throwable t) {
                        progressDialog.dismiss();
                        showNetworkErrorLayout();
                        ErrorUtils.connectivityError(GroupJoinActivity.this, R.string.error_no_network, new NetworkErrorDialogListener() {
                            @Override
                            public void retryClicked() {
                                search();
                            }
                        });
                    }
                });
    }

    private void showNetworkErrorLayout() {
        jrRecyclerView.setVisibility(View.GONE);
        errorLayout.setVisibility(View.VISIBLE);
        imNOInternet.setVisibility(View.VISIBLE);

    }

    private void showNoResultsErrorLayout() {
        jrRecyclerView.setVisibility(View.GONE);
        errorLayout.setVisibility(View.VISIBLE);
        imNOResults.setVisibility(View.VISIBLE);

    }

    private void hideErrorLayout() {
        jrRecyclerView.setVisibility(View.INVISIBLE);
        errorLayout.setVisibility(View.INVISIBLE);
        imNOInternet.setVisibility(View.INVISIBLE);
        imNOResults.setVisibility(View.INVISIBLE);
        imServerError.setVisibility(View.INVISIBLE);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

    }

    private void sendJoinRequest() {
        Log.e(TAG, "sendJoinRequest");
        progressDialog.show();
        String phoneNumber = PreferenceUtils.getuser_mobilenumber(this);
        String code = PreferenceUtils.getuser_token(this);
        GrassrootRestService.getInstance().getApi().groupJoinRequest(phoneNumber, code, uid)
                .enqueue(new Callback<GenericResponse>() {
                    @Override
                    public void onResponse(Call<GenericResponse> call, Response<GenericResponse> response) {
                        progressDialog.dismiss();
                        if (response.isSuccessful()) {
                            AlertDialog.Builder builder = new AlertDialog.Builder(GroupJoinActivity.this);
                            builder.setMessage(R.string.Title_Join_Request_Sent)
                                    .setPositiveButton(R.string.pp_OK, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            finish();
                                        }
                                    }).create().show();
                            return;
                        }
                        ErrorUtils.handleServerError(rlRoot, GroupJoinActivity.this, response);
                        showSnackBar(getString(R.string.USER_ALREADY_PART), "", snackbar.LENGTH_LONG);
                    }

                    @Override
                    public void onFailure(Call<GenericResponse> call, Throwable t) {
                        progressDialog.dismiss();
                        ErrorUtils.connectivityError(GroupJoinActivity.this, R.string.error_no_network, new NetworkErrorDialogListener() {
                            @Override
                            public void retryClicked() {
                                sendJoinRequest();
                            }
                        });
                    }
                });
    }

    private void setUpToolbar() {
        toolbar.setNavigationIcon(R.drawable.btn_back_wt);
        toolbar.setNavigationOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }


    private void showSnackBar(String message, String buttontext, int length) {
        snackbar = Snackbar.make(rlRoot, message, length);
        snackbar.setActionTextColor(Color.RED);

        if (!buttontext.isEmpty()) {
            snackbar.setAction(buttontext, new OnClickListener() {
                @Override
                public void onClick(View v) {
                    sendJoinRequest();
                }
            });
        }
        snackbar.show();
    }

    @OnClick({R.id.im_no_internet, R.id.im_no_results, R.id.im_server_error})
    public void onClick(View v) {
        if (v == imNOInternet || v == imNOResults || v == imNOResults)
            search();

    }
}