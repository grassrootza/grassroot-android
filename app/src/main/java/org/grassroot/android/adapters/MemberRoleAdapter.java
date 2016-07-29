package org.grassroot.android.adapters;

import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import io.realm.RealmObject;
import io.realm.RealmResults;
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
    private RealmList<Member> members=new RealmList<>();
    private Map<String, Integer> mapUidPosition;
    private final Map<String, Integer> roleMap;

    private MemberRoleClickListener listener;
    private final String userMobile; // for disallowing click on self

    public interface MemberRoleClickListener {
        void onGroupMemberClicked(final String memberUid, final String memberName);
    }

    public MemberRoleAdapter(String groupUid, MemberRoleClickListener listener) {
        this.groupUid  = groupUid;
         RealmUtils.loadListFromDB(Member.class, "groupUid", groupUid).subscribe(new Action1<List<Member>>() {
            @Override public void call(List<Member> realmResults) {
                for(Member m : realmResults){
                    members.add(m);
                };
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

    @Override
    public MemberRoleViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View item = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_member_role, parent, false);
        return new MemberRoleViewHolder(item);
    }

    @Override
    public void onBindViewHolder(MemberRoleViewHolder holder, int position) {
        final Member member = members.get(position);
        mapUidPosition.put(member.getMemberUid(), position);
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

    public void refreshDisplayedMember(final String memberUid) {
        Log.e(TAG, "refreshing group member");
        int position = mapUidPosition.get(memberUid);
        Member updatedMember = RealmUtils.loadObjectFromDB(Member.class, Member.PKEY, memberUid + groupUid);
        members.set(position, updatedMember);
        Log.e(TAG, "current role of member: " + members.get(position).getRoleName());
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
