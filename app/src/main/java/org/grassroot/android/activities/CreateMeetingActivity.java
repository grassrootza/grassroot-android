package org.grassroot.android.activities;

import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;

import org.grassroot.android.R;
import org.grassroot.android.fragments.CreateTaskFragment;
import org.grassroot.android.interfaces.GroupConstants;
import org.grassroot.android.interfaces.TaskConstants;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by luke on 2016/05/24.
 */
public class CreateMeetingActivity extends PortraitActivity {

    private static final String TAG = CreateMeetingActivity.class.getCanonicalName();

    private boolean isGroupLocal;
    private String groupUid;

    @BindView(R.id.cmtg_tlb)
    Toolbar toolbar;

    private CreateTaskFragment ctskFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_meeting);
        ButterKnife.bind(this);

        Bundle b = getIntent().getExtras();
        if (b == null) {
            throw new UnsupportedOperationException("Error! Activity must be called with bundle");
        }

        groupUid = b.getString(GroupConstants.UID_FIELD);
        isGroupLocal = b.getBoolean(GroupConstants.LOCAL_FIELD);

        setUpToolbar();
        launchFragment();
    }

    private void setUpToolbar() {
        setSupportActionBar(toolbar);
        toolbar.setNavigationIcon(R.drawable.btn_back_wt);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }

    private void launchFragment() {
        ctskFragment = new CreateTaskFragment();
        Bundle args = new Bundle();
        args.putString(TaskConstants.TASK_TYPE_FIELD, TaskConstants.MEETING);
        args.putString(GroupConstants.UID_FIELD, groupUid);
        args.putBoolean(GroupConstants.LOCAL_FIELD, isGroupLocal);
        ctskFragment.setArguments(args);

        getSupportFragmentManager().beginTransaction()
                .add(R.id.cmtg_fl_fragment, ctskFragment)
                .commit();
    }

}