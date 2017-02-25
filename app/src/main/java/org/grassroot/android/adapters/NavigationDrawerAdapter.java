package org.grassroot.android.adapters;

/**
 * Recreated by Luke on 14/08/16
 */

import android.content.Context;
import android.graphics.Typeface;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.grassroot.android.R;
import org.grassroot.android.models.helpers.NavDrawerItem;

import java.util.List;
import java.util.Map;


public class NavigationDrawerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final String TAG = NavigationDrawerAdapter.class.getCanonicalName();

    private List<NavDrawerItem> data;
    private NavDrawerItemListener listener;

    public static final int SEPARATOR = 0; // separates sections
    public static final int PRIMARY = 1; // has a counter and can be selected
    public static final int SECONDARY = 2; // neither counter nor selected

    private Map<Integer, Integer> positionTypeMap;

    public interface NavDrawerItemListener {
        void onItemClicked(final String tag);
    }

    private final int textSelectedColor;
    private final int rowSelectedBgColor;
    private final int textNormalColor;
    private final int rowNormalColor;

    public NavigationDrawerAdapter(Context context, List<NavDrawerItem> items, Map<Integer, Integer> positionTypeMap,
                                   NavDrawerItemListener listener) {
        this.data = items;
        this.positionTypeMap = positionTypeMap;
        this.listener = listener;

        textSelectedColor = ContextCompat.getColor(context, R.color.primaryColor);
        rowSelectedBgColor = ContextCompat.getColor(context, R.color.text_beige);
        textNormalColor = ContextCompat.getColor(context, R.color.black);
        rowNormalColor = ContextCompat.getColor(context, R.color.white);
    }

    @Override
    public int getItemViewType(int position) {
        return positionTypeMap.get(position);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view;
        RecyclerView.ViewHolder viewHolder;
        switch (viewType) {
            case PRIMARY:
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_navdrawer_primary, parent, false);
                viewHolder = new PrimaryViewHolder(view);
                break;
            case SECONDARY:
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_navdrawer_secondary, parent, false);
                viewHolder = new SecondaryViewHolder(view);
                break;
            case SEPARATOR:
            default:
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_navdrawer_separator, parent, false);
                viewHolder = new SeparatorViewHolder(view);
                break;
        }

        return viewHolder;
    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, int position) {
        final NavDrawerItem drawerItem = data.get(position);
        final int itemType = positionTypeMap.get(position);

        switch (itemType) {
            case PRIMARY:
                bindPrimaryItem((PrimaryViewHolder) holder, drawerItem);
                break;
            case SECONDARY:
                bindSecondaryItem((SecondaryViewHolder) holder, drawerItem);
                break;
            case SEPARATOR:
            default:
                break;
        }
    }

    private void bindPrimaryItem(PrimaryViewHolder holder, final NavDrawerItem drawerItem) {
        holder.label.setText(drawerItem.getItemLabel());
        if (drawerItem.isChecked()) {
            holder.rootView.setBackgroundColor(rowSelectedBgColor);
            holder.label.setTextColor(textSelectedColor);
            holder.label.setTypeface(Typeface.DEFAULT_BOLD);
            holder.icon.setBackgroundResource(drawerItem.getSelectedIcon());
        } else {
            holder.rootView.setBackgroundColor(rowNormalColor);
            holder.label.setTextColor(textNormalColor);
            holder.label.setTypeface(Typeface.DEFAULT);
            holder.icon.setBackgroundResource(drawerItem.getDefaultIcon());
        }

        if (drawerItem.isShowItemCount()) {
            holder.counter.setVisibility(View.VISIBLE);
            holder.counter.setText(String.valueOf(drawerItem.getItemCount()));
        } else {
            Log.e(TAG, "error ... by definition primary items should have counter ...");
            holder.counter.setVisibility(View.GONE);
        }

        holder.rootView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onItemClicked(drawerItem.getTag());
            }
        });
    }

    private void bindSecondaryItem(SecondaryViewHolder holder, final NavDrawerItem drawerItem) {
        holder.label.setText(drawerItem.getItemLabel());
        holder.label.setTextColor(textNormalColor);
        holder.label.setTypeface(Typeface.DEFAULT);
        holder.icon.setBackgroundResource(drawerItem.getDefaultIcon());

        holder.rootView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onItemClicked(drawerItem.getTag());
            }
        });
    }

    public void switchItemTypes(Map<Integer, Integer> newTypeMap) {
        this.positionTypeMap = newTypeMap;
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    private static class PrimaryViewHolder extends RecyclerView.ViewHolder {
        ViewGroup rootView;
        TextView label;
        TextView counter;
        ImageView icon;

        public PrimaryViewHolder(View itemView) {
            super(itemView);
            rootView = (ViewGroup) itemView.findViewById(R.id.item_root_view);
            label = (TextView) itemView.findViewById(R.id.item_label);
            icon = (ImageView) itemView.findViewById(R.id.item_icon);
            counter = (TextView) itemView.findViewById(R.id.item_counter);
        }
    }

    private static class SecondaryViewHolder extends RecyclerView.ViewHolder {
        ViewGroup rootView;
        TextView label;
        ImageView icon;

        public SecondaryViewHolder(View itemView) {
            super(itemView);
            rootView = (ViewGroup) itemView.findViewById(R.id.item_root_view);
            label = (TextView) itemView.findViewById(R.id.item_label);
            icon = (ImageView) itemView.findViewById(R.id.item_icon);
        }
    }

    private static class SeparatorViewHolder extends RecyclerView.ViewHolder {
        // just make the separator isn't clickable
        View separator;

        public SeparatorViewHolder(View view) {
            super(view);
            separator = view.findViewById(R.id.nav_separator);
            separator.setClickable(false);
        }
    }
}