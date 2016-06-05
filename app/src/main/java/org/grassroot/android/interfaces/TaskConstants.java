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

    SimpleDateFormat dateDisplayFormatWithHours = new SimpleDateFormat("HH:mm dd-MM");

}
