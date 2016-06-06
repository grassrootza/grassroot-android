package org.grassroot.android.interfaces;

import java.text.SimpleDateFormat;

/**
 * Created by luke on 2016/06/01.
 */
public interface TaskConstants {

    String MEETING = "MEETING";
    String VOTE = "VOTE";
    String TODO = "TODO";

    String TASK_TYPE_FIELD = "taskType";
    String NO_TASKS_FOUND = "NO_GROUP_ACTIVITIES"; // todo: change literal on server nad here

    String TODO_PENDING = "pending";
    String TODO_DONE = "completed";
    String TODO_OVERDUE = "overdue";

    SimpleDateFormat dateDisplayFormatWithHours = new SimpleDateFormat("HH:mm dd-MM");
    SimpleDateFormat dateDisplayWithDayName = new SimpleDateFormat("H:mm, EEE, d MMM");

}
