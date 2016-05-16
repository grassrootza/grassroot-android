package com.techmorphosis.grassroot.adapters;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.techmorphosis.grassroot.R;

import java.util.ArrayList;

/**
 * Created by ravi on 6/5/16.
 */
public class ProfileAdapter extends RecyclerView.Adapter<ProfileAdapter.ProfileHolder> {

    ArrayList<String> titlelist;

    public ProfileAdapter()
    {

    }

    @Override
    public ProfileHolder onCreateViewHolder(ViewGroup parent, int viewType)
    {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_profile,parent,false);
        ProfileHolder profileHolder = new ProfileHolder(v);
        return  profileHolder;

    }

    @Override
    public void onBindViewHolder(ProfileHolder holder, int position)
    {
        switch (position)
        {
            case  0 ://Updtaename
            holder.ivPpTitleIcon.setImageResource(R.drawable.ic_update_name);
            holder.txtPpTitle.setText(R.string.pp_txt_Updatename);
            break;

            case  1 ://language
                holder.ivPpTitleIcon.setImageResource(R.drawable.ic_language);
                holder.txtPpTitle.setText(R.string.pp_txt_language);
            break;

            case  2 ://notifications
                holder.ivPpTitleIcon.setImageResource(R.drawable.ic_configure);
                holder.txtPpTitle.setText(R.string.pp_txt_notifications);
            break;

            case  3 ://Settings
                holder.ivPpTitleIcon.setImageResource(R.drawable.ic_security);
                holder.txtPpTitle.setText(R.string.pp_txt_settings);
             break;


        }
    }

    @Override
    public int getItemCount() {
        return 3;
    }

    public class ProfileHolder extends RecyclerView.ViewHolder
    {
        private ImageView ivPpTitleIcon;
        private TextView txtPpTitle;

        public ProfileHolder(View view) {
            super(view);
            ivPpTitleIcon = (ImageView) view.findViewById(R.id.iv_pp_title_icon);
            txtPpTitle = (TextView) view.findViewById(R.id.txt_pp_title);

        }
    }
}
