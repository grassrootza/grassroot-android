package org.grassroot.android.fragments;

import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.ProgressBar;

import org.grassroot.android.R;
import org.grassroot.android.adapters.PermissionsAdapter;
import org.grassroot.android.models.Permission;
import org.grassroot.android.services.GroupService;
import org.grassroot.android.utils.ErrorUtils;
import org.grassroot.android.utils.NetworkUtils;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import rx.Subscriber;
import rx.functions.Action1;

/**
 * Created by luke on 2016/07/18.
 */
public class GroupPermissionsFragment extends Fragment {

    // note : with so many potential issues with conflicting changes etc., this should only ever be called while online

    private String groupUid;
    private String role;

    @BindView(R.id.gset_permission_list) ListView listView;
    @BindView(R.id.progressBar) ProgressBar progressBar;

    PermissionsAdapter permissionsAdapter;
    List<Permission> permissions;

    private Subscriber<String> subscriber;

    private Unbinder unbinder;

    public static GroupPermissionsFragment newInstance(String groupUid, String role,
                                                       List<Permission> permissions, Subscriber<String> subscriber) {
        GroupPermissionsFragment fragment = new GroupPermissionsFragment();
        fragment.groupUid = groupUid;
        fragment.role = role;
        fragment.permissions = permissions;
        fragment.subscriber = subscriber;
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_permission_set, container, false);
        unbinder = ButterKnife.bind(this, view);

        permissionsAdapter = new PermissionsAdapter(getContext(), permissions);
        listView.setAdapter(permissionsAdapter);
        listView.setVisibility(View.VISIBLE);

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }

    @OnClick(R.id.gset_perm_save)
    public void onSaveClick() {
        progressBar.setVisibility(View.VISIBLE);
        List<Permission> permissions = permissionsAdapter.getPermissions();
        GroupService.getInstance().updateGroupPermissions(groupUid, role, permissions)
            .subscribe(new Action1<String>() {
                @Override
                public void call(String s) {
                    progressBar.setVisibility(View.GONE);
                    subscriber.onNext(NetworkUtils.SAVED_SERVER);
                }
            }, new Action1<Throwable>() {
                @Override
                public void call(Throwable e) {
                    progressBar.setVisibility(View.GONE);
                    if (NetworkUtils.CONNECT_ERROR.equals(e.getMessage())) {
                        ErrorUtils.snackBarWithAction(listView, R.string.gset_perms_error_connect,
                            R.string.snackbar_try_again, new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    onSaveClick();
                                }
                            });
                    } else {
                        Snackbar.make(listView, ErrorUtils.serverErrorText(e), Snackbar.LENGTH_SHORT)
                            .show();
                    }
                }
            });
    }

}