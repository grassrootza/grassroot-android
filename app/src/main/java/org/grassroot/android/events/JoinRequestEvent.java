package org.grassroot.android.events;

/**
 * Created by luke on 2016/07/09.
 * Note : should only be posted when something happens in background (i.e., notification), not after deliberate fetch
 */
public class JoinRequestEvent {

	private final String TAG;

	public JoinRequestEvent(String TAG) {
		this.TAG = TAG;
	}

	public String getTAG() { return TAG; }

}
