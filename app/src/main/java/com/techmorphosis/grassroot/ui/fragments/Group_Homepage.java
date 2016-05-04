package com.techmorphosis.grassroot.ui.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.ParseException;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import com.github.clans.fab.FloatingActionButton;
import com.github.clans.fab.FloatingActionMenu;
import com.techmorphosis.grassroot.Interface.SortInterface;
import com.techmorphosis.grassroot.R;
import com.techmorphosis.grassroot.adapters.Group_homepageAdapter;
import com.techmorphosis.grassroot.services.GrassrootRestService;
import com.techmorphosis.grassroot.services.model.Group;
import com.techmorphosis.grassroot.services.model.GroupResponse;
import com.techmorphosis.grassroot.ui.activities.Create_Group;
import com.techmorphosis.grassroot.ui.activities.CustomItemAnimator;
import com.techmorphosis.grassroot.ui.activities.Group_Activities;
import com.techmorphosis.grassroot.ui.activities.Join_Request;
import com.techmorphosis.grassroot.ui.activities.SwipeableRecyclerViewTouchListener;
import com.techmorphosis.grassroot.utils.SettingPreference;
import com.techmorphosis.grassroot.utils.UtilClass;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import retrofit.RetrofitError;
import rx.Observer;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class Group_Homepage extends android.support.v4.app.Fragment {

    UtilClass utilClass;
    private LinearLayoutManager mLayoutManager;

    @BindView(R.id.rl_ghp_root)
    RelativeLayout rlGhpRoot;
    @BindView(R.id.iv_ghp_drawer)
    ImageView ivGhpDrawer;
    @BindView(R.id.iv_ghp_search)
    ImageView ivGhpSearch;
    @BindView(R.id.iv_ghp_sort)
    ImageView ivGhpSort;
    @BindView(R.id.recycler_view)
    RecyclerView rcGhp;
    @BindView(R.id.error_layout)
    View errorLayout;
    @BindView(R.id.im_no_results)
    ImageView imNoResults;
    @BindView(R.id.im_server_error)
    ImageView imServerError;
    @BindView(R.id.im_no_internet)
    ImageView imNoInternet;
    @BindView(R.id.menu1)
    FloatingActionMenu menu1;
    @BindView(R.id.ic_fab_join_group)
    FloatingActionButton icFabJoinGroup;
    @BindView(R.id.ic_fab_start_group)
    FloatingActionButton icFabStartGroup;
    private Context context;
    @BindView(R.id.iv_cross)
    ImageView ivCross;
    @BindView(R.id.et_search)
    EditText et_search;
    @BindView(R.id.rl_search)
    RelativeLayout rlSearch;
    @BindView(R.id.rl_simple)
    RelativeLayout rlSimple;
    @BindView(R.id.progressBar)
    ProgressBar mProgressBar;


    private Group_homepageAdapter group_homepageAdapter;

    private ArrayList<Group> sortedList;
    private ArrayList<Group> organizerList;
    private ArrayList<Group> memberList;
    private ArrayList<Group> groupList;
    private ArrayList<Group> groupListclone;

    private String TAG = Group_Homepage.class.getSimpleName();
    public boolean date_click = false, role_click = false, defaults_click = false;
    private FragmentCallbacks mCallbacks;
    private GrassrootRestService grassrootRestService = new GrassrootRestService();


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "Inside group homepage on create view ... onCreateView");
        View view = inflater.inflate(R.layout.activity_group__homepage, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        Log.d(TAG, "Inside group homepage on create view ... onActivityCreated");
        super.onCreate(savedInstanceState);
        context = getActivity();
        findAllViews();
        init();
        RecylerView();
        userGroupWS();
    }

    /**
     * Method executed to retrieve and populate list of groups. Note: this does not handle the absence
     * of a connection very well, at all. Will probably need to rethink.
     */
    private void userGroupWS() {
        Log.d(TAG, "Inside group homepage on create view ... userGroupWS");
        //preExecute
        mProgressBar.setVisibility(View.VISIBLE);
        rcGhp.setVisibility(View.INVISIBLE);
        errorLayout.setVisibility(View.GONE);
        imNoInternet.setVisibility(View.GONE);
        imServerError.setVisibility(View.GONE);
        imNoResults.setVisibility(View.GONE);

        String mobileNumber = SettingPreference.getuser_mobilenumber(getActivity());
        String code = SettingPreference.getuser_token(getActivity());

        grassrootRestService.getApi().getUserGroups(mobileNumber, code)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<GroupResponse>() {
                    @Override
                    public void onCompleted() {
                        mProgressBar.setVisibility(View.INVISIBLE);
                        rcGhp.setVisibility(View.VISIBLE);
                    }

                    @Override
                    public void onError(Throwable e) {
                        mProgressBar.setVisibility(View.INVISIBLE);
                        if (((RetrofitError) e).getKind().equals(RetrofitError.Kind.NETWORK)) {
                            errorLayout.setVisibility(View.VISIBLE);
                            imNoInternet.setVisibility(View.VISIBLE);
                        } else {
                            errorLayout.setVisibility(View.VISIBLE);
                            imNoInternet.setVisibility(View.VISIBLE);
                            Snackbar.make(rlGhpRoot, getString(R.string.Unknown_error), Snackbar.LENGTH_INDEFINITE).show();
                        }
                    }

                    @Override
                    public void onNext(GroupResponse response) {
                        groupList.addAll(response.getGroups());
                        groupListclone.addAll(response.getGroups());
                        rcGhp.setVisibility(View.VISIBLE);
                        group_homepageAdapter.addData(groupList);
                        ivGhpSearch.setEnabled(true);
                        ivGhpSort.setEnabled(true);
                    }
                });
    }


    private void RecylerView() {

        Log.d(TAG, "Inside group homepage on create view ... RecyclerView");
        mLayoutManager = new LinearLayoutManager(getActivity());
        rcGhp.setLayoutManager(mLayoutManager);
        rcGhp.setItemAnimator(new CustomItemAnimator());
        group_homepageAdapter = new Group_homepageAdapter(getActivity(), new ArrayList<Group>(), Group_Homepage.this);
        rcGhp.setAdapter(group_homepageAdapter);

        SwipeableRecyclerViewTouchListener swipeDeleteTouchListener = new SwipeableRecyclerViewTouchListener(
                context,
                rcGhp,
                R.id.main_view,
                R.id.main_background_view,

                new SwipeableRecyclerViewTouchListener.SwipeListener() {
                    @Override
                    public boolean canSwipe(int position) {
                        //  Toast.makeText(getActivity(), "canSwipe", Toast.LENGTH_LONG).show();
                        try {
                            menu1.close(true);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        Intent blank = new Intent(getActivity(), Group_Activities.class);
                        blank.putExtra("groupid", groupList.get(position).getId());
                        blank.putExtra("groupName", groupList.get(position).getGroupName());
                        startActivity(blank);
                        return false;
                    }

                    @Override
                    public void onDismissedBySwipe(RecyclerView recyclerView, int[] reverseSortedPositions) {
                        //Toast.makeText(getActivity(),"onDismissedBySwipe",Toast.LENGTH_LONG).show();
                    }

                });
        rcGhp.addOnItemTouchListener(swipeDeleteTouchListener);


    }

    private void init() {
        Log.d(TAG, "Inside group homepage on create view ... init method");
        utilClass = new UtilClass();
        groupList = new ArrayList<>();
        groupListclone = new ArrayList<>();
        context = getActivity().getBaseContext();
        ivGhpSort.setEnabled(false);
        ivGhpSearch.setEnabled(false);
    }

    private void findAllViews() {
        Log.d(TAG, "Inside group homepage on create view ... findAllViews");
        ivGhpSort.setOnClickListener(ivGhpSort());
        menu1.setOnMenuToggleListener(new FloatingActionMenu.OnMenuToggleListener() {
            @Override
            public void onMenuToggle(boolean opened) {
                String text = "";
                if (opened) {
                    icFabJoinGroup.setVisibility(View.VISIBLE);
                    icFabStartGroup.setVisibility(View.VISIBLE);
                    text = "Menu opened";


                } else {
                    icFabStartGroup.setVisibility(View.GONE);
                    icFabJoinGroup.setVisibility(View.GONE);
                    text = "Menu closed";
                    // menu2.removeMenuButton(programFab2);
                }
            }
        });

        et_search.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() > 0) {
                    Filter(et_search.getText().toString());
                } else {
                    Filter("");
                }
            }
        });


    }

    private void Filter(String s) {
        //convert to Lowercase and then pass to adapter
        String searchwords = s.toLowerCase(Locale.getDefault());
        group_homepageAdapter.filter(searchwords);
    }

    @OnClick(R.id.iv_ghp_drawer)
    public void ivGhpDrawer() {
                mCallbacks.menuClick();
    }

    @OnClick(R.id.iv_ghp_search)
    public void ivGhpSearch() {
        rlSimple.setVisibility(View.GONE);
        rlSearch.setVisibility(View.VISIBLE);
    }

    private View.OnClickListener ivGhpSort() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //sort
                SortFragment sortFragment = new SortFragment();
                Bundle b = new Bundle();
                b.putBoolean("Date", date_click);
                b.putBoolean("Role", role_click);
                b.putBoolean("Default", defaults_click);
                sortFragment.setArguments(b);
                sortFragment.show(getFragmentManager(), "SortFragment");
                sortFragment.setListener(new SortInterface() {

                    @Override
                    public void tvDateClick(boolean date, boolean role, boolean defaults) {

                        Log.e(TAG, "tvDateClick is ");
                        date_click = date;
                        role_click = role;
                        defaults_click = defaults;

                        Collections.sort(groupListclone, byDatebigger);
                        rcGhp.setVisibility(View.GONE);
                        mProgressBar.setVisibility(View.VISIBLE);
                        group_homepageAdapter.clearGroups();
                        rcGhp.setVisibility(View.VISIBLE);
                        mProgressBar.setVisibility(View.GONE);
                        group_homepageAdapter.addData(groupListclone);

                    }

                    @Override
                    public void roleClick(boolean date, boolean role, boolean defaults) {


                        Log.e(TAG, "roleClick is ");


                        date_click = date;
                        role_click = role;
                        defaults_click = defaults;
                        rcGhp.setVisibility(View.GONE);
                        mProgressBar.setVisibility(View.VISIBLE);
                        sortedList = new ArrayList<Group>();
                        organizerList = new ArrayList<Group>();
                        memberList = new ArrayList<Group>();
                        for (int i = 0; i < groupListclone.size(); i++) {
                            Group sortmodel = groupListclone.get(i);

                            if (sortmodel.getRole().equalsIgnoreCase("ROLE_GROUP_ORGANIZER")) {
                                // Log.e(TAG,"organizer groupName  " + sortmodel.groupName);
                                organizerList.add(sortmodel);
                            } else if (sortmodel.getRole().equalsIgnoreCase("ROLE_ORDINARY_MEMBER")) {
                                // Log.e(TAG,"member groupName  " + sortmodel.groupName);

                                memberList.add(sortmodel);
                            }

                        }

                        Collections.sort(organizerList, byDatebigger);
                        Collections.sort(memberList, byDatebigger);
                        sortedList.addAll(organizerList);
                        sortedList.addAll(memberList);
                        group_homepageAdapter.clearGroups();
                        rcGhp.setVisibility(View.VISIBLE);
                        mProgressBar.setVisibility(View.GONE);

                        //set data for list

                        group_homepageAdapter.addData(sortedList);
                         groupList.addAll(groupListclone);


                    }

                    @Override
                    public void defaultsClick(boolean date, boolean role, boolean defaults) {
                        Log.e(TAG, "defaultsClick is ");

                        date_click = date;
                        role_click = role;
                        defaults_click = defaults;

                        rcGhp.setVisibility(View.GONE);
                        mProgressBar.setVisibility(View.VISIBLE);

                        group_homepageAdapter.clearGroups();


                        rcGhp.setVisibility(View.VISIBLE);
                        mProgressBar.setVisibility(View.GONE);

                        //set data for list
                        group_homepageAdapter.addData(groupList);


                    }

                    final Comparator<Group> byDatebigger = new Comparator<Group>() {
                        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yy:HH:mm:SS");

                        public int compare(Group lhs, Group rhs) {
                            Date d1 = null;
                            Date d2 = null;
                            try {
                                d1 = sdf.parse(lhs.getDateTimefull());
                                d2 = sdf.parse(rhs.getDateTimefull());


                            } catch (ParseException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            } catch (java.text.ParseException e) {
                                e.printStackTrace();
                            }
                            return (d1.getTime() > d2.getTime() ? -1 : 1);     //descending



                        }
                    };


                });


            }
        };
    }


    @OnClick(R.id.iv_cross)
    public void ivCross() {

                if (et_search.getText().toString().isEmpty()) {
                    rlSearch.setVisibility(View.GONE);
                    rlSimple.setVisibility(View.VISIBLE);

                    try {
                        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(getActivity().getCurrentFocus().getWindowToken(), 0);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    et_search.setText("");
                }


    }

    @OnClick(R.id.ic_fab_join_group)
    public void icFabJoinGroup() {
        try {
            menu1.close(true);
        } catch (Exception e) {
            e.printStackTrace();
        }

        Intent icFabJoinGroup = new Intent(getActivity(), Join_Request.class);
        startActivity(icFabJoinGroup);
    }

    @OnClick(R.id.ic_fab_start_group)
    public void icFabStartGroup() {
                try {
                    menu1.close(true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                Intent icFabStartGroup=new Intent(getActivity(), Create_Group.class);
                startActivity(icFabStartGroup);


    }

    @OnClick({R.id.im_no_results, R.id.im_no_internet,R.id.im_server_error})
    public void onClick(View v)
    {
        if (v==imNoResults || v==imServerError || v==imNoInternet )
            userGroupWS();

    }

    public void addClickStringAction(Context context, View cardView, final int position)
    {
        cardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                try {
                    menu1.close(true);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                Intent blank= new Intent(getActivity(),Group_Activities.class);
                blank.putExtra("groupid",groupList.get(position).getId());
                blank.putExtra("groupName",groupList.get(position).getGroupName());
                startActivity(blank);
            }
        });

    }

    public void addLongClickStringAction(final Context context, View button, final int position) {
        button.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Log.e(TAG, "Inside group homepage ... onLongClick at position ... " + position);
                Boolean Meeting = false, ToDo = false, Vote = false;
                Group dialog_model = groupList.get(position);

                // todo: refactor this. turn permissions into a hashsetset and use contains, instead of loop.
                if (dialog_model.getPermissions().size() > 0) {
                    for (int i = 0; i < dialog_model.getPermissions().size(); i++) {
                        switch (dialog_model.getPermissions().get(i)) {
                            case "GROUP_PERMISSION_CREATE_GROUP_VOTE":
                                Vote = true;
                                break;
                            case "GROUP_PERMISSION_CREATE_LOGBOOK_ENTRY":
                                ToDo = true;
                                break;
                            case "GROUP_PERMISSION_CREATE_GROUP_MEETING":
                                Meeting = true;
                                break;
                        }
                    }
                }

                Group_ActivityMenuDialog dialog = new Group_ActivityMenuDialog();

                Bundle args = new Bundle();
                args.putBoolean("Meeting", Meeting);
                args.putBoolean("Vote", Vote);
                args.putBoolean("ToDo", ToDo);
                dialog.setArguments(args);
                dialog.show(getFragmentManager(), "Group_ActivityMenuDialog");
                return true;
            }
        });
    }



    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        Activity activity = (Activity)context;
        try {
            mCallbacks = (FragmentCallbacks) activity;
            Log.e("onAttach", "Attached");
        } catch (ClassCastException e) {
            throw new ClassCastException("Activity must implement Fragment One.");
        }
    }

    public static interface FragmentCallbacks {
        void menuClick();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallbacks = null;
        Log.e("onDetach", "Detached");
    }


}
