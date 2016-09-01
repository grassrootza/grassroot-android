package org.grassroot.android.fragments;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.grassroot.android.R;
import org.grassroot.android.adapters.JoinRequestAdapter;
import org.grassroot.android.events.JoinRequestEvent;
import org.grassroot.android.fragments.dialogs.NetworkErrorDialogFragment;
import org.grassroot.android.interfaces.GroupConstants;
import org.grassroot.android.models.GroupJoinRequest;
import org.grassroot.android.services.ApplicationLoader;
import org.grassroot.android.services.GroupSearchService;
import org.grassroot.android.services.GroupService;
import org.grassroot.android.utils.ErrorUtils;
import org.grassroot.android.utils.NetworkUtils;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.observers.Subscribers;

/**
 * Created by luke on 2016/07/14.
 */
public class JoinRequestListFragment extends Fragment implements JoinRequestAdapter.JoinRequestClickListener {

    private static final String TAG = JoinRequestListFragment.class.getSimpleName();

    private JoinRequestAdapter adapter;
    private String type;

    @BindView(R.id.jreq_swipe_refresh) SwipeRefreshLayout swipeRefreshLayout;
    @BindView(R.id.jreq_recycler_view) RecyclerView recyclerView;
    @BindView(R.id.jreq_no_requests) TextView noRequestsMessage;

		@BindView(R.id.progressBar) ProgressBar progressBar;

    Unbinder unbinder;

    public static JoinRequestListFragment newInstance(final String type) {
        JoinRequestListFragment fragment = new JoinRequestListFragment();
        Bundle args = new Bundle();
        args.putString("type", type);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        EventBus.getDefault().register(this);

        type = getArguments().getString("type");
        if (!GroupJoinRequest.SENT_REQUEST.equals(type) && !GroupJoinRequest.REC_REQUEST.equals(type)) {
            type = GroupJoinRequest.SENT_REQUEST; // default to sent if request is malformed
        }

        adapter = new JoinRequestAdapter(context, type, this);
    }


    @Override
    public void backgroundCallComplete() {
        selectMessageOrList();
    }

    @Override
    public void positiveClicked(GroupJoinRequest request, int position) {
        if (GroupJoinRequest.SENT_REQUEST.equals(request.getJoinReqType())) {
            remindAboutJoinRequest(request.getGroupUid());
        } else {
            respondToJoinRequest(GroupConstants.APPROVE_JOIN_REQUEST, request.getRequestUid(), position);
        }
    }

    @Override
    public void negativeClicked(final GroupJoinRequest request, final int position) {
        if (GroupJoinRequest.SENT_REQUEST.equals(request.getJoinReqType())) {
            new AlertDialog.Builder(getContext())
                .setMessage(R.string.jreq_cancel_confirm)
                .setCancelable(true)
                .setPositiveButton(R.string.alert_confirm, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            cancelJoinRequest(request.getGroupUid(), position);
                        }
                    })
                .show();
        } else {
            respondToJoinRequest(GroupConstants.DENY_JOIN_REQUEST, request.getRequestUid(), position);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_join_request_list, container, false);
        unbinder = ButterKnife.bind(this, view);
        noRequestsMessage.setText(type.equals(GroupJoinRequest.SENT_REQUEST) ?
            R.string.jreq_no_requests_sent : R.string.jreq_no_requests);
        setUpRecyclerView();
        return view;
    }

    @Override
    public void onResume() {
      super.onResume();

    }

    private void setUpRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setHasFixedSize(true);
        recyclerView.setAdapter(adapter);
        recyclerView.setItemAnimator(null);

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
        } else  {
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

    private void refreshJoinRequests() {
        swipeRefreshLayout.setRefreshing(true);
        GroupService.getInstance().fetchGroupJoinRequests(AndroidSchedulers.mainThread())
            .subscribe(new Action1<String>() {
				@Override
				public void call(String s) {
              switch (s) {
                case NetworkUtils.FETCHED_SERVER:
                  adapter.refreshList();
									EventBus.getDefault().post(new JoinRequestEvent(TAG));
                  break;
                case NetworkUtils.OFFLINE_SELECTED:
									NetworkErrorDialogFragment.newInstance(R.string.connect_error_jreqs_offline, progressBar,
											goOnlineSubscriber());
                  break;
                case NetworkUtils.CONNECT_ERROR:
                  ErrorUtils.networkErrorSnackbar(swipeRefreshLayout, R.string.connect_error_jreqs_list, new View.OnClickListener() {
										@Override
										public void onClick(View v) {
											refreshJoinRequests();
										}
									});
                  break;
                case NetworkUtils.SERVER_ERROR:
									Snackbar.make(swipeRefreshLayout, R.string.server_error_general, Snackbar.LENGTH_SHORT).show();
              }
              hideProgess();
				}
			});
    }

	private Subscriber<String> goOnlineSubscriber() {
		return Subscribers.create(new Action1<String>() {
			@Override
			public void call(String s) {
				progressBar.setVisibility(View.GONE);
				if (s.equals(NetworkUtils.SERVER_ERROR)) {
					Snackbar.make(swipeRefreshLayout, R.string.connect_error_failed_retry, Snackbar.LENGTH_SHORT).show();
				} else {
					refreshJoinRequests();
				}
			}
		});
	}

    private void respondToJoinRequest(final String approvedOrDenied, final String requestUid, final int position) {
        showProgress();
        GroupService.getInstance().respondToJoinRequest(approvedOrDenied, requestUid, AndroidSchedulers.mainThread())
            .subscribe(new Subscriber<String>() {
                @Override
                public void onNext(String s) {
                    adapter.clearRequest(position);
                    hideProgess();
                    int snackMsg = approvedOrDenied.equals(GroupConstants.APPROVE_JOIN_REQUEST) ?
                        R.string.jreq_approved : R.string.jreq_denied;
                    Toast.makeText(ApplicationLoader.applicationContext, snackMsg, Toast.LENGTH_SHORT).show();
                    selectMessageOrList();
										EventBus.getDefault().post(new JoinRequestEvent(TAG));
                }

                @Override
                public void onError(Throwable e) {
                    hideProgess();
                    if (NetworkUtils.CONNECT_ERROR.equals(e.getMessage())) {
                        Snackbar.make(swipeRefreshLayout, R.string.jreq_response_connect_error, Snackbar.LENGTH_SHORT).show();
                    } else {
                        Snackbar.make(swipeRefreshLayout, ErrorUtils.serverErrorText(e), Snackbar.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onCompleted() {}
            });
    }

    private void remindAboutJoinRequest(final String groupUid) {
        progressBar.setVisibility(View.VISIBLE);
        GroupSearchService.getInstance().remindJoinRequest(groupUid, AndroidSchedulers.mainThread())
            .subscribe(new Subscriber<String>() {
                @Override
                public void onNext(String s) {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(ApplicationLoader.applicationContext, R.string.gs_req_remind, Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onError(Throwable e) {
                    progressBar.setVisibility(View.GONE);
                    if (NetworkUtils.CONNECT_ERROR.equals(e.getMessage())) {
                        final String errorMsg = getString(R.string.gs_req_remind_cancelled_connect_error);
                        Snackbar.make(swipeRefreshLayout, errorMsg, Snackbar.LENGTH_SHORT).show();
                    } else {
                        Snackbar.make(swipeRefreshLayout, ErrorUtils.serverErrorText(e),
                            Snackbar.LENGTH_LONG).show();
                    }
                }

                @Override
                public void onCompleted() { }
            });
    }

    private void cancelJoinRequest(final String groupUid, final int position) {
        progressBar.setVisibility(View.VISIBLE);
        GroupSearchService.getInstance().cancelJoinRequest(groupUid, AndroidSchedulers.mainThread())
            .subscribe(new Subscriber<String>() {
                @Override
                public void onNext(String s) {
                  progressBar.setVisibility(View.GONE);
                  Toast.makeText(ApplicationLoader.applicationContext, R.string.gs_req_cancelled,
                      Toast.LENGTH_SHORT).show();
                  adapter.clearRequest(position);
                  selectMessageOrList();
									EventBus.getDefault().post(new JoinRequestEvent(TAG));
                }

                @Override
                public void onError(Throwable e) {
                    progressBar.setVisibility(View.GONE);
                    if (NetworkUtils.CONNECT_ERROR.equals(e.getMessage())) {
                        final String errorMsg = getString(R.string.gs_req_remind_cancelled_connect_error);
                        Snackbar.make(swipeRefreshLayout, errorMsg, Snackbar.LENGTH_SHORT).show();
                    } else {
                        Snackbar.make(swipeRefreshLayout, ErrorUtils.serverErrorText(e),
                            Snackbar.LENGTH_LONG).show();
                    }
                }

                @Override
                public void onCompleted() { }
            });

    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void groupJoinRequestReceived(JoinRequestEvent e) {
			Log.e(TAG, "got a join request! with tag ... " + e.getTAG());
      if (!TAG.equals(e.getTAG()) && swipeRefreshLayout != null) {
          adapter.refreshList();
      }
    }

    private void showProgress() {
        progressBar.setVisibility(View.VISIBLE);
    }

    private void hideProgess() {
        // callbacks might happen after view destroyed
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setRefreshing(false);
        }
        if (progressBar != null) {
            progressBar.setVisibility(View.GONE);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        EventBus.getDefault().unregister(this);
        unbinder.unbind();
    }

}
