package org.grassroot.android.adapters;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.grassroot.android.R;

/**
 * Created by ravi on 6/5/16.
 */
public class ProfileAdapter extends RecyclerView.Adapter<ProfileAdapter.ProfileHolder> {

    final int[] titles;
    final int[] icons;

    public ProfileAdapter(int[] titles, int[] icons) {
        this.titles = titles;
        this.icons = icons;
    }

    @Override
    public ProfileHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_profile,parent,false);
        return new ProfileHolder(v);
    }

    @Override
    public void onBindViewHolder(ProfileHolder holder, int position) {
        holder.icon.setImageResource(icons[position]);
        holder.text.setText(titles[position]);
    }

    @Override
    public int getItemCount() {
        return titles.length;
    }

    public class ProfileHolder extends RecyclerView.ViewHolder {

        private ImageView icon;
        private TextView text;

        public ProfileHolder(View view) {
            super(view);
            icon = (ImageView) view.findViewById(R.id.iv_pp_title_icon);
            text = (TextView) view.findViewById(R.id.txt_pp_title);
        }
    }
}
