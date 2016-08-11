package org.grassroot.android.adapters;

import android.content.Context;
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

import java.util.ArrayList;
import java.util.List;

/**
 * Created by luke on 2016/05/06.
 */
public class MemberListAdapter extends RecyclerView.Adapter<MemberListAdapter.ViewHolder> {

    private static final String TAG = MemberListAdapter.class.getCanonicalName();

    private List<Member> members;
    private boolean showSelected;
    private LayoutInflater layoutInflater;

    /**
     * Internal class that constructs the shell of the view for an element in the data list
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {

        public TextView tvMemberName;
        public ImageView ivSelectedIcon;

        public ViewHolder(View itemLayoutView) {
            super(itemLayoutView);
            tvMemberName = (TextView) itemLayoutView.findViewById(R.id.mlist_tv_member_name);
            ivSelectedIcon = (ImageView) itemLayoutView.findViewById(R.id.mlist_iv_selected);
        }
    }

    public MemberListAdapter(Context context) {
        this.members = new ArrayList<>();
        this.layoutInflater = LayoutInflater.from(context);
    }

    public void setShowSelected(boolean showSelected) {
        this.showSelected = showSelected;
    }

    @Override
    public int getItemCount() {
        return members.size();
    }

    public void addMembers(List<Member> memberList) {
        members.addAll(memberList);
        this.notifyDataSetChanged(); // todo : as everywhere, optimize this, later
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
            Log.e(TAG, "removed member! at position : " + positions[i] + ", remaining members : " + members.toString());
        }
    }

    public void resetMembers(List<Member> memberList) {
        members = new ArrayList<>(memberList);
        this.notifyDataSetChanged();
    }

    public void toggleMemberSelected(int position) {
        members.get(position).toggleSelected();
        notifyDataSetChanged();
    }

    public void updateMember(int position, Member member) {
        members.set(position, member);
        notifyDataSetChanged();
    }

    /**
     * Method to create the view holder that will be filled with content
     * @param parent The view group containing this record
     * @param viewType The type of view, in case meaningful in future
     * @return
     */
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View listItem = layoutInflater.inflate(R.layout.member_list_item, parent, false); // todo : switch to getting inflater in here?
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
        Member thisMember = members.get(position);

        viewHolder.tvMemberName.setText(thisMember.getDisplayName());

        if (thisMember.isNumberInvalid()) {
            viewHolder.tvMemberName.setTextColor(ContextCompat.getColor(ApplicationLoader.applicationContext,
                R.color.red));
            viewHolder.ivSelectedIcon.setImageResource(R.drawable.ic_exclamation_black);
        }

        if (showSelected) {
            viewHolder.ivSelectedIcon.setImageResource(thisMember.isSelected() ?
                    R.drawable.btn_checked : R.drawable.btn_unchecked);
        }
    }

}
