package org.grassroot.android.fragments;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.grassroot.android.R;
import org.grassroot.android.adapters.GroupPickAdapter;
import org.grassroot.android.models.Group;
import org.grassroot.android.utils.RealmUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import io.reactivex.functions.Consumer;
import io.realm.Sort;

/**
 * Created by luke on 2016/06/29.
 */
public class GroupPickFragment extends Fragment {

  private static final String TAG = GroupPickFragment.class.getSimpleName();

  private List<Group> filteredGroups;

  @BindView(R.id.gpick_recycler_view) RecyclerView recyclerView;

  private GroupPickAdapter groupPickAdapter;
  private Unbinder unbinder;

  public static GroupPickFragment newInstance(final String permissionToFilter, final String returnTag) {
    final GroupPickFragment fragment = new GroupPickFragment();
    Bundle args = new Bundle();
    args.putString("TYPE", "PERMISSION");
    args.putString("RETURN_TAG", returnTag);
    args.putString("PERMISSION", permissionToFilter);
    fragment.setArguments(args);
    return fragment;
  }

  public static GroupPickFragment newInstance(boolean paidFor, final String returnTag) {
    final GroupPickFragment fragment = new GroupPickFragment();
    Bundle args = new Bundle();
    args.putString("TYPE", "PAID");
    args.putString("RETURN_TAG", returnTag);
    args.putBoolean("PAID", paidFor);
    fragment.setArguments(args);
    return fragment;
  }

  public static GroupPickFragment newInstance(ArrayList<Group> groupsToPick, final String returnTag) {
    final GroupPickFragment fragment = new GroupPickFragment();
    Bundle args = new Bundle();
    args.putString("TYPE", "MANUAL");
    args.putString("RETURN_TAG", returnTag);
    args.putParcelableArrayList("GROUPS", groupsToPick);
    fragment.setArguments(args);
    return fragment;
  }

  @Override public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    Bundle args = getArguments();
    String returnTag = args.getString("RETURN_TAG");

    filteredGroups = new ArrayList<>();
    // note : activity must implement adapter's listener
    groupPickAdapter = new GroupPickAdapter(returnTag, filteredGroups, getActivity());
    final String type = args.getString("TYPE");
    if ("PERMISSION".equals(type)) {
      loadGroupsByPermission(args.getString("PERMISSION"));
    } else if ("PAID".equals(type)) {
      loadGroupsByPaidStatus(args.getBoolean("PAID"));
    } else if ("MANUAL".equals(type)) {
      ArrayList<Group> groups = args.getParcelableArrayList("GROUPS");
      filteredGroups = groups;
      groupPickAdapter.setGroupList(filteredGroups);
    }

  }

  private void loadGroupsByPermission(final String permissionToFilter) {
    RealmUtils.loadGroupsSorted().subscribe(new Consumer<List<Group>>() {
      @Override
      public void accept(List<Group> groups) {
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

  private void loadGroupsByPaidStatus(final boolean paidFor) {
    final Map<String, Object> map = new HashMap<>();
    map.put("paidFor", paidFor);
    RealmUtils.loadGroupsFilteredSorted(map, "lastMajorChangeMillis", Sort.DESCENDING).subscribe(new Consumer<List<Group>>() {
      @Override
      public void accept(List<Group> groups) {
        Log.e(TAG, "loaded paid groups! with flag : " + paidFor + " and " + groups.size() + " returned");
        filteredGroups = groups;
        groupPickAdapter.setGroupList(filteredGroups);
      }
    });
  }

  @Override public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
    View viewToReturn = inflater.inflate(R.layout.fragment_group_picker, container, false);
    unbinder = ButterKnife.bind(this, viewToReturn);

    recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
    recyclerView.addItemDecoration(new DividerItemDecoration(getActivity()));
    recyclerView.setAdapter(groupPickAdapter);
    recyclerView.setHasFixedSize(true);

    return viewToReturn;
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    unbinder.unbind();
  }

  public class DividerItemDecoration extends RecyclerView.ItemDecoration {

    private final int[] ATTRS = new int[] { android.R.attr.listDivider };
    private Drawable divider;

    public DividerItemDecoration(Context context) {
      final TypedArray styledAttributes = context.obtainStyledAttributes(ATTRS);
      divider = styledAttributes.getDrawable(0);
      styledAttributes.recycle();
    }

    // should switch this to multiple item types in adapter in future
    @Override
    public void onDraw(Canvas c, RecyclerView parent, RecyclerView.State state) {
      final int left = parent.getPaddingLeft();
      final int right = parent.getWidth() - parent.getPaddingRight();

      final int childCount = parent.getChildCount();
      for (int i = 0; i < childCount; i++) {
        View child = parent.getChildAt(i);
        RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) child.getLayoutParams();
        int top = child.getBottom() + params.bottomMargin;
        int bottom = top + divider.getIntrinsicHeight();
        divider.setBounds(left, top, right, bottom);
        divider.draw(c);
      }
    }
  }
}