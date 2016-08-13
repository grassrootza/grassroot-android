package org.grassroot.android.adapters;

/**
 * Created by Ravi on 29/07/15.
 */

import android.content.Context;
import android.graphics.Typeface;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.grassroot.android.R;
import org.grassroot.android.models.NavDrawerItem;

import java.util.List;


public class NavigationDrawerAdapter extends RecyclerView.Adapter<NavigationDrawerAdapter.MyViewHolder> {

    private static final String TAG = NavigationDrawerAdapter.class.getCanonicalName();

    private List<NavDrawerItem> data;
    private NavDrawerItemListener listener;

    public interface NavDrawerItemListener {
        void onItemClicked(final String tag);
    }

    private final boolean showCounters;
    private final boolean showSelected;

    final int textSelectedColor;
    final int rowSelectedBgColor;
    final int textNormalColor;
    final int rowNormalColor;

    public NavigationDrawerAdapter(Context context, List<NavDrawerItem> data, boolean showCounters,
                                   boolean showSelected, NavDrawerItemListener listener) {
        this.data = data;
        this.listener = listener;
        this.showCounters = showCounters;
        this.showSelected = showSelected;

        textSelectedColor = ContextCompat.getColor(context, R.color.primaryColor);
        rowSelectedBgColor = ContextCompat.getColor(context, R.color.text_beige);
        textNormalColor = ContextCompat.getColor(context, R.color.black);
        rowNormalColor = ContextCompat.getColor(context, R.color.white);
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.listview_row_navigationdrawer, parent, false);
        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final MyViewHolder holder, int position) {

        final NavDrawerItem drawerItem = data.get(position);

        holder.title.setText(drawerItem.getItemLabel());

        if (showSelected && drawerItem.isChecked()) {
            holder.rlDrawerRow.setBackgroundColor(rowSelectedBgColor);
            holder.title.setTextColor(textSelectedColor);
            holder.title.setTypeface(Typeface.DEFAULT_BOLD);
            holder.titleicon.setBackgroundResource(drawerItem.getSelectedIcon());
        } else {
            holder.rlDrawerRow.setBackgroundColor(rowNormalColor);
            holder.title.setTextColor(textNormalColor);
            holder.title.setTypeface(Typeface.DEFAULT);
            holder.titleicon.setBackgroundResource(drawerItem.getDefaultIcon());
        }

        if (showCounters && drawerItem.isShowItemCount()) {
            holder.txtTitleCounter.setVisibility(View.VISIBLE);
            holder.txtTitleCounter.setText(String.valueOf(drawerItem.getItemCount()));
        } else {
            holder.txtTitleCounter.setVisibility(View.GONE);
        }

        holder.rlDrawerRow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onItemClicked(drawerItem.getTag());
            }
        });
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {

        RelativeLayout rlDrawerRow;

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