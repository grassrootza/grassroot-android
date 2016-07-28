package org.grassroot.android.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import org.grassroot.android.events.ConnectionFailedEvent;
import org.grassroot.android.events.OfflineActionsSent;
import org.grassroot.android.events.OnlineOfflineToggledEvent;
import org.grassroot.android.models.GenericResponse;
import org.grassroot.android.models.Group;
import org.grassroot.android.models.Member;
import org.grassroot.android.models.PreferenceObject;
import org.grassroot.android.models.TaskModel;
import org.grassroot.android.services.ApplicationLoader;
import org.grassroot.android.services.GrassrootRestService;
import org.grassroot.android.services.GroupService;
import org.grassroot.android.services.TaskService;
import org.greenrobot.eventbus.EventBus;

import java.util.HashMap;
import java.util.Map;

import io.realm.RealmList;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NetworkUtils {

	private static final String TAG = NetworkUtils.class.getSimpleName();

	public static final String ONLINE_DEFAULT = "default";
	public static final String OFFLINE_SELECTED = "offline_selected"; // i.e., user chose to go offline
	public static final String OFFLINE_ON_FAIL = "offline_on_fail"; // i.e., network calls failed, but user said to keep trying

	public static final String SERVER_ERROR = "server_error";
	public static final String CONNECT_ERROR = "connection_error";

	static boolean sendingLocalQueue = false;
	static boolean fetchingServerEntities = false;

	public interface NetworkListener {
		void connectionEstablished();
		void networkAvailableButConnectFailed(String failureType);
		void networkNotAvailable();
		void setOffline();
	}

	public static boolean isOnline() {
		return isOnline(ApplicationLoader.applicationContext);
	}

	public static void toggleOnlineOffline(final Context context, final boolean sendQueue, final NetworkListener listener) {
		final String currentStatus = RealmUtils.loadPreferencesFromDB().getOnlineStatus();
		Log.e(TAG, "toggling offline and online, from current status : " + currentStatus);
		if (ONLINE_DEFAULT.equals(currentStatus)) {
			switchToOfflineMode(listener);
		} else {
			trySwitchToOnline(context, sendQueue, listener);
		}
	}

	public static void setOnline() {
		PreferenceObject prefs = RealmUtils.loadPreferencesFromDB();
		prefs.setOnlineStatus(ONLINE_DEFAULT);
		RealmUtils.saveDataToRealm(prefs);
	}

	public static void switchToOfflineMode(NetworkListener listener) {
		PreferenceObject prefs = RealmUtils.loadPreferencesFromDB();
		prefs.setOnlineStatus(OFFLINE_SELECTED);
		RealmUtils.saveDataToRealm(prefs);
		EventBus.getDefault().post(new OnlineOfflineToggledEvent(false));
		if (listener != null) {
			listener.setOffline();
		}
	}

	public static void setOnlineFailed() {
		PreferenceObject prefs = RealmUtils.loadPreferencesFromDB();
		prefs.setOnlineStatus(OFFLINE_ON_FAIL);
		RealmUtils.saveDataToRealm(prefs);
	}

	public static void trySwitchToOnline(final Context context, final boolean sendQueue, final NetworkListener listener) {
		if (!isNetworkAvailable(context)) {
			listener.networkNotAvailable();
		} else {
			final String phoneNumber = RealmUtils.loadPreferencesFromDB().getMobileNumber();
			final String token = RealmUtils.loadPreferencesFromDB().getToken();
			GrassrootRestService.getInstance().getApi().testConnection(phoneNumber, token)
					.enqueue(new Callback<GenericResponse>() {
						@Override
						public void onResponse(Call<GenericResponse> call, Response<GenericResponse> response) {
							if (response.isSuccessful()) {
								setOnline();
								EventBus.getDefault().post(new OnlineOfflineToggledEvent(true));
								if (listener != null) {
									listener.connectionEstablished();
								}
								if (sendQueue) {
									syncLocalAndServer(context);
								}
							} else {
								setOnlineFailed();
								EventBus.getDefault().post(new ConnectionFailedEvent(SERVER_ERROR));
								if (listener != null) {
									listener.networkAvailableButConnectFailed(SERVER_ERROR);
								}
							}
						}

						@Override
						public void onFailure(Call<GenericResponse> call, Throwable t) {
							setOnlineFailed();
							EventBus.getDefault().post(new ConnectionFailedEvent(CONNECT_ERROR));
							if (listener != null) {
								listener.networkAvailableButConnectFailed(CONNECT_ERROR);
							}
						}
					});
		}
	}

	public static boolean isOnline(Context context) {
		final String status = RealmUtils.loadPreferencesFromDB().getOnlineStatus();
		return (!status.equals(OFFLINE_SELECTED) && isNetworkAvailable(context)); // this means we try to connect every time, unless told not to
	}

	public static boolean isNetworkAvailable(Context context) {
		ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo ni = cm.getActiveNetworkInfo();
		return (ni != null && ni.isAvailable() && ni.isConnected());
	}

	public static void syncLocalAndServer(Context context) {
		Log.d(TAG, "inside network utils ... about to call sending queued entities ...");
		if (!sendingLocalQueue) {
			sendingLocalQueue = true;
			if (isOnline(context)) {
				sendLocalGroups();
				sendLocallyAddedMembers();
				sendNewLocalTasks();
				sendEditedTasks();
				EventBus.getDefault().post(new OfflineActionsSent());
			}
		}
		sendingLocalQueue = false;
		fetchEntitiesFromServer(context);
	}

	public static void fetchEntitiesFromServer(final Context context) {
		if (!fetchingServerEntities) {
			fetchingServerEntities = true;
			if (isOnline(context)) {
				GroupService.getInstance().fetchGroupListWithoutError();
				GroupService.getInstance().fetchGroupJoinRequests(null);
				TaskService.getInstance().fetchUpcomingTasks(null);
			}
		}
		fetchingServerEntities = false;
	}

	private static void sendLocalGroups() {
		RealmList<Group> list = RealmUtils.loadListFromDB(Group.class, "isLocal", true);
		for (final Group g : list) {
			GroupService.getInstance().sendNewGroupToServer(g.getGroupUid(), null);
		}
	}

	// todo : maybe use a boolean, locallyEdited, to optimize this
	private static void sendLocallyAddedMembers() {
		Map<String, Object> queryMap = new HashMap<>();
		queryMap.put("isLocal", true);
		RealmList<Group> groups = RealmUtils.loadGroupsSorted();
		for (final Group g : groups) {
			queryMap.put("groupUid", g.getGroupUid());
			final RealmList<Member> addedMembers = RealmUtils.loadListFromDB(Member.class, queryMap);
			if (addedMembers.size() > 0) {
				GroupService.getInstance().postNewGroupMembers(addedMembers, g.getGroupUid(), new GroupService.MembersAddedListener() {
					@Override
					public void membersAdded(String saveType) {
						// blank for now
					}

					@Override
					public void membersAddedError(String errorType, Object data) {
						// blank for now
					}
				});
			}
		}
	}

	private static void sendNewLocalTasks() {
		Map<String, Object> map = new HashMap<>();
		map.put("isLocal", true);
		map.put("isEdited", false);
		map.put("isParentLocal", false);
		final RealmList<TaskModel> tasks = RealmUtils.loadListFromDB(TaskModel.class, map);
		for (final TaskModel model : tasks) {
			final String localUid = model.getTaskUid();
			TaskService.getInstance().sendNewTaskToServer(model, new TaskService.TaskCreationListener() {
				@Override
				public void taskCreatedLocally(TaskModel task) {
					RealmUtils.saveDataToRealm(task);
					RealmUtils.removeObjectFromDatabase(TaskModel.class, "taskUid", localUid);
					System.out.println("TASK CREATED" + task.toString());
				}

				@Override
				public void taskCreatedOnServer(TaskModel task) {

				}

				@Override
				public void taskCreationError(TaskModel task) {

				}
			});
		}
	}

	private static void sendEditedTasks() {
		Map<String, Object> map1 = new HashMap<>();
		map1.put("isLocal", true);
		map1.put("isEdited", true);
		map1.put("isParentLocal", false);
		final RealmList<TaskModel> tasks1 = RealmUtils.loadListFromDB(TaskModel.class, map1);
		for (final TaskModel model : tasks1) {
			TaskService.getInstance().sendTaskUpdateToServer(model, true); // todo : work out selected member change logic
		}
	}
}
