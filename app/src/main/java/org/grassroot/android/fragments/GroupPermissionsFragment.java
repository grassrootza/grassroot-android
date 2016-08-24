package org.grassroot.android.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import org.grassroot.android.R;
import org.grassroot.android.adapters.PermissionsAdapter;
import org.grassroot.android.interfaces.GroupConstants;
import org.grassroot.android.models.Group;
import org.grassroot.android.models.Permission;
import org.grassroot.android.services.GroupService;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;

/**
 * Created by luke on 2016/07/18.
 */
public class GroupPermissionsFragment extends Fragment implements GroupService.GroupPermissionsListener {

    private static final String TAG = GroupPermissionsFragment.class.getSimpleName();

    private Group group;
    private String role;

    @BindView(R.id.gset_permission_list) ListView listView;
    PermissionsAdapter permissionsAdapter;

    private Unbinder unbinder;

    public static GroupPermissionsFragment newInstance(Group group, String role) {
        GroupPermissionsFragment fragment = new GroupPermissionsFragment();
        if (group == null) {
            throw new UnsupportedOperationException("Error! Permissions fragment called without role");
        }
        if (!group.canEditGroup()) {
            throw new UnsupportedOperationException("Error! Permissions fragment called without permission to adjust");
        }
        if (!GroupConstants.ROLE_GROUP_ORGANIZER.equals(role) && !GroupConstants.ROLE_COMMITTEE_MEMBER.equals(role)
                && !GroupConstants.ROLE_ORDINARY_MEMBER.equals(role)) {
            throw new UnsupportedOperationException("Error! Permissions fragment called with non-standard role");
        }
        fragment.group = group;
        fragment.role = role;
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_permission_set, container, false);
        unbinder = ButterKnife.bind(this, view);
        GroupService.getInstance().fetchGroupPermissions(group, role, this);
        return view;
    }

    @OnClick(R.id.gset_perm_save)
    public void onSaveClick() {
        List<Permission> permissions = permissionsAdapter.getPermissions();
        GroupService.getInstance().updateGroupPermissions(group, role, permissions, this);
    }

    @Override
    public void permissionsLoaded(List<Permission> permissions) {
        permissionsAdapter = new PermissionsAdapter(getContext(), permissions);
        listView.setAdapter(permissionsAdapter);
        listView.setVisibility(View.VISIBLE);
        // permissionsAdapter.setPermissionsList(permissions);
    }

    @Override
    public void permissionsUpdated(List<Permission> permissions) {
        // todo : show error
    }

    @Override
    public void errorLoadingPermissions(String errorDescription) {
        // todo : show error
    }

    @Override
    public void errorUpdatingPermissions(String errorDescription) {

    }

    // note : with so many potential issues with conflicting changes etc., this should only ever be called while online

}
