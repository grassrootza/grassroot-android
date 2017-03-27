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
import org.grassroot.android.fragments.dialogs.NetworkErrorDialogFragment;
import org.grassroot.android.models.Permission;
import org.grassroot.android.services.GroupService;
import org.grassroot.android.utils.ErrorUtils;
import org.grassroot.android.utils.NetworkUtils;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import io.reactivex.SingleObserver;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;

/**
 * Created by luke on 2016/07/18.
 */
public class GroupPermissionsFragment extends Fragment {

    // note : with so many potential issues with conflicting changes etc., this should only ever be called while online
    // todo : make sure "mute member" is included here

    private String groupUid;
    private String role;

    @BindView(R.id.gset_permission_list) ListView listView;
    @BindView(R.id.progressBar) ProgressBar progressBar;

    PermissionsAdapter permissionsAdapter;
    List<Permission> permissions;

    private SingleObserver<String> subscriber;

    private Unbinder unbinder;

    public static GroupPermissionsFragment newInstance(String groupUid, String role,
                                                       List<Permission> permissions, SingleObserver<String> subscriber) {
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
            .subscribe(new Consumer<String>() {
                @Override
                public void accept(String s) {
                    progressBar.setVisibility(View.GONE);
                    subscriber.onSuccess(NetworkUtils.SAVED_SERVER);
                }
            }, new Consumer<Throwable>() {
                @Override
                public void accept(Throwable e) {
                    progressBar.setVisibility(View.GONE);
                    if (NetworkUtils.CONNECT_ERROR.equals(e.getMessage())) {
                        NetworkErrorDialogFragment.newInstance(R.string.gset_perms_error_connect, progressBar,
                                new SingleObserver<String>() {
                                    @Override
                                    public void onSubscribe(Disposable d) {

                                    }

                                    @Override
                                    public void onSuccess(String s) {
                                        progressBar.setVisibility(View.GONE);
                                        if (s.equals(NetworkUtils.CONNECT_ERROR)) {
                                            Snackbar.make(listView, R.string.connect_error_failed_retry, Snackbar.LENGTH_SHORT).show();
                                        } else {
                                            onSaveClick();
                                        }
                                    }

                                    @Override
                                    public void onError(Throwable e) {

                                    }
                                }).show(getFragmentManager(), "dialog");
                    } else {
                        Snackbar.make(listView, ErrorUtils.serverErrorText(e), Snackbar.LENGTH_SHORT)
                            .show();
                    }
                }
            });
    }

}