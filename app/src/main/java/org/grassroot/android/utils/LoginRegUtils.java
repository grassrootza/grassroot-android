package org.grassroot.android.utils;

import org.grassroot.android.BuildConfig;
import org.grassroot.android.models.GenericResponse;
import org.grassroot.android.models.PreferenceObject;
import org.grassroot.android.models.TokenResponse;
import org.grassroot.android.models.exceptions.ApiCallException;
import org.grassroot.android.services.GrassrootRestService;

import java.io.IOException;

import retrofit2.Response;
import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Created by luke on 2016/08/08.
 */
public class LoginRegUtils {

	public static final String OTP_ALREADY_SENT = "otp_already_sent";
	public static final String OTP_PROD_SENT = "otp_sent_prod";
	public static final String AUTH_HAS_GROUPS = "authenticated";
	public static final String AUTH_NO_GROUPS = "authenticated_no_groups";

	public static Observable<String> reqLogin(final String mobileNumber) {
		return Observable.create(new Observable.OnSubscribe<String>() {
			@Override
			public void call(Subscriber<? super String> subscriber) {
				final String msisdn = Utilities.formatNumberToE164(mobileNumber);
				try {
					Response<GenericResponse> response = GrassrootRestService.getInstance().getApi()
						.login(msisdn).execute();
					if (response.isSuccessful()) {
						final String returnTag = BuildConfig.FLAVOR.equals(Constant.STAGING) ? (String) response.body().getData() :
							OTP_PROD_SENT;
						subscriber.onNext(returnTag);
						subscriber.onCompleted();
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
		return Observable.create(new Observable.OnSubscribe<String>() {
			@Override
			public void call(Subscriber<? super String> subscriber) {
				final String msisdn = Utilities.formatNumberToE164(mobileNumber);
				try {
					Response<GenericResponse> response = GrassrootRestService.getInstance().getApi()
						.addUser(msisdn, displayName).execute();
					if (response.isSuccessful()) {
						final String returnTag = BuildConfig.FLAVOR.equals(Constant.STAGING) ?
							(String) response.body().getData() : OTP_PROD_SENT;
						subscriber.onNext(returnTag);
						subscriber.onCompleted();
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
		return Observable.create(new Observable.OnSubscribe<String>() {
			@Override
			public void call(Subscriber<? super String> subscriber) {
				final String msisdn = Utilities.formatNumberToE164(mobileNumber);
				try {
					Response<GenericResponse> resend = GrassrootRestService.getInstance().getApi()
						.resendRegOtp(msisdn).execute();
					if (resend.isSuccessful()) {
						final String returnTag = BuildConfig.FLAVOR.equals(Constant.STAGING) ?
							(String) resend.body().getData() : OTP_PROD_SENT;
						subscriber.onNext(returnTag);
						subscriber.onCompleted();
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
		return Observable.create(new Observable.OnSubscribe<String>() {
			@Override
			public void call(Subscriber<? super String> subscriber) {
				try {
					final String msisdn = Utilities.formatNumberToE164(mobileNumber);
					Response<TokenResponse> response = GrassrootRestService.getInstance().getApi()
						.authenticate(msisdn, otpEntered).execute();
					if (response.isSuccessful()) {
						PreferenceObject preferenceObject = setupPreferences(msisdn, response.body());
						RealmUtils.saveDataToRealmSync(preferenceObject);
						subscriber.onNext(response.body().getHasGroups() ? AUTH_HAS_GROUPS : AUTH_NO_GROUPS);
						subscriber.onCompleted();
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
		return Observable.create(new Observable.OnSubscribe<String>() {
			@Override
			public void call(Subscriber<? super String> subscriber) {
				try {
					final String msisdn = Utilities.formatNumberToE164(mobileNumber);
					Response<TokenResponse> response=  GrassrootRestService.getInstance().getApi()
						.verify(msisdn, otpEntered).execute();
					if (response.isSuccessful()) {
						PreferenceObject preferenceObject = setupPreferences(msisdn, response.body());
						RealmUtils.saveDataToRealmSync(preferenceObject);
						subscriber.onNext(AUTH_NO_GROUPS);
						subscriber.onCompleted();
					}
				} catch (IOException e) {
					throw new ApiCallException(NetworkUtils.CONNECT_ERROR);
				}
			}
		}).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread());
	}

	// note: (a) auth code may be wiped from Realm by the time this executes, so passing it makes more thread safe
	// (b) need to pass auth code to make sure user can't be logged out by impersonation
	public static Observable<String> logOutUser(final String msisdn, final String currentAuthCode) {
		return Observable.create(new Observable.OnSubscribe<String>() {
			@Override
			public void call(Subscriber<? super String> subscriber) {
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
				} finally {
					subscriber.onCompleted();
				}
			}
		}).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread());
	}

	private static PreferenceObject setupPreferences(final String msisdn, final TokenResponse response) {
		PreferenceObject preferences = new PreferenceObject();
		preferences.setToken(response.getToken().getCode());
		preferences.setMobileNumber(msisdn);
		preferences.setLoggedIn(true);
		preferences.setHasGroups(response.getHasGroups());
		preferences.setUserName(response.getDisplayName());
		preferences.setNotificationCounter(response.getUnreadNotificationCount());
		return preferences;
	}

}
