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
    String TASK_UID_FIELD = "taskUid";
    String TASK_ENTITY_FIELD = "taskEntity";

    String NO_TASKS_FOUND = "NO_GROUP_ACTIVITIES"; // todo: change literal on server nad here

    String TODO_PENDING = "pending";
    String TODO_DONE = "completed";
    String TODO_OVERDUE = "overdue";

    String RESPONSE_YES = "Yes";
    String RESPONSE_NO = "No";
    String RESPONSE_NONE = "No response yet"; // todo: okay really need to fix this on server...asking for trouble in this form

    SimpleDateFormat dateDisplayFormatWithHours = new SimpleDateFormat("HH:mm dd-MM");
    SimpleDateFormat dateDisplayWithDayName = new SimpleDateFormat("H:mm, EEE, d MMM");

}
