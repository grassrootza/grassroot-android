package org.grassroot.android.services;

import android.Manifest;
import android.content.Context;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;

import org.grassroot.android.BuildConfig;
import org.grassroot.android.models.responses.GenericResponse;
import org.grassroot.android.utils.Constant;
import org.grassroot.android.utils.PermissionUtils;
import org.grassroot.android.utils.RealmUtils;

import java.util.HashMap;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Response;
import rx.Observable;
import rx.Scheduler;
import rx.Subscriber;
import rx.schedulers.Schedulers;

/**
 * Created by luke on 2016/05/10.
 */
public class LocationServices implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
        LocationListener, ResultCallback<LocationSettingsResult> {

    private static final String TAG = LocationServices.class.getCanonicalName();

    private long UPDATE_INTERVAL = 60 * 1000;  // 1 minute
    private long FASTEST_INTERVAL = 10 * 1000; // 10 seconds

    private static LocationServices instance = null;

    private Context context;
    private GoogleApiClient googleApiClient;
    private Location lastKnownLocation;

    public static LocationServices getInstance() {
        LocationServices localInstance = instance;
        if (localInstance == null) {
            synchronized (LocationServices.class) {
                localInstance = instance;
                if (localInstance == null) {
                    instance = localInstance = new LocationServices(ApplicationLoader.applicationContext);
                }
            }
        }
        return localInstance;
    }

    private LocationServices(Context context) {
        this.context = context;
        if (googleApiClient == null) {
            // call backs mean this is on the background thread
            Log.d(TAG, "asking for a Google API Client");
            googleApiClient = new GoogleApiClient.Builder(context)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(com.google.android.gms.location.LocationServices.API)
                    .build();
        }
    }

    // only call on background thread
    public void connect() {
        if (googleApiClient != null) {
            googleApiClient.connect();

            if (BuildConfig.BUILD_TYPE.equals("debug")) {
                Log.d(TAG, "We are in test mode, fire off a random location!");
                double latitude = Constant.testLatitude + Math.random();
                double longitude = Constant.testLongitude + Math.random();
                lastKnownLocation = new Location("");
                lastKnownLocation.setLatitude(latitude);
                lastKnownLocation.setLongitude(longitude);
                storeUserLocation(latitude, longitude, Schedulers.io()).subscribe();
            }
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if (havePermission()) {
            LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                    .addLocationRequest(createLocationRequest());
            PendingResult<LocationSettingsResult> result = com.google.android.gms.location.LocationServices.SettingsApi
                    .checkLocationSettings(googleApiClient, builder.build());
            result.setResultCallback(this);
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        // don't need to worry about this
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        // log and move on
        Log.d(TAG, "Connection failed! Cause: " + connectionResult.toString());
    }

    @Override
    public void onLocationChanged(Location location) {
        if (location != null) {
            lastKnownLocation = location;
            storeUserLocation(location.getLatitude(),
                    location.getLongitude(),
                    Schedulers.immediate()).subscribe();
        }
    }

    @Override
    public void onResult(@NonNull LocationSettingsResult locationSettingsResult) {
        if (havePermission()) {
            com.google.android.gms.location.LocationServices
                    .FusedLocationApi
                    .requestLocationUpdates(googleApiClient, createLocationRequest(), this);

            Location apiLocation = com.google.android.gms.location.LocationServices.FusedLocationApi.getLastLocation(googleApiClient);

            if (apiLocation != null) {
                lastKnownLocation = apiLocation;
                storeUserLocation(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude(), Schedulers.io())
                        .subscribe();
            }
        }
    }

    public boolean hasLasKnownLocation() {
        return lastKnownLocation != null;
    }

    public Location getLastKnownLocation() {
        return lastKnownLocation;
    }

    public HashMap<String, RequestBody> getLocationAsRequestMap() {
        RequestBody latitude = RequestBody.create(MultipartBody.FORM, String.valueOf(lastKnownLocation.getLatitude()));
        RequestBody longitude = RequestBody.create(MultipartBody.FORM, String.valueOf(lastKnownLocation.getLongitude()));
        HashMap<String, RequestBody> location = new HashMap<>();
        location.put("latitude", latitude);
        location.put("longitude", longitude);
        return location;
    }

    private LocationRequest createLocationRequest() {
        return LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_LOW_POWER)
                .setInterval(UPDATE_INTERVAL)
                .setFastestInterval(FASTEST_INTERVAL);
    }

    private boolean havePermission() {
        return PermissionUtils.genericPermissionCheck(context, Manifest.permission.ACCESS_COARSE_LOCATION);
    }

    private Observable<Boolean> storeUserLocation(final double latitude, final double longitude,
                                                 @NonNull Scheduler observingThread) {
        return Observable.create(new Observable.OnSubscribe<Boolean>() {
            @Override
            public void call(Subscriber<? super Boolean> subscriber) {
                String mobileNumber = RealmUtils.loadPreferencesFromDB().getMobileNumber();
                String code = RealmUtils.loadPreferencesFromDB().getToken();
                try {
                    Response<GenericResponse> response = GrassrootRestService.getInstance().getApi()
                        .logLocation(mobileNumber, code, latitude, longitude).execute();
                    if (response.isSuccessful()) {
                        Log.d(TAG, "location recording successful!");
                        subscriber.onNext(true);
                    } else {
                        Log.d(TAG, "location recording failed on server");
                        subscriber.onNext(false);
                    }
                } catch (Exception e) {
                    // swallow all exceptions so we guarantee this fails quietly
                    Log.d(TAG, "location recording failed to connect");
                    subscriber.onNext(false);
                }
            }
        }).subscribeOn(Schedulers.io()).observeOn(observingThread);
    }

}
