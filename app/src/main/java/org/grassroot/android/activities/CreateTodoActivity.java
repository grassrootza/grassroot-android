package org.grassroot.android.activities;

import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;

import org.grassroot.android.R;
import org.grassroot.android.interfaces.GroupConstants;
import org.grassroot.android.interfaces.TaskConstants;
import org.grassroot.android.fragments.CreateTaskFragment;
import org.grassroot.android.utils.Constant;

import butterknife.BindView;
import butterknife.ButterKnife;

public class CreateTodoActivity extends PortraitActivity {

    private String groupUid;
    private boolean isGroupLocal;
    private CreateTaskFragment createTaskFragment;

    @BindView(R.id.ctodo_tlb)
    Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_todo);
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
        setTitle(R.string.ctodo_title);
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
        createTaskFragment = new CreateTaskFragment();
        Bundle args = new Bundle();
        args.putString(TaskConstants.TASK_TYPE_FIELD, TaskConstants.TODO);
        args.putString(GroupConstants.UID_FIELD, groupUid);
        args.putBoolean(GroupConstants.LOCAL_FIELD, isGroupLocal);
        createTaskFragment.setArguments(args);

        getSupportFragmentManager().beginTransaction()
                .add(R.id.ctodo_fl_fragment, createTaskFragment)
                .commit();
    }
}
