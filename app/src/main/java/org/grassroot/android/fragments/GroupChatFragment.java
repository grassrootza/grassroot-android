package org.grassroot.android.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.TextView;


import org.grassroot.android.R;
import org.grassroot.android.adapters.GroupChatAdapter;
import org.grassroot.android.events.GroupChatEvent;
import org.grassroot.android.models.Message;
import org.grassroot.android.services.GcmListenerService;
import org.grassroot.android.services.GcmUpstreamMessageService;
import org.grassroot.android.utils.RealmUtils;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.Date;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;

/**
 * Created by paballo on 2016/08/30.
 */
public class GroupChatFragment extends Fragment {

    private String groupUid;

    @BindView(R.id.gc_recycler_view)
    RecyclerView gc_recycler_view;
    @BindView(R.id.text)
    TextView txt_message;
    @BindView(R.id.btn_send)
    Button bt_send;

    private GroupChatAdapter groupChatAdapter;
    private LinearLayoutManager layoutManager;
    private static final String TAG = GroupChatFragment.class.getCanonicalName();

    public static GroupChatFragment newInstance(final String parentUid) {
        GroupChatFragment fragment = new GroupChatFragment();
        fragment.groupUid = parentUid;
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_group_chat, container, false);
        ButterKnife.bind(this, view);
        setview();

        return view;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EventBus.getDefault().register(this);
    }

    private void setview() {
        loadMessages();

    }

    @OnClick(R.id.btn_send)
    public void sendMessage() {

        if(!TextUtils.isEmpty(txt_message.getText())) {
            final String phoneNumber = RealmUtils.loadPreferencesFromDB().getMobileNumber();
            Message message = new Message(phoneNumber, groupUid, null, new Date(), txt_message.getText().toString(), false, "");
            Log.d(TAG, "sending message, with ID  = " + message.getId());
            RealmUtils.saveDataToRealmSync(message);
            Log.e(TAG, "number of messages in DB : " + RealmUtils.countObjectsInDB(Message.class));
            loadMessages();
            GcmUpstreamMessageService.sendMessage(message, getActivity(),
                    AndroidSchedulers.mainThread())
                    .subscribe(new Action1<String>() {
                        @Override
                        public void call(String s) {
                            groupChatAdapter.reloadFromdb(groupUid);
                        }
                    });

            txt_message.setText(""); //clear text
            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(getActivity().getCurrentFocus().getWindowToken(), 0);

        }
    }

    public void loadMessages() {
        RealmUtils.loadMessagesFromDb(groupUid).subscribe(new Action1<List<Message>>() {
            @Override
            public void call(List<Message> msgs) {
                if (groupChatAdapter == null) {
                    setUpListAndAdapter(msgs);
                } else {
                    groupChatAdapter.reloadFromdb(groupUid);
                }
                gc_recycler_view.smoothScrollToPosition(groupChatAdapter.getItemCount());

            }
        });
    }


    private void setUpListAndAdapter(List<Message> messages) {
        groupChatAdapter = new GroupChatAdapter(messages);
        layoutManager = new LinearLayoutManager(getActivity());
        layoutManager.setAutoMeasureEnabled(true);
        layoutManager.setStackFromEnd(true);
        if (gc_recycler_view != null) {
            gc_recycler_view.setAdapter(groupChatAdapter);
            gc_recycler_view.setLayoutManager(layoutManager);
            gc_recycler_view.setItemViewCacheSize(20);
            gc_recycler_view.setDrawingCacheEnabled(true);
            gc_recycler_view.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);

        }

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        EventBus.getDefault().unregister(this);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(GroupChatEvent groupChatEvent) {
        loadMessages();
        if(this.isVisible() && !groupChatEvent.getGroupUid().equals(groupUid)) {
            GcmListenerService.showNotification(groupChatEvent.getBundle(), getActivity()).subscribe();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    public String getGroupUid() {
        return groupUid;
    }

}
