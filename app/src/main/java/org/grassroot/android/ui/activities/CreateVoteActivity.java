package org.grassroot.android.ui.activities;

import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;

import org.grassroot.android.R;
import org.grassroot.android.interfaces.GroupConstants;
import org.grassroot.android.interfaces.TaskConstants;
import org.grassroot.android.ui.fragments.CreateTaskFragment;
import org.grassroot.android.utils.Constant;

import butterknife.BindView;
import butterknife.ButterKnife;

public class CreateVoteActivity extends PortraitActivity {

    private static final String TAG = CreateVoteActivity.class.getCanonicalName();

    private String groupUid;

    @BindView(R.id.cvote_tlb)
    Toolbar toolbar;

    private CreateTaskFragment ctskFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_vote);
        ButterKnife.bind(this);

        Bundle b = getIntent().getExtras();
        if (b == null) {
            throw new UnsupportedOperationException("Error! Activity must be called with bundle");
        }

        groupUid = b.getString(Constant.GROUPUID_FIELD);
        setUpToolbar();
        launchFragment();
    }

    private void setUpToolbar() {
        setTitle(R.string.cvote_title);
        setSupportActionBar(toolbar);
        toolbar.setNavigationIcon(R.drawable.btn_back_wt);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish(); // todo : uh, fix
            }
        });
    }

    private void launchFragment() {
        ctskFragment = new CreateTaskFragment();
        Bundle args = new Bundle();
        args.putString(TaskConstants.TASK_TYPE_FIELD, TaskConstants.VOTE);
        args.putString(GroupConstants.UID_FIELD, groupUid);
        ctskFragment.setArguments(args);

        getSupportFragmentManager().beginTransaction()
                .add(R.id.cvote_fl_fragment, ctskFragment)
                .commit();
    }
}