package org.grassroot.android.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.grassroot.android.R;
import org.grassroot.android.events.GroupRenamedEvent;
import org.grassroot.android.fragments.dialogs.EditTextDialogFragment;
import org.grassroot.android.models.Group;
import org.grassroot.android.services.GroupService;
import org.greenrobot.eventbus.Subscribe;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnCheckedChanged;
import butterknife.OnClick;
import butterknife.Unbinder;

/**
 * Created by luke on 2016/07/15.
 */
public class GroupSettingsMainFragment extends Fragment {

    private static final String TAG = GroupSettingsMainFragment.class.getSimpleName();

    private Group group;
    private GroupSettingsListener listener;

    private Unbinder unbinder;
    @BindView(R.id.gsfrag_header) TextView header;

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
        setUpViews();
        return v;
    }

    private void setUpViews() {
        if (group != null) {
            header.setText(group.getGroupName());
        }
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
                            GroupService.getInstance().renameGroup(group.getGroupUid(), textEntered.trim());
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

    }

    @OnCheckedChanged(R.id.gset_switch_join_code)
    public void switchJoinCodeOnOff() {

    }

    @Subscribe
    public void onGroupRenamed(GroupRenamedEvent e) {
        // just in case multiple of these around
        // todo : probably do this right away instead of waiting
        Log.e(TAG, "group renamed .. switching this to ... " + e.groupName);
        if (e.groupUid.equals(group.getGroupUid())) {
            header.setText(e.groupName);
        }
    }


}
