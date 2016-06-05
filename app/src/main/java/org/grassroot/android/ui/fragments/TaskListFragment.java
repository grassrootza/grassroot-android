package org.grassroot.android.ui.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.grassroot.android.R;
import org.grassroot.android.adapters.TasksAdapter;
import org.grassroot.android.services.GrassrootRestService;
import org.grassroot.android.services.model.TaskModel;
import org.grassroot.android.ui.views.CustomItemAnimator;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by luke on 2016/05/13.
 */
public class TaskListFragment extends Fragment {

    private static final String TAG = TaskListFragment.class.getCanonicalName();

    private String groupUid;
    private String groupName;

    private GrassrootRestService grassrootRestService;

    private List<TaskModel> listOfTasks;
    private TasksAdapter groupTasksAdapter;

    @BindView(R.id.rc_task_list)
    RecyclerView rcTaskView;

    public void setGroupUid(String groupUid) { this.groupUid = groupUid; }
    public void setGroupName(String groupName) { this.groupName = groupName; }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View viewToReturn = inflater.inflate(R.layout.fragment_task_list, container, false);
        ButterKnife.bind(this, viewToReturn);
        return viewToReturn;
    }

    private void setUpRecyclerView() {
        rcTaskView.setLayoutManager(new LinearLayoutManager(this.getContext()));
        rcTaskView.setItemAnimator(new CustomItemAnimator());
        // groupTasksAdapter = new TasksAdapter(new ArrayList<>(), this.getActivity());
    }



}