package org.grassroot.android.services;

import android.util.Log;

import org.grassroot.android.interfaces.TaskConstants;
import org.grassroot.android.models.TaskModel;
import org.grassroot.android.models.TaskResponse;
import org.grassroot.android.utils.NetworkUtils;
import org.grassroot.android.utils.RealmUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import io.realm.Realm;
import io.realm.RealmList;
import io.realm.RealmResults;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Created by luke on 2016/07/06.
 */
public class TaskService {

    private static final String TAG = TaskService.class.getSimpleName();

    private Realm realm;
    public ArrayList<TaskModel> upcomingTasks;
    public boolean hasLoadedTasks;

    private static TaskService instance;

    public interface TaskServiceListener {
        void tasksLoadedFromServer();
        void taskLoadingFromServerFailed(ResponseBody errorBody);
        void tasksLoadedFromDB();
    }

    public interface TaskCreationListener {
        void taskCreatedLocally(TaskModel task);
        void taskCreatedOnServer(TaskModel task);
        void taskCreationError(TaskModel task);
    }

    protected TaskService() {
        upcomingTasks = new ArrayList<>();
        hasLoadedTasks = false;
        realm = Realm.getDefaultInstance();
    }

    public static TaskService getInstance() {
        TaskService methodInstance = instance;
        if (methodInstance == null) {
            synchronized (TaskService.class) {
                methodInstance = instance;
                if (methodInstance == null) {
                    instance = methodInstance = new TaskService();
                }
            }
        }
        return methodInstance;
    }

    public boolean hasUpcomingTasks() {
        return !upcomingTasks.isEmpty();
    }

    public void loadCachedUpcomingTasks(TaskServiceListener listener) {
        RealmList<TaskModel> tasks = new RealmList<>();
        if (realm != null && !realm.isClosed()) {
            RealmResults<TaskModel> results = realm.where(TaskModel.class)
                    .greaterThan("deadlineDate", new Date()).findAll();
            tasks.addAll(results.subList(0, results.size()));
        }
        upcomingTasks = new ArrayList<>(tasks);
        hasLoadedTasks = true;
        listener.tasksLoadedFromDB();
    }

    private void saveTasksInDB(List<TaskModel> tasks) {
        if (tasks != null && realm != null && !realm.isClosed()) {
            realm.beginTransaction();
            realm.copyToRealmOrUpdate(tasks);
            realm.commitTransaction();
            realm.close();
        }
    }

    public void fetchUpcomingTasks(final TaskServiceListener listener) {
        loadCachedUpcomingTasks(listener);
        final String mobile = RealmUtils.loadPreferencesFromDB().getMobileNumber();
        final String code = RealmUtils.loadPreferencesFromDB().getToken();
        GrassrootRestService.getInstance().getApi().getUserTasks(mobile, code)
                .enqueue(new Callback<TaskResponse>() {
                    @Override
                    public void onResponse(Call<TaskResponse> call, Response<TaskResponse> response) {
                        if (response.isSuccessful()) {
                            // todo : obviously better ways of doing this
                            upcomingTasks = new ArrayList<>(response.body().getTasks());
                            saveTasksInDB(upcomingTasks);
                            listener.tasksLoadedFromServer();
                        } else {
                            listener.taskLoadingFromServerFailed(response.errorBody());
                            loadCachedUpcomingTasks(listener);
                        }
                    }

                    @Override
                    public void onFailure(Call<TaskResponse> call, Throwable t) {
                        listener.taskLoadingFromServerFailed(null);
                    }
                });
    }

    public void createTask(final TaskModel task, final TaskCreationListener listener) {
        if (NetworkUtils.isNetworkAvailable(ApplicationLoader.applicationContext)) {
            setUpApiCall(task).enqueue(new Callback<TaskResponse>() {
                @Override
                public void onResponse(Call<TaskResponse> call, Response<TaskResponse> response) {
                    if (response.isSuccessful()) {
                        listener.taskCreatedOnServer(response.body().getTasks().get(0));
                    } else {
                        listener.taskCreationError(task);
                    }
                }

                @Override
                public void onFailure(Call<TaskResponse> call, Throwable t) {
                    Log.e(TAG, "Error! Should not occur ... check Network Utils");
                    RealmUtils.saveDataToRealm(task);
                    listener.taskCreatedLocally(task);
                }
            });
        } else {
            RealmUtils.saveDataToRealm(task);
            listener.taskCreatedLocally(task);
        }
    }

    private Call<TaskResponse> setUpApiCall(TaskModel task) {
        final String phoneNumber = RealmUtils.loadPreferencesFromDB().getMobileNumber();
        final String code = RealmUtils.loadPreferencesFromDB().getToken();

        switch (task.getType()) {
            case TaskConstants.MEETING:
                final String location = task.getLocation();
                return GrassrootRestService.getInstance()
                        .getApi()
                        .createMeeting(phoneNumber, code, task.getParentUid(), task.getTitle(), task.getDescription(),
                                task.getDeadlineISO(), task.getMinutes(), location, new HashSet<>(RealmUtils.convertListOfRealmStringInListOfString(task.getMemberUIDS())));
            case TaskConstants.VOTE:
                return GrassrootRestService.getInstance()
                        .getApi()
                        .createVote(phoneNumber, code, task.getParentUid(), task.getTitle(), task.getDescription(),
                                task.getDeadlineISO(), task.getMinutes(), new HashSet<>(RealmUtils.convertListOfRealmStringInListOfString(task.getMemberUIDS())), false);
            case TaskConstants.TODO:
                return GrassrootRestService.getInstance()
                        .getApi()
                        .createTodo(phoneNumber, code, task.getParentUid(), task.getTitle(), task.getDescription(),
                                task.getDeadlineISO(), task.getMinutes(), new HashSet<>(RealmUtils.convertListOfRealmStringInListOfString(task.getMemberUIDS())));
            default:
                throw new UnsupportedOperationException("Error! Missing task type in call");
        }
    }



}
