package org.grassroot.android.activities;

import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;

import com.google.android.gms.gcm.Task;

import org.grassroot.android.R;
import org.grassroot.android.fragments.CreateTaskFragment;
import org.grassroot.android.fragments.EditTaskFragment;
import org.grassroot.android.interfaces.GroupConstants;
import org.grassroot.android.interfaces.TaskConstants;
import org.grassroot.android.models.TaskModel;
import org.grassroot.android.utils.Constant;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by paballo on 2016/06/21.
 */
public class EditTaskActivity extends PortraitActivity {

    private EditTaskFragment editTaskFragment;

    @BindView(R.id.eta_toolbar)
    Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_task);
        ButterKnife.bind(this);
        Bundle b = getIntent().getExtras();
        TaskModel taskModel = b.getParcelable(TaskConstants.TASK_ENTITY_FIELD);
        setUpToolbar(taskModel);
        launchFragment(taskModel);
    }

    private void setUpToolbar(TaskModel taskModel) {
        setTitle(getTitle(taskModel));
        setSupportActionBar(toolbar);
        toolbar.setNavigationIcon(R.drawable.btn_back_wt);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (editTaskFragment != null && editTaskFragment.isVisible()) {
                    finish();
                } else {
                    getSupportFragmentManager().popBackStack();
                }
            }
        });
    }

    private void launchFragment(TaskModel taskModel) {
        editTaskFragment = EditTaskFragment.newInstance(taskModel);
        getSupportFragmentManager().beginTransaction()
                .add(R.id.eta_fragment_holder, editTaskFragment)
                .commit();
    }

    private String getTitle(TaskModel taskModel) {
        switch (taskModel.getType()) {
            case TaskConstants.MEETING:
                return getString(R.string.etsk_mtg_edit);
            case TaskConstants.VOTE:
                return getString(R.string.etsk_vote_edit);
            case TaskConstants.TODO:
                return getString(R.string.etsk_todo_edit);
            default:
                return null;
        }

    }
}

