package org.grassroot.android.adapters;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.grassroot.android.R;
import org.grassroot.android.models.Message;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by paballo on 2016/09/12.
 */
public class GroupChatCenterAdapter extends RecyclerView.Adapter<GroupChatCenterAdapter.GCCViewHolder> {

    private List<Message> messages = new ArrayList<>();


    @Override
    public GCCViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new GCCViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.row_chat_center, parent, false));
    }

    @Override
    public void onBindViewHolder(GCCViewHolder holder, int position) {
        Message message = messages.get(position);
        holder.txt_message.setText(message.getText());
        holder.txt_name.setText(message.getText());
      //  holder


    }

    @Override
    public int getItemCount() {
        return messages.size();
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
