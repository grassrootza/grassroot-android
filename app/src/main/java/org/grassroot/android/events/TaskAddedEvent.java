package org.grassroot.android.events;

import org.grassroot.android.services.model.TaskModel;

/**
 * Created by paballo on 2016/05/31.
 */
public class TaskAddedEvent {

    private final TaskModel taskCreated;

    private TaskAddedEvent() {
        this.taskCreated = null;
    } // making private to ensure it's never called empty

    public TaskAddedEvent(TaskModel taskCreated) {
        this.taskCreated = taskCreated;
    }

    public TaskModel getTaskCreated() { return taskCreated; }

}
