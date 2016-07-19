package org.grassroot.android.fragments;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SwitchCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.grassroot.android.R;
import org.grassroot.android.adapters.MemberRoleAdapter;
import org.grassroot.android.events.GroupEditErrorEvent;
import org.grassroot.android.events.GroupEditedEvent;
import org.grassroot.android.fragments.dialogs.ConfirmCancelDialogFragment;
import org.grassroot.android.fragments.dialogs.EditTextDialogFragment;
import org.grassroot.android.interfaces.GroupConstants;
import org.grassroot.android.models.GenericResponse;
import org.grassroot.android.models.Group;
import org.grassroot.android.models.Member;
import org.grassroot.android.services.GroupService;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.Collections;
import java.util.HashMap;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnCheckedChanged;
import butterknife.OnClick;
import butterknife.Unbinder;

/**
 * Created by luke on 2016/07/15.
 */
public class GroupSettingsMainFragment extends Fragment implements GroupService.GroupEditingListener,
        MemberRoleAdapter.MemberRoleClickListener {

    private static final String TAG = GroupSettingsMainFragment.class.getSimpleName();

    private Group group;
    private GroupSettingsListener listener;

    private Unbinder unbinder;
    @BindView(R.id.gsfrag_header) TextView header;
    @BindView(R.id.gset_switch_public_private) SwitchCompat switchPublicOnOff;
    @BindView(R.id.gset_switch_join_code) SwitchCompat switchJoinCode;

    @BindView(R.id.gset_member_roles) RecyclerView memberRoles;

    ProgressDialog progressDialog;

    public interface GroupSettingsListener {
        void changeGroupPicture();
        void addOrganizer();
        void changePermissions();
    }

    public static GroupSettingsMainFragment newInstance(Group group, GroupSettingsListener listener) {
        if (group == null) {
            throw new UnsupportedOperationException("Error! No group passed");
        }
        GroupSettingsMainFragment fragment = new GroupSettingsMainFragment();
        fragment.group = group;
        fragment.listener = listener;
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_group_settings_main, container, false);
        unbinder = ButterKnife.bind(this, v);
        EventBus.getDefault().register(this);
        setUpViews();
        return v;
    }

    private void setUpViews() {
        if (group != null) {
            header.setText(group.getGroupName());
            switchPublicOnOff.setChecked(group.isPublic());
            memberRoles.setHasFixedSize(true);
            memberRoles.setLayoutManager(new LinearLayoutManager(getContext()));
            MemberRoleAdapter adapter = new MemberRoleAdapter(group.getGroupUid(), this);
            memberRoles.setAdapter(adapter);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
        EventBus.getDefault().unregister(this);
        if (progressDialog != null) {
            progressDialog.dismiss();
        }
    }

    @OnClick(R.id.gset_btn_rename)
    public void renameGroupDialog() {
        final String hint = String.format(getString(R.string.gset_rename_hint), group.getGroupName());
        EditTextDialogFragment dialog = EditTextDialogFragment.newInstance(R.string.gset_rename_heading, hint,
                new EditTextDialogFragment.EditTextDialogListener() {
                    @Override
                    public void confirmClicked(String textEntered) {
                        if (!textEntered.isEmpty()) {
                            showProgressDialog();
                            GroupService.getInstance().renameGroup(group, textEntered.trim());
                        }
                    }
                });
        dialog.show(getFragmentManager(), "RENAME");
    }

    @OnClick(R.id.gset_btn_picture)
    public void launchPictureFragment() {
        listener.changeGroupPicture();
    }

    @OnClick(R.id.gset_btn_add_org)
    public void addOrganizerDialog() {
        listener.addOrganizer();
    }

    @OnClick(R.id.gset_btn_change_perms)
    public void launchPermissionsFrag() {
        listener.changePermissions();
    }

    @OnCheckedChanged(R.id.gset_switch_public_private)
    public void switchPublicPrivate() {
        switchPublicOnOff.setChecked(!group.isPublic());
        int message = group.isPublic() ? R.string.gset_public_to_off : R.string.gset_public_to_on;
        // todo : custom dialog to show multiple lines
        ConfirmCancelDialogFragment fragment = ConfirmCancelDialogFragment.newInstance(message, new ConfirmCancelDialogFragment.ConfirmDialogListener() {
            @Override
            public void doConfirmClicked() {
                Log.e(TAG, "switch it!");
                GroupService.getInstance().switchGroupPublicStatus(group, !group.isPublic());
            }
        });
        fragment.show(getFragmentManager(), "SWITCH_PUBLIC");
    }

    @OnCheckedChanged(R.id.gset_switch_join_code)
    public void switchJoinCodeOnOff() {
        switchJoinCode.setChecked(!(group.getJoinCode() == null));
        int message = group.hasJoinCode() ? R.string.gset_join_code_to_off : R.string.gset_join_code_to_off;
        ConfirmCancelDialogFragment fragment = ConfirmCancelDialogFragment.newInstance(message, new ConfirmCancelDialogFragment.ConfirmDialogListener() {
            @Override
            public void doConfirmClicked() {
                progressDialog.show();
                if (group.hasJoinCode()) {
                    GroupService.getInstance().closeJoinCode(group, GroupSettingsMainFragment.this);
                } else {
                    GroupService.getInstance().openJoinCode(group, GroupSettingsMainFragment.this);
                }
            }
        });
        fragment.show(getFragmentManager(), "SWITCH_JOIN_CODE");
    }

    public void onGroupMemberClicked(final String memberUid, final String memberName) {
        // todo : tweak / fix the UI here
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        final String changeRole = getString(R.string.gset_member_role);
        final String removeMember = String.format(getString(R.string.gset_member_remove), memberName);
        builder.setItems(new CharSequence[]{changeRole, removeMember}, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0:
                        switchMemberRole(memberUid, memberName);
                        break;
                    case 1:
                        removeMemberConfirm(memberUid, memberName);
                        break;
                }
            }
        });
        builder.create().show();
    }

    private void switchMemberRole(final String memberUid, final String memberName) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        // todo : prevent organizer
        builder.setItems(R.array.gset_roles, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0:
                                GroupService.getInstance().changeMemberRole(group.getGroupUid(),
                                        memberUid, GroupConstants.ROLE_GROUP_ORGANIZER);
                                break;
                            case 1:
                                GroupService.getInstance().changeMemberRole(group.getGroupUid(),
                                        memberUid, GroupConstants.ROLE_COMMITTEE_MEMBER);
                                break;
                            case 2:
                                GroupService.getInstance().changeMemberRole(group.getGroupUid(),
                                        memberUid, GroupConstants.ROLE_ORDINARY_MEMBER);
                                break;
                        }
                    }
                });
        builder.create().show();
    }

    private void removeMemberConfirm(final String memberUid, final String memberName) {
        final String confirmMessage = String.format(getString(R.string.gset_remove_confirm), memberName);
        ConfirmCancelDialogFragment.newInstance(confirmMessage, new ConfirmCancelDialogFragment.ConfirmDialogListener() {
            @Override
            public void doConfirmClicked() {
                removeMember(memberUid);
            }
        }).show(getFragmentManager(), "confirm_remove");
    }

    private void removeMember(final String memberUid) {
        showProgressDialog();
        GroupService.getInstance().removeGroupMembers(group, Collections.singleton(memberUid), new GroupService.MembersRemovedListener() {
            @Override
            public void membersRemoved(String saveType) {
                hideProgressDialog();
                // todo : remove the member in the view
            }

            @Override
            public void memberRemovalError(String errorType, Object data) {
                hideProgressDialog();
            }
        });
    }

    @Subscribe
    public void onEvent(GroupEditedEvent e) {
        // todo : maybe do this right away instead of waiting
        hideProgressDialog();
        if (e.groupUid.equals(group.getGroupUid())) {
            header.setText(e.groupName);
        }
    }

    @Subscribe
    public void onEvent(GroupEditErrorEvent e) {
        // todo : show an error of some sort
        hideProgressDialog();
    }

    @Override
    public void joinCodeOpened(final String joinCode) {

    }

    @Override
    public void apiCallComplete() {
        hideProgressDialog();
    }

    private void showProgressDialog() {
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(getContext(), R.style.AppTheme);
            progressDialog.setMessage(getString(R.string.txt_pls_wait));
            progressDialog.setIndeterminate(true);

        }

        if (!progressDialog.isShowing()) {
            progressDialog.show();
        }
    }

    private void hideProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.hide();
        }
    }


}
