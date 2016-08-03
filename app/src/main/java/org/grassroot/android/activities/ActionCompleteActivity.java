package org.grassroot.android.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.grassroot.android.R;
import org.grassroot.android.fragments.NewTaskMenuFragment;
import org.grassroot.android.interfaces.GroupConstants;
import org.grassroot.android.models.Group;
import org.grassroot.android.utils.Constant;
import org.grassroot.android.utils.MenuUtils;
import org.grassroot.android.utils.NetworkUtils;
import org.grassroot.android.utils.RealmUtils;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by luke on 2016/07/12.
 * Simple class to display a screen notifying user that task has been complete (to be customized & reused in time)
 * todo : switch this to using the GiantMessageFragment, which is based on this
 */
public class ActionCompleteActivity extends PortraitActivity implements NewTaskMenuFragment.NewTaskMenuListener {

    private static final String TAG = ActionCompleteActivity.class.getSimpleName();

    public static final String HEADER_FIELD = "header";
    public static final String BODY_FIELD = "body";
    public static final String BTN_FIELD = "button";
    public static final String TASK_BUTTONS = "task_buttons";
    public static final String OFFLINE_BUTTONS = "offline_buttons";

    public static final String ACTION_INTENT = "action_intent";
    public static final String HOME_SCREEN = "home_screen";
    public static final String GROUP_SCREEN = "group_screen";

    private boolean customValues;
    private boolean showTaskButtons;
    private boolean showOfflineButtons;

    private String actionIntent;
    private Group groupToPass;

    @BindView(R.id.ac_header) TextView header;
    @BindView(R.id.ac_body) TextView body;
    @BindView(R.id.ac_bt_done) Button done;
    @BindView(R.id.bt_avatar) Button pickAvatar;
    @BindView(R.id.ac_btn_tasks) RelativeLayout taskButtons;
    @BindView(R.id.ac_btn_offline) LinearLayout offlineButtons;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_action_complete);
        ButterKnife.bind(this);

        Bundle args = getIntent().getExtras();
        if (args == null) {
            customValues = false;
            setToDefaultValues();
        } else {
            customValues = true;
            setToArguments(args);
        }
    }

    private void setToDefaultValues() {
        header.setText(R.string.ac_header_default);
        body.setText(R.string.ac_body_default);
        done.setText(R.string.ac_btn_done);
    }

    private void setToArguments(Bundle args) {
        actionIntent = args.getString(ACTION_INTENT);
        groupToPass = args.getParcelable(GroupConstants.OBJECT_FIELD);

        final int headerInt = args.getInt(HEADER_FIELD, R.string.ac_header_default);
        String bodyString = args.getString(BODY_FIELD);
        if (TextUtils.isEmpty(bodyString)) { // getString with default is restricted
            bodyString = getString(R.string.ac_body_default);
        }

        header.setText(headerInt);
        body.setText(bodyString);
        setUpButtons(args);
    }

    private void setUpButtons(Bundle args) {
        // note : cases: either (a) all is fine and we show task buttons
        // or : (b) there was a server error or something and need to show options to go offline
        // or : (c) it's uncertain, and we're showing just the done button
        showTaskButtons = args.getBoolean(TASK_BUTTONS, false);
        showOfflineButtons = args.getBoolean(OFFLINE_BUTTONS, false);

        if (showTaskButtons) {
            done.setVisibility(View.GONE);
            offlineButtons.setVisibility(View.GONE);
            taskButtons.setVisibility(View.VISIBLE);
            if (!NetworkUtils.ONLINE_DEFAULT.equals(RealmUtils.loadPreferencesFromDB().getOnlineStatus())) {
                pickAvatar.setVisibility(View.GONE);
            }
        } else if (showOfflineButtons) {
            done.setVisibility(View.GONE);
            taskButtons.setVisibility(View.GONE);
            offlineButtons.setVisibility(View.VISIBLE);
        } else {
            final int buttonInt = args.getInt(BTN_FIELD, R.string.ac_btn_done);
            done.setText(buttonInt);
            done.setVisibility(View.VISIBLE);
            taskButtons.setVisibility(View.GONE);
            offlineButtons.setVisibility(View.GONE);
        }
    }

    @OnClick(R.id.ac_bt_done)
    public void screenOver() {
        if (!customValues) {
            startActivity(homeIntent());
        } else {
            startActivity(doneIntent());
        }
        finish();
    }

    @OnClick(R.id.bt_avatar)
    public void setAvatar() {
        Intent intent = MenuUtils.constructIntent(this, GroupAvatarActivity.class, groupToPass);
        intent.putExtra(Constant.INDEX_FIELD, 0);
        startActivity(intent);
    }

    @OnClick(R.id.bt_tasks)
    public void newTask() {
        NewTaskMenuFragment fragment = NewTaskMenuFragment.newInstance(groupToPass, true);
        fragment.setShowAddMembers(false);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.ac_root_layout, fragment)
                .addToBackStack(null)
                .commit();
    }

    @OnClick(R.id.bt_home)
    public void goHome() {
        startActivity(homeIntent());
    }

    @OnClick(R.id.bt_stay_offline)
    public void setOfflineDelib() {
        NetworkUtils.switchToOfflineMode(null);
        startActivity(doneIntent());
        finish();
    }

    @OnClick(R.id.bt_keep_trying)
    public void setKeepRetrying() {
        startActivity(doneIntent());
        finish();
    }

    private Intent doneIntent() {
        switch (actionIntent) {
            case HOME_SCREEN:
                return homeIntent();
            case GROUP_SCREEN:
                Intent i = new Intent(ActionCompleteActivity.this, GroupTasksActivity.class);
                i.putExtra(GroupConstants.OBJECT_FIELD, groupToPass);
                i.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                return i;
            default:
                return homeIntent();
        }
    }

    private Intent homeIntent() {
        Intent i = new Intent(ActionCompleteActivity.this, HomeScreenActivity.class);
        if (!showTaskButtons) {
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        }
        return i;
    }

    @Override
    public void menuCloseClicked() {
        getSupportFragmentManager().popBackStack();
    }
}
