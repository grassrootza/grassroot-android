package org.grassroot.android.models;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

public class PreferenceObject extends RealmObject {
  @PrimaryKey
  private int id = 0;

  private boolean isLoggedIn;
  private boolean hasRatedUs;
  private boolean hasGroups;
  private boolean hasGcmRegistered;
  private boolean mustRefresh;
  private String userName;
  private String mobileNumber;
  private String token;
  private long lastTimeGroupsFetched;

  public String getAlert() {
    return alert;
  }

  public void setAlert(String alert) {
    this.alert = alert;
  }

  private String alert;
  private String languagePreference;
  private int notificationCounter;

  public boolean isLoggedIn() {
    return isLoggedIn;
  }

  public void setLoggedIn(boolean loggedIn) {
    isLoggedIn = loggedIn;
  }

  public boolean isHasRatedUs() {
    return hasRatedUs;
  }

  public void setHasRatedUs(boolean hasRatedUs) {
    this.hasRatedUs = hasRatedUs;
  }

  public boolean isHasGroups() {
    return hasGroups;
  }

  public void setHasGroups(boolean hasGroups) {
    this.hasGroups = hasGroups;
  }

  public boolean isHasGcmRegistered() {
    return hasGcmRegistered;
  }

  public void setHasGcmRegistered(boolean hasGcmRegistered) {
    this.hasGcmRegistered = hasGcmRegistered;
  }

  public boolean isMustRefresh() {
    return mustRefresh;
  }

  public void setMustRefresh(boolean mustRefresh) {
    this.mustRefresh = mustRefresh;
  }

  public String getUserName() {
    return userName;
  }

  public void setUserName(String userName) {
    this.userName = userName;
  }

  public String getMobileNumber() {
    return mobileNumber;
  }

  public void setMobileNumber(String mobileNumber) {
    this.mobileNumber = mobileNumber;
  }

  public String getToken() {
    return token;
  }

  public void setToken(String token) {
    this.token = token;
  }

  public String getLanguagePreference() {
    return languagePreference;
  }

  public void setLanguagePreference(String languagePreference) {
    this.languagePreference = languagePreference;
  }

  public int getNotificationCounter() {
    return notificationCounter;
  }

  public void setNotificationCounter(int notificationCounter) {
    this.notificationCounter = notificationCounter;
  }

  public void setLastTimeGroupsFetched(long lastTimeGroupsFetched) { this.lastTimeGroupsFetched = lastTimeGroupsFetched; }

  public long getLastTimeGroupsFetched() { return lastTimeGroupsFetched; }
}
