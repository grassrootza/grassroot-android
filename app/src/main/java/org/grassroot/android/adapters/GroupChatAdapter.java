package org.grassroot.android.adapters;


import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import org.grassroot.android.R;
import org.grassroot.android.fragments.GroupChatFragment;
import org.grassroot.android.models.Message;
import org.grassroot.android.services.ApplicationLoader;
import org.grassroot.android.utils.MqttConnectionManager;
import org.grassroot.android.utils.RealmUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import hani.momanii.supernova_emoji_library.Helper.EmojiconTextView;
import rx.functions.Action1;


/**
 * Created by paballo on 2016/08/30.
 */
public class GroupChatAdapter extends RecyclerView.Adapter<GroupChatAdapter.GCViewHolder> {

    private static final String TAG = GroupChatAdapter.class.getSimpleName();
    private SimpleDateFormat dateFormatter = new SimpleDateFormat("HH:mm");
    private Context context;
    private GroupChatAdapterListener listener;

    private int selectedPos = -1;

    private List<Message> messages;
    public final static int SELF = 100;
    public final static int OTHER = 200;
    public final static int SERVER = 300;
    public final static int ERROR = 400;

    private final String deliveredSub;
    private final String sentSub;
    private final String sendingSub;
    private final String notSentSub;
    private final String readSub;
    private final String errorSub;

    private final String waitMsg;

    private final String thisPhoneNumber;

    public interface GroupChatAdapterListener {
        void createTaskFromMessage(Message message);
    }

    public GroupChatAdapter(List<Message> messages, GroupChatFragment fragment) {
        this.context = ApplicationLoader.applicationContext;
        this.messages = new ArrayList<>(messages);
        this.listener = fragment;

        deliveredSub = context.getString(R.string.chat_message_delivered);
        sentSub = context.getString(R.string.chat_message_sent);
        sendingSub = context.getString(R.string.chat_message_sending);
        notSentSub = context.getString(R.string.chat_message_not_sent);
        readSub = context.getString(R.string.chat_message_read);
        errorSub = context.getString(R.string.chat_message_error);

        waitMsg = context.getString(R.string.wait_message);

        thisPhoneNumber = RealmUtils.loadPreferencesFromDB().getMobileNumber();

        notifyDataSetChanged();
    }

    @Override
    public GCViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View message;
        if (viewType == SELF) {
            message = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_chat_self, parent, false);
        } else if (viewType == OTHER) {
            message = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_chat_other, parent, false);
        } else {
            message = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_chat_server, parent, false);
        }
        return new GCViewHolder(message);
    }


    @Override
    public void onBindViewHolder(GCViewHolder holder, int position) {
        holder.itemView.setSelected(selectedPos == position);
        final Message message = messages.get(position);
        final int viewType = getItemViewType(position);

        holder.message.setText(message.getText());
        final String time = DateUtils.isToday(message.getTime().getTime()) ? dateFormatter.format(message.getTime()) : (String)
                DateUtils.getRelativeDateTimeString(context, message.getTime().getTime(),
                        System.currentTimeMillis(), DateUtils.SECOND_IN_MILLIS, DateUtils.FORMAT_ABBREV_RELATIVE);
        holder.timestamp.setText(time);

        switch (viewType) {
            case OTHER:
                holder.user.setText(message.getDisplayName());
                holder.user.setVisibility(View.VISIBLE);
                break;
            case SERVER:
                holder.user.setText(context.getText(R.string.chat_message_server));
                holder.user.setVisibility(View.VISIBLE);
                final boolean validCommand = message.getTokens() != null && message.getTokens().size() > 0;
                handleServerMessageBtns(holder, validCommand, message);
                break;
            case ERROR:
                holder.user.setText(context.getString(R.string.chat_error_header));
                holder.user.setVisibility(View.VISIBLE);
                if (message.hasCommands()) {
                    handleErrorMessageBtns(holder, message);
                }
            case SELF:
                final String subtitle = message.isRead() ? readSub
                        : message.isDelivered() ? deliveredSub
                        : message.isSent() ? sentSub
                        : message.isSending() ? sendingSub
                        : message.isErrorMessage() ? errorSub
                        : message.exceedsMaximumSendingAttempts() ? notSentSub : "";
                holder.timestamp.setText(subtitle.concat(time));
                break;
        }
    }

    private void setGroupList(List<Message> messages) {
        this.messages = new ArrayList<>(messages);
        this.notifyDataSetChanged();
    }

    public void reloadFromdb(String groupUid) {
        RealmUtils.loadMessagesFromDb(groupUid).subscribe(new Action1<List<Message>>() {
            @Override
            public void call(List<Message> messages) {
                setGroupList(messages);
            }
        });
    }

    public void addOrUpdateMessage(Message message) {
        if (messages.contains(message)) {
            updateMessage(message);
        } else {
            addMessage(message);
        }
    }

    public void addMessage(Message message) {
        this.messages.add(message);
        this.notifyItemInserted(getItemCount() - 1);
    }

    public Message findMessage(String messageUid) {
        for (int i = messages.size() - 1; i >= 0; i--) {
            if (messages.get(i).getUid().equals(messageUid)) {
                return messages.get(i);
            }
        }
        return null;
    }

    public void deleteAll() {
        this.messages.clear();
        this.notifyDataSetChanged();
    }

    public void updateMessage(final Message message) {
        for (int i = 0; i < messages.size(); i++) {
            if (messages.get(i).getUid().equals(message.getUid())) {
                messages.set(i, message);
            }
        }

        if (message.isServerMessage()) {
            removeOldSystemMessages(message);
        }

        this.notifyDataSetChanged();
    }

    private void removeOldSystemMessages(Message messageToKeep) {
        for (int i = messages.size() - 1; i >= 0; i--) {
            if (!messages.get(i).equals(messageToKeep) && messages.get(i).isServerMessage() && !messages.get(i).isToKeep()) {
                RealmUtils.deleteMessageFromDb(messages.get(i).getUid()).subscribe();
                messages.remove(i);
            }
        }
    }

    public void deleteOne(String messageUid) {
        for (int i = 0; i < messages.size(); i++) {
            if (messages.get(i).getUid().equals(messageUid)) {
                messages.remove(i);
                this.notifyItemRemoved(i);
            }
        }
    }

    public void selectPosition(int position) {
        int oldPos = selectedPos;
        if (position == selectedPos) {
            selectedPos = -1;
            notifyItemChanged(oldPos);
        } else {
            selectedPos = position;
            notifyItemChanged(selectedPos);
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    @Override
    public int getItemViewType(int position) {
        Message message = messages.get(position);
        if (message != null && message.isServerMessage()) {
            return SERVER;
        } else if (message != null && message.isErrorMessage()) {
            return ERROR;
        } else if (message != null && (TextUtils.isEmpty(message.getPhoneNumber()) || !message.getPhoneNumber().equals(thisPhoneNumber))) {
            return OTHER;
        } else {
            return SELF;
        }
    }

    public List<Message> getMessages() {
        return messages;
    }

    private void handleServerMessageBtns(GCViewHolder holder, boolean buttonsVisible, final Message message) {
        if (buttonsVisible && holder.bt_yes != null && holder.bt_no != null) {
            holder.bt_yes.setVisibility(View.VISIBLE);
            holder.bt_no.setVisibility(View.VISIBLE);

            holder.bt_no.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    RealmUtils.deleteMessageFromDb(message.getUid()).subscribe(new Action1<String>() {
                        @Override
                        public void call(String s) {
                            reloadFromdb(message.getGroupUid());
                        }
                    });
                }
            });
            holder.bt_yes.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    listener.createTaskFromMessage(message);
                }
            });
        } else {
            if (holder.bt_no != null && holder.bt_yes != null) {
                holder.bt_yes.setVisibility(View.GONE);
                holder.bt_no.setVisibility(View.GONE);
            }
        }
    }

    private void handleErrorMessageBtns(final GCViewHolder holder, final Message message) {
        if (holder.bt_yes != null && holder.bt_no != null) {
            holder.bt_yes.setVisibility(View.VISIBLE);
            holder.bt_no.setVisibility(View.VISIBLE);

            holder.bt_no.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    reloadFromdb(message.getGroupUid());
                }
            });

            holder.bt_yes.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    MqttConnectionManager.getInstance().connect();
                    holder.bt_yes.setVisibility(View.GONE);
                    holder.bt_no.setVisibility(View.GONE);
                    holder.message.setText(waitMsg);
                    message.setHasCommands(false);
                    message.setText(waitMsg);
                }
            });
        }

    }

    class GCViewHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.timestamp)
        TextView timestamp;

        @BindView(R.id.user)
        TextView user;

        @BindView(R.id.text)
        EmojiconTextView message;

        @Nullable
        @BindView(R.id.bt_yes)
        Button bt_yes;

        @Nullable
        @BindView(R.id.bt_no)
        Button bt_no;

        GCViewHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
        }

    }

}