package org.grassroot.android.fragments;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.grassroot.android.R;
import org.grassroot.android.adapters.PublicGroupAdapter;
import org.grassroot.android.adapters.RecyclerTouchListener;
import org.grassroot.android.fragments.dialogs.MultiLineTextDialog;
import org.grassroot.android.interfaces.ClickListener;
import org.grassroot.android.models.PublicGroupModel;
import org.grassroot.android.services.GroupSearchService;
import org.grassroot.android.utils.NetworkUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import io.reactivex.functions.Consumer;

public class GroupSearchResultsFragment extends Fragment {

	private static final String TAG = GroupSearchResultsFragment.class.getSimpleName();

	Unbinder unbinder;
	SearchResultsListener listener;

	@BindView(R.id.find_group_results_display) RecyclerView resultsDisplay;

	private PublicGroupAdapter resultsAdapter;

	public interface SearchResultsListener {
		void sendJoinRequest(PublicGroupModel groupModel);
		void cancelJoinRequest(PublicGroupModel groupModel);
		void remindJoinRequest(PublicGroupModel groupModel);
		void returnToSearchStart();
	}

	@Override
	public void onAttach(Context context) {
		super.onAttach(context);
		try {
			listener = (SearchResultsListener) context;
		} catch (ClassCastException e) {
			Log.e(TAG, "activity does not implement listener ... disabling join request sending");
		}

		refreshResultsList();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View viewToReturn = inflater.inflate(R.layout.fragment_group_search_results, container, false);
		unbinder = ButterKnife.bind(this, viewToReturn);
		setUpNameResultsDisplay();
		return viewToReturn;
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		unbinder.unbind();
	}

	public void refreshResultsList() {
		Map<Integer, Integer> sectionHeaders = new HashMap<>();
		List<PublicGroupModel> groupNameRequests = new ArrayList<>();

		if (GroupSearchService.getInstance().hasNameResults()) {
			groupNameRequests.addAll(GroupSearchService.getInstance().foundByGroupName);
			sectionHeaders.put(0, R.string.find_group_byname_header);
		}

		if (GroupSearchService.getInstance().hasSubjectResults()) {
			groupNameRequests.addAll(GroupSearchService.getInstance().foundByTaskName);
			if (GroupSearchService.getInstance().hasNameResults()) {
				// need to account for opening header, hence + 1
				sectionHeaders.put(GroupSearchService.getInstance().foundByGroupName.size() + 1,
					R.string.find_group_subject_header_short);
			} else {
				sectionHeaders.put(0, R.string.find_group_subject_header_long);
			}
		}

		if (resultsAdapter == null) {
			resultsAdapter = new PublicGroupAdapter(getContext(), groupNameRequests, sectionHeaders);
		} else  {
			resultsAdapter.resetResults(groupNameRequests, sectionHeaders);
		}
	}

	private void setUpNameResultsDisplay() {
		resultsDisplay.setAdapter(resultsAdapter);
		resultsDisplay.setLayoutManager(new LinearLayoutManager(getContext()));
		resultsDisplay.setItemViewCacheSize(10);
		resultsDisplay.setDrawingCacheEnabled(true);
		resultsDisplay.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_AUTO);

		// not sure if we need this ...
		if (resultsAdapter.getItemCount() > 0) {
			resultsDisplay.setVisibility(View.VISIBLE);
			setRecyclerViewTouch(resultsDisplay, resultsAdapter);
		} else {
			resultsDisplay.setVisibility(View.GONE);
		}
	}

	private void setRecyclerViewTouch(RecyclerView recyclerView, final PublicGroupAdapter adapter) {
		if (listener != null) {
			recyclerView.addOnItemTouchListener(new RecyclerTouchListener(getContext(), recyclerView, new ClickListener() {
				@Override
				public void onClick(View view, int position) {
					if (resultsAdapter.getItemViewType(position) != 0) {
						sendJoinRequest(adapter.getPublicGroup(position, true), position);
					}
				}

				@Override
				public void onLongClick(View view, int position) { }
			}));
		}
	}

	private void sendJoinRequest(final PublicGroupModel group, final int adapterViewPosition) {
		if (!group.isHasOpenRequest()) {
			String description = TextUtils.isEmpty(group.getDescription()) ?
					getString(R.string.gs_dialog_no_desc_format, group.getGroupName(),
						group.getCreatedDate(), group.getGroupCreator(), group.getMemberCount()) :
					getString(R.string.group_description_prefix, group.getDescription());

			MultiLineTextDialog.showMultiLineDialog(getFragmentManager(), R.string.gs_dialog_title, description,
					R.string.gs_dialog_message_hint, R.string.gs_dialog_send).subscribe(new Consumer<String>() {
				@Override
				public void accept(String s) {
					group.setDescription(s);
					listener.sendJoinRequest(group);
					resultsAdapter.toggleRequestSent(adapterViewPosition, true);
				}
			});
		} else {
			AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
			// might prefer to have verbose items and simple title ... to test w/users
			builder.setMessage(R.string.gs_req_open_dialog);
			// handling offline caching of cancel or remind on join request more complexity than worth, at present
			if (NetworkUtils.isOnline()) {
				builder.setPositiveButton(R.string.gs_req_open_remind, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						listener.remindJoinRequest(group);
					}
				})
					.setNegativeButton(R.string.gs_req_open_cancel, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							listener.cancelJoinRequest(group);
							resultsAdapter.toggleRequestSent(adapterViewPosition, false);
						}
					});
			}
			builder.create().show();
		}
	}

	@OnClick(R.id.fgroup_btn_search)
	public void searchAgain() {
		listener.returnToSearchStart();
	}

}