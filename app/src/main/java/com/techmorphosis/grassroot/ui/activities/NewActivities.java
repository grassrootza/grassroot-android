package com.techmorphosis.grassroot.ui.activities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;

import com.techmorphosis.grassroot.R;

public class NewActivities extends AppCompatActivity {
    private ImageView ivBack;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new);
        registeringReceiver();


        findAllViews();
    }

    private void findAllViews() {
        ivBack = (ImageView) findViewById(R.id.iv_back);
        findViewById(R.id.bt_vote).setOnClickListener(vote());
        findViewById(R.id.bt_meeting).setOnClickListener(meeting());
        findViewById(R.id.bt_todo).setOnClickListener(todo());
        ivBack.setOnClickListener(back());
    }

    private View.OnClickListener back() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();

            }
        };
    }

    private View.OnClickListener todo() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent todo=new Intent(NewActivities.this,Blank.class);
                todo.putExtra("title","ToDO");
                startActivity(todo);
            }
        };
    }

    private View.OnClickListener meeting() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent meeting=new Intent(NewActivities.this,Blank.class);
                meeting.putExtra("title","Meeting");
                startActivity(meeting);

            }
        };
    }

    private View.OnClickListener vote() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent vote=new Intent(NewActivities.this,CreateVote.class);
               // todo.putExtra("title","Vote");
                startActivity(vote);

            }
        };
    }

    public void onBackPressed() {
        super.onBackPressed();

        overridePendingTransition(R.anim.push_down_in, R.anim.push_down_out);

    }

    BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            NewActivities.this.finish();
        }
    };


    public void registeringReceiver() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(getString(R.string.bs_BR_name));
        registerReceiver(broadcastReceiver, intentFilter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(broadcastReceiver);
    }
}
