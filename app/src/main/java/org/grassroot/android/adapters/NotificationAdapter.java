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
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
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

    private boolean onlyUnread;
    private boolean isSearching;
    private String lowerCaseQuery;

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

    public void addNotifications(List<TaskNotification> notificationsToAdd) {
        // note : come back and optimize this in next phase
        if (onlyUnread || isSearching) {
            storedNotifications.addAll(notificationsToAdd);
            final int size = notificationsToAdd.size();
            for (int i = 0; i < size; i++) {
                if (onlyUnread && !notificationsToAdd.get(i).isViewedAndroid()) {
                    notifications.add(notificationsToAdd.get(i));
                }
                if (isSearching && notificationsToAdd.get(i).containsText(lowerCaseQuery)) {
                    notifications.add(notificationsToAdd.get(i));
                }
            }
        } else {
            this.notifications.addAll(notificationsToAdd);
        }
        notifyDataSetChanged();
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

    public Observable<Boolean> filterByUnviewed() {
        return Observable.create(new Observable.OnSubscribe<Boolean>() {
            @Override
            public void call(Subscriber<? super Boolean> subscriber) {
                long startTime = SystemClock.currentThreadTimeMillis();
                if (storedNotifications == null || storedNotifications.isEmpty()) {
                    storedNotifications = new ArrayList<>(notifications);
                }

                onlyUnread = true;

                // note : can also do this via Rx filters, but then thread choreography gets a bit complex
                // and this is marginally faster, so sacrificing a little elegance
                final int size = storedNotifications.size();
                notifications.clear();
                for (int i =0; i < size; i++) {
                    if (storedNotifications.get(i).isViewedAndroid()) {
                        notifications.add(storedNotifications.get(i));
                    }
                }

                subscriber.onNext(true);

                Log.e(TAG, "number of notifications after filter .... " + notifications.size() + " " +
                    "and it took ... " + (SystemClock.currentThreadTimeMillis() - startTime));
                subscriber.onCompleted();
            }
        }).subscribeOn(Schedulers.computation()).observeOn(AndroidSchedulers.mainThread());
    }

    public Observable<Boolean> searchText(final String queryText) {
        return Observable.create(new Observable.OnSubscribe<Boolean>() {
            @Override
            public void call(Subscriber<? super Boolean> subscriber) {
                if (storedNotifications == null || storedNotifications.isEmpty()) {
                    storedNotifications = new ArrayList<>(notifications);
                } else {
                    notifications = new ArrayList<>(storedNotifications);
                }

                onlyUnread = true;
                final String lCase = queryText.toLowerCase();
                final int size = storedNotifications.size();

                // note : as above re Rx filters and threads
                for (int i = 0; i < size; i++) {
                    if (!storedNotifications.get(i).containsText(lCase)) {
                        notifications.remove(storedNotifications.get(i));
                    }
                }

                subscriber.onNext(true);
                lowerCaseQuery = lCase; // stores it for future adds
                subscriber.onCompleted();
            }
        }).subscribeOn(Schedulers.computation()).observeOn(AndroidSchedulers.mainThread());
    }

    public void resetToStored() {
        onlyUnread = false;
        isSearching = false;
        lowerCaseQuery = "";
        notifications = new ArrayList<>(storedNotifications);
        notifyDataSetChanged();
    }


    public List<TaskNotification> getNotifications() {
        return notifications;
    }
}
