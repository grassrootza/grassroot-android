/**
 * Copyright 2015 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.techmorphosis.grassroot.Service;

import android.app.IntentService;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;
import com.techmorphosis.grassroot.R;
import com.techmorphosis.grassroot.utils.SettingPreference;

import java.io.IOException;

public class GcmRegistrationService extends IntentService {

    private static final String TAG = "RegIntentService";
    private static final String[] TOPICS = {"global"};

    public GcmRegistrationService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.e(TAG,"onHandleIntent is called");

        try {

            InstanceID instanceID = InstanceID.getInstance(this);
            String token = instanceID.getToken(getString(R.string.sender_id), GoogleCloudMessaging.INSTANCE_ID_SCOPE, null);

            Log.e(TAG, "GCM Registration Token: " + token);

            // TODO: Implement this method to send any registration to your app's servers.
            sendRegistrationtokenToServer2(token);


    } catch (Exception e) {
            Log.e(TAG, "Failed to complete token refresh", e);
             SettingPreference.setPrefGcmSentTokenToServer(getApplicationContext(),false);

        }

        Log.e(TAG, "All done");
    }

    private void sendRegistrationtokenToServer2(final String token) {

        /*NetworkCall networkCall = new NetworkCall
                (
                        getApplicationContext(),
                        new ResponseListenerVolley() {
                            @Override
                            public void onSuccess(String response) {
                                try {
                                    JSONObject jsonObject = new JSONObject(response);
                                    if (jsonObject.getString("status").equalsIgnoreCase("SUCCESS")) {
                                        Log.e(TAG, "onSuccess ");
                                        SettingPreffrence.setPREF_Gcmtoken(getApplicationContext(), token);
                                        SettingPreffrence.setPrefGcmSentTokenToServer(getApplicationContext(), true);
                                    }
                                    else {
                                        SettingPreffrence.setPREF_Gcmtoken(getApplicationContext(), "");
                                        SettingPreffrence.setPrefGcmSentTokenToServer(getApplicationContext(), false);
                                    }
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                    Log.e(TAG,"JSONException occured " + e.getMessage());
                                    SettingPreffrence.setPREF_Gcmtoken(getApplicationContext(), "");
                                    SettingPreffrence.setPrefGcmSentTokenToServer(getApplicationContext(), false);
                                }



                            }
                        },
                        new ErrorListenerVolley() {
                            @Override
                            public void onError(VolleyError volleyError) {

                                Log.e(TAG,"onError");
                                SettingPreffrence.setPREF_Gcmtoken(getApplicationContext(), "");
                                SettingPreffrence.setPrefGcmSentTokenToServer(getApplicationContext(), false);

                            }
                        },
                        AllLinsks.gcm_register + SettingPreffrence.getPREF_Phone_Token(getApplicationContext()),
                        "",
                        false
                );

        HashMap<String,String> hashMap = new HashMap();
        hashMap.put("registration_id", token);
        networkCall.makeStringRequest_POST(hashMap);*/
    }


    private void sendRegistrationToServer(final String token) {
        // Add custom implementation, as needed.
        final GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(getApplicationContext());
        final String senderId = getString(R.string.sender_id);
        int Id = 1;
        Integer id=(int) (Math.random() * Integer.parseInt(String.valueOf(Id)));

        final String msgId = String.valueOf(id);
        final Bundle data = new Bundle();

        data.putString("registration_id", token);


        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... params) {
                try {
                    gcm.send(senderId + "@gcm.googleapis.com", msgId, data);

                    Log.e(TAG, "Successfully sent upstream message");
                    return null;
                } catch (IOException ex) {
                    Log.e(TAG, "Error sending upstream message", ex);
                    return "Error sending upstream message:" + ex.getMessage();
                }
            }

            @Override
            protected void onPostExecute(String result) {

                if (result != null) {
                    Log.e(TAG,"send Failed : " + result);
                } else {
                    Log.e(TAG,"result is  fine: " + result);
                    SettingPreference.setPREF_Gcmtoken(getApplicationContext(), token);
                }
            }
        }.execute(null, null, null);
    }


}
