package com.techmorphosis.grassroot.services;

import android.content.Context;
import android.support.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.techmorphosis.grassroot.services.model.EventResponse;
import com.techmorphosis.grassroot.services.model.GenericResponse;
import com.techmorphosis.grassroot.services.model.GroupResponse;
import com.techmorphosis.grassroot.services.model.GroupSearchResponse;
import com.techmorphosis.grassroot.services.model.Member;
import com.techmorphosis.grassroot.services.model.MemberDeserializer;
import com.techmorphosis.grassroot.services.model.MemberList;
import com.techmorphosis.grassroot.services.model.NotificationList;
import com.techmorphosis.grassroot.services.model.ProfileResponse;
import com.techmorphosis.grassroot.services.model.TaskResponse;
import com.techmorphosis.grassroot.services.model.TokenResponse;
import com.techmorphosis.grassroot.utils.Constant;

import java.util.List;
import java.util.Set;

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

    private static final String GRASSROOT_SERVER_URL = Constant.stagingUrl;
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

    private static GsonConverterFactory buildGsonConverterFactory() {
        GsonBuilder gsonBuilder = new GsonBuilder();

        // add any custom deserializers here
        gsonBuilder.registerTypeAdapter(Member.class, new MemberDeserializer());
        Gson myGson = gsonBuilder.create();

        return GsonConverterFactory.create(myGson);
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
                                                @Query("phoneNumbers") String[] phoneNumbers);

        @POST("group/create/new/{phoneNumber}/{code}/{groupName}/{description}")
        Call<GenericResponse> createGroupNew(@Path("phoneNumber") String phoneNumber, @Path("code") String code,
                                             @Path("groupName") String groupName, @Path("description") String groupDescription,
                                             @Body List<Member> membersToAdd);

         //user groups
        @GET("group/list/{phoneNumber}/{code}")
        Call<GroupResponse> getUserGroups(@Path("phoneNumber") String phoneNumber,
                                          @Path("code") String code);

        @GET("group/get/{phoneNumber}/{code}/{groupUid}")
        Call<GroupResponse> getSingleGroup(@Path("phoneNumber") String phoneNumber, @Path("code") String code,
                                           @Path("groupUid") String groupUid);

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

        //rsvp for a meeting
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
        @GET("group/members/list/{phoneNumber}/{code}/{groupUid}/{selected}")
        Call<MemberList> getGroupMembers(@Path("groupUid") String groupUid, @Path("phoneNumber") String phoneNumber,
                                         @Path("code") String code, @Path("selected") boolean selected);

        @Headers("Content-Type: application/json")
        @POST("group/members/add/{phoneNumber}/{code}/{uid}")
        Call<GenericResponse> addGroupMembers(@Path("uid") String groupUid, @Path("phoneNumber") String phoneNumber,
                                              @Path("code") String code, @Body List<Member> membersToAdd);

        @POST("group/members/remove/{phoneNumber}/{code}/{groupUid}")
        Call<GenericResponse> removeGroupMembers(@Path("phoneNumber") String phoneNumber, @Path("code") String code,
                                                 @Path("groupUid") String groupUid, @Query("memberUids") Set<String> memberUids);

        @POST("gcm/register/{phoneNumber}/{code}")
        Call<GenericResponse> pushRegistration(@Path("phoneNumber") String phoneNumber,
                                                     @Path("code") String code,
                                                     @Query("registration_id") String regId);

        //retrieve notifications
        @GET("notification/list/{phoneNumber}/{code}")
        Call<NotificationList> getUserNotifications(@Path("phoneNumber") String phoneNumber, @Path("code") String code,
                                                    @Nullable @Query("page") Integer page
                ,@Nullable @Query("size") Integer size);


       //view vote
        @GET("vote/view/{id}/{phoneNumber}/{code}")
        Call<EventResponse> viewVote(@Path("phoneNumber") String phoneNumber, @Path("code") String code,
                                     @Path("id") String id);

        //edit vote
        @POST("vote/update/{id}/{phoneNumber}/{code}")
        Call<GenericResponse> editVote(@Path("phoneNumber") String phoneNumber, @Path("code") String code,
                                 @Path("id") String id,@Query("title") String title, @Query("description") String description,
                                       @Query("closingTime") String closingTime);

        //create vote
        @POST("vote/create/{id}/{phoneNumber}/{code}")
        Call<GenericResponse> createVote(@Path("phoneNumber") String phoneNumber, @Path("code") String code, @Path("id") String groupId,
                                         @Query("title") String title, @Query("description") String description, @Query("closingTime") String closingTime,
                                         @Query("reminderMins") int minutes, @Query("members") List<String> members, @Query("notifyGroup") boolean relayable);


        //Profile settings
        @GET("user/profile/settings/{phoneNumber}/{code}")
        Call<ProfileResponse> getUserProfile(@Path("phoneNumber") String phoneNumber,
                                             @Path("code") String code);
        //Update profile settings
        @POST("user/profile/settings/update/{phoneNumber}/{code}")
        Call<GenericResponse> updateProfile(@Path("phoneNumber") String phoneNumber, @Path("code") String code,
                                            @Query("displayName") String displayName, @Query("language") String language,
        @Query("alertPreference") String preference);




    }

}
