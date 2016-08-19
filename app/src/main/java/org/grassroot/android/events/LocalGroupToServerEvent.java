package org.grassroot.android.events;

import android.support.annotation.NonNull;

/**
 * Created by luke on 2016/08/18.
 */
public class LocalGroupToServerEvent {

	@NonNull public final String localGroupUid;
	@NonNull public final String serverUid;

	public LocalGroupToServerEvent(final String localGroupUid, final String serverUid) {
		this.localGroupUid = localGroupUid;
		this.serverUid = serverUid;
	}

}
