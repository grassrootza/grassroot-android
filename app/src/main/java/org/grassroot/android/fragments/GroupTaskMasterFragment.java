package org.grassroot.android.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import org.grassroot.android.R;
import org.grassroot.android.activities.GroupTasksActivity;
import org.grassroot.android.interfaces.GroupConstants;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

/**
 * Created by paballo on 2016/09/05.
 */
public class GroupTaskMasterFragment extends Fragment implements TaskListFragment.TaskListListener{

    private static final String TAG = GroupTaskMasterFragment.class.getSimpleName();
    private String groupUid;
    private String groupName;
    private static final int PAGERCOUNT = 2;
    Unbinder unbinder;

    @BindView(R.id.tasks_pager)
    ViewPager requestPager;
    @BindView(R.id.tasks_tab_layout)
    TabLayout tabLayout;

    public TaskPagerAdapter pagerAdapter;

    GroupChatFragment groupChatFragment;
    TaskListFragment taskListFragment;
    TaskListFragment.TaskListListener taskListListener;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        groupUid = getArguments().getString(GroupConstants.UID_FIELD);
        groupName = getArguments().getString(GroupConstants.NAME_FIELD);
        setHasOptionsMenu(true);
    }

    public static GroupTaskMasterFragment newInstance(String groupUid, TaskListFragment.TaskListListener taskListListener, String groupName){
        GroupTaskMasterFragment groupTaskMasterFragment = new GroupTaskMasterFragment();
        Bundle args = new Bundle();
        args.putString(GroupConstants.UID_FIELD, groupUid);
        args.putString(GroupConstants.NAME_FIELD, groupName);
        groupTaskMasterFragment.setArguments(args);
        groupTaskMasterFragment.taskListListener = taskListListener;

        return groupTaskMasterFragment;
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_tasks_pager, container, false);
        unbinder = ButterKnife.bind(this, view);

        pagerAdapter = new TaskPagerAdapter(getChildFragmentManager());
        requestPager.setAdapter(pagerAdapter);
        tabLayout.setupWithViewPager(requestPager);

        return view;
    }

   @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        if (menu.findItem(R.id.action_search) != null)
            menu.findItem(R.id.action_search).setVisible(false);
        if (menu.findItem(R.id.mi_icon_sort) != null)
            menu.findItem(R.id.mi_icon_sort).setVisible(false);
        if (menu.findItem(R.id.mi_icon_filter) != null)
            menu.findItem(R.id.mi_icon_filter).setVisible(false);
        if (menu.findItem(R.id.mi_share_default) != null)
            menu.findItem(R.id.mi_share_default).setVisible(false);
        if (menu.findItem(R.id.mi_only_unread) != null)
            menu.findItem(R.id.mi_only_unread).setVisible(false);
        if (menu.findItem(R.id.mi_refresh_screen) != null)
            menu.findItem(R.id.mi_refresh_screen).setVisible(true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.mi_refresh_screen:
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }

    @Override
    public void onTaskLoaded(String taskName) {
        GroupTasksActivity groupTasksActivity = (GroupTasksActivity)getActivity();
        groupTasksActivity.onTaskLoaded(taskName);
    }

    @Override
    public void onTaskLoaded(int position, String taskUid, String taskType, String taskTitle) {
        GroupTasksActivity groupTasksActivity = (GroupTasksActivity)getActivity();
        groupTasksActivity.onTaskLoaded(position,taskUid,taskType,taskTitle);
    }

    @Override
    public void onFabClicked() {
        GroupTasksActivity groupTasksActivity = (GroupTasksActivity)getActivity();
        groupTasksActivity.onFabClicked();

    }

    public TaskListFragment getTaskListFragment() {
        return taskListFragment;
    }

    public class TaskPagerAdapter extends FragmentStatePagerAdapter {

        private final CharSequence[] titles=  { "Tasks", "Chat"}; // todo : externalize

        public TaskPagerAdapter(FragmentManager fragmentManager) {
            super(fragmentManager);
        }
        @Override
        public int getCount() {
            return PAGERCOUNT;
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0:
                    taskListFragment = TaskListFragment.newInstance(groupUid, GroupTaskMasterFragment.this);
                    return taskListFragment;
                case 1:
                    groupChatFragment = GroupChatFragment.newInstance(groupUid, groupName);
                    return groupChatFragment;
                default:
                    return taskListFragment;
            }

        }

        @Override
        public CharSequence getPageTitle(int position) {
            return titles[position];
        }
    }

    public ViewPager getRequestPager() {
        return requestPager;
    }
}
