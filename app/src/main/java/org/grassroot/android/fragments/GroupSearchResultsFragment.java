package org.grassroot.android.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.grassroot.android.R;
import org.grassroot.android.adapters.PublicGroupAdapter;
import org.grassroot.android.adapters.RecyclerTouchListener;
import org.grassroot.android.fragments.dialogs.SendJoinRequestFragment;
import org.grassroot.android.interfaces.ClickListener;
import org.grassroot.android.models.PublicGroupModel;
import org.grassroot.android.services.GroupSearchService;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

public class GroupSearchResultsFragment extends Fragment {

    private static final String TAG = GroupSearchResultsFragment.class.getSimpleName();

	Unbinder unbinder;
	SearchResultsListener listener;

	@BindView(R.id.fgroup_byname_header) TextView nameResultsHeader;
	@BindView(R.id.find_group_results_display) RecyclerView byNameDisplay;
	private PublicGroupAdapter byNameAdapter;
	private boolean nameResultsVisible;

	@BindView(R.id.fgroup_bysubject_header) TextView subjectResultsHeader;
	@BindView(R.id.find_group_subject_display) RecyclerView bySubjectDisplay;
	private PublicGroupAdapter bySubjectAdapter;
	private boolean subjectResultsVisible;

	@BindView(R.id.fgroup_divider) View separator;

	public interface SearchResultsListener {
		void sendJoinRequest(PublicGroupModel groupModel);
	}

	@Override
	public void onAttach(Context context) {
		super.onAttach(context);
		try {
			listener = (SearchResultsListener) context;
		} catch (ClassCastException e) {
			Log.e(TAG, "activity does not implement listener ... disabling join request sending");
		}

		if (GroupSearchService.getInstance().foundByGroupName != null) {
			byNameAdapter = new PublicGroupAdapter(context, GroupSearchService.getInstance().foundByGroupName);
		} else {
			byNameAdapter = new PublicGroupAdapter(context, new ArrayList<PublicGroupModel>());
		}

		if (GroupSearchService.getInstance().foundByTaskName != null) {
			bySubjectAdapter = new PublicGroupAdapter(context, GroupSearchService.getInstance().foundByTaskName);
		} else {
			bySubjectAdapter = new PublicGroupAdapter(context, new ArrayList<PublicGroupModel>());
		}

	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View viewToReturn = inflater.inflate(R.layout.fragment_group_search_results, container, false);
		unbinder = ButterKnife.bind(this, viewToReturn);
		setUpNameResultsDisplay();
		setUpSubjectResultsDisplay();
		setUpTextsAndSeparator();
		return viewToReturn;
	}

	private void setUpNameResultsDisplay() {
		byNameDisplay.setAdapter(byNameAdapter);
		byNameDisplay.setLayoutManager(new LinearLayoutManager(getContext()));
		byNameDisplay.setItemViewCacheSize(10);
		byNameDisplay.setDrawingCacheEnabled(true);
		byNameDisplay.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_AUTO);

		if (byNameAdapter.getItemCount() > 0) {
			nameResultsHeader.setVisibility(View.VISIBLE);
			byNameDisplay.setVisibility(View.VISIBLE);
			setRecyclerViewTouch(byNameDisplay, byNameAdapter);
			nameResultsVisible = true;
		} else {
			nameResultsHeader.setVisibility(View.GONE);
			byNameDisplay.setVisibility(View.GONE);
			separator.setVisibility(View.GONE);
			nameResultsVisible = false;
		}
	}

	private void setUpSubjectResultsDisplay() {
		bySubjectDisplay.setAdapter(bySubjectAdapter);
		bySubjectDisplay.setLayoutManager(new LinearLayoutManager(getContext()));
		bySubjectDisplay.setItemViewCacheSize(10);
		bySubjectDisplay.setDrawingCacheEnabled(true);
		bySubjectDisplay.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_AUTO);

		if (bySubjectAdapter.getItemCount() > 0) {
			subjectResultsHeader.setVisibility(View.VISIBLE);
			bySubjectDisplay.setVisibility(View.VISIBLE);
			setRecyclerViewTouch(bySubjectDisplay, bySubjectAdapter);
			subjectResultsVisible = true;
		} else {
			subjectResultsHeader.setVisibility(View.GONE);
			bySubjectDisplay.setVisibility(View.GONE);
			separator.setVisibility(View.GONE);
			subjectResultsVisible = false;
		}
	}

	private void setUpTextsAndSeparator() {
		separator.setVisibility(nameResultsVisible && subjectResultsVisible ? View.VISIBLE : View.GONE);
		subjectResultsHeader.setText(nameResultsVisible ? R.string.find_group_subject_header_short
			: R.string.find_group_subject_header_long);
	}

	private void setRecyclerViewTouch(RecyclerView recyclerView, final PublicGroupAdapter adapter) {
		if (listener != null) {
			recyclerView.addOnItemTouchListener(new RecyclerTouchListener(getContext(), recyclerView, new ClickListener() {
				@Override
				public void onClick(View view, int position) {
					sendJoinRequest(adapter.getPublicGroup(position));
				}

				@Override
				public void onLongClick(View view, int position) { }
			}));
		}
	}

	private void sendJoinRequest(PublicGroupModel group) {
		SendJoinRequestFragment.newInstance(group, new SendJoinRequestFragment.SendJoinRequestListener() {
			@Override
			public void requestConfirmed(PublicGroupModel groupModel) {
				listener.sendJoinRequest(groupModel);
			}
		}).show(getFragmentManager(), "dialog");
	}


}