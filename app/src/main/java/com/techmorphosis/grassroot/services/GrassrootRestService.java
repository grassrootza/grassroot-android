package com.techmorphosis.grassroot.services;

import android.content.Context;

import com.techmorphosis.grassroot.services.model.GenericResponse;
import com.techmorphosis.grassroot.services.model.GroupResponse;
import com.techmorphosis.grassroot.services.model.GroupSearchResponse;
import com.techmorphosis.grassroot.services.model.Member;
import com.techmorphosis.grassroot.services.model.MemberList;
import com.techmorphosis.grassroot.services.model.TaskResponse;
import com.techmorphosis.grassroot.services.model.TokenResponse;
import com.techmorphosis.grassroot.utils.Constant;

import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * Created by paballo on 2016/05/03.
 */
public class GrassrootRestService {

    private static final String GRASSROOT_SERVER_URL = Constant.restUrl;
    private RestApi mRestApi;

    // todo: consider switching to static (but then requires handling connection manager differently...)

    public GrassrootRestService(Context context) {

        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BASIC);

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(logging)
                .addNetworkInterceptor(new ConnectivityInterceptor(context))
                .addNetworkInterceptor(new HeaderInterceptor())
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(GRASSROOT_SERVER_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .client(client).build();

        mRestApi = retrofit.create(RestApi.class);
    }

    public RestApi getApi() {
        return mRestApi;
    }

    public interface RestApi {

        @GET("user/add/{phoneNumber}/{displayName}")
        Call<GenericResponse> addUser(@Path("phoneNumber") String phoneNumber,
                                            @Path("displayName") String displayName);

        @GET("user/login/{phoneNumber}")
        Call<GenericResponse> login(@Path("phoneNumber") String phoneNumber);

       //authenticate existing user
        @GET("user/login/authenticate/{phoneNumber}/{code}")
        Call<TokenResponse> authenticate(@Path("phoneNumber") String phoneNumber,
                                               @Path("code") String code);

        //verify new user login credential
        @GET("user/verify/{phoneNumber}/{code}")
        Call<TokenResponse> verify(@Path("phoneNumber") String phoneNumber,
                                         @Path("code") String code);

        //store user location
        @GET("user/location/{phoneNumber}/{code}/{latitude}/{longitude}")
        Call<GenericResponse> logLocation(@Path("phoneNumber") String phoneNumber, @Path("code") String code,
                                          @Path("latitude") double latitude, @Path("longitude") double longitude);

         //create new group
        @POST("group/create/{phoneNumber}/{code}")
        Call<GenericResponse> createGroup(@Path("phoneNumber") String phoneNumber,
                                                @Path("code") String code,
                                                @Query("groupName") String groupName,
                                                @Query("description") String description,
                                                @Query("phoneNumbers") String[] phoneNumbers); // todo: send names & roles, too
         //user groups
        @GET("group/list/{phoneNumber}/{code}")
        Call<GroupResponse> getUserGroups(@Path("phoneNumber") String phoneNumber,
                                          @Path("code") String code);

        //group join request
        @POST("group/join/request/{phoneNumber}/{code}")
        Call<GenericResponse> groupJoinRequest(@Path("phoneNumber") String phoneNumber,
                                                     @Path("code") String code,
                                                     @Query("uid" )String uid);
        //search for public groups
        @GET("group/search")
        Call<GroupSearchResponse> search(@Query("searchTerm") String searchTerm);

        @GET("task/list/{id}/{phoneNumber}/{code}")
        Call<TaskResponse> getGroupTasks(@Path("id") String groupUid, @Path("phoneNumber") String phoneNumber, @Path("code") String code);

        //cast vote
        @GET("vote/do/{id}/{phoneNumber}/{code}")
        Call<GenericResponse> castVote(@Path("id") String voteId,
                                             @Path("phoneNumber") String phoneNumber,
                                             @Path("code") String code,
                                             @Query("response") String response);

        //cast vote
        @GET("meeting/rsvp/{id}/{phoneNumber}/{code}")
        Call<GenericResponse> rsvp(@Path("id") String voteId,
                                   @Path("phoneNumber") String phoneNumber,
                                   @Path("code") String code,
                                   @Query("response") String response);

        //complete logbook
        @GET("logbook/complete/{phoneNumber}/{code}/{id}")
        Call<GenericResponse> completeTodo(@Path("phoneNumber") String phoneNumber,
                                                 @Path("code") String code,
                                                 @Path("id") String todoId);


        // retrieve group members
        @GET("group/members/list/{phoneNumber}/{code}/{groupUid}")
        Call<MemberList> getGroupMembers(@Path("groupUid") String groupUid, @Path("phoneNumber") String phoneNumber,
                                         @Path("code") String code);

        @Headers("Content-Type: application/json")
        @POST("group/members/add/{phoneNumber}/{code}/{uid}")
        Call<GenericResponse> addGroupMembers(@Path("uid") String groupUid, @Path("phoneNumber") String phoneNumber,
                                              @Path("code") String code, @Body List<Member> membersToAdd);

        @POST("gcm/register/{phoneNumber}/{code}")
        Call<GenericResponse> pushRegistration(@Path("phoneNumber") String phoneNumber,
                                                     @Path("code") String code,
                                                     @Query("registration_id") String regId);
    }

}
