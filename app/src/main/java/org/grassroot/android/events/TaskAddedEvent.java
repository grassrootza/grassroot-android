package org.grassroot.android.events;

import android.support.annotation.Nullable;

import org.grassroot.android.models.TaskModel;

/**
 * Created by paballo on 2016/05/31.
 */
public class TaskAddedEvent {

    private TaskModel taskCreated;
    private String message;

    private TaskAddedEvent() {
        this.taskCreated = null;
    } // making private to ensure it's never called empty

    public TaskAddedEvent(TaskModel taskModel, @Nullable String message){
        this.taskCreated = taskModel;
        this.message = message;
    }

    public TaskModel getTaskCreated() { return taskCreated; }

    public String getMessage() {
        return message;
    }
}
