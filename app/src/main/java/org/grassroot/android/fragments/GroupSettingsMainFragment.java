package org.grassroot.android.fragments;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
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
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.grassroot.android.R;
import org.grassroot.android.adapters.MemberRoleAdapter;
import org.grassroot.android.events.GroupEditErrorEvent;
import org.grassroot.android.events.GroupEditedEvent;
import org.grassroot.android.fragments.dialogs.ConfirmCancelDialogFragment;
import org.grassroot.android.fragments.dialogs.EditTextDialogFragment;
import org.grassroot.android.interfaces.GroupConstants;
import org.grassroot.android.models.Group;
import org.grassroot.android.services.ApplicationLoader;
import org.grassroot.android.services.GroupService;
import org.grassroot.android.utils.ErrorUtils;
import org.grassroot.android.utils.NetworkUtils;
import org.grassroot.android.utils.RealmUtils;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.Collections;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;

/**
 * Created by luke on 2016/07/15.
 */
public class GroupSettingsMainFragment extends Fragment implements MemberRoleAdapter.MemberRoleClickListener {

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

    @BindView(R.id.progressBar) ProgressBar progressBar;

    public interface GroupSettingsListener {
        void changeGroupPicture();
        void addOrganizer();
        void changePermissions(String roleName);
    }

    public static GroupSettingsMainFragment newInstance(@NonNull Group group, GroupSettingsListener listener) {
        GroupSettingsMainFragment fragment = new GroupSettingsMainFragment();
        fragment.group = group;
        fragment.listener = listener;
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_group_settings_main, container, false);
        unbinder = ButterKnife.bind(this, v);
        setUpViews();
        return v;
    }

    private void setUpViews() {
        header.setText(group.getGroupName());
        if (roleAdapter == null) {
            roleAdapter = new MemberRoleAdapter(group.getGroupUid(), this);
        } else {
            roleAdapter.refreshToDB(); // in case members changed while fragment removed
        }
        memberRoles.setAdapter(roleAdapter);
        memberRoles.setLayoutManager(new LinearLayoutManager(getContext()));

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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }

    @OnClick(R.id.gset_btn_rename)
    public void renameGroupDialog() {
        final String hint = String.format(getString(R.string.gset_rename_hint), group.getGroupName());
        EditTextDialogFragment dialog = EditTextDialogFragment.newInstance(R.string.gset_rename_heading, hint,
                new EditTextDialogFragment.EditTextDialogListener() {
                    @Override
                    public void confirmClicked(String textEntered) {
                    if (!textEntered.isEmpty()) {
                        renameGroupCall(textEntered.trim());
                    } else {
                        // todo : add the error handling into the fragment
                    }
                    }
                });
        dialog.show(getFragmentManager(), "RENAME");
    }

    private void renameGroupCall(final String newName) {
        progressBar.setVisibility(View.VISIBLE);
        GroupService.getInstance().renameGroup(group.getGroupUid(), newName, null)
            .subscribe(new Action1<String>() {
                @Override
                public void call(String s) {
                    progressBar.setVisibility(View.GONE);
                    if (NetworkUtils.SAVED_SERVER.equals(s)) {
                        Toast.makeText(ApplicationLoader.applicationContext, R
                            .string.gset_rename_done, Toast.LENGTH_SHORT).show();
                    } else {
                        ErrorUtils.snackBarWithAction(mainRoot, R.string.gset_rename_offline,
                            R.string.snackbar_try_again, new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    renameGroupCall(newName);
                                }
                            });
                    }
                    header.setText(newName);
                    group.setGroupName(newName); // to make sure local reference is up to date
                }
            }, new Action1<Throwable>() {
                @Override
                public void call(Throwable throwable) {
                    Snackbar.make(mainRoot, ErrorUtils.serverErrorText(throwable), Snackbar.LENGTH_SHORT)
                        .show();
                }
            });
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
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(R.string.gset_perms_title)
            .setItems(R.array.gset_roles, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    switch (which) {
                        case 0:
                            listener.changePermissions(GroupConstants.ROLE_GROUP_ORGANIZER);
                            break;
                        case 1:
                            listener.changePermissions(GroupConstants.ROLE_COMMITTEE_MEMBER);
                            break;
                        case 2:
                            listener.changePermissions(GroupConstants.ROLE_ORDINARY_MEMBER);
                            break;
                    }
                }
            });
        builder.create().show();
    }

    public void switchPublicPrivate(final boolean checkedState) {
        int message = group.isDiscoverable() ?
            R.string.gset_public_to_off :
            R.string.gset_public_to_on;

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        builder.setMessage(message)
                .setPositiveButton(R.string.alert_confirm, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        serviceCallPublicOnOff(checkedState);
                    }
                })
                .setNegativeButton(R.string.alert_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switchWithoutEvent(switchPublicOnOff, group.isDiscoverable(), publicPrivateListener);
                    }
                });

        builder.setCancelable(true)
            .setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    switchWithoutEvent(switchPublicOnOff, group.isDiscoverable(), publicPrivateListener);
                }
            });

        builder.create().show();
    }

    private void serviceCallPublicOnOff(final boolean setToPublic) {
        progressBar.setVisibility(View.VISIBLE);
        GroupService.getInstance().switchGroupPublicPrivate(group.getGroupUid(), setToPublic,
            AndroidSchedulers.mainThread()).subscribe(new Action1<String>() {
            @Override
            public void call(String s) {
                progressBar.setVisibility(View.GONE);
                if (s.equals(NetworkUtils.SAVED_SERVER)) {
                    Toast.makeText(ApplicationLoader.applicationContext, R
                        .string.gset_public_done, Toast.LENGTH_SHORT).show();
                } else {
                    ErrorUtils.snackBarWithAction(mainRoot, R.string.gset_offline_generic,
                        R.string.snackbar_try_again, new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                serviceCallPublicOnOff(setToPublic);
                            }
                        });
                }
                group.setDiscoverable(setToPublic);
            }
        }, new Action1<Throwable>() {
            @Override
            public void call(Throwable throwable) {
                progressBar.setVisibility(View.GONE);
                Snackbar.make(mainRoot, ErrorUtils.serverErrorText(throwable), Snackbar.LENGTH_SHORT)
                    .show();
                switchWithoutEvent(switchPublicOnOff, group.isDiscoverable(), publicPrivateListener);
            }
        });
    }

    public void switchJoinCodeOnOff(final boolean isChangingToOpen) {
        if (!NetworkUtils.isOnline()) {
            switchWithoutEvent(switchJoinCode, !isChangingToOpen, joinCodeListener);
            connectFailSnackbar(R.string.gset_join_code_offline);
        } else {
            final int message = group.hasJoinCode() ? R.string.gset_join_code_to_off : R.string.gset_join_code_to_off;
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setMessage(message)
                .setPositiveButton(R.string.alert_confirm, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        progressBar.setVisibility(View.VISIBLE);
                        if (isChangingToOpen)
                            serviceCallOpenJoinCode();
                        else
                            serviceCallCloseJoinCode();
                    }
                })
                .setNegativeButton(R.string.alert_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switchWithoutEvent(switchJoinCode, !isChangingToOpen, joinCodeListener);
                    }
                });

            builder.setCancelable(true)
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        switchWithoutEvent(switchJoinCode, !isChangingToOpen, joinCodeListener);
                    }
                });

            builder
                .create()
                .show();
        }
    }

    private void serviceCallOpenJoinCode() {
        GroupService.getInstance().openJoinCode(group.getGroupUid(), AndroidSchedulers.mainThread())
            .subscribe(new Action1<String>() {
                @Override
                public void call(String s) {
                    progressBar.setVisibility(View.GONE);
                    final String message = String.format(getString(R.string.gset_join_code_done), s);
                    Snackbar.make(mainRoot, message, Snackbar.LENGTH_LONG).show();
                }
            }, new Action1<Throwable>() {
                @Override
                public void call(Throwable e) {
                    progressBar.setVisibility(View.GONE);
                    if (e.getMessage().equals(NetworkUtils.CONNECT_ERROR)) {
                        connectFailSnackbar(R.string.gset_error_join_code_create_offline);
                    } else {
                        Snackbar.make(mainRoot, ErrorUtils.serverErrorText(e), Snackbar.LENGTH_SHORT).show();
                    }
                }
            });
    }

    private void serviceCallCloseJoinCode() {
        GroupService.getInstance().closeJoinCode(group.getGroupUid(), AndroidSchedulers.mainThread())
            .subscribe(new Action1<String>() {
                @Override
                public void call(String s) {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(ApplicationLoader.applicationContext,
                        R.string.gset_join_code_closed_done, Toast.LENGTH_SHORT).show();
                }
            }, new Action1<Throwable>() {
                @Override
                public void call(Throwable e) {
                    progressBar.setVisibility(View.GONE);
                    if (e.getMessage().equals(NetworkUtils.CONNECT_ERROR)) {
                        connectFailSnackbar(R.string.gset_error_join_code_close_offline);
                    } else {
                        Snackbar.make(mainRoot, ErrorUtils.serverErrorText(e), Snackbar.LENGTH_SHORT).show();
                    }
                }
            });
    }

    private void switchWithoutEvent(SwitchCompat switchCompat, boolean state, CompoundButton.OnCheckedChangeListener listener) {
        switchCompat.setOnCheckedChangeListener(null);
        switchCompat.setChecked(state);
        switchCompat.setOnCheckedChangeListener(listener);
    }

    private void connectFailSnackbar(final int message) {
        ErrorUtils.snackBarWithAction(mainRoot, message, R.string.snackbar_try_connect, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NetworkUtils.trySwitchToOnlineRx(getContext(), true, AndroidSchedulers.mainThread())
                    .subscribe(new Action1<String>() {
                        @Override
                        public void call(String s) {
                            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                            builder.setMessage(R.string.go_online_success);
                            builder.setCancelable(true).create().show();
                        }
                    }, new Action1<Throwable>() {
                        @Override
                        public void call(Throwable throwable) {
                            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                            builder.setMessage(R.string.go_online_failure_network);
                            builder.setCancelable(true).create().show();
                        }
                    });
            }
        });
    }

    public void onGroupMemberClicked(final String memberUid, final String memberName) {
        if (NetworkUtils.isOnline()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setItems(R.array.gset_member_popup, new DialogInterface.OnClickListener() {
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
    }

    private void switchMemberRole(final String memberUid, final String memberName) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setItems(R.array.gset_roles, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0:
                                changeRoleDo(memberUid, memberName, GroupConstants.ROLE_GROUP_ORGANIZER);
                                break;
                            case 1:
                                changeRoleDo(memberUid, memberName, GroupConstants.ROLE_COMMITTEE_MEMBER);
                                break;
                            case 2:
                                changeRoleDo(memberUid, memberName, GroupConstants.ROLE_ORDINARY_MEMBER);
                                break;
                        }
                    }
                });
        builder.create().show();
    }

    private void changeRoleDo(final String memberUid, final String memberName, final String roleToSet) {
        progressBar.setVisibility(View.VISIBLE);
        GroupService.getInstance().changeMemberRole(group.getGroupUid(), memberUid, roleToSet)
            .subscribe(new Action1<String>() {
                @Override
                public void call(String s) {
                    progressBar.setVisibility(View.GONE);
                    final String message = String.format(getString(R.string.gset_role_done), memberName);
                    Toast.makeText(ApplicationLoader.applicationContext, message, Toast.LENGTH_SHORT).show();
                    roleAdapter.refreshDisplayedMember(memberUid);
                }
            }, new Action1<Throwable>() {
                @Override
                public void call(Throwable e) {
                    progressBar.setVisibility(View.GONE);
                    if (NetworkUtils.CONNECT_ERROR.equals(e.getMessage())) {
                        connectFailSnackbar(R.string.gset_role_connect_error);
                    } else {
                        Snackbar.make(mainRoot, ErrorUtils.serverErrorText(e), Snackbar.LENGTH_LONG).show();
                    }
                }
            });
    }

    private void removeMemberConfirm(final String memberUid, final String memberName) {
        final String confirmMessage = String.format(getString(R.string.gset_remove_confirm), memberName);
        ConfirmCancelDialogFragment.newInstance(confirmMessage, new ConfirmCancelDialogFragment.ConfirmDialogListener() {
            @Override
            public void doConfirmClicked() {
                removeMember(memberUid, memberName);
            }
        }).show(getFragmentManager(), "confirm_remove");
    }

    private void removeMember(final String memberUid, final String memberName) {
        progressBar.setVisibility(View.VISIBLE);
        GroupService.getInstance().removeGroupMembers(group.getGroupUid(), Collections.singleton(memberUid))
            .subscribe(new Action1<String>() {
                @Override
                public void call(String s) {
                    progressBar.setVisibility(View.GONE);
                    if (NetworkUtils.SAVED_SERVER.equals(s)) {
                        final String message = String.format(getString(R.string.gset_remove_done), memberName);
                        Toast.makeText(ApplicationLoader.applicationContext, message, Toast.LENGTH_SHORT).show();
                        roleAdapter.removeDisplayedMember(memberUid);
                    } else {
                        connectFailSnackbar(R.string.gset_remove_connect_error);
                        roleAdapter.removeDisplayedMember(memberUid);
                    }
                }
            }, new Action1<Throwable>() {
                @Override
                public void call(Throwable e) {
                    progressBar.setVisibility(View.GONE);
                    if (NetworkUtils.CONNECT_ERROR.equals(e.getMessage())) {
                        connectFailSnackbar(R.string.gset_remove_connect_error);
                        roleAdapter.removeDisplayedMember(memberUid);
                    } else {
                        Snackbar.make(mainRoot, ErrorUtils.serverErrorText(e), Snackbar.LENGTH_SHORT).show();
                    }
                }
            });
    }

}
