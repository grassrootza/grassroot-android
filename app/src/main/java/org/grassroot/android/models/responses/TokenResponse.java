package org.grassroot.android.models.responses;

import com.google.gson.annotations.SerializedName;

public class TokenResponse extends AbstractResponse {

  @SerializedName("data") private Token token;
  private Boolean hasGroups;
  private String userUid;
  private String displayName;
  private int unreadNotificationCount;

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

  public String getUserUid() {
    return userUid;
  }

  public void setUserUid(String userUid) {
    this.userUid = userUid;
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


