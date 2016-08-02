package org.grassroot.android.fragments;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SwitchCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.TextView;

import org.grassroot.android.R;
import org.grassroot.android.adapters.MemberRoleAdapter;
import org.grassroot.android.events.GroupEditErrorEvent;
import org.grassroot.android.events.GroupEditedEvent;
import org.grassroot.android.fragments.dialogs.ConfirmCancelDialogFragment;
import org.grassroot.android.fragments.dialogs.EditTextDialogFragment;
import org.grassroot.android.interfaces.GroupConstants;
import org.grassroot.android.models.Group;
import org.grassroot.android.services.GroupService;
import org.grassroot.android.utils.Constant;
import org.grassroot.android.utils.RealmUtils;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.Collections;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import rx.Subscriber;

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
    @BindView(R.id.gset_main_view) NestedScrollView mainRoot;

    @BindView(R.id.gset_switch_public_private) SwitchCompat switchPublicOnOff;
    private CompoundButton.OnCheckedChangeListener publicPrivateListener;

    @BindView(R.id.gset_switch_join_code) SwitchCompat switchJoinCode;
    private CompoundButton.OnCheckedChangeListener joinCodeListener;

    MemberRoleAdapter roleAdapter;
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
            memberRoles.setLayoutManager(new LinearLayoutManager(getContext()));
            roleAdapter = new MemberRoleAdapter(group.getGroupUid(), this);
            memberRoles.setAdapter(roleAdapter);

            publicPrivateListener = new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    switchPublicPrivate(isChecked);
                }
            };
            switchWithoutEvent(switchPublicOnOff, group.isDiscoverable(), publicPrivateListener);

            joinCodeListener = new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    switchJoinCodeOnOff(isChecked);
                }
            };
            switchWithoutEvent(switchJoinCode, group.hasJoinCode(), joinCodeListener);
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

    // todo : check for an error and, if one, then switch toggle back; same with cancel listener
    public void switchPublicPrivate(final boolean checkedState) {
        int message = group.isDiscoverable() ? R.string.gset_public_to_off : R.string.gset_public_to_on;
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(message)
                .setPositiveButton(R.string.alert_confirm, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        GroupService.getInstance().switchGroupPublicStatus(group, checkedState, GroupSettingsMainFragment.this);
                    }
                })
                .setNegativeButton(R.string.alert_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switchWithoutEvent(switchPublicOnOff, group.isDiscoverable(), publicPrivateListener);
                    }
                });
        builder.create().show();
    }

    public void switchJoinCodeOnOff(final boolean checkedState) {
        int message = group.hasJoinCode() ? R.string.gset_join_code_to_off : R.string.gset_join_code_to_off;
        ConfirmCancelDialogFragment fragment = ConfirmCancelDialogFragment.newInstance(message, new ConfirmCancelDialogFragment.ConfirmDialogListener() {
            @Override
            public void doConfirmClicked() {
                showProgressDialog();
                if (checkedState) {
                    GroupService.getInstance().openJoinCode(group, GroupSettingsMainFragment.this);
                } else {
                    GroupService.getInstance().closeJoinCode(group, GroupSettingsMainFragment.this);
                }
            }
        });
        fragment.show(getFragmentManager(), "SWITCH_JOIN_CODE");
    }

    private void switchWithoutEvent(SwitchCompat switchCompat, boolean state, CompoundButton.OnCheckedChangeListener listener) {
        switchCompat.setOnCheckedChangeListener(null);
        switchCompat.setChecked(state);
        switchCompat.setOnCheckedChangeListener(listener);
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
        GroupService.getInstance().removeGroupMembers(group.getGroupUid(), Collections.singleton(memberUid))
            .subscribe(new Subscriber() {
                @Override
                public void onCompleted() {
                    hideProgressDialog();
                }

                @Override
                public void onError(Throwable e) {

                }

                @Override
                public void onNext(Object o) {
                    roleAdapter.removeDisplayedMember(memberUid);
                }
            });
    }

    @Subscribe
    public void onEvent(GroupEditedEvent e) {
        Log.e(TAG, "received group edit event ...");
        hideProgressDialog();
        switch (e.editAction) {
            case GroupEditedEvent.RENAMED:
                header.setText(e.auxString);
                break;
            case GroupEditedEvent.ORGANIZER_ADDED:
            case GroupEditedEvent.ROLE_CHANGED:
                roleAdapter.refreshDisplayedMember(e.auxString);
                break;
        }
    }

    @Subscribe
    public void onEvent(GroupEditErrorEvent e) {
        // todo : show an error of some sort
        hideProgressDialog();
    }

    @Override
    public void joinCodeOpened(final String joinCode) {
        final String message = String.format(getString(R.string.gset_join_code_done), joinCode);
        Snackbar.make(mainRoot, message, Snackbar.LENGTH_LONG).show();
    }

    @Override
    public void apiCallComplete() {
        hideProgressDialog();
        reloadGroup();
    }

    @Override
    public void apiCallFailed(String tag, String offOrOnline) {
        Log.e(TAG, "API call failed ... revert state and show error message");
        hideProgressDialog();
        switch (tag) {
            case GroupEditedEvent.JOIN_CODE_CLOSED:
                switchWithoutEvent(switchJoinCode, true, joinCodeListener);
                Snackbar.make(mainRoot, Constant.OFFLINE.equals(offOrOnline) ? R.string.gset_error_join_code_create_offline :
                        R.string.gset_error_join_code_create_offline, Snackbar.LENGTH_SHORT).show();
                break;
            case GroupEditedEvent.JOIN_CODE_OPENED:
                switchWithoutEvent(switchJoinCode, false, joinCodeListener);
                Snackbar.make(mainRoot, Constant.OFFLINE.equals(offOrOnline) ? R.string.gset_error_join_code_create_offline :
                        R.string.gset_error_join_code_close_online, Snackbar.LENGTH_SHORT).show();
                break;
            case GroupEditedEvent.PUBLIC_STATUS_CHANGED:
                switchWithoutEvent(switchPublicOnOff, group.isDiscoverable(), publicPrivateListener);
                Snackbar.make(mainRoot, R.string.gset_error_public_online, Snackbar.LENGTH_SHORT).show();
                break;
        }
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

    private void reloadGroup() {
        group = RealmUtils.loadGroupFromDB(group.getGroupUid());
    }

}
