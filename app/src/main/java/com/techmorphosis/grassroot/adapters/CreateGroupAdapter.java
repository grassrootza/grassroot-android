package com.techmorphosis.grassroot.adapters;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.techmorphosis.grassroot.R;
import com.techmorphosis.grassroot.models.Create_GroupModel;
import com.techmorphosis.grassroot.models.ItemTouchHelperCallback;

import java.util.List;

public class CreateGroupAdapter extends
		RecyclerView.Adapter<CreateGroupAdapter.ViewHolder>  implements ItemTouchHelperCallback.ItemTouchHelperAdapter {

	private List<Create_GroupModel> stList;

	public CreateGroupAdapter(List<Create_GroupModel> students) {
		this.stList = students;

	}

	// Create new views
	@Override
	public ViewHolder onCreateViewHolder(ViewGroup parent,
			int viewType) {
		// create a new view
		View itemLayoutView = LayoutInflater.from(parent.getContext()).inflate(
				R.layout.cardview_row, null);

		// create ViewHolder

		ViewHolder viewHolder = new ViewHolder(itemLayoutView);

		return viewHolder;
	}

	@Override
	public void onBindViewHolder(ViewHolder viewHolder, int position) {

		final int pos = position;

		if (position==0)
		{
			viewHolder.tv_cg_title.setVisibility(View.VISIBLE);
		}
		else {
			viewHolder.tv_cg_title.setVisibility(View.GONE);
		}
		viewHolder.tvName.setText(stList.get(position).getName());


		viewHolder.chkSelected.setChecked(stList.get(position).isSelected());

		viewHolder.chkSelected.setTag(stList.get(position));


		viewHolder.chkSelected.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				CheckBox cb = (CheckBox) v;
				Create_GroupModel contact = (Create_GroupModel) cb.getTag();

				contact.setSelected(cb.isChecked());
				stList.get(pos).setSelected(cb.isChecked());


			}
		});

	}

	// Return the size arraylist
	@Override
	public int getItemCount() {
		return stList.size();
	}

	@Override
	public void onItemDismiss(int position) {

	}

	public static class ViewHolder extends RecyclerView.ViewHolder {

		public TextView tvName,tv_cg_title;

		public RelativeLayout rl_row;

		public CheckBox chkSelected;

		public Create_GroupModel singlestudent;

		public ViewHolder(View itemLayoutView) {
			super(itemLayoutView);

			tvName = (TextView) itemLayoutView.findViewById(R.id.tvName);

			tv_cg_title = (TextView) itemLayoutView.findViewById(R.id.tv_cg_title);

			rl_row = (RelativeLayout) itemLayoutView.findViewById(R.id.rl_row);

			chkSelected = (CheckBox) itemLayoutView.findViewById(R.id.chkSelected);

		}

	}

	// method to access in activity after updating selection
	public List<Create_GroupModel> getStudentist() {
		return stList;
	}

}
