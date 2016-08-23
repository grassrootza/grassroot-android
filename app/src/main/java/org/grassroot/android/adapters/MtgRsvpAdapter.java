package org.grassroot.android.adapters;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.grassroot.android.R;
import org.grassroot.android.interfaces.TaskConstants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by luke on 2016/06/09.
 */
public class MtgRsvpAdapter extends RecyclerView.Adapter<MtgRsvpAdapter.ViewHolder> {

    private static final String TAG = MtgRsvpAdapter.class.getSimpleName();

    private Map<String, String> mapOfResponses;
    private List<String> listOfNames;

    private final Map<String, Integer> responseTextMap;

    public MtgRsvpAdapter() {
        this.mapOfResponses = new HashMap<>();
        this.listOfNames = new ArrayList<>();

        responseTextMap = new HashMap<>();
        responseTextMap.put(TaskConstants.RESPONSE_YES, R.string.vt_rlist_response_yes);
        responseTextMap.put(TaskConstants.RESPONSE_NO, R.string.vt_rlist_response_no);
        responseTextMap.put(TaskConstants.RESPONSE_NONE, R.string.vt_rlist_response_none);
    }

    // todo: make sure it's sorted on server, and sort is transmitting, to avoid processing here
    // todo: figure out how to use UIDs in here to avoid duplication if identical names ...
    public void setMapOfResponses(Map<String, String> responses) {
        this.mapOfResponses = responses;
        this.listOfNames = new ArrayList<>(responses.keySet());
        notifyDataSetChanged();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View item = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_meeting_rsvp, parent, false);
        return new ViewHolder(item);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        final String name = listOfNames.get(position);
        holder.displayName.setText(name);
        final Integer response = responseTextMap.get(mapOfResponses.get(name));
        holder.responseText.setText(response == null ? R.string.vt_rlist_response_none : response);
    }

    @Override
    public int getItemCount() {
        return listOfNames.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        TextView displayName;
        TextView responseText;

        public ViewHolder(View itemView) {
            super(itemView);
            displayName = (TextView) itemView.findViewById(R.id.tv_display_name);
            responseText = (TextView) itemView.findViewById(R.id.tv_response);
        }
    }

}
