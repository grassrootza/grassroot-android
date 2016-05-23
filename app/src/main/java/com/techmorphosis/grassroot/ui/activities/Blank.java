package com.techmorphosis.grassroot.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.techmorphosis.grassroot.R;

public class Blank extends PortraitActivity {

    private LinearLayout llToolbar;
    private ImageView ivBack;
    private TextView txtToolbarTitle;
    private TextView txtTitle;
    private static final String TAG = "Blank";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_blank);

        finAllViews();
        if (getIntent() != null)
        {
            txtTitle.setText(getIntent().getExtras().getString("title"));
            if (txtTitle.getText().toString().equalsIgnoreCase("Vote")) {
                Intent i = new Intent(Blank.this,CreateVote.class);
                startActivity(i);
            }
            txtToolbarTitle.setText(getIntent().getExtras().getString("title"));
        }
        else
        {
            Log.e(TAG, "null");
        }

    }

    private void finAllViews() {
        llToolbar = (LinearLayout) findViewById(R.id.ll_toolbar);
        ivBack = (ImageView) findViewById(R.id.iv_back);
        txtToolbarTitle = (TextView) findViewById(R.id.txt_toolbar_title);
        txtTitle = (TextView) findViewById(R.id.txt_title);
        ivBack.setOnClickListener(ivBack());
    }

    private View.OnClickListener ivBack() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                finish();
            }
        };
    }


}
