package com.techmorphosis.grassroot.ui.activities;

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.techmorphosis.grassroot.Interface.ClickListener;
import com.techmorphosis.grassroot.R;
import com.techmorphosis.grassroot.RecyclerView.RecyclerTouchListener;
import com.techmorphosis.grassroot.adapters.JoinRequestAdapter;
import com.techmorphosis.grassroot.services.GrassrootRestService;
import com.techmorphosis.grassroot.services.model.GenericResponse;
import com.techmorphosis.grassroot.services.model.GroupSearchModel;
import com.techmorphosis.grassroot.services.model.GroupSearchResponse;
import com.techmorphosis.grassroot.ui.fragments.AlertDialogFragment;
import com.techmorphosis.grassroot.utils.SettingPreference;
import com.techmorphosis.grassroot.utils.UtilClass;
import com.techmorphosis.grassroot.utils.listener.AlertDialogListener;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import retrofit.RetrofitError;
import rx.Observer;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class Join_Request extends PortraitActivity implements OnClickListener{

    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.txt_toolbar)
    TextView txtToolbar;
    @BindView(R.id.et_searchbox)
    EditText et_searchbox;
    private UtilClass utilclass;
    @BindView(R.id.jr_RecyclerView)
    RecyclerView jrRecyclerView;
    @BindView(R.id.im_no_results)
    ImageView imNOResults;
    @BindView(R.id.im_server_error)
    ImageView imServerError;
    @BindView(R.id.im_no_internet)
    ImageView imNOInternet;
    @BindView(R.id.rl_root)
    RelativeLayout rlRoot;
    private LinearLayoutManager mLayoutManager;
    private JoinRequestAdapter joinrequestAdapter;
    private List<GroupSearchModel> joinrequestList;
    private boolean btn_close;
    private String TAG=Join_Request.class.getSimpleName();
    private Snackbar snackbar;
    @BindView(R.id.error_layout)
    View errorLayout;
    private AlertDialogFragment alerdialog;
    private String uid;
    //todo implement dependency injection
    private GrassrootRestService grassrootRestService = new GrassrootRestService();
    private ProgressDialog progressDialog;
    private String prgMessage = "Please Wait..";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_join__request);
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage(prgMessage);
        ButterKnife.bind(this);
        setUpSearchBox();
        setUpToolbar();
        init();
        setUpRecyclerView();
    }

    private void setUpRecyclerView()
    {
        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        jrRecyclerView.setHasFixedSize(true);

        // use a linear layout manager
        mLayoutManager = new LinearLayoutManager(this);
        jrRecyclerView.setLayoutManager(mLayoutManager);
        jrRecyclerView.setItemAnimator(new CustomItemAnimator());

        // specify an adapter
        joinrequestAdapter = new JoinRequestAdapter(getApplicationContext(),new ArrayList<GroupSearchModel>());
        jrRecyclerView.setAdapter(joinrequestAdapter);


        // setUpRecyclerView.setItemAnimator(new DefaultItemAnimator());
        jrRecyclerView.addOnItemTouchListener(new RecyclerTouchListener(getApplicationContext(), jrRecyclerView, new ClickListener() {
            @Override
            public void onClick(View view, int position) {
                uid = joinrequestList.get(position).getId();

                alerdialog = utilclass.showAlerDialog(getFragmentManager(), getString(R.string.alertbox), "NO", "YES", false, new AlertDialogListener() {
                    @Override
                    public void setRightButton() {
                        joinRequestWS();
                        alerdialog.dismiss();

                    }

                    @Override
                    public void setLeftButton() {
                        alerdialog.dismiss();

                    }
                });
            }

            @Override
            public void onLongClick(View view, int position) {

            }

        }));


    }


    private void init()
    {
        utilclass= new UtilClass();
        joinrequestList=new ArrayList<>();
    }

    private void setUpSearchBox()
    {

        et_searchbox.addTextChangedListener(new TextWatcher()
        {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s)
            {
                if (s.length()>0)
                {
                    et_searchbox.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.btn_close, 0);
                    btn_close=true;
                }
                else
                {
                    et_searchbox.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.btn_search_gray, 0);
                    btn_close=false;
                }

            }
        });



        et_searchbox.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                final int DRAWABLE_LEFT = 0;
                final int DRAWABLE_TOP = 1;
                final int DRAWABLE_RIGHT = 2;
                final int DRAWABLE_BOTTOM = 3;

                if (event.getAction() == MotionEvent.ACTION_UP) {
                    if (event.getRawX() >= (et_searchbox.getRight() - et_searchbox.getTotalPaddingRight())) {
                        // your action here
                        if (btn_close) {
                            //   Group_SearchWS();
                            et_searchbox.setText("");
                        }

                        return true;
                    }
                }
                return false;
            }
        });

        et_searchbox.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event)
            {
                if (actionId== EditorInfo.IME_ACTION_SEARCH)
                {
                    try {
                        InputMethodManager imm= (InputMethodManager) getApplicationContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    if (et_searchbox.getText().toString().trim().isEmpty())
                    {
                        showSnackBar(getString(R.string.validate_search_box),"",Snackbar.LENGTH_SHORT);

                    }
                    else
                    {
                        Group_SearchWS();
                    }



                }

                return false;
            }
        });

    }

    private void Group_SearchWS()
    {

       String searchTerm = et_searchbox.getText().toString().trim();
        Log.e(TAG, "Group_SearchWS");
        joinrequestList.clear();
        jrRecyclerView.setVisibility(View.INVISIBLE);
        errorLayout.setVisibility(View.INVISIBLE);
        imNOInternet.setVisibility(View.INVISIBLE);
        imNOResults.setVisibility(View.INVISIBLE);
        imServerError.setVisibility(View.INVISIBLE);
        grassrootRestService.getApi().search(searchTerm).subscribeOn(Schedulers.newThread())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(new Observer<GroupSearchResponse>() {
                @Override
                public void onCompleted() {
                    progressDialog.dismiss();
                }
                @Override
                public void onError(Throwable e) {
                    progressDialog.dismiss();
                    jrRecyclerView.setVisibility(View.GONE);

                    RetrofitError error = (RetrofitError)e;
                    if(error.getKind().equals(RetrofitError.Kind.NETWORK)){
                        showSnackBar(getString(R.string.No_network),getString(R.string.Retry),Snackbar.LENGTH_INDEFINITE);
                    }else{
                        errorLayout.setVisibility(View.VISIBLE);
                        imNOResults.setVisibility(View.VISIBLE);
                    }

                }
                @Override
                public void onNext(GroupSearchResponse response) {
                    progressDialog.dismiss();
                    jrRecyclerView.setVisibility(View.VISIBLE);
                    joinrequestAdapter.clearApplications();
                    joinrequestList = response.getGroups();
                    joinrequestAdapter.addResults(joinrequestList);
                }
            });



    }

    private void joinRequestWS()
    {
        Log.e(TAG, "joinRequestWS");
        progressDialog.show();
        String phoneNumber = SettingPreference.getuser_mobilenumber(this);
        String code = SettingPreference.getuser_token(this);
        grassrootRestService.getApi().groupJoinRequest(phoneNumber,code,uid)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<GenericResponse>() {
                    @Override
                    public void onCompleted() {
                        progressDialog.dismiss();
                    }
                    @Override
                    public void onError(Throwable e) {
                        progressDialog.dismiss();
                        RetrofitError error = (RetrofitError)e;
                        if(error.getKind().equals(RetrofitError.Kind.NETWORK)){
                            showSnackBar(getString(R.string.No_network),getString(R.string.Retry),Snackbar.LENGTH_INDEFINITE);
                        }else{
                            showSnackBar(getString(R.string.USER_ALREADY_PART),"",snackbar.LENGTH_LONG);
                        }

                    }
                    @Override
                    public void onNext(GenericResponse response) {
                        progressDialog.dismiss();
                        alerdialog = utilclass.showAlerDialog(getFragmentManager(),"Your request has been sent " ,"" ,"OK" ,false,new AlertDialogListener() {
                            @Override
                            public void setRightButton() {
                                finish();
                                alerdialog.dismiss();

                            }
                            @Override
                            public void setLeftButton() {

                            }
                        });
                    }
                });

    }

    private void setUpToolbar() {
        toolbar.setNavigationIcon(R.drawable.btn_back_wt);
        toolbar.setNavigationOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v)
            {
                finish();
            }
        });
    }


    private void showSnackBar(String message, String buttontext,int length)
    {
        snackbar= Snackbar.make(rlRoot,message,length);
        snackbar.setActionTextColor(Color.RED);

        if (!buttontext.isEmpty())
        {
            snackbar.setAction(buttontext, new OnClickListener() {
                @Override
                public void onClick(View v) {
                    joinRequestWS();
                }
            });
        }
        snackbar.show();

    }

    @OnClick({R.id.im_no_internet, R.id.im_no_results, R.id.im_server_error})
    public void onClick(View v)
    {
        if (v==imNOInternet || v==imNOResults || v==imNOResults )
            Group_SearchWS();

    }


}
