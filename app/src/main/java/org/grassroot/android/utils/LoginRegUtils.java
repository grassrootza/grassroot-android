package org.grassroot.android.utils;

import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

import org.grassroot.android.BuildConfig;
import org.grassroot.android.models.PreferenceObject;
import org.grassroot.android.models.exceptions.ApiCallException;
import org.grassroot.android.models.responses.GenericResponse;
import org.grassroot.android.models.responses.TokenResponse;
import org.grassroot.android.services.GrassrootRestService;

import java.io.IOException;

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

	// note: (a) auth code may be wiped from Realm by the time this executes, so passing it makes more thread safe
	// (b) need to pass auth code to make sure user can't be logged out by impersonation
	public static Observable<String> logOutUser(final String msisdn, final String currentAuthCode) {
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

	public static void wipeAllButMessagesAndMsisdn() {
		final String phoneNumber = RealmUtils.deleteAllExceptMessagesAndPhone();
		PreferenceObject storedMsisdn = new PreferenceObject();
		storedMsisdn.setMobileNumber(phoneNumber);
		RealmUtils.saveDataToRealmSync(storedMsisdn);
	}

}
