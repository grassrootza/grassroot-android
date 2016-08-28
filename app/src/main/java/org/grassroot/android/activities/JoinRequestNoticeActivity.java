package org.grassroot.android.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.NavUtils;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;

import org.grassroot.android.R;
import org.grassroot.android.fragments.GiantMessageFragment;
import org.grassroot.android.interfaces.GroupConstants;
import org.grassroot.android.interfaces.NotificationConstants;
import org.grassroot.android.models.Group;
import org.grassroot.android.services.GroupService;
import org.grassroot.android.utils.ErrorUtils;
import org.grassroot.android.utils.NetworkUtils;
import org.grassroot.android.utils.RealmUtils;

import butterknife.BindView;
import butterknife.ButterKnife;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;

/**
 * Created by luke on 2016/08/28.
 */
public class JoinRequestNoticeActivity extends PortraitActivity {

	private static final String TAG = JoinRequestNoticeActivity.class.getSimpleName();

	@BindView(R.id.jrn_toolbar) Toolbar toolbar;
	@BindView(R.id.progressBar) ProgressBar progressBar;

	private String typeOfNotice;
	private String message;
	private String requestUid;
	private String groupUid;
	private Group group;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_join_req_notice);
		ButterKnife.bind(this);

		if (getIntent().getExtras() == null) {
			Log.e(TAG, "error! join request notice view called without extras");
			startActivity(ErrorUtils.gracefulExitToHome(this));
		}

		typeOfNotice = getIntent().getStringExtra(NotificationConstants.ENTITY_TYPE);
		message = getIntent().getStringExtra(NotificationConstants.BODY);

		if (TextUtils.isEmpty(typeOfNotice) || TextUtils.isEmpty(message)) {
			Log.e(TAG, "error! join request without message or type of notice");
		}

		setSupportActionBar(toolbar);
		if (getSupportActionBar() != null) {
			getSupportActionBar().setHomeButtonEnabled(true);
			getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		}
		setUpViews();

		toolbar.setNavigationIcon(R.drawable.btn_close_white);
		setTitle(""); // would just be repetitive
	}

	private void setUpViews() {
		GiantMessageFragment fragment;

		groupUid = getIntent().getStringExtra(GroupConstants.UID_FIELD);
		requestUid = getIntent().getStringExtra(NotificationConstants.ENTITY_UID);
		group = TextUtils.isEmpty(groupUid) ? null : RealmUtils.loadGroupFromDB(groupUid);

		switch (typeOfNotice) {
			case GroupConstants.JREQ_RECEIVED:
			case GroupConstants.JREQ_REMIND:
				fragment = GiantMessageFragment.newInstance(R.string.jreq_gmsg_header, message, requestUid != null, true);
				if (requestUid != null) {
					fragment.setButtonOne(R.string.jreq_btn_approve, new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							approveJoinRequest(requestUid);
						}
					});
				}
				fragment.setButtonTwo(R.string.jreq_btn_later, new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						if (group != null) {
							goToGroupTasks(group);
						} else {
							NavUtils.navigateUpFromSameTask(JoinRequestNoticeActivity.this);
						}
					}
				});
				break;

			case GroupConstants.JREQ_APPROVED:
				fragment = GiantMessageFragment.newInstance(R.string.jreq_gmsg_header_approved, message, true, false);
				fragment.setButtonOne(R.string.jreq_btn_group, new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						Group group = RealmUtils.loadGroupFromDB(groupUid);
						if (group != null) {
							goToGroupTasks(group);
						} else {
							tryFetchGroupAndExit(groupUid);
						}
					}
				});
				break;

			case GroupConstants.JREQ_DENIED:
			default:
				setTitle("");
				fragment = GiantMessageFragment.newInstance(R.string.jreq_gmsg_header_denied, message, false, false);
				break;
		}

		getSupportFragmentManager().beginTransaction()
				.add(R.id.jrn_fragment_holder, fragment)
				.commit();
	}

	private void approveJoinRequest(String requestUid) {
		progressBar.setVisibility(View.VISIBLE);
		GroupService.getInstance().respondToJoinRequest(GroupConstants.JOIN_REQUEST_APPROVE,
				requestUid, AndroidSchedulers.mainThread()).subscribe(new Subscriber<String>() {
			@Override
			public void onNext(String s) {
				progressBar.setVisibility(View.GONE);
				NavUtils.navigateUpFromSameTask(JoinRequestNoticeActivity.this);
				finish();
			}

			@Override
			public void onError(Throwable e) {
				if (NetworkUtils.CONNECT_ERROR.equals(e.getMessage())) {
					Snackbar.make(toolbar, R.string.jreq_response_connect_error, Snackbar.LENGTH_SHORT).show();
				} else {
					Snackbar.make(toolbar, ErrorUtils.serverErrorText(e), Snackbar.LENGTH_SHORT).show();
				}
			}

			@Override
			public void onCompleted() {}
		});
	}

	private void goToGroupTasks(Group groupToLoad) {
		Intent viewGroup = new Intent(JoinRequestNoticeActivity.this, GroupTasksActivity.class);
		viewGroup.putExtra(GroupConstants.OBJECT_FIELD, groupToLoad);
		startActivity(viewGroup);
		finish();
	}

	private void tryFetchGroupAndExit(final String groupUid) {
		progressBar.setVisibility(View.VISIBLE);
		GroupService.getInstance().fetchGroupList(AndroidSchedulers.mainThread()).subscribe(new Action1<String>() {
			@Override
			public void call(String s) {
				progressBar.setVisibility(View.GONE);
				if (NetworkUtils.FETCHED_SERVER.equals(s)) {
					Group fetchedGroup = RealmUtils.loadGroupFromDB(groupUid);
					if (fetchedGroup != null) {
						goToGroupTasks(fetchedGroup);
					}
				} else {
					Snackbar.make(toolbar, R.string.jreq_approved_load_error, Snackbar.LENGTH_SHORT);
				}
			}
		});
	}

}
