package org.grassroot.android.models;

import io.realm.RealmObject;

/**
 * Created by paballo on 2016/05/05.
 */
public class PublicGroupModel extends RealmObject{

        private String id;
        private String groupName;
        private String description;
        private String groupCreator;
        private Integer count;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getGroupName() {
            return groupName;
        }

        public void setGroupName(String groupName) {
            this.groupName = groupName;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getGroupCreator() {
            return groupCreator;
        }

        public void setGroupCreator(String groupCreator) {
            this.groupCreator = groupCreator;
        }

        public Integer getCount() {
            return count;
        }

        public void setCount(Integer count) {
            this.count = count;
        }

}