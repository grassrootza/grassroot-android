package org.grassroot.android.adapters;

import android.content.Context;
import android.graphics.Color;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.grassroot.android.R;
import org.grassroot.android.models.Member;
import org.grassroot.android.services.ApplicationLoader;
import org.grassroot.android.utils.RealmUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by luke on 2016/05/06.
 */
public class MemberListAdapter extends RecyclerView.Adapter<MemberListAdapter.ViewHolder> {

    private static final String TAG = MemberListAdapter.class.getCanonicalName();

    private final boolean canIncludeCurrentUser;
    private final String thisUserPhoneNumber;

    private List<Member> members;
    private boolean showSelected;
    private LayoutInflater layoutInflater;

    public MemberListAdapter(Context layoutContext, boolean canIncludeCurrentMember) {
        this.members = new ArrayList<>();
        this.layoutInflater = LayoutInflater.from(layoutContext);
        this.canIncludeCurrentUser = canIncludeCurrentMember;
        this.thisUserPhoneNumber = RealmUtils.loadPreferencesFromDB().getMobileNumber();
    }

    public void setShowSelected(boolean showSelected) {
        this.showSelected = showSelected;
    }

    @Override
    public int getItemCount() {
        return members.size();
    }

    public void addMembers(List<Member> memberList) {
        final int priorSize = members.size();
        members.addAll(memberList);
        this.notifyItemRangeChanged(priorSize, memberList.size());
    }

    public void setMembers(List<Member> memberList) {
        members = new ArrayList<>(memberList);
        notifyDataSetChanged();
    }

    public List<Member> getMembers() {
        return members;
    }

    public String getMemberUid(int position) {
        return members.get(position).getMemberUid();
    }

    public void removeMembers(final int[] positions) {
        for (int i = 0; i < positions.length; i++) {
            members.remove(positions[i]);
            notifyItemRemoved(positions[i]);
            Log.d(TAG, "removed member! at position : " + positions[i] + ", remaining members : " + members.toString());
        }
    }

    public void toggleMemberSelected(int position) {
        members.get(position).toggleSelected();
        notifyItemChanged(position);
    }

    public void updateMember(int position, Member member) {
        members.set(position, member);
        notifyItemChanged(position);
    }

    /**
     * Method to create the view holder that will be filled with content
     * @param parent The view group containing this record
     * @param viewType The type of view, in case meaningful in future
     * @return
     */
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View listItem = layoutInflater.inflate(R.layout.row_member_list, parent, false);
        ViewHolder vh = new ViewHolder(listItem);
        vh.ivSelectedIcon.setVisibility(showSelected ? View.VISIBLE : View.GONE);
        return vh;
    }

    /**
     * Method to fill out an element in the recycler view with data for the member
     * @param viewHolder The holder of the row/card being constructed
     * @param position Where in the list we are constructing
     * @return
     */
    @Override
    public void onBindViewHolder(ViewHolder viewHolder, int position) {
        final Member member = members.get(position);

        final boolean isPhoneUser;

        if (!canIncludeCurrentUser || !thisUserPhoneNumber.equals(member.getPhoneNumber())) {
            viewHolder.tvMemberName.setText(member.getDisplayName());
            viewHolder.tvMemberName.setTextColor(Color.BLACK);
            isPhoneUser = false;
        } else {
            viewHolder.tvMemberName.setTextColor(ContextCompat.getColor(ApplicationLoader.applicationContext,
                R.color.primaryColor));
            viewHolder.tvMemberName.setText(R.string.member_list_you);
            viewHolder.itemView.setClickable(false);
            isPhoneUser = true;
        }

        if (member.isNumberInvalid()) {
            viewHolder.tvMemberName.setTextColor(ContextCompat.getColor(ApplicationLoader.applicationContext,
                R.color.red));
            viewHolder.ivSelectedIcon.setImageResource(R.drawable.ic_exclamation_black);
        }

        if (showSelected) {
            if (isPhoneUser) {
                viewHolder.ivSelectedIcon.setVisibility(View.GONE);
            } else {
                viewHolder.ivSelectedIcon.setImageResource(member.isSelected() ?
                    R.drawable.btn_checked : R.drawable.btn_unchecked);
            }
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        public TextView tvMemberName;
        public ImageView ivSelectedIcon;

        public ViewHolder(View itemLayoutView) {
            super(itemLayoutView);
            tvMemberName = (TextView) itemLayoutView.findViewById(R.id.mlist_tv_member_name);
            ivSelectedIcon = (ImageView) itemLayoutView.findViewById(R.id.mlist_iv_selected);
        }
    }

}
