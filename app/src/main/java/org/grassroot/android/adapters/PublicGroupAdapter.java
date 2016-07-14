package org.grassroot.android.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.grassroot.android.R;
import org.grassroot.android.models.PublicGroupModel;


import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 */
public class PublicGroupAdapter extends RecyclerView.Adapter<PublicGroupAdapter.JoinRequestViewHolder> {

    private final List<PublicGroupModel> data;
    LayoutInflater inflater;

    public PublicGroupAdapter(Context context, List<PublicGroupModel> joinrequestList) {
        this.data = joinrequestList;
        inflater = LayoutInflater.from(context);
    }

    public void addResults(List<PublicGroupModel> groups) {
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
        PublicGroupModel model = data.get(position);
        holder.txtGroupname.setText(model.getGroupName());
        holder.txtGroupownername.setText(model.getGroupCreator());
        if (!TextUtils.isEmpty(model.getDescription())) {
            holder.txtGroupdesc.setText(model.getDescription());
        } else {
            holder.txtGroupdesc.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    public PublicGroupModel getPublicGroup(int position) {
        if (data == null || data.size() < position) {
            throw new UnsupportedOperationException("Error! Asked for public group not in search results");
        } else {
            return data.get(position);
        }
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
