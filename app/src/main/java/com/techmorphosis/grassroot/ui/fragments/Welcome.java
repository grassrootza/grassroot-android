package com.techmorphosis.grassroot.ui.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.techmorphosis.grassroot.R;


public class Welcome extends Fragment  implements  View.OnClickListener{
    private String TAG = Welcome.class.getSimpleName();
    int error_flag = 0; // 1= network error , 2 = invalid response , 3 = failure,
    LinearLayout requestcontent;
    LinearLayout requestsupport;
    LinearLayout task;

    private FragmentCallbacks mCallbacks;
    private ImageView ivMembership;
    TextView txtMembership;
    Toolbar toolbar;
    TextView txtFragments,toolbarText;
    EditText etEditText;
    Button btnOpenRandomFrag, btnOpenRandomActivity;



    @Override
    public void onClick(View v) {



    }

    public static interface FragmentCallbacks {

        void menuClick();

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

    @Override
    public void onDetach() {
        super.onDetach();
        mCallbacks = null;
        Log.e("onDetach", "Detached");
    }

    private View v;
    private String strEditTextSaved;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        v = inflater.inflate(R.layout.fragment_home, container, false);
        findViewsById();




        return v;
    }

    private void setEditTextSaved() {
        /*if (strEditTextSaved != null) {
            etEditText.setText(strEditTextSaved);
        }*/
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        Log.e("onSaveInstanceState", "Saved");
       /* if (strEditTextSaved != null)
            outState.putString("edit_text_saved", etEditText.getText().toString());*/
    }


    private void findViewsById() {
        toolbar = (Toolbar) v.findViewById(R.id.toolbar);
        toolbarText = (TextView) toolbar.findViewById(R.id.txt_welcometitle);
        //setup width of custom title to match in parent toolbar
        toolbar.postDelayed(new Runnable() {
            @Override
            public void run() {
                int maxWidth = toolbar.getWidth();
                int titleWidth = toolbarText.getWidth();
                int iconWidth = maxWidth - titleWidth;

                if (iconWidth > 0) {
                    //icons (drawer, menu) are on left and right side
                    int width = maxWidth - iconWidth * 2;
                    toolbarText.setMinimumWidth(width);
                    toolbarText.getLayoutParams().width = width;
                    // toolbarText.getLayoutParams().height = maxheight;

                }
            }
        }, 0);
        toolbarText.setText("Welcome");
      //toolbar.setTitle(getResources().getString(R.string.fragment_one));
        toolbar.setNavigationIcon(getResources().getDrawable(R.drawable.btn_navigation));


      /*  requestcontent.setOnClickListener(this);
        requestsupport.setOnClickListener(this);
        task.setOnClickListener(this);
*/




        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCallbacks.menuClick();
            }
        });


    }



    public void onResume() {
        super.onResume();
        Log.e(TAG, "onResume ");




    }

    


}
