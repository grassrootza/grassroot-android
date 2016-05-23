package com.techmorphosis.grassroot.ui.activities;

import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Toast;

import com.android.volley.NoConnectionError;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.techmorphosis.grassroot.Network.AllLinsks;
import com.techmorphosis.grassroot.Network.NetworkCall;
import com.techmorphosis.grassroot.R;
import com.techmorphosis.grassroot.Service.GcmRegistrationService;
import com.techmorphosis.grassroot.Service.QuickstartPreferences;
import com.techmorphosis.grassroot.ui.DialogFragment.AlertDialogFragment;
import com.techmorphosis.grassroot.ui.DialogFragment.NotificationDialog;
import com.techmorphosis.grassroot.ui.fragments.Group_Homepage;
import com.techmorphosis.grassroot.ui.fragments.NavigationDrawerFragment;
import com.techmorphosis.grassroot.ui.fragments.NewWelcomeFragment;
import com.techmorphosis.grassroot.ui.fragments.RateUsFragment;
import com.techmorphosis.grassroot.utils.SettingPreffrence;
import com.techmorphosis.grassroot.utils.UtilClass;
import com.techmorphosis.grassroot.utils.listener.AlertDialogListener;
import com.techmorphosis.grassroot.utils.listener.ErrorListenerVolley;
import com.techmorphosis.grassroot.utils.listener.ResponseListenerVolley;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class HomeScreen extends PortraitActivity implements NavigationDrawerFragment.NavigationDrawerCallbacks,NewWelcomeFragment.NewFragmentCallbacks,Group_Homepage.FragmentCallbacks{


    private NotificationDialog alertDialogFragment22;
    private boolean isReceiverRegistered;
    private BroadcastReceiver mRegistrationBroadcastReceiver;
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    private DrawerLayout drawer;
   android.support.v4.app.Fragment fragment = null;
    private String openFragment;
    public  String TAG="HomeScreen";
    AlertDialogFragment alertDialogFragment;
    UtilClass utilClass;
    private NavigationDrawerFragment drawerFrag;
    private Snackbar homesnackbar;
    private Snackbar snackbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_homescreen);
       // registeringReceiver();

        drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawerFrag = (NavigationDrawerFragment) getFragmentManager().findFragmentById(R.id.navigation_drawer);
        drawerFrag.setUp(R.id.navigation_drawer, drawer);
        utilClass = new UtilClass();

        if (checkPlayServices()) {
            if (!SettingPreffrence.getPrefGcmSentTokenToServer(HomeScreen.this) && SettingPreffrence.getPREF_Gcmtoken(HomeScreen.this).isEmpty()) {
                Intent intent = new Intent(this, GcmRegistrationService.class);
                startService(intent);

            } else {
                Log.e(TAG,"GCm else ");
                Log.e(TAG,"Server else " + SettingPreffrence.getPrefGcmSentTokenToServer(HomeScreen.this));
                Log.e(TAG,"token else " + SettingPreffrence.getPREF_Gcmtoken(HomeScreen.this).isEmpty());
            }

        }



        HomeFragment();
        hasUserRatedApp();

    }

/*    private void registeringReceiver() {
        IntentFilter intentfilter = new IntentFilter();
        intentfilter.addAction(getString(R.string.Logout));
        registerReceiver(broadcastReceiver, intentfilter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(broadcastReceiver);
    }

    BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            logout();
        }
    };*/


    private void hasUserRatedApp()
    {


        if (!SettingPreffrence.getisRateus(getApplicationContext()))
        {
            //if user does not do rate us yet

            int rateuscount = SettingPreffrence.getIsRateuscounter(getApplicationContext());
            if (rateuscount == -1)
            {
                SettingPreffrence.setIsRateuscounter(HomeScreen.this, 1);
                Log.e(TAG, "counter is firsttime");

            }
            else
            {
                rateuscount++;
                SettingPreffrence.setIsRateuscounter(HomeScreen.this, rateuscount);

                if (rateuscount%15==0)
                {
                    //user attempt to open 15 time
                    Log.e(TAG,"IF rateuscount is " + rateuscount);
                   // Toast.makeText(getApplicationContext(),"rateuscount is " + rateuscount,Toast.LENGTH_LONG);
                    RateUsFragment rateUsFragment = new RateUsFragment();
                    rateUsFragment.show(getFragmentManager(),"RateUsFragment");

                }
                else
                {
                  //  Toast.makeText(getApplicationContext(),"rateuscount is " + rateuscount,Toast.LENGTH_LONG);
                    Log.e(TAG,"ELSE rateuscount is " + rateuscount);
                }

            }


        }

    }


    @Override
    public void onNavigationDrawerItemSelected(int position) {

        if (drawer != null) drawer.closeDrawer(Gravity.LEFT);
        fragment=null;

            switch(position)
            {
                case 0:
                  //Profile

                    Intent profile = new Intent(getApplicationContext(),ProfileSettings.class);
                    startActivity(profile);


                    break;


                case 1:
                    //FAQ
                   /* fragment = new FAQActivity();
                    openFragment="FAQActivity";*/
                    Intent faq= new Intent(HomeScreen.this,FAQActivity.class);
                    startActivity(faq);
                    break;

                case 2:
                    //NotificationCenter
                    Intent noifications = new Intent(HomeScreen.this,NotificationCenter.class);
                    startActivity(noifications);
                    SettingPreffrence.setIsNotificationcounter(HomeScreen.this,0);

                    break;

                case 3:
                    //Share

                    //implicit Intent
                    Intent shareapp= new Intent("android.intent.action.SEND");
                    shareapp.setType("text/plain");
                    shareapp.setAction("android.intent.action.SEND");
                    shareapp.putExtra("android.intent.extra.TEXT", getString(R.string.share_app_text));
                    startActivity(Intent.createChooser(shareapp,"Share via.."));

                    break;

                case 4:
                    //Rate App

                    //implicit Intent
                    try {
                        Intent rateapp = new Intent("android.intent.action.VIEW", Uri.parse("market://details?id=com.techmorphosis.grassroot"));
                        startActivity(rateapp);
                    } catch (ActivityNotFoundException activitynotfoundexception) {
                        Intent rateapp2 = new Intent("android.intent.action.VIEW" ,Uri.parse("https://play.google.com/store/apps/details?id=com.techmorphosis.grassroot"));
                        startActivity(rateapp2);
                    }


                    break;

                case  5:
                    logout();
                  break;


            }

        if (fragment != null)
        {

            android.support.v4.app.FragmentManager fragmentManager1 = getSupportFragmentManager();
            android.support.v4.app.FragmentTransaction ft1 = fragmentManager1.beginTransaction();
            ft1.replace(R.id.fragment_container, fragment);
            ft1.commit();

        }
        else
        {
            Log.e("Error", "Error in creating fragment");
        }
    }

    private void logout()
    {
        alertDialogFragment =utilClass.showAlerDialog(getFragmentManager(), getString(R.string.Logout_message), "Yes","No",true, new AlertDialogListener()
        {
            @Override
            public void setRightButton()
            {//no


                alertDialogFragment.dismiss();
            }

            @Override
            public void setLeftButton()
            {
                //Yes

                alertDialogFragment.dismiss();


                if (SettingPreffrence.getPREF_Gcmtoken(HomeScreen.this).isEmpty() && SettingPreffrence.getPrefGcmSentTokenToServer(HomeScreen.this)) {

                    clearAll();
                } else {

                    logoutWS();
                }
            }
        });
    }

    private void clearAll() {
        SettingPreffrence.clearAll(getApplicationContext());
        Intent open = new Intent(HomeScreen.this, StartActivity.class);
        startActivity(open);
        finish();

    }

    private void logoutWS() {

        NetworkCall networkCall = new NetworkCall
                (
                        HomeScreen.this,
                        new ResponseListenerVolley() {
                            @Override
                            public void onSuccess(String s) {

                                try {
                                    JSONObject jsonObject = new JSONObject(s);
                                    if (jsonObject.getString("status").equalsIgnoreCase("SUCCESS")) {
                                        clearAll();
                                      //  Log.e(TAG,"if SettingPreffrence.getPREF_Gcmtoken(HomeScreen.this) is " + SettingPreffrence.getPREF_Gcmtoken(HomeScreen.this));
                                    }
                                    else {
                                        clearAll();
                                        //Log.e(TAG, "else SettingPreffrence.getPREF_Gcmtoken(HomeScreen.this) is " + SettingPreffrence.getPREF_Gcmtoken(HomeScreen.this));

                                    }

                                } catch (JSONException e) {
                                    e.printStackTrace();

                                }
                            }
                        },
                        new ErrorListenerVolley() {
                            @Override
                            public void onError(VolleyError volleyError) {
                                if ((volleyError instanceof NoConnectionError) || (volleyError instanceof TimeoutError)) {

                                    showSnackbar(getString(R.string.No_network),snackbar.LENGTH_INDEFINITE,getString(R.string.Retry));
                                } else {
                                    clearAll();
                                    //Log.e(TAG, "else SettingPreffrence.getPREF_Gcmtoken(HomeScreen.this) is " + SettingPreffrence.getPREF_Gcmtoken(HomeScreen.this));

                                }

                            }
                        },
                        AllLinsks.gcm_deregister+"/"+SettingPreffrence.getPREF_Phone_Token(HomeScreen.this),
                        "",
                        false
                );

        networkCall.makeStringRequest_GET();

        //Log.e(TAG,"url is " + AllLinsks.DOMAIN2+SettingPreffrence.getPREF_Gcmtoken(HomeScreen.this)+"/deregister"+ "/"+SettingPreffrence.getPREF_Phone_Token(HomeScreen.this));
        Log.e(TAG,"url 1 is "+ AllLinsks.gcm_deregister+"/"+SettingPreffrence.getPREF_Phone_Token(HomeScreen.this));
    }

    private void HomeFragment() {
        if (SettingPreffrence.getisHasgroup(HomeScreen.this))
        {
            fragment = new Group_Homepage();
            openFragment="Group_Homepage";
        }
        else
        {
            fragment = new NewWelcomeFragment();
            openFragment="WelcomeFragment";

        }
        if (fragment != null)
        {

            android.support.v4.app.FragmentManager fragmentManager1 = getSupportFragmentManager();
            android.support.v4.app.FragmentTransaction ft1 = fragmentManager1.beginTransaction();
            ft1.replace(R.id.fragment_container, fragment);
            ft1.commit();

        }


    }

    @Override
    public void menuClick() { // Getting data from fragment
        drawerFrag.clearNotificationDrawersSelection();
        if (drawer != null) drawer.openDrawer(Gravity.LEFT);
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        if (SettingPreffrence.getPrefHasSaveClicked(this)){

            SettingPreffrence.setPrefHasSaveClicked(this, false);

            if (SettingPreffrence.getisHasgroup(HomeScreen.this))
            {
                Log.e("onResume", "Error in creating fragment");

                fragment = new Group_Homepage();
                openFragment="Group_Homepage";
            }
            else
            {
                fragment = new NewWelcomeFragment();
                openFragment="WelcomeFragment";

            }
            if (fragment != null)
            {

                android.support.v4.app.FragmentManager fragmentManager1 = getSupportFragmentManager();
                android.support.v4.app.FragmentTransaction ft1 = fragmentManager1.beginTransaction();
                ft1.replace(R.id.fragment_container, fragment);
                ft1.commit();

            } else
            {
                Log.e("Error", "Error in creating fragment");
            }

        }
        else if (SettingPreffrence.getPREF_HAS_Update(HomeScreen.this))
        {
            Snackbar.make(drawer,getString(R.string.pp_update_msg),Snackbar.LENGTH_SHORT).show();
            drawerFrag.updateNotificationDrawersname();
            SettingPreffrence.setPREF_HAS_Update(HomeScreen.this,false);
        }
       // registerReceiver();

        //drawerFrag.clearNotificationDrawersSelection();



    }

    private void registerReceiver(){
        if(!isReceiverRegistered) {
            LocalBroadcastManager.getInstance(this).registerReceiver(mRegistrationBroadcastReceiver,
                    new IntentFilter(QuickstartPreferences.REGISTRATION_COMPLETE));
            isReceiverRegistered = true;
        }
    }

    @Override
    public void onBackPressed() {

        if (this.drawer.isDrawerOpen(GravityCompat.START)) {
            this.drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }



    }


    private void doGcmSendUpstreamMessage() {
        final GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(getApplicationContext());
        final String senderId = getString(R.string.sender_id);
        final String msgId = "1";
        final Bundle data = new Bundle();

        data.putString("register",SettingPreffrence.getPREF_Gcmtoken(HomeScreen.this));


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
                    Toast.makeText(getApplicationContext(), "result is : " + result, Toast.LENGTH_LONG).show();
                    Log.e(TAG,"result is : " + result);
                }
            }
        }.execute(null, null, null);
    }

    /**
     * Check the device to make sure it has the Google Play Services APK. If
     * it doesn't, display a dialog that allows users to download the APK from
     * the Google Play Store or enable it in the device's system settings.
     */
    private boolean checkPlayServices() {

        Log.e(TAG, "checkPlayServices is called");

        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = apiAvailability.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (apiAvailability.isUserResolvableError(resultCode)) {
                //apiAvailability.getErrorDialog(this, resultCode, PLAY_SERVICES_RESOLUTION_REQUEST).show();
                Log.e(TAG, "This device is  supported.");

            } else {
                Log.e(TAG, "This device is not supported.");
                finish();
            }
            return false;
        }
        return true;
    }


    @Override
    protected void onNewIntent(Intent intent)
    {
        super.onNewIntent(intent);

        Log.e(TAG, "onNewIntent ");

        if (this.drawer.isDrawerOpen(GravityCompat.START)) {
            this.drawer.closeDrawer(GravityCompat.START);
        }

        String body = intent.getStringExtra("body");
        String entity_type = intent.getStringExtra("entity_type");
        String title = intent.getStringExtra("title");
        final String id = intent.getStringExtra("id");

        Log.e(TAG, "body: " + body);
        Log.e(TAG, "title: " + title);
        Log.e(TAG, "entity_type : " + entity_type);
        Log.e(TAG, "id  : " + id);



        alertDialogFragment22 = utilClass.showAlertDialog1(getSupportFragmentManager(), title, body, "Dismiss", "View", false, new AlertDialogListener() {
            @Override
            public void setRightButton() {

               // Log.e(TAG, "setRightButton ");
                alertDialogFragment22.dismiss();
                Intent view_vote = new Intent(HomeScreen.this, ViewVote.class);
                view_vote.putExtra("voteid", id);
                startActivity(view_vote);

            }

            @Override
            public void setLeftButton() {
                //Log.e(TAG,"setLeftButton ");
                alertDialogFragment22.dismiss();
                notificationCounter();

            }
        });
    }

    private void notificationCounter() {

        int notificationcount = SettingPreffrence.getIsNotificationcounter(getApplicationContext());
        Log.e(TAG, "b4 notificationcount is " + notificationcount);

        notificationcount++;
        SettingPreffrence.setIsNotificationcounter(getApplicationContext(), notificationcount);
        Log.e(TAG, "after notificationcount is " + notificationcount);


    }

    public  void  showSnackbar(String msg,int length,String  actionbuttontxt) {
        snackbar = Snackbar.make(drawer, msg, length);
        snackbar.setActionTextColor(Color.RED);

        if (!actionbuttontxt.isEmpty()) {
            snackbar.setAction(actionbuttontxt, new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    logoutWS();
                }
            });
        }
        snackbar.show();
    }
}
