package org.grassroot.android.adapters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import org.grassroot.android.R;
import org.grassroot.android.models.GroupJoinRequest;
import org.grassroot.android.utils.RealmUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import rx.Observable;
import rx.functions.Action1;

/**
 * Created by luke on 2016/07/14.
 */
public class JoinRequestAdapter extends RecyclerView.Adapter<JoinRequestAdapter.JoinRequestViewHolder> {

    private static final String TAG = JoinRequestAdapter.class.getSimpleName();

    private final Context mContext;
    private ArrayList<GroupJoinRequest> openRequests;
    private JoinRequestClickListener listener;
    private Observable<List<GroupJoinRequest>> dataLoader;

    private static final SimpleDateFormat sentFormat = new SimpleDateFormat("EEE, d MMM", Locale.getDefault());

    public interface JoinRequestClickListener {
        void backgroundCallComplete();
        void positiveClicked(GroupJoinRequest request, int position);
        void negativeClicked(GroupJoinRequest request, int position);
    }

    @SuppressWarnings("unchecked")
    public JoinRequestAdapter(Context context, final String type, @NonNull final JoinRequestClickListener listener) {
        mContext = context;
        this.listener = listener; // todo : watch for memory leaks

        Map<String, Object> requestMap = new HashMap<>();
        if (!TextUtils.isEmpty(type)) {
            requestMap.put("joinReqType", type);
        }

        dataLoader = RealmUtils.loadListFromDB(GroupJoinRequest.class, requestMap);
        openRequests = new ArrayList<>();
        dataLoader.subscribe(new Action1<List<GroupJoinRequest>>() {
            @Override
            public void call(List<GroupJoinRequest> requests) {
                openRequests.addAll(requests);
                notifyDataSetChanged();
                listener.backgroundCallComplete();
            }
        });
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

        if (!TextUtils.isEmpty(request.getRequestDescription())) {
            holder.requestDescription.setText(mContext.getString(R.string.jreq_desc_head, request.getRequestDescription()));
        } else {
            holder.requestDescription.setText(mContext.getString(R.string.jreq_desc_head, mContext.getString(R.string.jreq_no_desc_rec)));
        }

        if (GroupJoinRequest.REC_REQUEST.equals(request.getJoinReqType())) {
            setUpViewRequestReceived(holder, request);
        } else {
            setUpViewRequestSent(holder, request);
        }

        final int fixedPosition = holder.getAdapterPosition();
        holder.approve.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.positiveClicked(request, fixedPosition);
            }
        });

        holder.deny.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.negativeClicked(request, fixedPosition);
            }
        });
    }

    private void setUpViewRequestReceived(JoinRequestViewHolder holder, GroupJoinRequest request) {
        holder.requestorName.setText(String.format(mContext.getString(R.string.jreq_req_name), request.getRequestorName()));
        holder.requestorPhone.setText(String.format(mContext.getString(R.string.jreq_req_number), request.getRequestorNumber()));
        holder.approve.setText(R.string.jreq_approve);
        holder.deny.setText(R.string.jreq_deny);
    }

    private void setUpViewRequestSent(JoinRequestViewHolder holder, GroupJoinRequest request) {
        holder.requestorName.setText(String.format(mContext.getString(R.string.jreq_sent_date), sentFormat.format(request.getCreatedDateTime())));
        holder.requestorPhone.setVisibility(View.GONE);
        holder.approve.setText(R.string.jreq_remind);
        holder.deny.setText(R.string.alert_cancel);
    }

    @Override
    public int getItemCount() {
        return openRequests.size();
    }

    public void refreshList() {
        dataLoader.subscribe(new Action1<List<GroupJoinRequest>>() {
            @Override
            public void call(List<GroupJoinRequest> requests) {
                openRequests.clear();
                openRequests.addAll(requests);
                notifyDataSetChanged();
                listener.backgroundCallComplete();
            }
        });
    }

    public void clearList() {
        openRequests.clear();
        notifyDataSetChanged();
    }

    public void clearRequest(int position) {
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
