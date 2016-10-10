package org.grassroot.android.fragments;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
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
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.ImageView;

import org.grassroot.android.R;
import org.grassroot.android.activities.GroupTasksActivity;
import org.grassroot.android.activities.MultiMessageNotificationActivity;
import org.grassroot.android.adapters.CommandsAdapter;
import org.grassroot.android.adapters.GroupChatAdapter;
import org.grassroot.android.adapters.RecyclerTouchListener;
import org.grassroot.android.events.GroupChatEvent;
import org.grassroot.android.events.MessageNotSentEvent;
import org.grassroot.android.interfaces.ClickListener;
import org.grassroot.android.interfaces.GroupConstants;
import org.grassroot.android.interfaces.TaskConstants;
import org.grassroot.android.models.Command;
import org.grassroot.android.models.Message;
import org.grassroot.android.models.RealmString;
import org.grassroot.android.models.TaskModel;
import org.grassroot.android.models.responses.GroupChatSettingResponse;
import org.grassroot.android.services.GcmListenerService;
import org.grassroot.android.services.GcmUpstreamMessageService;
import org.grassroot.android.services.GroupService;
import org.grassroot.android.services.TaskService;
import org.grassroot.android.utils.Constant;
import org.grassroot.android.utils.EmojIconMultiAutoCompleteActions;
import org.grassroot.android.utils.RealmUtils;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import hani.momanii.supernova_emoji_library.Helper.EmojiconMultiAutoCompleteTextView;
import io.realm.RealmList;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;

/**
 * Created by paballo on 2016/08/30.
 */
public class GroupChatFragment extends Fragment implements GroupChatAdapter.GroupChatAdapterListener {

    private static final String TAG = GroupChatFragment.class.getCanonicalName();

    private String groupUid;
    private String groupName;
    private boolean isMuted;
    private List<String> mutedUsersUid;

    private Unbinder unbinder;

    @BindView(R.id.root_view)
    ViewGroup rootView;

    @BindView(R.id.emoji_btn)
    ImageView openEmojis;
    @BindView(R.id.gc_recycler_view)
    RecyclerView chatMessageView;
    @BindView(R.id.text)
    EmojiconMultiAutoCompleteTextView textView;
    @BindView(R.id.btn_send)
    ImageView sendMessage;

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
        EventBus.getDefault().register(this);

        groupUid = getArguments().getString(GroupConstants.UID_FIELD);
        groupName = getArguments().getString(GroupConstants.NAME_FIELD);
        mutedUsersUid = new ArrayList<>();

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
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        loadGroupSettings();

    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        if (menu.findItem(R.id.mi_group_mute) != null) {
            menu.findItem(R.id.mi_group_mute).setVisible(true);
            menu.findItem(R.id.mi_group_mute).setTitle(!isMuted ? R.string.gp_mute : R.string.gp_un_mute);
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
            mute(null, groupUid, true, !isMuted ? false : true);
        if (item.getItemId() == R.id.mi_delete_messages) deleteAllMessages(groupUid);

        return super.onOptionsItemSelected(item);
    }

    private void setView() {
        if (getActivity() instanceof MultiMessageNotificationActivity) {
            GcmListenerService.clearChatNotifications(getContext());
            getActivity().setTitle(groupName);
        }
        loadMessages();
        textView.setInputType(textView.getInputType() & (~EditorInfo.TYPE_TEXT_FLAG_AUTO_COMPLETE));
        textView.setAdapter(arrayAdapter);
        textView.setThreshold(1); //setting it in xml does not seem to be working
        textView.requestFocus();


        textView.setEnabled(!isMuted);
        sendMessage.setEnabled(!isMuted);
        openEmojis.setEnabled(!isMuted);
        emojIconAction = new EmojIconMultiAutoCompleteActions(getActivity(), rootView, textView, openEmojis);
        emojIconAction.ShowEmojIcon();
        RealmUtils.markMessagesAsRead(groupUid);

    }

    @OnClick(R.id.btn_send)
    public void sendMessage() {

        if (!TextUtils.isEmpty(textView.getText())) {
            final String phoneNumber = RealmUtils.loadPreferencesFromDB().getMobileNumber();
            Message message = new Message(phoneNumber, groupUid, null, new Date(), textView.getText().toString(), false, "");
            RealmUtils.saveDataToRealmSync(message);
            groupChatAdapter.addMessage(message);
            chatMessageView.smoothScrollToPosition(groupChatAdapter.getItemCount());
            GcmUpstreamMessageService.sendMessage(message, getActivity(),
                    AndroidSchedulers.mainThread())
                    .subscribe(new Action1<String>() {
                        @Override
                        public void call(String s) {
                        }
                    });

            textView.setText(""); //clear text
            textView.requestFocus();
            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(getActivity().getCurrentFocus().getWindowToken(), 0);
        }
    }

    public void loadMessages() {
        RealmUtils.loadMessagesFromDb(groupUid).subscribe(new Action1<List<Message>>() {
            @Override
            public void call(List<Message> msgs) {
                if (groupChatAdapter != null) {
                    groupChatAdapter.reloadFromdb(groupUid);
                } else {
                    setUpListAndAdapter(msgs);
                }
                chatMessageView.smoothScrollToPosition(groupChatAdapter.getItemCount());
            }
        });

    }

    private void setUpListAndAdapter(List<Message> messages) {
        groupChatAdapter = new GroupChatAdapter(messages, this);
        layoutManager = new LinearLayoutManager(getActivity());
        layoutManager.setAutoMeasureEnabled(true);
        layoutManager.setStackFromEnd(true);
        if (chatMessageView != null) {
            chatMessageView.setAdapter(groupChatAdapter);
            chatMessageView.setLayoutManager(layoutManager);
            chatMessageView.setItemViewCacheSize(20);
            chatMessageView.setDrawingCacheEnabled(true);
            chatMessageView.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);

            chatMessageView.addOnItemTouchListener(new RecyclerTouchListener(getActivity(), chatMessageView, new ClickListener() {
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
        String groupUidInMessage = groupChatEvent.getGroupUid();
        if ((this.isVisible() && !groupUidInMessage.equals(groupUid))) {
            GcmListenerService.showNotification(groupChatEvent.getBundle(), getActivity()).subscribe();
            RealmUtils.markMessagesAsRead(groupUid);
        } else if (((this.isVisible() && groupUidInMessage.equals(groupUid)) && !isActiveTab(groupUidInMessage))) {
            GcmListenerService.showNotification(groupChatEvent.getBundle(), getActivity()).subscribe();
            updateRecyclerView(groupChatEvent);
            RealmUtils.markMessagesAsRead(groupUid);
        } else {
            updateRecyclerView(groupChatEvent);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(MessageNotSentEvent messageNotSentEvent) {
        Message message = messageNotSentEvent.getMessage();
        if (groupChatAdapter.getMessages().contains(message)) {
            groupChatAdapter.updateMessage(message);
        }
    }

    @Override
    public void createTaskFromMessage(final Message message) {
        String title = message.getTokens().get(0).getString();
        String location = null;
        if(message.getType().equals(TaskConstants.MEETING))location = message.getTokens()
                .get(2).getString();
        String time = message.getTokens().get(1).getString();
        TaskModel taskModel = generateTaskObject(message.getGroupUid(), title,time , location,message.getType() );
        TaskService.getInstance().sendTaskToServer(taskModel, AndroidSchedulers.mainThread()).subscribe(new Subscriber<TaskModel>() {
            @Override
            public void onCompleted() {}

            @Override
            public void onError(Throwable e) {
                Snackbar.make(rootView,R.string.chat_task_failure,Snackbar.LENGTH_LONG).show();
            }

            @Override
            public void onNext(TaskModel taskModel) {
                Snackbar.make(rootView,getString(R.string.chat_task_called,
                        taskModel.getType().toLowerCase()),Snackbar.LENGTH_LONG).show();
                RealmUtils.deleteMessageFromDb(message.getUid()).subscribe(new Action1<String>() {
                    @Override
                    public void call(String s) {
                        groupChatAdapter.reloadFromdb(message.getGroupUid());
                    }
                });
            }
        });

    }


    public String getGroupUid() {
        return groupUid;
    }

    private void loadGroupSettings() {
        GroupService.getInstance().fetchGroupChatSetting(groupUid, AndroidSchedulers.mainThread(), null).subscribe(new Subscriber<GroupChatSettingResponse>() {
            @Override
            public void onNext(GroupChatSettingResponse groupChatSettingResponse) {
                isMuted = groupChatSettingResponse.isCanSend();
                mutedUsersUid = groupChatSettingResponse.getMutedUsersUids();
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
        Log.e(TAG, "Grassroot: calling mute user of user");
        GroupService.getInstance().updateMemberChatSetting(groupUid, userUid, userInitiated, active, AndroidSchedulers.mainThread()).subscribe(new Action1<String>() {
            @Override
            public void call(String s) {
                if (userUid == null) {
                    updateEntryView(active);
                    getActivity().supportInvalidateOptionsMenu();
                } else {
                    if (active) {
                        mutedUsersUid.remove(userUid);
                    } else {
                        mutedUsersUid.add(userUid);
                    }
                }
            }
        });
    }

    private void updateEntryView(boolean active) {
        isMuted = !active;
        textView.setEnabled(active);
        sendMessage.setEnabled(active);
        openEmojis.setEnabled(active);
    }

    private void longClickOptions(final Message message, int messageType) {

        if (messageType != GroupChatAdapter.SERVER) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

            if (messageType == GroupChatAdapter.SELF) {
                builder.setItems(R.array.self_options, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        deleteMessage(message.getUid());
                    }
                });
            } else if (messageType == GroupChatAdapter.OTHER) {
                //0 - Delete Message
                //1 = Mute or Unmute user

                int otherOptions = (mutedUsersUid != null && mutedUsersUid.contains(message.getUid())) ?
                        R.array.chat_msg_already_muted : R.array.chat_msg_mute_available;

                builder.setItems(otherOptions, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        switch (i) {
                            case 0:
                                deleteMessage(message.getUid());
                                break;
                            case 1:
                                mute(message.getUserUid(), groupUid, false, !mutedUsersUid.contains(message.getUid()));
                                break;
                        }
                    }
                });
            }

            builder.setCancelable(true).create().show();
        }

    }

    private void deleteAllMessages(final String groupUid) {
        RealmUtils.deleteAllGroupMessagesFromDb(groupUid).subscribe(new Action1<String>() {
            @Override
            public void call(String s) {
                groupChatAdapter.deleteAll();
            }
        });

    }

    private void deleteMessage(final String messageId) {
        RealmUtils.deleteMessageFromDb(messageId).subscribe(new Action1<String>() {
            @Override
            public void call(String s) {
                groupChatAdapter.deleteOne(messageId);
            }
        });

    }

    private void updateRecyclerView(GroupChatEvent groupChatEvent) {
        Message message = groupChatEvent.getMessage();
        if (groupChatAdapter.getMessages().contains(message)) {
            groupChatAdapter.updateMessage(message);
        } else {
            groupChatAdapter.addMessage(message);
        }
        chatMessageView.smoothScrollToPosition(groupChatAdapter.getItemCount());
    }

    private boolean isActiveTab(String groupUid) {
        if (getActivity() instanceof GroupTasksActivity) {
            GroupTaskMasterFragment masterFragment = (GroupTaskMasterFragment) this.getParentFragment();
            return masterFragment.getRequestPager().getCurrentItem() == 1;
        }
        return (getActivity() instanceof MultiMessageNotificationActivity &&  this.groupUid == groupUid);
    }


    private TaskModel generateTaskObject(String groupUid, String title, String time, @Nullable String venue, String type) {

        TaskModel model = new TaskModel();
        model.setTitle(title);
        model.setDescription(title);
        model.setCreatedByUserName(RealmUtils.loadPreferencesFromDB().getUserName());
        model.setDeadlineISO(time);
        model.setLocation(venue);
        model.setParentUid(groupUid);
        model.setTaskUid(UUID.randomUUID().toString());
        model.setType(type);
        model.setParentLocal(false);
        model.setLocal(true);
        model.setMinutes(0);
        model.setCanEdit(true);
        model.setCanAction(true);
        model.setReply(TaskConstants.TODO_PENDING);
        model.setHasResponded(false);
        model.setWholeGroupAssigned(true);
        model.setMemberUIDS(new RealmList<RealmString>());

        return model;
    }


}