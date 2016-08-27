package org.grassroot.android.interfaces;

import org.grassroot.android.R;

import java.text.SimpleDateFormat;
import java.util.Locale;

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

    SimpleDateFormat dateDisplayFormatWithHours = new SimpleDateFormat("HH:mm dd-MM", Locale.getDefault());
    SimpleDateFormat dateDisplayWithDayName = new SimpleDateFormat("H:mm, EEE, d MMM", Locale.getDefault());
    SimpleDateFormat dateDisplayWithoutHours = new SimpleDateFormat("EEE, d MMM", Locale.getDefault());
    SimpleDateFormat timeDisplayWithoutDate = new SimpleDateFormat("HH:mm", Locale.getDefault());

    int[] meetingReminderMinutes = { 60 * 24, 60 * 6, 60 };
    int[] meetingReminderDesc = { R.string.one_day, R.string.half_day, R.string.one_hour };

    int[] todoReminderMinutes = { 7 * 60 * 24, 60 * 24, 0 };
    int[] todoReminderDesc = { R.string.one_week, R.string.one_day, R.string.on_day };

}
