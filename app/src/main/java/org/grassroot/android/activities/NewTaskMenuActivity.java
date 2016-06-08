package org.grassroot.android.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import org.grassroot.android.R;
import org.grassroot.android.interfaces.GroupConstants;
import org.grassroot.android.models.Group;
import org.grassroot.android.utils.Constant;
import org.grassroot.android.utils.MenuUtils;

import java.util.HashSet;
import java.util.Set;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class NewTaskMenuActivity extends AppCompatActivity {

    private static final String TAG = NewTaskMenuActivity.class.getCanonicalName();

    @BindView(R.id.iv_back)
    ImageView ivBack;
    @BindView(R.id.bt_vote)
    Button bt_vote;
    @BindView(R.id.bt_meeting)
    Button bt_meeting;
    @BindView(R.id.bt_todo)
    Button bt_todo;
    @BindView(R.id.bt_newmember)
    Button bt_addmember;

    private String groupUid;
    private String groupName;

    private Group groupMembership;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new);
        ButterKnife.bind(this);

        Bundle b = getIntent().getExtras();
        if (b == null) {
            throw new UnsupportedOperationException("Error! Null arguments passed to modal");
        }

        this.groupMembership = b.getParcelable(GroupConstants.OBJECT_FIELD);

        if (groupMembership == null) {
            this.groupUid = groupMembership.getGroupUid();
            this.groupName = groupMembership.getGroupName();
            setVisibility(groupMembership);
        } else {
            this.groupUid = b.getString(Constant.GROUPUID_FIELD);
            this.groupName = b.getString(Constant.GROUPNAME_FIELD);
        }
    }

    private void setVisibility(Group groupMembership) {
        final Set<String> permissions = new HashSet<>(groupMembership.getPermissions());

        // todo : handle situation where it contains none of the permissions
        bt_meeting.setVisibility(permissions.contains(GroupConstants.PERM_CREATE_MTG) ? View.VISIBLE : View.GONE);
        bt_vote.setVisibility(permissions.contains(GroupConstants.PERM_CALL_VOTE) ? View.VISIBLE : View.GONE);
        bt_todo.setVisibility(permissions.contains(GroupConstants.PERM_CREATE_TODO) ? View.VISIBLE : View.GONE);
        bt_addmember.setVisibility(permissions.contains(GroupConstants.PERM_ADD_MEMBER) ? View.VISIBLE : View.GONE);
    }

    @OnClick(R.id.iv_back)
    public void onBackClick() {
        onBackPressed();
    }

    @OnClick(R.id.bt_todo)
    public void onTodoButtonClick() {
        Intent todo = MenuUtils.constructIntent(this, CreateTodoActivity.class, groupUid, groupName);
        startActivity(todo);
    }

    @OnClick(R.id.bt_meeting)
    public void onMeetingButtonClick() {
        Intent createMeeting = MenuUtils.constructIntent(this, CreateMeetingActivity.class, groupUid, groupName);
        startActivity(createMeeting);
    }

    @OnClick(R.id.bt_vote)
    public void onVoteButtonClick() {
        Intent createVote = MenuUtils.constructIntent(this, CreateVoteActivity.class, groupUid, groupName);
        createVote.putExtra("title", "Vote");
        startActivity(createVote);
    }

    @OnClick(R.id.bt_newmember)
    public void onNewMemberButtonClick() {
        // todo: if called from here, on finishing go back to group page (manage stack)
        Intent addMembers = new Intent(this, AddMembersActivity.class);
        addMembers.putExtra(Constant.GROUPUID_FIELD, groupUid);
        addMembers.putExtra(Constant.GROUPNAME_FIELD, groupName);
        startActivity(addMembers);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.push_down_in, R.anim.push_down_out);
    }

}
