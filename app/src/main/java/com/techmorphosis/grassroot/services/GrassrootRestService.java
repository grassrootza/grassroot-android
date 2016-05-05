package com.techmorphosis.grassroot.services;

import com.google.gson.GsonBuilder;
import com.techmorphosis.grassroot.services.model.GenericResponse;
import com.techmorphosis.grassroot.services.model.GroupResponse;
import com.techmorphosis.grassroot.services.model.GroupSearchResponse;
import com.techmorphosis.grassroot.services.model.TaskResponse;
import com.techmorphosis.grassroot.services.model.TokenResponse;

import retrofit.RequestInterceptor;
import retrofit.RestAdapter;
import retrofit.converter.GsonConverter;
import retrofit.http.Path;
import retrofit.http.GET;
import retrofit.http.POST;
import retrofit.http.Query;
import rx.Observable;

/**
 * Created by paballo on 2016/05/03.
 */
public class GrassrootRestService {

        private static final String GRASSROOT_SERVER_URL = "http://staging.grassroot.org.za/api";
        private RestApi mRestApi;

        public GrassrootRestService() {


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

           //authenticate existing user
            @GET("/user/login/authenticate/{phoneNumber}/{code}")
            Observable<TokenResponse> authenticate(@Path("phoneNumber") String phoneNumber,
                                                   @Path("code") String code);

            //verify new user login credential
            @GET("/user/verify/{phoneNumber}/{code}")
            Observable<TokenResponse> verify(@Path("phoneNumber") String phoneNumber,
                                             @Path("code") String code);

             //create new group
            @POST("/group/create/{phoneNumber}/{code}")
            Observable<GenericResponse> createGroup(@Path("phoneNumber") String phoneNumber,
                                                    @Path("code") String code,
                                                    @Query("groupName") String groupName,
                                                    @Query("description") String description,
                                                    @Query("phoneNumbers") String[] phoneNumbers);
             //user groups
            @GET("/group/list/{phoneNumber}/{code}")
            Observable<GroupResponse> getUserGroups(@Path("phoneNumber") String phoneNumber,
                                                    @Path("code") String code);
            //group join request
            @POST("/group/join/request/{phoneNumber}/{code}")
            Observable<GenericResponse> groupJoinRequest(@Path("phoneNumber") String phoneNumber,
                                                         @Path("code") String code,
                                                         @Query("uid" )String uid);
            //search for public groups
            @GET("/group/search")
            Observable<GroupSearchResponse> search(@Query("searchTerm") String searchTerm);

            @GET("/task/list/{id}/{phoneNumber}/{code}")
            Observable<TaskResponse> getGroupTasks(@Path("id") String groupUid, @Path("phoneNumber")
            String phoneNumber, @Path("code") String code);

            //cast vote
            @GET("/vote/do/{id}/{phoneNumber}/{code}")
            Observable<GenericResponse> castVote(@Path("id") String voteId,
                                                 @Path("phoneNumber") String phoneNumber,
                                                 @Path("code") String code,
                                                 @Query("response") String response);

            //cast vote
            @GET("/meeting/rsvp/{id}/{phoneNumber}/{code}")
            Observable<GenericResponse> rsvp(@Path("id") String voteId,
                                                 @Path("phoneNumber") String phoneNumber,
                                                 @Path("code") String code,
                                                 @Query("response") String response);

            //complete logbook
            @GET("logbook/complete/do/{id}/{phoneNumber}/{code}")
            Observable<GenericResponse> completeTodo(@Path("id") String todoId,
                                                     @Path("phoneNumber") String phoneNumber,
                                                     @Path("code") String code);











        }

}
