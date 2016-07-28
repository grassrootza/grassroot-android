package org.grassroot.android.events;

import org.grassroot.android.models.TaskModel;

/**
 * Created by paballo on 2016/06/22.
 */
public class TaskCancelledEvent {
    private final TaskModel task;

    public TaskCancelledEvent(){
        this.task = null;
    }

    public TaskCancelledEvent(TaskModel task){
        this.task = task;
    }

    public TaskModel getTask() {
        return task;
    }
}
