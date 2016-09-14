package org.grassroot.android.activities;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.Toolbar;

import org.grassroot.android.R;
import org.grassroot.android.fragments.GroupChatFragment;
import org.grassroot.android.fragments.MessageCenterFragment;
import org.grassroot.android.fragments.NotificationCenterFragment;
import org.grassroot.android.interfaces.GroupConstants;
import org.grassroot.android.interfaces.NotificationConstants;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

/**
 * Created by paballo on 2016/09/06.
 */
public class ViewNotificationActivity extends PortraitActivity {

    private static final String TAG = ViewNotificationActivity.class.getCanonicalName();

    private Unbinder unbinder;
    private String groupUid;
    private String groupName;
    private String clickAction;
    private Fragment fragment;

    @BindView(R.id.vca_toolbar)
    Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification_activtity);
        unbinder = ButterKnife.bind(this);

        groupUid = getIntent().getStringExtra(GroupConstants.UID_FIELD);
        groupName = getIntent().getStringExtra(GroupConstants.NAME_FIELD);
        clickAction = getIntent().getStringExtra(NotificationConstants.CLICK_ACTION);

        if (NotificationConstants.CHAT_MESSAGE.equals(clickAction)) {
            fragment = createGroupChatFragment(groupUid, groupName);
        }
        if (NotificationConstants.CHAT_LIST.equals(clickAction)) {
            fragment = createGroupChatListFragment();
        }
        if (NotificationConstants.NOTIFICATION_LIST.equals(clickAction)) {
            fragment = createNotificationCenterFragment();
        }

        setUpToolbar();
        getSupportFragmentManager().beginTransaction().add(R.id.gca_fragment_holder, fragment)
                .commit();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbinder.unbind();

    }

    private void setUpToolbar() {
        setSupportActionBar(toolbar);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }


    private Fragment createGroupChatFragment(String groupUid, String groupName) {
        this.setTitle(groupName);
        toolbar.setNavigationIcon(R.drawable.btn_close_white);
        GroupChatFragment groupChatFragment = GroupChatFragment.newInstance(groupUid, groupName);
        return groupChatFragment;
    }

    private Fragment createGroupChatListFragment() {
        toolbar.setNavigationIcon(R.drawable.btn_back_wt);
        MessageCenterFragment messageCenterFragment = MessageCenterFragment.newInstance();
        return messageCenterFragment;
    }

    private Fragment createNotificationCenterFragment(){
        return  new NotificationCenterFragment();
    }


}
