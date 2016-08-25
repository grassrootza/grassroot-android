package org.grassroot.android.adapters;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.grassroot.android.R;
import org.grassroot.android.interfaces.GroupConstants;
import org.grassroot.android.models.Member;
import org.grassroot.android.utils.RealmUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.realm.RealmList;
import rx.functions.Action1;

/**
 * Created by luke on 2016/07/19.
 */
public class MemberRoleAdapter extends RecyclerView.Adapter<MemberRoleAdapter.MemberRoleViewHolder> {

    private static final String TAG = MemberRoleAdapter.class.getSimpleName();

    final private String groupUid;
    private RealmList<Member> members = new RealmList<>();

    private Map<String, Integer> mapUidPosition;
    private final Map<String, Integer> roleMap;

    private MemberRoleClickListener listener;
    private final String userMobile; // for disallowing click on self

    public interface MemberRoleClickListener {
        void onGroupMemberClicked(final String memberUid, final String memberName);
    }

    public MemberRoleAdapter(String groupUid, MemberRoleClickListener listener) {
        this.groupUid  = groupUid;

        RealmUtils.loadGroupMembers(groupUid, true).subscribe(new Action1<List<Member>>() {
            @Override public void call(List<Member> realmResults) {
                members.addAll(realmResults);
            }
        });

        userMobile = RealmUtils.loadPreferencesFromDB().getMobileNumber();
        roleMap = new HashMap<>();
        roleMap.put(GroupConstants.ROLE_GROUP_ORGANIZER, R.string.gset_role_organizer);
        roleMap.put(GroupConstants.ROLE_COMMITTEE_MEMBER, R.string.gset_role_committee);
        roleMap.put(GroupConstants.ROLE_ORDINARY_MEMBER, R.string.gset_role_ordinary);
        this.listener = listener;
        mapUidPosition = new HashMap<>();
    }

    public void refreshToDB() {
        members.clear();
        RealmUtils.loadGroupMembers(groupUid, true).subscribe(new Action1<List<Member>>() {
                @Override
                public void call(List<Member> realmResults) {
                    members.addAll(realmResults);
                    notifyDataSetChanged();
                }
            });
    }

    @Override
    public MemberRoleViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View item = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_member_role, parent, false);
        return new MemberRoleViewHolder(item);
    }

    @Override
    public void onBindViewHolder(MemberRoleViewHolder holder, int position) {
        final Member member = members.get(position);
        mapUidPosition.put(member.getMemberUid(), position);
        holder.userRole.setText(roleMap.get(member.getRoleName()));
        if (!userMobile.equals(member.getPhoneNumber())) {
            holder.userName.setText(member.getDisplayName());
            holder.rootView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    listener.onGroupMemberClicked(member.getMemberUid(), member.getDisplayName());
                }
            });
        } else {
            holder.userName.setText(R.string.member_list_you);
            holder.rootView.setClickable(false);
        }
    }

    @Override
    public int getItemCount() {
        return members.size();
    }

    public void refreshDisplayedMember(final String memberUid) {
        int position = mapUidPosition.get(memberUid);
        Member updatedMember = RealmUtils.loadObjectFromDB(Member.class, Member.PKEY, memberUid + groupUid);
        members.set(position, updatedMember);
        notifyItemChanged(position);
    }

    public void removeDisplayedMember(final String memberUid) {
        int position = mapUidPosition.get(memberUid);
        // do a double check in case reference elsewhere has already removed...
        if (members.get(position).getMemberUid().equals(memberUid)) {
            members.remove(position);
        }
        notifyItemRemoved(position);
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
