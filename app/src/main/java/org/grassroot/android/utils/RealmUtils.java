package org.grassroot.android.utils;

import android.os.Looper;
import android.support.annotation.NonNull;
import android.util.Log;

import org.grassroot.android.models.Group;
import org.grassroot.android.models.GroupJoinRequest;
import org.grassroot.android.models.LocalGroupEdits;
import org.grassroot.android.models.Member;
import org.grassroot.android.models.Permission;
import org.grassroot.android.models.PreferenceObject;
import org.grassroot.android.models.PublicGroupModel;
import org.grassroot.android.models.ResponseTotalsModel;
import org.grassroot.android.models.ShareModel;
import org.grassroot.android.models.TaskModel;
import org.grassroot.android.models.TaskNotification;
import org.grassroot.android.models.helpers.RealmString;
import org.grassroot.android.models.responses.Token;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Scheduler;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import io.realm.Realm;
import io.realm.RealmList;
import io.realm.RealmObject;
import io.realm.RealmQuery;
import io.realm.RealmResults;
import io.realm.Sort;

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

    public static boolean realmListContains(String term, RealmList<RealmString> list) {
        if (list == null || list.isEmpty()) {
            return false;
        }
        for (RealmString realmString : list) {
            if (realmString.getString().contains(term)) {
                return true;
            }
        }
        return false;
    }

    public static void deleteAllObjects() {
        Realm realm = Realm.getDefaultInstance();
        realm.beginTransaction();
        realm.deleteAll();
        realm.commitTransaction();
        realm.close();
    }

    public static String deleteAllExceptMessagesAndPhone() {
        final String phoneNumber = RealmUtils.loadPreferencesFromDB().getMobileNumber();
        Realm realm = Realm.getDefaultInstance();
        realm.beginTransaction();

        realm.delete(Group.class);
        realm.delete(GroupJoinRequest.class);
        realm.delete(LocalGroupEdits.class);
        realm.delete(Member.class);
        realm.delete(Permission.class);
        realm.delete(PublicGroupModel.class);
        realm.delete(ResponseTotalsModel.class);

        realm.delete(TaskModel.class);
        realm.delete(TaskNotification.class);

        realm.delete(ShareModel.class);
        realm.delete(Token.class);
        realm.delete(PreferenceObject.class);

        realm.commitTransaction();
        realm.close();
        return phoneNumber;
    }

    public static Observable<Boolean> saveDataToRealm(final List<? extends RealmObject> list, Scheduler observingThread) {
        observingThread = (observingThread == null) ? AndroidSchedulers.mainThread() : observingThread;
        return Observable.create(new ObservableOnSubscribe<Boolean>() {
            @Override
            public void subscribe(ObservableEmitter<Boolean> subscriber) {
                Realm realm = Realm.getDefaultInstance();
                realm.beginTransaction();
                realm.copyToRealmOrUpdate(list);
                realm.commitTransaction();
                realm.close();
                subscriber.onNext(true);
            }
        }).subscribeOn(Schedulers.io()).observeOn(observingThread);
    }

    public static Observable<Boolean> saveDataToRealm(final RealmObject object) {
        return Observable.create(new ObservableOnSubscribe<Boolean>() {
            @Override
            public void subscribe(ObservableEmitter<Boolean> subscriber) {
                Realm realm = Realm.getDefaultInstance();
                realm.beginTransaction();
                realm.copyToRealmOrUpdate(object);
                realm.commitTransaction();
                realm.close();
                subscriber.onNext(true);
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

    public static void saveDataToRealmSync(final List<? extends RealmObject> list) {
        Realm realm = Realm.getDefaultInstance();
        realm.beginTransaction();
        realm.copyToRealmOrUpdate(list);
        realm.commitTransaction();
        realm.close();
    }

    public static void saveDataToRealmWithSubscriber(final RealmObject object) {
        Observable.create(new ObservableOnSubscribe<Boolean>() {
            @Override
            public void subscribe(ObservableEmitter<Boolean> subscriber) {
                Realm realm = Realm.getDefaultInstance();
                realm.beginTransaction();
                realm.copyToRealmOrUpdate(object);
                realm.commitTransaction();
                realm.close();
                subscriber.onNext(true);
            }
        })
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(new Consumer<Boolean>() {
            @Override
            public void accept(Boolean aBoolean) {
                // Log.d(TAG, "saved");
            }
        });
    }

    public static void saveGroupToRealm(Group group) {
        saveDataToRealmSync(group);
    }

    public static Observable<List<Group>> loadGroupsSorted() {
        return Observable.create(new ObservableOnSubscribe<List<Group>>() {
            @Override
            public void subscribe(ObservableEmitter<List<Group>> subscriber) {
                final Realm realm = Realm.getDefaultInstance();
                List<Group> groups = realm.copyFromRealm(
                        realm.where(Group.class).findAllSorted("lastMajorChangeMillis", Sort.DESCENDING));
                subscriber.onNext(groups);
                realm.close();
            }
        }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread());
    }

    public static Observable<List<Group>> loadGroupsFilteredSorted(final Map<String, Object> filterMap,
                                                                   final String sortField, final Sort sortOrder) {
        return Observable.create(new ObservableOnSubscribe<List<Group>>() {
            @Override
            public void subscribe(ObservableEmitter<List<Group>> subscriber) {
                final Realm realm = Realm.getDefaultInstance();
                RealmQuery<? extends RealmObject> query = realm.where(Group.class);
                for (Map.Entry<String, Object> entry : filterMap.entrySet()) {
                    if (entry.getValue() instanceof String) {
                        query.equalTo(entry.getKey(), entry.getValue().toString());
                    } else {
                        query.equalTo(entry.getKey(), Boolean.valueOf(entry.getValue().toString()));
                    }
                }

                List<Group> groups = (List<Group>) realm.copyFromRealm(query.findAllSorted(sortField, sortOrder));
                subscriber.onNext(groups);
                realm.close();
            }
        }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread());
    }

    public static <T> Observable loadListFromDB(final Class<? extends RealmObject> model, Scheduler observingThread) {
        Observable<List<RealmObject>> observable = Observable.create(new ObservableOnSubscribe<List<RealmObject>>() {
            @Override
            public void subscribe(ObservableEmitter<List<RealmObject>> subscriber) {
                RealmList<RealmObject> objects = new RealmList<>();
                Realm realm = Realm.getDefaultInstance();
                objects.addAll(realm.copyFromRealm(realm.where(model).findAll()));
                subscriber.onNext(objects);
                realm.close();
            }
        }).subscribeOn(Schedulers.io()).observeOn(observingThread);
        return observable;
    }

    public static <T> Observable loadListFromDB(final Class<? extends RealmObject> model,
                                                final String pName, final boolean pValue, Scheduler returnThread) {
        return Observable.create(new ObservableOnSubscribe<List<RealmObject>>() {
                    @Override
                    public void subscribe(final ObservableEmitter<List<RealmObject>> subscriber) {
                        // System.out.println("load list " + Thread.currentThread().getName());
                        final Realm realm = Realm.getDefaultInstance();
                        List<RealmObject> realmResults = (List<RealmObject>) realm.copyFromRealm(
                                (realm.where(model).equalTo(pName, pValue).findAll()));
                        realm.close();
                        subscriber.onNext(realmResults);
                    }
                }).subscribeOn(Schedulers.io()).observeOn(returnThread);
    }

    public static Observable loadListFromDB(final Class<? extends RealmObject> model,
                                            final Map<String, Object> map) {
        return Observable.create(new ObservableOnSubscribe<List<RealmObject>>() {
                    @Override
                    public void subscribe(final ObservableEmitter<List<RealmObject>> subscriber) {
                        RealmList<RealmObject> objects = loadListFromDBInline(model, map);
                        subscriber.onNext(objects);
                    }
                }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread());
    }

    public static <T extends RealmList> T loadListFromDBInline(final Class<? extends RealmObject> model,
                                                               final Map<String, Object> map) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            throw new UnsupportedOperationException("Error! Calling inline DB query on main thread");
        }

        RealmList<RealmObject> objects = new RealmList<>();
        Realm realm = Realm.getDefaultInstance();
        RealmQuery<? extends RealmObject> query = realm.where(model);
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (entry.getValue() instanceof String) {
                query.equalTo(entry.getKey(), entry.getValue().toString());
            } else {
                query.equalTo(entry.getKey(), Boolean.valueOf(entry.getValue().toString()));
            }
        }
        objects.addAll(realm.copyFromRealm(query.findAll()));
        realm.close();
        return (T) objects;
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
        realm.beginTransaction();
        if (query.findAll().size() > 0) query.findAll().deleteAllFromRealm();
        realm.commitTransaction();
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

    // only call this from background thread
    public static long countListInDB(final Class<? extends RealmObject> model, final Map<String, Object> map) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            throw new UnsupportedOperationException("Error! Calling inline DB query on main thread");
        }

        Realm realm = Realm.getDefaultInstance();
        RealmQuery<? extends RealmObject> query = realm.where(model);
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (entry.getValue() instanceof String) {
                query.equalTo(entry.getKey(), entry.getValue().toString());
            } else {
                query.equalTo(entry.getKey(), Boolean.valueOf(entry.getValue().toString()));
            }
        }
        long count = query.count();
        realm.close();
        return count;
    }

    /*
    SECTION : methods for specific entity fetch, count, removal
     */

    public static PreferenceObject loadPreferencesFromDB() {
        Realm realm = Realm.getDefaultInstance();
        List<PreferenceObject> object =
            realm.copyFromRealm(realm.where(PreferenceObject.class).findAll());
        realm.close();
        return object.size() > 0 ? object.get(0) : new PreferenceObject();
    }

    public static Group loadGroupFromDB(final String groupUid) {
        return loadObjectFromDB(Group.class, "groupUid", groupUid);
    }

    public static long countGroupsInDB() {
        Realm realm = Realm.getDefaultInstance();
        long count = realm
            .where(Group.class)
            .count();
        realm.close();
        return count;
    }

    public static boolean groupExistsInDB(final String groupUid) {
        Realm realm = Realm.getDefaultInstance();
        long count = realm.where(Group.class).equalTo("groupUid", groupUid).count();
        realm.close();
        return count > 0;
    }

    public static long countGroupMembers(final String groupUid) {
        Realm realm = Realm.getDefaultInstance();
        long count = realm
            .where(Member.class)
            .equalTo("groupUid", groupUid)
            .count();
        realm.close();
        return count;
    }

    public static Observable<List<Member>> loadGroupMembers(final String groupUid, final boolean includeUser) {
        return Observable.create(new ObservableOnSubscribe<List<Member>>() {
            @Override
            public void subscribe(ObservableEmitter<List<Member>> subscriber) {
                RealmList<Member> members = new RealmList<>();
                final Realm realm = Realm.getDefaultInstance();
                final String userMsisdn = realm.where(PreferenceObject.class)
                    .findAll().get(0).getMobileNumber();

                Log.d(TAG, "REALM: total number of members in DB ... " + realm.where(Member.class).count());

                RealmQuery<Member> query;
                if (includeUser) {
                    query = realm
                        .where(Member.class)
                        .equalTo("groupUid", groupUid)
                        .equalTo("phoneNumber", userMsisdn);
                    if (query.count() != 0) {
                        members.add(realm.copyFromRealm(query.findAll().first()));
                    }
                }

                query = realm
                    .where(Member.class)
                    .equalTo("groupUid", groupUid)
                    .notEqualTo("phoneNumber", userMsisdn);
                members.addAll(realm.copyFromRealm(query.findAllSorted("displayName")));

                subscriber.onNext(members);
                realm.close();
            }
        }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread());
    }

    public static long countUpcomingTasksInDB() {
        Realm realm = Realm.getDefaultInstance();
        long count = realm
            .where(TaskModel.class)
            .greaterThan("deadlineDate", new Date())
            .count();
        realm.close();
        return count;
    }

    public static long countGroupTasksInDB(final String parentUid) {
        Realm realm = Realm.getDefaultInstance();
        long count = realm
            .where(TaskModel.class)
            .equalTo("parentUid", parentUid)
            .count();
        realm.close();
        return count;
    }

    public static Observable<List<TaskModel>> loadUpcomingTasks() {
        return Observable.create(new ObservableOnSubscribe<List<TaskModel>>() {
            @Override
            public void subscribe(ObservableEmitter<List<TaskModel>> subscriber) {
                RealmList<TaskModel> tasks = new RealmList<>();
                Realm realm = Realm.getDefaultInstance();
                RealmResults<TaskModel> results = realm
                    .where(TaskModel.class)
                    .greaterThan("deadlineDate", new Date())
                    .findAll()
                    .sort("deadlineDate", Sort.DESCENDING);
                tasks.addAll(realm.copyFromRealm(results));
                realm.close();
                subscriber.onNext(tasks);
            }
        }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread());
    }

    public static Observable<List<TaskModel>> loadTasksSorted(final String parentUid) {
        return Observable.create(new ObservableOnSubscribe<List<TaskModel>>() {
            @Override
            public void subscribe(ObservableEmitter<List<TaskModel>> subscriber) {
                RealmList<TaskModel> tasks = new RealmList<>();
                final Realm realm = Realm.getDefaultInstance();
                RealmResults<TaskModel> results = realm
                    .where(TaskModel.class)
                    .equalTo("parentUid", parentUid)
                    .findAllSorted("deadlineDate", Sort.DESCENDING);
                tasks.addAll(realm.copyFromRealm(results));
                subscriber.onNext(tasks);
                realm.close();
            }
        }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread());
    }

    public static long countObjectsInDB(Class<? extends RealmObject> clazz) {
        Realm realm = Realm.getDefaultInstance();
        RealmQuery<? extends RealmObject> query = realm.where(clazz);
        long count = query.count();
        realm.close();
        return count;
    }

    public static Observable<List<Member>> loadMembersSortedInvalid(final String groupUid) {
        return Observable.create(new ObservableOnSubscribe<List<Member>>() {
            @Override
            public void subscribe(ObservableEmitter<List<Member>> subscriber) {
                RealmList<Member> members = new RealmList<>();
                final Realm realm = Realm.getDefaultInstance();
                List<PreferenceObject> preferences = realm
                    .where(PreferenceObject.class).findAll();
                final String userMsisdn = preferences.get(0).getMobileNumber();
                RealmResults<Member> results = realm
                    .where(Member.class)
                    .equalTo("groupUid", groupUid)
                    .notEqualTo("phoneNumber", userMsisdn)
                    .findAllSorted("isNumberInvalid", Sort.DESCENDING);
                members.addAll(realm.copyFromRealm(results));
                subscriber.onNext(members);
                realm.close();
            }
        }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread());
    }

    public static Observable saveNotificationsToRealm(final List<TaskNotification> notifications) {
        return Observable.create(new ObservableOnSubscribe<Boolean>() {
            @Override
            public void subscribe(ObservableEmitter<Boolean> subscriber) {
                Realm realm = Realm.getDefaultInstance();
                realm.beginTransaction();
                realm.copyToRealmOrUpdate(notifications);
                realm.commitTransaction();
                final List<TaskNotification> savedNotifications = RealmUtils.loadNotificationsSorted();
                if(savedNotifications.size()>100){
                    Log.d(TAG,"Saved objects of size " + String.valueOf(savedNotifications.size()));

                    RealmUtils.saveDataToRealm(savedNotifications.subList(0,100), null)
                            .subscribe(new Consumer<Boolean>() {
                                @Override
                                public void accept(Boolean b) {
                                    Log.d(TAG,"Deleting objects of size " + String.valueOf(savedNotifications.size() - 100));
                                    for(TaskNotification notification : savedNotifications.subList(100,savedNotifications.size())){
                                        Log.d(TAG,"Deleting objects " + notification.getMessage());
                                        RealmUtils.removeObjectFromDatabase(TaskNotification.class,"uid",notification.getUid());
                                    }
                                }
                            });
                }
                realm.close();
                subscriber.onNext(true);
            }
        }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread());
    }

    public static List<TaskNotification> loadNotificationsSorted() {
        final Realm realm = Realm.getDefaultInstance();
        List<TaskNotification> notifications = realm.copyFromRealm(
            realm.where(TaskNotification.class).findAllSorted("createdDateTime", Sort.DESCENDING));
        realm.close();

        return notifications;
    }


    public static void persistFullListJoinRequests(@NonNull final List<GroupJoinRequest> requests) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            Log.e(TAG, "Error! Calling inline DB query on main thread");
            return;
        }

        final Realm realm = Realm.getDefaultInstance();
        if (realm != null && !realm.isClosed()) {
            realm.executeTransaction(new Realm.Transaction() {
                @Override
                public void execute(Realm realm) {
                    if (!requests.isEmpty()) {
                        realm.copyToRealmOrUpdate(requests);
                        final int size = requests.size();
                        final String[] presentUids = new String[size];
                        for (int i = 0; i < size; i++) {
                            presentUids[i] = requests.get(i).getRequestUid();
                        }

                        realm.where(GroupJoinRequest.class)
                            .not()
                            .beginGroup()
                            .in("requestUid", presentUids)
                            .endGroup()
                            .findAll()
                            .deleteAllFromRealm();
                    } else {
                        realm.where(GroupJoinRequest.class)
                            .findAll()
                            .deleteAllFromRealm();
                    }
                }
            });
            realm.close();
        }
    }

    /*
    HELPER METHODS TO DEAL WITH & HANDLE STRINGS
     */

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

    public static RealmList<RealmString> convertListOfStringInRealmListOfString(List<String> list) {
        if (list != null) {
            String[] arrayOfStrings = new String[list.size()];
            list.toArray(arrayOfStrings);
            RealmString[] arrayOfRealmStrings = createRealmStringArrayFromStringArray(arrayOfStrings);
            return new RealmList<>(arrayOfRealmStrings);
        }
        return null;
    }
}