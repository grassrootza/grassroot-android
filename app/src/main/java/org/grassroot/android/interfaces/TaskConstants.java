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

    String TODO_PENDING = "pending";
    String TODO_DONE = "completed";
    String TODO_OVERDUE = "overdue";

    String RESPONSE_YES = "YES";
    String RESPONSE_NO = "NO";
    String RESPONSE_NONE = "NO_RESPONSE";

    SimpleDateFormat dateDisplayFormatWithHours = new SimpleDateFormat("HH:mm dd-MM");
    SimpleDateFormat dateDisplayWithDayName = new SimpleDateFormat("H:mm, EEE, d MMM");
    SimpleDateFormat dateDisplayWithoutHours = new SimpleDateFormat("EEE, d MMM");
    SimpleDateFormat timeDisplayWithoutDate = new SimpleDateFormat("HH:mm");

}
