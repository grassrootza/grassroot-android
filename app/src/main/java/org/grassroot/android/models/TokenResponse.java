package org.grassroot.android.models;

import com.google.gson.annotations.SerializedName;

import io.realm.RealmObject;

public class TokenResponse extends RealmObject {

  private String status;
  private Integer code;
  private String message;
  @SerializedName("data") private Token token;
  private Boolean hasGroups;
  private String displayName;
  private int unreadNotificationCount;

  public TokenResponse() {

  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public Integer getCode() {
    return code;
  }

  public void setCode(Integer code) {
    this.code = code;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public Token getToken() {
    return token;
  }

  public void setToken(Token token) {
    this.token = token;
  }

  public Boolean getHasGroups() {
    return hasGroups;
  }

  public void setHasGroups(Boolean hasGroups) {
    this.hasGroups = hasGroups;
  }

  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public int getUnreadNotificationCount() {
    return unreadNotificationCount;
  }

  public void setUnreadNotificationCount(int unreadNotificationCount) {
    this.unreadNotificationCount = unreadNotificationCount;
  }
}


