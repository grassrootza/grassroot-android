package org.grassroot.android.adapters;

/**
 * Created by Ravi on 29/07/15.
 */

import android.animation.AnimatorInflater;
import android.animation.AnimatorSet;
import android.content.Context;
import android.graphics.Typeface;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.grassroot.android.R;
import org.grassroot.android.interfaces.NavigationConstants;
import org.grassroot.android.models.NavDrawerItem;
import org.grassroot.android.services.ApplicationLoader;
import org.grassroot.android.utils.PreferenceUtils;

import java.util.ArrayList;
import java.util.List;


public class NavigationDrawerAdapter extends RecyclerView.Adapter<NavigationDrawerAdapter.MyViewHolder> {

    private static final String TAG = NavigationDrawerAdapter.class.getCanonicalName();

    List<NavDrawerItem> data;
    private Context mContext;
    private LayoutInflater inflater;
    private AnimatorSet set;
    private MyViewHolder holder;

    final int textSelectedColor;
    final int rowSelectedBgColor;
    final int textNormalColor;
    final int rowNormalColor;

    public NavigationDrawerAdapter(Context context, List<NavDrawerItem> data) {
        this.data = data;
        this.mContext = context;
        this.inflater = LayoutInflater.from(context);

        textSelectedColor = ContextCompat.getColor(context, R.color.primaryColor);
        rowSelectedBgColor = ContextCompat.getColor(context, R.color.text_beige);
        textNormalColor = ContextCompat.getColor(context, R.color.black);
        rowNormalColor = ContextCompat.getColor(context, R.color.white);
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.listview_row_navigationdrawer, parent, false);
        holder = new MyViewHolder(view);
        return holder;
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {

        NavDrawerItem drawerItem = data.get(position);

        holder.title.setText(drawerItem.getTitle());
        holder.titleicon.setBackgroundResource(drawerItem.getIcon());

        if (drawerItem.isChecked()) {
            holder.rlDrawerRow.setBackgroundColor(rowSelectedBgColor);
            holder.title.setTextColor(textSelectedColor);
            holder.title.setTypeface(Typeface.DEFAULT_BOLD);
            holder.titleicon.setBackgroundResource(drawerItem.getChangeicon());
        } else {
            holder.rlDrawerRow.setBackgroundColor(rowNormalColor);
            holder.title.setTextColor(textNormalColor);
        }

        if (drawerItem.isShowItemCount()) {
            holder.txtTitleCounter.setVisibility(View.VISIBLE);
            holder.txtTitleCounter.setText(String.valueOf(drawerItem.getItemCount()));
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