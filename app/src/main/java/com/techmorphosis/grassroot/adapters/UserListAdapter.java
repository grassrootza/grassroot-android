package com.techmorphosis.grassroot.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.techmorphosis.grassroot.R;
import com.techmorphosis.grassroot.services.model.Member;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by luke on 2016/05/06.
 */
public class UserListAdapter extends RecyclerView.Adapter<UserListAdapter.ViewHolder> {

    private static final String TAG = UserListAdapter.class.getCanonicalName();
    private List<Member> members;
    private LayoutInflater layoutInflater;

    /**
     * Internal class that constructs the shell of the view for an element in the data list
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {

        public TextView tvMemberName;

        public ViewHolder(View itemLayoutView) {
            super(itemLayoutView);
            // Log.e(TAG, "userListAdaptor! inside internal viewholder class!");
            tvMemberName = (TextView) itemLayoutView.findViewById(R.id.mlist_tv_member_name);
        }
    }

    public UserListAdapter(Context context) {
        this.members = new ArrayList<>();
        this.layoutInflater = LayoutInflater.from(context);
    }

    @Override
    public int getItemCount() { return members.size(); }

    public void addMembers(List<Member> memberList) {
        Log.d(TAG, members.size() + " members so far, add these to adaptor: " + memberList.toString());
        members.addAll(memberList);
        this.notifyItemRangeInserted(0, memberList.size() - 1);
    }

    public List<Member> getMembers() { return members; }

    /**
     * Method to create the view holder that will be filled with content
     * @param parent The view group containing this record
     * @param viewType The type of view, in case meaningful in future
     * @return
     */
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // Log.e(TAG, "userListAdaptor! creating view holder!");
        View listItem = layoutInflater.inflate(R.layout.member_list_item, parent, false); // todo : switch to getting inflater in here?
        ViewHolder vh = new ViewHolder(listItem);
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
        // Log.e(TAG, "userListAdaptor! binding view holder!");
    }

}
