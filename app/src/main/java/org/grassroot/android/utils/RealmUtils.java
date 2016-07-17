package org.grassroot.android.utils;

import android.util.Log;

import io.realm.Realm;
import io.realm.RealmList;
import io.realm.RealmObject;
import io.realm.RealmQuery;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.grassroot.android.models.Group;
import org.grassroot.android.models.PreferenceObject;
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

  public static void deleteAllObjects() {
    Realm realm = Realm.getDefaultInstance();
    realm.beginTransaction();
    realm.deleteAll();
    realm.commitTransaction();
    realm.close();
  }

  public static void saveDataToRealm(List<? extends RealmObject> list) {
    Realm realm = Realm.getDefaultInstance();
    realm.beginTransaction();
    realm.copyToRealmOrUpdate(list);
    realm.commitTransaction();
    realm.close();
  }

  public static void saveDataToRealm(RealmObject object) {
    Realm realm = Realm.getDefaultInstance();
    realm.beginTransaction();
    realm.copyToRealmOrUpdate(object);
    realm.commitTransaction();
    realm.close();
  }

  public static void saveGroupToRealm(Group group) {
    Log.e("REALM", "saving group: " + group.toString());
    saveDataToRealm(group);
    Group updatedGroup = RealmUtils.loadObjectFromDB(Group.class, "groupUid", group.getGroupUid());
    Log.e("REALM", "group as in Realm : " + updatedGroup.toString());
  }

  public static <T extends RealmList> T loadListFromDB(Class<? extends RealmObject> model,
      String pName, String pValue) {
    RealmList<RealmObject> groups = new RealmList<>();
    Realm realm = Realm.getDefaultInstance();
    groups.addAll(realm.copyFromRealm(realm.where(model).equalTo(pName, pValue).findAll()));
    realm.close();
    return (T) groups;
  }

  public static <T extends RealmList> T loadListFromDB(Class<? extends RealmObject> model,
      String pName, boolean pValue) {
    RealmList<RealmObject> groups = new RealmList<>();
    Realm realm = Realm.getDefaultInstance();
    groups.addAll(realm.copyFromRealm(realm.where(model).equalTo(pName, pValue).findAll()));
    realm.close();
    return (T) groups;
  }

  public static <T extends RealmList> T loadListFromDB(Class<? extends RealmObject> model,
      Map<String, Object> map) {
    RealmList<RealmObject> groups = new RealmList<>();
    Realm realm = Realm.getDefaultInstance();
    RealmQuery<? extends RealmObject> query = realm.where(model);
    for (Map.Entry<String, Object> entry : map.entrySet()) {
      if (entry.getValue() instanceof String) {
        query.equalTo(entry.getKey(), entry.getValue().toString());
      } else {
        query.equalTo(entry.getKey(), Boolean.valueOf(entry.getValue().toString()));
      }
    }
    groups.addAll(realm.copyFromRealm(query.findAll()));
    realm.close();
    return (T) groups;
  }

  public static void removeObjectFromDatabase(Class<? extends RealmObject> clazz, String pName,
      String pValue) {
    Realm realm = Realm.getDefaultInstance();
    realm.beginTransaction();
    realm.where(clazz).equalTo(pName, pValue).findFirst().deleteFromRealm();
    realm.commitTransaction();
    realm.close();
  }

  public static void removeObjectsFromDatabase(Class<? extends RealmObject> clazz,
      Map<String, Object> map) {
    Realm realm = Realm.getDefaultInstance();
    RealmQuery<? extends RealmObject> query = realm.where(clazz);
    for (Map.Entry<String, Object> entry : map.entrySet()) {
      if (entry.getValue() instanceof String) {
        query.equalTo(entry.getKey(), entry.getValue().toString());
      } else {
        query.equalTo(entry.getKey(), Boolean.valueOf(entry.getValue().toString()));
      }
    }
    realm.beginTransaction();
    query.findAll().deleteAllFromRealm();
    realm.commitTransaction();
    realm.close();
  }

  public static <T extends RealmList> T loadListFromDB(Class<? extends RealmObject> model) {
    RealmList<RealmObject> groups = new RealmList<>();
    Realm realm = Realm.getDefaultInstance();
    groups.addAll(realm.copyFromRealm(realm.where(model).findAll()));
    realm.close();
    return (T) groups;
  }

  public static PreferenceObject loadPreferencesFromDB() {
    Realm realm = Realm.getDefaultInstance();
    List<PreferenceObject> object = realm.copyFromRealm(realm.where(PreferenceObject.class).findAll());
    realm.close();
    return object.size() > 0 ? object.get(0) : new PreferenceObject();
  }

  public static <T extends RealmObject> T loadObjectFromDB(Class<? extends RealmObject> model,
      String pName, String pValue) {
    RealmList<RealmObject> groups = new RealmList<>();
    Realm realm = Realm.getDefaultInstance();
    groups.addAll(realm.copyFromRealm(realm.where(model).equalTo(pName, pValue).findAll()));
    realm.close();
    return (T) groups.get(0);
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