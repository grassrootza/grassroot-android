package org.grassroot.android.utils;

import io.realm.RealmList;
import java.util.ArrayList;
import java.util.List;
import org.grassroot.android.models.RealmString;

public class RealmUtils {

  public static List<String> convertListOfRealmStringInListOfString(
      RealmList<RealmString> realmList) {
    if (realmList != null) {
      List<String> list = new ArrayList<>();
      for (RealmString realmString : realmList) {
        list.add(realmString.getString());
      }
      return list;
    }
    return null;
  }

  public static RealmList<RealmString> convertListOfStringInRealmListOfString(List<String> list) {
    if (list != null) {
      String[] arrayOfStrings = new String[list.size()];
      list.toArray(arrayOfStrings);
      RealmString[] arrayOfRealmStrings = createRealmStringArrayFromStringArray(arrayOfStrings);
      return new RealmList<>(arrayOfRealmStrings);
    }
    return null;
  }

  private static RealmString[] createRealmStringArrayFromStringArray(String[] array) {
    RealmString[] realmStrings = null;
    if (array != null) {
      realmStrings = new RealmString[array.length];
      for (int i = 0; i < array.length; i++) {
        realmStrings[i] = new RealmString(array[i]);
      }
    }
    return realmStrings;
  }
}