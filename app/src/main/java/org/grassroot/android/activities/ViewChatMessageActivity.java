package org.grassroot.android.activities;

import android.os.Bundle;
import android.widget.FrameLayout;

import org.grassroot.android.R;
import org.grassroot.android.events.GroupChatEvent;
import org.grassroot.android.fragments.GroupChatFragment;
import org.grassroot.android.interfaces.GroupConstants;
import org.grassroot.android.services.GcmListenerService;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by paballo on 2016/09/06.
 */
public class ViewChatMessageActivity extends PortraitActivity{

    private static final String TAG = ViewChatMessageActivity.class.getCanonicalName();
    private String groupUid;
    private String groupName;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_message_activtity);
        ButterKnife.bind(this);


        groupUid = getIntent().getStringExtra(GroupConstants.UID_FIELD);
        groupName = getIntent().getStringExtra(GroupConstants.NAME_FIELD);

        setView();

    }


    @Override
    protected void onDestroy() {
        super.onDestroy();

    }

    private void setView(){

        setTitle(groupName);
        GroupChatFragment groupChatFragment = GroupChatFragment.newInstance(groupUid);
        getSupportFragmentManager().beginTransaction().add(R.id.gca_fragment_holder,groupChatFragment).commit();
    }











    }
