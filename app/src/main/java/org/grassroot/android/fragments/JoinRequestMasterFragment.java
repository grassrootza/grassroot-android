package org.grassroot.android.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;

import org.grassroot.android.R;
import org.grassroot.android.models.GroupJoinRequest;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

/**
 * Created by luke on 2016/09/01.
 */
public class JoinRequestMasterFragment extends Fragment {

	private static final String TAG = JoinRequestMasterFragment.class.getSimpleName();

	Unbinder unbinder;

	@BindView(R.id.jreq_pager) ViewPager requestPager;
	@BindView(R.id.jreq_tab_layout) TabLayout tabLayout;

	JreqPagerAdapter pagerAdapter;

	JoinRequestListFragment fragmentSent;
	JoinRequestListFragment fragmentReceived;

	@Override
	public void onAttach(Context context) {
		super.onAttach(context);
		fragmentSent = JoinRequestListFragment.newInstance(GroupJoinRequest.SENT_REQUEST);
		fragmentReceived = JoinRequestListFragment.newInstance(GroupJoinRequest.REC_REQUEST);
		setHasOptionsMenu(true);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_join_request_pager, container, false);
		unbinder = ButterKnife.bind(this, view);

		pagerAdapter = new JreqPagerAdapter(getChildFragmentManager(), fragmentSent, fragmentReceived);
		requestPager.setAdapter(pagerAdapter);
		tabLayout.setupWithViewPager(requestPager);

		return view;
	}

	@Override
	public void onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);
		if (menu.findItem(R.id.action_search) != null)
			menu.findItem(R.id.action_search).setVisible(false);
		if (menu.findItem(R.id.mi_icon_sort) != null)
			menu.findItem(R.id.mi_icon_sort).setVisible(false);
		if (menu.findItem(R.id.mi_icon_filter) != null)
			menu.findItem(R.id.mi_icon_filter).setVisible(false);
		if (menu.findItem(R.id.mi_share_default) != null)
			menu.findItem(R.id.mi_share_default).setVisible(false);
		if (menu.findItem(R.id.mi_only_unread) != null)
			menu.findItem(R.id.mi_only_unread).setVisible(false);
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		unbinder.unbind();
	}

	public static class JreqPagerAdapter extends FragmentPagerAdapter {

		private final CharSequence[] titles=  { "Sent", "Received"};
		private final Fragment fragmentSent;
		private final Fragment fragmentReceived;

		public JreqPagerAdapter(FragmentManager manager, Fragment fragmentSent, Fragment fragmentRec) {
			super(manager);
			this.fragmentReceived = fragmentRec;
			this.fragmentSent = fragmentSent;
		}

		@Override
		public Fragment getItem(int position) {
			if (position == 0) {
				return fragmentSent;
			} else {
				return fragmentReceived;
			}
		}

		@Override
		public int getCount() {
			return titles.length;
		}

		@Override
		public CharSequence getPageTitle(int position) {
			return titles[position];
		}
	}

}
