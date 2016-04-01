package com.techmorphosis.grassroot.ui.activities;

import android.os.Bundle;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.TextInputLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.github.clans.fab.FloatingActionButton;
import com.github.clans.fab.FloatingActionMenu;
import com.techmorphosis.grassroot.R;
import com.techmorphosis.grassroot.adapters.CreateGroupAdapter;
import com.techmorphosis.grassroot.models.Create_GroupModel;

import java.util.ArrayList;
import java.util.List;

public class Create_Group extends PortraitActivity {

    private RecyclerView mRecyclerView;
    private CreateGroupAdapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;
   public static FloatingActionMenu menu2;
    private FloatingActionButton fab12;
    private FloatingActionButton fab22;

    private LinearLayout llToolbar;
    private TextView txtToolbar;
    private RelativeLayout rlOne;
    private TextInputLayout et1;
    private TextInputLayout et2;
    private TextView tvCounter;
    private TextView tvRcTitle;
    private RecyclerView rcContacts;

    private List<Create_GroupModel> studentList;

    private Button btnSelection;
    private EditText et_groupnames;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create__group);

        findAllView();
        mRecyclerView();

/*
        et_groupnames.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count)
            {
                tvCounter.setText(""+count+"/"+"160");
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
*/


    }

    private void mRecyclerView()
    {


        studentList = new ArrayList<Create_GroupModel>();

        for (int i = 1; i <= 15; i++) {
            Create_GroupModel st = new Create_GroupModel("Person " + i, "androidstudent" + i
                    + "@gmail.com", false);

            studentList.add(st);
        }



        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        mRecyclerView.setHasFixedSize(true);

        // use a linear layout manager
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        // create an Object for Adapter
        mAdapter = new CreateGroupAdapter(studentList);


        // set the adapter object to the Recyclerview
        mRecyclerView.setAdapter(mAdapter);


      /*  ItemTouchHelper.Callback callback = new ItemTouchHelperCallback(mAdapter);
        ItemTouchHelper touchHelper = new ItemTouchHelper(callback);
        touchHelper.attachToRecyclerView(mRecyclerView);

        mRecyclerView.addItemDecoration(HeaderDecoration.with(mRecyclerView)
                .inflate(R.layout.vh_item_header)
                .build());*/

    }

    private void findAllView()
    {
        llToolbar = (LinearLayout) findViewById(R.id.ll_toolbar);
        CollapsingToolbarLayout collapsingToolbar = (CollapsingToolbarLayout) findViewById(R.id.collapsing_toolbar);
        menu2 = (FloatingActionMenu) findViewById(R.id.menu2);
        fab12 = (FloatingActionButton) findViewById(R.id.fab12);
        fab22 = (FloatingActionButton) findViewById(R.id.fab22);

        fab12.setOnClickListener(fab12());
        fab22.setOnClickListener(fab22());

        txtToolbar = (TextView) findViewById(R.id.txt_toolbar);
        rlOne = (RelativeLayout) findViewById(R.id.rl_one);
        et1 = (TextInputLayout) findViewById(R.id.et1);
         et_groupnames = (EditText) findViewById(R.id.et_groupnames);
        tvCounter = (TextView) findViewById(R.id.tv_counter);
       // tvRcTitle = (TextView) findViewById(R.id.tv_rc_title);
        mRecyclerView = (RecyclerView) findViewById(R.id.rc_contacts);
        btnSelection = (Button) findViewById(R.id.bt_save);
        btnSelection.setOnClickListener(Save());

     /*   final ContextThemeWrapper context = new ContextThemeWrapper(this, R.style.MenuButtonsStyle);
        final FloatingActionButton programFab2 = new FloatingActionButton(context);
        programFab2.setLabelText("Programmatically added button");
        programFab2.setImageResource(R.drawable.ic_edit);*/


        menu2.setOnMenuToggleListener(new FloatingActionMenu.OnMenuToggleListener() {
            @Override
            public void onMenuToggle(boolean opened) {
                String text = "";
                if (opened) {
                    fab22.setVisibility(View.VISIBLE);
                    fab12.setVisibility(View.VISIBLE);
                    text = "Menu opened";
                   // menu2.addMenuButton(programFab2);

                } else {
                    fab22.setVisibility(View.GONE);
                    fab12.setVisibility(View.GONE);

                    text = "Menu closed";
                   // menu2.removeMenuButton(programFab2);

                }
              //  Toast.makeText(Create_Group.this, text, Toast.LENGTH_SHORT).show();
            }
        });


    }

    private View.OnClickListener fab22() {
            return  new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                }
            };
    }

    private View.OnClickListener fab12() {
            return  new View.OnClickListener() {
                @Override
                public void onClick(View v) {


                }
            };
    }

    private View.OnClickListener Save() {
        return  new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                String data = "";
                List<Create_GroupModel> stList = ((CreateGroupAdapter) mAdapter)
                        .getStudentist();

                for (int i = 0; i < stList.size(); i++) {
                    Create_GroupModel singleStudent = stList.get(i);
                    if (singleStudent.isSelected() == true) {

                        data = data + "\n" + singleStudent.getName().toString();

                    }

                }

                Toast.makeText(getApplicationContext(), "Selected Students: \n" + data, Toast.LENGTH_LONG)
                        .show();

            }
        };
    }


}
