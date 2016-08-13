package org.grassroot.android.models;

import io.realm.RealmObject;

public class Token extends RealmObject {

    private String code;
    private Long createdDateTime;
    private Long expiryDateTime;


    public String getCode() {
        return code;
    }
    public void setCode(String code) {
        this.code = code;
    }

    public Long getCreatedDateTime() {
        return createdDateTime;
    }
    public void setCreatedDateTime(Long createdDateTime) {
        this.createdDateTime = createdDateTime;
    }

    public Long getExpiryDateTime() {
        return expiryDateTime;
    }
    public void setExpiryDateTime(Long expiryDateTime) {
        this.expiryDateTime = expiryDateTime;
    }

}
