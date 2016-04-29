package com.techmorphosis.grassroot.services;

import com.google.gson.GsonBuilder;
import com.techmorphosis.grassroot.services.model.GenericResponse;
import com.techmorphosis.grassroot.services.model.TokenResponse;

import retrofit.RequestInterceptor;
import retrofit.RestAdapter;
import retrofit.converter.GsonConverter;
import retrofit.http.Path;
import retrofit.http.GET;
import rx.Observable;

/**
 * Created by paballo on 2016/05/03.
 */
public class GrassrootService {

        private static final String GRASSROOT_SERVER_URL = "http://staging.grassroot.org.za/api";
        private RestApi mRestApi;

        public GrassrootService() {


            RequestInterceptor requestInterceptor = new RequestInterceptor() {
                @Override
                public void intercept(RequestInterceptor.RequestFacade request) {
                    request.addHeader("Accept", "application/json");
                }
            };

            RestAdapter restAdapter = new RestAdapter.Builder()
                    .setEndpoint(GRASSROOT_SERVER_URL)
                   .setConverter(new GsonConverter(new GsonBuilder().create()))
                    .setRequestInterceptor(requestInterceptor)
                    .setLogLevel(RestAdapter.LogLevel.FULL)
                    .build();

            mRestApi = restAdapter.create(RestApi.class);
        }

        public RestApi getApi() {

            return mRestApi;
        }

        public interface RestApi {

            @GET("/user/add/{phoneNumber}/{displayName}")
            Observable<GenericResponse> addUser(@Path("phoneNumber") String phoneNumber,
                                                       @Path("displayName") String displayName);

            @GET("/user/login/{phoneNumber}")
            Observable<GenericResponse>
            login(@Path("phoneNumber") String phoneNumber);

            @GET("/user/login/authenticate/{phoneNumber}/{code}")
            Observable<TokenResponse> authenticate(@Path("phoneNumber") String phoneNumber,
                                                   @Path("code") String code);

            @GET("/user/verify/{phoneNumber}/{code}")
            Observable<TokenResponse> verify(@Path("phoneNumber") String phoneNumber,@Path("code") String code);

    }

}
