package org.grassroot.android.utils.image;

import android.util.Log;

import org.grassroot.android.models.ImageRecord;
import org.grassroot.android.models.TaskModel;
import org.grassroot.android.models.exceptions.ApiCallException;
import org.grassroot.android.models.exceptions.ImageNotAnalyzedYetException;
import org.grassroot.android.models.responses.RestResponse;
import org.grassroot.android.services.GrassrootRestService;
import org.grassroot.android.services.LocationServices;
import org.grassroot.android.utils.ErrorUtils;
import org.grassroot.android.utils.NetworkUtils;
import org.grassroot.android.utils.RealmUtils;

import java.io.IOException;
import java.util.HashMap;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Response;

/**
 * Created by luke on 2017/03/21.
 */

public class NetworkImageUtils {

    private static final String TAG = NetworkImageUtils.class.getSimpleName();

    public static Observable<ImageRecord> checkForImageAnalysis(final String logUid, final String taskType) {
        return Observable.create(new ObservableOnSubscribe<ImageRecord>() {
            @Override
            public void subscribe(ObservableEmitter<ImageRecord> e) throws Exception {
                // Log.e(TAG, "inside check for analysis, starting ...");
                final String phoneNumber = RealmUtils.loadPreferencesFromDB().getMobileNumber();
                final String token = RealmUtils.loadPreferencesFromDB().getToken();
                try {
                    Response<RestResponse<ImageRecord>> response = GrassrootRestService.getInstance().getApi()
                            .fetchImageRecord(phoneNumber, token, taskType, logUid).execute();
                    if (response.isSuccessful()) {
                        // Log.e(TAG, "inside check for analysis, got a record, checking for analysis ...");
                        ImageRecord record = response.body().getData();
                        if (record.isAnalyzed()) {
                            e.onNext(response.body().getData());
                        } else {
                            throw new ImageNotAnalyzedYetException();
                        }
                    } else {
                        // Log.e(TAG, "inside check for analysis, exiting without record...");
                        throw new ApiCallException(NetworkUtils.SERVER_ERROR,
                                ErrorUtils.getRestMessage(response.errorBody()));
                    }
                } catch (IOException error) {
                    // Log.e(TAG, "inside check for analysis, IO error ...");
                    throw new ApiCallException(NetworkUtils.CONNECT_ERROR);
                }
            }
        });
    }

    public static Observable<String> uploadTaskImage(final TaskModel task, final String localImagePath,
                                                     final String mimeType, final boolean tryUploadLongLat) {
        return Observable.create(new ObservableOnSubscribe<String>() {
            @Override
            public void subscribe(ObservableEmitter<String> subscriber) throws Exception {
                final String phoneNumber = RealmUtils.loadPreferencesFromDB().getMobileNumber();
                final String token = RealmUtils.loadPreferencesFromDB().getToken();

                MultipartBody.Part image = LocalImageUtils.getImageFromPath(localImagePath, mimeType);
                HashMap<String, RequestBody> location = tryUploadLongLat && LocationServices.getInstance().hasLasKnownLocation() ?
                        LocationServices.getInstance().getLocationAsRequestMap() : null;

                Call<RestResponse<String>> uploadCall = location == null ?
                        GrassrootRestService.getInstance().getApi().uploadImageForTask(phoneNumber, token,
                                task.getType(), task.getTaskUid(), image) :
                        GrassrootRestService.getInstance().getApi().uploadImageWithLocation(phoneNumber, token,
                                task.getType(), task.getTaskUid(), location, image);

                try {
                    Response<RestResponse<String>> response = uploadCall.execute();
                    if (response.isSuccessful()) {
                        subscriber.onNext(response.body().getData());
                    } else {
                        throw new ApiCallException(NetworkUtils.SERVER_ERROR, ErrorUtils.getRestMessage(response.errorBody()));
                    }
                } catch (IOException e) {
                    throw new ApiCallException(NetworkUtils.CONNECT_ERROR);
                }
            }
        });
    }

    public static Observable<String> removeTaskImage(final String taskType, final ImageRecord imageRecord) {
        return Observable.create(new ObservableOnSubscribe<String>() {
            @Override
            public void subscribe(ObservableEmitter<String> subscriber) {
                final String phoneNumber = RealmUtils.loadPreferencesFromDB().getMobileNumber();
                final String code = RealmUtils.loadPreferencesFromDB().getToken();
                try {
                    Response<RestResponse<String>> response = GrassrootRestService.getInstance().getApi()
                            .deleteImageRecord(phoneNumber, code, taskType, imageRecord.getKey(), false).execute();
                    if (response.isSuccessful()) {
                        subscriber.onNext(response.body().getData());
                    } else {
                        throw new ApiCallException(NetworkUtils.SERVER_ERROR,
                                ErrorUtils.getRestMessage(response.errorBody()));
                    }
                } catch (IOException e) {
                    throw new ApiCallException(NetworkUtils.CONNECT_ERROR);
                }
            }
        });
    }

    public static Observable<ImageRecord> updateImageFaceCount(final String logUid, final String taskType,
                                                               final int revisedFaceCount) {
        return Observable.create(new ObservableOnSubscribe<ImageRecord>() {
            @Override
            public void subscribe(ObservableEmitter<ImageRecord> e) throws Exception {
                final String phoneNumber = RealmUtils.loadPreferencesFromDB().getMobileNumber();
                final String code = RealmUtils.loadPreferencesFromDB().getToken();
                try {
                    Response<RestResponse<ImageRecord>> response = GrassrootRestService.getInstance().getApi()
                            .updateFaceCount(phoneNumber, code, taskType, logUid, revisedFaceCount).execute();
                    if (response.isSuccessful()) {
                        Log.e(TAG, "response succeeded, exiting with onNext");
                        e.onNext(response.body().getData());
                    } else {
                        throw new ApiCallException(NetworkUtils.SERVER_ERROR,
                                ErrorUtils.getRestMessage(response.errorBody()));
                    }
                } catch (IOException t) {
                    throw new ApiCallException(NetworkUtils.CONNECT_ERROR);
                }
            }
        });
    }

}
