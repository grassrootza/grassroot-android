package com.techmorphosis.grassroot.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import com.techmorphosis.grassroot.R;
import com.techmorphosis.grassroot.utils.Constant;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class NewActivities extends AppCompatActivity {

    private static final String TAG = NewActivities.class.getCanonicalName();

    private String groupUid;
    private String groupName;

    @BindView(R.id.iv_back)
    ImageView ivBack;
    @BindView(R.id.bt_vote)
    Button bt_vote;
    @BindView(R.id.bt_meeting)
    Button bt_meeting;
    @BindView(R.id.bt_todo)
    Button bt_todo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new);
        ButterKnife.bind(this);

        Bundle b = getIntent().getExtras();
        if (b == null) { throw new UnsupportedOperationException("Error! Null arguments passed to modal"); }

        Log.d(TAG, "inside newActivities, passed bundle = " + b.toString());

        this.groupUid = b.getString(Constant.GROUPUID_FIELD);
        this.groupName = b.getString(Constant.GROUPNAME_FIELD);
    }

    @OnClick(R.id.iv_back)
    public void onBackClick() {
      onBackPressed();
    }

    @OnClick(R.id.bt_todo)
    public void onTodoButtonClick() {
                Intent todo=new Intent(this,Blank.class);
                todo.putExtra("title","ToDO");
                startActivity(todo);
    }

    @OnClick(R.id.bt_meeting)
    public void onMeetingButtonClick() {
                Intent todo=new Intent(this,Blank.class);
                todo.putExtra("title","Meeting");
                startActivity(todo);
    }

    @OnClick(R.id.bt_vote)
    public void onVoteButtonClick() {
                Intent todo=new Intent(this,Blank.class);
                todo.putExtra("title","Vote");
                startActivity(todo);
    }

    @OnClick(R.id.bt_newmember)
    public void onNewMemberButtonClick() {
        // todo: if called from here, on finishing go back to group page (manage stack)
        Intent addMembers = new Intent(this, AddMembersActivity.class);
        addMembers.putExtra(Constant.GROUPUID_FIELD, groupUid);
        addMembers.putExtra(Constant.GROUPNAME_FIELD, groupName);
        startActivity(addMembers);
    }

    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.push_down_in, R.anim.push_down_out);

    }

}
