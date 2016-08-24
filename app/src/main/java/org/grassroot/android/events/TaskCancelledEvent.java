package org.grassroot.android.events;

/**
 * Created by paballo on 2016/06/22.
 */
public class TaskCancelledEvent {

    private final String taskUid;

    public TaskCancelledEvent(String taskUid) {
        this.taskUid = taskUid;
    }

    public String getTaskUid() {
        return taskUid;
    }
}
