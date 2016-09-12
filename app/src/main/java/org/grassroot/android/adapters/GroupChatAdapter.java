package org.grassroot.android.adapters;


import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.grassroot.android.R;
import org.grassroot.android.models.Group;
import org.grassroot.android.models.Message;
import org.grassroot.android.utils.RealmUtils;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import rx.functions.Action1;


/**
 * Created by paballo on 2016/08/30.
 */
public class GroupChatAdapter extends RecyclerView.Adapter<GroupChatAdapter.GCViewHolder> {


    List<Message> messages;
    private final int SELF = 100;
    private final int OTHER = 200;


    public GroupChatAdapter(List<Message> messages) {
        this.messages = new ArrayList<>(messages);
        notifyDataSetChanged();
    }

    @Override
    public GCViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View message;
        if (viewType == SELF) {
            message = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.row_chat_self, parent, false);
        } else {
            message = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.row_chat_other, parent, false);
        }
        return new GCViewHolder(message);
    }


    @Override
    public void onBindViewHolder(GCViewHolder holder, int position) {
        Message message = messages.get(position);
        holder.message.setText(message.getText());
        holder.timestamp.setText(message.getTime().toString());
       if(getItemViewType(position)==OTHER) {
            holder.user.setText(message.getDisplayName());
            holder.user.setVisibility(View.VISIBLE);
        }
        if(getItemViewType(position)==SELF && message.isDelivered()) {
            holder.sent.setVisibility(View.VISIBLE);
        }
   }

    public void setGroupList(List<Message> messages) {
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

    @Override
    public int getItemCount() {
        return messages.size();
    }

    @Override
    public int getItemViewType(int position) {
        Message message = messages.get(position);
        if (message != null && !message.getPhoneNumber().equals(RealmUtils.loadPreferencesFromDB().
                getMobileNumber())) {
            return OTHER;
        }
        return SELF;
    }

    public class GCViewHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.timestamp)
        TextView timestamp;

        @BindView(R.id.user)
        TextView user;

        @BindView(R.id.text)
        TextView message;

        @BindView(R.id.sent)
        TextView sent;

        public GCViewHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
        }

    }




}