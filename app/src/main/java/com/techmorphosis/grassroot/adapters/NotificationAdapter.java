package com.techmorphosis.grassroot.adapters;

import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.techmorphosis.grassroot.R;
import com.techmorphosis.grassroot.models.NotificationModel;

import java.util.ArrayList;

/**
 * Created by ravi on 12/5/16.
 */
public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.VH> {

    public ArrayList<NotificationModel> notifyList;
    private static final String TAG = "NotificationAdapter";
    
    public NotificationAdapter(ArrayList<NotificationModel> dataList) {
        this.notifyList = dataList;

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

        NotificationModel setmodel = (NotificationModel)notifyList.get(position);
        holder.txtNcMessage.setText(setmodel.getMessage());
        holder.txtDate.setText(setmodel.getCreatedDatetime());

    }

    @Override
    public int getItemCount() {
        return notifyList.size();
    }

    public class VH extends RecyclerView.ViewHolder {


        private CardView mainView;
        private TextView txtNcMessage;
        private TextView txtDate;

        public VH(View view) {
            super(view);
            mainView = (CardView) view.findViewById(R.id.main_view);
            txtNcMessage = (TextView) view.findViewById(R.id.txt_nc_message);
            txtDate = (TextView) view.findViewById(R.id.txt_date);
        }
    }
}
