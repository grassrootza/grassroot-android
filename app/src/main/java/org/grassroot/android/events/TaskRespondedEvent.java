package org.grassroot.android.events;

/**
 * Created by luke on 2016/08/23.
 */
public class TaskRespondedEvent {

	private final String taskUid;
	private final String response;

	public TaskRespondedEvent(final String taskUid, final String response) {
		this.taskUid = taskUid;
		this.response = response;
	}

}
