package org.grassroot.android.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.grassroot.android.R;
import org.grassroot.android.models.GroupSearchModel;


import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by admin on 26-Mar-16.
 */
public class JoinRequestAdapter extends RecyclerView.Adapter<JoinRequestAdapter.JoinRequestViewHolder> {

    private final Context mcontext;
    private final List<GroupSearchModel> data;
    LayoutInflater inflater;

    public JoinRequestAdapter(Context context, List<GroupSearchModel> joinrequestList) {
        this.mcontext = context;
        this.data = joinrequestList;
        inflater = LayoutInflater.from(context);
    }

    public void addResults(List<GroupSearchModel> groups) {
        this.data.addAll(groups);
        this.notifyItemRangeInserted(0, groups.size() - 1);
    }

    @Override
    public JoinRequestViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View v = inflater.inflate(R.layout.listview_row_joinrequest, parent, false);
        JoinRequestViewHolder holder = new JoinRequestViewHolder(v);
        return holder;
    }

    @Override
    public void onBindViewHolder(JoinRequestViewHolder holder, int position) {
        GroupSearchModel model = data.get(position);
        holder.txtGroupname.setText(model.getGroupCreator());
        holder.txtGroupownername.setText(model.getGroupCreator());
        holder.txtGroupdesc.setText(model.getDescription());

    }


    @Override
    public int getItemCount() {
        return data.size();
    }

    public void clearApplications() {
        int size = this.data.size();
        if (size > 0) {
            for (int i = 0; i < size; i++) {
                data.remove(0);
            }

            this.notifyItemRangeRemoved(0, size);
        }
    }

    public static class JoinRequestViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.txt_groupname)
        TextView txtGroupname;
        @BindView(R.id.txt_groupownername)
        TextView txtGroupownername;
        @BindView(R.id.txt_groupdesc)
        TextView txtGroupdesc;

        public JoinRequestViewHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);

        }
    }


}
