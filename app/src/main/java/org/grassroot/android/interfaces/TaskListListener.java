package org.grassroot.android.interfaces;

import android.view.View;

/**
 * Created by luke on 2016/05/13.
 */
public interface TaskListListener {

    void respondToTask(String taskUid, String taskType, String response);

    void onCardClick(View view,int position);

}
