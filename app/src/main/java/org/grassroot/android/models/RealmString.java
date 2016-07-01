package org.grassroot.android.models;

import io.realm.RealmObject;

public class RealmString extends RealmObject {

  String string;

  public RealmString() {
    this.string = "";
  }

  public RealmString(String string) {
    this.string = string;
  }

  public String getString() {
    return string;
  }

  public void setString(String string) {
    this.string = string;
  }

  @Override
  public String toString() {
    return string;
  }
}