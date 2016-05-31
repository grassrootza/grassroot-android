package org.grassroot.android.utils;

import android.Manifest;
import android.app.Activity;
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
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import org.grassroot.android.services.GrassrootRestService;
import org.grassroot.android.services.model.GenericResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Created by luke on 2016/05/10.
 */
public class LocationUtils implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
        LocationListener, ResultCallback<LocationSettingsResult> {

    private static final String TAG = LocationUtils.class.getCanonicalName();

    private Activity callingActivity;
    private GoogleApiClient googleApiClient;
    private LocationRequest locationRequest;

    private Location lastKnownLocation;

    private GrassrootRestService grassrootRestService;

    public LocationUtils(Activity activity) {
        callingActivity = activity;
        grassrootRestService = new GrassrootRestService(activity);
        if (googleApiClient == null) {
            googleApiClient = new GoogleApiClient.Builder(activity)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }

        if (Constant.restUrl.equals(Constant.localUrl)) {
            Log.e(TAG, "We are in test mode, fire off a random location!");
            double latitude = Constant.testLatitude + Math.random();
            double longitude = Constant.testLongitude + Math.random();
            storeUserLocation(latitude, longitude);
        }
    }

    public void connect() {
        if (googleApiClient != null) {
            Log.d(TAG, "Connecting to API client!");
            googleApiClient.connect();
        } else {
            Log.d(TAG, "Error! Location utils -- API client not connected");
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        // todo : check for permissions here too
        Log.e(TAG, "We're connected to location services!");
        if (havePermission()) {
            Log.e(TAG, "We have permission to access coarse locations!");
            LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                    .addLocationRequest(createLocationRequest());
            PendingResult<LocationSettingsResult> result = LocationServices.SettingsApi
                    .checkLocationSettings(googleApiClient, builder.build());
            result.setResultCallback(this);
        }
    }

    private LocationRequest createLocationRequest() {
        locationRequest = new LocationRequest();
        locationRequest.setPriority(LocationRequest.PRIORITY_LOW_POWER); // consider switching to no power
        locationRequest.setInterval(60*60*1000); // once per hour, tops
        locationRequest.setFastestInterval(10*60*1000); // ten minutes, tops
        return locationRequest;
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG, "Well, connection suspended ... Who knows why?");
        // todo: resume, when this is more important
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.d(TAG, "Connection failed! Cause: " + connectionResult.toString());
    }

    @Override
    public void onLocationChanged(Location location) {
        if (location != null)
            storeUserLocation(location.getLatitude(), location.getLongitude());
        else
            Log.e(TAG, "A badly designed framework sent us a null value in a callback");
    }

    @Override
    public void onResult(@NonNull LocationSettingsResult locationSettingsResult) {
        Log.d(TAG, "Got the location settings result back!");
        if (havePermission()) {
            LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, this.locationRequest, this);
            lastKnownLocation = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
            if (lastKnownLocation != null) {
                storeUserLocation(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());
            }
        }
    }

    private boolean havePermission() {
        return PermissionUtils.genericPermissionCheck(callingActivity, Manifest.permission.ACCESS_COARSE_LOCATION);
    }

    private void storeUserLocation(double latitude, double longitude) {
        String userNumber = PreferenceUtils.getuser_mobilenumber(callingActivity);
        String userToken = PreferenceUtils.getuser_token(callingActivity);

        if (userNumber == null || userToken == null)
            throw new UnsupportedOperationException("Error! Environment not set up to do this");

        grassrootRestService.getApi()
                .logLocation(userNumber, userToken, latitude, longitude)
                .enqueue(new Callback<GenericResponse>() {
                    @Override
                    public void onResponse(Call<GenericResponse> call, Response<GenericResponse> response) {
                        if (response.isSuccessful()) {
                            Log.d(TAG, "Done! Location recorded");
                        } else {
                            Log.d(TAG, "Nope! Something went wrong, but not the network");
                        }
                    }

                    @Override
                    public void onFailure(Call<GenericResponse> call, Throwable t) {
                        Log.e(TAG, "Something went wrong with the connection");
                    }
                });
    }

}
