package org.grassroot.android.events;

import org.grassroot.android.services.model.TaskModel;

/**
 * Created by luke on 2016/06/06.
 */
public final class TaskChangedEvent {

    private final int position;
    private final TaskModel taskModel;

    // prevent zero field construction
    private TaskChangedEvent() {
        position = -1;
        taskModel = null;
    }

    public TaskChangedEvent(int position, TaskModel taskModel) {
        this.position = position;
        this.taskModel = taskModel;
    }

    public int getPosition() { return position; }

    public TaskModel getTaskModel() { return taskModel; }
}
