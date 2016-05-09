package com.techmorphosis.grassroot.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.techmorphosis.grassroot.R;
import com.techmorphosis.grassroot.models.Contact;
import com.techmorphosis.grassroot.models.ItemTouchHelperCallback;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MemberListAdapter extends
		RecyclerView.Adapter<MemberListAdapter.ViewHolder>  implements ItemTouchHelperCallback.ItemTouchHelperAdapter {

	private static final String TAG = MemberListAdapter.class.getSimpleName();
	private ArrayList<Contact> data;
	private LayoutInflater inflater;

	public MemberListAdapter(ArrayList<Contact> contact, Context context) {
		this.data = contact;
		inflater=LayoutInflater.from(context);
	}

	@Override
	public int getItemViewType(int position) {
		if (position==0) {
			return 0;
		} else {
			return 1;
		}
	}


	@Override
	public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

		View v=inflater.inflate(R.layout.cardview_row, parent, false);
		ViewHolder viewHolder = new ViewHolder(v);

		return viewHolder;
	}

	@Override
	public void onBindViewHolder(ViewHolder viewHolder, int position) {

		Contact model= data.get(position);
		Log.e(TAG,"model.name is " + model.name);

		viewHolder.tv_person_name.setText(model.name);

		if (model.isSelected)
		{
			viewHolder.iv_Selected.setImageResource(R.drawable.btn_checked);
		}
		else
		{
			viewHolder.iv_Selected.setImageResource(R.drawable.btn_unchecked);
		}

	}

	// Return the size arraylist
	@Override
	public int getItemCount() {
		return data.size();
	}

	@Override
	public void onItemDismiss(int position) {

	}

	public static class ViewHolder extends RecyclerView.ViewHolder {

		@BindView(R.id.tv_cg_title)
		TextView tv_person_name;
		@BindView(R.id.tv_person_name)
		ImageView iv_Selected;
		@BindView(R.id.iv_Selected_cr)
		TextView tv_cg_title;


		public ViewHolder(View itemLayoutView)
		{
			super(itemLayoutView);
			ButterKnife.bind(this, itemLayoutView);

		}

	}



}
