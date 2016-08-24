package org.grassroot.android.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

import org.grassroot.android.R;
import org.grassroot.android.models.Permission;

import java.util.List;

/**
 * Created by luke on 2016/07/18.
 */
public class PermissionsAdapter extends ArrayAdapter<Permission> {

    private static final String TAG = PermissionsAdapter.class.getSimpleName();

    List<Permission> permissions;

    public PermissionsAdapter(Context context, List<Permission> permissions) {
        super(context, R.layout.row_permission, permissions);
        this.permissions = permissions;
    }

    public List<Permission> getPermissions() {
        return permissions;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        final View viewToReturn;
        final PermissionViewHolder viewHolder;

        if (convertView != null) {
            try {
                viewToReturn = convertView;
                viewHolder = (PermissionViewHolder) convertView.getTag();
            } catch (ClassCastException e) {
                throw new UnsupportedOperationException("Error! Contact list adapter passed wrong kind of view");
            }
        } else {
            viewToReturn = LayoutInflater.from(getContext()).inflate(R.layout.row_permission, parent, false);
            viewHolder = new PermissionViewHolder(viewToReturn);
            viewHolder.permissionLabel = (TextView) viewToReturn.findViewById(R.id.permission_label);
            viewHolder.permissionDesc = (TextView) viewToReturn.findViewById(R.id.permission_description);
            viewHolder.permissionEnabled = (CheckBox) viewToReturn.findViewById(R.id.permission_selected);
        }

        final Permission permission = permissions.get(position);

        viewHolder.permissionLabel.setText(permission.getPermissionLabel());
        viewHolder.permissionDesc.setText(permission.getPermissionDesc());
        viewHolder.permissionEnabled.setChecked(permission.isPermissionEnabled());
        viewHolder.permissionEnabled.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CheckBox cb = (CheckBox) v;
                permission.setPermissionEnabled(cb.isChecked());
            }
        });

        return viewToReturn;
    }

    @Override
    public int getCount() {
        return permissions.size();
    }

    @Override
    public Permission getItem(final int position) {
        return permissions.get(position);
    }

    public static class PermissionViewHolder {

        public final View permissionView;

        TextView permissionLabel;
        TextView permissionDesc;
        CheckBox permissionEnabled;

        public PermissionViewHolder(View rootView) {
            permissionView = rootView;
            permissionView.setTag(this);
        }

    }



}
