package com.techmorphosis.grassroot.ui.activities;

import android.content.Intent;
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
                Intent todo=new Intent(NewActivities.this,Blank.class);
                todo.putExtra("title","Meeting");
                startActivity(todo);

            }
        };
    }

    private View.OnClickListener vote() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent todo=new Intent(NewActivities.this,Blank.class);
                todo.putExtra("title","Vote");
                startActivity(todo);

            }
        };
    }

    public void onBackPressed() {
        super.onBackPressed();

        overridePendingTransition(R.anim.push_down_in, R.anim.push_down_out);

    }

}
