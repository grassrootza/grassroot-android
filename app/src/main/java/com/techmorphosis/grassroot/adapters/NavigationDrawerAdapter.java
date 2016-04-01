package com.techmorphosis.grassroot.adapters;

/**
 * Created by Ravi on 29/07/15.
 */

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.techmorphosis.grassroot.R;
import com.techmorphosis.grassroot.models.NavDrawerItem;

import java.util.ArrayList;


public class NavigationDrawerAdapter extends RecyclerView.Adapter<NavigationDrawerAdapter.MyViewHolder>
{

         ArrayList<NavDrawerItem> data;
    private  Context mContext;
    private  LayoutInflater inflater;

    public  NavigationDrawerAdapter(Context context,ArrayList<NavDrawerItem> data)
    {
        this.data=data;
        this.mContext=context;
        this.inflater=LayoutInflater.from(context);
    }


    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view=inflater.inflate(R.layout.listview_row_navigationdrawer,parent,false);
        MyViewHolder holder= new MyViewHolder(view);

        return holder;
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {

        NavDrawerItem myDrawerModel = data.get(position);

        holder.title.setText(myDrawerModel.getTitle());
        holder.titleicon.setBackgroundResource(myDrawerModel.getIcon());


        if (myDrawerModel.isChecked())
        {
            holder.title.setTextColor(mContext.getResources().getColor(R.color.buttoncolor));
            holder.titleicon.setBackgroundResource(myDrawerModel.getChangeicon());
        }
        else
        {
            holder.title.setTextColor(mContext.getResources().getColor(R.color.black));
        }

    }

    @Override
    public int getItemCount() {
        return data.size();
    }


    public static  class MyViewHolder extends  RecyclerView.ViewHolder
    {
        TextView title;
        ImageView titleicon;

        public MyViewHolder(View itemView) {
            super(itemView);
            title = (TextView) itemView.findViewById(R.id.txt_title);
            titleicon  = (ImageView) itemView.findViewById(R.id.iv_titleicon);

        }
    }
}