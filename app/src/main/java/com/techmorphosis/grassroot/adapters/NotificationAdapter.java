package com.techmorphosis.grassroot.adapters;

import android.app.Activity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.techmorphosis.grassroot.R;
import com.techmorphosis.grassroot.services.model.Notification;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by ravi on 12/5/16.
 */
public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.VH> {

    public List<Notification> notifyList = new ArrayList<>();
    private static final String TAG = "NotificationAdapter";
    private Activity activity;
    
    public NotificationAdapter(ArrayList<Notification> dataList) {
        this.notifyList = dataList;

    }

    public NotificationAdapter(Activity activity){
        this.activity = activity;
    }

    @Override
    public VH onCreateViewHolder(ViewGroup parent, int viewType)
    {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_notification, parent, false);
        VH vh = new VH(view);
        return vh;
    }

    @Override
    public void onBindViewHolder(VH holder, int position) {

        Notification notification = notifyList.get(position);
        holder.txtNcMessage.setText(notification.getMessage());
        holder.txtDate.setText(notification.getCreatedDateTime());

    }

    @Override
    public int getItemCount() {
        return notifyList.size();
    }

    public class VH extends RecyclerView.ViewHolder {

        @BindView(R.id.main_view)
        CardView mainView;
        @BindView(R.id.txt_nc_message)
        TextView txtNcMessage;
        @BindView(R.id.txt_date)
        TextView txtDate;

        public VH(View view) {
            super(view);
            ButterKnife.bind(this, view);
        }
    }

    public void addData(List<Notification> notificationList){
        this.notifyList.addAll(notificationList);
        this.notifyDataSetChanged();

    }

    public void updateData(List<Notification> notifications){
        this.notifyList.addAll(notifications);



    }
}
