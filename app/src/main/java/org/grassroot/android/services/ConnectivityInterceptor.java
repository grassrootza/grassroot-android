package org.grassroot.android.services;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import org.grassroot.android.utils.NetworkUtils;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by luke on 2016/05/15.
 */
public class ConnectivityInterceptor implements Interceptor {

    private static final String TAG = ConnectivityInterceptor.class.getCanonicalName();

    private ConnectivityManager connectivityManager;

    public ConnectivityInterceptor(Context context) {
        this.connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
    }


    @Override
    public Response intercept(Chain chain) throws IOException {

        Log.e(TAG,"This was called.");
        NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
        if (activeNetwork == null || !activeNetwork.isConnectedOrConnecting()) {
//            throw new NoConnectivityException("No connectivity", chain.request().url().toString());
        }
        Request request = chain.request();
        Response response = chain.proceed(request);
        return response;
    }
}
