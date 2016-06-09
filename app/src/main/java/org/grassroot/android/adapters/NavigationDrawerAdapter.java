package org.grassroot.android.adapters;

/**
 * Created by Ravi on 29/07/15.
 */

import android.animation.AnimatorInflater;
import android.animation.AnimatorSet;
import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.grassroot.android.R;
import org.grassroot.android.events.NotificationEvent;
import org.grassroot.android.models.NavDrawerItem;
import org.grassroot.android.utils.PreferenceUtils;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.ArrayList;


public class NavigationDrawerAdapter extends RecyclerView.Adapter<NavigationDrawerAdapter.MyViewHolder> {

    private static final String TAG = NavigationDrawerAdapter.class.getCanonicalName();

    ArrayList<NavDrawerItem> data;
    private Context mContext;
    private LayoutInflater inflater;
    private AnimatorSet set;
    private MyViewHolder holder;

    public NavigationDrawerAdapter(Context context, ArrayList<NavDrawerItem> data) {
        this.data = data;
        this.mContext = context;
        this.inflater = LayoutInflater.from(context);

    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.listview_row_navigationdrawer, parent, false);
        holder = new MyViewHolder(view);
        return holder;
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {

        NavDrawerItem myDrawerModel = data.get(position);

        holder.title.setText(myDrawerModel.getTitle());
        holder.titleicon.setBackgroundResource(myDrawerModel.getIcon());


        if (myDrawerModel.isChecked()) {
            holder.rlDrawerRow.setBackgroundResource(R.color.text_beige);
            holder.title.setTextColor(mContext.getResources().getColor(R.color.primaryColor));
            holder.titleicon.setBackgroundResource(myDrawerModel.getChangeicon());
        } else {
            holder.rlDrawerRow.setBackgroundResource(R.color.white);
            holder.title.setTextColor(mContext.getResources().getColor(R.color.black));
        }

        if (myDrawerModel.getTitle().equalsIgnoreCase(mContext.getString(R.string.Notifications))) {
            holder.txtTitleCounter.setVisibility(View.VISIBLE);
            int notificationcount = PreferenceUtils.getIsNotificationcounter(mContext);
            Log.e(TAG, "notificationcount is " + notificationcount);
            holder.txtTitleCounter.setText("" + notificationcount);

            if (notificationcount > 0) {
                set = (AnimatorSet) AnimatorInflater.loadAnimator(mContext, R.animator.flip);
                set.setTarget(holder.txtTitleCounter);
                set.start();
            }


        } else {
            holder.txtTitleCounter.setVisibility(View.INVISIBLE);
        }

    }

    @Override
    public int getItemCount() {
        return data.size();
    }


    public static class MyViewHolder extends RecyclerView.ViewHolder {
        private final RelativeLayout rlDrawerRow;
        TextView title;
        TextView txtTitleCounter;
        ImageView titleicon;

        public MyViewHolder(View itemView) {
            super(itemView);
            rlDrawerRow = (RelativeLayout) itemView.findViewById(R.id.rl_drawer_row);
            title = (TextView) itemView.findViewById(R.id.txt_title);
            titleicon = (ImageView) itemView.findViewById(R.id.iv_titleicon);
            txtTitleCounter = (TextView) itemView.findViewById(R.id.txt_title_counter);
        }
    }


}