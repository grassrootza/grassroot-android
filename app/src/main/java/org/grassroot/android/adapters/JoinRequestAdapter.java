package org.grassroot.android.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import org.grassroot.android.R;
import org.grassroot.android.models.GroupJoinRequest;
import org.grassroot.android.services.GroupService;

import java.util.ArrayList;

/**
 * Created by luke on 2016/07/14.
 */
public class JoinRequestAdapter extends RecyclerView.Adapter<JoinRequestAdapter.JoinRequestViewHolder> {

    private static final String TAG = JoinRequestAdapter.class.getSimpleName();

    private final Context mContext;
    private ArrayList<GroupJoinRequest> openRequests;
    private JoinRequestClickListener listener;

    public interface JoinRequestClickListener {
        void requestApproved(GroupJoinRequest request, int position);
        void requestDenied(GroupJoinRequest request, int position);
    }

    public JoinRequestAdapter(Context context, JoinRequestClickListener listener) {
        mContext = context;
        openRequests = new ArrayList(GroupService.getInstance().loadRequestsFromDB());
        if (listener == null) {
            throw new UnsupportedOperationException("Error! Join request adapater requires click callbacks");
        } else {
            this.listener = listener;
        }
    }

    @Override
    public JoinRequestViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_join_request, parent, false);
        return new JoinRequestViewHolder(v);
    }

    @Override
    public void onBindViewHolder(JoinRequestViewHolder holder, int position) {
        final GroupJoinRequest request = openRequests.get(position);

        holder.groupName.setText(String.format(mContext.getString(R.string.jreq_group_name), request.getGroupName()));
        holder.requestorName.setText(String.format(mContext.getString(R.string.jreq_req_name), request.getRequestorName()));
        holder.requestorPhone.setText(String.format(mContext.getString(R.string.jreq_req_number), request.getRequestorNumber()));
        holder.requestDescription.setText(request.getRequestDescription());

        final int fixedPosition = holder.getAdapterPosition();
        holder.approve.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.requestApproved(request, fixedPosition);
            }
        });

        holder.deny.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.requestDenied(request, fixedPosition);
            }
        });
    }

    @Override
    public int getItemCount() {
        return openRequests.size();
    }

    public void refreshList() {
        openRequests.clear();
        openRequests.addAll(GroupService.getInstance().loadRequestsFromDB());
        notifyDataSetChanged();
    }

    public void clearList() {
        openRequests.clear();
        notifyDataSetChanged();
    }

    public void clearRequest(int position) {
        Log.e(TAG, "removing request at position ...");
        openRequests.remove(position);
        notifyItemChanged(position);
    }

    public void insertRequest(GroupJoinRequest request) {
        openRequests.add(0, request);
        notifyItemChanged(0);
    }

    public static class JoinRequestViewHolder extends RecyclerView.ViewHolder {

        TextView groupName;
        TextView requestorName;
        TextView requestorPhone;
        TextView requestDescription;

        Button approve;
        Button deny;

        public JoinRequestViewHolder(View view) {
            super(view);

            groupName = (TextView) view.findViewById(R.id.jreq_group_name);
            requestorName = (TextView) view.findViewById(R.id.jreq_requestor_name);
            requestorPhone = (TextView) view.findViewById(R.id.jreq_requestor_number);
            requestDescription = (TextView) view.findViewById(R.id.jreq_request_description);
            approve = (Button) view.findViewById(R.id.jreq_btn_approve);
            deny = (Button) view.findViewById(R.id.jreq_btn_deny);

        }

    }

}
