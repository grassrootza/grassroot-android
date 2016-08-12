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


import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 */
public class PublicGroupAdapter extends RecyclerView.Adapter<PublicGroupAdapter.PublicGroupViewHolder> {

    private List<PublicGroupModel> data;
    LayoutInflater inflater;

    public PublicGroupAdapter(Context context, List<PublicGroupModel> joinrequestList) {
        this.data = joinrequestList;
        inflater = LayoutInflater.from(context);
    }

    public void addResults(List<PublicGroupModel> groups) {
        this.data.addAll(groups);
        this.notifyItemRangeInserted(0, groups.size() - 1);
    }

    public void resetResults(List<PublicGroupModel> groups) {
        this.data = (groups != null) ? groups : new ArrayList<PublicGroupModel>();
        notifyDataSetChanged();
    }

    @Override
    public PublicGroupViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = inflater.inflate(R.layout.row_public_group, parent, false);
        PublicGroupViewHolder holder = new PublicGroupViewHolder(v);
        return holder;
    }

    @Override
    public void onBindViewHolder(PublicGroupViewHolder holder, int position) {
        PublicGroupModel model = data.get(position);

        final String groupName = String.format(inflater.getContext().getString(R.string.pgroup_name_format),
            model.getGroupName());
        holder.txtGroupname.setText(groupName);

        final String groupCreator = String.format(inflater.getContext().getString(R.string.pgroup_org_format),
            model.getGroupCreator());
        holder.txtGroupownername.setText(groupCreator);

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

    public static class PublicGroupViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.txt_groupname)
        TextView txtGroupname;
        @BindView(R.id.txt_groupownername)
        TextView txtGroupownername;
        @BindView(R.id.txt_groupdesc)
        TextView txtGroupdesc;

        public PublicGroupViewHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
        }
    }


}
