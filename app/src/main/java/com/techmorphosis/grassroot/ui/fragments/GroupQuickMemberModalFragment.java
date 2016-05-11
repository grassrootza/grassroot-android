package com.techmorphosis.grassroot.ui.fragments;


import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;

import com.techmorphosis.grassroot.R;
import com.techmorphosis.grassroot.ui.activities.AddMembersActivity;
import com.techmorphosis.grassroot.ui.activities.Blank;
import com.techmorphosis.grassroot.ui.activities.CreateGroupActivity;
import com.techmorphosis.grassroot.ui.activities.PhoneBookContactsActivity;
import com.techmorphosis.grassroot.utils.Constant;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;


public class GroupQuickMemberModalFragment extends android.support.v4.app.DialogFragment {

    private static final String TAG = GroupQuickMemberModalFragment.class.getSimpleName();

    @BindView(R.id.ic_home_add_member_active)
    ImageView icAddMemberIcon;
    @BindView(R.id.ic_home_view_members_active)
    ImageView icViewMemberIcon;
    @BindView(R.id.ic_edit_group_active)
    ImageView icEditSettingsIcon;
    private String groupUid;
    private String groupName;
    private boolean addMemberPermitted, viewMembersPermitted, editSettingsPermitted;

    // would rather use good practice and not have empty constructor, but Android is Android
    public GroupQuickMemberModalFragment() { }

    @Override
    public void onStart() {
        super.onStart();
        getDialog().getWindow().setWindowAnimations(R.style.animation_fast_flyinout);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.modal_group_members_quick, container, false);
        ButterKnife.bind(this, view);
        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(0));
        return view;
    }


    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        Bundle b = getArguments();
        if (b == null) { throw new UnsupportedOperationException("Error! Null arguments passed to modal"); }

        Log.d(TAG, "inside quickGroupMemberBundle, passed bundle = " + b.toString());

        this.groupUid = b.getString(Constant.GROUPUID_FIELD);
        this.groupName = b.getString(Constant.GROUPNAME_FIELD);

        addMemberPermitted = b.getBoolean("addMember");
        viewMembersPermitted = b.getBoolean("viewMembers");
        editSettingsPermitted = b.getBoolean("editSettings");

        int addIcon = addMemberPermitted ? R.drawable.ic_home_call_meeting_active : R.drawable.ic_home_call_meeting_inactive;
        int viewIcon = viewMembersPermitted ? R.drawable.ic_home_vote_active : R.drawable.ic_home_vote_inactive;
        int editIcon = editSettingsPermitted ? R.drawable.ic_home_to_do_active : R.drawable.ic_home_to_do_inactive;

        icViewMemberIcon.setImageResource(addIcon);
        icAddMemberIcon.setImageResource(viewIcon);
        icEditSettingsIcon.setImageResource(editIcon);
    }

    @OnClick(R.id.ic_home_add_member_active)
    public void gmAddMemberIconListener() {

                if (addMemberPermitted) {
                    Log.d(TAG, "inside modal ... add member icon clicked! for group with UID = " + groupUid + ", and name = "
                            + groupName);
                    Intent addMember = new Intent(getActivity(), AddMembersActivity.class);
                    addMember.putExtra(Constant.GROUPUID_FIELD, groupUid);
                    addMember.putExtra(Constant.GROUPNAME_FIELD, groupName);
                    startActivity(addMember);
                    getDialog().dismiss();
                } else {
                    getDialog().dismiss();
                }

    }

    @OnClick(R.id.ic_home_view_members_active)
    public  void gmViewMembersIconListener() {
        Log.d(TAG, "inside modal ... view member icon clicked!");
                if (viewMembersPermitted) {
                    Intent viewMembers = new Intent(getActivity(), Blank.class);
                    startActivity(viewMembers);
                    getDialog().dismiss();
                } else {
                    getDialog().dismiss();
                }

    }

    @OnClick(R.id.ic_edit_group_active)
    public void gmEditSettingsIconListener() {
        Log.d(TAG, "inside modal ... edit settings icon clicked!");

        if (editSettingsPermitted) {
            Intent editGroupSettings = new Intent(getActivity(), Blank.class);
            startActivity(editGroupSettings);
            getDialog().dismiss();
        } else {
            getDialog().dismiss();


        }
    } }