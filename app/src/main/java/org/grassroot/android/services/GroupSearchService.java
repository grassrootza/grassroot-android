package org.grassroot.android.services;

import android.util.Log;

import org.grassroot.android.models.GroupJoinRequest;
import org.grassroot.android.models.PublicGroupModel;
import org.grassroot.android.models.exceptions.ApiCallException;
import org.grassroot.android.models.responses.GenericResponse;
import org.grassroot.android.models.responses.GroupSearchResponse;
import org.grassroot.android.models.responses.JoinRequestResponse;
import org.grassroot.android.utils.ErrorUtils;
import org.grassroot.android.utils.NetworkUtils;
import org.grassroot.android.utils.RealmUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Scheduler;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import retrofit2.Response;

/**
 * Created by luke on 2016/08/11.
 */
public class GroupSearchService {

	public static final String TAG = GroupSearchService.class.getSimpleName();

	private static GroupSearchService instance = null;

	public List<PublicGroupModel> foundByGroupName;
	public List<PublicGroupModel> foundByTaskName;

	protected GroupSearchService() {
	}

	public static GroupSearchService getInstance() {
		GroupSearchService methodInstance = instance;
		if (methodInstance == null) {
			synchronized (GroupSearchService.class) {
				methodInstance = instance;
				if (methodInstance == null) {
					instance = methodInstance = new GroupSearchService();
				}
			}
		}
		return methodInstance;
	}

	public boolean hasResults() {
		return ((foundByGroupName != null && !foundByGroupName.isEmpty()) ||
			(foundByTaskName != null && !foundByTaskName.isEmpty()));
	}

	public boolean hasNameResults() {
		return foundByGroupName != null && !foundByGroupName.isEmpty();
	}

	public boolean hasSubjectResults() {
		return foundByTaskName != null && !foundByTaskName.isEmpty();
	}

	public Observable<String> searchForGroups(final String searchTerm, final boolean searchNamesAndTerms,
											  final boolean restrictByLocation, final int searchRadius) {
		return Observable.create(new ObservableOnSubscribe<String>() {
			@Override
			public void subscribe(ObservableEmitter<String> subscriber) {
				if (NetworkUtils.isOnline()) {
					try {
						final String mobileNumber = RealmUtils.loadPreferencesFromDB().getMobileNumber();
						final String code = RealmUtils.loadPreferencesFromDB().getToken();
						final String trimmedTerm = searchTerm.trim();
						Response<GroupSearchResponse> searchResponse = GrassrootRestService.getInstance()
							.getApi().search(mobileNumber, code, trimmedTerm, !searchNamesAndTerms,
								restrictByLocation, searchRadius).execute();
						if (searchResponse.isSuccessful()) {
							separatePublicGroupResults(searchResponse.body().getGroups());
							subscriber.onNext(NetworkUtils.FETCHED_SERVER);
						} else {
							throw new ApiCallException(NetworkUtils.SERVER_ERROR,
								ErrorUtils.getRestMessage(searchResponse.errorBody()));
						}
					} catch (IOException e) {
						NetworkUtils.setConnectionFailed();
						throw new ApiCallException(NetworkUtils.CONNECT_ERROR);
					}
				} else  {
					throw new ApiCallException(NetworkUtils.CONNECT_ERROR);
				}
			}
		}).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread());
	}

	private void separatePublicGroupResults(List<PublicGroupModel> results) {
		foundByGroupName = new ArrayList<>();
		foundByTaskName = new ArrayList<>();

		for (PublicGroupModel model : results) {
			if (model.isTermInName()) {
				foundByGroupName.add(model);
			} else {
				foundByTaskName.add(model);
			}
		}
	}

	public Observable<String> sendJoinRequest(final PublicGroupModel groupModel, Scheduler observingThread) {
		observingThread = (observingThread == null) ? AndroidSchedulers.mainThread() : observingThread;
		return Observable.create(new ObservableOnSubscribe<String>() {
			@Override
			public void subscribe(ObservableEmitter<String> subscriber) {
				try {
					final String mobileNumber = RealmUtils.loadPreferencesFromDB().getMobileNumber();
					final String code = RealmUtils.loadPreferencesFromDB().getToken();
					Response<JoinRequestResponse> sendRequest = GrassrootRestService.getInstance().getApi()
						.sendGroupJoinRequest(mobileNumber, code, groupModel.getId(), groupModel.getDescription()).execute();
					if (sendRequest.isSuccessful()) {
						removePublicGroup(groupModel);
						Log.e(TAG, "response = " + sendRequest.body().toString());
						RealmUtils.saveDataToRealmSync(sendRequest.body().getRequests().first());
						subscriber.onNext(NetworkUtils.SAVED_SERVER);
					} else {
						throw new ApiCallException(NetworkUtils.SERVER_ERROR,
							ErrorUtils.getRestMessage(sendRequest.errorBody()));
					}
				} catch (IOException e) {
					storeJoinRequest(groupModel);
					NetworkUtils.setConnectionFailed();
					throw new ApiCallException(NetworkUtils.CONNECT_ERROR);
				}
			}
		}).subscribeOn(Schedulers.io()).observeOn(observingThread);
	}

	public Observable<String> cancelJoinRequest(final String groupUid, Scheduler observingThread) {
		observingThread = (observingThread == null) ? AndroidSchedulers.mainThread() : observingThread;
		return Observable.create(new ObservableOnSubscribe<String>() {
			@Override
			public void subscribe(ObservableEmitter<String> subscriber) {
				try {
					final String mobileNumber = RealmUtils.loadPreferencesFromDB().getMobileNumber();
					final String code = RealmUtils.loadPreferencesFromDB().getToken();
					Response<GenericResponse> response = GrassrootRestService.getInstance().getApi()
						.cancelJoinRequest(mobileNumber, code, groupUid).execute();
					if (response.isSuccessful()) {
						RealmUtils.removeObjectFromDatabase(GroupJoinRequest.class, "groupUid", groupUid);
						subscriber.onNext(NetworkUtils.SAVED_SERVER);
					} else {
						final String restMessage = ErrorUtils.getRestMessage(response.errorBody());
						if (ErrorUtils.JREQ_NOT_FOUND.equals(restMessage)) {
							// slight violence here, but just in case it was approved already (should display different msg in future .. todo)
							subscriber.onNext(NetworkUtils.SAVED_SERVER);
							RealmUtils.removeObjectFromDatabase(GroupJoinRequest.class, "groupUid", groupUid);
						} else {
							throw new ApiCallException(NetworkUtils.SERVER_ERROR,
									ErrorUtils.getRestMessage(response.errorBody()));
						}
					}
				} catch (IOException e) {
					NetworkUtils.setConnectionFailed();
					throw new ApiCallException(NetworkUtils.CONNECT_ERROR);
				}
			}
		}).subscribeOn(Schedulers.io()).observeOn(observingThread);
	}

	public Observable<String> remindJoinRequest(final String groupUid, Scheduler observingThread) {
		observingThread = (observingThread == null) ? AndroidSchedulers.mainThread() : observingThread;
		return Observable.create(new ObservableOnSubscribe<String>() {
			@Override
			public void subscribe(ObservableEmitter<String> subscriber) {
				try {
					final String mobileNumber = RealmUtils.loadPreferencesFromDB().getMobileNumber();
					final String code = RealmUtils.loadPreferencesFromDB().getToken();
					Response<GenericResponse> response = GrassrootRestService.getInstance().getApi()
						.remindJoinRequest(mobileNumber, code, groupUid).execute();
					if (response.isSuccessful()) {
						subscriber.onNext(NetworkUtils.SAVED_SERVER);
					} else {
						throw new ApiCallException(NetworkUtils.SERVER_ERROR,
							ErrorUtils.getRestMessage(response.errorBody()));
					}
				} catch (IOException e) {
					NetworkUtils.setConnectionFailed();
					throw new ApiCallException(NetworkUtils.CONNECT_ERROR);
				}
			}
		}).subscribeOn(Schedulers.io()).observeOn(observingThread);
	}

	public Observable<String> sendStoredJoinRequests(Scheduler observingThread) {
		observingThread = (observingThread == null) ? AndroidSchedulers.mainThread() : observingThread;
		return Observable.create(new ObservableOnSubscribe<String>() {
			@Override
			public void subscribe(ObservableEmitter<String> subscriber) {
				Map<String, Object> recallMap = new HashMap<>();
				recallMap.put("isJoinReqLocal", true);
				List<PublicGroupModel> storedModels = RealmUtils.loadListFromDBInline(PublicGroupModel.class, recallMap);
				if (storedModels != null && !storedModels.isEmpty()) {
					// note : the for loop is quite inefficient, but don't expect many of these (if changes, switch to a set)
					final String mobileNumber = RealmUtils.loadPreferencesFromDB().getMobileNumber();
					final String code = RealmUtils.loadPreferencesFromDB().getToken();
					try {
						for (PublicGroupModel storedRequest : storedModels) {
							// note : only possible server error is 'already part of group', in which case can safely ignore & delete
							GrassrootRestService.getInstance().getApi().sendGroupJoinRequest(mobileNumber, code, storedRequest.getId(), storedRequest.getDescription())
								.execute();
							storedRequest.setJoinReqLocal(false);
							RealmUtils.saveDataToRealmSync(storedRequest);
						}
						subscriber.onNext(NetworkUtils.SAVED_SERVER);
					} catch (IOException e) {
						NetworkUtils.setConnectionFailed();
						subscriber.onNext(NetworkUtils.OFFLINE_ON_FAIL);
					}
				}
			}
		}).subscribeOn(Schedulers.io()).observeOn(observingThread);
	}

	private void storeJoinRequest(final PublicGroupModel groupModel) {
		groupModel.setJoinReqLocal(true);
		RealmUtils.saveDataToRealmSync(groupModel);
	}

	private void removePublicGroup(final PublicGroupModel groupModel) {
		RealmUtils.removeObjectFromDatabase(PublicGroupModel.class,
			"id", groupModel.getId());
		if (foundByGroupName != null) {
			foundByGroupName.remove(groupModel);
		}
		if (foundByTaskName != null) {
			foundByTaskName.remove(groupModel);
		}
	}

}
