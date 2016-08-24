package org.grassroot.android.events;

import org.grassroot.android.models.Group;

/**
 * Created by paballo on 2016/05/31.
 */
public class GroupCreatedEvent {

  private final Group group;

  private final String groupUid;

  public GroupCreatedEvent(Group group){
    this.group = group;
    this.groupUid = group.getGroupUid();
  }

  public GroupCreatedEvent(String groupUid) {
    this.group = null;
    this.groupUid = groupUid;
  }

  public Group getGroup() {
    return group;
  }

  public String getGroupUid() { return groupUid; }

  @Override
  public String toString() {
    return "GroupCreatedEvent{" +
        "groupUid='" + groupUid + '\'' +
        ", group=" + group +
        '}';
  }
}
