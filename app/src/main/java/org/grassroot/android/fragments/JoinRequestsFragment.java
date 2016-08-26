package org.grassroot.android.fragments;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import org.grassroot.android.R;
import org.grassroot.android.adapters.JoinRequestAdapter;
import org.grassroot.android.events.JoinRequestReceived;
import org.grassroot.android.interfaces.GroupConstants;
import org.grassroot.android.models.GenericResponse;
import org.grassroot.android.models.GroupJoinRequest;
import org.grassroot.android.services.GrassrootRestService;
import org.grassroot.android.services.GroupService;
import org.grassroot.android.utils.ErrorUtils;
import org.grassroot.android.utils.NetworkUtils;
import org.grassroot.android.utils.RealmUtils;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;

/**
 * Created by luke on 2016/07/14.
 */
public class JoinRequestsFragment extends Fragment {

    private static final String TAG = JoinRequestsFragment.class.getSimpleName();

    private JoinRequestAdapter adapter;
    private String mobile;
    private String code;

    @BindView(R.id.jreq_fl_root) FrameLayout frameLayout;
    @BindView(R.id.jreq_swipe_refresh) SwipeRefreshLayout swipeRefreshLayout;
    @BindView(R.id.jreq_recycler_view) RecyclerView recyclerView;
    @BindView(R.id.jreq_no_requests) TextView noRequestsMessage;

    Unbinder unbinder;
    ProgressDialog progressDialog;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        EventBus.getDefault().register(this);
        mobile = RealmUtils.loadPreferencesFromDB().getMobileNumber();
        code = RealmUtils.loadPreferencesFromDB().getToken();

        adapter = new JoinRequestAdapter(context, new JoinRequestAdapter.JoinRequestClickListener() {
            @Override
            public void requestApproved(GroupJoinRequest request, int position) {
                respondToJoinRequest(GroupConstants.JOIN_REQUEST_APPROVE, request.getRequestUid(), position);
            }

            @Override
            public void requestDenied(GroupJoinRequest request, int position) {
                respondToJoinRequest(GroupConstants.JOIN_REQUEST_DENY, request.getRequestUid(), position);
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_join_requests, container, false);
        unbinder = ButterKnife.bind(this, view);
        setUpRecyclerView();
        selectMessageOrList();
        return view;
    }

    private void setUpRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setHasFixedSize(true);
        recyclerView.setAdapter(adapter);
        swipeRefreshLayout.setColorSchemeColors(ContextCompat.getColor(getActivity(), R.color.primaryColor));
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refreshJoinRequests();
            }
        });
    }

    private void selectMessageOrList() {
        if (adapter.getItemCount() == 0) {
            switchToEmptyList();
        } else {
            switchToShownList();
        }
    }

    private void switchToEmptyList() {
        recyclerView.setVisibility(View.GONE);
        noRequestsMessage.setVisibility(View.VISIBLE);
    }

    private void switchToShownList() {
        recyclerView.setVisibility(View.VISIBLE);
        noRequestsMessage.setVisibility(View.GONE);
    }

    // todo : differentiate among these
    private void refreshJoinRequests() {
        swipeRefreshLayout.setRefreshing(true);
        GroupService.getInstance().fetchGroupJoinRequests(AndroidSchedulers.mainThread())
			.subscribe(new Action1<String>() {
				@Override
				public void call(String s) {
					switch (s) {
						case NetworkUtils.FETCHED_SERVER:
							adapter.refreshList();
							selectMessageOrList();
							break;
						case NetworkUtils.OFFLINE_SELECTED:
							// todo : show an error message
							break;
						case NetworkUtils.CONNECT_ERROR:
							// todo : show an error message
							break;
					}
					swipeRefreshOff(); // todo : combine these
					hideProgess();
				}
			});
    }

    private void swipeRefreshOff() {
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setRefreshing(false);
        }
    }

    private void respondToJoinRequest(final String approvedOrDenied, final String requestUid, final int position) {
        if (NetworkUtils.isOnline(getContext())) {
            showProgress();
            GrassrootRestService.getInstance().getApi().respondToJoinRequest(mobile, code,
                    requestUid, approvedOrDenied).enqueue(new Callback<GenericResponse>() {
                @Override
                public void onResponse(Call<GenericResponse> call, Response<GenericResponse> response) {
                    if (response.isSuccessful()) {
                        Log.e(TAG, "sucess! hide this, and do the remainder");
                        adapter.clearRequest(position);
                        RealmUtils.removeObjectFromDatabase(GroupJoinRequest.class, "requestUid", requestUid);
                        int snackMsg = approvedOrDenied.equals(GroupConstants.JOIN_REQUEST_APPROVE) ?
                                R.string.jreq_approved : R.string.jreq_denied;
                        ErrorUtils.showSnackBar(frameLayout, snackMsg, Snackbar.LENGTH_SHORT);
                        selectMessageOrList();
                        hideProgess();
                    } else {
                        final String errorMessage = ErrorUtils.serverErrorText(response.errorBody(), getContext());
                        Snackbar.make(frameLayout, errorMessage, Snackbar.LENGTH_SHORT).show();
                        hideProgess();
                    }
                }

                @Override
                public void onFailure(Call<GenericResponse> call, Throwable t) {
                    // todo : store this offline & send once connected
                    hideProgess();
                }
            });
        } else {
            // todo : store this offline & send once connected
        }
    }

    @OnClick(R.id.jreq_no_requests)
    public void onNoRequestsClicked() {
        showProgress();
        refreshJoinRequests();
    }

    // note : this isn't triggered anywhere yet, but will be once notifications etc properly wired
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void groupJoinRequestReceived(JoinRequestReceived e) {
        adapter.insertRequest(e.request);
    }

    private void showProgress() {
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(getContext());
            progressDialog.setIndeterminate(true);
            progressDialog.setMessage(getString(R.string.txt_pls_wait));
        }
        if (!progressDialog.isShowing()) {
            progressDialog.show();
        }
    }

    private void hideProgess() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
        if (progressDialog != null) {
            progressDialog.dismiss();
        }
        EventBus.getDefault().unregister(this);
    }

}
