package org.grassroot.android.fragments;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.grassroot.android.R;
import org.grassroot.android.activities.MultiMessageNotificationActivity;
import org.grassroot.android.adapters.GroupChatAdapter;
import org.grassroot.android.adapters.GroupListAdapter;
import org.grassroot.android.adapters.RecyclerTouchListener;
import org.grassroot.android.events.GroupChatEvent;
import org.grassroot.android.interfaces.ClickListener;
import org.grassroot.android.models.Message;
import org.grassroot.android.models.responses.MessengerSetting;
import org.grassroot.android.services.GcmListenerService;
import org.grassroot.android.services.GcmUpstreamMessageService;
import org.grassroot.android.services.GroupService;
import org.grassroot.android.utils.RealmUtils;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.Date;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;

/**
 * Created by paballo on 2016/08/30.
 */
public class GroupChatFragment extends Fragment {

    private String groupUid;
    private String groupName;
    private boolean canReceive;
    private boolean canSend;

    @BindView(R.id.gc_recycler_view)
    RecyclerView gc_recycler_view;
    @BindView(R.id.text)
    TextView txt_message;
    @BindView(R.id.btn_send)
    Button bt_send;
    @BindView(R.id.chat_entry_layout)
    LinearLayout ll_chat_entry;

    private GroupChatAdapter groupChatAdapter;
    private LinearLayoutManager layoutManager;
    private static final String TAG = GroupChatFragment.class.getCanonicalName();

    public static GroupChatFragment newInstance(final String groupUid, String groupName) {
        GroupChatFragment fragment = new GroupChatFragment();
        Bundle bundle = new Bundle();
        bundle.putString("groupUid", groupUid);
        bundle.putString("groupName", groupName);
        fragment.setArguments(bundle);

        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_group_chat, container, false);
        ButterKnife.bind(this, view);

        groupUid = getArguments().getString("groupUid");
        groupName = getArguments().getString("groupName");
        setHasOptionsMenu(true);
        setView();
        loadUserSettings();

        return view;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EventBus.getDefault().register(this);

    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        if (menu.findItem(R.id.mi_group_mute) != null) {
            menu.findItem(R.id.mi_group_mute).setVisible(true);
            menu.findItem(R.id.mi_group_mute).setTitle(canReceive ? R.string.gp_mute : R.string.gp_un_mute);
        }
        if (menu.findItem(R.id.mi_delete_messages) != null)
            menu.findItem(R.id.mi_delete_messages).setVisible(true);
        if (menu.findItem(R.id.mi_refresh_screen) != null)
            menu.findItem(R.id.mi_refresh_screen).setVisible(false);
        super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (item.getItemId() == R.id.mi_group_mute) {
            mute(null, groupUid, true, item.getTitle().equals(getString(R.string.gp_mute)) ? false : true);
        }
        if (item.getItemId() == R.id.mi_delete_messages) deleteMessages(groupUid);

        return super.onOptionsItemSelected(item);
    }

    private void setView() {
        if (getActivity() instanceof MultiMessageNotificationActivity) {
            GcmListenerService.clearChatNotifications(getContext());
            getActivity().setTitle(groupName);
        }
        loadMessages();
        RealmUtils.markMessagesAsRead(groupUid);

    }

    @OnClick(R.id.btn_send)
    public void sendMessage() {

        if (!TextUtils.isEmpty(txt_message.getText())) {
            final String phoneNumber = RealmUtils.loadPreferencesFromDB().getMobileNumber();
            Message message = new Message(phoneNumber, groupUid, null, new Date(), txt_message.getText().toString(), false, "");
            Log.d(TAG, "sending message, with ID  = " + message.getId());
            RealmUtils.saveDataToRealmSync(message);
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
        groupChatAdapter = new GroupChatAdapter(messages, getActivity());
        layoutManager = new LinearLayoutManager(getActivity());
        layoutManager.setAutoMeasureEnabled(true);
        layoutManager.setStackFromEnd(true);
        if (gc_recycler_view != null) {
            gc_recycler_view.setAdapter(groupChatAdapter);
            gc_recycler_view.setLayoutManager(layoutManager);
            gc_recycler_view.setItemViewCacheSize(20);
            gc_recycler_view.setDrawingCacheEnabled(true);
            gc_recycler_view.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);

            gc_recycler_view.addOnItemTouchListener(new RecyclerTouchListener(getActivity(), gc_recycler_view, new ClickListener() {
                @Override
                public void onClick(View view, int position) {
                    if (groupChatAdapter.getItemViewType(position) == GroupChatAdapter.OTHER) {

                    }

                }

                @Override
                public void onLongClick(View view, int position) {

                }
            }));
        }

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        EventBus.getDefault().unregister(this);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(GroupChatEvent groupChatEvent) {
        if (this.isVisible() && !groupChatEvent.getGroupUid().equals(groupUid)) {
            GcmListenerService.showNotification(groupChatEvent.getBundle(), getActivity()).subscribe();
            RealmUtils.markMessagesAsRead(groupUid);
        }
        loadMessages();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

    }

    public String getGroupUid() {
        return groupUid;
    }

    private void loadUserSettings() {
        GroupService.getInstance().fetchGroupChatSetting(groupUid, AndroidSchedulers.mainThread()).subscribe(new Subscriber<MessengerSetting>() {
            @Override
            public void onNext(MessengerSetting messengerSetting) {
                canReceive = messengerSetting.isCanReceive();
                canSend = messengerSetting.isCanSend();
                ll_chat_entry.setVisibility(canSend ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onCompleted() {
            }

            @Override
            public void onError(Throwable e) {
            }
        });
    }

    /**
     * @param userUid       user whose activity status is to be changed can be null
     * @param groupUid      //the uid of the group
     * @param userInitiated // true if initiated by the user to stop receiving messages from the group
     * @param active        if set to false and user is muting  another user, the latter will continue to receive messages from the group
     *                      but will not be able to participate, however if user initiated, no messages from the group will be delivered
     */

    private void mute(@Nullable String userUid, String groupUid, boolean userInitiated, final boolean active) {
        GroupService.getInstance().updateMemberChatSetting(groupUid, userUid, userInitiated, active, AndroidSchedulers.mainThread()).subscribe(new Action1<String>() {
            @Override
            public void call(String s) {
                loadUserSettings();
                getActivity().supportInvalidateOptionsMenu();
            }
        });
    }

    private void deleteMessages(final String groupUid) {
        RealmUtils.deleteMessagesFromDb(groupUid).subscribe(new Action1<String>() {
            @Override
            public void call(String s) {
                loadMessages();
            }
        });

    }


    private void longClickOptions(Message message, int viewType) {

        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Options");

        final String[] other = {"Mute User", "Delete Message"};
        final String[] self = {"Delete Message"};
        if(viewType == GroupChatAdapter.OTHER){
            MessengerSetting messengerSetting;
          
        }




        builder.setTitle("Options");


        builder.setTitle("Options")
                .setItems(R.array.group_sort_options, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });

        builder.setCancelable(true)
                .create();

    }

}
