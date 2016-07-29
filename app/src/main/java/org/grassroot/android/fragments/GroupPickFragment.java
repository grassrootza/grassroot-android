package org.grassroot.android.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import io.realm.RealmResults;
import java.util.ArrayList;
import java.util.List;
import org.grassroot.android.R;
import org.grassroot.android.adapters.GroupPickAdapter;
import org.grassroot.android.fragments.dialogs.ConfirmCancelDialogFragment;
import org.grassroot.android.interfaces.TaskConstants;
import org.grassroot.android.models.Group;
import org.grassroot.android.utils.RealmUtils;
import rx.Subscriber;

/**
 * Created by luke on 2016/06/29.
 */
public class GroupPickFragment extends Fragment
    implements GroupPickAdapter.GroupPickAdapterListener {

  private static final String TAG = GroupPickFragment.class.getSimpleName();

  private List<Group> filteredGroups;

  private GroupPickListener listener;
  private String returnTag;
  private String permissionToFilter;

  @BindView(R.id.gpick_recycler_view) RecyclerView recyclerView;

  private GroupPickAdapter groupPickAdapter;
  private Unbinder unbinder;

  public interface GroupPickListener {
    void onGroupPicked(Group group, String returnTag);
  }

  public static GroupPickFragment newInstance(final String permissionToFilter,
      final String returnTag, final GroupPickListener listener) {
    if (permissionToFilter == null || listener == null || TextUtils.isEmpty(returnTag)) {
      throw new UnsupportedOperationException(
          "Error! Group picker called without groups, task type or listener");
    }
    final GroupPickFragment fragment = new GroupPickFragment();
    fragment.returnTag = returnTag;
    fragment.listener = listener;
    fragment.permissionToFilter = permissionToFilter;
    return fragment;
  }

  @Override public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    filteredGroups = new ArrayList<>();
    groupPickAdapter = GroupPickAdapter.newInstance(filteredGroups, getContext(), this);
    RealmUtils.loadGroupsSorted().subscribe(new Subscriber<List<Group>>() {
      @Override public void onCompleted() {

      }

      @Override public void onError(Throwable e) {

      }

      @Override public void onNext(List<Group> groups) {
        // todo : show a friendlier error dialog if filtered groups list is empty
        for (Group g : groups) {
          List<String> permissions = g.getPermissionsList();
          if (permissions.contains(permissionToFilter)) {
            filteredGroups.add(g);
          }
        }
        groupPickAdapter.setGroupList(filteredGroups);
      }
    });
  }

  @Override public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
    View viewToReturn = inflater.inflate(R.layout.fragment_group_picker, container, false);
    unbinder = ButterKnife.bind(this, viewToReturn);
    recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
    recyclerView.setAdapter(groupPickAdapter);
    recyclerView.setHasFixedSize(true);
    return viewToReturn;
  }

  @Override public void onDestroyView() {
    super.onDestroyView();
    unbinder.unbind();
  }

  @Override public void onGroupPicked(final Group group) {
    // todo : have a "don't show this again" option
    final String message =
        String.format(getString(R.string.group_picker_confirm_string), getString(constructVerb()),
            group.getGroupName());
    ConfirmCancelDialogFragment.newInstance(message,
        new ConfirmCancelDialogFragment.ConfirmDialogListener() {
          @Override public void doConfirmClicked() {
            // todo : probably want to collapse the listener chain
            listener.onGroupPicked(group, returnTag);
          }
        }).show(getFragmentManager(), "confirm");
  }

  private int constructVerb() {
    switch (returnTag) {
      case TaskConstants.MEETING:
        return R.string.picker_call_mtg;
      case TaskConstants.VOTE:
        return R.string.picker_call_vote;
      case TaskConstants.TODO:
        return R.string.picker_rec_action;
      default:
        throw new UnsupportedOperationException("Error! Return tag in group picker not known type");
    }
  }
}