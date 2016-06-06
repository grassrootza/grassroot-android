package org.grassroot.android.ContactLib;

import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.BaseAdapter;
import android.widget.SectionIndexer;
import android.widget.TextView;


public abstract class BaseSectionedListViewAdapter extends BaseAdapter implements SectionIndexer
  {
	private SectionIndexer _sectionIndexer;
	private boolean mHeaderViewVisible = true;

	public void setSectionIndexer(final SectionIndexer sectionIndexer) {
		_sectionIndexer = sectionIndexer;
	}

	/** remember to call bindSectionHeader(v,position); before calling return */
	@Override
	public abstract View getView(final int position, final View convertView, final ViewGroup parent);

	public abstract CharSequence getSectionTitle(int sectionIndex);

	protected void bindSectionHeader(final TextView headerView, final View dividerView, final int position) {

        final int sectionIndex = getSectionForPosition(position);
		if (getPositionForSection(sectionIndex) == position) {
			final CharSequence title = getSectionTitle(sectionIndex);
			headerView.setText(title);
			headerView.setVisibility(View.VISIBLE);
			if (dividerView != null)
				dividerView.setVisibility(View.GONE);
		} else {
			headerView.setVisibility(View.GONE);
			if (dividerView != null)
				dividerView.setVisibility(View.VISIBLE);
		}
		// move the divider for the last item in a section
		if (dividerView != null)
			if (getPositionForSection(sectionIndex + 1) - 1 == position)
				dividerView.setVisibility(View.GONE);
			else
				dividerView.setVisibility(View.VISIBLE);
		if (!mHeaderViewVisible)
			headerView.setVisibility(View.GONE);
	}

	@Override
	public int getPositionForSection(final int sectionIndex) {
		if (_sectionIndexer == null)
			return -1;
		return _sectionIndexer.getPositionForSection(sectionIndex);
	}

	@Override
	public int getSectionForPosition(final int position) {
		if (_sectionIndexer == null)
			return -1;
		return _sectionIndexer.getSectionForPosition(position);
	}

	@Override
	public Object[] getSections() {
		if (_sectionIndexer == null)
			return new String[] { " " };
		return _sectionIndexer.getSections();
	}

	@Override
	public long getItemId(final int position) {
		return position;
	}
}
