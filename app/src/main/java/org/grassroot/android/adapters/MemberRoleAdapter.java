package org.grassroot.android.adapters;

import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.grassroot.android.R;
import org.grassroot.android.interfaces.GroupConstants;
import org.grassroot.android.models.Member;
import org.grassroot.android.utils.RealmUtils;

import java.util.HashMap;
import java.util.Map;

import io.realm.RealmList;

/**
 * Created by luke on 2016/07/19.
 */
public class MemberRoleAdapter extends RecyclerView.Adapter<MemberRoleAdapter.MemberRoleViewHolder> {

    private static final String TAG = MemberRoleAdapter.class.getSimpleName();

    private RealmList<Member> members;
    private final Map<String, Integer> roleMap;
    private MemberRoleClickListener listener;
    private final String userMobile; // for disallowing click on self

    public interface MemberRoleClickListener {
        void onGroupMemberClicked(final String memberUid, final String memberName);
    }

    public MemberRoleAdapter(String groupUid, MemberRoleClickListener listener) {
        members = RealmUtils.loadListFromDB(Member.class, "groupUid", groupUid);
        userMobile = RealmUtils.loadPreferencesFromDB().getMobileNumber();
        roleMap = new HashMap<>();
        roleMap.put(GroupConstants.ROLE_GROUP_ORGANIZER, R.string.gset_role_organizer);
        roleMap.put(GroupConstants.ROLE_COMMITTEE_MEMBER, R.string.gset_role_committee);
        roleMap.put(GroupConstants.ROLE_ORDINARY_MEMBER, R.string.gset_role_ordinary);
        this.listener = listener;
    }

    @Override
    public MemberRoleViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View item = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_member_role, parent, false);
        MemberRoleViewHolder holder = new MemberRoleViewHolder(item);
        return holder;
    }

    @Override
    public void onBindViewHolder(MemberRoleViewHolder holder, int position) {
        final Member member = members.get(position);
        holder.userName.setText(member.getDisplayName());
        holder.userRole.setText(roleMap.get(member.getRoleName()));
        if (!userMobile.equals(member.getPhoneNumber())) {
            holder.rootView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.e(TAG, "member clicked!");
                    listener.onGroupMemberClicked(member.getMemberUid(), member.getDisplayName());
                }
            });
        } else {
            holder.rootView.setClickable(false);
        }
    }

    @Override
    public int getItemCount() {
        return members.size();
    }

    public static class MemberRoleViewHolder extends RecyclerView.ViewHolder {

        View rootView;
        TextView userName;
        TextView userRole;

        public MemberRoleViewHolder(View itemLayoutView) {
            super(itemLayoutView);
            rootView = itemLayoutView;
            userName = (TextView) itemLayoutView.findViewById(R.id.rlist_member_name);
            userRole = (TextView) itemLayoutView.findViewById(R.id.rlist_role_name);
        }

    }



}
