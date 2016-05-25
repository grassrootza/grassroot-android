package com.techmorphosis.grassroot.adapters;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.techmorphosis.grassroot.R;
import com.techmorphosis.grassroot.services.model.Member;
import com.techmorphosis.grassroot.ui.activities.VoteNotifyMembersActivity;

import java.util.ArrayList;

/**
 * Created by ravi on 11/5/16.
 */
public class VoteNotifyMembersAdapter extends RecyclerView.Adapter<VoteNotifyMembersAdapter.NotifyMemberVH> {

    private final VoteNotifyMembersActivity context;
    public   ArrayList<Member> data;
    private static final String TAG = "MembersAdapter";
    
    public VoteNotifyMembersAdapter(VoteNotifyMembersActivity activity, ArrayList<Member> List) {
        this.context = activity;
        this.data = List;
    }

    @Override
    public NotifyMemberVH onCreateViewHolder(ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_votemember, parent, false);
        NotifyMemberVH notifyMemberVH = new NotifyMemberVH(view);
        return notifyMemberVH;
    }

    @Override
    public void onBindViewHolder(NotifyMemberVH holder, int position)
    {

//        holder.llRowDialog.setBackgroundColor(context.getResources().getColor(R.color.vv_Body_agree_disagree));
        Member model= data.get(position);

        holder.tvDialogPersonName.setText(model.getDisplayName());

        if (model.isSelected())
        {
            holder.ivDialogSelected.setImageResource(R.drawable.btn_checked);
         /*   Log.e(TAG, "name " + model.displayName);
            Log.e(TAG, "click " + model.isSelected);*/
        }
        else
        {
            holder.ivDialogSelected.setImageResource(R.drawable.btn_unchecked);
            /*Log.e(TAG, "name " + model.displayName);
            Log.e(TAG, "else click " + model.isSelected);*/
        }

        if (position == data.size()) {
            holder.divider.setVisibility(View.INVISIBLE);
        }


    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    public class NotifyMemberVH extends RecyclerView.ViewHolder {

        private final LinearLayout llRowVotemember;
        private TextView tvDialogPersonName;
        private ImageView ivDialogSelected;
        private final View divider;


        public NotifyMemberVH(View view) {
            super(view);
            llRowVotemember = (LinearLayout) view.findViewById(R.id.ll_row_votemember);
            tvDialogPersonName = (TextView) view.findViewById(R.id.tv_dialog_person_name);
            ivDialogSelected = (ImageView) view.findViewById(R.id.iv_dialog_Selected);
            divider = (View) view.findViewById(R.id.divider);
        }
    }
}
