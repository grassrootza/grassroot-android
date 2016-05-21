package com.techmorphosis.grassroot.ui.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.ParseException;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.android.volley.AuthFailureError;
import com.android.volley.NoConnectionError;
import com.android.volley.ServerError;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.github.clans.fab.FloatingActionButton;
import com.github.clans.fab.FloatingActionMenu;
import com.techmorphosis.grassroot.Animator.CustomItemAnimator;
import com.techmorphosis.grassroot.Interface.SortInterface;
import com.techmorphosis.grassroot.Network.AllLinsks;
import com.techmorphosis.grassroot.Network.NetworkCall;
import com.techmorphosis.grassroot.R;
import com.techmorphosis.grassroot.RecyclerView.SwipeableRecyclerViewTouchListener;
import com.techmorphosis.grassroot.adapters.Group_homepageAdapter;
import com.techmorphosis.grassroot.models.Group_Homepage_Model;
import com.techmorphosis.grassroot.ui.DialogFragment.AlertDialogFragment;
import com.techmorphosis.grassroot.ui.DialogFragment.Group_ActivityMenuDialog;
import com.techmorphosis.grassroot.ui.activities.Create_Group;
import com.techmorphosis.grassroot.ui.activities.Group_Activities;
import com.techmorphosis.grassroot.ui.activities.Join_Request;
import com.techmorphosis.grassroot.utils.L;
import com.techmorphosis.grassroot.utils.ProgressBarCircularIndeterminate;
import com.techmorphosis.grassroot.utils.SettingPreffrence;
import com.techmorphosis.grassroot.utils.UtilClass;
import com.techmorphosis.grassroot.utils.listener.ErrorListenerVolley;
import com.techmorphosis.grassroot.utils.listener.ResponseListenerVolley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;

public class Group_Homepage extends android.support.v4.app.Fragment implements View.OnClickListener {

    UtilClass utilClass;
    private RelativeLayout rlGhpRoot;
    private RelativeLayout rlGhpToolbarTitle;
    private ImageView ivGhpDrawer;
    private ImageView ivGhpSearch;
    private ImageView ivGhpSort;
    private RecyclerView rcGhp;
    private Group_homepageAdapter group_homepageAdapter;
    private ArrayList<Group_Homepage_Model> groupList;
    private ArrayList<Group_Homepage_Model> groupListclone;

    private String TAG = Group_Homepage.class.getSimpleName();


    private LinearLayoutManager mLayoutManager;

    private FloatingActionMenu menu1;
    private FloatingActionButton icFabJoinGroup;
    private FloatingActionButton icFabStartGroup;
    private Context context;
    private ImageView ivCross;
    private EditText et_search;
    private RelativeLayout rlSearch;
    private RelativeLayout rlSimple;
    private ArrayList<Group_Homepage_Model> sortedList;
    private ArrayList<Group_Homepage_Model> organizerList;
    private ArrayList<Group_Homepage_Model> memberList;
    private ProgressBar mProgressBar;
    public boolean date_click = false, role_click = false, defaults_click = false;
    private View view;
    private FragmentCallbacks mCallbacks;
    public Snackbar snackbar;
    private AlertDialogFragment alertDialogFragment;
    private DrawerLayout drawer;

    private View errorLayout;
    private LinearLayout llNoResult;
    private LinearLayout llNoInternet;
    private LinearLayout llServerError;

    private int error_flag;//0-success 1- no Internet 2- Invalid Token 3- Unknown error
    private ProgressBarCircularIndeterminate prgGh;
    private TextView txtPrgGh;
    private LinearLayout llInvalidToken;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.activity_group__homepage, container, false);
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = getActivity();
        findAllViews();
        init();
        Recylerview();
        UserGroupWS();
    }

    public void UserGroupWS() {

        preExecute();

        doInBacground();


    }


    private void preExecute() {


        error_flag = 0;

        //visible
        prgGh.setVisibility(View.VISIBLE);
        txtPrgGh.setVisibility(View.VISIBLE);

        //gone
        rcGhp.setVisibility(View.INVISIBLE);
        errorLayout.setVisibility(View.GONE);
        llNoInternet.setVisibility(View.GONE);
        llServerError.setVisibility(View.GONE);
        llNoResult.setVisibility(View.GONE);


    }

    private void doInBacground() {

        NetworkCall networkCall = new NetworkCall
                (
                        getActivity(),
                        new ResponseListenerVolley() {
                            @Override
                            public void onSuccess(String repsonse) {
                                error_flag = 0;
                                postExecute(repsonse);
                            }
                        },
                        new ErrorListenerVolley() {
                            @Override
                            public void onError(VolleyError volleyError) {
                                if ((volleyError instanceof NoConnectionError) || (volleyError instanceof TimeoutError)) {

                                    error_flag = 1;
                                    postExecute("");

                                } else if (volleyError instanceof ServerError) {
                                    String response = null, status, message;
                                    try {
                                        response = new String(volleyError.networkResponse.data, "utf-8");
                                        L.e(TAG, "response is  ", response);
                                        JSONObject jsonObject_error = new JSONObject(response);

                                        error_flag = 0;
                                        postExecute(response);

                                    } catch (UnsupportedEncodingException e) {// Data is not able to UnsupportedEncodingException
                                        e.printStackTrace();

                                        error_flag = 5;
                                        postExecute("");

                                    } catch (JSONException e) {// Data is not able to parse
                                        e.printStackTrace();

                                        error_flag= 5;
                                        postExecute("");
                                    }


                                } else if (volleyError instanceof AuthFailureError) {

                                    error_flag = 2;
                                    postExecute("");

                                } else {// Unknown error
                                    error_flag = 5;
                                    postExecute("");

                                }

                            }
                        },
                        AllLinsks.usergroups + SettingPreffrence.getuser_mobilenumber(getActivity()) + "/" + SettingPreffrence.getuser_token(getActivity())
                        ,
                        getString(R.string.prg_message)
                        ,
                        false
                );

        networkCall.makeStringRequest_GET();
        Log.e(TAG, "link is " + AllLinsks.usergroups + SettingPreffrence.getuser_mobilenumber(getActivity()) + "/" + SettingPreffrence.getuser_token(getActivity()));
    }

    public void postExecute(String repsonse) {



        if (error_flag == 1) {//no Internet
            prgGh.setVisibility(View.GONE);
            txtPrgGh.setVisibility(View.GONE);

            errorLayout.setVisibility(View.VISIBLE);
            llNoInternet.setVisibility(View.VISIBLE);
        }
        else if (error_flag==2) {// Invalid Token
            prgGh.setVisibility(View.GONE);
            txtPrgGh.setVisibility(View.GONE);

            errorLayout.setVisibility(View.VISIBLE);
            llInvalidToken.setVisibility(View.VISIBLE);
        }
        else if (error_flag==3) {//Unknown error
            prgGh.setVisibility(View.GONE);
            txtPrgGh.setVisibility(View.GONE);

            errorLayout.setVisibility(View.VISIBLE);
            llServerError.setVisibility(View.VISIBLE);
        }

        else if (error_flag == 0) {
//            rcGhp.setVisibility(View.VISIBLE);

            String id = null, status, code, message;
            try {
                JSONObject jsonObject_success = new JSONObject(repsonse);
                status = jsonObject_success.getString("status");
                message = jsonObject_success.getString("message");

                if (status.equalsIgnoreCase("SUCCESS")) {

                    code = jsonObject_success.getString("code");
                    JSONArray data_array = jsonObject_success.getJSONArray("data");
                    if (data_array.length() > 0) {

                        for (int i = 0; i < data_array.length(); i++) {
                            jsonObject_success = (JSONObject) data_array.get(i);

                            Group_Homepage_Model model = new Group_Homepage_Model();

                            model.id = jsonObject_success.getString("id");
                            model.groupName = jsonObject_success.getString("groupName");
                            model.description = jsonObject_success.getString("description");
                            model.groupCreator = jsonObject_success.getString("groupCreator");
                            model.role = jsonObject_success.getString("role");
                            model.groupMemberCount = jsonObject_success.getString("groupMemberCount");
                            JSONObject dateTimeObject = jsonObject_success.getJSONObject("dateTime");
                                            /*Time*/
                            model.hour = dateTimeObject.getString("hour");
                            model.minute = dateTimeObject.getString("minute");
                            model.second = dateTimeObject.getString("second");
                            model.nano = dateTimeObject.getString("nano");

                                            /*Date*/
                            model.dayOfMonth = dateTimeObject.getString("dayOfMonth");
                            model.monthValue = dateTimeObject.getString("monthValue");
                            model.year = dateTimeObject.getString("year");

                            model.dateTimefull = Calendar2Date(model.dayOfMonth, model.monthValue, model.year, model.hour, model.minute, model.second, model.nano);
                            model.dateTimeshort = Calendar3Date(model.dayOfMonth, model.monthValue, model.year, model.hour, model.minute, model.second, model.nano);


                            JSONArray jsonArray_permission = jsonObject_success.getJSONArray("permissions");
                            //Log.e(TAG,"model.groupName   is  " + model.groupName);

                            ArrayList<String> permissionList = new ArrayList<>();
                            if (jsonArray_permission.length() > 0) {

                                for (int j = 0; j < jsonArray_permission.length(); j++) {
                                    permissionList.add(jsonArray_permission.getString(j));
                                }

                                model.permissionsList = permissionList;
                                // Log.e(TAG,"model.permissionsList.size() is  " + model.permissionsList.size());

                            } else {
                                model.permissionsList = permissionList;
                                // Log.e(TAG,"model.permissionsList.size() is  " + model.permissionsList.size());
                            }


                            groupListclone.add(model);
                            groupList.add(model);

                        }

                        group_homepageAdapter.addApplications(groupList);


                        //step1-hide the loader
                        prgGh.setVisibility(View.GONE);
                        txtPrgGh.setVisibility(View.GONE);

                        //step2- show the list
                        rcGhp.setVisibility(View.VISIBLE);

                        //step3- now enable the ui onclick
                        ivGhpSearch.setEnabled(true);
                        ivGhpSort.setEnabled(true);


                    }
                    else
                    {

                        //No result
                        prgGh.setVisibility(View.GONE);
                        txtPrgGh.setVisibility(View.GONE);

                        errorLayout.setVisibility(View.VISIBLE);
                        llNoResult.setVisibility(View.VISIBLE);
                    }



                } else if (status.equalsIgnoreCase("Failure")) {
                    Log.e(TAG, "Failure ");

                    prgGh.setVisibility(View.GONE);
                    txtPrgGh.setVisibility(View.GONE);


                    errorLayout.setVisibility(View.VISIBLE);
                    llNoResult.setVisibility(View.VISIBLE);

                } else {    // if status not matched
                    errorLayout.setVisibility(View.VISIBLE);
                    llServerError.setVisibility(View.VISIBLE);

                }


            } catch (JSONException e) {
                e.printStackTrace();
                Log.e(TAG, "JSONException is " + e.getMessage());

                prgGh.setVisibility(View.GONE);
                txtPrgGh.setVisibility(View.GONE);

                errorLayout.setVisibility(View.VISIBLE);
                llServerError.setVisibility(View.VISIBLE);

            }

        }
        else {

            Log.e(TAG, "case not match is "  );

            prgGh.setVisibility(View.GONE);
            txtPrgGh.setVisibility(View.GONE);

            errorLayout.setVisibility(View.VISIBLE);
            llServerError.setVisibility(View.VISIBLE);
        }


    }

    private String Calendar2Date(String dayOfMonth, String monthValue, String year, String hour, String minute, String second, String nano) {



        Calendar calendar = Calendar.getInstance();

        /*Date*/
        calendar.set(Calendar.DAY_OF_MONTH, Integer.parseInt(dayOfMonth));
        calendar.set(Calendar.MONTH, Integer.parseInt(monthValue) - 1);
        calendar.set(Calendar.YEAR, Integer.parseInt(year));

        /*Time*/
        calendar.set(Calendar.HOUR, Integer.parseInt(hour));
        calendar.set(Calendar.MINUTE, Integer.parseInt(minute));
        calendar.set(Calendar.SECOND, Integer.parseInt(second));

        Date date = calendar.getTime();

        SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yy:HH:mm:SS");
        String dateString = formatter.format(date);

        // Log.e(TAG,"dateString " + dateString);
        return dateString;
    }

    private String Calendar3Date(String dayOfMonth, String monthValue, String year, String hour, String minute, String second, String nano) {


        //get the current date as Calendar object
        Calendar calendar = Calendar.getInstance();

        /*Date*/
        calendar.set(Calendar.DAY_OF_MONTH, Integer.parseInt(dayOfMonth));
        calendar.set(Calendar.MONTH, Integer.parseInt(monthValue) - 1);
        calendar.set(Calendar.YEAR, Integer.parseInt(year));

        /*Time*/
        calendar.set(Calendar.HOUR, Integer.parseInt(hour));
        calendar.set(Calendar.MINUTE, Integer.parseInt(minute));
        calendar.set(Calendar.SECOND, Integer.parseInt(second));

        Date date = calendar.getTime();

        SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy");
        String dateString = formatter.format(date);

        return dateString;
    }


    private void Recylerview() {
        //   rcGhp.setHasFixedSize(true);

        // use a linear layout manager
        mLayoutManager = new LinearLayoutManager(getActivity());
        rcGhp.setLayoutManager(mLayoutManager);
        rcGhp.setItemAnimator(new CustomItemAnimator());
        group_homepageAdapter = new Group_homepageAdapter(getActivity(), new ArrayList<Group_Homepage_Model>(), Group_Homepage.this);
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
                        blank.putExtra("groupid", groupList.get(position).id);
                        blank.putExtra("groupName", groupList.get(position).groupName);
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
        utilClass = new UtilClass();
        groupList = new ArrayList<>();
        groupListclone = new ArrayList<>();
        context = getActivity().getBaseContext();
        ivGhpSort.setEnabled(false);
        ivGhpSearch.setEnabled(false);
    }

    private void findAllViews() {
        rlGhpRoot = (RelativeLayout) view.findViewById(R.id.rl_ghp_root);
        drawer = (DrawerLayout) getActivity().findViewById(R.id.drawer_layout);
        ivGhpDrawer = (ImageView) view.findViewById(R.id.iv_ghp_drawer);
        ivGhpSearch = (ImageView) view.findViewById(R.id.iv_ghp_search);
        ivGhpSort = (ImageView) view.findViewById(R.id.iv_ghp_sort);
        ivCross = (ImageView) view.findViewById(R.id.iv_cross);

        rlSearch = (RelativeLayout) view.findViewById(R.id.rl_search);
        rlSimple = (RelativeLayout) view.findViewById(R.id.rl_simple);


        et_search = (EditText) view.findViewById(R.id.et_search);

        rcGhp = (RecyclerView) view.findViewById(R.id.recycler_view);
        errorLayout = view.findViewById(R.id.icl_nm_error_layout);

        llNoResult = (LinearLayout) errorLayout.findViewById(R.id.ll_no_result);
        llNoInternet = (LinearLayout) errorLayout.findViewById(R.id.ll_no_internet);
        llServerError = (LinearLayout) errorLayout.findViewById(R.id.ll_server_error);
        llInvalidToken = (LinearLayout) errorLayout.findViewById(R.id.ll_invalid_token);

        // Handle ProgressBar
        mProgressBar = (ProgressBar) view.findViewById(R.id.progressBar);


        menu1 = (FloatingActionMenu) view.findViewById(R.id.menu1);
        icFabJoinGroup = (FloatingActionButton) view.findViewById(R.id.ic_fab_join_group);
        icFabStartGroup = (FloatingActionButton) view.findViewById(R.id.ic_fab_start_group);

        llNoResult.setOnClickListener(this);
        llServerError.setOnClickListener(this);
        llNoInternet.setOnClickListener(this);
        llInvalidToken.setOnClickListener(this);

        icFabJoinGroup.setOnClickListener(icFabJoinGroup());
        icFabStartGroup.setOnClickListener(icFabStartGroup());

        ivGhpSort.setOnClickListener(ivGhpSort());
        ivGhpSearch.setOnClickListener(ivGhpSearch());
        ivCross.setOnClickListener(ivCross());
        ivGhpDrawer.setOnClickListener(ivGhpDrawer());

        prgGh = (ProgressBarCircularIndeterminate) view.findViewById(R.id.prg_gh);
        txtPrgGh = (TextView) view.findViewById(R.id.txt_prg_gh);



        menu1.setOnMenuToggleListener(new FloatingActionMenu.OnMenuToggleListener() {
            @Override
            public void onMenuToggle(boolean opened) {
                String text = "";
                if (opened) {
                    icFabJoinGroup.setVisibility(View.VISIBLE);
                    icFabStartGroup.setVisibility(View.VISIBLE);
                    text = "Menu opened";
                    // menu2.addMenuButton(programFab2);

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


        try {
            if (group_homepageAdapter.getItemCount()>0) {
                group_homepageAdapter.filter(searchwords);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    private View.OnClickListener ivGhpDrawer() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //drawer
                mCallbacks.menuClick();

            }
        };
    }

    private View.OnClickListener ivGhpSearch() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //search
                rlSimple.setVisibility(View.GONE);
                rlSearch.setVisibility(View.VISIBLE);

            }
        };
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

                        //show progress
                        rcGhp.setVisibility(View.GONE);
                        mProgressBar.setVisibility(View.VISIBLE);

                        //pre-execute
                        group_homepageAdapter.clearApplications();

                        //doInBackground
                        // groupList.clear();


                        //postExecute
                        //handle visibility
                        rcGhp.setVisibility(View.VISIBLE);
                        mProgressBar.setVisibility(View.GONE);

                        //set data for list
                        group_homepageAdapter.addApplications(groupListclone);


                    }

                    @Override
                    public void roleClick(boolean date, boolean role, boolean defaults) {


                        Log.e(TAG, "roleClick is ");


                        date_click = date;
                        role_click = role;
                        defaults_click = defaults;

                        //show progress
                        rcGhp.setVisibility(View.GONE);
                        mProgressBar.setVisibility(View.VISIBLE);


                        // groupList.clear();
                        sortedList = new ArrayList<Group_Homepage_Model>();
                        organizerList = new ArrayList<Group_Homepage_Model>();
                        memberList = new ArrayList<Group_Homepage_Model>();
                        for (int i = 0; i < groupListclone.size(); i++) {
                            Group_Homepage_Model sortmodel = groupListclone.get(i);

                            if (sortmodel.role.equalsIgnoreCase("ROLE_GROUP_ORGANIZER")) {
                                // Log.e(TAG,"organizer groupName  " + sortmodel.groupName);
                                organizerList.add(sortmodel);
                            } else if (sortmodel.role.equalsIgnoreCase("ROLE_ORDINARY_MEMBER")) {
                                // Log.e(TAG,"member groupName  " + sortmodel.groupName);

                                memberList.add(sortmodel);
                            }

                        }

                        //sorted in descending order first
                        Collections.sort(organizerList, byDatebigger);
                        Collections.sort(memberList, byDatebigger);


                        //now ready to add into list
                        sortedList.addAll(organizerList);
                        sortedList.addAll(memberList);


                        //pre-execute
                        group_homepageAdapter.clearApplications();


                        //doInBackground
                        //  groupList.clear();

                        //postExecute
                        //handle visibility
                        rcGhp.setVisibility(View.VISIBLE);
                        mProgressBar.setVisibility(View.GONE);

                        //set data for list
                        group_homepageAdapter.addApplications(sortedList);

       /* for (int i = 0; i < sortedList.size(); i++)
        {
            Log.e(TAG,"sorted name  " + sortedList.get(i).groupName);
            Log.e(TAG,"sorted role  " + sortedList.get(i).role);
            Log.e(TAG,"sorted date  " + sortedList.get(i).dateTime);
        }*/

                        // groupList.addAll(groupListclone);


                    }

                    @Override
                    public void defaultsClick(boolean date, boolean role, boolean defaults) {
                        Log.e(TAG, "defaultsClick is ");

                        date_click = date;
                        role_click = role;
                        defaults_click = defaults;


                        //show progress
                        rcGhp.setVisibility(View.GONE);
                        mProgressBar.setVisibility(View.VISIBLE);

                        //pre-execute
                        group_homepageAdapter.clearApplications();

                        //doInBackground

                        //    groupList.clear();

                        //postExecute
                        //handle visibility
                        rcGhp.setVisibility(View.VISIBLE);
                        mProgressBar.setVisibility(View.GONE);

                        //set data for list
                        group_homepageAdapter.addApplications(groupList);


                    }

                    final Comparator<Group_Homepage_Model> byDatebigger = new Comparator<Group_Homepage_Model>() {
                        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yy:HH:mm:SS");

                        public int compare(Group_Homepage_Model lhs, Group_Homepage_Model rhs) {
                /* Log.e(TAG,"groupName1 is " + lhs.groupName  + " d1 is " + lhs.dateTimefull);
                            Log.e(TAG,"groupName2 is " + rhs.groupName  + " d2 is " + rhs.dateTimefull);
*/
                            Date d1 = null;
                            Date d2 = null;
                            try {
                                d1 = sdf.parse(lhs.dateTimefull);
                                d2 = sdf.parse(rhs.dateTimefull);
             /*   Log.e(TAG,"d1.getTime() is " + d1.getTime());
                Log.e(TAG,"d2.getTime() is " + d2.getTime());*/

                            } catch (ParseException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            } catch (java.text.ParseException e) {
                                e.printStackTrace();
                            }

                            return (d1.getTime() > d2.getTime() ? -1 : 1);     //descending
                            //  return (d1.getTime() > d2.getTime() ? 1 : -1);     //ascending
                            //return rhs.dateTime.compareTo(lhs.dateTime);


                        }
                    };


                });


            }
        };
    }


    private View.OnClickListener ivCross() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
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
        };
    }


    private View.OnClickListener icFabJoinGroup() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                try {
                    menu1.close(true);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                Intent icFabJoinGroup = new Intent(getActivity(), Join_Request.class);
                startActivity(icFabJoinGroup);
            }
        };
    }

    @Override
    public void onResume() {
        super.onResume();
        Animation animation1 = AnimationUtils.loadAnimation(getActivity(), R.anim.fab2);
        menu1.startAnimation(animation1);
        menu1.setVisibility(View.VISIBLE);


        if (SettingPreffrence.getPREF_Call_Vote(getActivity())) {

            SettingPreffrence.setPREF_Call_Vote(getActivity(), false);

            Snackbar.make(rlGhpRoot, getString(R.string.nm_cratevote_msg), Snackbar.LENGTH_SHORT).show();

        }
    }

    private View.OnClickListener icFabStartGroup() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    menu1.close(true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                Intent icFabStartGroup = new Intent(getActivity(), Create_Group.class);
                startActivity(icFabStartGroup);
            }
        };
    }

    @Override
    public void onClick(View v) {

        /*if (v == llNoResult || v == llServerError || v == llNoInternet || v==llInvalidToken) {
            llNoResult.setAlpha((float) 0.3);
        }*/
        switch (v.getId())
        {
            case  R.id.ll_no_result :
              //  llNoResult.setAlpha((float) 0.2);
                UserGroupWS();
                break;
            case  R.id.ll_server_error :
               // llServerError.setAlpha((float) 0.2);
                UserGroupWS();

                break;
            case  R.id.ll_no_internet :
               // llNoInternet.setAlpha((float) 0.2);
                UserGroupWS();

                break;
           /* case  R.id.ll_invalid_token :
                //llInvalidToken.setAlpha((float) 0.2);
                UserGroupWS();

                break;*/
        }


    }

    public void addClickStringAction(Context context, View cardView, final int position) {
        cardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                try {

                    menu1.close(true);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                Log.e(TAG, "Click");
                Log.e(TAG, "getGroupId is " + groupList.get(position).id);

                SettingPreffrence.setGroupId(getActivity(), groupList.get(position).id);


                Intent blank = new Intent(getActivity(), Group_Activities.class);
                blank.putExtra("groupid", groupList.get(position).id);
                blank.putExtra("groupName", groupList.get(position).groupName);
                startActivity(blank);
            }
        });

    }


    public void addLongClickStringAction(final Context context, View button, final int position) {
        button.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Log.e(TAG, "onLongClick ");
                Log.e(TAG, "getGroupId is " + groupList.get(position).id);

                SettingPreffrence.setGroupId(getActivity(), groupList.get(position).id);

                Boolean Meeting = false, ToDo = false, Vote = false;
                Group_Homepage_Model dialog_model = groupList.get(position);

                if (dialog_model.permissionsList.size() > 0) {
                    for (int i = 0; i < dialog_model.permissionsList.size(); i++) {
                        switch (dialog_model.permissionsList.get(i)) {
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
    public void onAttach(Activity activity) {
        super.onAttach(activity);
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


    public void showSnackbar(String message, int length, String actionButtontxt) {


        snackbar = Snackbar.make(drawer, message, length);
        snackbar.setActionTextColor(Color.RED);
        if (!TextUtils.isEmpty(actionButtontxt)) {

            if (actionButtontxt.equalsIgnoreCase(getString(R.string.Logout))) {
                snackbar.setAction(actionButtontxt, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        logout();
                    }
                });

            } else {
                snackbar.setAction(actionButtontxt, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        UserGroupWS();
                    }
                });
            }

        }

        snackbar.show();
    }


    private void logout() {
        getActivity().sendBroadcast(new Intent().setAction(getString(R.string.Logout)));
    }

}
