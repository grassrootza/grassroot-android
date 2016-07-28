package org.grassroot.android.utils;

import android.util.Log;

import org.grassroot.android.models.Group;
import org.grassroot.android.models.Member;
import org.grassroot.android.models.PreferenceObject;
import org.grassroot.android.models.RealmString;
import org.grassroot.android.models.TaskModel;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import io.realm.Realm;
import io.realm.RealmList;
import io.realm.RealmObject;
import io.realm.RealmQuery;
import io.realm.RealmResults;
import io.realm.Sort;
import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

public class RealmUtils {

    private static final String TAG = RealmUtils.class.getSimpleName();

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

    public static Observable saveDataToRealm(final List<? extends RealmObject> list) {
        return Observable.create(new Observable.OnSubscribe<Boolean>() {
            @Override
            public void call(Subscriber<? super Boolean> subscriber) {
                Realm realm = Realm.getDefaultInstance();
                realm.beginTransaction();
                realm.copyToRealmOrUpdate(list);
                realm.commitTransaction();
                realm.stopWaitForChange();
                realm.close();
                subscriber.onNext(true);
                subscriber.onCompleted();
            }
        }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread());
    }

    public static Observable saveDataToRealm(final RealmObject object) {
        return Observable.create(new Observable.OnSubscribe<Boolean>() {
            @Override
            public void call(Subscriber<? super Boolean> subscriber) {
                Realm realm = Realm.getDefaultInstance();
                realm.beginTransaction();
                realm.copyToRealmOrUpdate(object);
                realm.commitTransaction();
                realm.close();
                subscriber.onNext(true);
                subscriber.onCompleted();
            }
        }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread());
    }

    public static void saveDataToRealmSync(final RealmObject object) {
        Realm realm = Realm.getDefaultInstance();
        realm.beginTransaction();
        realm.copyToRealmOrUpdate(object);
        realm.commitTransaction();
        realm.close();
    }


    public static void saveDataToRealmWithSubscriber(final RealmObject object) {
        Observable.create(new Observable.OnSubscribe<Boolean>() {
            @Override
            public void call(Subscriber<? super Boolean> subscriber) {
                Realm realm = Realm.getDefaultInstance();
                realm.beginTransaction();
                realm.copyToRealmOrUpdate(object);
                realm.commitTransaction();
                realm.close();
                subscriber.onNext(true);
                subscriber.onCompleted();
            }
        })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<Boolean>() {
                    @Override
                    public void call(Boolean aBoolean) {
                        System.out.println("saved");
                    }
                });
    }

    public static void saveGroupToRealm(Group group) {
        Log.d("REALM", "saving group: " + group.toString());
        saveDataToRealmSync(group);
    }

    public static Observable<List<Group>> loadGroupsSorted() {
        Observable<List<Group>> observable =
                Observable.create(new Observable.OnSubscribe<List<Group>>() {
                    @Override
                    public void call(Subscriber<? super List<Group>> subscriber) {
                        final Realm realm = Realm.getDefaultInstance();
                        List<Group> groups = realm.copyFromRealm(
                                realm.where(Group.class).findAllSorted("lastMajorChangeMillis", Sort.DESCENDING));
                        subscriber.onNext(groups);
                        subscriber.onCompleted();
                    }
                }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread());
        return observable;
    }

    public static <T> Observable loadListFromDB(final Class<? extends RealmObject> model,
                                                final String pName, final String pValue) {
        Observable<List<RealmObject>> observable =
                Observable.create(new Observable.OnSubscribe<List<RealmObject>>() {
                    @Override
                    public void call(Subscriber<? super List<RealmObject>> subscriber) {
                        System.out.println("load list " + Thread.currentThread().getName());
                        final Realm realm = Realm.getDefaultInstance();
                        List<RealmObject> realmResults = (List<RealmObject>) realm.copyFromRealm(
                                (realm.where(model).equalTo(pName, pValue).findAll()));
                        realm.close();
                        subscriber.onNext(realmResults);
                        subscriber.onCompleted();
                    }
                }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread());
        return observable;
    }

    public static <T> Observable loadListFromDB(final Class<? extends RealmObject> model,
                                                final String pName, final boolean pValue) {
        Observable<List<RealmObject>> observable =
                Observable.create(new Observable.OnSubscribe<List<RealmObject>>() {
                    @Override
                    public void call(final Subscriber<? super List<RealmObject>> subscriber) {
                        System.out.println("load list " + Thread.currentThread().getName());
                        final Realm realm = Realm.getDefaultInstance();
                        List<RealmObject> realmResults = (List<RealmObject>) realm.copyFromRealm(
                                (realm.where(model).equalTo(pName, pValue).findAll()));
                        realm.close();
                        subscriber.onNext(realmResults);
                        subscriber.onCompleted();
                    }
                }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread());
        return observable;
    }

    public static Observable loadListFromDB(final Class<? extends RealmObject> model,
                                            final Map<String, Object> map) {
        Observable<List<RealmObject>> observable =
                Observable.create(new Observable.OnSubscribe<List<RealmObject>>() {
                    @Override
                    public void call(final Subscriber<? super List<RealmObject>> subscriber) {
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
                        subscriber.onNext(groups);
                        subscriber.onCompleted();
                        realm.close();
                    }
                }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread());
        return observable;
    }

    public static void removeObjectFromDatabase(Class<? extends RealmObject> clazz, String pName,
                                                String pValue) {
        Realm realm = Realm.getDefaultInstance();
        realm.beginTransaction();
        RealmObject object = realm.where(clazz).equalTo(pName, pValue).findFirst();
        if (object != null) object.deleteFromRealm();
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
        System.out.println("Remove objects size " + query.findAll().size());
        realm.beginTransaction();
        if (query.findAll().size() > 0) query.findAll().deleteAllFromRealm();
        realm.commitTransaction();
        System.out.println("now  " + query.findAll().size());
        realm.close();
    }

    public static void removeObjectsByUid(Class<? extends RealmObject> clazz, final String pName,
                                          List<String> pValues) {
        if (pValues != null && pValues.size() > 0) {
            Realm realm = Realm.getDefaultInstance();
            final int size = pValues.size();
            RealmQuery<? extends RealmObject> query = realm.where(clazz).equalTo(pName, pValues.get(0));
            for (int i = 1; i < size; i++) {
                query = query.or().equalTo(pName, pValues.get(i));
            }
            final RealmResults<? extends RealmObject> results = query.findAll();
            realm.executeTransaction(new Realm.Transaction() {
                @Override
                public void execute(Realm realm) {
                    results.deleteAllFromRealm();
                }
            });
            realm.close();
        }
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
        List<PreferenceObject> object =
                realm.copyFromRealm(realm.where(PreferenceObject.class).findAll());
        realm.close();
        return object.size() > 0 ? object.get(0) : new PreferenceObject();
    }

    public static <T extends RealmObject> T loadObjectFromDB(Class<? extends RealmObject> model,
                                                             String pName, String pValue) {
        RealmList<RealmObject> objects = new RealmList<>();
        Realm realm = Realm.getDefaultInstance();
        objects.addAll(realm.copyFromRealm(realm.where(model).equalTo(pName, pValue).findAll()));
        realm.close();
        if (!objects.isEmpty()) {
            return (T) objects.get(0);
        } else {
            return null;
        }
    }

    public static Group loadGroupFromDB(final String groupUid) {
        return loadObjectFromDB(Group.class, "groupUid", groupUid);
    }

    public static List<TaskModel> loadUpcomingTasksFromDB() {
        RealmList<TaskModel> tasks = new RealmList<>();
        Realm realm = Realm.getDefaultInstance();
        RealmResults<TaskModel> results = realm
                .where(TaskModel.class)
                .greaterThan("deadlineDate", new Date())
                .findAll()
                .sort("deadlineDate", Sort.DESCENDING);
        tasks.addAll(realm.copyFromRealm(results));
        realm.close();
        return tasks;
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

    public static long countObjectsInDB(Class<? extends RealmObject> clazz) {
        Realm realm = Realm.getDefaultInstance();
        RealmQuery<? extends RealmObject> query = realm.where(clazz);
        long count = query.count();
        realm.close();
        return count;
    }

    public static Observable loadGroupMembers(final String groupUid) {
        return loadListFromDB(Member.class, "groupUid", groupUid);
    }
}