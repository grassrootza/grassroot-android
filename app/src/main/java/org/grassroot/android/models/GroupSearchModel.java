package org.grassroot.android.models;

/**
 * Created by paballo on 2016/05/05.
 */
public class GroupSearchModel{

        private String id;
        private String groupName;
        private String description;
        private String groupCreator;
        private Integer count;

        /**
         *
         * @return
         * The id
         */
        public String getId() {
            return id;
        }

        /**
         *
         * @param id
         * The id
         */
        public void setId(String id) {
            this.id = id;
        }

        /**
         *
         * @return
         * The groupName
         */
        public String getGroupName() {
            return groupName;
        }

        /**
         *
         * @param groupName
         * The groupName
         */
        public void setGroupName(String groupName) {
            this.groupName = groupName;
        }

        /**
         *
         * @return
         * The description
         */
        public String getDescription() {
            return description;
        }

        /**
         *
         * @param description
         * The description
         */
        public void setDescription(String description) {
            this.description = description;
        }

        /**
         *
         * @return
         * The groupCreator
         */
        public String getGroupCreator() {
            return groupCreator;
        }

        /**
         *
         * @param groupCreator
         * The groupCreator
         */
        public void setGroupCreator(String groupCreator) {
            this.groupCreator = groupCreator;
        }

        /**
         *
         * @return
         * The count
         */
        public Integer getCount() {
            return count;
        }

        /**
         *
         * @param count
         * The count
         */
        public void setCount(Integer count) {
            this.count = count;
        }

}