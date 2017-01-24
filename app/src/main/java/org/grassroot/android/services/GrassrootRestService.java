package org.grassroot.android.services;

import android.content.Context;
import android.support.annotation.Nullable;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import org.grassroot.android.BuildConfig;
import org.grassroot.android.models.AccountBill;
import org.grassroot.android.models.GroupJoinRequest;
import org.grassroot.android.models.Member;
import org.grassroot.android.models.NotificationList;
import org.grassroot.android.models.Permission;
import org.grassroot.android.models.RealmString;
import org.grassroot.android.models.ResponseTotalsModel;
import org.grassroot.android.models.RsvpListModel;
import org.grassroot.android.models.responses.AccountResponse;
import org.grassroot.android.models.responses.GenericResponse;
import org.grassroot.android.models.responses.GroupChatSettingResponse;
import org.grassroot.android.models.responses.GroupResponse;
import org.grassroot.android.models.responses.GroupSearchResponse;
import org.grassroot.android.models.responses.GroupsChangedResponse;
import org.grassroot.android.models.responses.JoinRequestResponse;
import org.grassroot.android.models.responses.MemberListResponse;
import org.grassroot.android.models.responses.PermissionResponse;
import org.grassroot.android.models.responses.ProfileResponse;
import org.grassroot.android.models.responses.RestResponse;
import org.grassroot.android.models.responses.TaskChangedResponse;
import org.grassroot.android.models.responses.TaskResponse;
import org.grassroot.android.models.responses.TokenResponse;
import org.grassroot.android.utils.Constant;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Set;

import io.realm.RealmList;
import io.realm.RealmObject;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * Created by paballo on 2016/05/03.
 */
public class GrassrootRestService {

  private static final String GRASSROOT_SERVER_URL = Constant.restUrl;
  private Retrofit retrofit;
  private RestApi restApi;

  private static GrassrootRestService instance = null;

  //default constructor required instantiation by the loaders
  public GrassrootRestService() {
  }

  public static GrassrootRestService getInstance() {
    GrassrootRestService methodInstance = instance;
    if (methodInstance == null) {
      synchronized (GrassrootRestService.class) {
        methodInstance = instance;
        if (methodInstance == null) {
          instance = methodInstance = new GrassrootRestService(ApplicationLoader.applicationContext);
        }
      }
    }
    return methodInstance;
  }

  private GrassrootRestService(Context context) {
    HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
    logging.setLevel(BuildConfig.BUILD_TYPE.equals("debug") ?
        HttpLoggingInterceptor.Level.BODY : HttpLoggingInterceptor.Level.HEADERS);

    // logging.setLevel(HttpLoggingInterceptor.Level.BODY);

    OkHttpClient client = new OkHttpClient.Builder()
        .addInterceptor(logging)
        .addNetworkInterceptor(new HeaderInterceptor())
        .build();
    Type token = new TypeToken<RealmList<RealmString>>() {
    }.getType();

    Gson gson = new GsonBuilder()
        .setExclusionStrategies(new ExclusionStrategy() {
            @Override
            public boolean shouldSkipField(FieldAttributes f) {
              return f.getDeclaringClass().equals(RealmObject.class);
            }

            @Override
            public boolean shouldSkipClass(Class<?> clazz) {
              return false;
            }
        }).registerTypeAdapter(token, new TypeAdapter<RealmList<RealmString>>() {

          @Override
          public void write(JsonWriter out, RealmList<RealmString> value) throws IOException {
            // Ignore
          }

          @Override
          public RealmList<RealmString> read(JsonReader in) throws IOException {
            RealmList<RealmString> list = new RealmList<RealmString>();
            in.beginArray();
            while (in.hasNext()) {
              list.add(new RealmString(in.nextString()));
            }
            in.endArray();
            return list;
          }
        })
        .create();

    retrofit = new Retrofit.Builder()
        .baseUrl(GRASSROOT_SERVER_URL)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .client(client).build();

    restApi = retrofit.create(RestApi.class);
  }

  public Retrofit getRetrofit() { return retrofit; }

  public RestApi getApi() {
    return restApi;
  }

  public interface RestApi {

    /*
    SECTION : User login, authentication, etc calls
     */
    @GET("user/add/{phoneNumber}/{displayName}")
    Call<GenericResponse> addUser(@Path("phoneNumber") String phoneNumber,
        @Path("displayName") String displayName);

    @GET("user/login/{phoneNumber}")
    Call<GenericResponse> login(@Path("phoneNumber") String phoneNumber);

    //authenticate existing user
    @GET("user/login/authenticate/{phoneNumber}/{code}")
    Call<TokenResponse> authenticate(@Path("phoneNumber") String phoneNumber,
        @Path("code") String code);

    //request resend of OTP for user registration
    @GET("user/verify/resend/{phoneNumber}")
    Call<GenericResponse> resendRegOtp(@Path("phoneNumber") String phoneNumber);

    //verify new user login credential
    @GET("user/verify/{phoneNumber}/{code}")
    Call<TokenResponse> verify(@Path("phoneNumber") String phoneNumber,
        @Path("code") String code);

    //store user location
    @GET("user/location/{phoneNumber}/{code}/{latitude}/{longitude}")
    Call<GenericResponse> logLocation(@Path("phoneNumber") String phoneNumber, @Path("code") String code,
        @Path("latitude") double latitude, @Path("longitude") double longitude);

    @POST("gcm/register/{phoneNumber}/{code}")
    Call<GenericResponse> pushRegistration(@Path("phoneNumber") String phoneNumber,
        @Path("code") String code,
        @Query("registration_id") String regId);

    @GET("user/logout/{phoneNumber}/{code}")
    Call<GenericResponse> logoutUser(@Path("phoneNumber") String phoneNumber, @Path("code") String code);

    //retrieve notifications
    @GET("notification/list/{phoneNumber}/{code}")
    Call<NotificationList> getUserNotifications(@Path("phoneNumber") String phoneNumber,
                                                @Path("code") String code,
                                                @Nullable @Query("page") Integer page,
                                                @Nullable @Query("size") Integer size);

    @GET("notification/list/{phoneNumber}/{code}")
    Call<NotificationList> getUserNotificationsChangedSince(@Path("phoneNumber") String phoneNumber, @Path("code") String code,
                                                 @Query("changedSince") Long changedSince);

    //Profile settings
    @GET("user/profile/settings/{phoneNumber}/{code}")
    Call<ProfileResponse> getUserProfile(@Path("phoneNumber") String phoneNumber,
        @Path("code") String code);

    //Change user name
    @GET("user/profile/settings/rename/{phoneNumber}/{code}")
    Call<GenericResponse> renameUser(@Path("phoneNumber") String phoneNumber, @Path("code") String code,
                                     @Query("displayName") String displayName); // use query, not path, for better encoding options

    //Alter notification priority preference
    @GET("user/profile/settings/notify/priority/{phoneNumber}/{code}")
    Call<GenericResponse> changeNotifyPriority(@Path("phoneNumber") String phoneNumber, @Path("code") String code,
                                               @Query("alertPreference") String alertPreference);

    //Change user language (for SMSs)
    @GET("user/profile/settings/language/{phoneNumber}/{code}")
    Call<GenericResponse> changeUserLanguage(@Path("phoneNumber") String phoneNumber, @Path("code") String code,
                                             @Query("language") String languageKey);

    @GET("gcm/deregister/{phoneNumber}/{code}")
    Call<GenericResponse> pushUnregister(@Path("phoneNumber") String phoneNumber,
        @Path("code") String code);

    // send a chat message (cannot trust GCM to handle this leg of journey)
    @GET("gcm/chat/send/{phoneNumber}/{code}/{groupUid}")
    Call<GenericResponse> sendChatMessage(@Path("phoneNumber") String phoneNumber, @Path("code") String code,
                                          @Path("groupUid") String groupUid, @Query("messageText") String message,
                                          @Query("messageUid") String messageUid, @Query("gcmKey") String gcmKey);

    // update notification read status (for single notification, on open & view via click)
    @POST("notification/update/read/{phoneNumber}/{code}")
    Call<GenericResponse> updateRead(@Path("phoneNumber") String phoneNumber,
                                     @Path("code") String code,
                                     @Query("uid") String uid);

    // update notifications in a batch, e.g., after scrolling through a list of them
    @POST("notification/update/read/batch/{phoneNumber}/{code}")
    Call<GenericResponse> updateReadBatch(@Path("phoneNumber") String phoneNumber,
                                          @Path("code") String code,
                                          @Query("read") boolean read,
                                          @Query("notificationUids") List<String> notificationUids);

    //check if server connection is working (for online/offline switching)
    @GET("user/connect/{phoneNumber}/{code}")
    Call<GenericResponse> testConnection(@Path("phoneNumber") String phoneNumber, @Path("code") String code);

    /*
    SECTION : Group related calls
     */
    @POST("group/create/{phoneNumber}/{code}/{groupName}/{description}")
    Call<GroupResponse> createGroup(@Path("phoneNumber") String phoneNumber, @Path("code") String code,
                                    @Path("groupName") String groupName, @Path("description") String groupDescription,
                                    @Body List<Member> membersToAdd);

    @Multipart
    @POST("group/image/upload/{phoneNumber}/{code}/{groupUid}")
    Call<GroupResponse> uploadImage(@Path("phoneNumber") String phoneNumber, @Path("code") String code, @Path("groupUid") String groupUid,
        @Part  MultipartBody.Part image);
    
    @POST("group/image/default/{phoneNumber}/{code}")
    Call<GroupResponse> changeDefaultImage(@Path("phoneNumber") String phoneNumber, @Path("code") String code,
                                             @Query("groupUid") String groupUid, @Query("defaultImage") String defaultImage);

    //user groups
    @GET("group/list/{phoneNumber}/{code}")
    Call<GroupsChangedResponse> getUserGroups(@Path("phoneNumber") String phoneNumber,
        @Path("code") String code);

    @GET("group/list/{phoneNumber}/{code}")
    Call<GroupsChangedResponse> getUserGroupsChangedSince(@Path("phoneNumber") String phoneNumber,
                                                          @Path("code") String code,
                                                          @Query("changedSince") Long changedSince);

    //refresh group members
    @GET("group/members/list/{phoneNumber}/{code}/{groupUid}")
    Call<MemberListResponse> fetchCurrentGroupMembers(@Path("phoneNumber") String phoneNumber,
                                                      @Path("code") String code,
                                                      @Path("groupUid") String groupUid);

    // leave a group
    @POST("group/members/unsubscribe/{phoneNumber}/{code}")
    Call<GenericResponse> unsubscribeFromGroup(@Path("phoneNumber") String phoneNumber,
                                               @Path("code") String code,
                                               @Query("groupUid") String groupUid);

    //search for public groups
    @GET("group/search/{phoneNumber}/{code}")
    Call<GroupSearchResponse> search(@Path("phoneNumber") String phoneNumber,
                                     @Path("code") String code,
                                     @Query("searchTerm") String searchTerm,
                                     @Query("onlySearchNames") boolean searchNamesOnly,
                                     @Query("searchByLocation") boolean searchByLocation,
                                     @Query("searchRadius") Integer searchRadius);

    //group join request
    @POST("group/join/request/{phoneNumber}/{code}")
    Call<JoinRequestResponse> sendGroupJoinRequest(@Path("phoneNumber") String phoneNumber,
                                                   @Path("code") String code,
                                                   @Query("uid") String uid, @Query("message") String message);

    //find open join requests assigned to this user
    @GET("group/join/list/{phoneNumber}/{code}")
    Call<RealmList<GroupJoinRequest>> getOpenJoinRequests(@Path("phoneNumber") String phoneNumber,
                                                          @Path("code") String code);

    // send join request response
    @POST("group/join/respond/{phoneNumber}/{code}")
    Call<GenericResponse> respondToJoinRequest(@Path("phoneNumber") String phoneNumber,
                                               @Path("code") String code,
                                               @Query("requestUid") String requestUid,
                                               @Query("response") String response);

    // for both of these, maintaining local requestUid in theory cleaner, but requires maintaining
    // them locally, and sending back with group search, both of which add too much complexity (since requests are unique to requestor-group pair)

    // cancel a join request previously sent
    @POST("group/join/request/cancel/{phoneNumber}/{code}")
    Call<GenericResponse> cancelJoinRequest(@Path("phoneNumber") String phoneNumber,
                                            @Path("code") String code,
                                            @Query("groupUid") String groupUid);

    // request to send a reminder for a previously sent join request
    @POST("group/join/request/remind/{phoneNumber}/{code}")
    Call<GenericResponse> remindJoinRequest(@Path("phoneNumber") String phoneNumber,
                                            @Path("code") String code,
                                            @Query("groupUid") String groupUid);

    // add members to a group
    @Headers("Content-Type: application/json")
    @POST("group/members/add/{phoneNumber}/{code}/{uid}")
    Call<GroupResponse> addGroupMembers(@Path("uid") String groupUid, @Path("phoneNumber") String phoneNumber,
        @Path("code") String code, @Body List<Member> membersToAdd);

    // remove members from a group
    @POST("group/members/remove/{phoneNumber}/{code}/{groupUid}")
    Call<GenericResponse> removeGroupMembers(@Path("phoneNumber") String phoneNumber, @Path("code") String code,
        @Path("groupUid") String groupUid, @Query("memberUids") Set<String> memberUids);

    // combine several common changes (add calls here, or separate calls, as needed
    @POST("group/edit/multi/{phoneNumber}/{code}/{groupUid}")
    Call<GroupResponse> combinedGroupEdits(@Path("phoneNumber") String phoneNumber, @Path("code") String code, @Path("groupUid") String groupUid,
                                           @Query("name") String newName, @Query("description") String description,
                                           @Query("resetImage") boolean resetGroupImage, @Query("dfltImageName") String imageName,
                                           @Query("changePublicPrivate") boolean changePubPriv, @Query("isPublic") boolean changeToPublic,
                                           @Query("closeJoinCode") boolean closeJoinCode, @Query("membersToRemove") List<String> memberUids,
                                           @Query("organizersToAdd") List<String> newOrganizerUids);

    //fetch group chat settings of user
    @GET("group/messenger/fetch_settings/{phoneNumber}/{code}/{groupUid}")
    Call<GroupChatSettingResponse> fetchGroupMessengerSettings(@Path("phoneNumber") String phoneNumber, @Path("code") String code, @Path("groupUid") String groupUid, @Query("userUid") String userUid);

    //update user group chat settings mute and unmute
    @POST("group/messenger/update/{phoneNumber}/{code}/{groupUid}")
    Call<GenericResponse> updateUserGroupChatSettings(@Path("phoneNumber") String phoneNumber, @Path("code") String code,
                                                      @Path("groupUid") String groupUid, @Query("userUid") String userUid,
                                                      @Query("active") boolean active, @Query("userInitiated") boolean userInitated);
    //request ping from server
    @GET("group/messenger/ping/{phoneNumber}/{code}/{groupUid}")
    Call<GenericResponse> requestPing(@Path("phoneNumber") String phoneNumber, @Path("code") String code, @Path("groupUid") String groupUid);

    @POST("group/messenger/mark_read/{phoneNumber}/{code}/{groupUid}")
    Call<GenericResponse> markAsRead(@Path("phoneNumber") String phoneNumber, @Path("code") String code,
                                     @Path("groupUid") String groupUid, @Query("messageUids") Set<String> messageUid);


        /*
        SECTION: Fetch tasks, and task details
         */

    // get all the tasks for a user
    @GET("task/list/{phoneNumber}/{code}")
    Call<TaskResponse> getUserTasks(@Path("phoneNumber") String phoneNumber, @Path("code") String code);

    // get all the tasks for a user, including those cancelled, since last fetch
    @GET("task/list/since/{phoneNumber}/{code}")
    Call<TaskChangedResponse> getUpcomingTasksAndCancellations(@Path("phoneNumber") String phoneNumber,
                                                               @Path("code") String code,
                                                               @Query("changedSince") long changeedSinceMillis);

    // get all the tasks for a group
    @GET("task/list/{phoneNumber}/{code}/{parentUid}")
    Call<TaskChangedResponse> getGroupTasks(@Path("phoneNumber") String phoneNumber, @Path("code") String code,
        @Path("parentUid") String groupUid);

    // fetch a task (of any type)
    @GET("task/fetch/{phoneNumber}/{code}/{taskUid}/{taskType}")
    Call<TaskResponse> fetchTaskEntity(@Path("phoneNumber") String phoneNumber, @Path("code") String code,
        @Path("taskUid") String taskUid, @Path("taskType") String taskType);

    // get meeting RSVP list
    @GET("meeting/rsvps/{phoneNumber}/{code}/{meetingUid}")
    Call<RsvpListModel> fetchMeetingRsvps(@Path("phoneNumber") String phoneNumber, @Path("code") String code,
        @Path("meetingUid") String meetingUid);

    @GET("vote/totals/{phoneNumber}/{code}/{voteUid}")
    Call<ResponseTotalsModel> fetchVoteTotals(@Path("phoneNumber") String phoneNumber, @Path("code") String code,
        @Path("voteUid") String voteUid);

        /*
        SECTION: RESPOND TO TASKS
         */

    //cast vote
    @GET("vote/do/{id}/{phoneNumber}/{code}")
    Call<TaskResponse> castVote(@Path("id") String voteId,
        @Path("phoneNumber") String phoneNumber,
        @Path("code") String code,
        @Query("response") String response);

    //rsvp for a meeting
    @GET("meeting/rsvp/{id}/{phoneNumber}/{code}")
    Call<TaskResponse> rsvp(@Path("id") String voteId,
        @Path("phoneNumber") String phoneNumber,
        @Path("code") String code,
        @Query("response") String response);

    //complete to-do
    @GET("todo/complete/{phoneNumber}/{code}/{id}")
    Call<TaskResponse> completeTodo(@Path("phoneNumber") String phoneNumber,
        @Path("code") String code,
        @Path("id") String todoId);

        /*
        SECTION : CREATE TASKS
         */


    //create vote
    @POST("vote/create/{id}/{phoneNumber}/{code}")
    Call<TaskResponse> createVote(@Path("phoneNumber") String phoneNumber, @Path("code") String code,
        @Path("id") String groupId, @Query("title") String title,
        @Query("description") String description,
        @Query("closingTime") String closingTime,
        @Query("reminderMins") int minutes,
        @Query("members") Set<String> members, @Query("notifyGroup") boolean relayable);

    // create meeting
    @POST("meeting/create/{phoneNumber}/{code}/{parentUid}")
    Call<TaskResponse> createMeeting(@Path("phoneNumber") String phoneNumber, @Path("code") String code,
        @Path("parentUid") String parentUid, @Query("title") String title,
        @Query("description") String description,
        @Query("eventStartDateTime") String dateTimeISO,
        @Query("reminderMinutes") int reminderMinutes,
        @Query("location") String location,
        @Query("members") Set<String> memberUids);


    // create to-do
    @POST("todo/create/{phoneNumber}/{code}/{parentUid}")
    Call<TaskResponse> createTodo(@Path("phoneNumber") String phoneNumber, @Path("code") String code,
        @Path("parentUid") String parentUid, @Query("title") String title,
        @Query("description") String description,
        @Query("dueDate") String dueDate,
        @Query("reminderMinutes") int reminderMinutes,
        @Query("members") Set<String> membersAssigned);

        /*
        SECTION : EDIT TASKS
         */

    //edit vote
    @POST("vote/update/{uid}/{phoneNumber}/{code}")
    Call<TaskResponse> editVote(@Path("phoneNumber") String phoneNumber, @Path("code") String code,
        @Path("uid") String id,
        @Query("title") String title,
        @Query("description") String description,
        @Query("closingTime") String closingTime);

    //edit meeting
    @POST("meeting/update/{phoneNumber}/{code}/{meetingUid}")
    Call<TaskResponse> editMeeting(@Path("phoneNumber") String phoneNumber, @Path("code") String code,
        @Path("meetingUid") String uid,
        @Query("title") String title,
        @Query("description") String description,
        @Query("location") String location,
        @Query("startTime") String startTime,
        @Query("members") List<String> assignedMemberUids);

    //edit action
    @POST("todo/update/{phoneNumber}/{code}/{taskUid}")
    Call<TaskResponse> editTodo(@Path("phoneNumber") String phoneNumber, @Path("code") String code,
                                @Path("taskUid") String taskUid, @Query("title") String title,
                                @Query("description") String description, @Query("dueDate") String dueDate,
                                @Query("members") Set<String> membersAssigned);

    @GET("task/assigned/{phoneNumber}/{code}/{taskUid}/{taskType}")
    Call<MemberListResponse> fetchAssignedMembers(@Path("phoneNumber") String phoneNumber, @Path("code") String code,
                                                  @Path("taskUid") String taskUid, @Path("taskType") String taskType);

    // should clearly consolidate / abstract / simplify these to one call in future versions

    //cancel meeting
    @POST("meeting/cancel/{phoneNumber}/{code}")
    Call<GenericResponse> cancelMeeting(@Path("phoneNumber") String phoneNumber, @Path("code") String code,
        @Query("uid") String uid);

    //cancel vote
    @POST("vote/cancel/{phoneNumber}/{code}")
    Call<GenericResponse> cancelVote(@Path("phoneNumber") String phoneNumber, @Path("code") String code,
        @Query("uid") String uid);

    //cancel to-do
    @POST("todo/cancel/{phoneNumber}/{code}")
    Call<GenericResponse> cancelTodo(@Path("phoneNumber") String phoneNumber, @Path("code") String code,
                                     @Query("todoUid") String todoUid);

    /* SECTION : EDIT GROUPS */

    @POST("group/edit/rename/{phoneNumber}/{code}")
    Call<GenericResponse> renameGroup(@Path("phoneNumber") String phoneNumber, @Path("code") String code,
                                      @Query("groupUid") String groupUid, @Query("name") String name);

    @POST("group/edit/description/{phoneNumber}/{code}")
    Call<GenericResponse> changeGroupDesc(@Path("phoneNumber") String phoneNumber, @Path("code") String code,
                                          @Query("groupUid") String groupUid, @Query("description") String description);

    @POST("group/edit/public_switch/{phoneNumber}/{code}")
    Call<GenericResponse> switchGroupPublicPrivate(@Path("phoneNumber") String phoneNumber, @Path("code") String code,
                                                   @Query("groupUid") String groupUid, @Query("state") boolean state);

    @POST("group/edit/open_join/{phoneNumber}/{code}")
    Call<GenericResponse> openJoinCode(@Path("phoneNumber") String phoneNumber, @Path("code") String code,
                                       @Query("groupUid") String groupUid);

    @POST("group/edit/close_join/{phoneNumber}/{code}")
    Call<GenericResponse> closeJoinCode(@Path("phoneNumber") String phoneNumber, @Path("code") String code,
                                        @Query("groupUid") String groupUid);

    @POST("group/edit/add_organizer/{phoneNumber}/{code}")
    Call<GenericResponse> addOrganizer(@Path("phoneNumber") String phoneNumber, @Path("code") String code,
                                       @Query("groupUid") String groupUid, @Query("memberUid") String memberUid);

    @POST("group/edit/fetch_permissions/{phoneNumber}/{code}")
    Call<PermissionResponse> fetchPermissions(@Path("phoneNumber") String phoneNumber, @Path("code") String code,
                                              @Query("groupUid") String groupUid, @Query("roleName") String roleName);

    @POST("group/edit/update_permissions/{phoneNumber}/{code}/{groupUid}/{roleName}")
    Call<GenericResponse> updatePermissions(@Path("phoneNumber") String phoneNumber, @Path("code") String code,
                                            @Path("groupUid") String groupUid, @Path("roleName") String roleName,
                                            @Body List<Permission> permissions);

    @POST("group/edit/change_role/{phoneNumber}/{code}")
    Call<GenericResponse> changeMemberRole(@Path("phoneNumber") String phoneNumber, @Path("code") String code,
                                           @Query("groupUid") String groupUid, @Query("memberUid") String memberUid,
                                           @Query("roleName") String newRole);

    /*
    SECTION: Grassroot Extra settings, notifications, etc
     */
    @GET("account/settings/fetch/{phoneNumber}/{code}")
    Call<AccountResponse> getGrassrootExtraSettings(@Path("phoneNumber") String phoneNumber,
                                                    @Path("code") String code);

    @GET("account/payment/signup/initiate/{phoneNumber}/{code}")
    Call<RestResponse<AccountBill>> initiateAccountSignup(@Path("phoneNumber") String phoneNumber,
                                                          @Path("code") String code,
                                                          @Query("accountName") String accountName,
                                                          @Query("billingEmail") String billingEmail,
                                                          @Query("accountType") String accountType);

    @GET("account/groups/add/{phoneNumber}/{code}")
    Call<GenericResponse> addGroupToAccount(@Path("phoneNumber") String phoneNumber,
                                            @Path("code") String code,
                                            @Query("accountUid") String accountUid,
                                            @Query("groupUid") String groupUid);

    @GET("account/message/send/{phoneNumber}/{code}")
    Call<GenericResponse> sendFreeForm(@Path("phoneNumber") String phoneNumber,
                                       @Path("code") String code,
                                       @Query("accountUid") String accountUid,
                                       @Query("groupUid") String groupUid,
                                       @Query("message") String message);

  }
}