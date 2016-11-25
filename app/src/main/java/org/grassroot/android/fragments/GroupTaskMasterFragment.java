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
import org.grassroot.android.models.Group;
import org.grassroot.android.utils.ErrorUtils;
import org.grassroot.android.utils.RealmUtils;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

import static org.grassroot.android.interfaces.GroupConstants.GROUP_OPEN_PAGE;
import static org.grassroot.android.interfaces.GroupConstants.NAME_FIELD;
import static org.grassroot.android.interfaces.GroupConstants.OPEN_ON_CHAT;
import static org.grassroot.android.interfaces.GroupConstants.OPEN_ON_TASKS;
import static org.grassroot.android.interfaces.GroupConstants.OPEN_ON_USER_PREF;
import static org.grassroot.android.interfaces.GroupConstants.UID_FIELD;

/**
 * Created by paballo on 2016/09/05.
 */
public class GroupTaskMasterFragment extends Fragment {

    private static final String TAG = GroupTaskMasterFragment.class.getSimpleName();

    private String groupUid;
    private String groupName;

    private static final int PAGERCOUNT = 2;

    Unbinder unbinder;

    @BindView(R.id.tasks_pager) ViewPager requestPager;
    @BindView(R.id.tasks_tab_layout) TabLayout tabLayout;

    public TaskPagerAdapter pagerAdapter;

    GroupChatFragment groupChatFragment;
    TaskListFragment taskListFragment;


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        groupUid = getArguments().getString(UID_FIELD);
        groupName = getArguments().getString(NAME_FIELD);
        setHasOptionsMenu(true);
    }

    public static GroupTaskMasterFragment newInstance(Group group, int openingPage) {
        GroupTaskMasterFragment groupTaskMasterFragment = new GroupTaskMasterFragment();
        Bundle args = new Bundle();
        args.putString(UID_FIELD, group.getGroupUid());
        args.putString(NAME_FIELD, group.getGroupName());
        args.putInt(GROUP_OPEN_PAGE, openingPage);
        groupTaskMasterFragment.setArguments(args);
        return groupTaskMasterFragment;
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_tasks_pager, container, false);
        unbinder = ButterKnife.bind(this, view);

        Bundle args = getArguments();
        int openingPagePref = args == null ? OPEN_ON_USER_PREF :
                args.getInt(GROUP_OPEN_PAGE, OPEN_ON_USER_PREF);
        int openingPage = openingPagePref == OPEN_ON_TASKS ? 0 :
                openingPagePref == OPEN_ON_CHAT ? 1 : (RealmUtils.openGroupOnChat(groupUid) ? 1 : 0);

        pagerAdapter = new TaskPagerAdapter(getChildFragmentManager());
        requestPager.setAdapter(pagerAdapter);
        requestPager.setCurrentItem(openingPage);
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
        RealmUtils.setOpenGroupOnChat(groupUid, isOnChatView());
        unbinder.unbind();
    }

    public boolean isOnChatView() {
      return requestPager != null && requestPager.getCurrentItem() != 0;
    }

    public void transitionToPage(int page) {
      requestPager.setCurrentItem(page, true);
    }

    public TaskListFragment getTaskListFragment() {
        return taskListFragment;
    }

    public class TaskPagerAdapter extends FragmentStatePagerAdapter {

        private final CharSequence[] titles=  getActivity().getResources().getStringArray(R.array.group_task_tab_titles);

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
                  try {
                    taskListFragment = TaskListFragment.newInstance(groupUid,
                        (TaskListFragment.TaskListListener) getActivity());
                  } catch (ClassCastException e) {
                    startActivity(ErrorUtils.gracefulExitToHome(getActivity()));
                  }
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
