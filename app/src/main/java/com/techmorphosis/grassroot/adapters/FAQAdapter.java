
package com.techmorphosis.grassroot.adapters;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.techmorphosis.grassroot.R;

import java.util.List;

/**
 * Created by karthik on 16-09-2015.
 */
public class FAQAdapter extends BaseExpandableListAdapter {


    private Context _context;
    private List<String> _listDataHeader; // header titles
    // child data in format of header title, child title
    private List<String>_listDataChild;

    LayoutInflater inflater;

    int lastPosition=-1;
    private Typeface typeFaceBold,typeFaceNormal;

    public FAQAdapter(Context context, List<String> listDataHeader, List<String> listChildData) {
        this._context = context;
        this._listDataHeader = listDataHeader;
        this._listDataChild = listChildData;

       // inflater = (LayoutInflater) this._context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater = LayoutInflater.from(this._context);

/*
        typeFaceBold = Typeface.createFromAsset(_context.getAssets(), "font/OpenSans_CondBold.ttf");
        typeFaceNormal = Typeface.createFromAsset(_context.getAssets(), "font/OpenSans_CondLight.ttf");
*/
    }


    @Override
    public int getGroupCount() {

        return this._listDataHeader.size();
    }

    @Override
    public int getChildrenCount(int groupPosition) {

        return 1;
    }

    @Override
    public Object getGroup(int groupPosition) {

        return this._listDataHeader.get(groupPosition);
    }

    @Override
    public Object getChild(int groupPosition, int childPosition) {

        return this._listDataChild.get(groupPosition);
    }

    @Override
    public long getGroupId(int groupPosition) {

        return groupPosition;
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {

        return childPosition;
    }



    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public void notifyDataSetChanged() {
        super.notifyDataSetChanged();
        lastPosition = -1;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {

        lastPosition = -1;
        ViewHolder viewHolder = null;
        String headerTitle = (String) getGroup(groupPosition);

        if (convertView == null) {
            viewHolder = new ViewHolder();
            convertView = inflater.inflate(R.layout.list_group, null);
            viewHolder.txt_lblListHeader = (TextView) convertView.findViewById(R.id.txt_list_header);
            viewHolder.img_group_icon = (ImageView) convertView.findViewById(R.id.explist_indicator);
            viewHolder.view_line = (View) convertView.findViewById(R.id.view);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        viewHolder.img_group_icon.setImageResource(R.drawable.ic_arrow_down);
        viewHolder.txt_lblListHeader.setText(headerTitle);

        if (isExpanded) {
            viewHolder.img_group_icon.setImageResource(R.drawable.ic_arrow_up);
            viewHolder.txt_lblListHeader.setTypeface(Typeface.DEFAULT_BOLD);
            viewHolder.view_line.setVisibility(View.GONE);


        } else {
            viewHolder.img_group_icon.setImageResource(R.drawable.ic_arrow_down);
            viewHolder.txt_lblListHeader.setTypeface(Typeface.DEFAULT);
            viewHolder.view_line.setVisibility(View.VISIBLE);

        }



        return convertView;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {


        ViewHolder viewHolder = null;
        final String childText = (String) getChild(groupPosition, childPosition);


        if (convertView == null) {
            viewHolder = new ViewHolder();
            convertView = inflater.inflate(R.layout.list_child, null);
            viewHolder.txt_txtListChild = (TextView) convertView.findViewById(R.id.txt_list_child);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }
        viewHolder.txt_txtListChild.setText(childText);


      /*  Animation animation = AnimationUtils.loadAnimation(this._context, (childPosition > lastPosition) ? R.anim.up_from_bottom : R.anim.down_from_top);
        convertView.startAnimation(animation);
     */   lastPosition = childPosition;

        return convertView;

    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return false;
    }


    static class ViewHolder {
        TextView txt_lblListHeader;
        TextView txt_txtListChild;
        ImageView img_group_icon;
        View view_line;
    }
}
