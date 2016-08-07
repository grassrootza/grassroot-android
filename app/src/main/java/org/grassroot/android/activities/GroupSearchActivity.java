package org.grassroot.android.activities;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
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
import org.grassroot.android.adapters.PublicGroupAdapter;
import org.grassroot.android.fragments.dialogs.SendJoinRequestFragment;
import org.grassroot.android.interfaces.ClickListener;
import org.grassroot.android.interfaces.GroupConstants;
import org.grassroot.android.interfaces.NetworkErrorDialogListener;
import org.grassroot.android.models.GenericResponse;
import org.grassroot.android.models.PublicGroupModel;
import org.grassroot.android.models.GroupSearchResponse;
import org.grassroot.android.services.GrassrootRestService;
import org.grassroot.android.adapters.RecyclerTouchListener;
import org.grassroot.android.utils.ErrorUtils;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnEditorAction;
import butterknife.OnTextChanged;
import butterknife.OnTouch;
import org.grassroot.android.utils.RealmUtils;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class GroupSearchActivity extends PortraitActivity {

    private static final String TAG = GroupSearchActivity.class.getSimpleName();

    @BindView(R.id.rl_root) RelativeLayout rlRoot;

    @BindView(R.id.toolbar) Toolbar toolbar;
    @BindView(R.id.et_searchbox) EditText et_searchbox;

    @BindView(R.id.jr_RecyclerView) RecyclerView jrRecyclerView;

    @BindView(R.id.gs_rl_text_icon) RelativeLayout rlIconText;
    @BindView(R.id.gs_im_icon) ImageView imDisplayIcon;
    @BindView(R.id.gs_text_body) TextView tvDisplayText;

    private PublicGroupAdapter groupAdapter;

    private boolean btn_close;
    private String uid;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_search);
        ButterKnife.bind(this);

        progressDialog = new ProgressDialog(this);
        progressDialog.setIndeterminate(true);
        progressDialog.setMessage(getString(R.string.txt_pls_wait));

        setUpToolbar();
        switchToOpeningText();
        setUpRecyclerView();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        progressDialog.dismiss();
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

    private void setUpDescription() {
        tvDisplayText.setText(R.string.gs_describe_activity);
        imDisplayIcon.setVisibility(View.GONE);
    }

    private void setUpRecyclerView() {
        jrRecyclerView.setHasFixedSize(true);
        jrRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        groupAdapter = new PublicGroupAdapter(getApplicationContext(), new ArrayList<PublicGroupModel>());
        jrRecyclerView.setAdapter(groupAdapter);

        jrRecyclerView.addOnItemTouchListener(new RecyclerTouchListener(getApplicationContext(), jrRecyclerView, new ClickListener() {
            @Override
            public void onClick(View view, int position) {
                PublicGroupModel group = groupAdapter.getPublicGroup(position);
                SendJoinRequestFragment.newInstance(group, new SendJoinRequestFragment.SendJoinRequestListener() {
                    @Override
                    public void requestConfirmed(PublicGroupModel groupModel) {
                        Log.e(TAG, "send message! with this message: " + groupModel.getDescription());
                        sendJoinRequest(groupModel);
                    }
                }).show(getSupportFragmentManager(), "send_message");
            }

            @Override
            public void onLongClick(View view, int position) {
                // todo: add a modal with group description
            }
        }));
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
                if (btn_close) {
                    et_searchbox.setText("");
                }
                return true;
            }
        }
        return false;
    }

    @OnEditorAction(R.id.et_searchbox)
    public boolean searchForGroup(TextView v, int actionId, KeyEvent event) {
        if (actionId == EditorInfo.IME_ACTION_SEARCH || event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
            InputMethodManager imm = (InputMethodManager) getApplicationContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);

            if (TextUtils.isEmpty(et_searchbox.getText())) {
                ErrorUtils.showSnackBar(rlRoot, R.string.validate_search_box, Snackbar.LENGTH_SHORT);
            } else {
                search();
            }
            return true;
        } else {
            return false;
        }
    }

    private void search() {
        final String mobile = RealmUtils.loadPreferencesFromDB().getMobileNumber();
        final String code = RealmUtils.loadPreferencesFromDB().getToken();
        final String searchTerm = et_searchbox.getText().toString().trim();
        // todo : switch to Rx
        GrassrootRestService.getInstance().getApi().search(mobile, code, searchTerm)
                .enqueue(new Callback<GroupSearchResponse>() {
                    @Override
                    public void onResponse(Call<GroupSearchResponse> call, Response<GroupSearchResponse> response) {
                        progressDialog.dismiss();
                        if (response.isSuccessful() && response.body().getMessage().equals(GroupConstants.POSSIBLE_MATCHES)) {
                            switchToResultsList();
                            jrRecyclerView.setVisibility(View.VISIBLE);
                            groupAdapter.clearApplications();
                            groupAdapter.addResults(response.body().getGroups());
                        } else {
                            switchToNoResults();
                        }
                    }

                    @Override
                    public void onFailure(Call<GroupSearchResponse> call, Throwable t) {
                        progressDialog.dismiss();
                        ErrorUtils.connectivityError(GroupSearchActivity.this, R.string.error_no_network, new NetworkErrorDialogListener() {
                            @Override
                            public void retryClicked() {
                                search();
                            }
                        });
                    }
                });
    }

    private void switchToResultsList() {
        rlIconText.setVisibility(View.GONE);
        jrRecyclerView.setVisibility(View.VISIBLE);
    }

    private void switchToOpeningText() {
        jrRecyclerView.setVisibility(View.GONE);
        rlIconText.setVisibility(View.VISIBLE);
        imDisplayIcon.setVisibility(View.GONE);
        tvDisplayText.setVisibility(View.VISIBLE);
        tvDisplayText.setText(R.string.gs_describe_activity);
    }

    private void switchToNoResults() {
        jrRecyclerView.setVisibility(View.GONE);
        rlIconText.setVisibility(View.VISIBLE);
        imDisplayIcon.setVisibility(View.VISIBLE);
        tvDisplayText.setVisibility(View.VISIBLE);
        tvDisplayText.setText(R.string.gs_none_found_text);
    }

    private void sendJoinRequest(final PublicGroupModel groupModel) {
        progressDialog.show();
        final String phoneNumber = RealmUtils.loadPreferencesFromDB().getMobileNumber();
        final String code = RealmUtils.loadPreferencesFromDB().getToken();
        GrassrootRestService.getInstance().getApi().sendGroupJoinRequest(phoneNumber, code,
                groupModel.getId(), groupModel.getDescription()).enqueue(new Callback<GenericResponse>() {
                    @Override
                    public void onResponse(Call<GenericResponse> call, Response<GenericResponse> response) {
                        progressDialog.dismiss();
                        if (response.isSuccessful()) {
                            AlertDialog.Builder builder = new AlertDialog.Builder(GroupSearchActivity.this);
                            builder.setMessage(R.string.Title_Join_Request_Sent)
                                    .setPositiveButton(R.string.pp_OK, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            finish();
                                        }
                                    }).create().show();
                        } else {
                            final String errorMsg = ErrorUtils.serverErrorText(response.errorBody(), GroupSearchActivity.this);
                            Snackbar.make(rlRoot, errorMsg, Snackbar.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<GenericResponse> call, Throwable t) {
                        progressDialog.dismiss();
                        ErrorUtils.connectivityError(GroupSearchActivity.this, R.string.error_no_network, new NetworkErrorDialogListener() {
                            @Override
                            public void retryClicked() {
                                sendJoinRequest(groupModel);
                            }
                        });
                    }
                });
    }
}