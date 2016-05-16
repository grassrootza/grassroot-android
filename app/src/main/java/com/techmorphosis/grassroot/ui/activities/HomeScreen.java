package com.techmorphosis.grassroot.ui.activities;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.Gravity;

import com.techmorphosis.grassroot.R;
import com.techmorphosis.grassroot.ui.DialogFragment.AlertDialogFragment;
import com.techmorphosis.grassroot.ui.fragments.Group_Homepage;
import com.techmorphosis.grassroot.ui.fragments.NavigationDrawerFragment;
import com.techmorphosis.grassroot.ui.fragments.RateUsFragment;
import com.techmorphosis.grassroot.ui.fragments.WelcomeFragment;
import com.techmorphosis.grassroot.utils.SettingPreffrence;
import com.techmorphosis.grassroot.utils.UtilClass;
import com.techmorphosis.grassroot.utils.listener.AlertDialogListener;

public class HomeScreen extends PortraitActivity implements NavigationDrawerFragment.NavigationDrawerCallbacks,WelcomeFragment.FragmentCallbacks,Group_Homepage.FragmentCallbacks{

    private DrawerLayout drawer;
   android.support.v4.app.Fragment fragment = null;
    private String openFragment;
    public  String TAG="HomeScreen";
    AlertDialogFragment alertDialogFragment;
    UtilClass utilClass;
    private NavigationDrawerFragment drawerFrag;
    private Snackbar homesnackbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_homescreen);
       // registeringReceiver();

        drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawerFrag = (NavigationDrawerFragment) getFragmentManager().findFragmentById(R.id.navigation_drawer);
        drawerFrag.setUp(R.id.navigation_drawer, drawer);
        utilClass = new UtilClass();
       // Snackbar.make(drawer, "hello", Snackbar.LENGTH_INDEFINITE).show();
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
    /*    else
        {
            //user has already done rate us
            Toast.makeText(getApplicationContext(),"rate done ",Toast.LENGTH_LONG);
            Log.e(TAG, "RATE DONE " );
        }*/

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
                SettingPreffrence.clearAll(getApplicationContext());
                Intent open= new Intent(HomeScreen.this,StartActivity.class);
                startActivity(open);
                finish();
                alertDialogFragment.dismiss();

            }
        });
    }

    private void HomeFragment() {
        if (SettingPreffrence.getisHasgroup(HomeScreen.this))
        {
            fragment = new Group_Homepage();
            openFragment="Group_Homepage";
        }
        else
        {
            fragment = new WelcomeFragment();
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
                fragment = new WelcomeFragment();
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




    }

    @Override
    public void onBackPressed() {
//        super.onBackPressed();

        if (getFragmentManager().getBackStackEntryCount() > 1)
        {

        //popup fragment first
            getFragmentManager().popBackStackImmediate();

            if (getSupportFragmentManager().findFragmentById(R.id.fragment_container) instanceof Group_Homepage) {

                drawerFrag.updateNotificationDrawers(0);
            }
            else if (getSupportFragmentManager().findFragmentById(R.id.fragment_container) instanceof WelcomeFragment) {

                drawerFrag.updateNotificationDrawers(0);

            }
            /*else if (getSupportFragmentManager().findFragmentById(R.id.fragment_container) instanceof Group_Homepage) {

            }*/
        }
        else
        {
            super.onBackPressed();
        }
    }

/*
    public void showSnackbar(String message,int length,String actionButtontxt) {


        homesnackbar = Snackbar.make(drawer, message, length);
        homesnackbar.setActionTextColor(Color.RED);
        if (!TextUtils.isEmpty(actionButtontxt)) {

            if (actionButtontxt.equalsIgnoreCase(getString(R.string.Logout))) {
                homesnackbar.setAction(actionButtontxt, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        logout();
                    }
                });

            }
            else
            {
                homesnackbar.setAction(actionButtontxt, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Group_Homepage fragment = (Group_Homepage) getSupportFragmentManager().findFragmentByTag("Group_Homepage");
                        fragment.UserGroupWS();
                    }
                });
            }

        }

        homesnackbar.show();
    }
*/


}
