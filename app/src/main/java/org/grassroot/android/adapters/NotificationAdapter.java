package org.grassroot.android.adapters;

import android.graphics.Typeface;
import android.os.SystemClock;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.grassroot.android.R;
import org.grassroot.android.interfaces.TaskConstants;
import org.grassroot.android.models.TaskNotification;
import org.grassroot.android.services.ApplicationLoader;
import org.grassroot.android.utils.ImageUtils;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

/**
 * Created by luke.
 */
public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {

    private static final String TAG = NotificationAdapter.class.getSimpleName();

    private List<TaskNotification> notifications = new ArrayList<>();
    private List<TaskNotification> storedNotifications = new ArrayList<>();

    private final String titleFormatEvent;
    private final String titleFormatTodo;

    public NotificationAdapter() {
        notifications = new ArrayList<>();
        storedNotifications = new ArrayList<>();
        titleFormatEvent = ApplicationLoader.applicationContext.getString(R.string.notification_title_format_event);
        titleFormatTodo = ApplicationLoader.applicationContext.getString(R.string.notification_title_format_todo);
    }

    public void setToNotifications(List<TaskNotification> notificationList) {
        this.notifications.clear();
        this.notifications.addAll(notificationList);
        this.notifyDataSetChanged();
    }

    public void addNotifications(List<TaskNotification> notifications) {
        this.notifications.addAll(notifications);
        this.notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return notifications.size();
    }

    public TaskNotification getItem(final int position) {
        return notifications.get(position);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_notification, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        TaskNotification notification = notifications.get(position);

        holder.title.setText(assembleTitle(notification));
        holder.message.setText(notification.getMessage());

        if (notification.isViewedAndroid()) {
            setIcon(notification, holder.icon);
            holder.title.setTypeface(null, Typeface.NORMAL);
        } else {
            holder.icon.setImageResource(R.drawable.ic_excl_green);
            holder.title.setTypeface(null, Typeface.BOLD);
        }
    }

    // notification.getTitle holds group name (if have different in future, will need to add checks)
    private String assembleTitle(TaskNotification notification) {
        switch (notification.getEntityType()) {
            case TaskConstants.MEETING:
                return String.format(titleFormatEvent, "Meeting", notification.getTitle());
            case TaskConstants.VOTE:
                return String.format(titleFormatEvent, "Vote", notification.getTitle());
            case TaskConstants.TODO:
                return String.format(titleFormatTodo, "Todo", notification.getTitle());
            default:
                return notification.getTitle();
        }
    }

    private void setIcon(TaskNotification notification, ImageView icon) {
        int defaultImage = ImageUtils.convertDefaultImageTypeToResource(notification.getDefaultImage());
        if (TextUtils.isEmpty(notification.getImageUrl())) {
            icon.setImageResource(defaultImage);
        } else {
            try {
                // monitor performance of this too, likely want to pre-fetch & load
                ImageUtils.setAvatarImage(icon, notification.getImageUrl(), defaultImage);
            } catch (OutOfMemoryError e) {
                icon.setImageResource(defaultImage);
            }
        }
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.main_view) ViewGroup mainView;
        @BindView(R.id.notification_text_title) TextView title;
        @BindView(R.id.notification_text_message) TextView message;
        @BindView(R.id.notification_icon) ImageView icon;

        public ViewHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
        }
    }

    public void filterByUnviewed() {
        long startTime = SystemClock.currentThreadTimeMillis();
        if (storedNotifications == null || storedNotifications.isEmpty()) {
            storedNotifications = new ArrayList<>(notifications);
        }
        Observable.from(storedNotifications)
            .filter(new Func1<TaskNotification, Boolean>() {
                @Override
                public Boolean call(TaskNotification notification) {
                    return notification.isViewedAndroid();
                }
            }).subscribe(new Action1<TaskNotification>() {
            @Override
            public void call(TaskNotification notification) {
                notifications.remove(notification);
            }
        }, new Action1<Throwable>() {
            @Override
            public void call(Throwable throwable) {
                throwable.printStackTrace();
            }
        });
        Log.e(TAG, "number of notifications after filter .... " + notifications.size() + " " +
            "and it took ... " + (SystemClock.currentThreadTimeMillis() - startTime));
        notifyDataSetChanged();
    }

    public void searchText(String queryText) {
        if (storedNotifications == null || storedNotifications.isEmpty()) {
                storedNotifications = new ArrayList<>(notifications);
        } else {
            notifications = new ArrayList<>(storedNotifications);
        }

        final String lCase = queryText.toLowerCase();

        Observable.from(storedNotifications)
            .subscribeOn(Schedulers.computation())
            .observeOn(Schedulers.immediate())
            .filter(new Func1<TaskNotification, Boolean>() {
                @Override
                public Boolean call(TaskNotification notification) {
                    return !notification.containsText(lCase);
                }
            })
            .subscribe(new Action1<TaskNotification>() {
                @Override
                public void call(TaskNotification notification) {
                    notifications.remove(notification);
                }
            }, new Action1<Throwable>() {
                @Override
                public void call(Throwable throwable) {
                    throwable.printStackTrace();
                }
            });

        notifyDataSetChanged();
    }

    public void resetToStored() {
        notifications = new ArrayList<>(storedNotifications);
        notifyDataSetChanged();
    }


    public List<TaskNotification> getNotifications() {
        return notifications;
    }
}
