package org.grassroot.android.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.grassroot.android.R;
import org.grassroot.android.models.PublicGroupModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 */
public class PublicGroupAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final String TAG = PublicGroupAdapter.class.getSimpleName();

    private List<PublicGroupModel> publicGroupModels;
    private Map<Integer, Integer> positionHeaderMap;
    LayoutInflater inflater;

    public PublicGroupAdapter(Context context, List<PublicGroupModel> publicGroups, Map<Integer, Integer> sectionHeadings) {
        this.publicGroupModels = publicGroups;
        this.positionHeaderMap = sectionHeadings;
        inflater = LayoutInflater.from(context);
    }

    public void resetResults(List<PublicGroupModel> groups, Map<Integer, Integer> sectionHeaders) {
        this.publicGroupModels = (groups != null) ? groups : new ArrayList<PublicGroupModel>();
        this.positionHeaderMap = sectionHeaders;
        notifyDataSetChanged();
    }

    public void toggleRequestSent(int position, boolean requestSent) {
        publicGroupModels.get(removeHeadersFromPosition(position)).setHasOpenRequest(requestSent);
        notifyItemChanged(position);
    }

    @Override
    public int getItemViewType(int position) {
        // type 0 is header, all other types above that (only one other at present)
        return (positionHeaderMap.containsKey(position)) ? 0 : 1;
    }

    private int removeHeadersFromPosition(int position) {
        // keep an eye on this if start adding several sections / headers
        int adjustedPosition = position;
        for (Integer n : positionHeaderMap.keySet()) {
            if (n < position) {
                adjustedPosition--;
            }
        }
        return adjustedPosition;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v;
        RecyclerView.ViewHolder holder;

        Log.d(TAG, "creating view holder ... of type = " + viewType);
        switch (viewType) {
            case 0 :
                v = inflater.inflate(R.layout.row_public_group_sec_header, parent, false);
                holder = new PublicGroupSectionHeader(v);
                break;
            default:
                v = inflater.inflate(R.layout.row_public_group, parent, false);
                holder = new PublicGroupViewHolder(v);
                break;
        }

        return holder;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        switch (getItemViewType(position)) {
            case 0:
                bindPublicGroupHeader((PublicGroupSectionHeader) holder, position);
                break;
            default:
                int adjustedPosition = removeHeadersFromPosition(position);
                bindPublicGroup((PublicGroupViewHolder) holder, adjustedPosition);
        }
    }

    private void bindPublicGroupHeader(PublicGroupSectionHeader holder, int position) {
        Integer headerText = positionHeaderMap.get(position);
        holder.sectionHeaderText.setText(headerText == null ? R.string.find_group_header_default
            : headerText);
    }

    private void bindPublicGroup(PublicGroupViewHolder holder, int position) {

        PublicGroupModel model = publicGroupModels.get(position);

        holder.sentReqIcon.setVisibility(model.isHasOpenRequest() ? View.VISIBLE : View.GONE);

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
        return (publicGroupModels.size() + positionHeaderMap.size());
    }

    public PublicGroupModel getPublicGroup(int position, boolean adjustPositionForHeader) {
        int listPosition = adjustPositionForHeader ? removeHeadersFromPosition(position) : position;
        if (publicGroupModels == null || publicGroupModels.size() < listPosition) {
            throw new UnsupportedOperationException("Error! Asked for public group not in search results");
        } else {
            return publicGroupModels.get(listPosition);
        }
    }

    public static class PublicGroupViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.txt_groupname) TextView txtGroupname;
        @BindView(R.id.txt_groupownername) TextView txtGroupownername;
        @BindView(R.id.txt_groupdesc) TextView txtGroupdesc;
        @BindView(R.id.public_group_req_sent) ImageView sentReqIcon;

        public PublicGroupViewHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
        }
    }

    public static class PublicGroupSectionHeader extends RecyclerView.ViewHolder {
        TextView sectionHeaderText;

        public PublicGroupSectionHeader(View view) {
            super(view);
            sectionHeaderText = (TextView) view.findViewById(R.id.find_group_section_header_text);
        }
    }
}