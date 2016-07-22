package org.grassroot.android.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.v7.app.AlertDialog;
import android.util.Log;

import org.grassroot.android.R;
import org.grassroot.android.events.OfflineActionsSent;
import org.grassroot.android.models.Group;
import org.grassroot.android.models.Member;
import org.grassroot.android.models.TaskModel;
import org.grassroot.android.services.ApplicationLoader;
import org.grassroot.android.services.GroupService;
import org.grassroot.android.services.TaskService;
import org.greenrobot.eventbus.EventBus;

import java.util.HashMap;
import java.util.Map;

import io.realm.RealmList;

public class NetworkUtils {

	private static final String TAG = NetworkUtils.class.getSimpleName();

	static boolean sendingLocalQueue = false;
	static boolean fetchingServerEntities = false;

	public static boolean isNetworkAvailable() {
		return isNetworkAvailable(ApplicationLoader.applicationContext);
	}

	public static boolean isNetworkAvailable(Context context) {
		ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo ni = cm.getActiveNetworkInfo();
		return (ni != null && ni.isAvailable() && ni.isConnected());
	}

	public static void syncLocalAndServer(Context context) {
		Log.e(TAG, "inside network utils ... about to call sending queued entities ...");
		if (!sendingLocalQueue) {
			sendingLocalQueue = true;
			if (isNetworkAvailable(context)) {
				sendLocalGroups();
				sendLocallyAddedMembers();
				sendNewLocalTasks();
				sendEditedTasks();
				EventBus.getDefault().post(new OfflineActionsSent());
			}
		}
		sendingLocalQueue = false;
		Log.e(TAG, "inside network utils .... fetching server entities ...");
		if (!fetchingServerEntities) {
			fetchingServerEntities = true;
			if (isNetworkAvailable(context)) {
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
				GroupService.getInstance().postNewGroupMembers(addedMembers, g.getGroupUid());
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
