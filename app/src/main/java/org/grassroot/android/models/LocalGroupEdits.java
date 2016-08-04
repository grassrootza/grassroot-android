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

	private boolean changedImage;
	private String changedImageName;

	public boolean changedPublicPrivate; // was it changed
	public boolean changedToPublic; // if yes, was it changed to public

	public boolean closedJoinCode; // since opening locally is not possible, if this is true, we call close the code

	private RealmList<RealmString> organizersToAdd;

	public LocalGroupEdits() {

	}

	public LocalGroupEdits(String referenceGroupUid) {
		this.groupEditUid = UUID.randomUUID().toString();
		this.groupUid = referenceGroupUid;
		this.membersToRemove = new RealmList<>();
		this.revisedGroupName = "";
		this.changedImage = false;
		this.changedImageName = null;
		this.changedPublicPrivate = false;
		this.closedJoinCode = false;
		this.organizersToAdd = new RealmList<>();
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

	public boolean isChangedImage() {
		return changedImage;
	}

	public void setChangedImage(boolean changedImage) {
		this.changedImage = changedImage;
	}

	public String getChangedImageName() {
		return changedImageName;
	}

	public void setChangedImageName(String changedImageName) {
		this.changedImageName = changedImageName;
	}

	public boolean isChangedPublicPrivate() {
		return changedPublicPrivate;
	}

	public void setChangedPublicPrivate(boolean changedPublicPrivate) {
		this.changedPublicPrivate = changedPublicPrivate;
	}

	public boolean isChangedToPublic() {
		return changedToPublic;
	}

	public void setChangedToPublic(boolean changedToPublic) {
		this.changedToPublic = changedToPublic;
	}

	public boolean isClosedJoinCode() {
		return closedJoinCode;
	}

	public void setClosedJoinCode(boolean closedJoinCode) {
		this.closedJoinCode = closedJoinCode;
	}

	public RealmList<RealmString> getOrganizersToAdd() {
		return organizersToAdd;
	}

	public void setOrganizersToAdd(RealmList<RealmString> organizersToAdd) {
		this.organizersToAdd = organizersToAdd;
	}

	public void addOrganizer(String memberUid) {
		organizersToAdd.add(new RealmString(memberUid));
	}
}
