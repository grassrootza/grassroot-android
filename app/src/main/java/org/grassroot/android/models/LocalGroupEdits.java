package org.grassroot.android.models;

import java.util.UUID;

import io.realm.RealmList;
import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

/**
 * Created by luke on 2016/08/03.
 */
public class LocalGroupEdits extends RealmObject {

	@PrimaryKey
	private String groupEditUid; // note : using own primary key here to preserve coherence/integrity of design

	private String groupUid; // would like this to be unique, but Realm doesn't have that annotation

	private String revisedGroupName;
	private RealmList<RealmString> membersToRemove;

	public LocalGroupEdits() {

	}

	public LocalGroupEdits(String referenceGroupUid) {
		this.groupEditUid = UUID.randomUUID().toString();
		this.groupUid = referenceGroupUid;
		this.membersToRemove = new RealmList<>();
		this.revisedGroupName = "";
	}

	public String getGroupEditUid() {
		return groupEditUid;
	}

	public void setGroupEditUid(String groupEditUid) {
		this.groupEditUid = groupEditUid;
	}

	public String getGroupUid() {
		return groupUid;
	}

	public void setGroupUid(String groupUid) {
		this.groupUid = groupUid;
	}

	public String getRevisedGroupName() {
		return revisedGroupName;
	}

	public void setRevisedGroupName(String revisedGroupName) {
		this.revisedGroupName = revisedGroupName;
	}

	public RealmList<RealmString> getMembersToRemove() {
		return membersToRemove;
	}

	public void setMembersToRemove(RealmList<RealmString> membersToRemove) {
		this.membersToRemove = membersToRemove;
	}
}
