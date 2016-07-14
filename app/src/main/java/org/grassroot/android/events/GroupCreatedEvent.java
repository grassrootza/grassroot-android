package org.grassroot.android.events;

import android.support.annotation.Nullable;
import org.grassroot.android.models.Group;
import org.grassroot.android.models.TaskModel;

/**
 * Created by paballo on 2016/05/31.
 */
public class GroupCreatedEvent {

  private Group group;

  private GroupCreatedEvent() {
    this.group = null;
  }

  public GroupCreatedEvent(Group group){
    this.group = group;
  }

  public Group getGroup() {
    return group;
  }
}
