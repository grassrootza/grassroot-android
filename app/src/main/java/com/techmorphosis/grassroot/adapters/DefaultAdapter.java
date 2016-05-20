package com.techmorphosis.grassroot.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.RadioButton;
import android.widget.TextView;

import com.techmorphosis.grassroot.R;
import com.techmorphosis.grassroot.models.NavDrawerItem;
import com.techmorphosis.grassroot.utils.DialogUtils;
import com.techmorphosis.grassroot.utils.MDTintHelper;

import java.util.ArrayList;

/**
 * Created by RAVI on 08-05-2016.
 */
public class DefaultAdapter extends BaseAdapter {
    public  View view;
    ArrayList<NavDrawerItem> modelArrayList;
    private ViewHolder holder;
    Context context;
    public DefaultAdapter(ArrayList<NavDrawerItem> arrayList,Context contextl) {
        this.context=contextl;
        this.modelArrayList=arrayList;
    }

    @Override
    public int getCount() {
        return modelArrayList.size();
    }

    @Override
    public Object getItem(int position) {
        return modelArrayList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.md_listitem_singlechoice, parent, false);
            holder = new ViewHolder();
            holder.radio = (RadioButton) convertView.findViewById(R.id.control);

            holder.tv = (TextView) convertView.findViewById(R.id.radiotitle);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        NavDrawerItem model= modelArrayList.get(position);
        boolean selected =model.isChecked();


        if (selected) {
            MDTintHelper.setTint(holder.radio, DialogUtils.getColor(context, R.color.primaryColor));
        }

        holder.radio.setChecked(selected);

        holder.tv.setText(model.getTitle());


        return  convertView;

    }
    class ViewHolder {
         RadioButton radio;
        TextView tv;


    }



}
