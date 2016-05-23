package com.techmorphosis.grassroot.ui.activities;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.techmorphosis.grassroot.R;
import com.techmorphosis.grassroot.adapters.FAQAdapter;
import com.techmorphosis.grassroot.models.FAQModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by Ravi on 16-09-2015.
 */
public class FAQActivity extends PortraitActivity implements View.OnClickListener {

    private int lastExpandedPosition = -1;

    ExpandableListView faqListview;

    List<String> listDataHeader;
    List<String> listDataChild;
    //List<String> listDatChild0, listDatChild1, listDatChild2, listDatChild3, listDatChild4, listDatChild5, listDatChild6, listDatChild7, listDatChild8, listDatChild9, listDatChild10, listDatChild11;
    List<String> listrow;


    private FAQAdapter faqAdapter;
    private FAQModel faqModel;
    private List<FAQModel> faqListData;


    private TextView txtNavOption;

    private ImageButton imgNinjaDelights, imgBack, imgClose;

    View rootView;
    private Toolbar fqToolbar;
    private LinearLayout rlRootFaq;

    @Nullable
    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.faq);
        findAllViews();
        setUpToolbar();
        init();
       // Snackbar.make(rlRootFaq, "FAQ", Snackbar.LENGTH_INDEFINITE).show();

    }


    private void setUpToolbar() {
        fqToolbar.setNavigationIcon(R.drawable.btn_back_wt);
        fqToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
    }


    public void onBackPressed()
    {

        finish();
    }

    private void findAllViews()
    {
        rlRootFaq = (LinearLayout) findViewById(R.id.rl_root_faq);
        faqListview = (ExpandableListView) findViewById(R.id.listview_faq);
        fqToolbar = (Toolbar) findViewById(R.id.fq_toolbar);
    }

    public void init() {





        faqListview.setGroupIndicator(null);
        listDataHeader = new ArrayList<String>();




        listrow = new ArrayList<String>();


        faqListData = new ArrayList<>();



        generateData();





        faqAdapter = new FAQAdapter(this, listDataHeader, listDataChild);
        faqListview.setAdapter(faqAdapter);


    }

    private void generateData() {
        String[] fq_qstn = getResources().getStringArray(R.array.fq_Header);
        listDataHeader = Arrays.asList(fq_qstn);


        // Answers
        String[] fq_ans = getResources().getStringArray(R.array.fq_Answer);
        listDataChild = Arrays.asList(fq_ans);




        faqListview.setOnGroupExpandListener(new ExpandableListView.OnGroupExpandListener() {
            @Override
            public void onGroupExpand(int groupPosition) {
                if (lastExpandedPosition != -1
                        && groupPosition != lastExpandedPosition) {
                    faqListview.collapseGroup(lastExpandedPosition);
                }
                lastExpandedPosition = groupPosition;
            }
        });

    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {


         /*   case R.id.img_back:
                getActivity().onBackPressed();
                break;*/

        }
    }
}
