package org.grassroot.android.events;

import org.grassroot.android.models.TaskModel;

/**
 * Created by paballo on 2016/06/22.
 */
public class TaskUpdatedEvent {

    private final TaskModel task;

    private TaskUpdatedEvent(){
        this.task = null;

    }

    public TaskUpdatedEvent(TaskModel task){
        this.task = task;
    }

    public TaskModel getTask() {
        return task;
    }
}
