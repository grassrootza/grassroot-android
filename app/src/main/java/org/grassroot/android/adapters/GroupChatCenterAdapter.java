package org.grassroot.android.adapters;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.grassroot.android.R;
import org.grassroot.android.models.Message;
import org.grassroot.android.utils.RealmUtils;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import rx.functions.Action1;

/**
 * Created by paballo on 2016/09/12.
 */
public class GroupChatCenterAdapter extends RecyclerView.Adapter<GroupChatCenterAdapter.GCCViewHolder> {

    private List<Message> messages;



    public GroupChatCenterAdapter(List<Message> messages){
        this.messages = new ArrayList<>(messages);
        notifyDataSetChanged();
    }


    @Override
    public GCCViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new GCCViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.row_chat_center, parent, false));
    }

    @Override
    public void onBindViewHolder(GCCViewHolder holder, int position) {
        Message message = messages.get(position);
        holder.txt_message.setText(message.getText());
        holder.txt_name.setText(message.getGroupName());
        long count = RealmUtils.countUnreadMessages(message.getGroupUid());
      //  holder


    }


    public void reloadFromDb(){
       RealmUtils.loadDistinctMessages().subscribe(new Action1<List<Message>>() {
            @Override
            public void call(List<Message> messages) {
                setList(messages);

            }
        });
    }

    private void setList(List<Message> messages){
        messages.clear();
        messages.addAll(messages);
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    public List<Message> getChatList(){
        return messages;
    }


    public class GCCViewHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.notification_icon)
        ImageView im_notification_icon;

        @BindView(R.id.name)
        TextView txt_name;

        @BindView(R.id.message)
        TextView txt_message;

        @BindView(R.id.timestamp)
        TextView txt_time;


        @BindView(R.id.count)
        TextView txt_count;


        public GCCViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }
}
