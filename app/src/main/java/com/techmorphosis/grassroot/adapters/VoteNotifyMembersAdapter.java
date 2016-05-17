package com.techmorphosis.grassroot.adapters;

import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.techmorphosis.grassroot.R;
import com.techmorphosis.grassroot.models.Contact;
import com.techmorphosis.grassroot.ui.activities.VoteNotifyMembers;

import java.util.ArrayList;

/**
 * Created by ravi on 11/5/16.
 */
public class VoteNotifyMembersAdapter extends RecyclerView.Adapter<VoteNotifyMembersAdapter.NotifyMemberVH> {

    private final VoteNotifyMembers context;
    private   String TAG = MyRecyclerAdapter.class.getSimpleName() ;
    public   ArrayList<Contact> data;

    public VoteNotifyMembersAdapter(VoteNotifyMembers activity, ArrayList<Contact> List) {
        this.context = activity;
        this.data = List;
    }

    @Override
    public NotifyMemberVH onCreateViewHolder(ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_dialog, parent, false);
        NotifyMemberVH notifyMemberVH = new NotifyMemberVH(view);
        return notifyMemberVH;
    }

    @Override
    public void onBindViewHolder(NotifyMemberVH holder, int position)
    {

//        holder.llRowDialog.setBackgroundColor(context.getResources().getColor(R.color.vv_Body_agree_disagree));
        holder.llRowDialog.setBackgroundColor(ContextCompat.getColor(context,R.color.vv_Body_agree_disagree));
        Contact model= data.get(position);

        holder.tvDialogPersonName.setText(model.selectedNumber);

        if (model.isSelected)
        {
            holder.ivDialogSelected.setImageResource(R.drawable.btn_checked);
        }
        else
        {
            holder.ivDialogSelected.setImageResource(R.drawable.btn_unchecked);
        }



    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    public class NotifyMemberVH extends RecyclerView.ViewHolder {

        private final LinearLayout llRowDialog;
        private TextView tvDialogPersonName;
        private ImageView ivDialogSelected;

        public NotifyMemberVH(View view) {
            super(view);
            llRowDialog = (LinearLayout) view.findViewById(R.id.ll_row_dialog);
            tvDialogPersonName = (TextView) view.findViewById(R.id.tv_dialog_person_name);
            ivDialogSelected = (ImageView) view.findViewById(R.id.iv_dialog_Selected);
        }
    }
}
