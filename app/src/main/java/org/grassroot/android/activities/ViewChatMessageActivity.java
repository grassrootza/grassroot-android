package org.grassroot.android.activities;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.widget.FrameLayout;

import org.grassroot.android.R;
import org.grassroot.android.events.GroupChatEvent;
import org.grassroot.android.fragments.GroupChatFragment;
import org.grassroot.android.fragments.MessageCenterFragment;
import org.grassroot.android.interfaces.GroupConstants;
import org.grassroot.android.interfaces.NotificationConstants;
import org.grassroot.android.services.GcmListenerService;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

/**
 * Created by paballo on 2016/09/06.
 */
public class ViewChatMessageActivity extends PortraitActivity {

    private static final String TAG = ViewChatMessageActivity.class.getCanonicalName();

    private Unbinder unbinder;
    private String groupUid;
    private String groupName;
    private String clickAction;
    private Fragment fragment;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_message_activtity);
        unbinder = ButterKnife.bind(this);

        groupUid = getIntent().getStringExtra(GroupConstants.UID_FIELD);
        groupName = getIntent().getStringExtra(GroupConstants.NAME_FIELD);
        clickAction = getIntent().getStringExtra(NotificationConstants.CLICK_ACTION);

     if (NotificationConstants.CHAT_MESSAGE.equals(clickAction)) {
         fragment = createGroupChatListFragment();
           // fragment = createGroupChatFragment(groupUid, groupName);
        }

        if (NotificationConstants.CHAT_LIST.equals(clickAction)) {
            fragment = createGroupChatListFragment();
        }
        getSupportFragmentManager().beginTransaction().add(R.id.gca_fragment_holder, fragment).commit();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbinder.unbind();

    }

    private void switchFragment(String  clickAction){

    }

    private Fragment createGroupChatFragment(String groupUid, String groupName) {
        GroupChatFragment groupChatFragment = GroupChatFragment.newInstance(groupUid);
        return groupChatFragment;
    }

    private Fragment createGroupChatListFragment(){
        MessageCenterFragment messageCenterFragment = MessageCenterFragment.newInstance();
        return messageCenterFragment;
    }


}
