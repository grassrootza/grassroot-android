package org.grassroot.android.activities;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.Toolbar;
import android.view.Menu;

import org.grassroot.android.R;
import org.grassroot.android.fragments.GroupChatFragment;
import org.grassroot.android.fragments.JoinRequestListFragment;
import org.grassroot.android.fragments.MultiGroupChatFragment;
import org.grassroot.android.fragments.NotificationCenterFragment;
import org.grassroot.android.interfaces.GroupConstants;
import org.grassroot.android.interfaces.NotificationConstants;
import org.grassroot.android.models.GroupJoinRequest;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

/**
 * Created by paballo on 2016/09/06.
 */
public class MultiMessageNotificationActivity extends PortraitActivity {

    private static final String TAG = MultiMessageNotificationActivity.class.getCanonicalName();
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

        switch (clickAction){
            case NotificationConstants.CHAT_MESSAGE:
                fragment = createGroupChatFragment(groupUid, groupName);
                break;
            case NotificationConstants.CHAT_LIST:
                fragment = createGroupChatListFragment();
                break;
            case NotificationConstants.NOTIFICATION_LIST:
                fragment = createNotificationCenterFragment();
                break;
            case NotificationConstants.JOIN_REQUEST_LIST:
                fragment = createJoinRequestListFragment();
                break;
            default:
                createNotificationCenterFragment();
                break;
        }
        setUpToolbar();
        getSupportFragmentManager().beginTransaction().add(R.id.gca_fragment_holder, fragment,TAG)
                .commit();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_noti_messages, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.mi_group_mute).setVisible(false);
        menu.findItem(R.id.mi_delete_messages).setVisible(false);
        return super.onPrepareOptionsMenu(menu);
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
        return GroupChatFragment.newInstance(groupUid, groupName);
    }

    private Fragment createGroupChatListFragment() {
        toolbar.setNavigationIcon(R.drawable.btn_back_wt);
        return MultiGroupChatFragment.newInstance();
    }
    private Fragment createJoinRequestListFragment(){
        this.setTitle(groupName);
        toolbar.setNavigationIcon(R.drawable.btn_close_white);
        return JoinRequestListFragment.newInstance(GroupJoinRequest.REC_REQUEST);
    }

    private Fragment createNotificationCenterFragment(){
        this.setTitle(R.string.drawer_notis);
        return new NotificationCenterFragment();
    }






}
