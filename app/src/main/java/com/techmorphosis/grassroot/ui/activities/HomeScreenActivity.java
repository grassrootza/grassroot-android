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
import com.techmorphosis.grassroot.ui.fragments.NotificationDialog;
import com.techmorphosis.grassroot.ui.fragments.AlertDialogFragment;
import com.techmorphosis.grassroot.ui.fragments.HomeGroupListFragment;
import com.techmorphosis.grassroot.ui.fragments.NavigationDrawerFragment;
import com.techmorphosis.grassroot.ui.fragments.WelcomeFragment;
import com.techmorphosis.grassroot.utils.Constant;
import com.techmorphosis.grassroot.utils.PreferenceUtils;
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
        mainFragment = PreferenceUtils.getisHasgroup(this) ? new HomeGroupListFragment() : new WelcomeFragment();
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
                Intent shareapp = new Intent("android.intent.action.SEND");
                shareapp.setType("text/plain");
                shareapp.setAction("android.intent.action.SEND");
                shareapp.putExtra("android.intent.extra.TEXT", getString(R.string.share_app_text));
                startActivity(Intent.createChooser(shareapp, "Share via.."));
                break;
            case 4:
                //Rate App
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


    private void logout() {
        alertDialogFragment = UtilClass.showAlertDialog(getFragmentManager(), getString(R.string.Logout_message), "Yes", "No", true, new AlertDialogListener() {
            @Override
            public void setRightButton() {//no
                alertDialogFragment.dismiss();
            }

            @Override
            public void setLeftButton() {
                //Yes
                PreferenceUtils.clearAll(getApplicationContext());
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

        if (PreferenceUtils.getPrefHasSaveClicked(this)) {
            PreferenceUtils.setPrefHasSaveClicked(this, false);
        }


    }
}
