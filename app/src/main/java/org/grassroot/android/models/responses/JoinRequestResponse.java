package org.grassroot.android.models.responses;

import com.google.gson.annotations.SerializedName;

import org.grassroot.android.models.GroupJoinRequest;

import io.realm.RealmList;

/**
 * Created by luke on 2016/09/01.
 */
public class JoinRequestResponse extends AbstractResponse {

	@SerializedName("data")
	private RealmList<GroupJoinRequest> requests = new RealmList<>();

	public RealmList<GroupJoinRequest> getRequests() { return requests; }

	@Override
	public String toString() {
		return "JoinRequestResponse{" +
				"status='" + status + '\'' +
				", code=" + code +
				", message='" + message + '\'' +
				", requests=" + requests +
				'}';
	}
}
