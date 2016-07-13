package org.grassroot.android.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.grassroot.android.R;
import org.grassroot.android.interfaces.GroupConstants;
import org.grassroot.android.models.Group;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by luke on 2016/07/12.
 * Simple class to display a screen notifying user that task has been complete (to be customized & reused in time)
 */
public class ActionCompleteActivity extends PortraitActivity {

    private static final String TAG = ActionCompleteActivity.class.getSimpleName();

    public static final String HEADER_FIELD = "header";
    public static final String BODY_FIELD = "body";
    public static final String BTN_FIELD = "button";
    public static final String TASK_BUTTONS = "task_buttons";

    public static final String ACTION_INTENT = "action_intent";
    public static final String HOME_SCREEN = "home_screen";
    public static final String GROUP_SCREEN = "group_screen";

    private boolean customValues;
    private boolean showTaskButtons;
    private String actionIntent;
    private Group groupToPass;

    @BindView(R.id.ac_header) TextView header;
    @BindView(R.id.ac_body) TextView body;
    @BindView(R.id.ac_bt_done) Button done;
    @BindView(R.id.ac_btn_tasks) RelativeLayout taskButtons;

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
        final int buttonInt = args.getInt(BTN_FIELD, R.string.ac_btn_done);
        showTaskButtons = args.getBoolean(TASK_BUTTONS, false);

        header.setText(headerInt);
        body.setText(bodyString);
        if (showTaskButtons) {
            done.setVisibility(View.GONE);
            taskButtons.setVisibility(View.VISIBLE);
        } else {
            done.setText(buttonInt);
            done.setVisibility(View.VISIBLE);
            taskButtons.setVisibility(View.GONE);
        }
    }

    @OnClick(R.id.ac_bt_done)
    public void screenOver() {
        Log.e(TAG, "actionIntent = " + actionIntent);
        if (!customValues) {
            startActivity(homeIntent());
        } else {
            switch (actionIntent) {
                case HOME_SCREEN:
                    startActivity(homeIntent());
                    break;
                case GROUP_SCREEN:
                    Log.e(TAG, "action intent is group screen ...");
                    Intent i = new Intent(ActionCompleteActivity.this, GroupTasksActivity.class);
                    i.putExtra(GroupConstants.OBJECT_FIELD, groupToPass);
                    startActivity(i);
                    break;
                default:
                    startActivity(homeIntent());
            }
        }
    }

    @OnClick(R.id.bt_meeting)
    public void newMeeting() {
        Intent i = new Intent(ActionCompleteActivity.this, CreateMeetingActivity.class);
        i.putExtra(GroupConstants.UID_FIELD, groupToPass.getGroupUid());
        startActivity(i);
    }

    @OnClick(R.id.bt_vote)
    public void newVote() {
        Intent i = new Intent(ActionCompleteActivity.this, CreateVoteActivity.class);
        i.putExtra(GroupConstants.UID_FIELD, groupToPass.getGroupUid());
        startActivity(i);
    }

    @OnClick(R.id.bt_todo)
    public void newTodo() {
        Intent i = new Intent(ActionCompleteActivity.this, CreateTodoActivity.class);
        i.putExtra(GroupConstants.UID_FIELD, groupToPass.getGroupUid());
        startActivity(i);
    }

    @OnClick(R.id.bt_home)
    public void goHome() {
        startActivity(homeIntent());
    }

    private Intent homeIntent() {
        Intent i = new Intent(ActionCompleteActivity.this, HomeScreenActivity.class);
        if (!showTaskButtons) {
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        }
        return i;
    }

}
