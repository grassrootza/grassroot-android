package org.grassroot.android.models.helpers;

import io.realm.RealmObject;

public class RealmString extends RealmObject {

  private String string;

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

  @Override
  public boolean equals(Object o) {
    // note : this is returning false when using RealmList because of RealmProxy generated classes, need way around
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    RealmString that = (RealmString) o;

    return string.equals(that.string);

  }

  @Override
  public int hashCode() {
    return string.hashCode();
  }
}