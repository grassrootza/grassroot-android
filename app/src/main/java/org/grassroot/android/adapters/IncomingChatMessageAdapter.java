package org.grassroot.android.adapters;

import android.app.Activity;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.grassroot.android.R;
import org.grassroot.android.models.Message;
import org.grassroot.android.utils.RealmUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import hani.momanii.supernova_emoji_library.Helper.EmojiconTextView;
import io.reactivex.functions.Consumer;

/**
 * Created by paballo on 2016/09/12.
 */
public class IncomingChatMessageAdapter extends RecyclerView.Adapter<IncomingChatMessageAdapter.GCCViewHolder> {

    private List<Message> messages;
    private Activity activity;
    private SimpleDateFormat dateFormatter = new SimpleDateFormat("HH:mm");

    public IncomingChatMessageAdapter(Activity activity, List<Message> messages){
        this.messages = new ArrayList<>(messages);
        this.activity = activity;
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
        String text = message.getText();
        holder.txt_message.setText(text);
        holder.txt_name.setText(message.getGroupName());
        long count = RealmUtils.countUnreadMessages(message.getGroupUid());
        if(count >0){
            holder.txt_count.setText(Long.toString(count));
            holder.txt_count.setVisibility(View.VISIBLE);
        }
        String time = DateUtils.isToday(message.getTime().getTime())? dateFormatter.format(message.getTime()): (String)
                DateUtils.getRelativeDateTimeString(activity, message.getTime().getTime(),
                        System.currentTimeMillis(), DateUtils.SECOND_IN_MILLIS, DateUtils.FORMAT_ABBREV_RELATIVE);
        holder.txt_time.setText(time);

    }

    public void reloadFromDb(){
       RealmUtils.loadDistinctMessages().subscribe(new Consumer<List<Message>>() {
            @Override
            public void accept(List<Message> messages) {
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
        EmojiconTextView txt_message;

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
