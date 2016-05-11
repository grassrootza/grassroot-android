package com.techmorphosis.grassroot.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import com.techmorphosis.grassroot.R;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class NewActivities extends AppCompatActivity {
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
    }



    @OnClick(R.id.iv_back)
    public void onBackClick() {
      onBackPressed();
    }

    @OnClick(R.id.bt_todo)
    public void onTodoButtonClick() {
                Intent todo=new Intent(NewActivities.this,Blank.class);
                todo.putExtra("title","ToDO");
                startActivity(todo);


    }

    @OnClick(R.id.bt_meeting)
    public void onMeetingButtonClick() {
                Intent todo=new Intent(NewActivities.this,Blank.class);
                todo.putExtra("title","Meeting");
                startActivity(todo);


    }

    @OnClick(R.id.bt_vote)
    public void onVoteButtonClick() {
                Intent todo=new Intent(NewActivities.this,Blank.class);
                todo.putExtra("title","Vote");
                startActivity(todo);
    }

    public void onBackPressed() {
        super.onBackPressed();

        overridePendingTransition(R.anim.push_down_in, R.anim.push_down_out);

    }

}
