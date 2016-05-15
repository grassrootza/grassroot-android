package com.techmorphosis.grassroot.services;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by luke on 2016/05/15.
 */
public class HeaderInterceptor implements Interceptor {

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request original = chain.request();
        Request modified = original.newBuilder()
                .header("Accept", "application/json")
                .method(original.method(), original.body())
                .build();
        return chain.proceed(modified);
    }
}
