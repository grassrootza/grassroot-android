package com.techmorphosis.grassroot.ui.activities;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.Gravity;

import com.techmorphosis.grassroot.R;
import com.techmorphosis.grassroot.ui.DialogFragment.NotificationDialog;
import com.techmorphosis.grassroot.ui.fragments.AlertDialogFragment;
import com.techmorphosis.grassroot.ui.fragments.HomeGroupListFragment;
import com.techmorphosis.grassroot.ui.fragments.NavigationDrawerFragment;
import com.techmorphosis.grassroot.ui.fragments.WelcomeFragment;
import com.techmorphosis.grassroot.utils.Constant;
import com.techmorphosis.grassroot.utils.ErrorUtils;
import com.techmorphosis.grassroot.utils.SettingPreference;
import com.techmorphosis.grassroot.utils.UtilClass;
import com.techmorphosis.grassroot.interfaces.AlertDialogListener;

import butterknife.BindView;
import butterknife.ButterKnife;

public class HomeScreenActivity extends PortraitActivity implements NavigationDrawerFragment.NavigationDrawerCallbacks,
        WelcomeFragment.FragmentCallbacks, HomeGroupListFragment.FragmentCallbacks {

    private static final String TAG = HomeScreenActivity.class.getCanonicalName();

    @BindView(R.id.drawer_layout)
    DrawerLayout drawer;

    private Fragment mainFragment;
    private NavigationDrawerFragment drawerFrag;
    private AlertDialogFragment alertDialogFragment;
    private NotificationDialog notificationDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.e(TAG, "onCreate for HomeScreenActivity ... ");
        setContentView(R.layout.activity_homescreen);
        drawerFrag = (NavigationDrawerFragment) getFragmentManager().findFragmentById(R.id.navigation_drawer);
        drawerFrag.setUp(R.id.navigation_drawer, drawer);
        ButterKnife.bind(this);
        setUpHomeFragment();
    }

    private void setUpHomeFragment() {
        mainFragment = SettingPreference.getisHasgroup(this) ? new HomeGroupListFragment() : new WelcomeFragment();
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, mainFragment)
                .commit();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK && data != null) {
            if (requestCode == Constant.activityAddMembersToGroup || requestCode == Constant.activityRemoveMembers) {
                Log.e(TAG, "Got a result from add or remove members to group!");
                int groupPosition = data.getIntExtra(Constant.INDEX_FIELD, -1);
                String groupUid = data.getStringExtra(Constant.GROUPUID_FIELD);
                HomeGroupListFragment hgl = (HomeGroupListFragment) mainFragment;
                hgl.updateSingleGroup(groupPosition, groupUid);
            } else if (requestCode == Constant.activityCallMeeting) {
                Log.e(TAG, "Called a meeting! Display the thing");
                HomeGroupListFragment hgl = (HomeGroupListFragment) mainFragment;
                hgl.showSuccessMessage(data);
            }
        }

    }


    @Override
    public void onNavigationDrawerItemSelected(int position) {

        Fragment fragmentToSwitch = null;

        if (drawer != null) {
            drawer.closeDrawer(Gravity.LEFT);
        }

        switch (position) {

            case 0:
                //Profile
                Intent profile = new Intent(getApplicationContext(), ProfileSettings.class);
                startActivity(profile);
                break;


            case 1:
                //faq
                Intent faq = new Intent(HomeScreenActivity.this, FAQActivity.class);
                startActivity(faq);
                break;
            case 2:
                //NotificationCenter
                Intent noifications = new Intent(HomeScreenActivity.this, NotificationCenter.class);
                startActivity(noifications);
                break;
            case 3:

                //Share

                //implicit Intent
                Intent shareapp = new Intent("android.intent.action.SEND");
                shareapp.setType("text/plain");
                shareapp.setAction("android.intent.action.SEND");
                shareapp.putExtra("android.intent.extra.TEXT", getString(R.string.share_app_text));
                startActivity(Intent.createChooser(shareapp, "Share via.."));

                break;


            case 4:
                //Rate App

                //implicit Intent
                try {
                    Intent rateapp = new Intent("android.intent.action.VIEW", Uri.parse("market://details?id=com.techmorphosis.grassroot"));
                    startActivity(rateapp);
                } catch (ActivityNotFoundException activitynotfoundexception) {
                    Intent rateapp2 = new Intent("android.intent.action.VIEW", Uri.parse("https://play.google.com/store/apps/details?id=com.techmorphosis.grassroot"));
                    startActivity(rateapp2);
                }


                break;

            case 5:
                logout();


        }

        if (fragmentToSwitch != null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, fragmentToSwitch)
                    .commit();
        } else {
            Log.e("Error", "Error in creating fragment");
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
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

     /*   if (!intent.hasExtra("update")) {
            notificationDialog = UtilClass.showAlertDialog1(getSupportFragmentManager(), title, body, "Dismiss", "View", false, new AlertDialogListener() {
                @Override
                public void setRightButton() {

                    // Log.e(TAG, "setRightButton ");
                    notificationDialog.dismiss();
                    Intent view_vote = new Intent(HomeScreenActivity.this, ViewVote.class);
                    view_vote.putExtra("voteid", id);
                    startActivity(view_vote);

                }

                @Override
                public void setLeftButton() {
                    //Log.e(TAG,"setLeftButton ");
                    notificationDialog.dismiss();
                    //  notificationCounter();

                }
            });
        }*/
    }

    private void logout() {
        alertDialogFragment = UtilClass.showAlertDialog(getFragmentManager(), getString(R.string.Logout_message), "Yes", "No", true, new AlertDialogListener() {
            @Override
            public void setRightButton() {//no
                alertDialogFragment.dismiss();
            }

            @Override
            public void setLeftButton() {
                //Yes
                SettingPreference.clearAll(getApplicationContext());
                Intent open = new Intent(HomeScreenActivity.this, StartActivity.class);
                startActivity(open);
                finish();
                alertDialogFragment.dismiss();
            }
        });
    }

    @Override
    public void menuClick() { // Getting data from fragment
        if (drawer != null) drawer.openDrawer(GravityCompat.START);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (SettingPreference.getPrefHasSaveClicked(this)) {
            SettingPreference.setPrefHasSaveClicked(this, false);
        }


    }
}
