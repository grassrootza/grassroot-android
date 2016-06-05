package org.grassroot.android.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;

import org.grassroot.android.R;
import org.grassroot.android.utils.Constant;
import org.grassroot.android.utils.MenuUtils;

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

    private String groupUid;
    private String groupName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new);
        ButterKnife.bind(this);
        Bundle b = getIntent().getExtras();
        if (b == null) {
            throw new UnsupportedOperationException("Error! Null arguments passed to modal");
        }

        Log.d(TAG, "inside newActivities, passed bundle = " + b.toString());

        this.groupUid = b.getString(Constant.GROUPUID_FIELD);
        this.groupName = b.getString(Constant.GROUPNAME_FIELD);
    }

    @Override
    public void onResume() {
        super.onResume();


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
    protected void onRestart() {
        super.onRestart();
    }


    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.push_down_in, R.anim.push_down_out);

    }

}
