package org.grassroot.android.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
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

/**
 * Created by luke on 2016/07/18.
 */
public class GroupPermissionsFragment extends Fragment implements GroupService.GroupPermissionsListener {

    private static final String TAG = GroupPermissionsFragment.class.getSimpleName();

    private Group group;
    private String role;

    ListView listView;
    PermissionsAdapter permissionsAdapter;

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
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_permission_set, container, false);
        listView = (ListView) view.findViewById(R.id.gset_permission_list);
        Log.e(TAG, "fetching current permissions ... ");
        GroupService.getInstance().fetchGroupPermissions(group, role, this);
        return view;
    }

    @Override
    public void permissionsLoaded(List<Permission> permissions) {
        Log.e(TAG, "permissions loaded, back to fragment with: " + permissions);
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

    // note : with so many potential issues with conflicting changes etc., this should only ever be called while online

}
