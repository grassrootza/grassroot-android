package org.grassroot.android.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

import org.grassroot.android.BuildConfig;
import org.grassroot.android.activities.StartActivity;
import org.grassroot.android.events.UserLoggedOutEvent;
import org.grassroot.android.interfaces.NotificationConstants;
import org.grassroot.android.models.PreferenceObject;
import org.grassroot.android.models.exceptions.ApiCallException;
import org.grassroot.android.models.responses.GenericResponse;
import org.grassroot.android.models.responses.RestResponse;
import org.grassroot.android.models.responses.TokenResponse;
import org.grassroot.android.services.GcmRegistrationService;
import org.grassroot.android.services.GrassrootRestService;
import org.grassroot.android.services.MqttConnectionManager;
import org.greenrobot.eventbus.EventBus;

import java.io.IOException;
import java.util.concurrent.Callable;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import retrofit2.Response;

/**
 * Created by luke on 2016/08/08.
 */
public class LoginRegUtils {

	private static final String TAG = LoginRegUtils.class.getSimpleName();

	public static final String OTP_ALREADY_SENT = "otp_already_sent";
	public static final String OTP_PROD_SENT = "otp_sent_prod";
	public static final String AUTH_HAS_GROUPS = "authenticated";
	public static final String AUTH_NO_GROUPS = "authenticated_no_groups";
	public static final String AUTH_REFRESHED = "authenticated_refreshed";

	public static Observable<String> reqLogin(final String mobileNumber) {
		return Observable.create(new ObservableOnSubscribe<String>() {
			@Override
			public void subscribe(ObservableEmitter<String> subscriber) {
				final String msisdn = Utilities.formatNumberToE164(mobileNumber);
				try {
					Response<GenericResponse> response = GrassrootRestService.getInstance().getApi()
						.login(msisdn).execute();
					if (response.isSuccessful()) {
						final String returnTag = BuildConfig.FLAVOR.equals(Constant.STAGING) ? (String) response.body().getData() :
							OTP_PROD_SENT;
						subscriber.onNext(returnTag);
					} else {
						// to be safe, make sure the OTP is sent again next time (unless server overrides)
						throw new ApiCallException(NetworkUtils.SERVER_ERROR, ErrorUtils.getRestMessage(response.errorBody()));
					}
				} catch (IOException e) {
					// if there was an interruption in the call, rather make sure a new one is sent
					throw new ApiCallException(NetworkUtils.CONNECT_ERROR);
				}
			}
		}).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread());
	}

	public static Observable<String> reqRegister(final String mobileNumber, final String displayName) {
		return Observable.create(new ObservableOnSubscribe<String>() {
			@Override
			public void subscribe(ObservableEmitter<String> subscriber) {
				final String msisdn = Utilities.formatNumberToE164(mobileNumber);
				try {
					Response<GenericResponse> response = GrassrootRestService.getInstance().getApi()
						.addUser(msisdn, displayName).execute();
					if (response.isSuccessful()) {
						final String returnTag = BuildConfig.FLAVOR.equals(Constant.STAGING) ?
							(String) response.body().getData() : OTP_PROD_SENT;
						subscriber.onNext(returnTag);
					} else {
						throw new ApiCallException(NetworkUtils.SERVER_ERROR, ErrorUtils.getRestMessage(response.errorBody()));
					}
				} catch (IOException e) {
					throw new ApiCallException(NetworkUtils.CONNECT_ERROR);
				}
			}
		}).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread());
	}

	public static Observable<String> resendRegistrationOtp(final String mobileNumber) {
		return Observable.create(new ObservableOnSubscribe<String>() {
			@Override
			public void subscribe(ObservableEmitter<String> subscriber) {
				final String msisdn = Utilities.formatNumberToE164(mobileNumber);
				try {
					Response<GenericResponse> resend = GrassrootRestService.getInstance().getApi()
						.resendRegOtp(msisdn).execute();
					if (resend.isSuccessful()) {
						final String returnTag = BuildConfig.FLAVOR.equals(Constant.STAGING) ?
							(String) resend.body().getData() : OTP_PROD_SENT;
						subscriber.onNext(returnTag);
					} else {
						throw new ApiCallException(NetworkUtils.SERVER_ERROR,
							ErrorUtils.getRestMessage(resend.errorBody()));
					}
				} catch (IOException e) {
					throw new ApiCallException(NetworkUtils.CONNECT_ERROR);
				}
			}
		}).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread());
	}

	public static Observable<String> authenticateLogin(final String mobileNumber, final String otpEntered) {
		return Observable.create(new ObservableOnSubscribe<String>() {
			@Override
			public void subscribe(ObservableEmitter<String> subscriber) {
				try {
					final String msisdn = Utilities.formatNumberToE164(mobileNumber);
					Response<TokenResponse> response = GrassrootRestService.getInstance().getApi()
						.authenticate(msisdn, otpEntered).execute();
					if (response.isSuccessful()) {
						checkUserArchiveAndSetupPrefs(msisdn, response.body());
						subscriber.onNext(response.body().getHasGroups() ? AUTH_HAS_GROUPS : AUTH_NO_GROUPS);
					} else {
						throw new ApiCallException(NetworkUtils.SERVER_ERROR,
							ErrorUtils.getRestMessage(response.errorBody()));
					}
				} catch (IOException e) {
					throw new ApiCallException(NetworkUtils.CONNECT_ERROR);
				}
			}
		}).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread());
	}

	public static Observable<String> authenticateRegister(final String mobileNumber, final String otpEntered) {
		return Observable.create(new ObservableOnSubscribe<String>() {
			@Override
			public void subscribe(ObservableEmitter<String> subscriber) {
				try {
					final String msisdn = Utilities.formatNumberToE164(mobileNumber);
					Response<TokenResponse> response=  GrassrootRestService.getInstance().getApi()
						.verify(msisdn, otpEntered).execute();
					if (response.isSuccessful()) {
						checkUserArchiveAndSetupPrefs(msisdn, response.body());
						subscriber.onNext(AUTH_NO_GROUPS);
					} else {
						throw new ApiCallException(NetworkUtils.SERVER_ERROR,
							ErrorUtils.getRestMessage(response.errorBody()));
					}
				} catch (IOException e) {
					throw new ApiCallException(NetworkUtils.CONNECT_ERROR);
				}
			}
		}).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread());
	}

	private static void checkUserArchiveAndSetupPrefs(final String msisdn, final TokenResponse response) {
		PreferenceObject preferences;
		if (!checkIfSameMsisdnAsStored(msisdn)) {
			RealmUtils.deleteAllObjects();
			preferences = new PreferenceObject();
		} else {
			preferences = RealmUtils.loadPreferencesFromDB();
		}
		preferences.setToken(response.getToken().getCode());
		preferences.setMobileNumber(msisdn);
		preferences.setLoggedIn(true);
		preferences.setHasGroups(response.getHasGroups());
		preferences.setUserName(response.getDisplayName());
		preferences.setNotificationCounter(response.getUnreadNotificationCount());
		RealmUtils.saveDataToRealmSync(preferences);
	}

	private static boolean checkIfSameMsisdnAsStored(@NonNull final String returnedMsisdn) {
		PreferenceObject storedPreferences = RealmUtils.loadPreferencesFromDB();
		if (storedPreferences == null || TextUtils.isEmpty(storedPreferences.getMobileNumber())) {
			Log.e(TAG, "nothing stored ... returning false");
			return false;
		} else {
			Log.e(TAG, "okay, an msisdn stored, it is: " + storedPreferences.getMobileNumber());
			return storedPreferences.getMobileNumber().equals(returnedMsisdn);
		}
	}

	public static void logout(Activity activity) {
		final String mobileNumber = RealmUtils.loadPreferencesFromDB().getMobileNumber();
		final String code = RealmUtils.loadPreferencesFromDB().getToken();
		Log.e(TAG, "unsubscribing from everything ...");
		MqttConnectionManager.getInstance().unsubscribeAllAndDisconnect(RealmUtils.loadGroupUidsSync());
		Log.e(TAG, "mqtt cleaned up, proceeding ...");
		unregisterGcm(activity); // maybe do preference switch off in log out?
		LoginRegUtils.logoutUserRestCall(mobileNumber, code).subscribe();
		EventBus.getDefault().post(new UserLoggedOutEvent());
		LoginRegUtils.wipeAllButMessagesAndMsisdn();
		Intent i = new Intent(activity, StartActivity.class);
		i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		activity.startActivity(i);
		activity.finish();
	}

	private static void unregisterGcm(Context context) {
		Intent gcmUnregister = new Intent(context, GcmRegistrationService.class);
		gcmUnregister.putExtra(NotificationConstants.ACTION, NotificationConstants.GCM_UNREGISTER);
		gcmUnregister.putExtra(NotificationConstants.PHONE_NUMBER, RealmUtils.loadPreferencesFromDB().getMobileNumber());
		gcmUnregister.putExtra(Constant.USER_TOKEN, RealmUtils.loadPreferencesFromDB().getToken());
		context.startService(gcmUnregister);
	}

	// note: (a) auth code may be wiped from Realm by the time this executes, so passing it makes more thread safe
	// (b) need to pass auth code to make sure user can't be logged out by impersonation
	private static Observable<String> logoutUserRestCall(final String msisdn, final String currentAuthCode) {
		return Observable.create(new ObservableOnSubscribe<String>() {
			@Override
			public void subscribe(ObservableEmitter<String> subscriber) {
				try {
					Response<GenericResponse> logout = GrassrootRestService.getInstance().getApi()
						.logoutUser(msisdn, currentAuthCode).execute();
					if (logout.isSuccessful()) {
						subscriber.onNext(NetworkUtils.SAVED_SERVER);
					} else {
						// any scenario in which need to catch & handle?
						subscriber.onNext(NetworkUtils.SERVER_ERROR);
					}
				} catch (IOException e) {
					// not much we can do with it, so just fail quietly ...
					subscriber.onNext(NetworkUtils.CONNECT_ERROR);
				}
			}
		}).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread());
	}

	private static void wipeAllButMessagesAndMsisdn() {
		final String phoneNumber = RealmUtils.deleteAllExceptMessagesAndPhone();
		PreferenceObject storedMsisdn = new PreferenceObject();
		storedMsisdn.setMobileNumber(phoneNumber);
		RealmUtils.saveDataToRealmSync(storedMsisdn);
	}

	// Helper methods to handle refreshing token code via OTP

	public static Observable<String> requestTokenRefreshOTP() {
		return Observable.create(new ObservableOnSubscribe<String>() {
			@Override
			public void subscribe(ObservableEmitter<String> e) throws Exception {
				try {
					final String phoneNumber = RealmUtils.loadPreferencesFromDB().getMobileNumber();
					Response<RestResponse<String>> requestNewOtp = GrassrootRestService.getInstance().getApi()
							.requestNewOtp(phoneNumber).execute();
					if (requestNewOtp.isSuccessful()) {
						final String returnTag = BuildConfig.FLAVOR.equals(Constant.STAGING) ?
								requestNewOtp.body().getData() : OTP_PROD_SENT;
						e.onNext(returnTag);
					} else {
						e.onNext(NetworkUtils.SERVER_ERROR);
					}
				} catch (IOException|NullPointerException error) { // adding the null pointer catch because Android
					e.onNext(NetworkUtils.CONNECT_ERROR);
				}
			}
		});
	}

	public static Observable<String> verifyOtpForNewToken(final String enteredOtp) {
		return Observable.fromCallable(new Callable<String>() {
			@Override
			public String call() throws Exception {
				try {
					final String phoneNumber = RealmUtils.loadPreferencesFromDB().getMobileNumber();
					Response<RestResponse<String>> response = GrassrootRestService.getInstance().getApi()
							.obtainNewToken(phoneNumber, enteredOtp).execute();
					if (response.isSuccessful()) {
						final String token = response.body().getData();
						PreferenceObject prefs = RealmUtils.loadPreferencesFromDB();
						prefs.setToken(token);
						RealmUtils.saveDataToRealmSync(prefs);
						return AUTH_REFRESHED;
					} else {
						return NetworkUtils.CONNECT_ERROR;
					}
				} catch (IOException|NullPointerException e) {
					return NetworkUtils.CONNECT_ERROR;
				}
			}
		});
	}

}
