package org.grassroot.android.adapters;


import android.app.Activity;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;


import org.grassroot.android.R;
import org.grassroot.android.interfaces.TaskConstants;
import org.grassroot.android.models.Message;
import org.grassroot.android.models.RealmString;
import org.grassroot.android.models.TaskModel;
import org.grassroot.android.services.TaskService;
import org.grassroot.android.utils.Constant;
import org.grassroot.android.utils.RealmUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import butterknife.BindView;
import butterknife.ButterKnife;
import hani.momanii.supernova_emoji_library.Helper.EmojiconTextView;
import io.realm.RealmList;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;


/**
 * Created by paballo on 2016/08/30.
 */
public class GroupChatAdapter extends RecyclerView.Adapter<GroupChatAdapter.GCViewHolder> {

    private static final String TAG = GroupChatAdapter.class.getCanonicalName();
    private SimpleDateFormat dateFormatter = new SimpleDateFormat("HH:mm");
    private Activity activity;

    private List<Message> messages;
    public final static int SELF = 100;
    public final static int OTHER = 200;
    public final static int SERVER = 300;

    public GroupChatAdapter(List<Message> messages, Activity activity) {
        this.activity = activity;
        this.messages = new ArrayList<>(messages);
        notifyDataSetChanged();
    }

    @Override
    public GCViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View message;
        if (viewType == SELF) {
            message = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.row_chat_self, parent, false);
        } else if (viewType == OTHER) {
            message = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.row_chat_other, parent, false);
        } else {
            message = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.row_chat_server, parent, false);
        }
        return new GCViewHolder(message);
    }


    @SuppressWarnings("deprecation")
    @Override
    public void onBindViewHolder(GCViewHolder holder, int position) {
        final Message message = messages.get(position);
        holder.message.setText(message.getText());
        String time = DateUtils.isToday(message.getTime().getTime()) ? dateFormatter.format(message.getTime()) : (String)
               DateUtils.getRelativeDateTimeString(activity, message.getTime().getTime(),
                       System.currentTimeMillis(), DateUtils.SECOND_IN_MILLIS, DateUtils.FORMAT_ABBREV_RELATIVE);
        holder.timestamp.setText(time);


        if (getItemViewType(position) == OTHER || getItemViewType(position) == SERVER) {
            holder.user.setText(message.getDisplayName());
            holder.user.setVisibility(View.VISIBLE);
            if(getItemViewType(position) == SERVER) showButtons(holder, false);
            if (message.getTokens() != null && message.getTokens().size() > 0 && getItemViewType(position) == SERVER) {
                showButtons(holder, true);
                holder.bt_no.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        RealmUtils.deleteMessageFromDb(message.getUid()).subscribe(new Action1<String>() {
                            @Override
                            public void call(String s) {
                                reloadFromdb(message.getGroupUid());
                            }
                        });
                    }
                });
                holder.bt_yes.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        String title = message.getTokens().get(0).getString();
                        String location = message.getTokens().get(2).getString();
                        String date = message.getTokens().get(1).getString();
                        Calendar calendar = Calendar.getInstance();
                        calendar.add(Calendar.DAY_OF_YEAR, 1);
                        Date time = Calendar.getInstance().getTime();
                        TaskModel taskModel = generateTaskObject(message.getGroupUid(), title, time, location);
                        taskModel.setDeadlineISO(date);
                        TaskService.getInstance().sendTaskToServer(taskModel, AndroidSchedulers.mainThread()).subscribe(new Subscriber<TaskModel>() {
                            @Override
                            public void onCompleted() {}
                            public void onError(Throwable e) {
                                Log.e(TAG, e.toString());
                                Toast.makeText(activity, "Unfortunately something went wrong and were unable to fulfil the request.", Toast.LENGTH_LONG).show();
                            }
                            @Override
                            public void onNext(TaskModel taskModel) {
                                Toast.makeText(activity, "Success! Meeting was called.", Toast.LENGTH_LONG).show();
                                RealmUtils.deleteMessageFromDb(message.getUid()).subscribe(new Action1<String>() {
                                    @Override
                                    public void call(String s) {
                                        reloadFromdb(message.getGroupUid());
                                    }
                                });
                            }
                        });

                    }
                });
            }
        }
        if (getItemViewType(position) == SELF && message.isDelivered()) {
            holder.timestamp.setText(activity.getString(R.string.chat_message_sent).concat(time));
        } else if (getItemViewType(position) == SELF && (!message.isDelivered() && message.exceedsMaximumSendingAttempts())) {
            holder.timestamp.setText(activity.getString(R.string.chat_message_not_sent).concat(time));
        }

    }

    public void setGroupList(List<Message> messages) {
        this.messages = new ArrayList<>(messages);
        this.notifyDataSetChanged();
    }

    public void reloadFromdb(String groupUid) {
        RealmUtils.loadMessagesFromDb(groupUid).subscribe(new Action1<List<Message>>() {
            @Override
            public void call(List<Message> messages) {
                setGroupList(messages);
            }
        });
    }


    public void addMessage(Message message){
        this.messages.add(message);
        this.notifyItemInserted(getItemCount()-1);
    }

    public void deleteAll(){
        this.messages.clear();
        this.notifyDataSetChanged();
    }

    public void updateMessage(Message message){
        for(int i=0; i<messages.size(); i++){
            if(messages.get(i).getUid().equals(message.getUid())){
                messages.set(i,message);
                this.notifyItemChanged(i);
            }
             }
    }

    public void deleteOne(String messageUid){
        for(int i=0; i<messages.size(); i++){
            if(messages.get(i).getUid().equals(messageUid)){
                messages.remove(i);
                this.notifyItemRemoved(i);
            }
        }
    }
    @Override
    public int getItemCount() {
        return messages.size();
    }

    @Override
    public int getItemViewType(int position) {
        Message message = messages.get(position);
        if (message != null && message.getPhoneNumber() == null) {
            return SERVER;
        }
        if (message != null && !message.getPhoneNumber().equals(RealmUtils.loadPreferencesFromDB().
                getMobileNumber())) {
            return OTHER;
        }
        return SELF;
    }


    public List<Message> getMessages() {
        return messages;
    }


    private TaskModel generateTaskObject(String groupUid, String title, Date time, String venue) {

        TaskModel model = new TaskModel();
        model.setTitle(title);
        model.setDescription(title);
        model.setCreatedByUserName(RealmUtils.loadPreferencesFromDB().getUserName());
        model.setDeadlineDate(time);
        model.setDeadlineISO(Constant.isoDateTimeSDF.format(time));
        model.setLocation(venue);
        model.setParentUid(groupUid);
        model.setTaskUid(UUID.randomUUID().toString());
        model.setType(TaskConstants.MEETING);
        model.setParentLocal(false);
        model.setLocal(true);
        model.setMinutes(0);
        model.setCanEdit(true);
        model.setCanAction(true);
        model.setReply(TaskConstants.TODO_PENDING);
        model.setHasResponded(false);
        model.setWholeGroupAssigned(true);
        model.setMemberUIDS(new RealmList<RealmString>());

        return model;
    }


    private void showButtons(GCViewHolder holder, boolean show) {
        if (show) {
            holder.bt_yes.setVisibility(View.VISIBLE);
            holder.bt_no.setVisibility(View.VISIBLE);
        } else {
            holder.bt_yes.setVisibility(View.GONE);
            holder.bt_no.setVisibility(View.GONE);
        }

    }

    public class GCViewHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.timestamp)
        TextView timestamp;

        @BindView(R.id.user)
        TextView user;

        @BindView(R.id.text)
        EmojiconTextView message;

        @Nullable
        @BindView(R.id.bt_yes)
        Button bt_yes;

        @Nullable
        @BindView(R.id.bt_no)
        Button bt_no;

        public GCViewHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
        }

    }


}