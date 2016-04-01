package com.techmorphosis.grassroot.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.techmorphosis.grassroot.R;
import com.techmorphosis.grassroot.models.Join_RequestModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by admin on 26-Mar-16.
 */
public class JoinRequestAdapter extends RecyclerView.Adapter<JoinRequestAdapter.JoinRequestViewHolder> {

    private final Context mcontext;
    private final ArrayList<Join_RequestModel> data;
    LayoutInflater inflater;

    public JoinRequestAdapter(Context context, ArrayList<Join_RequestModel> joinrequestList)
    {
        this.mcontext=context;
        this.data=joinrequestList;
        inflater=LayoutInflater.from(context);
    }

    public void addApplications(ArrayList<Join_RequestModel> applications) {
        this.data.addAll(applications);
        this.notifyItemRangeInserted(0, applications.size() - 1);
    }

    @Override
    public JoinRequestViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View v=inflater.inflate(R.layout.listview_row_joinrequest,parent,false);
        JoinRequestViewHolder holder = new JoinRequestViewHolder(v);
        return holder;
    }

    @Override
    public void onBindViewHolder(JoinRequestViewHolder holder, int position)
    {
        Join_RequestModel model= data.get(position);
        holder.txtGroupname.setText(model.getGroupname());
        holder.txtGroupownername.setText(model.getGroupCreator());
        holder.txtGroupdesc.setText(model.getGroup_describe());

    }


    @Override
    public int getItemCount() {
        return data.size();
    }

    public static class JoinRequestViewHolder extends  RecyclerView.ViewHolder
    {

         TextView txtGroupname;
         TextView txtGroupownername;
         TextView txtGroupdesc;

        public JoinRequestViewHolder(View view) {
            super(view);
            txtGroupname = (TextView) view.findViewById(R.id.txt_groupname);
            txtGroupownername = (TextView) view.findViewById(R.id.txt_groupownername);
            txtGroupdesc = (TextView) view.findViewById(R.id.txt_groupdesc);
        }
    }


}
