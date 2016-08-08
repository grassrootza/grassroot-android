package org.grassroot.android.events;

/**
 * Created by luke on 2016/07/26.
 */
public class TasksRefreshedEvent {

	public final String parentUid;

	public TasksRefreshedEvent() {
		parentUid = null;
	}

	public TasksRefreshedEvent(final String parentUid) {
		this.parentUid = parentUid;
	}

}
