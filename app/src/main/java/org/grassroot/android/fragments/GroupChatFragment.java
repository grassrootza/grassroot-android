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
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.ImageView;

import org.grassroot.android.R;
import org.grassroot.android.activities.MultiMessageNotificationActivity;
import org.grassroot.android.adapters.CommandsAdapter;
import org.grassroot.android.adapters.GroupChatAdapter;
import org.grassroot.android.adapters.RecyclerTouchListener;
import org.grassroot.android.events.GroupChatEvent;
import org.grassroot.android.events.MessageNotSentEvent;
import org.grassroot.android.interfaces.ClickListener;
import org.grassroot.android.interfaces.GroupConstants;
import org.grassroot.android.models.Command;
import org.grassroot.android.models.Message;
import org.grassroot.android.models.responses.MessengerSetting;
import org.grassroot.android.services.GcmListenerService;
import org.grassroot.android.services.GcmUpstreamMessageService;
import org.grassroot.android.services.GroupService;
import org.grassroot.android.utils.EmojIconMultiAutoCompleteActions;
import org.grassroot.android.utils.RealmUtils;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import hani.momanii.supernova_emoji_library.Actions.EmojIconActions;
import hani.momanii.supernova_emoji_library.Helper.EmojiconMultiAutoCompleteTextView;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;

/**
 * Created by paballo on 2016/08/30.
 */
public class GroupChatFragment extends Fragment {

    private static final String TAG = GroupChatFragment.class.getCanonicalName();

    private String groupUid;
    private String groupName;
    private boolean canReceive;
    private boolean canSend;

    private boolean isMuted; //other chat participant
    private Unbinder unbinder;

    @BindView(R.id.root_view)
    ViewGroup rootView;

    @BindView(R.id.emoji_btn)
    ImageView bt_emoji;
    @BindView(R.id.gc_recycler_view)
    RecyclerView gc_recycler_view;
    @BindView(R.id.text)
    EmojiconMultiAutoCompleteTextView txt_message;
    @BindView(R.id.btn_send)
    ImageView bt_send;

    private GroupChatAdapter groupChatAdapter;
    private ArrayAdapter<Command> arrayAdapter;
    private List<Command> commands;
    private LinearLayoutManager layoutManager;
    private EmojIconMultiAutoCompleteActions emojIconAction;


    public static GroupChatFragment newInstance(final String groupUid, String groupName) {
        GroupChatFragment fragment = new GroupChatFragment();
        Bundle bundle = new Bundle();
        bundle.putString(GroupConstants.UID_FIELD, groupUid);
        bundle.putString(GroupConstants.NAME_FIELD, groupName);
        fragment.setArguments(bundle);

        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_group_chat, container, false);
        unbinder = ButterKnife.bind(this, view);

        groupUid = getArguments().getString(GroupConstants.UID_FIELD);
        groupName = getArguments().getString(GroupConstants.NAME_FIELD);


        String[] commandArray = getActivity().getResources().getStringArray(R.array.commands);
        String[] hintArray = getActivity().getResources().getStringArray(R.array.command_hints);
        String[] descriptionArrays = getActivity().getResources().getStringArray(R.array.command_descriptions);

        if (commands == null) {
            commands = new ArrayList<>();
            for (int i = 0; i < commandArray.length; i++) {
                Command command = new Command(commandArray[i], hintArray[i], descriptionArrays[i]);
                commands.add(command);
            }
        }
        setHasOptionsMenu(true);
        arrayAdapter = new CommandsAdapter(getActivity(), commands);
        setView();
        loadGroupSettings();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

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

        if (item.getItemId() == R.id.mi_group_mute)
            mute(null, groupUid, true, item.getTitle().equals(getString(R.string.gp_mute)) ? false : true);
        if (item.getItemId() == R.id.mi_delete_messages) deleteAllMessages(groupUid);

        return super.onOptionsItemSelected(item);
    }

    private void setView() {
        if (getActivity() instanceof MultiMessageNotificationActivity) {
            GcmListenerService.clearChatNotifications(getContext());
            getActivity().setTitle(groupName);
        }
        loadMessages();
        txt_message.setAdapter(arrayAdapter);
        txt_message.setThreshold(1); //setting it in xml does not seem to be working
        txt_message.requestFocus();

        emojIconAction = new EmojIconMultiAutoCompleteActions(getActivity(), rootView, txt_message, bt_emoji);

        emojIconAction.ShowEmojIcon();
        emojIconAction.setKeyboardListener(new EmojIconActions.KeyboardListener() {
            @Override
            public void onKeyboardOpen() {
            }

            @Override
            public void onKeyboardClose() {
            }
        });


        RealmUtils.markMessagesAsRead(groupUid);

    }

    @OnClick(R.id.btn_send)
    public void sendMessage() {

        if (!TextUtils.isEmpty(txt_message.getText())) {
            final String phoneNumber = RealmUtils.loadPreferencesFromDB().getMobileNumber();
            Message message = new Message(phoneNumber, groupUid, null, new Date(), txt_message.getText().toString(), false, "");
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
            txt_message.requestFocus();
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
                }

                @Override
                public void onLongClick(View view, int position) {
                    int viewType = groupChatAdapter.getItemViewType(position);
                    longClickOptions(groupChatAdapter.getMessages().get(position), viewType);
                }
            }));
        }

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
        EventBus.getDefault().unregister(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(GroupChatEvent groupChatEvent) {
        if (this.isVisible() && !groupChatEvent.getGroupUid().equals(groupUid)) {
            GcmListenerService.showNotification(groupChatEvent.getBundle(), getActivity()).subscribe();
            RealmUtils.markMessagesAsRead(groupUid);
        }
        loadMessages();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(MessageNotSentEvent messageNotSentEvent) {
        loadMessages();
    }


    public String getGroupUid() {
        return groupUid;
    }

    private void loadGroupSettings() {
        GroupService.getInstance().fetchGroupChatSetting(groupUid, AndroidSchedulers.mainThread(), null).subscribe(new Subscriber<MessengerSetting>() {
            @Override
            public void onNext(MessengerSetting messengerSetting) {
                canReceive = messengerSetting.isCanReceive();
                canSend = messengerSetting.isCanSend();
                txt_message.setEnabled(canSend);
                bt_send.setEnabled(canSend);
                bt_emoji.setEnabled(canSend);

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
     * @param userUid       user whose activity status is to be changed. null if own
     * @param groupUid      uid of the group
     * @param userInitiated true if initiated by self to stop receiving messages from the group
     * @param active        if set to false and user is muting  another user, the latter will continue to receive messages from the group
     *                      but will not be able to send messages, however if self initiated, user will stop receiving messages from group
     */

    private void mute(@Nullable final String userUid, String groupUid, boolean userInitiated, final boolean active) {
        GroupService.getInstance().updateMemberChatSetting(groupUid, userUid, userInitiated, active, AndroidSchedulers.mainThread()).subscribe(new Action1<String>() {
            @Override
            public void call(String s) {
                if (userUid == null) {
                    loadGroupSettings();
                    getActivity().supportInvalidateOptionsMenu();
                }
            }
        });
    }


    private void longClickOptions(final Message message, int viewType) {


        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(getString(R.string.chat_long_click_options_title));
        if (viewType == GroupChatAdapter.SELF) {
            builder.setItems(R.array.self_options, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    deleteMessage(message.getUid());
                }
            });
        }
        if (viewType == GroupChatAdapter.OTHER) {
            GroupService.getInstance().fetchGroupChatSetting(groupUid, AndroidSchedulers.mainThread(), message.getUserUid()).subscribe(new Subscriber<MessengerSetting>() {
                @Override
                public void onCompleted() {
                }

                @Override
                public void onError(Throwable e) {
                }

                @Override
                public void onNext(MessengerSetting messengerSetting) {
                    isMuted = messengerSetting.isCanSend();
                }
            });

            //0 - Delete Message
            //1 = Mute or Unmute user
            int otherOptions = (isMuted) ? R.array.other_muted_options : R.array.other_mute_options;
            builder.setItems(otherOptions, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    switch (i) {
                        case 0:
                            deleteMessage(message.getUid());
                            break;
                        case 1:
                            mute(message.getUserUid(), groupUid, false, !isMuted);
                            break;
                    }
                }
            });
        }
        builder.setCancelable(true)
                .create().show();

    }

    private void deleteAllMessages(final String groupUid) {
        RealmUtils.deleteAllGroupMessagesFromDb(groupUid).subscribe(new Action1<String>() {
            @Override
            public void call(String s) {
                loadMessages();
            }
        });

    }

    private void deleteMessage(final String messageId) {
        RealmUtils.deleteMessageFromDb(messageId).subscribe(new Action1<String>() {
            @Override
            public void call(String s) {
                loadMessages();
            }
        });

    }

}



